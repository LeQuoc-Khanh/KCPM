package app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload CV (Giữ nguyên logic cũ của bạn)
     * Folder: phantichcv/cv
     * Public ID: Tên file gốc
     */
    public String uploadFile(MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) {
                originalFileName = "cv_file"; 
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "auto",
                    "public_id", originalFileName,
                    "unique_filename", true,
                    "folder", "phantichcv/cv"
                )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload file lên Cloudinary: " + e.getMessage());
        }
    }

    /**
     * [MỚI] Upload Avatar
     * Folder: avatar
     * Public ID: Tự động (Cloudinary sinh) hoặc Random để tránh trùng
     */
    public String uploadAvatar(MultipartFile file) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "image",      // Chỉ định rõ là ảnh
                    "folder", "phantichcv/avatar"             // Yêu cầu: lưu vào folder tên "avatar"
                )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload avatar lên Cloudinary: " + e.getMessage());
        }
    }

    public String uploadCompanyImage(MultipartFile file) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "phantichcv/company" // Lưu vào folder riêng
                )
            );
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh công ty: " + e.getMessage());
        }
    }
}