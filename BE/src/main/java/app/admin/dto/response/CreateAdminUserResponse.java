package app.admin.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAdminUserResponse {
    private AdminUserResponse user;
    private String generatedPassword; // null nếu admin tự nhập password
}
