package app.recruitment.controller;

import app.recruitment.dto.response.RecruiterDashboardResponse;
import app.recruitment.service.RecruiterDashboardService;
import app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import app.recruitment.dto.response.JobApplicationResponse;
import java.util.List;

@RestController
@RequestMapping("/api/recruiter/dashboard")
@RequiredArgsConstructor
public class RecruiterDashboardController {

    private final RecruiterDashboardService dashboardService;
    private final SecurityUtils securityUtils;

    @GetMapping("/stats")
    public ResponseEntity<RecruiterDashboardResponse> getDashboardStats() {
        Long recruiterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getDashboardStats(recruiterId));
    }
    @GetMapping("/recent-applications")
    public ResponseEntity<List<JobApplicationResponse>> getRecentApplications() {
        Long recruiterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getRecentApplications(recruiterId));
}
}