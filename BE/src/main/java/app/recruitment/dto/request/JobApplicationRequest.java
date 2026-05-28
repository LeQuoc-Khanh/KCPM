package app.recruitment.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class JobApplicationRequest {
    @NotNull
    private Long jobId;
    
    // Có thể bỏ @NotNull nếu cho phép hệ thống tự lấy từ Profile
    private String cvUrl; 

    private String coverLetter; 
    private Integer matchScore;
    private String aiEvaluation;
    private Integer matchedSkillsCount;
    private Integer missingSkillsCount;
    private Integer extraSkillsCount;
    private String missingSkillsList;
}