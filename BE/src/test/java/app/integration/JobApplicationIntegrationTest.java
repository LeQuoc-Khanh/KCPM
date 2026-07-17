package app.integration;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.dto.request.JobApplicationRequest;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.ApplicationStatus;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import app.recruitment.service.JobApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class JobApplicationIntegrationTest {
    @Autowired
    private JobApplicationService jobApplicationService;
    @Autowired
    private JobApplicationRepository jobApplicationRepository;
    @Autowired
    private JobPostingRepository jobPostingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CandidateProfileRepository profileRepository;
    private Long candidateId;
    private Long jobId;
    @BeforeEach
    void setupRealDataInH2() {
        // 1. Khởi tạo User Ứng viên
        User candidate = User.builder()
                .fullName("Phan Khanh Du Integration")
                .email("dupk.integration@ut.edu.vn")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
        candidate.setPassword("encodedPassword"); // Dùng setter để ép kiểu
        candidate = userRepository.save(candidate);
        candidateId = candidate.getId();

        // 2. Tạo Hồ sơ ứng viên
        CandidateProfile profile = CandidateProfile.builder()
                .user(candidate)
                .fullName("Phan Khanh Du Integration")
                .cvFilePath("https://cloudinary.com/real-cv-file.pdf")
                .skills(List.of("Java", "Spring Boot", "SQL"))
                .build();
        profileRepository.save(profile);

        // 3. Khởi tạo Nhà tuyển dụng
        User recruiter = User.builder()
                .fullName("Recruiter Real Corp")
                .email("recruiter.corp@test.com")
                .userRole(UserRole.RECRUITER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
        recruiter.setPassword("encodedPassword"); // Dùng setter để ép kiểu
        recruiter = userRepository.save(recruiter);

        // 4. Lưu bài đăng công việc
        JobPosting job = JobPosting.builder()
                .title("Senior Backend Engineer")
                .description("Yêu cầu ứng viên thành thạo Spring Boot Framework")
                .status(JobStatus.PUBLISHED)
                .recruiter(recruiter)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .build();
        job = jobPostingRepository.save(job);
        jobId = job.getId();
    }
    /**
    * * INT-F4-001: Kiểm thử tích hợp toàn bộ chu trình Ứng tuyển thực tế vào DB H2
    */
    @Test
    void testApplyWorkflow_Integration_ShouldSaveToDatabaseReal() {
        // Arrange: Khởi tạo DTO request thật không đính kèm link CV (Ép hệ thống tựcấu hình fallback tìm từ Profile)
        JobApplicationRequest request = new JobApplicationRequest();
        request.setJobId(jobId);
        request.setCoverLetter("Tôi rất mong muốn được cống hiến cho công ty.");
        // Act: Gọi trực tiếp Service thật, kích hoạt lưu xuống Database thật
        JobApplication savedApp = jobApplicationService.apply(candidateId, request);
        // Assert: Kiểm chứng tính toàn vẹn của dữ liệu liên kết thực tế giữa các bảng
        assertNotNull(savedApp.getId(), "ID Đơn ứng tuyển phải được sinh tự động bởi Hibernate Sequence.");
        assertEquals(ApplicationStatus.PENDING, savedApp.getStatus(), "Trạng thái khởi tạo mặc định của đơn ứng tuyển phải là PENDING.");
        assertEquals("https://cloudinary.com/real-cv-file.pdf", savedApp.getCvUrl(), "Hệ thống phải tự động fallback lấy chính xác link CV từ CandidateProfile.");
        // Kiểm tra câu truy vấn thực tế từ Repository xem bản ghi đã tồn tại trong H2 chưa
        List<JobApplication> dbList = jobApplicationRepository.findByCandidateId(candidateId);
        assertEquals(1, dbList.size(), "Database H2 phải ghi nhận chính xác 1 đơn ứng tuyển mới tồn tại.");
        assertEquals(jobId, dbList.get(0).getJobPosting().getId(), "Mối quan hệ khóa ngoại (ManyToOne) với bảng JobPosting phải chính xác.");
    }
}
