
package app.auth.service;

import lombok.RequiredArgsConstructor; // Lombok: Tự động tạo constructor với các trường final (hoặc @NonNull) -> thuận tiện cho constructor injection
import lombok.extern.slf4j.Slf4j;      // Lombok: Tự động cung cấp logger 'log' (SLF4J) cho class, dùng log.info/log.error...
import org.springframework.security.core.context.SecurityContextHolder; // Spring Security: truy cập thông tin Authentication hiện tại (user đang đăng nhập)
import org.springframework.security.crypto.password.PasswordEncoder;    // Spring Security: mã hoá & kiểm tra mật khẩu an toàn (BCrypt, v.v.)
import org.springframework.stereotype.Service;                         // Spring: đánh dấu lớp là một Service (bean chứa logic nghiệp vụ)
import org.springframework.transaction.annotation.Transactional;       // Spring: đảm bảo transactional (atomicity, rollback nếu lỗi) cho các thao tác DB
import app.auth.dto.request.ChangePasswordRequest;                  // DTO: dữ liệu request đổi mật khẩu (oldPassword, newPassword, v.v.)
import app.auth.dto.response.UserResponse;                          // DTO: dữ liệu trả về cho client (ẩn thông tin nhạy cảm, định dạng trả về)
import app.auth.exception.InvalidCredentialsException;              // Exception tuỳ chỉnh: ném khi thông tin xác thực không hợp lệ (mật khẩu cũ sai)
import app.auth.exception.UserNotFoundException;                    // Exception tuỳ chỉnh: ném khi không tìm thấy người dùng
import app.auth.model.User;
import app.auth.repository.UserRepository;                          // Repository: thao tác với DB cho User (findByEmail, save, delete, ...)

// @Service: Đăng ký bean tầng service trong Spring Container
// @RequiredArgsConstructor (Lombok): Tạo constructor gồm các trường final để Spring tự động inject (constructor injection)
// @Slf4j (Lombok): Cấp sẵn logger 'log' dùng cho ghi log
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    // Inject UserRepository (final) qua constructor nhờ @RequiredArgsConstructor
    private final UserRepository userRepository;
    // Inject PasswordEncoder để mã hoá/so khớp mật khẩu an toàn
    private final PasswordEncoder passwordEncoder;
    // Inject RefreshTokenService để xử lý refresh token (invalidation khi thay đổi mật khẩu/xoá tài khoản)
    private final RefreshTokenService refreshTokenService;
    
    /**
     * Lấy thông tin người dùng hiện tại từ SecurityContext
     * - Dùng SecurityContextHolder để lấy Authentication -> email (username)
     * - Tìm User theo email; nếu không có -> ném UserNotFoundException
     * - Chuyển User -> UserResponse (DTO) để trả về cho client
     * @return UserResponse của người dùng hiện tại
     */
    public UserResponse getCurrentUser() {
        // Lấy email (principal) từ context bảo mật hiện hành
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Truy vấn DB theo email; nếu không có -> ném exception
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        
        // Chuyển sang DTO để trả về
        return convertToUserResponse(user);
    }
    
    /**
     * Cập nhật hồ sơ người dùng (fullName, profileImageUrl) cho user hiện tại
     * - @Transactional: đảm bảo cập nhật trong một transaction; nếu lỗi sẽ rollback
     * - Chỉ cập nhật nếu chuỗi không null và không rỗng (trim().isEmpty())
     * - Lưu lại vào DB và ghi log
     * @param fullName: tên hiển thị mới (tuỳ chọn)
     * @param profileImageUrl: URL ảnh đại diện mới (tuỳ chọn)
     * @return UserResponse sau khi cập nhật
     */
    @Transactional
    public UserResponse updateProfile(String fullName, String profileImageUrl) {
        // Lấy email người dùng đang đăng nhập
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Tìm người dùng theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        
        // Cập nhật tên nếu hợp lệ (không null, không chuỗi trống sau trim)
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        
        // Cập nhật URL ảnh đại diện nếu hợp lệ
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            user.setProfileImageUrl(profileImageUrl);
        }
        
        // Lưu thay đổi vào DB
        user = userRepository.save(user);
        // Ghi log thông tin cập nhật
        log.info("Profile updated for user: {}", email);
        
        // Trả về DTO
        return convertToUserResponse(user);
    }
    
    /**
     * Đổi mật khẩu cho user hiện tại
     * - @Transactional: đảm bảo quá trình cập nhật mật khẩu và xoá refresh token là atomic
     * - Kiểm tra mật khẩu cũ bằng PasswordEncoder.matches(...)
     * - Mã hoá mật khẩu mới bằng PasswordEncoder.encode(...)
     * - Lưu user sau khi cập nhật
     * - Vô hiệu hoá tất cả refresh token của user để buộc đăng nhập lại (tăng bảo mật)
     * - Ghi log sự kiện
     * @param request: ChangePasswordRequest chứa oldPassword và newPassword
     * @throws InvalidCredentialsException nếu mật khẩu cũ không đúng
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // Lấy email người dùng hiện tại
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Truy vấn user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        
        // Verify old password: so khớp mật khẩu cũ với mật khẩu đã mã hoá lưu trong DB
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            // Nếu không khớp -> ném lỗi xác thực
            throw new InvalidCredentialsException("Mật khẩu cũ không đúng");
        }
        
        // Update password: mã hoá mật khẩu mới và gán vào user
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Lưu thay đổi mật khẩu vào DB
        userRepository.save(user);
        
        // Invalidate all refresh tokens: xoá mọi refresh token để tránh tiếp tục dùng session cũ
        refreshTokenService.deleteByUser(user);
        
        // Ghi log sự kiện đổi mật khẩu
        log.info("Password changed for user: {}", email);
    }
    
    /**
     * Xoá tài khoản người dùng hiện tại
     * - @Transactional: xoá refresh token và user trong cùng transaction
     * - Trước tiên: xoá tất cả refresh token của user
     * - Sau đó: xoá thực thể user (tài khoản) khỏi DB
     * - Ghi log sự kiện xoá tài khoản
     */
    @Transactional
    public void deleteAccount() {
        // Lấy email user hiện tại
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Truy vấn user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        
        // Delete all refresh tokens: đảm bảo người dùng không thể tiếp tục sử dụng token sau khi xoá tài khoản
        refreshTokenService.deleteByUser(user);
        
        // Delete user: xoá tài khoản khỏi DB
        userRepository.delete(user);
        
        // Ghi log sự kiện xoá
        log.info("Account deleted for user: {}", email);
    }
    
    /**
     * Chuyển đổi từ thực thể User sang DTO UserResponse
     * - Mục đích: chuẩn hoá dữ liệu trả về cho client, không lộ field nhạy cảm (ví dụ password)
     * - Sử dụng pattern builder của UserResponse để tạo DTO
     * @param user: thực thể User từ DB
     * @return UserResponse: bản dữ liệu gửi ra cho client
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .status(user.getStatus())
                .profileImageUrl(user.getProfileImageUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}