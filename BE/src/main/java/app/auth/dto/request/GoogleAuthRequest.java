
package app.auth.dto.request;

import app.auth.model.enums.UserRole;
// Jakarta Validation: cung cấp các annotation để kiểm tra dữ liệu đầu vào
import jakarta.validation.constraints.NotBlank;  // @NotBlank: chuỗi không được null và không chỉ chứa khoảng trắng

// Lombok: @Data tự động sinh getter, setter, toString, equals, hashCode
import lombok.Data;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu từ request đăng nhập bằng Google.
 * - @Data: Lombok tạo sẵn getter/setter, toString, equals, hashCode cho các field.
 */
@Data
public class GoogleAuthRequest {
    
    /**
     * Google token được gửi từ client sau khi xác thực với Google.
     * - @NotBlank: bắt buộc có giá trị, không được để trống hoặc chỉ chứa khoảng trắng.
     * - message: thông báo lỗi trả về nếu vi phạm ràng buộc.
     */
    @NotBlank(message = "Google token không được để trống")
    private String googleToken;
    
    /**
     * Vai trò của người dùng (UserRole).
     * - Không bắt buộc cho người dùng đã tồn tại.
     * - Required cho người dùng đăng nhập lần đầu để xác định quyền (ví dụ: STUDENT, TEACHER).
     */
    private UserRole userRole; // Required for first-time users
}
