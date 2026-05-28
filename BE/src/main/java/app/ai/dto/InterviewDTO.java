package app.ai.dto; 

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InterviewDTO {
    private Long id;
    private String status;
    private Integer score;
    private String feedback;
    
    // 👇 THÊM CÁI NÀY: Để hiển thị ngày tháng trên danh sách lịch sử
    private LocalDateTime createdAt; 
    
    // Thông tin Job
    private Long jobId;
    private String jobTitle;
    private String companyName;

    // Thông tin Ứng viên
    private Long candidateId;
    private String candidateName;

    // Danh sách tin nhắn (Có thể null nếu xem history)
    private List<MessageDTO> messages;

    @Data
    @Builder
    public static class MessageDTO {
        private String sender;  
        private String content;
        private LocalDateTime sentAt;
    }
}