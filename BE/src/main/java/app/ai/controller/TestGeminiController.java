package app.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.ai.service.cv.extractortext.CVTextExtractor;
import app.ai.service.cv.gemini.*;
import app.ai.service.cv.gemini.dto.GeminiResponse;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestGeminiController {

    private final GeminiService geminiService;

    @GetMapping("/ai")
    public ResponseEntity<?> testGemini() {
        // Giả lập một đoạn text CV ngắn để test
        String mockCVText = """
            Nguyễn Văn A
            Email: anguyen@gmail.com | SĐT: 0909123456
            
            KINH NGHIỆM LÀM VIỆC
            Công ty ABC Tech
            Java Backend Developer
            01/2020 - Hiện tại
            - Phát triển hệ thống Microservices dùng Spring Boot.
            
            KỸ NĂNG
            Java, Spring Boot, SQL, Docker
            """;

        System.out.println("Dang goi Gemini API...");
        GeminiResponse result = geminiService.parseCV(mockCVText);
        
        return ResponseEntity.ok(result);
    }

    private final CVTextExtractor cvTextExtractor;

    @PostMapping("/check-text")
    public ResponseEntity<String> checkTextExtraction(@RequestParam("file") MultipartFile file) {
        try {
            // Gọi hàm tách chữ
            String extractedText = cvTextExtractor.extractText(file);
            
            // Trả về text thô để kiểm tra
            // Mẹo: Dùng thẻ <pre> bao quanh nếu test trên trình duyệt để thấy rõ xuống dòng
            return ResponseEntity.ok(extractedText);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tách chữ: " + e.getMessage());
        }
}
@PostMapping("/ocr-image")
    public ResponseEntity<String> testOcrImage(@RequestParam("file") MultipartFile file) {
        try {
            // Kiểm tra định dạng ảnh
            String mimeType = file.getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Vui lòng upload file ảnh (JPG, PNG)!");
            }

            // Gọi Gemini convert
            String extractedText = geminiService.convertImageToText(file.getBytes(), mimeType);
            
            return ResponseEntity.ok(extractedText);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi OCR: " + e.getMessage());
        }
    }
}
