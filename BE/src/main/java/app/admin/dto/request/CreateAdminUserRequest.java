package app.admin.dto.request;

import app.auth.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAdminUserRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    // Có thể để trống -> backend tự sinh mật khẩu tạm
    @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6-72 ký tự")
    private String password;

    @NotNull(message = "Vai trò không được để trống")
    private UserRole userRole;
}
