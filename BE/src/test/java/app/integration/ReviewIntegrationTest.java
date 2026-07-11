package app.integration;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.content.model.Company;
import app.notification.repository.NotificationRepository;
import app.review.repository.CompanyReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feature7-review;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CompanyReviewRepository reviewRepository;

    @AfterEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void reviewEndpoints_shouldCreateReviewReadAverageAndNotifyRecruiter() throws Exception {
        User candidate = saveUser("feature7-candidate-review@example.com", "Feature7 Reviewer",
                UserRole.CANDIDATE);
        User recruiter = saveUser("feature7-recruiter-review@example.com", "Feature7 Recruiter",
                UserRole.RECRUITER);
        Company company = companyRepository.save(Company.builder()
                .name("Feature7 Review Company")
                .description("Company used by Feature 7 review integration tests")
                .industry("Software")
                .email("feature7-company@example.com")
                .recruiter(recruiter)
                .build());

        String requestBody = objectMapper.writeValueAsString(new ReviewPayload(
                company.getId(),
                5,
                "Excellent hiring process"
        ));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent hiring process"))
                .andExpect(jsonPath("$.reviewerName").value("Feature7 Reviewer"));

        assertThat(reviewRepository.existsByUserIdAndCompanyId(candidate.getId(), company.getId())).isTrue();

        mockMvc.perform(get("/api/reviews/company/{companyId}", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].comment").value("Excellent hiring process"));

        mockMvc.perform(get("/api/reviews/company/{companyId}/average", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5.0));

        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recruiter.getId()))
                .hasSize(1)
                .first()
                .satisfies(notification -> {
                    assertThat(notification.getTitle()).contains("5");
                    assertThat(notification.getMessage()).contains("Feature7 Reviewer");
                    assertThat(notification.getLink()).isEqualTo("/recruiter/company/reviews");
                });
    }

    @Test
    void createReview_withoutToken_shouldReturnUnauthorized() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new ReviewPayload(
                1L,
                5,
                "Unauthorized review"
        ));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReview_sameUserAndCompanyTwice_shouldKeepSingleReviewAndReturnServerError() throws Exception {
        User candidate = saveUser("feature7-duplicate-candidate-review@example.com", "Feature7 Duplicate Reviewer",
                UserRole.CANDIDATE);
        User recruiter = saveUser("feature7-duplicate-recruiter-review@example.com", "Feature7 Duplicate Recruiter",
                UserRole.RECRUITER);
        Company company = companyRepository.save(Company.builder()
                .name("Feature7 Duplicate Review Company")
                .description("Company used by duplicate review integration test")
                .industry("Software")
                .email("feature7-duplicate-company@example.com")
                .recruiter(recruiter)
                .build());

        String requestBody = objectMapper.writeValueAsString(new ReviewPayload(
                company.getId(),
                4,
                "First review"
        ));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(candidate))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(reviewRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId()))
                .hasSize(1);
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

    private record ReviewPayload(Long companyId, Integer rating, String comment) {
    }
}
