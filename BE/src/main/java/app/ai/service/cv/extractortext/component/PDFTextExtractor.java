package app.ai.service.cv.extractortext.component;

import app.ai.service.cv.extractortext.Interface.IFileTextExtractor;
import app.ai.service.cv.gemini.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PDFTextExtractor implements IFileTextExtractor {

    private final GeminiService geminiService; // Inject Gemini để dùng khi cần cứu viện

    @Override
    public boolean supports(MultipartFile file) {
        return file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf");
    }

    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return processDoc(document);
        }
    }

    @Override
    public String extractText(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            return processDoc(document);
        }
    }

    // --- LOGIC XỬ LÝ CHÍNH ---
    private String processDoc(PDDocument document) throws IOException {
        // CÁCH 1: Thử đọc text thuần túy (Nhanh nhất)
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String text = cleanForAI(stripper.getText(document));

        // Kiểm tra xem có lấy được nội dung ra hồn không?
        if (text.length() > 50) { 
            return text; // Ngon, PDF xịn -> Trả về luôn
        }

        // CÁCH 2: Nếu text quá ngắn hoặc rỗng -> Đây là PDF Scan (Ảnh) -> Dùng Gemini OCR
        log.warn("PDF không chứa text (hoặc rất ít). Chuyển sang chế độ OCR bằng Gemini...");
        return performOcrOnPdf(document);
    }

    private String performOcrOnPdf(PDDocument document) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();

        // Duyệt qua từng trang của PDF (Thường CV chỉ 1-2 trang)
        for (int page = 0; page < document.getNumberOfPages(); page++) {
            try {
                // 1. Render trang PDF thành Ảnh (300 DPI để rõ nét)
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);

                // 2. Chuyển BufferedImage thành byte[]
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();

                // 3. Gọi Gemini đọc ảnh này
                log.info("Đang gửi trang {}/{} lên Gemini OCR...", (page + 1), document.getNumberOfPages());
                String pageText = geminiService.convertImageToText(imageBytes, "image/jpeg");
                
                fullText.append(pageText).append("\n\n");

            } catch (Exception e) {
                log.error("Lỗi OCR trang " + page, e);
            }
        }
        return fullText.toString();
    }

    private String cleanForAI(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\t\\u00A0]+", " ").replaceAll("\\n\\s*\\n", "\n\n").trim();
    }
}