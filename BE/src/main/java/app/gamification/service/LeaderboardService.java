package app.gamification.service;

import app.gamification.dto.response.LeaderboardEntryResponse;
import app.gamification.dto.response.LeaderboardLogResponse;
import app.gamification.dto.response.LeaderboardMeResponse;
import app.gamification.event.PointEvent;
import app.gamification.model.LeaderboardPointsLog;
import app.gamification.model.UserPointAction;
import app.gamification.repository.LeaderboardPointsLogRepository;
import app.gamification.repository.LeaderboardScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final LeaderboardPointsLogRepository logRepo;
    private final LeaderboardScoreRepository scoreRepo;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // --- LOGIC XỬ LÝ EVENT ---

    /**
     * Listener này sẽ chạy khi có sự kiện PointEvent được publish.
     * @Async: Chạy ở thread riêng để không block luồng chính (cần @EnableAsync ở main config)
     * Nếu không muốn async (để đảm bảo consistency tức thì), bỏ @Async đi.
     */
    @Async 
    @EventListener
    @Transactional
    public void handlePointEvent(PointEvent event) {
        try {
            processAddPoints(event.getUserId(), event.getRoleGroup(), event.getAction(), event.getRefId());
        } catch (Exception e) {
            log.error("Lỗi khi tính điểm cho user {}: {}", event.getUserId(), e.getMessage());
        }
    }

    private void processAddPoints(Long userId, String rawRole, UserPointAction action, Long refId) {
        String role = normalizeRole(rawRole);
        if ("ADMIN".equals(role) || "UNKNOWN".equals(role)) return;

        // 1. Kiểm tra giới hạn ngày (Daily Limit)
        OffsetDateTime startOfToday = LocalDate.now(VN_ZONE).atStartOfDay(VN_ZONE).toOffsetDateTime();
        long currentCount = logRepo.countActionsToday(userId, action.name(), startOfToday);
        
        if (currentCount >= action.getDailyLimit()) {
            return; // Đã đạt giới hạn trong ngày
        }

        // 2. Kiểm tra trùng lặp Logic (nếu cần refId)
        // Ví dụ: Không thể nhận điểm 2 lần cho cùng 1 JobId khi ứng tuyển
        if (refId != null && logRepo.existsByUserIdAndActionTypeAndRefId(userId, action.name(), refId)) {
            return; 
        }

        // 3. Ghi Log
        LeaderboardPointsLog pointsLog = new LeaderboardPointsLog();
        pointsLog.setUserId(userId);
        pointsLog.setRole(role);
        pointsLog.setActionType(action.name());
        pointsLog.setPoints(action.getPoints());
        pointsLog.setRefId(refId);
        logRepo.save(pointsLog);

        // 4. Update bảng điểm (4 chu kỳ: Tuần, Tháng, Năm, All-Time)
        updateScoreMultiPeriods(userId, role, action.getPoints());
    }

    private void updateScoreMultiPeriods(Long userId, String role, int points) {
        LocalDate now = LocalDate.now(VN_ZONE);
        // FIX: Hardcode locale US hoặc ISO để week number ổn định, không phụ thuộc server locale
        WeekFields isoWf = WeekFields.ISO;
        String weekKey = String.format("%d-W%02d", now.get(isoWf.weekBasedYear()), now.get(isoWf.weekOfWeekBasedYear()));
        
        String monthKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String yearKey = now.format(DateTimeFormatter.ofPattern("yyyy"));

        scoreRepo.upsertScore(userId, role, "WEEK", weekKey, points);
        scoreRepo.upsertScore(userId, role, "MONTH", monthKey, points);
        scoreRepo.upsertScore(userId, role, "YEAR", yearKey, points);
        scoreRepo.upsertScore(userId, role, "ALL_TIME", "ALL", points);
    }

    // --- API SUPPORT (READ ONLY) ---

    public List<LeaderboardEntryResponse> getTopUsers(String role, String periodType, String periodKey, int limit) {
        String normalizedRole = normalizeRole(role);
        String validKey = resolvePeriodKey(periodType, periodKey);
        return scoreRepo.findTopRankings(normalizedRole, periodType.toUpperCase(), validKey, limit);
    }

    public LeaderboardMeResponse getMyRank(Long userId, String role, String periodType, String periodKey) {
        String normalizedRole = normalizeRole(role);
        String validKey = resolvePeriodKey(periodType, periodKey);
        return scoreRepo.findMyRank(userId, normalizedRole, periodType.toUpperCase(), validKey);
    }

    public List<LeaderboardLogResponse> getSystemLogs(int limit) {
        return logRepo.findRecentLogs(limit);
    }

    public List<Map<String, Object>> getMissions(String roleGroup, Long userId) {
        List<Map<String, Object>> missions = new ArrayList<>();
        String role = normalizeRole(roleGroup);
        boolean isRecruiter = "RECRUITER".equals(role);

        // Lấy thời gian bắt đầu ngày hôm nay (dùng lại logic của hàm processAddPoints)
        OffsetDateTime startOfToday = LocalDate.now(VN_ZONE).atStartOfDay(VN_ZONE).toOffsetDateTime();

        for (UserPointAction action : UserPointAction.values()) {
            boolean forRecruiter = action == UserPointAction.JOB_POST_APPROVED 
                                || action == UserPointAction.REVIEW_CV 
                                || action == UserPointAction.HIRED 
                                || action == UserPointAction.LOGIN_DAILY;
            
            boolean forCandidate = action == UserPointAction.APPLY 
                                || action == UserPointAction.INTERVIEW_PRACTICE 
                                || action == UserPointAction.UPLOAD_CV 
                                || action == UserPointAction.LOGIN_DAILY;

            if ((isRecruiter && forRecruiter) || (!isRecruiter && forCandidate)) {
                // LOGIC MỚI: Kiểm tra tiến độ
                long currentCount = 0;
                if (userId != null) {
                    // Tận dụng hàm countActionsToday đã có sẵn trong Repo (đang dùng ở processAddPoints)
                    currentCount = logRepo.countActionsToday(userId, action.name(), startOfToday);
                }
                
                // Đảm bảo không hiển thị quá giới hạn (ví dụ làm 6/5 thì chỉ hiện 5/5)
                long displayCount = Math.min(currentCount, action.getDailyLimit());

                missions.add(Map.of(
                    "code", action.name(),
                    "name", action.getDescription(),
                    "description", "Giới hạn: " + action.getDailyLimit() + " lần/ngày",
                    "points", action.getPoints(),
                    "dailyLimit", action.getDailyLimit(),
                    "completedCount", displayCount, // <--- TRƯỜNG MỚI THÊM
                    "isFinished", displayCount >= action.getDailyLimit() // <--- TRƯỜNG MỚI THÊM
                ));
            }
        }
        return missions;
    }

    // --- HELPER METHODS ---

    private String normalizeRole(String role) {
        if (role == null) return "UNKNOWN";
        String r = role.trim().toUpperCase();
        if (r.contains("CANDIDATE")) return "CANDIDATE";
        if (r.contains("RECRUITER")) return "RECRUITER";
        return r;
    }

    private String resolvePeriodKey(String type, String key) {
        if (key != null && !key.isBlank()) return key;
        
        LocalDate now = LocalDate.now(VN_ZONE);
        return switch (type.toUpperCase()) {
            case "WEEK" -> {
                WeekFields isoWf = WeekFields.ISO;
                yield String.format("%d-W%02d", now.get(isoWf.weekBasedYear()), now.get(isoWf.weekOfWeekBasedYear()));
            }
            case "MONTH" -> now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "YEAR" -> now.format(DateTimeFormatter.ofPattern("yyyy"));
            default -> "ALL";
        };
    }
}