
package app.auth.dto.request;

// Jakarta Validation: cung cấp các annotation để kiểm tra dữ liệu đầu vào (Bean Validation)
import jakarta.validation.constraints.NotBlank;   // @NotBlank: chuỗi không được null và không chỉ gồm khoảng trắng
import jakarta.validation.constraints.Pattern;    // @Pattern: kiểm tra chuỗi khớp với biểu thức chính quy (regex)
import jakarta.validation.constraints.Size;       // @Size: giới hạn độ dài chuỗi (min, max)

// Lombok: tự động sinh getter/setter, equals/hashCode, toString...
import lombok.Data;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu đổi mật khẩu từ request.
 * - @Data: Lombok tạo sẵn getter/setter, toString, equals, hashCode cho các field.
 */
@Data
public class ChangePasswordRequest {
    
    /**
     * Mật khẩu cũ người dùng nhập vào.
     * - @NotBlank: bắt buộc có giá trị, không được để trống hoặc chỉ chứa khoảng trắng.
     * - message: thông báo lỗi trả về nếu vi phạm ràng buộc.
     */
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;
    
    /**
     * Mật khẩu mới người dùng muốn đặt.
     * - @NotBlank: bắt buộc có giá trị.
     * - @Size(min=6, max=50): yêu cầu độ dài từ 6 đến 50 ký tự.
     * - @Pattern(regexp=...): yêu cầu có ít nhất:
     *     + 1 chữ thường [a-z]
     *     + 1 chữ hoa [A-Z]
     *     + 1 chữ số [0-9]
     *   Regex dùng nhóm khẳng định (positive lookahead) để đảm bảo sự hiện diện của các loại ký tự.
     * - message: thông báo lỗi tùy biến nếu không đáp ứng tiêu chí.
     */
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải từ 6-50 ký tự")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Mật khẩu mới phải chứa ít nhất 1 chữ hoa, 1 chữ thường và 1 số"
    )
       private String newPassword;
}