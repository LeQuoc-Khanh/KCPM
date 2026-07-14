package app.recruitment.service;

import app.ai.service.cv.gemini.GeminiService;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.recruitment.dto.response.JobPostingResponse;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceCoreTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Mock
    private GeminiService geminiService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private JobPostingServiceImpl jobPostingService;

    private JobPosting jobPosting;
    private JobPostingResponse response;

    @BeforeEach
    void setUp() {

        jobPosting = JobPosting.builder()
                .id(1L)
                .title("Java Developer")
                .description("Java Spring Boot")
                .requirements("Spring Boot")
                .salaryRange("1000$")
                .location("HCM")
                .status(JobStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        response = JobPostingResponse.builder()
                .id(1L)
                .title("Java Developer")
                .description("Java Spring Boot")
                .requirements("Spring Boot")
                .salaryRange("1000$")
                .location("HCM")
                .status("PUBLISHED")
                .applicationCount(5)
                .build();
    }

        
    @Test
    void TC_3_1_searchJobsSuccessfullyWithKeyword() {

        // Arrange
        String keyword = "Java";

        when(jobPostingRepository.searchJobs(keyword))
                .thenReturn(List.of(jobPosting));

        when(recruitmentMapper.toJobPostingResponse(jobPosting))
                .thenReturn(response);

        // Act
        List<JobPostingResponse> result = jobPostingService.searchJobs(keyword);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());

        verify(jobPostingRepository).searchJobs(keyword);
        verify(recruitmentMapper).toJobPostingResponse(jobPosting);
    }

    
    @Test
    void TC_3_2_searchJobsSuccessfullyWithoutKeyword() {

        // Arrange
        when(jobPostingRepository.findTop10ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED))
                .thenReturn(List.of(jobPosting));

        when(recruitmentMapper.toJobPostingResponse(jobPosting))
                .thenReturn(response);

        // Act
        List<JobPostingResponse> result = jobPostingService.searchJobs("");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());

        verify(jobPostingRepository)
                .findTop10ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED);

        verify(recruitmentMapper).toJobPostingResponse(jobPosting);
    }

    @Test
    void TC_3_3_getPublicJobDetailSuccessfully() {

        // Arrange
        when(jobPostingRepository.findById(1L))
                .thenReturn(java.util.Optional.of(jobPosting));

        when(recruitmentMapper.toJobPostingResponse(jobPosting))
                .thenReturn(response);

        when(jobApplicationRepository.countByJobPostingId(1L))
                .thenReturn(5L);

        // Act
        JobPostingResponse result = jobPostingService.getJobDetailPublic(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java Developer", result.getTitle());
        assertEquals(5, result.getApplicationCount());

        verify(jobPostingRepository).findById(1L);
        verify(jobApplicationRepository).countByJobPostingId(1L);
        verify(recruitmentMapper).toJobPostingResponse(jobPosting);
    }
}