package app.ai.service.cv.extractortext.component;

import app.ai.service.cv.extractortext.Interface.IFileTextExtractor;
import app.ai.service.cv.gemini.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DOCXTextExtractor implements IFileTextExtractor {

    private final GeminiService geminiService; // Inject Gemini

    // --- HỖ TRỢ MULTIPART FILE (Upload Form) ---
    @Override
    public boolean supports(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return fileName != null &&
               (fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc"));
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            return processDocument(document);
        }
    }

    // --- HỖ TRỢ FILE (Tải từ URL) ---
    @Override
    public boolean supports(File file) {
        String fileName = file.getName();
        return fileName != null &&
               (fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc"));
    }

    @Override
    public String extractText(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            return processDocument(document);
        }
    }

    // --- LOGIC XỬ LÝ CHÍNH ---
    private String processDocument(XWPFDocument document) {
        StringBuilder text = new StringBuilder();
        
        // 1. Cố gắng lấy Text theo cách thông thường
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph para : paragraphs) {
            String paraText = para.getText();
            if (paraText != null && !paraText.trim().isEmpty()) {
                text.append(paraText).append("\n");
            }
        }
        
        String cleanText = cleanForAI(text.toString());

        // 2. Kiểm tra độ dài Text
        if (cleanText.length() > 50) {
            return cleanText; // Text ngon -> Trả về luôn
        }

        // 3. Nếu Text quá ngắn -> Khả năng cao là ảnh dán trong Word -> Lấy ảnh ra gửi Gemini
        log.warn("DOCX ít chữ, chuyển sang chế độ quét ảnh (OCR)...");
        return extractImagesAndOCR(document);
    }

    private String extractImagesAndOCR(XWPFDocument document) {
        StringBuilder ocrResult = new StringBuilder();
        
        // Lấy tất cả ảnh nhúng trong file Word
        List<XWPFPictureData> pictures = document.getAllPictures();
        
        if (pictures.isEmpty()) {
            return "File không chứa văn bản và cũng không chứa ảnh nào.";
        }

        log.info("Tìm thấy {} ảnh trong file DOCX. Đang gửi sang Gemini...", pictures.size());

        for (int i = 0; i < pictures.size(); i++) {
            XWPFPictureData picture = pictures.get(i);
            byte[] imageBytes = picture.getData();
            String mimeType = picture.getPackagePart().getContentType(); // Ví dụ: image/png, image/jpeg

            try {
                // Chỉ xử lý các định dạng ảnh phổ biến để tránh lỗi
                if (mimeType.startsWith("image/")) {
                    log.info("OCR ảnh thứ {}/{} ({})", i + 1, pictures.size(), mimeType);
                    String extracted = geminiService.convertImageToText(imageBytes, mimeType);
                    ocrResult.append(extracted).append("\n\n");
                }
            } catch (Exception e) {
                log.error("Lỗi OCR ảnh trong DOCX: " + e.getMessage());
            }
        }
        
        return ocrResult.toString();
    }

    // Hàm làm sạch dữ liệu
    private String cleanForAI(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\t\\u00A0]+", " ")
                   .replaceAll("\\n\\s*\\n", "\n\n")
                   .replaceAll("(?m)^\\s+|\\s+$", "")
                   .trim();
    }
}