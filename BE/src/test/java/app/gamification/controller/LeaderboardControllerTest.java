package app.gamification.controller;

import app.gamification.dto.response.LeaderboardEntryResponse;
import app.gamification.dto.response.LeaderboardLogResponse;
import app.gamification.dto.response.LeaderboardMeResponse;
import app.gamification.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardController leaderboardController;

    @Test
    void getTop_shouldReturnSuccessAndData() {
        List<LeaderboardEntryResponse> rankings = List.of(mock(LeaderboardEntryResponse.class));
        when(leaderboardService.getTopUsers("CANDIDATE", "WEEK", "2026-W26", 20))
                .thenReturn(rankings);

        ResponseEntity<?> response = leaderboardController.getTop("CANDIDATE", "WEEK", "2026-W26", 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(rankings, body.get("data"));
        verify(leaderboardService).getTopUsers("CANDIDATE", "WEEK", "2026-W26", 20);
    }

    @Test
    void getMe_shouldReturnSuccessAndRankData_whenRankExists() {
        Long userId = 1L;
        LeaderboardMeResponse myRank = mock(LeaderboardMeResponse.class);
        when(leaderboardService.getMyRank(userId, "CANDIDATE", "WEEK", "2026-W26"))
                .thenReturn(myRank);

        ResponseEntity<?> response = leaderboardController.getMe(userId, "CANDIDATE", "WEEK", "2026-W26");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(myRank, body.get("data"));
    }

    @Test
    void getMe_shouldReturnEmptyMap_whenRankDoesNotExist() {
        Long userId = 1L;
        when(leaderboardService.getMyRank(userId, "CANDIDATE", "WEEK", null))
                .thenReturn(null);

        ResponseEntity<?> response = leaderboardController.getMe(userId, "CANDIDATE", "WEEK", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(Map.of(), body.get("data"));
    }

    @Test
    void getMissions_shouldReturnSuccessAndMissionData() {
        Long userId = 1L;
        List<Map<String, Object>> missions = List.of(
                Map.of("code", "LOGIN_DAILY", "points", 5)
        );
        when(leaderboardService.getMissions("CANDIDATE", userId)).thenReturn(missions);

        ResponseEntity<?> response = leaderboardController.getMissions("CANDIDATE", userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(missions, body.get("data"));
        verify(leaderboardService).getMissions("CANDIDATE", userId);
    }

    @Test
    void getLogs_shouldReturnSuccessAndLogData() {
        List<LeaderboardLogResponse> logs = List.of(mock(LeaderboardLogResponse.class));
        when(leaderboardService.getSystemLogs(10)).thenReturn(logs);

        ResponseEntity<?> response = leaderboardController.getLogs(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(logs, body.get("data"));
        verify(leaderboardService).getSystemLogs(10);
    }
}
