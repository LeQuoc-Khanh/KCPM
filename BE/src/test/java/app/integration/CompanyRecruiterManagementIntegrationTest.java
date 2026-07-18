package app.integration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.ai.models.InterviewSession;
import app.ai.service.InterviewService;
import app.ai.service.JobFastMatchingService;
import app.ai.service.JobMatchingService;
import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.ai.service.cv.gemini.dto.MatchResult;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.service.CandidateService;
import app.content.model.Company;
import app.gamification.service.LeaderboardService;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feature5;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
        "GOOGLE_CLIENT_ID=test-google-client",
        "spring.mail.host=localhost",
        "spring.mail.port=2525"
})
class CompanyRecruiterManagementIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final Long JOB_ID = 501L;
    private static final Long SESSION_ID = 901L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JobMatchingService matchingService;

    @MockBean
    private JobFastMatchingService fastMatchingService;

    @MockBean
    private CVAnalysisService cvAnalysisService;

    @MockBean
    private CandidateService candidateService;

    @MockBean
    private InterviewService interviewService;

    @MockBean
    private LeaderboardService leaderboardService;

    private User admin;
    private User recruiter;
    private User candidate;
    private JobPosting job;

    @BeforeEach
    void setUp() {
        admin = saveUser(
                "feature5-admin@example.com",
                "Feature 5 Admin",
                UserRole.ADMIN
        );

        recruiter = saveUser(
                "feature5-recruiter@example.com",
                "Feature 5 Recruiter",
                UserRole.RECRUITER
        );

        candidate = saveUser(
                "feature5-candidate@example.com",
                "Feature 5 Candidate",
                UserRole.CANDIDATE_VIP
        );

        Company company = Company.builder()
                .id(71L)
                .name("Feature 5 Company")
                .recruiter(recruiter)
                .build();

        job = JobPosting.builder()
                .id(JOB_ID)
                .title("Senior Java Engineer")
                .description("Build Spring services")
                .requirements("Java, Spring")
                .recruiter(recruiter)
                .company(company)
                .build();
    }

    @Test
    @Transactional
    @DisplayName("TC_5.1 - Login Admin")
    void tc_5_1_loginAdmin() throws Exception {
        login(admin);
    }

    @Test
    @Transactional
    @DisplayName("TC_5.2 - Get All Users")
    void tc_5_2_getAllUsers() throws Exception {
        String token = login(admin);
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.3 - Login Recruiter")
    void tc_5_3_loginRecruiter() throws Exception {
        login(recruiter);
    }

    @Test
    @Transactional
    @DisplayName("TC_5.4 - Recruiter Run AI Screening")
    void tc_5_4_recruiterRunAiScreening() throws Exception {
        String recruiterToken = login(recruiter);
        mvc.perform(post("/api/matching/recruiter/screen/{jobId}", JOB_ID)
                        .header("Authorization", bearer(recruiterToken)))
                .andExpect(status().isOk());
        verify(matchingService).screenApplications(JOB_ID);
    }

    @Test
    @Transactional
    @DisplayName("TC_5.5 - Get Ranked Applications")
    void tc_5_5_getRankedApplications() throws Exception {
        String recruiterToken = login(recruiter);
        JobApplication application = JobApplication.builder().id(801L).jobPosting(job)
                .candidate(candidate).cvUrl("https://example.com/cv.pdf").matchScore(92).build();
        when(matchingService.getRankedApplications(JOB_ID, 70)).thenReturn(List.of(application));

        mvc.perform(get("/api/matching/recruiter/ranking/{jobId}", JOB_ID)
                        .param("minScore", "70")
                        .header("Authorization", bearer(recruiterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName").value(candidate.getFullName()));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.6 - Login Candidate")
    void tc_5_6_loginCandidate() throws Exception {
        login(candidate);
    }

    @Test
    @Transactional
    @DisplayName("TC_5.7 - Candidate Batch Job Scores")
    void tc_5_7_candidateBatchJobScores() throws Exception {
        String token = login(candidate);
        CandidateProfile profile = candidateProfile();
        when(candidateService.getProfileForMatching(candidate.getId())).thenReturn(profile);
        when(fastMatchingService.calculateBatchCompatibility(profile.getSkills(), List.of(JOB_ID)))
                .thenReturn(Map.of(JOB_ID,
                        new FastMatchResult(88, List.of("Java"), List.of("Docker"))));

        mvc.perform(post("/api/matching/candidate/batch-scores")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("[501]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.501.matchScore").value(88));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.8 - Candidate Preview Job Match")
    void tc_5_8_candidatePreviewJobMatch() throws Exception {
        String token = login(candidate);
        CandidateProfile profile = candidateProfile();
        when(candidateService.getProfile(candidate.getId())).thenReturn(profile);
        when(cvAnalysisService.getTextFromUrl(profile.getCvFilePath())).thenReturn("Java Spring CV");
        when(matchingService.matchCandidateWithJobAI(candidate.getId(), "Java Spring CV", JOB_ID,
                profile.getCvFilePath())).thenReturn(MatchResult.builder().matchPercentage(88).build());

        mvc.perform(get("/api/matching/candidate/preview/{jobId}", JOB_ID)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(88));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.9 - Start AI Interview")
    void tc_5_9_startAiInterview() throws Exception {
        String token = login(candidate);
        when(interviewService.startInterview(candidate.getId(), JOB_ID))
                .thenReturn(createSession("ONGOING", null, null));
        when(interviewService.getInitialGreeting(candidate.getId(), JOB_ID)).thenReturn("Welcome");

        mvc.perform(post("/api/interview/start").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jobId\":501}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.10 - Interview Chat")
    void tc_5_10_interviewChat() throws Exception {
        String token = login(candidate);
        when(interviewService.chat(eq(SESSION_ID), eq("I build resilient APIs"), anyList()))
                .thenReturn("How do you test them?");

        mvc.perform(post("/api/interview/{sessionId}/chat", SESSION_ID)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"I build resilient APIs\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("How do you test them?"));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.11 - End Interview")
    void tc_5_11_endInterview() throws Exception {
        String token = login(candidate);
        when(interviewService.endInterview(eq(SESSION_ID), anyList()))
                .thenReturn(createSession("COMPLETED", 85, "Strong technical answers"));

        mvc.perform(post("/api/interview/{sessionId}/end", SESSION_ID)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"history\":[{\"sender\":\"USER\",\"content\":\"Answer\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.score").value(85));
    }

    @Test
    @Transactional
    @DisplayName("TC_5.12 - Interview History")
    void tc_5_12_interviewHistory() throws Exception {
        String token = login(candidate);
        when(interviewService.getCompletedHistory(JOB_ID, candidate.getId()))
                .thenReturn(List.of(createSession("COMPLETED", 85, "Strong technical answers")));

        mvc.perform(get("/api/interview/history").param("jobId", JOB_ID.toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].feedback").value("Strong technical answers"));
    }

    private void completeFeature5Workflow() throws Exception {
        // TC_5.1 Login Admin
        String adminToken = login(admin);

        // TC_5.2 Get All Users
        mvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // TC_5.3 Login Recruiter
        login(recruiter);

        // TC_5.4 Recruiter Run AI Screening
        mvc.perform(post(
                        "/api/matching/recruiter/screen/{jobId}",
                        JOB_ID
                ).with(user(recruiter.getEmail()).roles("RECRUITER")))
                .andExpect(status().isOk());

        verify(matchingService).screenApplications(JOB_ID);

        // TC_5.5 Get Ranked Applications
        JobApplication rankedApplication = JobApplication.builder()
                .id(801L)
                .jobPosting(job)
                .candidate(candidate)
                .cvUrl("https://example.com/cv.pdf")
                .matchScore(92)
                .build();

        when(matchingService.getRankedApplications(JOB_ID, 70))
                .thenReturn(List.of(rankedApplication));

        mvc.perform(get(
                        "/api/matching/recruiter/ranking/{jobId}",
                        JOB_ID
                )
                        .param("minScore", "70")
                        .with(user(recruiter.getEmail()).roles("RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName")
                        .value(candidate.getFullName()));

        // TC_5.6 Login Candidate
        String candidateToken = login(candidate);

        CandidateProfile profile = CandidateProfile.builder()
                .user(candidate)
                .fullName(candidate.getFullName())
                .cvFilePath("https://example.com/cv.pdf")
                .skills(List.of("Java", "Spring"))
                .build();

        when(candidateService.getProfileForMatching(candidate.getId()))
                .thenReturn(profile);

        when(candidateService.getProfile(candidate.getId()))
                .thenReturn(profile);

        // TC_5.7 Candidate Batch Job Scores
        when(fastMatchingService.calculateBatchCompatibility(
                List.of("Java", "Spring"),
                List.of(JOB_ID)
        )).thenReturn(Map.of(
                JOB_ID,
                new FastMatchResult(
                        88,
                        List.of("Java"),
                        List.of("Docker")
                )
        ));

        mvc.perform(post("/api/matching/candidate/batch-scores")
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[501]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.501.matchScore").value(88));

        // TC_5.8 Candidate Preview Job Match
        MatchResult preview = MatchResult.builder()
                .matchPercentage(88)
                .jobTitle(job.getTitle())
                .company("Feature 5 Company")
                .candidateName(candidate.getFullName())
                .build();

        when(cvAnalysisService.getTextFromUrl(
                profile.getCvFilePath()
        )).thenReturn("Java Spring CV");

        when(matchingService.matchCandidateWithJobAI(
                candidate.getId(),
                "Java Spring CV",
                JOB_ID,
                profile.getCvFilePath()
        )).thenReturn(preview);

        mvc.perform(get(
                        "/api/matching/candidate/preview/{jobId}",
                        JOB_ID
                )
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercentage").value(88));

        InterviewSession ongoing = createSession(
                "ONGOING",
                null,
                null
        );

        InterviewSession completed = createSession(
                "COMPLETED",
                85,
                "Strong technical answers"
        );

        // TC_5.9 Start AI Interview
        when(interviewService.startInterview(
                candidate.getId(),
                JOB_ID
        )).thenReturn(ongoing);

        when(interviewService.getInitialGreeting(
                candidate.getId(),
                JOB_ID
        )).thenReturn("Welcome");

        mvc.perform(post("/api/interview/start")
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":501}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId")
                        .value(SESSION_ID));

        // TC_5.10 Interview Chat
        when(interviewService.chat(
                eq(SESSION_ID),
                eq("I build resilient APIs"),
                anyList()
        )).thenReturn("How do you test them?");

        mvc.perform(post(
                        "/api/interview/{sessionId}/chat",
                        SESSION_ID
                )
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "I build resilient APIs",
                                  "history": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data")
                        .value("How do you test them?"));

        // TC_5.11 End Interview
        when(interviewService.endInterview(
                eq(SESSION_ID),
                anyList()
        )).thenReturn(completed);

        mvc.perform(post(
                        "/api/interview/{sessionId}/end",
                        SESSION_ID
                )
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "history": [
                                    {
                                      "sender": "USER",
                                      "content": "Answer"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data.score")
                        .value(85));

        // TC_5.12 Interview History
        when(interviewService.getCompletedHistory(
                JOB_ID,
                candidate.getId()
        )).thenReturn(List.of(completed));

        mvc.perform(get("/api/interview/history")
                        .param("jobId", JOB_ID.toString())
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].feedback")
                        .value("Strong technical answers"));
    }

    private String login(User user) throws Exception {
        String responseBody = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.userRole")
                        .value(user.getUserRole().name()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        return response
                .get("data")
                .get("accessToken")
                .asText();
    }

    private User saveUser(
            String email,
            String name,
            UserRole role
    ) {
        return userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .userRole(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build());
    }

    private CandidateProfile candidateProfile() {
        return CandidateProfile.builder()
                .user(candidate)
                .fullName(candidate.getFullName())
                .cvFilePath("https://example.com/cv.pdf")
                .skills(List.of("Java", "Spring"))
                .build();
    }

    private InterviewSession createSession(
            String status,
            Integer score,
            String feedback
    ) {
        return InterviewSession.builder()
                .id(SESSION_ID)
                .user(candidate)
                .jobPosting(job)
                .status(status)
                .finalScore(score)
                .feedback(feedback)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
