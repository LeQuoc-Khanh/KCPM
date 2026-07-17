package app.candidate.service;

import app.ai.service.JobFastMatchingService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.content.model.Company;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRecommendationServiceTest {

    @Mock private CandidateProfileRepository profileRepository;
    @Mock private JobPostingRepository jobRepository;
    @Mock private JobFastMatchingService fastMatchingService;

    @InjectMocks private JobRecommendationService recommendationService;

    @Test
    void getAllJobs_ShouldReturnBasicInfo() {
        JobPosting job = new JobPosting();
        job.setId(1L);
        job.setTitle("Dev");
        when(jobRepository.findByStatus(JobStatus.PUBLISHED)).thenReturn(List.of(job));

        List<Map<String, Object>> result = recommendationService.getAllJobs();
        assertEquals(1, result.size());
        assertEquals("Dev", result.get(0).get("title"));
    }

    @Test
    void getRecentJobs_ShouldReturnTop10() {
        JobPosting job = new JobPosting();
        job.setId(2L);
        Company company = new Company();
        company.setName("Tech Corp");
        job.setCompany(company);
        
        when(jobRepository.findTop10ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED)).thenReturn(List.of(job));

        List<Map<String, Object>> result = recommendationService.getRecentJobs();
        assertEquals(1, result.size());
        assertEquals("Tech Corp", result.get(0).get("company"));
    }

    @Test
    void getMatchingJobs_WhenProfileEmpty_ShouldReturnEmptyList() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertTrue(recommendationService.getMatchingJobs(1L).isEmpty());
    }

    @Test
    void getMatchingJobs_WhenValid_ShouldFilterAndSortByScore() {
        CandidateProfile profile = new CandidateProfile();
        profile.setSkills(List.of("Java"));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        JobPosting job1 = new JobPosting(); job1.setId(10L); job1.setTitle("Job 10");
        JobPosting job2 = new JobPosting(); job2.setId(20L); job2.setTitle("Job 20");
        when(jobRepository.findByStatus(JobStatus.PUBLISHED)).thenReturn(List.of(job1, job2));

        // Giả lập Job 10 được 40 điểm (Loại), Job 20 được 85 điểm (Đậu)
        FastMatchResult result10 = new FastMatchResult(); result10.setMatchScore(40);
        FastMatchResult result20 = new FastMatchResult(); result20.setMatchScore(85);
        when(fastMatchingService.calculateBatchCompatibility(anyList(), anyList()))
                .thenReturn(Map.of(10L, result10, 20L, result20));

        when(jobRepository.findAllById(List.of(20L))).thenReturn(List.of(job2));

        List<Map<String, Object>> result = recommendationService.getMatchingJobs(1L);

        assertEquals(1, result.size()); // Chỉ giữ lại job 20
        assertEquals(85, result.get(0).get("matchScore"));
        assertEquals("Job 20", result.get(0).get("title"));
    }
}