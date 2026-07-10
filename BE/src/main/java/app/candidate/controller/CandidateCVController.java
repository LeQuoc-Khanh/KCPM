package app.candidate.controller;

import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateCV;
import app.candidate.repository.CandidateCVRepository;
import app.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidate/cv-builder")
public class CandidateCVController {

    @Autowired
    private CandidateCVRepository cvRepository;

    @Autowired
    private UserRepository userRepository;

    // Lưu hoặc Cập nhật CV
    @PostMapping("/save")
    public ResponseEntity<?> saveCV(@RequestBody Map<String, Object> payload) {
        // 1. KIỂM TRA PAYLOAD RỖNG (Fix lỗi 400 Bad Request)
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Dữ liệu gửi lên không được để trống!"
            ));
        }

        String cvTitle = (String) payload.get("cvTitle");
        String templateType = (String) payload.get("templateType");
        String cvDataJson = (String) payload.get("cvDataJson");

        // 2. KIỂM TRA CÁC TRƯỜNG BẮT BUỘC (Tránh lưu null vào DB)
        if (cvTitle == null || cvTitle.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu trường bắt buộc: cvTitle"));
        }
        if (templateType == null || templateType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu trường bắt buộc: templateType"));
        }
        if (cvDataJson == null || cvDataJson.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu trường bắt buộc: cvDataJson"));
        }

        // 3. XỬ LÝ LOGIC LƯU DB BÌNH THƯỜNG
        String email = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Unauthorized"));
        User user = userRepository.findByEmail(email).orElseThrow();
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

        cvRepository.save(cv);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lưu CV thành công",
                "data", cv
        ));
    }

    // Lấy danh sách CV của User
    @GetMapping("/my-cvs")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CandidateCV>> getMyCVs() {
        String email = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Unauthorized"));
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(cvRepository.findByUserId(user.getId()));
    }

    // Lấy chi tiết 1 CV
    @GetMapping("/{id}")
    public ResponseEntity<CandidateCV> getCV(@PathVariable Long id) {
        return ResponseEntity.ok(cvRepository.findById(id).orElseThrow());
    }
}