package app.ai.controller;

import app.ai.service.JobFastMatchingService;
import app.ai.service.JobMatchingService;
import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.ai.service.cv.gemini.dto.MatchResult;
import app.candidate.model.CandidateProfile;
import app.candidate.service.CandidateService;
import app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matching/candidate")
@RequiredArgsConstructor
@Slf4j
public class CandidateMatchingController {

    private final JobFastMatchingService fastMatchingService;
    private final JobMatchingService jobMatchingService;
    private final CVAnalysisService cvAnalysisService;
    private final CandidateService candidateService;
    private final SecurityUtils securityUtils; // Inject SecurityUtils

    // API 1: Tính điểm nhanh (Trang chủ)
    @PostMapping("/batch-scores")
    public ResponseEntity<Map<Long, FastMatchResult>> getBatchScores(@RequestBody List<Long> jobIds) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            CandidateProfile profile = candidateService.getProfileForMatching(userId);

            if (profile == null) {
                return ResponseEntity.ok(Map.of());
            }
            // Lấy List Skill từ DB (Logic mới tối ưu)
            List<String> candidateSkills = profile.getSkills();
            
            // Gọi Service Nhanh
            Map<Long, FastMatchResult> scores = fastMatchingService.calculateBatchCompatibility(candidateSkills, jobIds);
            
            return ResponseEntity.ok(scores);
        } catch (Exception e) {
            log.error("Lỗi tính điểm batch: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // API 2: Phân tích CV cụ thể
    @GetMapping("/preview/{jobId}")
    public ResponseEntity<MatchResult> previewMatch(@PathVariable Long jobId) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            CandidateProfile profile = candidateService.getProfile(userId);
            
            if (profile == null || profile.getCvFilePath() == null) {
                return ResponseEntity.badRequest().build();
            }

            // AI cần đọc text file để phân tích sâu
            String cvText = cvAnalysisService.getTextFromUrl(profile.getCvFilePath());
            MatchResult result = jobMatchingService.matchCandidateWithJobAI(userId, cvText, jobId, profile.getCvFilePath());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
             log.error("Lỗi preview match: ", e);
             return ResponseEntity.internalServerError().build();
        }
    }
}