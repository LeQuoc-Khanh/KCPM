package app.recruitment.service;

import app.recruitment.dto.request.JobPostingRequest;
import app.recruitment.dto.response.JobPostingResponse;
import app.recruitment.entity.JobPosting; // Import Enum

import java.util.List;
import java.util.Optional;

public interface JobPostingService {
    JobPosting create(Long recruiterId, JobPostingRequest request);
    JobPosting update(Long recruiterId, Long jobId, JobPostingRequest request);
    void delete(Long recruiterId, Long jobId);
    Optional<JobPosting> getById(Long id);
    List<JobPostingResponse> listByRecruiter(Long recruiterId);
    List<JobPosting> searchByTitle(String keyword);

    // Các hàm phục vụ Candidate
    List<JobPostingResponse> searchJobs(String keyword);
    List<JobPostingResponse> getAllJobPostings(); // Bổ sung hàm này
    JobPostingResponse getJobDetailPublic(Long id);
}