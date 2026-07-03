package app.candidate.controller;

import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateCV;
import app.candidate.repository.CandidateCVRepository;
import app.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidate/cv-builder")
public class CandidateCVController {

    @Autowired
    private CandidateCVRepository cvRepository;

    @Autowired
    private UserRepository userRepository;

    // Save or update CV
    @PostMapping("/save")
    public ResponseEntity<?> saveCV(@RequestBody Map<String, Object> payload) {
        String email = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Unauthorized"));
        User user = userRepository.findByEmail(email).orElseThrow();

        String cvTitle = (String) payload.get("cvTitle");
        String templateType = (String) payload.get("templateType");
        String cvDataJson = (String) payload.get("cvDataJson");
        Long id = payload.containsKey("id") ? ((Number) payload.get("id")).longValue() : null;

        CandidateCV cv;
        if (id != null) {
            cv = cvRepository.findById(id).orElse(new CandidateCV());
        } else {
            cv = new CandidateCV();
            cv.setUser(user);
        }

        cv.setCvTitle(cvTitle);
        cv.setTemplateType(templateType);
        cv.setCvDataJson(cvDataJson);

        CandidateCV savedCv = cvRepository.save(cv);
        return ResponseEntity.ok(CandidateCVDetailResponse.from(savedCv));
    }

    // Return only CV metadata for list view to avoid serializing large/lazy fields.
    @GetMapping("/my-cvs")
    public ResponseEntity<List<CandidateCVSummaryResponse>> getMyCVs() {
        String email = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Unauthorized"));
        User user = userRepository.findByEmail(email).orElseThrow();

        List<CandidateCVSummaryResponse> response = cvRepository.findByUserId(user.getId())
                .stream()
                .map(CandidateCVSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // Return full CV data for detail view.
    @GetMapping("/{id}")
    public ResponseEntity<CandidateCVDetailResponse> getCV(@PathVariable Long id) {
        CandidateCV cv = cvRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(CandidateCVDetailResponse.from(cv));
    }

    public record CandidateCVSummaryResponse(
            Long id,
            String cvTitle,
            String templateType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static CandidateCVSummaryResponse from(CandidateCV cv) {
            return new CandidateCVSummaryResponse(
                    cv.getId(),
                    cv.getCvTitle(),
                    cv.getTemplateType(),
                    cv.getCreatedAt(),
                    cv.getUpdatedAt()
            );
        }
    }

    public record CandidateCVDetailResponse(
            Long id,
            String cvTitle,
            String templateType,
            String cvDataJson,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static CandidateCVDetailResponse from(CandidateCV cv) {
            return new CandidateCVDetailResponse(
                    cv.getId(),
                    cv.getCvTitle(),
                    cv.getTemplateType(),
                    cv.getCvDataJson(),
                    cv.getCreatedAt(),
                    cv.getUpdatedAt()
            );
        }
    }
}
