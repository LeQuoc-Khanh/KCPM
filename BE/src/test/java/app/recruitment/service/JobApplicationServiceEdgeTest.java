package app.recruitment.service;

import app.ai.service.JobFastMatchingService;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.repository.CandidateProfileRepository;
import app.notification.service.NotificationService;
import app.recruitment.dto.request.JobApplicationRequest;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceEdgeTest {

    @Mock
    private JobApplicationRepository appRepo;

    @Mock
    private JobPostingRepository jobRepo;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateProfileRepository profileRepository;

    @Mock
    private CVAnalysisResultRepository analysisResultRepo;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JobFastMatchingService fastMatchingService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private JobApplicationServiceImpl service;

    private User candidate;
    private User recruiter;
    private JobPosting job;
    private JobApplicationRequest request;

    @BeforeEach
    void setUp() {

        recruiter = User.builder()
                .id(1L)
                .fullName("Recruiter")
                .build();

        candidate = User.builder()
                .id(2L)
                .fullName("Candidate")
                .email("candidate@test.com")
                .build();

        job = JobPosting.builder()
                .id(10L)
                .title("Java Developer")
                .status(JobStatus.PUBLISHED)
                .recruiter(recruiter)
                .createdAt(LocalDateTime.now())
                .build();

        request = new JobApplicationRequest();
        request.setJobId(10L);
        request.setCvUrl("cv.pdf");
        request.setCoverLetter("My Cover Letter");

    }

    @Test
    void TC_3_16_applyDuplicateJob() {

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.apply(candidate.getId(), request));

        assertEquals("Bạn đã ứng tuyển công việc này rồi.", ex.getMessage());

        verify(appRepo, never()).save(any());
    }

    @Test
    void TC_3_17_applyWithInvalidJobId() {

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.apply(candidate.getId(), request));

        verify(appRepo, never()).save(any());
    }

    @Test
    void TC_3_18_applyWithInvalidCandidateId() {

        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.apply(999L, request));

        verify(appRepo, never()).save(any());
    }

    @Test
    void TC_3_19_applyWithoutUploadedCvAndProfileHasNoCv() {

        request.setCvUrl(null);

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(profileRepository.findByUserId(candidate.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.apply(candidate.getId(), request));

        verify(appRepo, never()).save(any());
    }

    @Test
    void TC_3_20_getApplicationDetailWithInvalidApplicationId() {

        when(appRepo.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getDetail(999L));
    }

    @Test
    void TC_3_21_getApplicationByInvalidId() {

        when(appRepo.findById(anyLong()))
                .thenReturn(Optional.empty());

        Optional<JobApplication> result = service.getById(999L);

        assertTrue(result.isEmpty());

        verify(appRepo).findById(999L);
    }

    @Test
    void TC_3_22_checkApplicationStatusNotApplied() {

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        boolean result = service.hasApplied(candidate.getId(), job.getId());

        assertFalse(result);

        verify(appRepo)
                .existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId());
    }

    @Test
    void TC_3_23_getEmptyApplicationListForCandidate() {

        when(appRepo.findByCandidateId(candidate.getId()))
                .thenReturn(Collections.emptyList());

        List<JobApplication> result =
                service.listByCandidateId(candidate.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(appRepo).findByCandidateId(candidate.getId());
    }

}

    
