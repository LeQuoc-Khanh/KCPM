package app.gamification.service;

import app.gamification.dto.response.LeaderboardEntryResponse;
import app.gamification.dto.response.LeaderboardLogResponse;
import app.gamification.dto.response.LeaderboardMeResponse;
import app.gamification.event.PointEvent;
import app.gamification.model.LeaderboardPointsLog;
import app.gamification.model.UserPointAction;
import app.gamification.repository.LeaderboardPointsLogRepository;
import app.gamification.repository.LeaderboardScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private LeaderboardPointsLogRepository logRepo;

    @Mock
    private LeaderboardScoreRepository scoreRepo;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void handlePointEvent_shouldSaveLogAndUpdateScoresForAllPeriods_whenValidCandidateEvent() {
        Long userId = 1L;
        Long refId = 100L;
        UserPointAction action = UserPointAction.APPLY;
        PointEvent event = new PointEvent(this, userId, "ROLE_CANDIDATE", action, refId);

        when(logRepo.countActionsToday(eq(userId), eq(action.name()), any(OffsetDateTime.class)))
                .thenReturn(0L);
        when(logRepo.existsByUserIdAndActionTypeAndRefId(userId, action.name(), refId))
                .thenReturn(false);
        when(logRepo.save(any(LeaderboardPointsLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        leaderboardService.handlePointEvent(event);

        ArgumentCaptor<LeaderboardPointsLog> logCaptor = ArgumentCaptor.forClass(LeaderboardPointsLog.class);
        verify(logRepo).save(logCaptor.capture());

        LeaderboardPointsLog savedLog = logCaptor.getValue();
        assertEquals(userId, savedLog.getUserId());
        assertEquals("CANDIDATE", savedLog.getRole());
        assertEquals(action.name(), savedLog.getActionType());
        assertEquals(action.getPoints(), savedLog.getPoints());
        assertEquals(refId, savedLog.getRefId());

        verify(scoreRepo).upsertScore(userId, "CANDIDATE", "WEEK", currentWeekKey(), action.getPoints());
        verify(scoreRepo).upsertScore(userId, "CANDIDATE", "MONTH", currentMonthKey(), action.getPoints());
        verify(scoreRepo).upsertScore(userId, "CANDIDATE", "YEAR", currentYearKey(), action.getPoints());
        verify(scoreRepo).upsertScore(userId, "CANDIDATE", "ALL_TIME", "ALL", action.getPoints());
    }

    @Test
    void handlePointEvent_shouldNotProcess_whenRoleIsAdmin() {
        PointEvent event = new PointEvent(this, 1L, "ADMIN", UserPointAction.LOGIN_DAILY, null);

        leaderboardService.handlePointEvent(event);

        verifyNoInteractions(logRepo);
        verifyNoInteractions(scoreRepo);
    }

    @Test
    void handlePointEvent_shouldNotProcess_whenRoleIsUnknown() {
        PointEvent event = new PointEvent(this, 1L, null, UserPointAction.LOGIN_DAILY, null);

        leaderboardService.handlePointEvent(event);

        verifyNoInteractions(logRepo);
        verifyNoInteractions(scoreRepo);
    }

    @Test
    void handlePointEvent_shouldNotSaveOrUpdateScore_whenDailyLimitReached() {
        Long userId = 1L;
        UserPointAction action = UserPointAction.LOGIN_DAILY;
        PointEvent event = new PointEvent(this, userId, "CANDIDATE", action, null);

        when(logRepo.countActionsToday(eq(userId), eq(action.name()), any(OffsetDateTime.class)))
                .thenReturn((long) action.getDailyLimit());

        leaderboardService.handlePointEvent(event);

        verify(logRepo, never()).save(any(LeaderboardPointsLog.class));
        verify(scoreRepo, never()).upsertScore(anyLong(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void handlePointEvent_shouldNotSaveOrUpdateScore_whenDuplicateRefIdExists() {
        Long userId = 1L;
        Long refId = 200L;
        UserPointAction action = UserPointAction.APPLY;
        PointEvent event = new PointEvent(this, userId, "CANDIDATE", action, refId);

        when(logRepo.countActionsToday(eq(userId), eq(action.name()), any(OffsetDateTime.class)))
                .thenReturn(0L);
        when(logRepo.existsByUserIdAndActionTypeAndRefId(userId, action.name(), refId))
                .thenReturn(true);

        leaderboardService.handlePointEvent(event);

        verify(logRepo, never()).save(any(LeaderboardPointsLog.class));
        verify(scoreRepo, never()).upsertScore(anyLong(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void handlePointEvent_shouldNotThrow_whenRepositoryThrowsException() {
        Long userId = 1L;
        UserPointAction action = UserPointAction.APPLY;
        PointEvent event = new PointEvent(this, userId, "CANDIDATE", action, 300L);

        when(logRepo.countActionsToday(eq(userId), eq(action.name()), any(OffsetDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> leaderboardService.handlePointEvent(event));
    }

    @Test
    void getTopUsers_shouldNormalizeRoleAndUseProvidedPeriodKey() {
        List<LeaderboardEntryResponse> expected = List.of(mock(LeaderboardEntryResponse.class));
        when(scoreRepo.findTopRankings("RECRUITER", "MONTH", "2026-06", 10))
                .thenReturn(expected);

        List<LeaderboardEntryResponse> actual = leaderboardService.getTopUsers(
                "ROLE_RECRUITER", "month", "2026-06", 10
        );

        assertEquals(expected, actual);
        verify(scoreRepo).findTopRankings("RECRUITER", "MONTH", "2026-06", 10);
    }

    @Test
    void getTopUsers_shouldResolveAllTimeKey_whenPeriodKeyIsNull() {
        List<LeaderboardEntryResponse> expected = List.of(mock(LeaderboardEntryResponse.class));
        when(scoreRepo.findTopRankings("CANDIDATE", "ALL_TIME", "ALL", 50))
                .thenReturn(expected);

        List<LeaderboardEntryResponse> actual = leaderboardService.getTopUsers(
                "candidate", "ALL_TIME", null, 50
        );

        assertEquals(expected, actual);
        verify(scoreRepo).findTopRankings("CANDIDATE", "ALL_TIME", "ALL", 50);
    }

    @Test
    void getMyRank_shouldNormalizeRoleAndUseProvidedPeriodKey() {
        Long userId = 1L;
        LeaderboardMeResponse expected = mock(LeaderboardMeResponse.class);
        when(scoreRepo.findMyRank(userId, "CANDIDATE", "WEEK", "2026-W26"))
                .thenReturn(expected);

        LeaderboardMeResponse actual = leaderboardService.getMyRank(
                userId, "candidate", "week", "2026-W26"
        );

        assertEquals(expected, actual);
        verify(scoreRepo).findMyRank(userId, "CANDIDATE", "WEEK", "2026-W26");
    }

    @Test
    void getSystemLogs_shouldReturnRecentLogsFromRepository() {
        List<LeaderboardLogResponse> expected = List.of(mock(LeaderboardLogResponse.class));
        when(logRepo.findRecentLogs(5)).thenReturn(expected);

        List<LeaderboardLogResponse> actual = leaderboardService.getSystemLogs(5);

        assertEquals(expected, actual);
        verify(logRepo).findRecentLogs(5);
    }

    @Test
    void getMissions_shouldReturnCandidateMissionsWithProgress() {
        Long userId = 1L;
        when(logRepo.countActionsToday(eq(userId), anyString(), any(OffsetDateTime.class)))
                .thenReturn(1L);

        List<Map<String, Object>> missions = leaderboardService.getMissions("CANDIDATE", userId);

        assertEquals(4, missions.size());
        assertMissionCodes(missions, "LOGIN_DAILY", "APPLY", "INTERVIEW_PRACTICE", "UPLOAD_CV");

        Map<String, Object> loginMission = findMission(missions, "LOGIN_DAILY");
        assertEquals(1L, loginMission.get("completedCount"));
        assertEquals(true, loginMission.get("isFinished"));
    }

    @Test
    void getMissions_shouldReturnRecruiterMissionsWithProgress() {
        Long userId = 2L;
        when(logRepo.countActionsToday(eq(userId), anyString(), any(OffsetDateTime.class)))
                .thenReturn(0L);

        List<Map<String, Object>> missions = leaderboardService.getMissions("RECRUITER", userId);

        assertEquals(4, missions.size());
        assertMissionCodes(missions, "LOGIN_DAILY", "JOB_POST_APPROVED", "REVIEW_CV", "HIRED");

        Map<String, Object> hiredMission = findMission(missions, "HIRED");
        assertEquals("Tuyển dụng thành công", hiredMission.get("name"));
        assertEquals(50, hiredMission.get("points"));
        assertEquals(5, hiredMission.get("dailyLimit"));
        assertEquals(0L, hiredMission.get("completedCount"));
        assertEquals(false, hiredMission.get("isFinished"));
    }

    @Test
    void getMissions_shouldNotQueryProgress_whenUserIdIsNull() {
        List<Map<String, Object>> missions = leaderboardService.getMissions("CANDIDATE", null);

        assertEquals(4, missions.size());
        verify(logRepo, never()).countActionsToday(anyLong(), anyString(), any(OffsetDateTime.class));
    }

    private static void assertMissionCodes(List<Map<String, Object>> missions, String... expectedCodes) {
        List<String> actualCodes = missions.stream()
                .map(mission -> (String) mission.get("code"))
                .toList();

        for (String code : expectedCodes) {
            assertTrue(actualCodes.contains(code), "Missing mission code: " + code);
        }
    }

    private static Map<String, Object> findMission(List<Map<String, Object>> missions, String code) {
        return missions.stream()
                .filter(mission -> code.equals(mission.get("code")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Mission not found: " + code));
    }

    private static String currentWeekKey() {
        LocalDate now = LocalDate.now(VN_ZONE);
        WeekFields isoWf = WeekFields.ISO;
        return String.format("%d-W%02d", now.get(isoWf.weekBasedYear()), now.get(isoWf.weekOfWeekBasedYear()));
    }

    private static String currentMonthKey() {
        return LocalDate.now(VN_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private static String currentYearKey() {
        return LocalDate.now(VN_ZONE).format(DateTimeFormatter.ofPattern("yyyy"));
    }
}
