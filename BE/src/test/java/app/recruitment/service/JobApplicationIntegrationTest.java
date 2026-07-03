package app.recruitment.service;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.AuthProvider;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.auth.repository.CompanyRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.entity.CVAnalysisResult;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobPostingRepository;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "JWT_SECRET=day-la-mot-chuoi-bi-mat-gia-lap-danh-cho-moi-truong-test-phai-du-dai",
    "JWT_EXPIRATION=86400000",
    "GEMINI_API_KEYS=gia-lap-key-gemini-cho-moi-truong-test",
    "CLOUDINARY_URL=cloudinary://12345:abcde@test",
    "CLOUDINARY_CLOUD_NAME=mock-cloud-name",
    "CLOUDINARY_API_KEY=mock-api-key",
    "CLOUDINARY_API_SECRET=mock-api-secret",
    "GOOGLE_CLIENT_ID=mock-google-client-id",
    "GOOGLE_CLIENT_SECRET=mock-google-client-secret",
    "spring.mail.host=localhost",
    "spring.mail.port=3025" 
})
@AutoConfigureMockMvc 
@Transactional
class JobApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private CVAnalysisResultRepository cvAnalysisResultRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    private User savedCandidate;
    private JobPosting savedJob;

    @BeforeEach
    void setUpData() {
        // 1. Xóa sạch dữ liệu theo thứ tự để tránh lỗi ràng buộc khóa ngoại (FK)
        cvAnalysisResultRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        candidateProfileRepository.deleteAll();
        jobPostingRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        // 2. Tạo Recruiter
        User recruiter = new User();
        recruiter.setEmail("recruiter@company.com");
        recruiter.setFullName("Recruiter Name");
        recruiter.setPassword("dummy-password");
        recruiter.setAuthProvider(AuthProvider.LOCAL);
        recruiter.setUserRole(UserRole.RECRUITER);
        User savedRecruiter = userRepository.save(recruiter);

        // 3. Tạo Company
        Company company = new Company();
        company.setName("Tech Corp");
        company.setRecruiter(savedRecruiter);
        Company savedCompany = companyRepository.save(company);

        // 4. Tạo Job
        JobPosting job = new JobPosting();
        job.setTitle("Senior Java Engineer");
        job.setDescription("Spring Boot integration testing");
        job.setRequirements("Experienced");
        job.setLocation("Hồ Chí Minh");
        job.setStatus(JobStatus.PENDING);
        job.setExpiryDate(LocalDateTime.now().plusDays(15));
        job.setRecruiter(savedRecruiter);
        job.setCompany(savedCompany);
        savedJob = jobPostingRepository.save(job);

        // 5. Tạo Candidate
        User candidate = new User();
        candidate.setEmail("candidate@test.com");
        candidate.setFullName("Candidate Name");
        candidate.setPassword("dummy-password");
        candidate.setAuthProvider(AuthProvider.LOCAL);
        candidate.setUserRole(UserRole.CANDIDATE);
        savedCandidate = userRepository.save(candidate);

        // 5.5. MỒI HỒ SƠ ỨNG VIÊN (CANDIDATE PROFILE) ĐỂ TRÁNH LỖI 400 KHI KHÔNG TRUYỀN CV_URL
        CandidateProfile profile = new CandidateProfile();
        profile.setUser(savedCandidate); // Nếu Entity dùng Long userId, hãy đổi thành: profile.setUserId(savedCandidate.getId());
        profile.setCvFilePath("https://cloudinary.com/my-cv-profile.pdf");
        profile.setSkills(List.of("Java", "Spring Boot"));
        profile.setPhoneNumber("0987654321");
        profile.setEmail("candidate@test.com");
        candidateProfileRepository.save(profile);

        // 6. MỒI DỮ LIỆU CV ĐÃ ĐƯỢC PHÂN TÍCH
        CVAnalysisResult mockAnalysis = CVAnalysisResult.builder()
                .user(savedCandidate)
                .jobPosting(savedJob)
                .matchPercentage(85)
                .analysisDetails("{\"skills\": [\"Java\", \"Spring Boot\"], \"evaluation\": \"Good\"}")
                .cvUrlUsed("https://cloudinary.com/my-cv-profile.pdf")
                .build();
                
        cvAnalysisResultRepository.save(mockAnalysis);
    }

    // ================= IT_4.1: APPLY WITH NEW CV (JSON) =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_1_ApplyJob_WithCV_Success() throws Exception {
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + ", \"cvUrl\": \"https://cloudinary.com/new-cv.pdf\"}";
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is2xxSuccessful());
    }

    // ================= IT_4.2: APPLY WITH EXISTING PROFILE CV =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_2_ApplyJob_ExistingCV_Success() throws Exception {
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + "}";
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is2xxSuccessful());
    }

    // ================= IT_4.3: APPLY FOR NON-EXISTENT JOB =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_3_ApplyJob_JobNotFound_Failed() throws Exception {
        String jsonPayload = "{\"jobId\": 999999}";
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }
    
    // ================= IT_4.4: DUPLICATE APPLICATION =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_4_ApplyJob_Duplicate_Failed() throws Exception {
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + "}";

        // Lần 1: Thành công
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload));

        // Lần 2: Báo lỗi 4xx
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is4xxClientError()); 
    }

    // ================= IT_4.5: INVALID JSON PAYLOAD (BAD REQUEST) =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_5_ApplyJob_InvalidPayload_Failed() throws Exception {
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // ================= IT_4.6: UNAUTHORIZED (NO TOKEN) =================
    @Test
    void test_IT_4_6_ApplyJob_Unauthorized_Failed() throws Exception {
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + "}";

        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    // ================= IT_4.7: DATABASE INTEGRITY CHECK =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_7_DatabaseIntegrity_AfterApply() throws Exception {
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + "}";

        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is2xxSuccessful());
        
        long count = jobApplicationRepository.count();
        Assertions.assertTrue(count > 0, "Phải có ít nhất 1 đơn ứng tuyển được lưu vào DB");
    }

    // ================= IT_4.8: EVENT PUBLISHING (MOCK) =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_8_EventPublishing_AfterApply() throws Exception {
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + "}";

        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is2xxSuccessful());
    }

    // ================= IT_4.9: MISSING JOB ID (VALIDATION FAILED) =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_9_ApplyJob_MissingJobId_Failed() throws Exception {
        String jsonPayload = "{\"jobId\": null}";

        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is4xxClientError());
    }

    // ================= IT_4.10: APPLY FOR DELETED/CLOSED JOB =================
    @Test
    @WithMockUser(username = "candidate@test.com", roles = "CANDIDATE")
    void test_IT_4_10_ApplyJob_DeletedStatus_Failed() throws Exception {
        // 1. Cố tình sửa trạng thái của Job trong Database thành DELETED
        savedJob.setStatus(JobStatus.DELETED); 
        jobPostingRepository.save(savedJob);

        // 2. Tạo payload ứng tuyển vào chính cái Job vừa bị xóa
        String jsonPayload = "{\"jobId\": " + savedJob.getId() + "}";

        // 3. Thực hiện Request và KỲ VỌNG hệ thống phải báo lỗi 4xx (Bad Request)
        mockMvc.perform(post("/api/applications/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().is4xxClientError()); 
    }
}