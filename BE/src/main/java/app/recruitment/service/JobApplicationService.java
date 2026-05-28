package app.recruitment.service;

import app.recruitment.dto.request.JobApplicationRequest;
import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.enums.ApplicationStatus;

import java.util.List;
import java.util.Optional;

public interface JobApplicationService {
    
    // Ứng viên nộp đơn
    JobApplication apply(Long candidateId, JobApplicationRequest request);

    // Recruiter cập nhật trạng thái
    JobApplication updateStatus(Long recruiterId, Long applicationId, ApplicationStatus newStatus, String recruiterNote);
    
    List<JobApplicationResponse> listByJob(Long jobId);

    List<JobApplication> listByCandidateId(Long candidateId);

    // Lấy danh sách DTO theo Candidate (trả về thẳng cho Frontend)
    List<JobApplicationResponse> getApplicationsByCandidateId(Long candidateId);

    List<JobApplicationResponse> scanAndSuggestCandidates(Long jobId);
    // Lấy chi tiết đơn
    Optional<JobApplication> getById(Long id);
    
    JobApplicationResponse getDetail(Long id);

    boolean hasApplied(Long candidateId, Long jobId);
}
