package app.admin.report.dto.response;

import java.time.OffsetDateTime;

import app.admin.report.model.enums.ReportStatus;
import app.admin.report.model.enums.ReportTargetType;
import app.admin.report.model.enums.ViolationReason;

public record ViolationReportResponse(
        Long id,
        Long reporterId,
        String reporterName,

        ReportTargetType targetType,
        Long targetId,

        ViolationReason reason,
        String description,
        String evidenceUrl,

        ReportStatus status,

        Long handledById,
        String handledByName,
        String adminNote,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime handledAt
) {}
