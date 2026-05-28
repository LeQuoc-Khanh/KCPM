package app.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    
    private Long studentId;
    private String studentName;

    private String email;
    private String phone;

    private String companyName;
    private String cvUrl;
    
    private String status;
    
    private LocalDateTime appliedAt;
    private String recruiterNote;

    // --- CÁC TRƯỜNG AI ---
    private Integer matchScore;
    private String aiEvaluation;
    
    private String missingSkillsList; 
    private String matchedSkillsList;
    private String otherHardSkillsList;
    private String otherSoftSkillsList;

    private Integer matchedSkillsCount;
    private Integer missingSkillsCount;
    private Integer otherHardSkillsCount;
    private Integer otherSoftSkillsCount;
}