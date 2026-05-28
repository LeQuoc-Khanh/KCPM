package app.review.service;

import app.auth.model.User;
import app.auth.repository.CompanyRepository;
import app.auth.repository.UserRepository;
import app.content.model.Company;
import app.review.dto.ReviewRequest;
import app.review.dto.ReviewResponse;
import app.review.entity.CompanyReview;
import app.review.repository.CompanyReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.notification.service.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final CompanyReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReviewResponse addReview(Long userId, ReviewRequest request) {
        // 1. Kiểm tra User & Company
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // 2. Ràng buộc: Mỗi người chỉ đánh giá 1 lần
        if (reviewRepository.existsByUserIdAndCompanyId(userId, request.getCompanyId())) {
            throw new RuntimeException("Bạn đã đánh giá công ty này rồi!");
        }

        // 3. Tạo Entity
        CompanyReview review = CompanyReview.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .user(user)
                .company(company)
                .build();

        CompanyReview savedReview = reviewRepository.save(review);

        try {
            User recruiter = company.getRecruiter();
            if (recruiter != null) {
                String title = "Đánh giá mới: " + request.getRating() + " sao ⭐";
                String message = "Ứng viên " + user.getFullName() + " vừa đánh giá công ty " + company.getName();
                String link = "/recruiter/company/reviews"; // Link FE

                notificationService.sendNotification(recruiter.getId(), title, message, link);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo review: " + e.getMessage());
        }

        return mapToResponse(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByCompany(Long companyId) {
        return reviewRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long companyId) {
        Double avg = reviewRepository.getAverageRatingByCompanyId(companyId);
        return avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân
    }

    private ReviewResponse mapToResponse(CompanyReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewerName(review.getUser().getFullName()) // Giả sử User có field fullName
                .reviewerAvatar(review.getUser().getProfileImageUrl()) // Giả sử User có field avatarUrl
                .createdAt(review.getCreatedAt())
                .build();
    }
}