package app.integration;

import app.ai.service.cv.gemini.GeminiService;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.content.model.Company;
import app.gamification.service.LeaderboardService;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // H2 Database
        "spring.datasource.url=jdbc:h2:mem:feature3-job-management;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.open-in-view=true",
        "spring.jpa.show-sql=false",

        // Cache
        "spring.cache.type=simple",

        // JWT
        "jwt.secret=26ee5428aaf636fc2c48b269aca43a8913d7b1e4dd08eb4c6ae6b943c4e826f708a9a488feb75fb34e80928bfa0b058ff06282c5829d4fb34e80928bfa0b058ff06282c5829d4fb5777e55a2281f2e80",
        "jwt.access-token-expiration=1800000",
        "jwt.refresh-token-expiration=36000000",

        // Google OAuth
        "google.client-id=test-client-id",
        "google.client-secret=test-client-secret",

        // Cloudinary
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret",

        // Gemini
        "gemini.api.keys=test-key",

        // Mail
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test@gmail.com",
        "spring.mail.password=test-password"
})
class JobManagementIntegrationTest {

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
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @MockBean
    private GeminiService geminiService;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private LeaderboardService leaderboardService;

    private User recruiter;
    private Company company;

    @BeforeEach
    void setUp() {
        recruiter = saveUser("feature3-recruiter@example.com", "Feature3 Recruiter", UserRole.RECRUITER);
        company = companyRepository.save(Company.builder()
                .name("Feature3 Tech")
                .description("Company for Feature 3 integration tests")
                .industry("Software")
                .email("feature3-company@example.com")
                .address("Ho Chi Minh City")
                .recruiter(recruiter)
                .build());
        when(geminiService.extractSkillsFromJob(anyString(), anyString()))
                .thenAnswer(invocation -> new ArrayList<>(List.of("Java", "Spring Boot")));
    }

    @AfterEach
    void cleanDatabase() {
        jobApplicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createJob_success() throws Exception {
        mockMvc.perform(post("/api/recruiter/jobs")
                        .header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest("Backend Developer", LocalDate.now().plusDays(30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Backend Developer"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.companyName").value(company.getName()))
                .andExpect(jsonPath("$.recruiterId").value(recruiter.getId()));
    }

    @Test
    void createJob_validationFail() throws Exception {
        mockMvc.perform(post("/api/recruiter/jobs")
                        .header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest("", LocalDate.now().plusDays(30))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJob_withoutLogin() throws Exception {
        mockMvc.perform(post("/api/recruiter/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest("Backend Developer", LocalDate.now().plusDays(30))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createJob_expiryDatePast() throws Exception {
        mockMvc.perform(post("/api/recruiter/jobs")
                        .header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest("Backend Developer", LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobById_success() throws Exception {
        JobPosting job = saveJob("Backend Developer", JobStatus.PUBLISHED);

        mockMvc.perform(get("/api/recruiter/jobs/{id}", job.getId())
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.title").value("Backend Developer"));
    }

    @Test
    void updateJob_success() throws Exception {
        JobPosting job = saveJob("Old Backend Developer", JobStatus.PENDING);

        mockMvc.perform(put("/api/recruiter/jobs/{id}", job.getId())
                        .header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest("Updated Backend Developer", LocalDate.now().plusDays(45))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.title").value("Updated Backend Developer"));
    }

    @Test
    void deleteJob_success() throws Exception {
        JobPosting job = saveJob("Backend Developer", JobStatus.PENDING);

        mockMvc.perform(delete("/api/recruiter/jobs/{id}", job.getId())
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recruiter/jobs/me")
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listMyJobs_success() throws Exception {
        saveJob("Backend Developer", JobStatus.PENDING);
        saveJob("Frontend Developer", JobStatus.PUBLISHED);

        mockMvc.perform(get("/api/recruiter/jobs/me")
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].recruiterId").value(recruiter.getId()));
    }

    @Test
    void searchJob_success() throws Exception {
        saveJob("Java Backend Developer", JobStatus.PUBLISHED);
        saveJob("Mobile Engineer", JobStatus.PUBLISHED);
        saveJob("Draft Java Developer", JobStatus.PENDING);

        mockMvc.perform(get("/api/recruiter/jobs/search")
                        .param("keyword", "Java")
                        .header("Authorization", bearer(recruiter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Java Backend Developer"));
    }

    @Test
    void publicJobDetail_success() throws Exception {
        JobPosting job = saveJob("Public Backend Developer", JobStatus.PUBLISHED);

        mockMvc.perform(get("/api/recruiter/jobs/public/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.title").value("Public Backend Developer"))
                .andExpect(jsonPath("$.companyName").value(company.getName()));
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

    private JobPosting saveJob(String title, JobStatus status) {
        return jobPostingRepository.save(JobPosting.builder()
                .title(title)
                .description("Build and maintain Spring Boot services")
                .requirements("Java, Spring Boot, SQL")
                .salaryRange("1500-2500")
                .location("Ho Chi Minh City")
                .expiryDate(LocalDateTime.now().plusDays(30))
                .extractedSkills(new ArrayList<>(List.of("Java", "Spring Boot")))
                .status(status)
                .recruiter(recruiter)
                .company(company)
                .build());
    }

    private String jobRequest(String title, LocalDate expiryDate) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", "Build and maintain Spring Boot services",
                "requirements", "Java, Spring Boot, SQL",
                "salaryRange", "1500-2500",
                "location", "Ho Chi Minh City",
                "expiryDate", expiryDate.toString()
        ));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getEmail());
    }
}
