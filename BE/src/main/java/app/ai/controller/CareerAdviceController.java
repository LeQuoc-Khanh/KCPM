package app.ai.controller;

import app.ai.service.JobMatchingService;
import app.ai.service.cv.gemini.dto.MatchResult; // [SỬA] Đổi từ CareerAdviceResult sang MatchResult
import app.recruitment.entity.JobApplication;
import app.recruitment.repository.JobApplicationRepository;
import app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matching/advice")
@RequiredArgsConstructor
public class CareerAdviceController {

    private final JobMatchingService jobMatchingService;
    private final JobApplicationRepository applicationRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER')")
    public ResponseEntity<?> getCareerAdvice(@PathVariable Long applicationId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển"));

        // Lấy email người đang gọi API
        String currentUserEmail = securityUtils.getCurrentUserEmail();

        // Kiểm tra quyền sở hữu
        boolean isCandidate = app.getCandidate().getEmail().equals(currentUserEmail);
        boolean isRecruiter = app.getJobPosting().getRecruiter().getEmail().equals(currentUserEmail);

        if (!isCandidate && !isRecruiter) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền truy cập");
        }

        // [SỬA] Gọi hàm getApplicationAnalysis và dùng kiểu dữ liệu MatchResult
        MatchResult result = jobMatchingService.getApplicationAnalysis(applicationId);
        return ResponseEntity.ok(result);
    }
}