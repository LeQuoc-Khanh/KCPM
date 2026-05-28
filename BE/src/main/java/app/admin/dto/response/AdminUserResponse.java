package app.admin.dto.response;

import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private UserRole userRole;
    private UserStatus status;
    private LocalDateTime createdAt;
}

