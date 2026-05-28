package app.candidate.model;

import app.auth.model.User;
import app.ai.models.Experience;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    private String avatarUrl;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String linkedInUrl;
    private String websiteUrl;
    
    @Column(columnDefinition = "TEXT")
    private String aboutMe;

    @Column(columnDefinition = "TEXT")
    private String cvFilePath;

    @ElementCollection
    @CollectionTable(name = "candidate_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill_name")
    @JsonIgnore
    private List<String> skills;
    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Experience> experiences;

    @Column(columnDefinition = "TEXT") 
    private String educationJson; 
}