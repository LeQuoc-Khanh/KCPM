package app.review.repository;

import app.review.entity.CompanyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyReviewRepository extends JpaRepository<CompanyReview, Long> {
    
    // Lấy danh sách đánh giá theo công ty, mới nhất lên đầu
    List<CompanyReview> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    // Kiểm tra user đã đánh giá chưa
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    // Tính điểm trung bình
    @Query("SELECT AVG(r.rating) FROM CompanyReview r WHERE r.company.id = :companyId")
    Double getAverageRatingByCompanyId(Long companyId);
}