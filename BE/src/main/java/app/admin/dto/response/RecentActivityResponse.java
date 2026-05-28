package app.admin.dto.response;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String message;     // "Nguyễn Văn A vừa ứng tuyển vào Viettel"
    private String timeAgo;     // "2 phút trước"
    private Instant createdAt;  // optional để FE tự tính về sau
    private Long refId;         // jobApplicationId (optional)
}