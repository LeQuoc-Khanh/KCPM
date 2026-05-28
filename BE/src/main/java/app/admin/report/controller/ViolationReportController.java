package app.admin.report.controller;

import app.admin.report.dto.request.CreateViolationReportRequest;
import app.admin.report.dto.response.ViolationReportResponse;
import app.admin.report.service.ViolationReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ViolationReportController {

    private final ViolationReportService reportService;

    // Bạn lấy reporterId từ SecurityContext/JWT (khuyến khích), ở đây demo truyền thẳng
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ViolationReportResponse create(
            @RequestParam Long reporterId,
            @Valid @RequestBody CreateViolationReportRequest req
    ) {
        return reportService.create(reporterId, req);
    }
}
