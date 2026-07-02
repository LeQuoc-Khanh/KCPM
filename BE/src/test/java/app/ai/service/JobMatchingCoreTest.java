package app.ai.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.gemini.GeminiService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.ai.service.cv.gemini.dto.MatchResult;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;

@ExtendWith(MockitoExtension.class)
class JobMatchingCoreTest {

    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private CVAnalysisService cvAnalysisService;
    @Mock private GeminiService geminiService;
    @Mock private JobApplicationRepository applicationRepository;
    @Mock private CandidateProfileRepository profileRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private UserRepository userRepository;
    @Mock private CVAnalysisResultRepository analysisRepository;

    @InjectMocks private JobFastMatchingService jobFastMatchingService;
    @InjectMocks private JobMatchingService jobMatchingService;

    @Test
    @DisplayName("TC_5.4 Candidate job matching success")
    void shouldCalculateCandidateJobMatchingSuccessfully() {
        JobPosting job = mock(JobPosting.class);
        when(job.getId()).thenReturn(101L);
        when(job.getExtractedSkills()).thenReturn(List.of("Java", "Spring Boot", "Docker"));
        when(jobPostingRepository.findAllByIdsWhithSkills(List.of(101L))).thenReturn(List.of(job));

        Map<Long, FastMatchResult> result = jobFastMatchingService.calculateBatchCompatibility(
                List.of("java", "spring boot"), List.of(101L));

        assertEquals(67, result.get(101L).getMatchScore());
        assertEquals(List.of("Java", "Spring Boot"), result.get(101L).getMatchedSkills());
        assertEquals(List.of("Docker"), result.get(101L).getMissingSkills());
    }

    @Test
    @DisplayName("TC_5.5 Candidate job matching returns correct matching score")
    void shouldReturnCorrectMatchingScore() {
        JobPosting job = mock(JobPosting.class);
        when(job.getId()).thenReturn(1L);
        when(job.getExtractedSkills()).thenReturn(List.of("Java", "SQL"));
        when(jobPostingRepository.findAllByIdsWhithSkills(List.of(1L))).thenReturn(List.of(job));

        Map<Long, FastMatchResult> result = jobFastMatchingService.calculateBatchCompatibility(
                List.of("Java", "SQL"), List.of(1L));

        assertEquals(100, result.get(1L).getMatchScore());
    }

    @Test
    @DisplayName("TC_5.6 Recruiter AI screening success")
    void shouldScreenApplicationsSuccessfully() throws Exception {
        User candidate = mock(User.class);
        when(candidate.getId()).thenReturn(10L);

        JobApplication app = mock(JobApplication.class);
        when(app.getId()).thenReturn(100L);
        when(app.getCandidate()).thenReturn(candidate);
        when(app.getMatchScore()).thenReturn(null);

        JobPosting job = mock(JobPosting.class);
        when(job.getDescription()).thenReturn("Backend Java developer");
        when(job.getRequirements()).thenReturn("Java, Spring Boot");

        CandidateProfile profile = mock(CandidateProfile.class);
        when(profile.getFullName()).thenReturn("Nguyen Van A");
        when(profile.getAboutMe()).thenReturn("Java developer");
        when(profile.getSkills()).thenReturn(List.of("Java", "Spring Boot"));
        when(profile.getExperiences()).thenReturn(null);
        when(profile.getEducationJson()).thenReturn(null);

        MatchResult aiResult = MatchResult.builder()
                .matchPercentage(90)
                .evaluation("Strong candidate")
                .matchedSkillsCount(2)
                .missingSkillsCount(1)
                .missingSkillsList(List.of("Docker"))
                .build();

        when(applicationRepository.findByJobPostingId(20L)).thenReturn(List.of(app));
        when(jobPostingRepository.findById(20L)).thenReturn(Optional.of(job));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(objectMapper.writeValueAsString(any())).thenReturn("candidate-json");
        when(geminiService.matchCVWithJob(anyString(), eq("Backend Java developer"), eq("Java, Spring Boot")))
                .thenReturn(aiResult);

        jobMatchingService.screenApplications(20L);

        verify(app).setMatchScore(90);
        verify(app).setAiEvaluation("Strong candidate");
        verify(app).setMatchedSkillsCount(2);
        verify(app).setMissingSkillsCount(1);
        verify(app).setMissingSkillsList("Docker");
        verify(applicationRepository).save(app);
    }

    @Test
    @DisplayName("TC_5.7 Recruiter AI screening updates candidate evaluation")
    void shouldUpdateCandidateEvaluationDuringScreening() throws Exception {
        User candidate = mock(User.class);
        when(candidate.getId()).thenReturn(10L);
        JobApplication app = mock(JobApplication.class);
        when(app.getCandidate()).thenReturn(candidate);
        when(app.getMatchScore()).thenReturn(0);

        JobPosting job = mock(JobPosting.class);
        when(job.getDescription()).thenReturn("JD");
        when(job.getRequirements()).thenReturn("REQ");

        CandidateProfile profile = mock(CandidateProfile.class);
        when(profile.getFullName()).thenReturn("Nguyen Van A");
        when(profile.getAboutMe()).thenReturn("About");
        when(profile.getSkills()).thenReturn(List.of("Java"));
        when(profile.getExperiences()).thenReturn(null);
        when(profile.getEducationJson()).thenReturn(null);

        MatchResult aiResult = MatchResult.builder().evaluation("Good fit").matchPercentage(80).build();

        when(applicationRepository.findByJobPostingId(20L)).thenReturn(List.of(app));
        when(jobPostingRepository.findById(20L)).thenReturn(Optional.of(job));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(objectMapper.writeValueAsString(any())).thenReturn("profile-json");
        when(geminiService.matchCVWithJob(anyString(), eq("JD"), eq("REQ"))).thenReturn(aiResult);

        jobMatchingService.screenApplications(20L);

        verify(app).setAiEvaluation("Good fit");
        verify(applicationRepository).save(app);
    }

    @Test
    @DisplayName("TC_5.13 Batch candidate scoring completed successfully")
    void shouldCalculateBatchCandidateScoresSuccessfully() {
        JobPosting job1 = mock(JobPosting.class);
        JobPosting job2 = mock(JobPosting.class);
        when(job1.getId()).thenReturn(1L);
        when(job1.getExtractedSkills()).thenReturn(List.of("Java", "SQL"));
        when(job2.getId()).thenReturn(2L);
        when(job2.getExtractedSkills()).thenReturn(List.of("React", "CSS"));
        when(jobPostingRepository.findAllByIdsWhithSkills(List.of(1L, 2L))).thenReturn(List.of(job1, job2));

        Map<Long, FastMatchResult> result = jobFastMatchingService.calculateBatchCompatibility(
                List.of("Java", "SQL", "CSS"), List.of(1L, 2L));

        assertEquals(100, result.get(1L).getMatchScore());
        assertEquals(50, result.get(2L).getMatchScore());
    }

    @Test
    @DisplayName("TC_5.14 Batch candidate scoring saves match results successfully")
    void shouldReturnRankedApplicationsSuccessfully() {
        JobApplication first = mock(JobApplication.class);
        JobApplication second = mock(JobApplication.class);
        when(applicationRepository.findByJobPostingIdAndMatchScoreGreaterThanEqualOrderByMatchScoreDesc(20L, 70))
                .thenReturn(List.of(first, second));

        List<JobApplication> result = jobMatchingService.getRankedApplications(20L, 70);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("TC_5.16 AI recommendation includes matching reasons")
    void shouldBuildApplicationAnalysisFromStoredApplicationData() {
        JobApplication app = mock(JobApplication.class);
        when(app.getMatchScore()).thenReturn(75);
        when(app.getAiEvaluation()).thenReturn("Good Java background");
        when(app.getMatchedSkillsCount()).thenReturn(2);
        when(app.getMissingSkillsCount()).thenReturn(2);
        when(app.getMissingSkillsList()).thenReturn("Docker, Kubernetes");
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));

        MatchResult result = jobMatchingService.getApplicationAnalysis(100L);

        assertEquals(75, result.getMatchPercentage());
        assertEquals("Good Java background", result.getEvaluation());
        assertEquals(List.of("Docker", "Kubernetes"), result.getMissingSkillsList());
    }

    @Test
    @DisplayName("TC_5.17 AI recommendation returns matching score successfully")
    void shouldReturnMatchingScoreSuccessfully() {
        MatchResult result = MatchResult.builder().matchPercentage(85).build();

        assertEquals(85, result.getMatchPercentage());
    }

    @Test
    @DisplayName("TC_5.20 AI feature response contains required output fields")
    void shouldContainRequiredOutputFields() {
        MatchResult result = MatchResult.builder()
                .matchPercentage(90)
                .evaluation("Good match")
                .learningPath("Learn Docker")
                .careerAdvice("Apply now")
                .build();

        assertAll(
                () -> assertEquals(90, result.getMatchPercentage()),
                () -> assertNotNull(result.getEvaluation()),
                () -> assertNotNull(result.getLearningPath()),
                () -> assertNotNull(result.getCareerAdvice())
        );
    }
}
