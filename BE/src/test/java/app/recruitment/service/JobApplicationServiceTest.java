package app.recruitment.service;

import app.ai.service.JobFastMatchingService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.repository.CandidateProfileRepository;
import app.gamification.event.PointEvent;
import app.notification.service.NotificationService;
import app.recruitment.dto.request.JobApplicationRequest;
import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.ApplicationStatus;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    // 1. Khai báo Mock toàn bộ các dependencies có trong Service
    @Mock private JobFastMatchingService fastMatchingService;
    @Mock private JobApplicationRepository appRepo;
    @Mock private JobPostingRepository jobRepo;
    @Mock private UserRepository userRepository;
    @Mock private CandidateProfileRepository profileRepository;
    @Mock private CVAnalysisResultRepository analysisResultRepo;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NotificationService notificationService;

    // Inject các mock ở trên vào Service thực tế cần test
    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    // --- TEST CASE 1: Lỗi do Job đã bị xóa (TC_F4_03) ---
    @Test
    void apply_WhenJobIsDeleted_ShouldThrowException() {
        // Arrange (Chuẩn bị data giả)
        Long candidateId = 1L;
        JobApplicationRequest request = new JobApplicationRequest();
        request.setJobId(99L);

        User mockUser = new User();
        mockUser.setId(candidateId);
        
        JobPosting mockJob = new JobPosting();
        mockJob.setId(99L);
        mockJob.setStatus(JobStatus.DELETED); // Cố tình set status bị xóa

        when(userRepository.findById(candidateId)).thenReturn(Optional.of(mockUser));
        when(jobRepo.findById(99L)).thenReturn(Optional.of(mockJob));

        // Act & Assert (Thực thi và kiểm tra lỗi)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.apply(candidateId, request);
        });

        assertEquals("Cong viec nay da bi xoa hoac khong con nhan ho so.", exception.getMessage());
        // Xác nhận hàm save chưa bao giờ được gọi
        verify(appRepo, never()).save(any(JobApplication.class));
    }

    // --- TEST CASE 2: Lỗi do ứng viên đã nộp đơn rồi (TC_F4_04) ---
    @Test
    void apply_WhenAlreadyApplied_ShouldThrowException() {
        // Arrange
        Long candidateId = 1L;
        JobApplicationRequest request = new JobApplicationRequest();
        request.setJobId(10L);

        User mockUser = new User();
        mockUser.setId(candidateId);

        JobPosting mockJob = new JobPosting();
        mockJob.setId(10L);
        mockJob.setStatus(JobStatus.PUBLISHED);

        when(userRepository.findById(candidateId)).thenReturn(Optional.of(mockUser));
        when(jobRepo.findById(10L)).thenReturn(Optional.of(mockJob));
        
        // Giả lập DB trả về true (đã nộp rồi)
        when(appRepo.existsByCandidateIdAndJobPostingId(candidateId, 10L)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.apply(candidateId, request);
        });

        assertEquals("Bạn đã ứng tuyển công việc này rồi.", exception.getMessage());
    }

    // --- TEST CASE 3: Nộp đơn thành công - Happy Path (TC_F4_10) ---
    @Test
    void apply_HappyPath_ShouldSaveAndPublishEvent() {
        // Arrange
        Long candidateId = 1L;
        JobApplicationRequest request = new JobApplicationRequest();
        request.setJobId(10L);
        request.setCvUrl("https://cloudinary.com/my-cv.pdf"); // CV hợp lệ

        User mockUser = new User();
        mockUser.setId(candidateId);
        mockUser.setFullName("Nguyen Van An");

        User mockRecruiter = new User();
        mockRecruiter.setId(5L);

        JobPosting mockJob = new JobPosting();
        mockJob.setId(10L);
        mockJob.setTitle("Backend Java Developer");
        mockJob.setStatus(JobStatus.PUBLISHED);
        mockJob.setRecruiter(mockRecruiter);

        when(userRepository.findById(candidateId)).thenReturn(Optional.of(mockUser));
        when(jobRepo.findById(10L)).thenReturn(Optional.of(mockJob));
        when(appRepo.existsByCandidateIdAndJobPostingId(candidateId, 10L)).thenReturn(false); // Chưa nộp
        when(analysisResultRepo.findByUserIdAndJobPostingId(candidateId, 10L)).thenReturn(Optional.empty()); // Ko có AI result

        JobApplication mockSavedApp = new JobApplication();
        mockSavedApp.setId(100L);
        when(appRepo.save(any(JobApplication.class))).thenReturn(mockSavedApp);

        // Act
        JobApplication result = jobApplicationService.apply(candidateId, request);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());

        // Kiểm tra xem dữ liệu build trước khi save có đúng không (Dùng ArgumentCaptor)
        ArgumentCaptor<JobApplication> appCaptor = ArgumentCaptor.forClass(JobApplication.class);
        verify(appRepo).save(appCaptor.capture());
        
        JobApplication capturedApp = appCaptor.getValue();
        assertEquals("https://cloudinary.com/my-cv.pdf", capturedApp.getCvUrl());
        assertEquals(ApplicationStatus.PENDING, capturedApp.getStatus());
        assertEquals(0, capturedApp.getMatchScore()); // Do không có AI result

        // Xác nhận Event cộng điểm đã được bắn ra 1 lần
        verify(eventPublisher, times(1)).publishEvent(any(PointEvent.class));
        
        // Xác nhận Notification đã được gửi đi 1 lần
        verify(notificationService, times(1)).sendNotification(eq(5L), anyString(), anyString(), anyString());
    }
    // =========================================================================
    // HÀM UPDATE STATUS
    // =========================================================================

    // --- TEST CASE 4: Cập nhật thất bại do không tìm thấy đơn (TC_F4_11) ---
    @Test
    void updateStatus_WhenApplicationNotFound_ShouldThrowException() {
        // Arrange
        when(appRepo.findByIdWithCandidateAndJobPosting(99L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.updateStatus(1L, 99L, ApplicationStatus.SCREENING, "Note");
        });
        assertEquals("Application not found", exception.getMessage());
    }

    // --- TEST CASE 5: Cập nhật thất bại do sai quyền Recruiter (TC_F4_12) ---
    @Test
    void updateStatus_WhenUnauthorizedRecruiter_ShouldThrowException() {
        // Arrange
        JobApplication mockApp = new JobApplication();
        JobPosting mockJob = new JobPosting();
        User ownerRecruiter = new User();
        ownerRecruiter.setId(1L); // Chủ sở hữu Job là Recruiter ID 1
        
        mockJob.setRecruiter(ownerRecruiter);
        mockApp.setJobPosting(mockJob);

        when(appRepo.findByIdWithCandidateAndJobPosting(100L)).thenReturn(Optional.of(mockApp));

        // Act & Assert: Người thao tác là Recruiter ID 2 (Không có quyền)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.updateStatus(2L, 100L, ApplicationStatus.SCREENING, "Note");
        });
        assertEquals("Không có quyền chỉnh sửa đơn ứng tuyển này.", exception.getMessage());
    }

    // --- TEST CASE 6: Cập nhật thành công sang OFFERED (TC_F4_14) ---
    @Test
    void updateStatus_HappyPath_Offered_ShouldSaveAndPublishEvent() {
        // Arrange
        Long recruiterId = 1L;
        Long applicationId = 100L;

        JobApplication mockApp = new JobApplication();
        
        // Setup Job và Recruiter
        JobPosting mockJob = new JobPosting();
        mockJob.setTitle("Java Backend Developer");
        User ownerRecruiter = new User();
        ownerRecruiter.setId(recruiterId);
        mockJob.setRecruiter(ownerRecruiter);
        mockApp.setJobPosting(mockJob);
        
        // Setup Candidate
        User candidate = new User();
        candidate.setId(9L);
        candidate.setFullName("Nguyen Van B");
        mockApp.setCandidate(candidate);

        when(appRepo.findByIdWithCandidateAndJobPosting(applicationId)).thenReturn(Optional.of(mockApp));
        when(appRepo.save(any(JobApplication.class))).thenReturn(mockApp);

        // Act
        jobApplicationService.updateStatus(recruiterId, applicationId, ApplicationStatus.OFFERED, "Welcome to the team!");

        // Assert
        assertEquals(ApplicationStatus.OFFERED, mockApp.getStatus());
        assertEquals("Welcome to the team!", mockApp.getRecruiterNote());
        
        // Kiểm tra xem hàm lưu DB đã được gọi chưa
        verify(appRepo).save(mockApp);
        
        // Kiểm tra Event cộng điểm HIRED đã được bắn ra
        verify(eventPublisher, times(1)).publishEvent(any(PointEvent.class));
        
        // Kiểm tra Notification đã được gửi đến Candidate (ID 9)
        verify(notificationService, times(1)).sendNotification(eq(9L), anyString(), anyString(), anyString());
    }
    // =========================================================================
    // HÀM LIST BY JOB (QUÉT ỨNG VIÊN & FAST MATCHING)
    // =========================================================================

    // --- TEST CASE 7: Lấy danh sách thất bại do Job không tồn tại (TC_F4_16) ---
    @Test
    void listByJob_WhenJobNotFound_ShouldThrowException() {
        // Arrange
        when(jobRepo.existsById(99L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.listByJob(99L);
        });
        assertEquals("Job not found with ID: 99", exception.getMessage());
    }

    // --- TEST CASE 8: Lấy danh sách thành công kèm điểm Fast Match (TC_F4_17) ---
    @Test
    void listByJob_HappyPath_ShouldReturnSortedList() {
        // Arrange
        Long jobId = 10L;
        when(jobRepo.existsById(jobId)).thenReturn(true);

        // Tạo dữ liệu giả cho Đơn ứng tuyển
        JobApplication mockApp = new JobApplication();
        mockApp.setId(100L);
        mockApp.setStatus(ApplicationStatus.PENDING);
        
        JobPosting mockJob = new JobPosting();
        mockJob.setId(jobId);
        mockJob.setTitle("Java Dev");
        mockApp.setJobPosting(mockJob);
        
        User candidate = new User();
        candidate.setId(1L);
        candidate.setFullName("Nguyen Van An");
        mockApp.setCandidate(candidate);

        // Mock các repository
        when(appRepo.findByJobPostingId(jobId)).thenReturn(List.of(mockApp));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty()); // Giả sử chưa có Profile
        
        // Mock dịch vụ Fast Matching của AI trả về 85 điểm
        FastMatchResult matchResult = new FastMatchResult();
        matchResult.setMatchScore(85);
        
        when(fastMatchingService.calculateBatchCompatibility(anyList(), eq(List.of(jobId))))
            .thenReturn(Map.of(jobId, matchResult));

        // Act
        List<JobApplicationResponse> result = jobApplicationService.listByJob(jobId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nguyen Van An", result.get(0).getStudentName());
        assertEquals(85, result.get(0).getMatchScore());
        assertEquals("Java Dev", result.get(0).getJobTitle());
    }
}