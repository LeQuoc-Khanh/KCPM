package app.admin.report.controller;

import app.admin.report.dto.request.UpdateReportStatusRequest;
import app.admin.report.dto.response.ViolationReportResponse;
import app.admin.report.model.enums.ReportStatus;
import app.admin.report.service.ViolationReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/violation-reports")
public class AdminViolationReportController {

    private final ViolationReportService reportService;

    // FE dùng endpoint này để lấy số (badge)
    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return Map.of("pendingCount", reportService.countPending());
    }

    // List report, filter theo status (optional)
    @GetMapping
    public Page<ViolationReportResponse> list(
            @RequestParam(required = false) ReportStatus status,
            Pageable pageable
    ) {
        return reportService.list(status, pageable);
    }

    // Admin cập nhật trạng thái
    @PatchMapping("/{id}/status")
    public ViolationReportResponse updateStatus(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @Valid @RequestBody UpdateReportStatusRequest req
    ) {
        return reportService.updateStatus(id, adminId, req);
    }
}
