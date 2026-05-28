package app.candidate.dto.response;

import app.ai.service.cv.gemini.dto.ExperienceDTO;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CandidateProfileResponse {
    private Long id;
    private String avatarUrl;
    private String userFullName; // Trả về tên tài khoản để hiển thị/sửa
    private String fullName;     // Tên hồ sơ
    private String email;
    private String phoneNumber;
    private String address;
    private String aboutMe;
    private String linkedInUrl;
    private String websiteUrl;
    private String cvFilePath;

    private List<String> skills;
    private List<ExperienceDTO> experiences; 
}