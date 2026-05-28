package app.gamification.model;

import lombok.Getter;

@Getter
public enum UserPointAction {
    LOGIN_DAILY("Đăng nhập hàng ngày", 5, 1),
    APPLY("Ứng tuyển việc làm", 10, 3),
    JOB_POST_APPROVED("Đăng tin tuyển dụng", 20, 2),
    INTERVIEW_PRACTICE("Luyện phỏng vấn AI", 15, 1),
    UPLOAD_CV("Cập nhật CV/Profile", 20, 1),
    REVIEW_CV("Duyệt hồ sơ ứng viên", 5, 10),
    HIRED("Tuyển dụng thành công", 50, 5);

    private final String description;
    private final int points;
    private final int dailyLimit;

    UserPointAction(String description, int points, int dailyLimit) {
        this.description = description;
        this.points = points;
        this.dailyLimit = dailyLimit;
    }
}