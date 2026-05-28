package app.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private UserRole userRole;
    private UserStatus status;
    private String profileImageUrl;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    // --- THÊM FIELD MỚI ---
    private LocalDateTime vipExpirationDate;
}