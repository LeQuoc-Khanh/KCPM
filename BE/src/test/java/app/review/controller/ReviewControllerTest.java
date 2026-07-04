package app.review.controller;

import app.auth.security.UserPrincipal;
import app.review.dto.ReviewRequest;
import app.review.dto.ReviewResponse;
import app.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReview_shouldGetCurrentUserIdAndReturnCreatedReview() {
        Long userId = 1L;
        Long companyId = 100L;

        ReviewRequest request = new ReviewRequest();
        request.setCompanyId(companyId);
        request.setRating(5);
        request.setComment("Công ty tốt");

        ReviewResponse expectedResponse = ReviewResponse.builder()
                .id(10L)
                .rating(5)
                .comment("Công ty tốt")
                .reviewerName("Nguyễn Văn A")
                .reviewerAvatar("avatar.png")
                .build();

        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = mock(UserPrincipal.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getId()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(reviewService.addReview(userId, request)).thenReturn(expectedResponse);

        ResponseEntity<?> response = reviewController.createReview(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedResponse, response.getBody());
        verify(reviewService).addReview(userId, request);
    }

    @Test
    void getCompanyReviews_shouldReturnOkAndReviewList() {
        Long companyId = 100L;
        List<ReviewResponse> expectedReviews = List.of(
                ReviewResponse.builder()
                        .id(1L)
                        .rating(4)
                        .comment("Ổn")
                        .reviewerName("Trần Thị B")
                        .build()
        );

        when(reviewService.getReviewsByCompany(companyId)).thenReturn(expectedReviews);

        ResponseEntity<?> response = reviewController.getCompanyReviews(companyId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedReviews, response.getBody());
        verify(reviewService).getReviewsByCompany(companyId);
    }

    @Test
    void getAverageRating_shouldReturnOkAndAverageRating() {
        Long companyId = 100L;
        when(reviewService.getAverageRating(companyId)).thenReturn(4.3);

        ResponseEntity<?> response = reviewController.getAverageRating(companyId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4.3, response.getBody());
        verify(reviewService).getAverageRating(companyId);
    }
}
