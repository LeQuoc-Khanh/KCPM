package app.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ✅ ĐÂY LÀ POJO (Không còn @Entity, @Table, @Id)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewMessage {
    private String sender;  // "USER" hoặc "AI"
    private String content; // Nội dung
    
    // Bạn có thể thêm timestamp nếu muốn hiển thị giờ trong log
    // private LocalDateTime sentAt; 
}