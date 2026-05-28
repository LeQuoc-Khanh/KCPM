package app.admin.report.model.enums;

public enum AdminAction {
    NONE,
    WARN,

    // User
    LOCK_USER,
    UNLOCK_USER,

    // Company
    LOCK_COMPANY,
    UNLOCK_COMPANY,

    // Job
    HIDE_JOB,     // set status -> DRAFT/REJECTED
    DELETE_JOB,

    // Application
    HIDE_APPLICATION,  // set status -> CANCELED/REJECTED (nếu có)
    DELETE_APPLICATION
}
