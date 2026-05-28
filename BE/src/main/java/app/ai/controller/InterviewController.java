package app.ai.controller;

import app.ai.dto.InterviewChatRequest;
import app.ai.dto.InterviewDTO;
import app.ai.models.InterviewSession;
import app.ai.service.InterviewService;
import app.auth.dto.response.MessageResponse;
import app.auth.model.User;
import app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final SecurityUtils securityUtils;

    // 1. BẮT ĐẦU: Tạo Session + Trả về lời chào
    @PostMapping("/start")
    public ResponseEntity<?> startInterview(@RequestBody Map<String, Long> request) {
        try {
            Long jobId = request.get("jobId");
            User user = securityUtils.getCurrentUser();
            
            // Tạo session DB
            InterviewSession session = interviewService.startInterview(user.getId(), jobId);
            // Lấy lời chào (text)
            String greeting = interviewService.getInitialGreeting(user.getId(), jobId);

            // Trả về DTO chứa ID session và Lời chào đầu tiên
            return ResponseEntity.ok(MessageResponse.success("Bắt đầu thành công", Map.of(
                "sessionId", session.getId(),
                "greeting", greeting
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }

    // 2. CHAT: Nhận (Msg + History) -> Trả về (AI Msg)
    @PostMapping("/{sessionId}/chat")
    public ResponseEntity<?> chat(@PathVariable Long sessionId, 
                                  @RequestBody InterviewChatRequest request) {
        try {
            String aiReply = interviewService.chat(sessionId, request.getMessage(), request.getHistory());
            return ResponseEntity.ok(MessageResponse.success("Thành công", aiReply));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }

    // 3. KẾT THÚC: Nhận (Full History) -> Chấm điểm -> Lưu DB
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endInterview(@PathVariable Long sessionId, 
                                          @RequestBody InterviewChatRequest request) { // Dùng lại DTO này để lấy history
        try {
            // Lưu ý: request.getHistory() ở đây là toàn bộ cuộc hội thoại
            InterviewSession result = interviewService.endInterview(sessionId, request.getHistory());
            
            // Convert sang DTO trả về kết quả (Điểm, Feedback)
            return ResponseEntity.ok(MessageResponse.success("Kết thúc phỏng vấn", convertToDTO(result)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }

    // 4. LẤY LỊCH SỬ (Chỉ trả về Session Info, không có message vì ko lưu)
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam Long jobId) {
        try {
            User user = securityUtils.getCurrentUser();
            
            // 👇 SỬA: Gọi hàm mới getCompletedHistory (hoặc getHistory với tham số lọc)
            List<InterviewSession> history = interviewService.getCompletedHistory(jobId, user.getId());
            
            List<InterviewDTO> dtos = history.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(MessageResponse.success("Lấy lịch sử thành công", dtos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }
    
    // 5. XEM KẾT QUẢ CHI TIẾT (Theo ID)
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSessionResult(@PathVariable Long sessionId) {
         try {
            InterviewSession session = interviewService.getSessionById(sessionId);
            // Check auth...
             return ResponseEntity.ok(MessageResponse.success("Lấy kết quả thành công", convertToDTO(session)));
         } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }

    // Helper đơn giản hóa (Bỏ tham số includeMessages vì ko còn messages để include)
    private InterviewDTO convertToDTO(InterviewSession session) {
        return InterviewDTO.builder()
                .id(session.getId())
                .status(session.getStatus())
                .score(session.getFinalScore())
                .feedback(session.getFeedback())
                .createdAt(session.getCreatedAt())
                .jobId(session.getJobPosting().getId())
                .jobTitle(session.getJobPosting().getTitle())
                .companyName(session.getJobPosting().getCompany() != null ? session.getJobPosting().getCompany().getName() : "")
                .messages(null) // Luôn null
                .build();
    }
}