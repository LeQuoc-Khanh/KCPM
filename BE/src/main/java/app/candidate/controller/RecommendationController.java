package app.candidate.controller;

import app.auth.dto.response.MessageResponse;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.service.JobRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidate/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final JobRecommendationService recommendationService;
    private final UserRepository userRepository;

    // API 1: Lấy việc làm phù hợp (Có tính điểm & Lọc >= 50%)
    // URL: /api/candidate/recommendations/matching
    @GetMapping("/matching")
    public ResponseEntity<?> getMatchingJobs() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            var jobs = recommendationService.getMatchingJobs(user.getId());
            return ResponseEntity.ok(MessageResponse.success("Gợi ý việc làm phù hợp thành công", jobs));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllJobs() {
        try {
            var jobs = recommendationService.getAllJobs();
            return ResponseEntity.ok(MessageResponse.success("Lấy danh sách tất cả việc làm thành công", jobs));
        }catch(Exception e)
        {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
        }

    // API 2: Lấy việc làm mới nhất (Top 10, Không tính điểm)
    // URL: /api/candidate/recommendations/recent
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentJobs() {
        try {
            var jobs = recommendationService.getRecentJobs();
            return ResponseEntity.ok(MessageResponse.success("Lấy danh sách việc làm mới nhất thành công", jobs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MessageResponse.error(e.getMessage()));
        }
    }
}