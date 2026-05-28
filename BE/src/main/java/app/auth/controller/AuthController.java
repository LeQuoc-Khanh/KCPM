package app.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import app.auth.dto.request.*;
import app.auth.dto.response.AuthResponse;
import app.auth.dto.response.MessageResponse;
import app.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * ĐĂNG KÝ
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> register(
            @RequestPart("request") @Valid RegisterRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        log.info("Register request received for email: {}", request.getEmail());
        authService.register(request, avatar);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(MessageResponse.success("Đăng ký thành công. Vui lòng kiểm tra email để xác thực."));
    }

    /**
     * XÁC THỰC EMAIL
     */
    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @RequestParam String email,
            @RequestParam String code
    ) {
        log.info("Verify email request: {} with code: {}", email, code);
        authService.verifyEmail(email, code);
        return ResponseEntity.ok(MessageResponse.success("Xác thực tài khoản thành công"));
    }

    /**
     * [MỚI] GỬI LẠI MÃ XÁC THỰC
     * Endpoint này dùng khi người dùng không nhận được mã hoặc mã hết hạn (nếu có logic hết hạn)
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@RequestParam String email) {
        log.info("Resend verification code request for email: {}", email);
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(MessageResponse.success("Mã xác thực mới đã được gửi đến email của bạn"));
    }

    /**
     * ĐĂNG NHẬP
     */
    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authData = authService.login(request);
        return ResponseEntity.ok(MessageResponse.success("Đăng nhập thành công", authData));
    }

    /**
     * ĐĂNG NHẬP GOOGLE
     */
    @PostMapping("/google")
    public ResponseEntity<MessageResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse authData = authService.googleAuth(request);
        return ResponseEntity.ok(MessageResponse.success("Đăng nhập Google thành công", authData));
    }

    /**
     * LÀM MỚI TOKEN
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<MessageResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(MessageResponse.success("Token đã được làm mới", response));
    }

    /**
     * ĐĂNG XUẤT
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error("Không có phiên đăng nhập hợp lệ"));
        }
        authService.logout(authentication.getName());
        return ResponseEntity.ok(MessageResponse.success("Đăng xuất thành công"));
    }

    /**
     * QUÊN MẬT KHẨU
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(MessageResponse.success("Link đặt lại mật khẩu đã được gửi đến email của bạn"));
    }

    /**
     * ĐẶT LẠI MẬT KHẨU
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(MessageResponse.success("Mật khẩu đã được đặt lại thành công"));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth API is working!");
    }
}