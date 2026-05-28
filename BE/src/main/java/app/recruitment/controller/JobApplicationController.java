package app.recruitment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

import app.recruitment.service.JobApplicationService;
import app.ai.service.JobMatchingService;
import app.auth.dto.response.MessageResponse;
import app.recruitment.dto.request.JobApplicationRequest;
import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.mapper.RecruitmentMapper;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.enums.ApplicationStatus;
import app.auth.repository.UserRepository;
import app.ai.service.cv.gemini.dto.MatchResult;

@RestController
@RequestMapping("/api/applications") // Để chung là applications
@RequiredArgsConstructor
@Slf4j
public class JobApplicationController {

    private final JobApplicationService applicationService;
    private final UserRepository userRepository;
    private final RecruitmentMapper mapper;
    private final JobMatchingService jobMatchingService;

    /**
     * ỨNG VIÊN NỘP ĐƠN
     */
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@Valid @RequestBody JobApplicationRequest request) {
        Long candidateId = getCurrentUserId();
        JobApplication created = applicationService.apply(candidateId, request);
        
        return ResponseEntity.status(201).body(MessageResponse.success(
            "Ứng tuyển thành công!", 
            mapper.toJobApplicationResponse(created)
        ));
    }

    /**
     * RECRUITER CẬP NHẬT TRẠNG THÁI (DUYỆT/LOẠI)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus newStatus,
            @RequestParam(required = false) String recruiterNote
    ) {
        Long recruiterId = getCurrentUserId();
        JobApplication updated = applicationService.updateStatus(recruiterId, id, newStatus, recruiterNote);
        return ResponseEntity.ok(mapper.toJobApplicationResponse(updated));
    }

    /**
     * CƠ CHẾ LẤY ID TỪ TOKEN (ĐÃ FIX LỖI 1L)
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    /**
     * LẤY DANH SÁCH ĐƠN ĐÃ NỘP CỦA TÔI (DÀNH CHO CANDIDATE)
     * API này cực kỳ quan trọng để Frontend lấy được applicationId gọi sang AI
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyApplications() {
        Long candidateId = getCurrentUserId();
        // Gọi hàm trả về DTO trực tiếp đã viết ở Service
List<JobApplicationResponse> list = applicationService.getApplicationsByCandidateId(candidateId);
        
        return ResponseEntity.ok(MessageResponse.success(
            "Lấy danh sách ứng tuyển thành công", 
            list
        ));
    }

    @GetMapping("/check/{jobId}")
    public ResponseEntity<?> checkApplicationStatus(@PathVariable Long jobId) {
        try {
            Long candidateId = getCurrentUserId();
            boolean hasApplied = applicationService.hasApplied(candidateId, jobId);

            // Trả về JSON: { "hasApplied": true/false }
            return ResponseEntity.ok(java.util.Collections.singletonMap("hasApplied", hasApplied));
        } catch (Exception e) {
            // Trường hợp chưa đăng nhập hoặc lỗi token -> Coi như chưa apply
            return ResponseEntity.ok(java.util.Collections.singletonMap("hasApplied", false));
        }
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplicationResponse>> listByJob(@PathVariable Long jobId) {
        // Service đã xử lý transaction và mapping, Controller chỉ việc trả về
        List<JobApplicationResponse> list = applicationService.listByJob(jobId);
        return ResponseEntity.ok(list);
    }

   @PostMapping("/{id}/analysis") 
    public ResponseEntity<?> analyzeApplication(@PathVariable Long id) {
        try {
            // Gọi hàm phân tích thông minh bên Service
            MatchResult result = jobMatchingService.analyzeOneApplication(id);
            
            // Trả về thành công kèm MessageResponse
            return ResponseEntity.ok(MessageResponse.success(
                "Phân tích AI hoàn tất!", 
                result
            ));
        } catch (Exception e) {
            log.error("Lỗi phân tích AI cho đơn {}: {}", id, e.getMessage());
            
            // Trả về lỗi 400 kèm message chi tiết
            return ResponseEntity.badRequest().body(MessageResponse.error(
                "Phân tích thất bại: " + e.getMessage()
            ));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationDetail(@PathVariable Long id) {
        // Gọi thẳng hàm getDetail mà bạn đã viết trong Service
        JobApplicationResponse response = applicationService.getDetail(id);
        
        return ResponseEntity.ok(response);
    }}