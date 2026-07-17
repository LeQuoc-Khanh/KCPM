package app.recruitment.service;

import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.dto.response.RecruiterDashboardResponse;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.mapper.RecruitmentMapper;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterDashboardServiceTest {

    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private RecruitmentMapper recruitmentMapper;

    @InjectMocks private RecruiterDashboardService dashboardService;

    @Test
    void getDashboardStats_ShouldReturnCorrectStatsAndPipeline() {
        Long recruiterId = 1L;
        when(jobPostingRepository.countByRecruiterIdAndStatus(recruiterId, JobStatus.PUBLISHED)).thenReturn(5L);
        when(jobApplicationRepository.countByJobPostingRecruiterId(recruiterId)).thenReturn(50L);
        when(jobApplicationRepository.countByJobPostingRecruiterIdAndAppliedAtAfter(eq(recruiterId), any(LocalDateTime.class))).thenReturn(10L);

        // Giả lập Pipeline Stats
        Object[] row1 = {"SCREENING", 15L};
        Object[] row2 = {"INTERVIEW", 5L};
        when(jobApplicationRepository.countApplicationsByStatus(recruiterId)).thenReturn(List.of(row1, row2));

        RecruiterDashboardResponse response = dashboardService.getDashboardStats(recruiterId);

        assertEquals(5L, response.getTotalActiveJobs());
        assertEquals(50L, response.getTotalCandidates());
        assertEquals(10L, response.getNewCandidatesToday());
        assertEquals(15L, response.getPipelineStats().get("SCREENING"));
        assertEquals(5L, response.getPipelineStats().get("INTERVIEW"));
    }

    @Test
    void getRecentApplications_ShouldReturnMappedList() {
        Long recruiterId = 1L;
        JobApplication mockApp = new JobApplication();
        JobApplicationResponse mockRes = JobApplicationResponse.builder().id(100L).build();

        when(jobApplicationRepository.findRecentApplicationsByRecruiter(eq(recruiterId), any(Pageable.class)))
                .thenReturn(List.of(mockApp));
        when(recruitmentMapper.toJobApplicationResponse(mockApp)).thenReturn(mockRes);

        List<JobApplicationResponse> result = dashboardService.getRecentApplications(recruiterId);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }
}