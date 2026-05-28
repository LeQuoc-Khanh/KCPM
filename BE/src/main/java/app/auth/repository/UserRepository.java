
package app.auth.repository;

// Spring Data JPA: cung cấp interface JpaRepository và annotation hỗ trợ
import org.springframework.data.jpa.repository.JpaRepository; // Interface cho CRUD và query mặc định
import org.springframework.data.jpa.repository.Query;          // Cho phép viết JPQL hoặc native query tùy chỉnh
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;             // Đánh dấu interface là Repository (thành phần DAO)
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import app.auth.model.User;
import app.auth.model.enums.UserStatus;
import app.auth.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho entity User.
 * - Kế thừa JpaRepository<User, Long>: cung cấp các phương thức CRUD mặc định.
 * - @Repository: đánh dấu là bean quản lý bởi Spring, phục vụ thao tác DB.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Tìm người dùng theo email.
     * - Trả về Optional<User>: có thể rỗng nếu không tìm thấy.
     * - Spring Data JPA sẽ tự động tạo query dựa trên tên phương thức (findByEmail).
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Tìm người dùng theo Google ID (dùng cho đăng nhập bằng Google).
     * - Trả về Optional<User>: có thể rỗng nếu không tìm thấy.
     */
    Optional<User> findByGoogleId(String googleId);
    
    /**
     * Kiểm tra email đã tồn tại trong hệ thống hay chưa.
     * - Trả về Boolean: true nếu tồn tại, false nếu không.
     */
    Boolean existsByEmail(String email);
    
    /**
     * Lấy danh sách người dùng theo trạng thái (ACTIVE, INACTIVE, BANNED...).
     * - Trả về List<User>: danh sách người dùng có status tương ứng.
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * Tìm người dùng theo email nhưng chỉ khi trạng thái là ACTIVE.
     * - @Query: sử dụng JPQL để viết query tùy chỉnh.
     * - ':email' là tham số truyền vào.
     * - Trả về Optional<User>: có thể rỗng nếu không tìm thấy.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveUserByEmail(String email);

        // ===== SEARCH (dùng chung) =====
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName,String email,Pageable pageable);

    List<User> findByUserRole(UserRole role);

    // ===== ADMIN: search + exclude current admin id =====
    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :excludeId
          AND (
            :keyword IS NULL OR :keyword = '' OR
            LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
          AND (:role IS NULL OR u.userRole = :role)
    """)
    Page<User> searchUsersExcludeId(
            @Param("excludeId") Long excludeId,
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            Pageable pageable
    );
    @Query("select u.userRole, count(u) from User u group by u.userRole")
    List<Object[]> countUsersByRole();
}