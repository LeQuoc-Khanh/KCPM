
package app.auth.model.enums;

/**
 * Enum AuthProvider: Xác định nguồn (provider) dùng để xác thực người dùng.
 * - LOCAL: Người dùng đăng ký và đăng nhập trực tiếp qua hệ thống (email + mật khẩu).
 * - GOOGLE: Người dùng đăng nhập thông qua Google OAuth (đăng nhập bằng tài khoản Google).
 */
public enum AuthProvider {
    LOCAL,   // Đăng nhập bằng tài khoản nội bộ (email/password)
    GOOGLE   // Đăng nhập bằng Google OAuth
}
