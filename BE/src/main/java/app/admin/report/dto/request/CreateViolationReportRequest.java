package app.admin.report.dto.request;

import app.admin.report.model.enums.ReportTargetType;
import app.admin.report.model.enums.ViolationReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateViolationReportRequest(
        @NotNull ReportTargetType targetType,
        @NotNull Long targetId,
        @NotNull ViolationReason reason,
        @Size(max = 2000) String description,
        @Size(max = 2000) String evidenceUrl
) {}
