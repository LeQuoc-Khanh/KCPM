package app.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType; // Import MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile

import app.auth.dto.request.ChangePasswordRequest;
import app.auth.dto.response.MessageResponse;
import app.auth.dto.response.UserResponse;
import app.auth.service.UserService;
import app.service.CloudinaryService; // Import CloudinaryService

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final CloudinaryService cloudinaryService; // Inject thêm service này
    
    @GetMapping("/me")
    public ResponseEntity<MessageResponse> getCurrentUser() {
        log.info("Get current user request");
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(
                MessageResponse.success("Lấy thông tin người dùng thành công", user)
        );
    }
    
    /**
     * [POST] /api/users/upload-avatar
     * Mục đích: Upload file ảnh và cập nhật avatar cho user hiện tại.
     */
    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        log.info("Upload avatar request");
        
        // 1. Upload ảnh lên Cloudinary vào folder 'avatar'
        String avatarUrl = cloudinaryService.uploadAvatar(file);
        
        // 2. Cập nhật URL vào database (dùng lại hàm updateProfile có sẵn)
        // Tham số fullName truyền null để giữ nguyên tên cũ
        UserResponse user = userService.updateProfile(null, avatarUrl);
        
        return ResponseEntity.ok(
                MessageResponse.success("Cập nhật ảnh đại diện thành công", user)
        );
    }
    
    @PutMapping("/me")
    public ResponseEntity<MessageResponse> updateProfile(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String profileImageUrl
    ) {
        log.info("Update profile request");
        UserResponse user = userService.updateProfile(fullName, profileImageUrl);
        return ResponseEntity.ok(
                MessageResponse.success("Cập nhật thông tin thành công", user)
        );
    }
    
    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Change password request");
        userService.changePassword(request);
        return ResponseEntity.ok(
                MessageResponse.success("Đổi mật khẩu thành công")
        );
    }
    
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteAccount() {
        log.info("Delete account request");
        userService.deleteAccount();
        return ResponseEntity.ok(
                MessageResponse.success("Xóa tài khoản thành công")
        );
    }
}