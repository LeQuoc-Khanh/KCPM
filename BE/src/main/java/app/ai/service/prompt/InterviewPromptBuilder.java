package app.ai.service.prompt;

import app.ai.dto.InterviewMessage;
import app.recruitment.entity.JobPosting;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class InterviewPromptBuilder {

    /**
     * Prompt 1: Khởi động phỏng vấn (AI tự chào)
     */
    public String buildStartPrompt(String companyName, String jobTitle, String candidateName) {
        return String.format(
            "Bạn là HR chuyên nghiệp tại công ty %s. Bạn đang phỏng vấn ứng viên tên %s cho vị trí %s.\n" +
            "Nhiệm vụ: Hãy bắt đầu buổi phỏng vấn bằng một lời chào lịch sự, thân thiện và yêu cầu ứng viên giới thiệu ngắn gọn về bản thân.\n" +
            "Yêu cầu output: Chỉ đưa ra lời chào (text), không kèm JSON hay giải thích gì thêm.",
            companyName, candidateName, jobTitle
        );
    }

    /**
     * Prompt 2: Hội thoại (Chat) - Kèm lịch sử và ngữ cảnh
     */
    public String buildChatPrompt(JobPosting job, List<InterviewMessage> history) {
        StringBuilder sb = new StringBuilder();

        // A. System Instruction (Nhắc lại vai trò để AI không bị "quên")
        sb.append("--- NGỮ CẢNH ---\n")
          .append("Vai trò: Bạn là Nhà tuyển dụng (Interviewer) tại ").append(job.getCompany() != null ? job.getCompany().getName() : "Công ty ẩn danh").append(".\n")
          .append("Vị trí tuyển dụng: ").append(job.getTitle()).append(".\n")
          .append("Mô tả công việc (JD): ").append(job.getDescription()).append("\n")
          .append("Yêu cầu: ").append(job.getRequirements()).append("\n\n")
          .append("QUY TẮC:\n")
          .append("1. Luôn đóng vai HR, không được thoát vai.\n")
          .append("2. Chỉ hỏi MỘT câu hỏi mỗi lượt. Đừng hỏi dồn.\n")
          .append("3. Câu hỏi cần xoáy sâu vào kinh nghiệm thực tế liên quan đến JD.\n")
          .append("4. Giọng điệu chuyên nghiệp nhưng cởi mở.\n\n")
          .append("5. KIỂM SOÁT: Nếu bạn thấy đã hỏi đủ thông tin (khoảng 5-7 câu hỏi) HOẶC ứng viên chào tạm biệt, hãy kết thúc phỏng vấn.\n")
          .append("6. BẮT BUỘC: Khi kết thúc, hãy nói lời cảm ơn và thêm ký hiệu '[[END_INTERVIEW]]' vào CUỐI CÙNG câu trả lời để hệ thống nhận biết.\n\n");
        // B. Lịch sử chat (Context Memory)
        sb.append("--- LỊCH SỬ HỘI THOẠI ---\n");
        // Lấy tối đa 20 tin nhắn gần nhất để tránh lỗi quá dài (Token limit)
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            InterviewMessage msg = history.get(i);
            String role = "USER".equalsIgnoreCase(msg.getSender()) ? "Candidate" : "Interviewer";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        // C. Trigger AI trả lời
        sb.append("Interviewer (Tiếp tục phỏng vấn):");
        
        return sb.toString();
    }

    /**
     * Prompt 3: Kết thúc & Chấm điểm
     */
    public String buildGradingPrompt(List<InterviewMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (InterviewMessage msg : history) {
            sb.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
        }

        return """
            Dựa trên nội dung cuộc phỏng vấn trên, hãy đóng vai Quản lý tuyển dụng (Hiring Manager) để đánh giá ứng viên.
            
            YÊU CẦU OUTPUT:
            Trả về DUY NHẤT một JSON hợp lệ theo định dạng sau (không Markdown, không giải thích thêm):
            {
                "score": (số nguyên từ 0-100),
                "feedback": "Nhận xét chi tiết về điểm mạnh, điểm yếu và khả năng phù hợp với công việc..."
            }
            
            NỘI DUNG PHỎNG VẤN:
            """ + sb.toString();
    }
}