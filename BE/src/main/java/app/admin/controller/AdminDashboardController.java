package app.admin.controller;

import app.admin.service.SummaryService;
import app.admin.service.RecentActivityService;
import app.admin.service.ApplicationByDay;
import app.auth.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {
        private final SummaryService SummaryService;
        private final RecentActivityService RecentActivityService;
        private final ApplicationByDay ApplicationByDayService;

    @GetMapping("/summary")
    public ResponseEntity<MessageResponse> summary() {
        return ResponseEntity.ok(
                MessageResponse.success("Lấy thống kê thành công", SummaryService.getSummary())
        );
    }

    @GetMapping("/applications-chart")
    public ResponseEntity<MessageResponse> applicationsChart(
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("Request for applications chart with last {} days", days);
        return ResponseEntity.ok(
                MessageResponse.success("Lấy dữ liệu biểu đồ thành công", ApplicationByDayService.getApplicationsChart(days))
        );
    }

    // ✅ chỉ hoạt động ứng tuyển, giới hạn số dòng
    @GetMapping("/recent-activities")
    public ResponseEntity<MessageResponse> recentActivities(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(
                MessageResponse.success("Lấy hoạt động gần đây thành công", RecentActivityService.getRecentActivities(limit))
        );
    }

    // ✅ danh sách đầy đủ hoạt động ứng tuyển (có phân trang)
    @GetMapping("/all-activities")
    public ResponseEntity<MessageResponse> allActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                MessageResponse.success("Lấy hoạt động thành công", RecentActivityService.getAllActivities(page, size))
        );
    }
}
