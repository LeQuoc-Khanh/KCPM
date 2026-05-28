package app.ai.dto;


import lombok.Data;
import java.util.List;

@Data
public class InterviewChatRequest {
    private String message; // Câu nói mới nhất của User
    private List<MessageItem> history; // Lịch sử chat (Frontend gửi lên)

    @Data
    public static class MessageItem {
        private String sender; // "USER" hoặc "AI"
        private String content;
    }
}
