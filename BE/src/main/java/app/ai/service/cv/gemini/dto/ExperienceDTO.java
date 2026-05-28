package app.ai.service.cv.gemini.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperienceDTO {
    private String company;
    private String role;
    private String description;
    private String startDate; // AI trả về định dạng YYYY-MM
    private String endDate;   // AI trả về định dạng YYYY-MM hoặc Present
}