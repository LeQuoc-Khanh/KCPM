package app.ai.service.cv.extractortext.Interface;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

public interface IFileTextExtractor {
    boolean supports(MultipartFile file);
    boolean supports(File file); // [MỚI] Hỗ trợ file tải từ URL
    
    String extractText(MultipartFile file) throws Exception;
    String extractText(File file) throws Exception; // [MỚI]

}