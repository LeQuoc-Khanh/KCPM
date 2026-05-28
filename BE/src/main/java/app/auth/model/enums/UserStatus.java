
package app.auth.model.enums;

/**
 * Enum UserStatus: Xác định trạng thái tài khoản người dùng trong hệ thống.
 * Các giá trị:
 * - ACTIVE: Tài khoản đang hoạt động bình thường.
 * - INACTIVE: Tài khoản chưa được kích hoạt hoặc bị vô hiệu hóa.
 * - BANNED: Tài khoản bị cấm do vi phạm chính sách.
 * - PENDING_VERIFICATION: Tài khoản đang chờ xác thực (ví dụ: xác thực email).
 */
public enum UserStatus {
    ACTIVE,                // Tài khoản hoạt động
    BANNED,                // Tài khoản bị cấm
    PENDING_VERIFICATION   // Đang chờ xác thực email hoặc thông tin
}