package app.admin.service;

import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobPostingRepository;
import app.admin.dto.response.AdminJobPostingItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final JobPostingRepository jobPostingRepository;

    public Page<AdminJobPostingItemResponse> getPending(Pageable pageable) {
        return jobPostingRepository.findByStatus(JobStatus.PENDING, pageable).map(this::toDto);
    }

    public Page<AdminJobPostingItemResponse> getAll(String status, Pageable pageable) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return jobPostingRepository.findAll(pageable).map(this::toDto);
        }
        JobStatus st = JobStatus.valueOf(status.toUpperCase());
        return jobPostingRepository.findByStatus(st, pageable).map(this::toDto);
    }

    @Transactional
    public void approve(Long jobId, Long adminId) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(JobStatus.PUBLISHED);
    }

    @Transactional
    public void reject(Long jobId, Long adminId, String reason) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(JobStatus.REJECTED);
    }

    private AdminJobPostingItemResponse toDto(JobPosting j) {
        String companyName = (j.getCompany() != null) ? j.getCompany().getName() : null;
        return new AdminJobPostingItemResponse(
                j.getId(),
                j.getTitle(),
                companyName,
                j.getLocation(),
                j.getStatus().name(),
                j.getCreatedAt()
        );
    }
}
