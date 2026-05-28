package app.recruitment.controller;

import app.recruitment.service.CandidateSearchService; // Service này cần gọi tới JobMatchingService của AI
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruitment/search")
@RequiredArgsConstructor
public class CandidateSearchController {

    private final CandidateSearchService candidateSearchService;

    // Tìm kiếm ứng viên phù hợp dựa trên mô tả công việc (Text description)
    @PostMapping("/match-description")
    public ResponseEntity<?> searchCandidatesByDescription(@RequestBody String jobDescription) {
        // Logic: Dùng Gemini hoặc so sánh keyword để trả về list CandidateProfile
        return ResponseEntity.ok(candidateSearchService.searchByJobDescription(jobDescription));
    }
}