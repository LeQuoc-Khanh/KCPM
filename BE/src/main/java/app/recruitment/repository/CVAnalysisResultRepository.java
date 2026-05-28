package app.recruitment.repository;

import app.recruitment.entity.CVAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CVAnalysisResultRepository extends JpaRepository<CVAnalysisResult, Long> {
    
    // Tìm kết quả đã phân tích của User với 1 Job cụ thể
    Optional<CVAnalysisResult> findByUserIdAndJobPostingId(Long userId, Long jobPostingId);

    // [MỚI] Hàm xóa sạch lịch sử phân tích cũ của User
    // Để khi họ update CV, hệ thống buộc phải tính điểm lại từ đầu
    void deleteByUserId(Long userId);
}