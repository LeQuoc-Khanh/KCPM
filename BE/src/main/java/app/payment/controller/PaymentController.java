package app.payment.controller;

import app.auth.dto.response.AuthResponse;
import app.auth.dto.response.UserResponse; // Import UserResponse
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // BE/src/main/java/app/payment/controller/PaymentController.java

@PostMapping("/vip-upgrade")
public ResponseEntity<?> upgradeToVip() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

    UserRole currentRole = user.getUserRole();
    String currentRoleName = currentRole.name();

    // 1. Kiểm tra Admin
    if (currentRole == UserRole.ADMIN) {
        return ResponseEntity.badRequest().body("Admin không cần mua VIP.");
    }

    try {
        UserRole newRole = currentRole;

        // 2. Logic tính ngày hết hạn
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpirationDate;

        if (currentRoleName.endsWith("_VIP")) {
            // Trường hợp GIA HẠN: Nếu đang là VIP, cộng thêm 30 ngày vào ngày hết hạn hiện tại
            // (Nếu ngày hết hạn đã qua, thì tính từ thời điểm hiện tại)
            if (user.getVipExpirationDate() != null && user.getVipExpirationDate().isAfter(now)) {
                newExpirationDate = user.getVipExpirationDate().plusDays(30);
            } else {
                newExpirationDate = now.plusDays(30);
            }
            // Role giữ nguyên là VIP
            newRole = currentRole; 
        } else {
            // Trường hợp NÂNG CẤP MỚI: Role thường -> Role VIP
            newRole = UserRole.valueOf(currentRoleName + "_VIP");
            newExpirationDate = now.plusDays(30);
        }

        // 3. Cập nhật vào User
        user.setUserRole(newRole);
        user.setVipExpirationDate(newExpirationDate); // <-- QUAN TRỌNG: Lưu ngày hết hạn
        userRepository.save(user);

        // 4. Tạo Token mới (để cập nhật Claims trong token nếu cần)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 5. Build UserResponse
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .userRole(newRole)
                .status(user.getStatus())
                .profileImageUrl(user.getProfileImageUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                // .vipExpirationDate(user.getVipExpirationDate()) // Hãy đảm bảo UserResponse có field này
                .build();

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userResponse)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build());

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("Không tìm thấy gói VIP phù hợp cho role: " + currentRoleName);
    }
}
}