package app.recruitment.controller;

import app.auth.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

import app.recruitment.service.JobPostingService;
import app.util.SecurityUtils;
import app.recruitment.dto.request.JobPostingRequest;
import app.recruitment.dto.response.JobPostingResponse;
import app.recruitment.mapper.RecruitmentMapper;
import app.recruitment.entity.JobPosting;

@RestController
@RequestMapping("/api/recruiter/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final RecruitmentMapper mapper;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<JobPostingResponse> create(@Valid @RequestBody JobPostingRequest request) {
        Long recruiterId = securityUtils.getCurrentUserId();
        JobPosting created = jobPostingService.create(recruiterId, request);
        JobPostingResponse resp = mapper.toJobPostingResponse(created);
        return ResponseEntity.created(URI.create("/api/recruiter/jobs/" + resp.getId())).body(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobPostingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobPostingRequest request
    ) {
        Long recruiterId = securityUtils.getCurrentUserId();
        JobPosting updated = jobPostingService.update(recruiterId, id, request);
        return ResponseEntity.ok(mapper.toJobPostingResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        Long recruiterId = securityUtils.getCurrentUserId();
        jobPostingService.delete(recruiterId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable Long id) {
        JobPosting job = jobPostingService.getById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        return ResponseEntity.ok(mapper.toJobPostingResponse(job));
    }

    // Trong file JobPostingController.java
    @GetMapping("/me")
    public ResponseEntity<List<JobPostingResponse>> listByRecruiter() {
        Long recruiterId = securityUtils.getCurrentUserId();
        
        // Service đã trả về DTO rồi, không cần stream().map(...) nữa
        return ResponseEntity.ok(jobPostingService.listByRecruiter(recruiterId));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(@RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(MessageResponse.success("Tìm kiếm thành công", jobPostingService.searchJobs(keyword)));
    }
    @GetMapping("/public/{id}")
    public ResponseEntity<JobPostingResponse> getJobDetailPublic(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getJobDetailPublic(id));
    }

}