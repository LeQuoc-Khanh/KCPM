package app.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationsByDayResponse {
    private String date; // "T2".."CN" hoặc "2024-01-15"
    private Long count;
}
