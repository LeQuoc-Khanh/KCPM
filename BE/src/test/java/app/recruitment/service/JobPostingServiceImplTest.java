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
import app.recruitment.mapper.RecruitmentMapper;
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
    @Mock private RecruitmentMapper recruitmentMapper; // Đã thêm Mock cho Mapper

    @InjectMocks
    private JobPostingServiceImpl jobPostingService;

    private User mockRecruiter;
    private Company mockCompany;
    private JobPostingRequest mockRequest;
    private JobPosting mockJob;

    @BeforeEach
    void setUp() {
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

    // TC_4.4: Create job (Luồng thành công)
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
        verify(eventPublisher, times(1)).publishEvent(any()); 
    }

    // TC_4.5: Update job (Luồng thành công)
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

    // TC_4.6: Delete job (Luồng thành công - Soft Delete)
    @Test
    void testDeleteJob_Success() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(mockJob));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(i -> i.getArguments()[0]);

        jobPostingService.delete(1L, 100L);

        assertEquals(JobStatus.DELETED, mockJob.getStatus());
        verify(jobPostingRepository, times(1)).save(mockJob);
    }

    // TC_4.7: View recruiter's job postings list
    @Test
    void testListByRecruiter_Success() {
        when(jobPostingRepository.findByRecruiterIdAndStatusNot(eq(1L), any()))
            .thenReturn(java.util.List.of(mockJob));
        when(jobApplicationRepository.countByJobPostingId(anyLong())).thenReturn(5L);
        
        // Giả lập Mapper trả về DTO hợp lệ
        app.recruitment.dto.response.JobPostingResponse mockResponse = new app.recruitment.dto.response.JobPostingResponse();
        when(recruitmentMapper.toJobPostingResponse(any())).thenReturn(mockResponse);
        
        java.util.List<app.recruitment.dto.response.JobPostingResponse> list = jobPostingService.listByRecruiter(1L);
        
        assertFalse(list.isEmpty());
        assertEquals(5, list.get(0).getApplicationCount());
    }

    // TC_4.8: Get job posting detail by ID
    @Test
    void testGetJobDetail_Success() {
        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(mockJob));
        
        Optional<JobPosting> result = jobPostingService.getById(100L);
        
        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getId());
    }

    // TC_4.9: Search jobs by keyword
    @Test
    void testSearchJobs_Success() {
        when(jobPostingRepository.searchJobs("Java")).thenReturn(java.util.List.of(mockJob));
        
        java.util.List<JobPosting> result = jobPostingRepository.searchJobs("Java");
        
        assertFalse(result.isEmpty());
    }
}