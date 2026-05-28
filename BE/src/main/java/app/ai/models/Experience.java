package app.ai.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import app.candidate.model.CandidateProfile; // Import mới
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "experiences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Experience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;
    private String role;
    private String startDate;
    private String endDate;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    // --- SỬA Ở ĐÂY ---
    // Đổi từ Candidate sang CandidateProfile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id") 
    @JsonIgnore
    private CandidateProfile candidateProfile;
}