package app.ai.controller;

import app.ai.service.JobMatchingService;
import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.entity.JobApplication;
import app.recruitment.mapper.RecruitmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matching/recruiter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')") // Bảo vệ toàn bộ class
public class RecruiterMatchingController {

    private final JobMatchingService jobMatchingService;
    private final RecruitmentMapper mapper;

    // API 1: Kích hoạt sàng lọc
    @PostMapping("/screen/{jobId}")
    public ResponseEntity<?> triggerScreening(@PathVariable Long jobId) {
        jobMatchingService.screenApplications(jobId);
        return ResponseEntity.ok("Hệ thống đang tiến hành phân tích CV và chấm điểm. Vui lòng quay lại xem Bảng xếp hạng sau vài phút.");
    }

    // API 2: Xem bảng xếp hạng
    @GetMapping("/ranking/{jobId}")
    public ResponseEntity<List<JobApplicationResponse>> getRankedList(
            @PathVariable Long jobId,
            @RequestParam(required = false, defaultValue = "0") Integer minScore
    ) {
        List<JobApplication> applications = jobMatchingService.getRankedApplications(jobId, minScore);
        List<JobApplicationResponse> response = applications.stream()
                .map(mapper::toJobApplicationResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}