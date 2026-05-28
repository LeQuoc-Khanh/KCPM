package app.admin.dto.request;

import app.auth.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRoleRequest {
    @NotNull(message = "Vai trò không được để trống")
    private UserRole userRole;
}
