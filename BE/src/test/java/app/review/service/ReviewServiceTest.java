package app.review.service;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.notification.service.NotificationService;
import app.review.dto.ReviewRequest;
import app.review.dto.ReviewResponse;
import app.review.entity.CompanyReview;
import app.review.repository.CompanyReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private CompanyReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void addReview_shouldThrowException_whenUserNotFound() {
        Long userId = 999L;
        Long companyId = 100L;
        ReviewRequest request = createRequest(companyId, 5, "Tốt");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(userId, request));

        assertEquals("User not found", exception.getMessage());
        verify(companyRepository, never()).findById(anyLong());
        verify(reviewRepository, never()).save(any(CompanyReview.class));
    }

    @Test
    void addReview_shouldThrowException_whenCompanyNotFound() {
        Long userId = 1L;
        Long companyId = 999L;
        ReviewRequest request = createRequest(companyId, 5, "Tốt");

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(userId, request));

        assertEquals("Company not found", exception.getMessage());
        verify(reviewRepository, never()).save(any(CompanyReview.class));
    }

    @Test
    void addReview_shouldThrowException_whenUserAlreadyReviewedCompany() {
        Long userId = 1L;
        Long companyId = 100L;
        ReviewRequest request = createRequest(companyId, 5, "Tốt");

        User user = mock(User.class);
        Company company = mock(Company.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(reviewRepository.existsByUserIdAndCompanyId(userId, companyId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reviewService.addReview(userId, request));

        assertEquals("Bạn đã đánh giá công ty này rồi!", exception.getMessage());
        verify(reviewRepository, never()).save(any(CompanyReview.class));
        verify(notificationService, never()).sendNotification(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void addReview_validRequest_savesReviewMapsResponseAndNotifiesRecruiter() {
        Long userId = 1L;
        Long companyId = 100L;
        ReviewRequest request = createRequest(companyId, 5, "Excellent hiring process");
        User reviewer = createUser(userId, "Feature7 Reviewer", "feature7-reviewer@example.com", UserRole.CANDIDATE);
        User recruiter = createUser(2L, "Feature7 Recruiter", "feature7-recruiter@example.com", UserRole.RECRUITER);
        Company company = Company.builder()
                .id(companyId)
                .name("Feature7 Company")
                .recruiter(recruiter)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(reviewer));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(reviewRepository.existsByUserIdAndCompanyId(userId, companyId)).thenReturn(false);
        when(reviewRepository.save(any(CompanyReview.class))).thenAnswer(invocation -> {
            CompanyReview review = invocation.getArgument(0);
            review.setId(10L);
            review.setCreatedAt(LocalDateTime.of(2026, 7, 11, 10, 0));
            return review;
        });

        ReviewResponse response = reviewService.addReview(userId, request);

        assertEquals(10L, response.getId());
        assertEquals(5, response.getRating());
        assertEquals("Excellent hiring process", response.getComment());
        assertEquals("Feature7 Reviewer", response.getReviewerName());
        assertEquals("avatar-feature7-reviewer@example.com", response.getReviewerAvatar());

        ArgumentCaptor<CompanyReview> reviewCaptor = ArgumentCaptor.forClass(CompanyReview.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        CompanyReview savedReview = reviewCaptor.getValue();
        assertEquals(reviewer, savedReview.getUser());
        assertEquals(company, savedReview.getCompany());
        assertEquals(5, savedReview.getRating());
        assertEquals("Excellent hiring process", savedReview.getComment());

        verify(notificationService).sendNotification(
                eq(recruiter.getId()),
                contains("5"),
                contains("Feature7 Reviewer"),
                eq("/recruiter/company/reviews")
        );
    }

    @Test
    void addReview_companyWithoutRecruiter_savesReviewWithoutSendingNotification() {
        Long userId = 1L;
        Long companyId = 100L;
        ReviewRequest request = createRequest(companyId, 4, "Good company");
        User reviewer = createUser(userId, "Candidate", "candidate@example.com", UserRole.CANDIDATE);
        Company company = Company.builder()
                .id(companyId)
                .name("Company Without Recruiter")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(reviewer));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(reviewRepository.existsByUserIdAndCompanyId(userId, companyId)).thenReturn(false);
        when(reviewRepository.save(any(CompanyReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.addReview(userId, request);

        assertEquals(4, response.getRating());
        assertEquals("Good company", response.getComment());
        verify(notificationService, never()).sendNotification(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void getReviewsByCompany_existingReviews_mapsReviewResponses() {
        Long companyId = 100L;
        User reviewer = createUser(1L, "Mapped Reviewer", "mapped@example.com", UserRole.CANDIDATE);
        Company company = Company.builder()
                .id(companyId)
                .name("Mapped Company")
                .build();
        CompanyReview review = CompanyReview.builder()
                .id(11L)
                .rating(3)
                .comment("Average process")
                .user(reviewer)
                .company(company)
                .createdAt(LocalDateTime.of(2026, 7, 11, 9, 30))
                .build();
        when(reviewRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)).thenReturn(List.of(review));

        List<ReviewResponse> responses = reviewService.getReviewsByCompany(companyId);

        assertEquals(1, responses.size());
        ReviewResponse response = responses.get(0);
        assertEquals(11L, response.getId());
        assertEquals(3, response.getRating());
        assertEquals("Average process", response.getComment());
        assertEquals("Mapped Reviewer", response.getReviewerName());
        assertEquals("avatar-mapped@example.com", response.getReviewerAvatar());
    }

    @Test
    void getAverageRating_shouldReturnZero_whenRepositoryReturnsNull() {
        Long companyId = 100L;
        when(reviewRepository.getAverageRatingByCompanyId(companyId)).thenReturn(null);

        Double result = reviewService.getAverageRating(companyId);

        assertEquals(0.0, result);
    }

    @Test
    void getAverageRating_shouldRoundToOneDecimalPlace() {
        Long companyId = 100L;
        when(reviewRepository.getAverageRatingByCompanyId(companyId)).thenReturn(4.26);

        Double result = reviewService.getAverageRating(companyId);

        assertEquals(4.3, result);
    }

    private ReviewRequest createRequest(Long companyId, Integer rating, String comment) {
        ReviewRequest request = new ReviewRequest();
        request.setCompanyId(companyId);
        request.setRating(rating);
        request.setComment(comment);
        return request;
    }

    private User createUser(Long id, String fullName, String email, UserRole role) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .password("{noop}password")
                .userRole(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .profileImageUrl("avatar-" + email)
                .build();
    }
}
