
package app.auth.dto.request;

// Jakarta Validation: cung cấp các annotation để kiểm tra dữ liệu đầu vào
import jakarta.validation.constraints.NotBlank;   // @NotBlank: chuỗi không được null và không chỉ chứa khoảng trắng
import jakarta.validation.constraints.Pattern;    // @Pattern: kiểm tra chuỗi khớp với biểu thức chính quy (regex)
import jakarta.validation.constraints.Size;       // @Size: giới hạn độ dài chuỗi (min, max)

// Lombok: @Data tự động sinh getter, setter, toString, equals, hashCode
import lombok.Data;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu từ request đặt lại mật khẩu.
 * - @Data: Lombok tạo sẵn getter/setter, toString, equals, hashCode cho các field.
 */
@Data
public class ResetPasswordRequest {
    
    /**
     * Token xác thực được gửi từ email hoặc hệ thống để đặt lại mật khẩu.
     * - @NotBlank: bắt buộc có giá trị, không được để trống hoặc chỉ chứa khoảng trắng.
     */
    @NotBlank(message = "Token không được để trống")
    private String token;
    
    /**
     * Mật khẩu mới người dùng muốn đặt.
     * - @NotBlank: bắt buộc có giá trị.
     * - @Size(min=6, max=50): yêu cầu độ dài từ 6 đến 50 ký tự.
     * - @Pattern: yêu cầu mật khẩu chứa ít nhất:
     *     + 1 chữ thường [a-z]
     *     + 1 chữ hoa [A-Z]
     *     + 1 chữ số [0-9]
     *   Regex: sử dụng positive lookahead để kiểm tra sự hiện diện của các ký tự này.
     */
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải từ 6-50 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Mật khẩu mới phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số"
       )
    private String newPassword;
}