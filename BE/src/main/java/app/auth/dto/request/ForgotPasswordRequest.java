
package app.auth.dto.request;

// Jakarta Validation: cung cấp các annotation để kiểm tra dữ liệu đầu vào
import jakarta.validation.constraints.Email;     // @Email: kiểm tra chuỗi có đúng định dạng email
import jakarta.validation.constraints.NotBlank;  // @NotBlank: chuỗi không được null và không chỉ chứa khoảng trắng

// Lombok: @Data tự động sinh getter, setter, toString, equals, hashCode
import lombok.Data;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu từ request quên mật khẩu.
 * - @Data: Lombok tạo sẵn getter/setter, toString, equals, hashCode cho các field.
 */
@Data
public class ForgotPasswordRequest {
    
    /**
     * Email của người dùng để gửi yêu cầu quên mật khẩu.
     * - @NotBlank: bắt buộc có giá trị, không được để trống hoặc chỉ chứa khoảng trắng.
     * - @Email: kiểm tra định dạng email hợp lệ (ví dụ: user@example.com).
     * - message: thông báo lỗi trả về nếu vi phạm ràng buộc.
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}