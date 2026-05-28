package app.ai.service.cv.extractortext;

import app.ai.service.cv.extractortext.Interface.IFileTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CVTextExtractor {

    // Spring sẽ tự động tìm tất cả các Bean implement IFileTextExtractor (PDF, DOCX) và nhét vào List này
    private final List<IFileTextExtractor> extractors;

    public String extractText(MultipartFile file) {
        for (IFileTextExtractor extractor : extractors) {
            if (extractor.supports(file)) {
                try {
                    // Gọi hàm tách chữ của component tương ứng
                    return extractor.extractText(file);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi trích xuất nội dung file: " + e.getMessage(), e);
                }
            }
        }
        throw new IllegalArgumentException("Định dạng file không hỗ trợ: " + file.getOriginalFilename());
    }

    public String extractText(File file) {
        for (IFileTextExtractor extractor : extractors) {
            if (extractor.supports(file)) {
                try {
                    return extractor.extractText(file);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi đọc file tải về: " + e.getMessage(), e);
                }
            }
        }
        // Nếu không tìm thấy extractor phù hợp
        return "Không tìm thấy extractor phù hợp"; 
    }
}