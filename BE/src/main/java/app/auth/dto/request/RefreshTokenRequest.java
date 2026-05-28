
package app.auth.dto.request;

// Jakarta Validation: cung cấp các annotation để kiểm tra dữ liệu đầu vào
import jakarta.validation.constraints.NotBlank;  // @NotBlank: chuỗi không được null và không chỉ chứa khoảng trắng

// Lombok: @Data tự động sinh getter, setter, toString, equals, hashCode
import lombok.Data;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu từ request làm mới token (refresh token).
 * - @Data: Lombok tạo sẵn getter/setter, toString, equals, hashCode cho các field.
 */
@Data
public class RefreshTokenRequest {
    
    /**
     * Refresh token được gửi từ client để yêu cầu cấp lại access token.
     * - @NotBlank: bắt buộc có giá trị, không được để trống hoặc chỉ chứa khoảng trắng.
     * - message: thông báo lỗi trả về nếu vi phạm ràng buộc.
     */
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}