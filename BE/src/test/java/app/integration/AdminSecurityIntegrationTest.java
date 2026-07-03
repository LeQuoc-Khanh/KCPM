package app.integration;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.RefreshTokenRepository;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feature6-admin-security;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
        "google.client-id=test-google-client-id",
        "spring.mail.host=localhost",
        "spring.mail.port=2525"
})
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void tc_6_int_1_loginAdmin_shouldReturnAccessToken() throws Exception {
        saveUser("admin@test.com", "Admin Test", UserRole.ADMIN, "AdminTest123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "admin@test.com",
                                "password", "AdminTest123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.userRole").value("ADMIN"));
    }

    @Test
    void tc_6_int_2_adminCanAccessUsersApi() throws Exception {
        User admin = saveUser("admin@test.com", "Admin Test", UserRole.ADMIN, "AdminTest123");
        saveUser("candidate@test.com", "Candidate Test", UserRole.CANDIDATE, "CandidateTest123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void tc_6_int_3_noTokenCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc_6_int_4_candidateCannotAccessAdminApi() throws Exception {
        User candidate = saveUser("candidate@test.com", "Candidate Test", UserRole.CANDIDATE, "CandidateTest123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc_6_int_5_recruiterCannotAccessAdminApi() throws Exception {
        User recruiter = saveUser("recruiter@test.com", "Recruiter Test", UserRole.RECRUITER, "RecruiterTest123");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String email, String fullName, UserRole role, String rawPassword) {
        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .userRole(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getEmail());
    }
}
