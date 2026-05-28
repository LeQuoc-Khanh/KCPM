package app.ai.service.cv.extractortext.component;

import app.ai.service.cv.extractortext.Interface.IFileTextExtractor;
import app.ai.service.cv.gemini.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
public class ImageTextExtractor implements IFileTextExtractor {

    private final GeminiService geminiService;

    @Override
    public boolean supports(MultipartFile file) {
        String contentType = file.getContentType();
        // Hỗ trợ các đuôi ảnh phổ biến
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"));
    }

    @Override
    public boolean supports(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        return geminiService.convertImageToText(file.getBytes(), file.getContentType());
    }

    @Override
    public String extractText(File file) throws Exception {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String mimeType = Files.probeContentType(file.toPath());
        return geminiService.convertImageToText(fileContent, mimeType != null ? mimeType : "image/jpeg");
    }
}