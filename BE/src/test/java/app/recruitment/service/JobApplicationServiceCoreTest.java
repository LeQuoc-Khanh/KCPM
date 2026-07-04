package app.recruitment.service;

import app.ai.service.JobFastMatchingService;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
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
class JobApplicationServiceCoreTest {

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
    private JobApplication application;
    private JobApplicationRequest request;
    private CandidateProfile profile;

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

        application = JobApplication.builder()
                .id(100L)
                .candidate(candidate)
                .jobPosting(job)
                .cvUrl("cv.pdf")
                .status(ApplicationStatus.PENDING)
                .matchScore(80)
                .appliedAt(LocalDateTime.now())
                .build();

        profile = CandidateProfile.builder()
                .user(candidate)
                .cvFilePath("profile-cv.pdf")
                .phoneNumber("0123456789")
                .email("candidate@test.com")
                .skills(Collections.singletonList("Java"))
                .build();
    }

    @Test
    void TC_3_4_applyJobSuccessfully() {

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(appRepo.save(any(JobApplication.class)))
                .thenReturn(application);

        JobApplication result = service.apply(candidate.getId(), request);

        assertNotNull(result);
        assertEquals(application.getId(), result.getId());
        assertEquals(ApplicationStatus.PENDING, result.getStatus());

        verify(appRepo).save(any(JobApplication.class));
    }

    @Test
    void TC_3_5_applyUsingUploadedCvSuccessfully() {

        request.setCvUrl("uploaded-cv.pdf");

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(appRepo.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.apply(candidate.getId(), request);

        assertEquals("uploaded-cv.pdf", result.getCvUrl());

        verify(profileRepository, never()).findByUserId(anyLong());
    }

    @Test
    void TC_3_6_applyUsingCandidateProfileCvSuccessfully() {

        request.setCvUrl(null);

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(profileRepository.findByUserId(candidate.getId()))
                .thenReturn(Optional.of(profile));

        when(appRepo.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.apply(candidate.getId(), request);

        assertEquals("profile-cv.pdf", result.getCvUrl());

        verify(profileRepository).findByUserId(candidate.getId());
    }

    @Test
    void TC_3_7_publishRecruiterPointEventAfterSuccessfulApplication() {

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(appRepo.save(any(JobApplication.class)))
                .thenReturn(application);

        service.apply(candidate.getId(), request);

        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void TC_3_8_sendNotificationAfterSuccessfulApplication() {

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(appRepo.save(any(JobApplication.class)))
                .thenReturn(application);

        service.apply(candidate.getId(), request);

        verify(notificationService).sendNotification(
                eq(recruiter.getId()),
                anyString(),
                contains(candidate.getFullName()),
                contains("/applications/")
        );
    }

    @Test
    void TC_3_9_setDefaultMatchScoreWhenNoCvAnalysisExists() {

        when(userRepository.findById(candidate.getId()))
                .thenReturn(Optional.of(candidate));

        when(jobRepo.findById(job.getId()))
                .thenReturn(Optional.of(job));

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(false);

        when(analysisResultRepo.findByUserIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(Optional.empty());

        when(appRepo.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.apply(candidate.getId(), request);

        assertEquals(0, result.getMatchScore());
    }

    @Test
    void TC_3_10_getApplicationsByCandidateSuccessfully() {

        when(appRepo.findByCandidateId(candidate.getId()))
                .thenReturn(List.of(application));

        List<JobApplicationResponse> result =
                service.getApplicationsByCandidateId(candidate.getId());

        assertNotNull(result);
        assertEquals(1, result.size());

        JobApplicationResponse response = result.get(0);

        assertEquals(application.getId(), response.getId());
        assertEquals(candidate.getId(), response.getStudentId());
        assertEquals(job.getTitle(), response.getJobTitle());

        verify(appRepo).findByCandidateId(candidate.getId());
    }

    @Test
    void TC_3_11_getApplicationDetailSuccessfully() {

        when(appRepo.findById(application.getId()))
                .thenReturn(Optional.of(application));

        when(profileRepository.findByUserId(candidate.getId()))
                .thenReturn(Optional.of(profile));

        JobApplicationResponse result =
                service.getDetail(application.getId());

        assertNotNull(result);

        assertEquals(application.getId(), result.getId());
        assertEquals(candidate.getId(), result.getStudentId());
        assertEquals(candidate.getFullName(), result.getStudentName());
        assertEquals(job.getTitle(), result.getJobTitle());

        verify(appRepo).findById(application.getId());
    }

    @Test
    void TC_3_12_getApplicationDetailWithCandidateProfileSuccessfully() {

        when(appRepo.findById(application.getId()))
                .thenReturn(Optional.of(application));

        when(profileRepository.findByUserId(candidate.getId()))
                .thenReturn(Optional.of(profile));

        JobApplicationResponse result = service.getDetail(application.getId());

        assertNotNull(result);
        assertEquals(profile.getEmail(), result.getEmail());
        assertEquals(profile.getPhoneNumber(), result.getPhone());

        verify(profileRepository).findByUserId(candidate.getId());
    }

    @Test
    void TC_3_13_getApplicationByIdSuccessfully() {

        when(appRepo.findById(application.getId()))
                .thenReturn(Optional.of(application));

        Optional<JobApplication> result = service.getById(application.getId());

        assertTrue(result.isPresent());
        assertEquals(application.getId(), result.get().getId());

        verify(appRepo).findById(application.getId());
    }

    @Test
    void TC_3_14_checkApplicationStatusApplied() {

        when(appRepo.existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId()))
                .thenReturn(true);

        boolean result = service.hasApplied(candidate.getId(), job.getId());

        assertTrue(result);

        verify(appRepo)
                .existsByCandidateIdAndJobPostingId(candidate.getId(), job.getId());
    }

    @Test
    void TC_3_15_viewCandidateApplicationsSuccessfully() {

        when(appRepo.findByCandidateId(candidate.getId()))
                .thenReturn(List.of(application));

        List<JobApplication> result = service.listByCandidateId(candidate.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(application.getId(), result.get(0).getId());

        verify(appRepo).findByCandidateId(candidate.getId());
    }

}