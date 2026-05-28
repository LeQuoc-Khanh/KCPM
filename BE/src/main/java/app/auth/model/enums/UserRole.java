package app.auth.model.enums;

/**
 * Enum UserRole: Xác định vai trò của người dùng trong hệ thống.
 * Các giá trị:
 * - CANDIDATE: Người tìm việc (Ứng viên).
 * - RECRUITER: Người đăng tuyển (Nhà tuyển dụng).
 * - ADMIN: Quản trị viên hệ thống.
 *
 * Mỗi vai trò có một displayName (tên hiển thị) để dùng trong giao diện hoặc thông báo.
 */
public enum UserRole {
    CANDIDATE("Ứng viên"),       // Vai trò ứng viên
    CANDIDATE_VIP("Ứng viên VIP"), // Vai trò ứng viên VIP
    RECRUITER("Nhà tuyển dụng"), // Vai trò nhà tuyển dụng
    RECRUITER_VIP("Nhà tuyển dụng VIP"), // Vai trò nhà tuyển dụng VIP
    ADMIN("Quản trị viên");      // Vai trò quản trị viên


    /**
     * displayName: Tên hiển thị thân thiện cho người dùng (tiếng Việt).
     */
    private final String displayName;

    /**
     * Constructor của enum để gán giá trị displayName cho từng vai trò.
     */
    UserRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Getter để lấy tên hiển thị của vai trò.
     * @return displayName (ví dụ: "Ứng viên", "Nhà tuyển dụng", "Quản trị viên").
     */
    public String getDisplayName() {
        return displayName;
    }
}