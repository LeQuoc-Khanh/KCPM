package app.content.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import app.auth.model.User;
import app.recruitment.entity.JobPosting;

@Entity
@Table(name = "companies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private String logoUrl = "https://res.cloudinary.com/dpym64zg9/image/upload/v1769752122/avatar_Company_guzrau.jpg";

    @Builder.Default
    private String coverImageUrl = "https://res.cloudinary.com/dpym64zg9/image/upload/v1769752140/cover_company_rydzha.jpg";
    private String website;

    private String industry;      // Ngành nghề
    private String size;          // Quy mô (10-50, etc.)
    private String foundedYear;   // Năm thành lập
    private String address;       // Địa chỉ
    private String phone;         // Số điện thoại công ty
    private String email;         // Email liên hệ công ty
    // Liên kết với User (Recruiter quản lý công ty này)
    @OneToOne
    @JoinColumn(name = "recruiter_id")
    @JsonIgnore
    private User recruiter;
    
    // Một công ty có nhiều Job
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<JobPosting> jobs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}