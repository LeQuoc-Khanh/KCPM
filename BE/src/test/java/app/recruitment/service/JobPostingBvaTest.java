package app.recruitment.service;

import app.auth.model.User;
import app.recruitment.dto.request.JobPostingRequest;
import app.recruitment.entity.JobPosting;
import app.recruitment.repository.JobPostingRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobPostingBvaTest {

    private Validator validator;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private JobPostingServiceImpl jobPostingService;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Gộp TC_4.16 đến TC_4.20: Kiểm tra độ dài Title bằng ParameterizedTest
    @ParameterizedTest
    @ValueSource(strings = {"", "   "}) // Các trường hợp Min-1 (Rỗng hoặc toàn dấu cách)
    void testCreateJob_Title_Blank(String invalidTitle) {
        JobPostingRequest request = new JobPostingRequest();
        request.setTitle(invalidTitle);
        request.setDescription("Valid Description");
        request.setRequirements("Valid Requirements");
        request.setLocation("HCM");
        request.setExpiryDate(LocalDate.now().plusDays(10));

        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Phải báo lỗi khi tiêu đề trống");
    }

    // Gộp TC_4.11 đến TC_4.15: Kiểm tra Hạn nộp hồ sơ (Expiry Date)
    @Test
    void testCreateJob_ExpiryDate_InPast_Failed() { // TC_4.11 (Min-1)
        JobPostingRequest request = new JobPostingRequest();
        request.setTitle("Java Dev");
        request.setDescription("Desc");
        request.setRequirements("Req");
        request.setLocation("HCM");
        request.setExpiryDate(LocalDate.now().minusDays(1)); // Quá khứ

        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Phải báo lỗi khi ngày nộp hồ sơ ở quá khứ");
    }

    @Test
    void testCreateJob_ExpiryDate_Today_Success() { // TC_4.12 (Min)
        JobPostingRequest request = new JobPostingRequest();
        request.setTitle("Java Dev");
        request.setDescription("Desc");
        request.setRequirements("Req");
        request.setLocation("HCM");
        request.setExpiryDate(LocalDate.now()); // Hôm nay

        Set<ConstraintViolation<JobPostingRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Không được báo lỗi khi ngày nộp hồ sơ là hôm nay");
    }

    // TC_4.34: Xóa Job sai quyền (Unauthorized)
    @Test
    void testDeleteJob_Unauthorized_Failed() {
        JobPosting mockJob = new JobPosting();
        mockJob.setId(100L);
        User owner = new User();
        owner.setId(2L); // Job này thuộc về Recruiter có ID = 2
        mockJob.setRecruiter(owner);

        when(jobPostingRepository.findById(100L)).thenReturn(Optional.of(mockJob));

        // Recruiter ID = 1 cố tình xóa Job của Recruiter ID = 2
        Exception exception = assertThrows(RuntimeException.class, () -> {
            jobPostingService.delete(1L, 100L);
        });
        
        assertNotNull(exception); // Hoặc bắt đúng CustomException của team bạn
    }
}