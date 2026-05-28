
package app.auth.dto.response;

// Lombok: cung cấp các annotation để tự động sinh code
import lombok.AllArgsConstructor;  // Tạo constructor với tất cả các tham số
import lombok.Builder;             // Cho phép sử dụng Builder pattern để khởi tạo đối tượng
import lombok.Data;                // Tự động sinh getter, setter, toString, equals, hashCode
import lombok.NoArgsConstructor;   // Tạo constructor không tham số

/**
 * DTO (Data Transfer Object) dùng để trả về thông tin xác thực (authentication) cho client.
 * - @Data: Lombok tạo getter/setter, equals, hashCode, toString.
 * - @Builder: Cho phép khởi tạo đối tượng bằng cú pháp builder.
 * - @NoArgsConstructor: Tạo constructor rỗng.
 * - @AllArgsConstructor: Tạo constructor với tất cả các field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * accessToken: JWT token dùng để xác thực các request sau khi đăng nhập.
     */
    private String accessToken;
    
    /**
     * refreshToken: Token dùng để cấp lại accessToken khi hết hạn.
     */
    private String refreshToken;
    
    /**
     * tokenType: Loại token, mặc định là "Bearer" (chuẩn OAuth 2.0).
     */
    private String tokenType = "Bearer";
    
    /**
     * expiresIn: Thời gian sống của accessToken (tính bằng giây hoặc mili-giây).
     */
    private Long expiresIn;
    
    /**
     * user: Thông tin người dùng (được trả về dưới dạng UserResponse).
     */
       private UserResponse user;
}