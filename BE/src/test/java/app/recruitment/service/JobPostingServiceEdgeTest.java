package app.recruitment.service;

import app.ai.service.cv.gemini.GeminiService;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.recruitment.dto.response.JobPostingResponse;
import app.recruitment.mapper.RecruitmentMapper;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceEdgeTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Mock
    private GeminiService geminiService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private JobPostingServiceImpl service;


   

    @Test
    void TC_3_24_searchJobsReturnsEmptyList() {

        String keyword = "NonExistingKeyword";

        when(jobPostingRepository.searchJobs(keyword))
                .thenReturn(Collections.emptyList());

        List<JobPostingResponse> result = service.searchJobs(keyword);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(jobPostingRepository).searchJobs(keyword);
    }

    @Test
    void TC_3_25_getPublicJobDetailWithInvalidId() {

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(anyLong()))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getJobDetailPublic(999L));

        assertEquals("Job not found: 999", ex.getMessage());

        verify(jobPostingRepository).findByIdWithRecruiterAndCompany(999L); // Nhớ đổi ID tương ứng 1L hoặc 999L
    }

}