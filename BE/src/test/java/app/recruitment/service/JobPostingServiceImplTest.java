package app.recruitment.service;

import app.ai.service.cv.gemini.GeminiService;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.recruitment.dto.request.JobPostingRequest;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceImplTest {

    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private GeminiService geminiService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private JobApplicationRepository jobApplicationRepository;

    @InjectMocks
    private JobPostingServiceImpl jobPostingService;

    private User mockRecruiter;
    private Company mockCompany;
    private JobPostingRequest mockRequest;
    private JobPosting mockJob;

    @BeforeEach
    void setUp() {
        // Mock dữ liệu chuẩn bị cho các Test Case
        mockRecruiter = new User();
        mockRecruiter.setId(1L);
        mockRecruiter.setUserRole(UserRole.RECRUITER);

        mockCompany = new Company();
        mockCompany.setId(1L);

        mockRequest = new JobPostingRequest();
        mockRequest.setTitle("Java Backend Developer");
        mockRequest.setDescription("Làm việc với Spring Boot");
        mockRequest.setRequirements("3 năm kinh nghiệm");
        mockRequest.setLocation("Hồ Chí Minh");
        mockRequest.setExpiryDate(LocalDate.now().plusDays(30));

        mockJob = new JobPosting();
        mockJob.setId(100L);
        mockJob.setRecruiter(mockRecruiter);
        mockJob.setStatus(JobStatus.PENDING);
    }

    // TC_4.2: Create job (Luồng thành công)
    @Test
    void testCreateJob_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockRecruiter));
        when(companyRepository.findByRecruiterId(1L)).thenReturn(Optional.of(mockCompany));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(i -> i.getArguments()[0]);

        JobPosting createdJob = jobPostingService.create(1L, mockRequest);

        assertNotNull(createdJob);
        assertEquals("Java Backend Developer", createdJob.getTitle());
        assertEquals(JobStatus.PENDING, createdJob.getStatus());
        verify(jobPostingRepository, times(1)).save(any(JobPosting.class));
        verify(eventPublisher, times(1)).publishEvent(any()); // Kiểm tra có bắn event cộng điểm không
    }

    // TC_4.3: Update job (Luồng thành công)
    @Test
    void testUpdateJob_Success() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(mockJob));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(i -> i.getArguments()[0]);

        mockRequest.setTitle("Java Senior Developer");
        JobPosting updatedJob = jobPostingService.update(1L, 100L, mockRequest);

        assertNotNull(updatedJob);
        assertEquals("Java Senior Developer", updatedJob.getTitle());
        verify(jobPostingRepository, times(1)).save(any(JobPosting.class));
    }

    // TC_4.4: Delete job (Luồng thành công - Soft Delete)
    @Test
    void testDeleteJob_Success() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(mockJob));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(i -> i.getArguments()[0]);

        jobPostingService.delete(1L, 100L);

        // Assert trạng thái chuyển thành DELETED
        assertEquals(JobStatus.DELETED, mockJob.getStatus());
        verify(jobPostingRepository, times(1)).save(mockJob);
    }
}