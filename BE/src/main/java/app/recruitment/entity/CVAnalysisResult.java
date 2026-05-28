package app.recruitment.entity;

import app.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "cv_analysis_results", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "job_posting_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CVAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    // --- LIGHT DATA (Dữ liệu nhẹ - Load nhanh) ---
    @Column(name = "match_percentage")
    private int matchPercentage;

    // --- HEAVY DATA (Dữ liệu nặng - Chỉ load khi cần) ---
    // @Lob: Báo hiệu đây là văn bản lớn (Large Object)
    // FetchType.LAZY: Hibernate sẽ cố gắng không load cột này nếu không được gọi
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "analysis_details", columnDefinition = "TEXT") 
    private String analysisDetails; // JSON chi tiết từ AI

    @Column(name = "cv_url")
    private String cvUrlUsed; // Lưu lại URL CV lúc phân tích để so sánh version

    @CreatedDate
    @Column(name = "analyzed_at", updatable = false)
    private LocalDateTime analyzedAt;
}