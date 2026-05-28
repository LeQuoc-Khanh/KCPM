
package app.auth.dto.request;

import app.auth.model.enums.UserRole;
// Jakarta Validation: cung cấp các annotation để kiểm tra dữ liệu đầu vào
import jakarta.validation.constraints.*; // Bao gồm @NotBlank, @Size, @Email, @Pattern, @NotNull

// Lombok: @Data tự động sinh getter, setter, toString, equals, hashCode
import lombok.Data;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu từ request đăng ký tài khoản.
 * - @Data: Lombok tạo sẵn getter/setter, toString, equals, hashCode cho các field.
 */
@Data
public class RegisterRequest {
    
    /**
     * Họ tên người dùng.
     * - @NotBlank: bắt buộc có giá trị, không được để trống hoặc chỉ chứa khoảng trắng.
     * - @Size(min=2, max=100): yêu cầu độ dài từ 2 đến 100 ký tự.
     */
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;
    
    /**
     * Email người dùng.
     * - @NotBlank: bắt buộc có giá trị.
     * - @Email: kiểm tra định dạng email hợp lệ.
     * - @Size(max=100): giới hạn độ dài tối đa 100 ký tự.
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;
    
    /**
     * Mật khẩu người dùng.
     * - @NotBlank: bắt buộc có giá trị.
     * - @Size(min=6, max=50): yêu cầu độ dài từ 6 đến 50 ký tự.
     * - @Pattern: yêu cầu mật khẩu chứa ít nhất:
     *     + 1 chữ thường [a-z]
     *     + 1 chữ hoa [A-Z]
     *     + 1 chữ số [0-9]
     *   Regex: sử dụng positive lookahead để kiểm tra sự hiện diện của các ký tự này.
     */
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6-50 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Mật khẩu phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số"
    )
    private String password;
    
    /**
     * Vai trò của người dùng (UserRole).
     * - @NotNull: bắt buộc phải có giá trị (ví dụ: STUDENT     * - @NotNull: bắt buộc phải có giá trị (ví dụ: STUDENT, TEACHER, ADMIN).
     */
    @NotNull(message = "Vai trò không được để trống")
    private UserRole userRole;
}