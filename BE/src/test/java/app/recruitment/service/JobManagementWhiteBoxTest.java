package app.recruitment.service;

import app.ai.service.cv.gemini.GeminiService;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.recruitment.dto.request.JobPostingRequest;
import app.recruitment.dto.response.JobPostingResponse;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.mapper.RecruitmentMapper;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobManagementWhiteBoxTest {

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

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void UT01_create_success_withValidRecruiter_shouldSavePendingJob() throws Exception {
        User recruiter = user(1L, UserRole.RECRUITER);
        Company company = company(10L, recruiter);
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));

        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(companyRepository.findByRecruiterId(1L)).thenReturn(Optional.of(company));
        when(geminiService.extractSkillsFromJob(request.getDescription(), request.getRequirements()))
                .thenReturn(List.of("Java", "Spring Boot"));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(invocation -> {
            JobPosting job = invocation.getArgument(0);
            job.setId(100L);
            return job;
        });

        JobPosting result = service.create(1L, request);

        assertEquals(100L, result.getId());
        assertEquals(JobStatus.PENDING, result.getStatus());
        assertEquals(recruiter, result.getRecruiter());
        assertEquals(company, result.getCompany());
        assertEquals(List.of("Java", "Spring Boot"), result.getExtractedSkills());
    }

    @Test
    void UT02_create_success_withRecruiterVip_shouldSavePendingJob() {
        User recruiterVip = user(2L, UserRole.RECRUITER_VIP);
        Company company = company(20L, recruiterVip);
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));

        when(userRepository.findById(2L)).thenReturn(Optional.of(recruiterVip));
        when(companyRepository.findByRecruiterId(2L)).thenReturn(Optional.of(company));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobPosting result = service.create(2L, request);

        assertEquals(JobStatus.PENDING, result.getStatus());
        assertEquals(recruiterVip, result.getRecruiter());
    }

    @Test
    void UT03_create_shouldAcceptExpiryDateToday() {
        JobPostingRequest request = validRequest(LocalDate.now());

        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void UT04_create_shouldAcceptExpiryDateTomorrow() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void UT05_create_shouldRejectExpiryDateYesterday() {
        JobPostingRequest request = validRequest(LocalDate.now().minusDays(1));

        assertHasViolationOnField(request, "expiryDate");
    }

    @Test
    void UT06_create_shouldRejectNullTitle() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setTitle(null);

        assertHasViolationOnField(request, "title");
    }

    @Test
    void UT07_create_shouldRejectBlankTitle() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setTitle("");

        assertHasViolationOnField(request, "title");
    }

    @Test
    void UT08_create_shouldRejectWhitespaceTitle() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setTitle("   ");

        assertHasViolationOnField(request, "title");
    }

    @Test
    void UT09_create_shouldRejectBlankDescription() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setDescription("");

        assertHasViolationOnField(request, "description");
    }

    @Test
    void UT10_create_shouldRejectBlankRequirements() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setRequirements("");

        assertHasViolationOnField(request, "requirements");
    }

    @Test
    void UT11_create_shouldRejectBlankLocation() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setLocation("");

        assertHasViolationOnField(request, "location");
    }

    @Test
    void UT12_create_shouldAcceptNullSalaryRange() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setSalaryRange(null);

        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void UT13_create_shouldRejectNullExpiryDate() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));
        request.setExpiryDate(null);

        assertHasViolationOnField(request, "expiryDate");
    }

    @Test
    void UT14_create_shouldThrowWhenUserIsCandidate() {
        User candidate = user(3L, UserRole.CANDIDATE);
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));

        when(userRepository.findById(3L)).thenReturn(Optional.of(candidate));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.create(3L, request));

        assertEquals("Only recruiter can create job postings", ex.getMessage());
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void UT15_update_success_shouldUpdateFieldsAndStatus() throws Exception {
        User recruiter = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, recruiter, JobStatus.PENDING);
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(45));
        request.setTitle("Updated Backend Developer");
        request.setStatus("PUBLISHED");

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(20L)).thenReturn(Optional.of(job));
        when(geminiService.extractSkillsFromJob(request.getDescription(), request.getRequirements()))
                .thenReturn(List.of("Java"));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobPosting result = service.update(1L, 20L, request);

        assertEquals("Updated Backend Developer", result.getTitle());
        assertEquals(JobStatus.PUBLISHED, result.getStatus());
        assertEquals(List.of("Java"), result.getExtractedSkills());
    }

    @Test
    void UT16_update_shouldIgnoreInvalidStatus() {
        User recruiter = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, recruiter, JobStatus.PENDING);
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(45));
        request.setStatus("INVALID_STATUS");

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(20L)).thenReturn(Optional.of(job));
        when(jobPostingRepository.save(any(JobPosting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobPosting result = service.update(1L, 20L, request);

        assertEquals(JobStatus.PENDING, result.getStatus());
    }

    @Test
    void UT17_update_shouldThrowWhenNotOwner() {
        User owner = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, owner, JobStatus.PENDING);
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(20L)).thenReturn(Optional.of(job));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(99L, 20L, request));

        assertEquals("Unauthorized: cannot edit job of another recruiter", ex.getMessage());
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void UT18_update_shouldThrowWhenJobNotFound() {
        JobPostingRequest request = validRequest(LocalDate.now().plusDays(30));

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.update(1L, 999L, request));

        assertEquals("Job not found: 999", ex.getMessage());
    }

    @Test
    void UT19_delete_success_shouldMarkJobDeleted() {
        User recruiter = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, recruiter, JobStatus.PENDING);

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(20L)).thenReturn(Optional.of(job));

        service.delete(1L, 20L);

        assertEquals(JobStatus.DELETED, job.getStatus());
        verify(jobPostingRepository).save(job);
    }

    @Test
    void UT20_delete_shouldThrowWhenNotOwner() {
        User owner = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, owner, JobStatus.PENDING);

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(20L)).thenReturn(Optional.of(job));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.delete(99L, 20L));

        assertEquals("Unauthorized: cannot delete job of another recruiter", ex.getMessage());
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void UT21_searchJobs_shouldReturnTop10WhenKeywordBlank() {
        User recruiter = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, recruiter, JobStatus.PUBLISHED);
        JobPostingResponse response = JobPostingResponse.builder().id(20L).title("Java Developer").build();

        when(jobPostingRepository.findTop10ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED))
                .thenReturn(List.of(job));
        when(recruitmentMapper.toJobPostingResponse(job)).thenReturn(response);

        List<JobPostingResponse> result = service.searchJobs("   ");

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
    }

    @Test
    void UT22_searchJobs_shouldCallSearchRepositoryWhenKeywordPresent() {
        User recruiter = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, recruiter, JobStatus.PUBLISHED);
        JobPostingResponse response = JobPostingResponse.builder().id(20L).title("Java Developer").build();

        when(jobPostingRepository.searchJobs("java")).thenReturn(List.of(job));
        when(recruitmentMapper.toJobPostingResponse(job)).thenReturn(response);

        List<JobPostingResponse> result = service.searchJobs(" java ");

        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
    }

    @Test
    void UT23_getJobDetailPublic_success_shouldReturnApplicationCount() {
        User recruiter = user(1L, UserRole.RECRUITER);
        JobPosting job = job(20L, recruiter, JobStatus.PUBLISHED);
        JobPostingResponse response = JobPostingResponse.builder().id(20L).title("Java Developer").build();

        when(jobPostingRepository.findByIdWithRecruiterAndCompany(20L)).thenReturn(Optional.of(job));
        when(recruitmentMapper.toJobPostingResponse(job)).thenReturn(response);
        when(jobApplicationRepository.countByJobPostingId(20L)).thenReturn(3L);

        JobPostingResponse result = service.getJobDetailPublic(20L);

        assertEquals(20L, result.getId());
        assertEquals(3, result.getApplicationCount());
    }

    @Test
    void UT24_getJobDetailPublic_shouldThrowWhenJobNotFound() {
        when(jobPostingRepository.findByIdWithRecruiterAndCompany(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getJobDetailPublic(999L));

        assertEquals("Job not found: 999", ex.getMessage());
    }

    private void assertHasViolationOnField(JobPostingRequest request, String field) {
        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field)));
    }

    private JobPostingRequest validRequest(LocalDate expiryDate) {
        JobPostingRequest request = new JobPostingRequest();
        request.setTitle("Java Backend Developer");
        request.setDescription("Build and maintain REST APIs using Spring Boot.");
        request.setRequirements("Java, Spring Boot, PostgreSQL.");
        request.setSalaryRange("15000000-25000000");
        request.setLocation("Ho Chi Minh City");
        request.setExpiryDate(expiryDate);
        return request;
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .fullName("Test User")
                .email("user" + id + "@example.com")
                .password("password")
                .userRole(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();
    }

    private Company company(Long id, User recruiter) {
        return Company.builder()
                .id(id)
                .name("Test Company")
                .recruiter(recruiter)
                .build();
    }

    private JobPosting job(Long id, User recruiter, JobStatus status) {
        return JobPosting.builder()
                .id(id)
                .title("Backend Developer")
                .description("Build APIs")
                .requirements("Java")
                .salaryRange("15000000-25000000")
                .location("Ho Chi Minh City")
                .expiryDate(LocalDateTime.now().plusDays(30))
                .status(status)
                .recruiter(recruiter)
                .company(company(10L, recruiter))
                .extractedSkills(Collections.emptyList())
                .build();
    }
}
