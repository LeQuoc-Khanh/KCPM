package app.ai.service.cv.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResult {
    // Thống kê cơ bản
    private int matchPercentage;
    private int totalRequiredSkills;
    private String jobTitle;
    private String company;
    private String candidateName;
    private List<ExperienceDTO> candidateExperiences;

    // --- 5 CỘT KỸ NĂNG THEO YÊU CẦU ---

    
    // 1. Kỹ năng ĐÁP ỨNG (Job cần + CV có) - Cả Cứng & Mềm
    private int matchedSkillsCount;
    @Builder.Default
    private List<String> matchedSkillsList = new ArrayList<>();

    // 2. Kỹ năng THIẾU (Job cần + CV không có) - Cả Cứng & Mềm
    private int missingSkillsCount;
    @Builder.Default
    private List<String> missingSkillsList = new ArrayList<>();
    
    // 3. Kỹ năng CHUYÊN MÔN KHÁC (CV có + Job không cần) - Chỉ Hard Skills
    private int otherHardSkillsCount;
    @Builder.Default
    private List<String> otherHardSkillsList = new ArrayList<>();   

    // 4. Kỹ năng MỀM KHÁC (CV có + Job không cần) - Chỉ Soft Skills
    private int otherSoftSkillsCount;
    @Builder.Default
    private List<String> otherSoftSkillsList = new ArrayList<>(); 

    // 5. Kỹ năng GỢI Ý THÊM (Cả Job & CV đều không có, AI đề xuất cho thực tế)
    private int recommendedSkillsCount;
    @Builder.Default
    private List<String> recommendedSkillsList = new ArrayList<>();

    // Nội dung tư vấn
    private String evaluation; 
    private String learningPath; 
    private String careerAdvice; 
}