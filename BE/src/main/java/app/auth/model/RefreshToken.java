
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

import java.time.Instant;         // Mốc thời gian UTC (không phụ thuộc timezone), phù hợp cho thời điểm hết hạn token
import java.time.LocalDateTime;   // Ngày-giờ theo hệ thống (có thể phụ thuộc timezone hệ thống)

/**
 * Entity RefreshToken: Lưu token làm mới (refresh token) liên kết với người dùng.
 * - @Entity: đánh dấu lớp là một JPA entity.
 * - @Table: khai báo tên bảng và index để tối ưu tra cứu.
 * - @EntityListeners(AuditingEntityListener.class): bật auditing để tự động set createdAt.
 * - @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor: tiện ích từ Lombok.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token_careermate", columnList = "token") // Tạo index trên cột token để tìm nhanh
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    /**
     * Khóa chính (ID) tự tăng.
     * - @Id: đánh dấu là primary key.
     * - @GeneratedValue(strategy = GenerationType.IDENTITY): chiến lược IDENTITY, DB tự tăng.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Giá trị refresh token (chuỗi ngẫu nhiên/UUID, có thể mã hóa).
     * - unique = true: đảm bảo không trùng lặp token.
     * - nullable = false: bắt buộc có giá trị.
     * - length = 500: dự phòng cho token dài hoặc mã hóa.
     */
    @Column(unique = true, nullable = false, length = 500)
    private String token;
    
    /**
     * Quan hệ nhiều-đến-một với User: mỗi refresh token gắn với một người dùng.
     * - @ManyToOne(fetch = FetchType.LAZY): tải lười (không fetch User ngay để tối ưu).
     * - @JoinColumn(name = "user_id", nullable = false): khóa ngoại tới bảng users, bắt buộc có.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Thời điểm hết hạn của refresh token.
     * - Dùng Instant: biểu diễn thời gian UTC, tránh phụ thuộc timezone (phù hợp kiểm tra expiry).
     * - nullable = false: bắt buộc có, phục vụ kiểm tra token còn hiệu lực.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
    
    /**
     * Thời điểm tạo bản ghi.
     * - @CreatedDate: tự động gán thời điểm tạo khi entity được persist lần đầu (cần bật auditing).
     * - updatable = false: không cho phép chỉnh sửa sau khi set.
     * Lưu ý: để @CreatedDate hoạt động, cần bật JPA Auditing (ví dụ: @EnableJpaAuditing trong cấu hình).
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
