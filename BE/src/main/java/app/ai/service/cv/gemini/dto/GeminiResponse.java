package app.ai.service.cv.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {
    private ContactDTO contact;
    private List<ExperienceDTO> experiences;
    private List<String> skills;
    private String aboutMe;
}