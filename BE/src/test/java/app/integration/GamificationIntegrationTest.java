package app.integration;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.gamification.model.LeaderboardPointsLog;
import app.gamification.model.LeaderboardScore;
import app.gamification.model.UserPointAction;
import app.gamification.repository.LeaderboardPointsLogRepository;
import app.gamification.repository.LeaderboardScoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feature7-gamification;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.cache.type=simple",
        "jwt.secret=26ee5428aaf636fc2c48b269aca43a8913d7b1e4dd08eb4c6ae6b943c4e826f708a9a488feb75fb34e80928bfa0b058ff06282c5829d4fb5777e55a2281f2e80",
        "jwt.access-token-expiration=1800000",
        "jwt.refresh-token-expiration=36000000",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret",
        "gemini.api.keys=test-key",
        "spring.mail.host=localhost",
        "spring.mail.port=2525"
})
class GamificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaderboardScoreRepository leaderboardScoreRepository;

    @Autowired
    private LeaderboardPointsLogRepository leaderboardPointsLogRepository;

    @AfterEach
    void cleanDatabase() {
        leaderboardPointsLogRepository.deleteAll();
        leaderboardScoreRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void gamificationEndpoints_shouldReturnPersistedScoresRanksMissionsAndLogs() throws Exception {
        User topCandidate = saveUser("feature7-top-candidate@example.com", "Feature7 Top Candidate",
                UserRole.CANDIDATE);
        User secondCandidate = saveUser("feature7-second-candidate@example.com", "Feature7 Second Candidate",
                UserRole.CANDIDATE);

        leaderboardScoreRepository.save(score(topCandidate.getId(), "CANDIDATE", "ALL_TIME", "ALL", 120));
        leaderboardScoreRepository.save(score(secondCandidate.getId(), "CANDIDATE", "ALL_TIME", "ALL", 75));
        leaderboardPointsLogRepository.save(pointsLog(secondCandidate.getId(), "CANDIDATE",
                UserPointAction.LOGIN_DAILY, 5, 501L));

        mockMvc.perform(get("/api/leaderboard")
                        .param("role", "CANDIDATE")
                        .param("period", "ALL_TIME")
                        .param("limit", "2")
                        .header("Authorization", bearer(secondCandidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].userId").value(topCandidate.getId()))
                .andExpect(jsonPath("$.data[0].fullName").value("Feature7 Top Candidate"))
                .andExpect(jsonPath("$.data[0].score").value(120))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[1].userId").value(secondCandidate.getId()))
                .andExpect(jsonPath("$.data[1].rank").value(2));

        mockMvc.perform(get("/api/leaderboard/me")
                        .param("userId", secondCandidate.getId().toString())
                        .param("role", "CANDIDATE")
                        .param("period", "ALL_TIME")
                        .header("Authorization", bearer(secondCandidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(secondCandidate.getId()))
                .andExpect(jsonPath("$.data.fullName").value("Feature7 Second Candidate"))
                .andExpect(jsonPath("$.data.score").value(75))
                .andExpect(jsonPath("$.data.rank").value(2));

        mockMvc.perform(get("/api/leaderboard/missions")
                        .param("role", "CANDIDATE")
                        .param("userId", secondCandidate.getId().toString())
                        .header("Authorization", bearer(secondCandidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.code == 'LOGIN_DAILY')].completedCount").value(1))
                .andExpect(jsonPath("$.data[?(@.code == 'LOGIN_DAILY')].isFinished").value(true));

        mockMvc.perform(get("/api/leaderboard/logs")
                        .param("limit", "1")
                        .header("Authorization", bearer(secondCandidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId").value(secondCandidate.getId()))
                .andExpect(jsonPath("$.data[0].actionType").value(UserPointAction.LOGIN_DAILY.name()))
                .andExpect(jsonPath("$.data[0].points").value(5));
    }

    private User saveUser(String email, String fullName, UserRole role) {
        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password("{noop}password")
                .userRole(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .profileImageUrl("https://example.com/" + email + ".png")
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getEmail());
    }

    private LeaderboardScore score(Long userId, String role, String periodType, String periodKey, int points) {
        LeaderboardScore score = new LeaderboardScore();
        score.setUserId(userId);
        score.setRole(role);
        score.setPeriodType(periodType);
        score.setPeriodKey(periodKey);
        score.setScore(points);
        return score;
    }

    private LeaderboardPointsLog pointsLog(Long userId, String role, UserPointAction action, int points, Long refId) {
        LeaderboardPointsLog log = new LeaderboardPointsLog();
        log.setUserId(userId);
        log.setRole(role);
        log.setActionType(action.name());
        log.setPoints(points);
        log.setRefId(refId);
        log.setCreatedAt(OffsetDateTime.now());
        return log;
    }
}
