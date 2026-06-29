package app.gamification.model;

import app.gamification.dto.request.AddPointEventRequest;
import app.gamification.event.PointEvent;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardModelTest {

    @Test
    void pointEvent_shouldStoreConstructorValues() {
        Object source = new Object();
        Long userId = 1L;
        String roleGroup = "CANDIDATE";
        UserPointAction action = UserPointAction.APPLY;
        Long refId = 100L;

        PointEvent event = new PointEvent(source, userId, roleGroup, action, refId);

        assertEquals(source, event.getSource());
        assertEquals(userId, event.getUserId());
        assertEquals(roleGroup, event.getRoleGroup());
        assertEquals(action, event.getAction());
        assertEquals(refId, event.getRefId());
    }

    @Test
    void userPointAction_shouldReturnConfiguredValues() {
        assertEquals("Đăng nhập hàng ngày", UserPointAction.LOGIN_DAILY.getDescription());
        assertEquals(5, UserPointAction.LOGIN_DAILY.getPoints());
        assertEquals(1, UserPointAction.LOGIN_DAILY.getDailyLimit());

        assertEquals("Ứng tuyển việc làm", UserPointAction.APPLY.getDescription());
        assertEquals(10, UserPointAction.APPLY.getPoints());
        assertEquals(3, UserPointAction.APPLY.getDailyLimit());

        assertEquals("Tuyển dụng thành công", UserPointAction.HIRED.getDescription());
        assertEquals(50, UserPointAction.HIRED.getPoints());
        assertEquals(5, UserPointAction.HIRED.getDailyLimit());
    }

    @Test
    void leaderboardPointsLog_shouldStoreAndReturnValues() {
        OffsetDateTime createdAt = OffsetDateTime.now();
        LeaderboardPointsLog log = new LeaderboardPointsLog();

        log.setUserId(1L);
        log.setRole("CANDIDATE");
        log.setActionType("APPLY");
        log.setPoints(10);
        log.setRefId(100L);
        log.setCreatedAt(createdAt);

        assertEquals(1L, log.getUserId());
        assertEquals("CANDIDATE", log.getRole());
        assertEquals("APPLY", log.getActionType());
        assertEquals(10, log.getPoints());
        assertEquals(100L, log.getRefId());
        assertEquals(createdAt, log.getCreatedAt());
    }

    @Test
    void leaderboardScore_shouldStoreAndReturnValues() {
        OffsetDateTime updatedAt = OffsetDateTime.now();
        LeaderboardScore score = new LeaderboardScore();

        score.setUserId(1L);
        score.setRole("RECRUITER");
        score.setPeriodType("MONTH");
        score.setPeriodKey("2026-06");
        score.setScore(100);
        score.setUpdatedAt(updatedAt);

        assertEquals(1L, score.getUserId());
        assertEquals("RECRUITER", score.getRole());
        assertEquals("MONTH", score.getPeriodType());
        assertEquals("2026-06", score.getPeriodKey());
        assertEquals(100, score.getScore());
        assertEquals(updatedAt, score.getUpdatedAt());
    }

    @Test
    void leaderboardScore_shouldHaveDefaultScoreZero() {
        LeaderboardScore score = new LeaderboardScore();

        assertEquals(0, score.getScore());
    }

    @Test
    void addPointEventRequest_shouldBeInstantiable() {
        AddPointEventRequest request = new AddPointEventRequest();

        assertNotNull(request);
    }
}
