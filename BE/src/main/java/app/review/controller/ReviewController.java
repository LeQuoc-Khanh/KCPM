package app.review.controller;

import app.review.dto.ReviewRequest;
import app.review.service.ReviewService;
import app.util.SecurityUtils; // Sử dụng file utility có sẵn của bạn
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // API gửi đánh giá (Yêu cầu đăng nhập)
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewRequest request) {
        // Lấy thông tin User hiện tại từ Security Context
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        app.auth.security.UserPrincipal principal = (app.auth.security.UserPrincipal) auth.getPrincipal();
        Long userId = principal.getId(); // Lấy ID người dùng thật

        return ResponseEntity.ok(reviewService.addReview(userId, request));
    }

    // API lấy danh sách đánh giá (Public)
    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getCompanyReviews(@PathVariable Long companyId) {
        return ResponseEntity.ok(reviewService.getReviewsByCompany(companyId));
    }

    // API lấy điểm trung bình (Public)
    @GetMapping("/company/{companyId}/average")
    public ResponseEntity<?> getAverageRating(@PathVariable Long companyId) {
        return ResponseEntity.ok(reviewService.getAverageRating(companyId));
    }
}