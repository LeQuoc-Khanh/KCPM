package app.ai.service.cv;

import app.ai.service.cv.extractortext.CVTextExtractor;
import app.ai.service.cv.gemini.GeminiService; // Import lại Gemini
import app.ai.service.cv.gemini.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class CVAnalysisService {

    private final CVTextExtractor textExtractor;
    private final GeminiService geminiService; // Dùng lại Gemini

    public GeminiResponse analyzeCV(MultipartFile file) {
        // 1. Lấy chữ
        String rawText = textExtractor.extractText(file);

        // 2. Gọi Gemini (Miễn phí)
        return geminiService.parseCV(rawText);
    }
    // Hàm tải CV từ URL và trích xuất text
    public String getTextFromUrl(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.isEmpty()) return "";

        // 1. Xác định đuôi file để tạo file tạm đúng định dạng
        String extension = ".pdf"; // Mặc định
        String lowerUrl = fileUrl.toLowerCase();
        if (lowerUrl.contains(".docx")) extension = ".docx";
        else if (lowerUrl.contains(".doc")) extension = ".doc";

        // 2. Tải file về thư mục tạm
        File tempFile = File.createTempFile("cv_download_", extension);
        
        try {
            // [FIX LỖI DEPRECATED] Dùng URI.create().toURL() thay vì new URL()
            URL url = URI.create(fileUrl).toURL();
            
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 3. Gọi TextExtractor để lấy chữ (Đã sửa lỗi "extractors cannot be resolved")
            return textExtractor.extractText(tempFile);

        } catch (Exception e) {
            log.error("Lỗi tải/đọc CV từ URL: {}", fileUrl, e);
            return ""; // Trả về rỗng để không làm sập luồng chính
        } finally {
            // Dọn dẹp file rác
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}