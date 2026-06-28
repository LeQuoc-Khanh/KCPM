package app.recruitment.service;

import app.auth.model.User;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.ApplicationStatus;
import app.recruitment.repository.JobApplicationRepository;
import app.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository appRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private JobApplication mockApplication;
    private User mockRecruiter;
    private User mockCandidate;
    private JobPosting mockJob;

    @BeforeEach
    void setUp() {
        // Recruiter
        mockRecruiter = new User();
        mockRecruiter.setId(1L);

        // Candidate
        mockCandidate = new User();
        mockCandidate.setId(2L);
        mockCandidate.setFullName("Nguyễn Ứng Viên");

        // Job
        mockJob = new JobPosting();
        mockJob.setId(10L);
        mockJob.setTitle("Backend Java Developer");
        mockJob.setRecruiter(mockRecruiter);

        // Application (Đơn ứng tuyển đang chờ duyệt)
        mockApplication = new JobApplication();
        mockApplication.setId(100L);
        mockApplication.setCandidate(mockCandidate);
        mockApplication.setJobPosting(mockJob);
        mockApplication.setStatus(ApplicationStatus.PENDING);
    }

    @Test
    void testUpdateStatus_ToInterview_Success() {
        // Arrange
        when(appRepo.findById(100L)).thenReturn(Optional.of(mockApplication));
        when(appRepo.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act (Đổi trạng thái thành INTERVIEW)
        JobApplication updatedApp = jobApplicationService.updateStatus(1L, 100L, ApplicationStatus.INTERVIEW, "Đã gửi mail mời test logic");

        // Assert
        assertNotNull(updatedApp);
        assertEquals(ApplicationStatus.INTERVIEW, updatedApp.getStatus());
        assertEquals("Đã gửi mail mời test logic", updatedApp.getRecruiterNote());
        
        // Xác minh các tác vụ đi kèm (Lưu DB, Gửi Event, Gửi Thông báo) đã được gọi
        verify(appRepo, times(1)).save(any(JobApplication.class));
        verify(eventPublisher, times(1)).publishEvent(any());
        verify(notificationService, times(1)).sendNotification(
            eq(2L), 
            anyString(), 
            contains("Chúc mừng! Bạn đã được mời phỏng vấn"), 
            anyString()
        );
    }
}