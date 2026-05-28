package app.recruitment.entity;

import app.auth.model.User;
import app.recruitment.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 1. ĐỊNH DANH (Ai nộp? Nộp job nào?) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false) 
    private JobPosting jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate; // Chứa User ID và thông tin cá nhân

    // --- 2. HỒ SƠ GỐC (Snapshot) ---
    @Column(length = 1000)
    private String cvUrl; // Link CV (Quan trọng nhất)

    @Column(columnDefinition = "TEXT")
    private String coverLetter; // Thư xin việc

    // --- 3. KẾT QUẢ AI (Dành riêng cho Recruiter lọc) ---
    
    @Column(name = "match_score")
    private Integer matchScore; // Điểm số (0-100) -> Để sắp xếp

    @Column(columnDefinition = "TEXT")
    private String aiEvaluation; // Nhận xét tóm tắt -> Để đọc nhanh

    // Bộ đếm -> Để lọc (VD: Lọc ông nào thiếu ít hơn 2 skill)
    private Integer matchedSkillsCount;  
    private Integer missingSkillsCount;  
    private Integer otherHardSkillsCount;  
    private Integer otherSoftSkillsCount;  

    // Danh sách skill thiếu -> Hiển thị ngay trên bảng để Recruiter biết ứng viên hổng kiến thức gì
    @Column(columnDefinition = "TEXT")
    private String missingSkillsList; 
    // Danh sách skill đáp ứng -> Hiển thị ngay trên bảng để Recruiter biết ứng viên đáp ứng những gì
    @Column(columnDefinition = "TEXT")
    private String matchedSkillsList; 
    // Danh sách skill chuyên môn khác -> Hiển thị ngay trên bảng để Recruiter biết ứng viên đáp ứng những gì
    @Column(columnDefinition = "TEXT")
    private String otherHardSkillsList; 
    // Danh sách skill mềm khác -> Hiển thị ngay trên bảng để Recruiter biết ứng viên đáp ứng những gì
    @Column(columnDefinition = "TEXT")
    private String otherSoftSkillsList; 

    // --- 4. TRẠNG THÁI ---
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String recruiterNote; // Ghi chú nội bộ của Recruiter

    // --- 5. THỜI GIAN ---
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        if (status == null) status = ApplicationStatus.PENDING;
        if (matchScore == null) matchScore = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}