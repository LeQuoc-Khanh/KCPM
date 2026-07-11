package app.integration;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feature7-payment;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void paymentEndpoint_shouldUpgradeCandidateToVipAndPersistExpirationDate() throws Exception {
        User candidate = saveUser("feature7-payment-candidate@example.com", "Feature7 Payment Candidate",
                UserRole.CANDIDATE);

        LocalDateTime beforeExpectedExpiration = LocalDateTime.now().plusDays(30).minusSeconds(2);

        mockMvc.perform(post("/api/payment/vip-upgrade")
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(candidate.getId()))
                .andExpect(jsonPath("$.user.email").value(candidate.getEmail()))
                .andExpect(jsonPath("$.user.userRole").value(UserRole.CANDIDATE_VIP.name()));

        User upgraded = userRepository.findById(candidate.getId()).orElseThrow();
        assertThat(upgraded.getUserRole()).isEqualTo(UserRole.CANDIDATE_VIP);
        assertThat(upgraded.getVipExpirationDate())
                .isAfterOrEqualTo(beforeExpectedExpiration)
                .isBeforeOrEqualTo(LocalDateTime.now().plusDays(30).plusSeconds(2));
    }

    @Test
    void paymentEndpoint_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payment/vip-upgrade"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentEndpoint_adminUser_shouldReturnBadRequestAndNotChangeRole() throws Exception {
        User admin = saveUser("feature7-payment-admin@example.com", "Feature7 Payment Admin",
                UserRole.ADMIN);

        mockMvc.perform(post("/api/payment/vip-upgrade")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());

        User unchanged = userRepository.findById(admin.getId()).orElseThrow();
        assertThat(unchanged.getUserRole()).isEqualTo(UserRole.ADMIN);
        assertThat(unchanged.getVipExpirationDate()).isNull();
    }

    @Test
    void paymentEndpoint_shouldUpgradeRecruiterToVipAndPersistExpirationDate() throws Exception {
        User recruiter = saveUser("feature7-payment-recruiter@example.com", "Feature7 Payment Recruiter",
                UserRole.RECRUITER);

        LocalDateTime beforeExpectedExpiration = LocalDateTime.now().plusDays(30).minusSeconds(2);

        mockMvc.perform(post("/api/payment/vip-upgrade")
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(recruiter.getId()))
                .andExpect(jsonPath("$.user.email").value(recruiter.getEmail()))
                .andExpect(jsonPath("$.user.userRole").value(UserRole.RECRUITER_VIP.name()));

        User upgraded = userRepository.findById(recruiter.getId()).orElseThrow();
        assertThat(upgraded.getUserRole()).isEqualTo(UserRole.RECRUITER_VIP);
        assertThat(upgraded.getVipExpirationDate())
                .isAfterOrEqualTo(beforeExpectedExpiration)
                .isBeforeOrEqualTo(LocalDateTime.now().plusDays(30).plusSeconds(2));
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
}
