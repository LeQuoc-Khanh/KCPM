
package app.auth.repository;

// Spring Data JPA: cung cấp interface JpaRepository và annotation hỗ trợ
import org.springframework.data.jpa.repository.JpaRepository; // Interface cho CRUD và query mặc định
import org.springframework.data.jpa.repository.Modifying;      // Đánh dấu query là thao tác thay đổi dữ liệu (DELETE/UPDATE)
import org.springframework.stereotype.Repository;             // Đánh dấu interface là Repository (thành phần DAO)

import app.auth.model.PasswordResetToken;
import app.auth.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository cho entity PasswordResetToken.
 * - Kế thừa JpaRepository<PasswordResetToken, Long>: cung cấp các phương thức CRUD mặc định.
 * - @Repository: đánh dấu là bean quản lý bởi Spring, phục vụ thao tác DB.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * Tìm token đặt lại mật khẩu theo giá trị token.
     * - Trả về Optional<PasswordResetToken>: có thể rỗng nếu không tìm thấy.
     * - Spring Data JPA sẽ tự động tạo query dựa trên tên phương thức (findByToken).
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Xóa tất cả token liên quan đến một người dùng.
     * - @Modifying: đánh dấu đây là query thay đổi dữ liệu (DELETE).
     * - Spring sẽ tự động tạo query DELETE dựa trên tên phương thức (deleteByUser).
     */
    @Modifying
    void deleteByUser(User user);
    
    /**
     * Xóa tất cả token đã hết hạn (expiryDate < date).
     * - @Modifying: đánh dấu đây là query DELETE.
     * - deleteByExpiryDateBefore: Spring tự động tạo query DELETE WHERE expiry_date < date.
     */
    @Modifying
    void deleteByExpiryDateBefore(LocalDateTime date);
}