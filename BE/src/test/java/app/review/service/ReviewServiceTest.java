package app.review.service;

import app.auth.model.User;
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
}