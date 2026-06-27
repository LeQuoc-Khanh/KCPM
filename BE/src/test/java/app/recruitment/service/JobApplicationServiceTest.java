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
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

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

    // TC3.1 - Apply Job Successfully
@Test
void TC31_applyJobSuccessfully() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl("cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L, 10L))
            .thenReturn(false);

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    JobApplication result = service.apply(1L, request);

    assertNotNull(result);
    assertEquals("cv.pdf", result.getCvUrl());

    verify(appRepo).save(any(JobApplication.class));
}

// TC3.2 - View Application List
@Test
void TC32_getApplicationsByCandidate() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .candidate(candidate)
            .jobPosting(job)
            .cvUrl("cv.pdf")
            .build();

    when(appRepo.findByCandidateId(1L))
            .thenReturn(List.of(application));

    List<JobApplicationResponse> result =
            service.getApplicationsByCandidateId(1L);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(100L, result.get(0).getId());

    verify(appRepo).findByCandidateId(1L);
}

// TC3.3 - View Application Detail
@Test
void TC33_getApplicationDetail() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .email("kiet@gmail.com")
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .candidate(candidate)
            .jobPosting(job)
            .cvUrl("cv.pdf")
            .build();

    CandidateProfile profile = new CandidateProfile();
    profile.setPhoneNumber("0123456789");
    profile.setEmail("kiet@gmail.com");

    when(appRepo.findById(100L))
            .thenReturn(Optional.of(application));

    when(profileRepository.findByUserId(1L))
            .thenReturn(Optional.of(profile));

    JobApplicationResponse result =
            service.getDetail(100L);

    assertNotNull(result);
    assertEquals(100L, result.getId());
    assertEquals("Java Developer", result.getJobTitle());
    assertEquals("0123456789", result.getPhone());

    verify(appRepo).findById(100L);
}

// TC3.4 - Check Applied Status
@Test
void TC34_hasAppliedSuccessfully() {

    when(appRepo.existsByCandidateIdAndJobPostingId(1L, 10L))
            .thenReturn(true);

    boolean result =
            service.hasApplied(1L, 10L);

    assertTrue(result);

    verify(appRepo)
            .existsByCandidateIdAndJobPostingId(1L, 10L);
}

// TC3.5 - Update Application Status
@Test
void TC35_updateApplicationStatus() {

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .recruiter(recruiter)
            .title("Java Developer")
            .build();

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .jobPosting(job)
            .candidate(candidate)
            .status(ApplicationStatus.PENDING)
            .build();

    when(appRepo.findById(100L))
            .thenReturn(Optional.of(application));

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    JobApplication result = service.updateStatus(
            2L,
            100L,
            ApplicationStatus.SCREENING,
            "Good CV"
    );

    assertEquals(
            ApplicationStatus.SCREENING,
            result.getStatus()
    );

    assertEquals(
            "Good CV",
            result.getRecruiterNote()
    );

    verify(appRepo).save(any(JobApplication.class));
}

// TC3.6 - List Applications By Job
@Test
void TC36_listApplicationsByJob() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .candidate(candidate)
            .jobPosting(job)
            .status(ApplicationStatus.PENDING)
            .build();

    CandidateProfile profile = new CandidateProfile();
    profile.setSkills(List.of("Java", "Spring"));

    when(jobRepo.existsById(10L)).thenReturn(true);

    when(appRepo.findByJobPostingId(10L))
            .thenReturn(List.of(application));

    when(profileRepository.findByUserId(1L))
            .thenReturn(Optional.of(profile));

    when(fastMatchingService.calculateBatchCompatibility(
            anyList(),
            anyList()))
            .thenReturn(new HashMap<>());

    List<JobApplicationResponse> result =
            service.listByJob(10L);

    assertNotNull(result);
    assertEquals(1, result.size());

    verify(appRepo).findByJobPostingId(10L);
}

// TC3.7 - Get Candidate Applications
@Test
void TC37_listByCandidateId() {

    User candidate = User.builder()
            .id(1L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .build();

    JobApplication application = JobApplication.builder()
            .candidate(candidate)
            .jobPosting(job)
            .build();

    when(appRepo.findByCandidateId(1L))
            .thenReturn(List.of(application));

    List<JobApplication> result =
            service.listByCandidateId(1L);

    assertEquals(1, result.size());

    verify(appRepo).findByCandidateId(1L);
}

// TC3.8 - Scan And Suggest Candidates
@Test
void TC38_scanAndSuggestCandidates() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Backend Developer")
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .candidate(candidate)
            .jobPosting(job)
            .build();

    CandidateProfile profile = new CandidateProfile();
    profile.setSkills(List.of("Java"));

    when(jobRepo.existsById(10L))
            .thenReturn(true);

    when(appRepo.findByJobPostingId(10L))
            .thenReturn(List.of(application));

    when(profileRepository.findByUserId(1L))
            .thenReturn(Optional.of(profile));

    when(fastMatchingService.calculateBatchCompatibility(
            anyList(),
            anyList()))
            .thenReturn(new HashMap<>());

    List<JobApplicationResponse> result =
            service.scanAndSuggestCandidates(10L);

    assertNotNull(result);

    verify(appRepo).findByJobPostingId(10L);
}

// TC3.9 - Get Application By ID
@Test
void TC39_getApplicationById() {

    JobApplication application = JobApplication.builder()
            .id(100L)
            .build();

    when(appRepo.findById(100L))
            .thenReturn(Optional.of(application));

    Optional<JobApplication> result =
            service.getById(100L);

    assertTrue(result.isPresent());

    assertEquals(
            100L,
            result.get().getId()
    );

    verify(appRepo).findById(100L);
}

// TC3.10 - Get Detail With Candidate Profile
@Test
void TC310_getDetailWithCandidateProfile() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .email("kiet@gmail.com")
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .candidate(candidate)
            .jobPosting(job)
            .cvUrl("cv.pdf")
            .matchScore(90)
            .aiEvaluation("Excellent")
            .matchedSkillsList("Java")
            .missingSkillsList("Docker")
            .build();

    CandidateProfile profile = new CandidateProfile();
    profile.setEmail("kiet@gmail.com");
    profile.setPhoneNumber("0123456789");

    when(appRepo.findById(100L))
            .thenReturn(Optional.of(application));

    when(profileRepository.findByUserId(1L))
            .thenReturn(Optional.of(profile));

    JobApplicationResponse result =
            service.getDetail(100L);

    assertNotNull(result);

    assertEquals(
            "0123456789",
            result.getPhone()
    );

    assertEquals(
            "Excellent",
            result.getAiEvaluation()
    );

    assertEquals(
            90,
            result.getMatchScore()
    );

    verify(appRepo).findById(100L);
}

// TC3.11 - Apply Using Profile CV
@Test
void TC311_applyUsingProfileCv() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    CandidateProfile profile = new CandidateProfile();
    profile.setCvFilePath("profile_cv.pdf");

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl(null);

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L,10L))
            .thenReturn(false);

    when(profileRepository.findByUserId(1L))
            .thenReturn(Optional.of(profile));

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    JobApplication result =
            service.apply(1L,request);

    assertEquals("profile_cv.pdf",result.getCvUrl());

    verify(profileRepository).findByUserId(1L);
}

// TC3.12 - Apply Using Uploaded CV
@Test
void TC312_applyUsingUploadedCv() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl("uploaded_cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L,10L))
            .thenReturn(false);

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    JobApplication result =
            service.apply(1L,request);

    assertEquals("uploaded_cv.pdf",result.getCvUrl());

    verify(appRepo).save(any(JobApplication.class));
}

// TC3.13 - Publish Point Event
@Test
void TC313_publishPointEventAfterApply() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl("cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L,10L))
            .thenReturn(false);

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> {

                JobApplication app = invocation.getArgument(0);
                app.setId(100L);
                return app;

            });

    service.apply(1L,request);

    verify(eventPublisher,times(1))
            .publishEvent(any());
}

// TC3.14 - Send Notification After Apply
@Test
void TC314_sendNotificationAfterApply() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl("cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L,10L))
            .thenReturn(false);

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> {

                JobApplication app = invocation.getArgument(0);
                app.setId(100L);
                return app;

            });

    service.apply(1L,request);

    verify(notificationService)
            .sendNotification(
                    anyLong(),
                    anyString(),
                    anyString(),
                    anyString()
            );
}

// TC3.15 - Default Match Score
@Test
void TC315_defaultMatchScoreWhenNoAnalysisExists() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl("cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L,10L))
            .thenReturn(false);

    when(analysisResultRepo.findByUserIdAndJobPostingId(1L,10L))
            .thenReturn(Optional.empty());

    when(appRepo.save(any(JobApplication.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    JobApplication result =
            service.apply(1L,request);

    assertEquals(0,result.getMatchScore());

    verify(analysisResultRepo)
            .findByUserIdAndJobPostingId(1L,10L);
}

// TC3.16 - Apply duplicated job
@Test
void TC316_applyDuplicateJobShouldThrowException() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl("cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    // Candidate already applied
    when(appRepo.existsByCandidateIdAndJobPostingId(1L, 10L))
            .thenReturn(true);

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.apply(1L, request)
    );

    assertEquals("Bạn đã ứng tuyển công việc này rồi.", exception.getMessage());

    verify(appRepo, never()).save(any(JobApplication.class));
}

@Test
void TC317_applyInvalidJobId() {

    User candidate = User.builder()
            .id(1L)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(999L);
    request.setCvUrl("cv.pdf");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(999L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> service.apply(1L, request)
    );
}

@Test
void TC318_applyInvalidCandidateId() {

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);

    when(userRepository.findById(999L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> service.apply(999L, request)
    );
}

// TC3.19 - Apply without uploaded CV and profile has no CV
@Test
void TC319_applyWithoutUploadedCvAndProfileHasNoCv() {

    User candidate = User.builder()
            .id(1L)
            .fullName("Kiet")
            .build();

    User recruiter = User.builder()
            .id(2L)
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplicationRequest request = new JobApplicationRequest();
    request.setJobId(10L);
    request.setCvUrl(null); // Candidate does not upload CV

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(candidate));

    when(jobRepo.findById(10L))
            .thenReturn(Optional.of(job));

    when(appRepo.existsByCandidateIdAndJobPostingId(1L, 10L))
            .thenReturn(false);

    // Candidate profile exists but has no CV
    CandidateProfile profile = new CandidateProfile();
    profile.setCvFilePath(null);

    when(profileRepository.findByUserId(1L))
            .thenReturn(Optional.of(profile));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.apply(1L, request)
    );

    assertEquals(
            "Vui lòng upload CV hoặc cập nhật hồ sơ trước khi ứng tuyển.",
            exception.getMessage()
    );

    verify(appRepo, never()).save(any(JobApplication.class));
}

// TC3.20 - Recruiter without permission cannot update application status
@Test
void TC320_updateStatusByUnauthorizedRecruiterShouldThrowException() {

    User recruiter = User.builder()
            .id(2L)
            .fullName("Recruiter A")
            .build();

    JobPosting job = JobPosting.builder()
            .id(10L)
            .title("Java Developer")
            .recruiter(recruiter)
            .build();

    JobApplication application = JobApplication.builder()
            .id(100L)
            .jobPosting(job)
            .build();

    when(appRepo.findById(100L))
            .thenReturn(Optional.of(application));

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateStatus(
                    99L, // Recruiter khác, không phải người đăng job
                    100L,
                    ApplicationStatus.INTERVIEW,
                    "Interview candidate"
            )
    );

    assertEquals(
            "Không có quyền chỉnh sửa đơn ứng tuyển này.",
            exception.getMessage()
    );

    verify(appRepo, never()).save(any(JobApplication.class));
}

@Test
void TC321_updateStatusApplicationNotFound() {

    when(appRepo.findById(999L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> service.updateStatus(
                    1L,
                    999L,
                    ApplicationStatus.INTERVIEW,
                    "")
    );
}

@Test
void TC322_getDetailApplicationNotFound() {

    when(appRepo.findById(999L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> service.getDetail(999L)
    );
}

@Test
void TC323_hasAppliedFalse() {

    when(appRepo.existsByCandidateIdAndJobPostingId(1L,10L))
            .thenReturn(false);

    assertFalse(service.hasApplied(1L,10L));
}

@Test
void TC324_getApplicationsEmpty() {

    when(appRepo.findByCandidateId(1L))
            .thenReturn(List.of());

    List<JobApplicationResponse> result =
            service.getApplicationsByCandidateId(1L);

    assertTrue(result.isEmpty());
}

@Test
void TC325_listApplicantsEmpty() {

    when(jobRepo.existsById(10L))
            .thenReturn(true);

    when(appRepo.findByJobPostingId(10L))
            .thenReturn(List.of());

    List<JobApplicationResponse> result =
            service.listByJob(10L);

    assertTrue(result.isEmpty());
}
}
