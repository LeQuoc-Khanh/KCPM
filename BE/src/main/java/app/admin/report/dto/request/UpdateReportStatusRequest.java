package app.admin.report.dto.request;

import app.admin.report.model.enums.AdminAction;
import app.admin.report.model.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReportStatusRequest(
        @NotNull ReportStatus status,
        @NotNull AdminAction action,
        String adminNote
) {}
