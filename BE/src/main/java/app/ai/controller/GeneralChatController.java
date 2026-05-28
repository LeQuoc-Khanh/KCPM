package app.ai.controller;

import app.ai.dto.chat.ChatRequest;
import app.ai.dto.chat.ChatResponse;
import app.ai.service.cv.gemini.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class GeneralChatController {

    private final GeminiService geminiService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askAI(@RequestBody ChatRequest request) {
        // Validate đầu vào
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Vui lòng nhập nội dung câu hỏi."));
        }

        try {
            // Gọi Service xử lý
            String aiReply = geminiService.chatWithAI(request.getMessage());
            
            // Trả về kết quả
            return ResponseEntity.ok(new ChatResponse(aiReply));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Lỗi hệ thống AI: " + e.getMessage()));
        }
    }
}