package app.review.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewDtoTest {

    @Test
    void reviewRequest_shouldSetAndGetFields() {
        ReviewRequest request = new ReviewRequest();
        request.setCompanyId(100L);
        request.setRating(5);
        request.setComment("Công ty tốt");

        assertEquals(100L, request.getCompanyId());
        assertEquals(5, request.getRating());
        assertEquals("Công ty tốt", request.getComment());
    }

    @Test
    void reviewResponseBuilder_shouldBuildResponseWithAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 25, 12, 0);

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .rating(4)
                .comment("Ổn")
                .reviewerName("Nguyễn Văn A")
                .reviewerAvatar("avatar.png")
                .createdAt(createdAt)
                .build();

        assertEquals(1L, response.getId());
        assertEquals(4, response.getRating());
        assertEquals("Ổn", response.getComment());
        assertEquals("Nguyễn Văn A", response.getReviewerName());
        assertEquals("avatar.png", response.getReviewerAvatar());
        assertEquals(createdAt, response.getCreatedAt());
    }
}
