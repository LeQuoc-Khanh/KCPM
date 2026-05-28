package app.candidate.controller;

import app.auth.dto.response.MessageResponse;
import app.candidate.dto.request.CandidateProfileUpdateRequest;
import app.candidate.dto.response.CandidateProfileResponse; // 👈 Dùng DTO
import app.candidate.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import app.auth.model.User;
import app.auth.repository.UserRepository;

@RestController
@RequestMapping("/api/candidate/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateService candidateService;
    private final UserRepository userRepository;

    @PostMapping("/upload-cv")
    public ResponseEntity<?> uploadCV(@RequestParam("file") MultipartFile file) {
        try {
            // Lấy User hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 1. Xử lý upload và phân tích (Entity ẩn bên trong Service)
            candidateService.uploadAndAnalyzeCV(user.getId(), file);

            // 2. Gọi hàm lấy DTO an toàn để trả về FE
            CandidateProfileResponse response = candidateService.getProfileDTO(user.getId());

            return ResponseEntity.ok(MessageResponse.success("Phân tích CV thành công", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(MessageResponse.error("Lỗi xử lý CV: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            CandidateProfileResponse profile = candidateService.getProfileDTO(user.getId());
            return ResponseEntity.ok(MessageResponse.success("Lấy thông tin thành công", profile));
        } catch (Exception e) {
            return ResponseEntity.ok(MessageResponse.success("Chưa có hồ sơ", null));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody CandidateProfileUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // 1. Thực hiện update
            candidateService.updateProfile(user.getId(), request);
            
            // 2. Lấy lại dữ liệu mới nhất dạng DTO
            CandidateProfileResponse updatedProfile = candidateService.getProfileDTO(user.getId());
            
            return ResponseEntity.ok(MessageResponse.success("Cập nhật hồ sơ thành công", updatedProfile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Lỗi cập nhật: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Gọi service
            String newAvatarUrl = candidateService.uploadAvatar(user.getId(), file);

            return ResponseEntity.ok(MessageResponse.success("Cập nhật ảnh đại diện thành công", newAvatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error("Lỗi upload ảnh: " + e.getMessage()));
        }
    }
}