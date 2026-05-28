package app.ai.service.cv.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("#{'${gemini.api.keys}'.split(',')}")
    private List<String> apiKeys;

    private final AtomicInteger keyIndex = new AtomicInteger(0);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    private String getRotatedKey() {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new RuntimeException("Không tìm thấy API Key nào trong cấu hình!");
        }
        int index = keyIndex.getAndIncrement() % apiKeys.size();
        if (index < 0) index = Math.abs(index);
        
        String selectedKey = apiKeys.get(index);
        // Log ẩn bớt key để bảo mật
        log.info("Đang sử dụng Key thứ {}/{} : ...{}", (index + 1), apiKeys.size(), selectedKey.substring(Math.max(0, selectedKey.length() - 4)));
        return selectedKey;
    }

    /**
     * Gửi Prompt lên Google Gemini với cấu hình nhiệt độ (temperature) tùy chỉnh.
     */
    public String generateContent(String promptText, float temperature) {
        int maxRetries = 3; 
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return callGeminiApi(promptText, temperature);
            } catch (Exception e) {
                attempt++;
                log.warn("Lần thử {} thất bại: {}. Đang thử key khác...", attempt, e.getMessage());
                try {
                Thread.sleep(2000); // Đợi 2000ms (2 giây) để quota hạ nhiệt
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Đã thử " + maxRetries + " key khác nhau nhưng vẫn thất bại. Lỗi: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private String callGeminiApi(String promptText, float temperature) throws Exception {
        // 1. Lấy Key
        String currentKey = getRotatedKey();
        
        // 2. Tạo Body Request
        Map<String, Object> contentPart = new HashMap<>();
        contentPart.put("text", promptText);
        
        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(contentPart));
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        // 👇 [QUAN TRỌNG] CẤU HÌNH NHIỆT ĐỘ
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        requestBody.put("generationConfig", generationConfig);
        // 👆 KẾT THÚC CẤU HÌNH

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 3. Gọi API
        String url = GEMINI_API_URL + currentKey;
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getBody() == null || response.getBody().isEmpty()) {
            throw new RuntimeException("Gemini API trả về response rỗng");
        }

        // 4. Parse JSON
        var jsonNode = objectMapper.readTree(response.getBody());
        
        if (jsonNode.path("candidates").isEmpty() || !jsonNode.path("candidates").has(0)) {
            throw new RuntimeException("Gemini API response không hợp lệ (Block/Filter)");
        }
        
        var candidate = jsonNode.path("candidates").get(0);
        if (!candidate.has("content")) {
             throw new RuntimeException("Gemini không trả về nội dung (có thể do safety settings)");
        }
        
        String aiTextResponse = candidate.path("content").path("parts").get(0).path("text").asText();
        
        return aiTextResponse.replaceAll("```json", "")
                             .replaceAll("```", "")
                             .trim();
    }

    /**
     * [MỚI] Gửi ảnh + Prompt lên Gemini (Multimodal)
     */
    public String generateContentWithImage(String promptText, byte[] imageBytes, String mimeType, float temperature) {
        try {
           int maxRetries = 3; // Thêm số lần thử lại
    int attempt = 0;

    while (attempt < maxRetries) {
        try {
            return callGeminiApiWithImage(promptText, imageBytes, mimeType, temperature);
        } catch (Exception e) {
            attempt++;
            log.warn("Lần thử Vision {} thất bại: {}. Đang thử key khác...", attempt, e.getMessage());
            
            // Nếu đã thử hết số lần cho phép thì mới ném lỗi
            if (attempt >= maxRetries) {
                log.error("Lỗi gọi Gemini Vision sau {} lần thử: ", maxRetries, e);
                throw new RuntimeException("Gemini Vision Error: " + e.getMessage());
            }
        }
    }
    return null;
        } catch (Exception e) {
            log.error("Lỗi gọi Gemini Vision: ", e);
            throw new RuntimeException("Gemini Vision Error: " + e.getMessage());
        }
    }

    private String callGeminiApiWithImage(String promptText, byte[] imageBytes, String mimeType, float temperature) throws Exception {
        String currentKey = getRotatedKey();

        // 1. Tạo phần Text
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        // 2. Tạo phần Ảnh (Inline Data)
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", inlineData);

        // 3. Gộp Text và Ảnh vào parts
        Map<String, Object> parts = new HashMap<>();
        parts.put("parts", List.of(textPart, imagePart)); // Gửi cả 2

        // 4. Body request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(parts));

        // Config nhiệt độ
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        requestBody.put("generationConfig", generationConfig);

        // 5. Gọi API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = GEMINI_API_URL + currentKey;
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // 6. Parse kết quả (tương tự hàm cũ)
        if (response.getBody() == null) throw new RuntimeException("Empty Response");
        var jsonNode = objectMapper.readTree(response.getBody());
        var candidate = jsonNode.path("candidates").get(0);
        
        if (!candidate.has("content")) throw new RuntimeException("No content from Gemini");
        
        return candidate.path("content").path("parts").get(0).path("text").asText().trim();
    }
}