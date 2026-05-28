package app.ai.service;

import app.ai.models.InterviewSession;
import app.ai.repository.IInterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewCleanupService {

    private final IInterviewSessionRepository sessionRepository;

    /**
     * Chạy định kỳ mỗi 10 phút (600000 ms)
     * Nhiệm vụ: Tìm các phiên "ONGOING" đã tạo quá 1 tiếng mà chưa kết thúc.
     * Hành động: XÓA VĨNH VIỄN (vì không có dữ liệu chat để chấm).
     */
    @Scheduled(fixedRate = 600000) 
    @Transactional
    public void cleanupAbandonedSessions() {
        // Thời điểm giới hạn: 1 tiếng trước
        LocalDateTime cutOffTime = LocalDateTime.now().minusHours(1);

        // Tìm các phiên ONGOING cũ
        List<InterviewSession> abandonedSessions = sessionRepository.findByStatusAndCreatedAtBefore("ONGOING", cutOffTime);

        if (!abandonedSessions.isEmpty()) {
            log.info("Phát hiện {} phiên phỏng vấn bị bỏ dở. Đang tiến hành dọn dẹp...", abandonedSessions.size());
            
            sessionRepository.deleteAll(abandonedSessions);
            
            log.info("Đã xóa xong các phiên bị bỏ dở.");
        }
    }
}
