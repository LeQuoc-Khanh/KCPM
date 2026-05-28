package app.recruitment.entity;

import app.auth.model.User;
import app.content.model.Company; // Nhớ import Company từ module content
import app.recruitment.entity.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- THÔNG TIN HIỂN THỊ VÀ AI ---
    @Column(nullable = false)
    private String title; // Vd: Senior Java Developer

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description; // AI sẽ đọc cái này để so sánh

    @Column(columnDefinition = "TEXT")
    private String requirements; // AI sẽ đọc thêm cái này (nếu có)

    private String salaryRange;
    private String location;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_posting_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill_name")
    @Builder.Default
    private List<String> extractedSkills = new ArrayList<>();
    // --- QUẢN LÝ TRẠNG THÁI ---
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.DRAFT;

    private LocalDateTime expiryDate; // Ngày hết hạn tin tuyển dụng

    // --- QUAN HỆ ---
    
    // Người đăng tin (Recruiter) - Để quản lý quyền sửa/xóa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    // Tin này thuộc công ty nào - Để hiển thị Logo, Tên cty
    // (Lấy từ bảng Job cũ sang)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // --- AUDIT ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}