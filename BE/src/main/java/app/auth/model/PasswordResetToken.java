
package app.auth.model;

// Jakarta Persistence (JPA): cung cấp các annotation để ánh xạ entity với bảng trong DB
import jakarta.persistence.*; // @Entity, @Table, @Index, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn, @EntityListeners, FetchType, GenerationType

// Lombok: tự động sinh getter/setter, toString, equals/hashCode, constructor, builder...
import lombok.AllArgsConstructor; // Tạo constructor với tất cả tham số
import lombok.Builder;            // Hỗ trợ Builder pattern
import lombok.Data;               // Sinh getter/setter, equals/hashCode, toString
import lombok.NoArgsConstructor;  // Tạo constructor không tham số

// Spring Data JPA Auditing: tự động điền các trường thời gian tạo/sửa khi bật auditing
import org.springframework.data.annotation.CreatedDate;               // Đánh dấu trường sẽ tự động set thời điểm tạo
import org.springframework.data.jpa.domain.support.AuditingEntityListener; // Lắng nghe sự kiện auditing để set giá trị cho @CreatedDate/@LastModifiedDate

import java.time.LocalDateTime;

/**
 * Entity PasswordResetToken: Lưu token đặt lại mật khẩu cho người dùng.
 * - @Entity: đánh dấu lớp là một JPA entity.
 * - @Table: cấu hình tên bảng và index.
 * - @EntityListeners(AuditingEntityListener.class): bật tính năng auditing cho entity (tự động set createdAt).
 * - @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor: tiện ích từ Lombok.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token") // Tạo index trên cột token để tra cứu nhanh
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    
    /**
     * Khóa chính (ID) tự tăng.
     * - @Id: đánh dấu là primary key.
     * - @GeneratedValue(strategy = GenerationType.IDENTITY): dùng chiến lược IDENTITY (DB tự tăng).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Giá trị token đặt lại mật khẩu (thường là chuỗi ngẫu nhiên/UUID, có thể mã hóa).
     * - unique = true: đảm bảo token không trùng lặp.
     * - nullable = false: bắt buộc có giá trị.
     * - length = 500: độ dài tối đa của token (dự phòng cho token dài hoặc mã hóa).
     */
    @Column(unique = true, nullable = false, length = 500)
    private String token;
    
    /**
     * Quan hệ nhiều-đến-một với User: mỗi token thuộc về một người dùng.
     * - @ManyToOne(fetch = FetchType.LAZY): tải lười (không fetch User ngay lập tức để tối ưu).
     * - @JoinColumn(name = "user_id", nullable = false): khóa ngoại trỏ đến bảng users, bắt buộc có.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Thời điểm token hết hạn (expiry).
     * - nullable = false: bắt buộc có, để hệ thống kiểm tra token còn hiệu lực hay không.
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    /**
     * Đánh dấu token đã được sử dụng hay chưa (để phòng chống dùng lại).
     * - @Builder.Default: khi khởi tạo bằng builder mà không set, mặc định là false.
     * - nullable = false: luôn có giá trị.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;
    
    /**
     * Thời điểm tạo bản ghi token.
     * - @CreatedDate: tự động gán thời điểm tạo khi lưu entity lần đầu (cần bật auditing).
     * - updatable = false: không cho phép chỉnh sửa sau khi đã set.
     * Lưu ý: để @CreatedDate hoạt động, phải bật JPA Auditing (ví dụ: @EnableJpaAuditing trong cấu hình).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}