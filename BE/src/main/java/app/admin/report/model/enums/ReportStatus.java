package app.admin.report.model.enums;

public enum ReportStatus {
    PENDING,     // chờ xử lý (hiện badge)
    VALID,       // báo cáo đúng (xác nhận vi phạm)
    INVALID,     // báo cáo sai
    RESOLVED     // đã xử lý xong (ví dụ đã khoá tin/ user)
}
