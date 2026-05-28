
package app.auth.dto.response;

// Lombok: cung cấp các annotation để tự động sinh code
import lombok.AllArgsConstructor;  // Tạo constructor với tất cả các tham số
import lombok.Data;                // Tự động sinh getter, setter, toString, equals, hashCode
import lombok.NoArgsConstructor;   // Tạo constructor không tham số

/**
 * DTO (Data Transfer Object) dùng để trả về thông báo phản hồi cho client.
 * - @Data: Lombok tạo getter/setter, equals, hashCode, toString.
 * - @NoArgsConstructor: Tạo constructor rỗng.
 * - @AllArgsConstructor: Tạo constructor với tất cả các field.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    /**
     * success: Trạng thái phản hồi (true = thành công, false = thất bại).
     */
    private Boolean success;
    
    /**
     * message: Nội dung thông báo (ví dụ: "Đăng nhập thành công", "Có lỗi xảy ra").
     */
    private String message;
    
    /**
     * data: Dữ liệu trả về kèm theo (có thể là object bất kỳ, ví dụ thông tin người dùng).
     */
    private Object data;
    
    /**
     * Phương thức tiện ích để tạo phản hồi thành công (không có data).
     * @param message Nội dung thông báo.
     * @return MessageResponse với success = true và data = null.
     */
    public static MessageResponse success(String message) {
        return new MessageResponse(true, message, null);
    }
    
    /**
     * Phương thức tiện ích để tạo phản hồi thành công (có data).
     * @param message Nội dung thông báo.
     * @param data Dữ liệu trả về.
     * @return MessageResponse với success = true và data được truyền vào.
     */
    public static MessageResponse success(String message, Object data) {
        return new MessageResponse(true, message, data);
    }
    
    /**
     * Phương thức tiện ích để tạo phản hồi lỗi.
     * @param message Nội dung thông báo lỗi.
     * @return MessageResponse với success = false và data = null.
     */
    public static MessageResponse error(String message) {
        return new MessageResponse(false, message, null);
       }
}