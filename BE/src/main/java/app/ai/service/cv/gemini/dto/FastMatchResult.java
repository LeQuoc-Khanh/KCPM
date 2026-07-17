package app.ai.service.cv.gemini.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FastMatchResult {
    private int matchScore;              // Điểm số (85)
    private List<String> matchedSkills;  // List skill trùng (Màu xanh)
    private List<String> missingSkills;  // List skill thiếu (Màu đỏ - Optional)
}
