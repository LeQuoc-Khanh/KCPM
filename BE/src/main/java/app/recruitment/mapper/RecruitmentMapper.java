package app.recruitment.mapper;

import org.springframework.stereotype.Component;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.JobApplication;
import app.recruitment.dto.response.JobPostingResponse;
import app.recruitment.dto.response.JobApplicationResponse;
import app.content.model.Company; // [1] Import model Company

@Component
public class RecruitmentMapper {

    public JobPostingResponse toJobPostingResponse(JobPosting entity) {
        if (entity == null) return null;

        // 1. Map thông tin Recruiter (Người đăng)
        Long recruiterId = null;
        String recruiterName = null;
        if (entity.getRecruiter() != null) {
            recruiterId = entity.getRecruiter().getId();
            recruiterName = entity.getRecruiter().getFullName();
        }

        // 2. Khởi tạo Builder với các thông tin cơ bản
        JobPostingResponse.JobPostingResponseBuilder responseBuilder = JobPostingResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .requirements(entity.getRequirements())
                .salaryRange(entity.getSalaryRange())
                .location(entity.getLocation())
                .expiryDate(entity.getExpiryDate())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .recruiterId(recruiterId)
                .recruiterName(recruiterName)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        // 3. Map thông tin Công ty (QUAN TRỌNG)
        // Ưu tiên lấy từ entity.getCompany() (liên kết trực tiếp)
        Company company = entity.getCompany();

        // Nếu null, thử lấy fallback qua recruiter (trường hợp tin cũ hoặc logic dự phòng)
        if (company == null && entity.getRecruiter() != null) {
            company = entity.getRecruiter().getCompany();
        }

        // Nếu tìm thấy công ty, set dữ liệu vào response
        if (company != null) {
            responseBuilder.companyId(company.getId());
            responseBuilder.companyName(company.getName());
            responseBuilder.companyLogo(company.getLogoUrl()); // Chú ý: Entity của bạn dùng logoUrl
            responseBuilder.companyWebsite(company.getWebsite());
            responseBuilder.companyDescription(company.getDescription());
            responseBuilder.companyAddress(company.getAddress());
        }

        return responseBuilder.build();
    }

    public JobApplicationResponse toJobApplicationResponse(JobApplication entity) {
        if (entity == null) return null;

        Long studentId = null;
        String studentName = null;
        String jobTitle = null;

        if (entity.getCandidate() != null) {
            studentId = entity.getCandidate().getId();
            studentName = entity.getCandidate().getFullName();
        }

        if (entity.getJobPosting() != null) {
            jobTitle = entity.getJobPosting().getTitle();
        }

        return JobApplicationResponse.builder()
                .id(entity.getId())
                .jobId(entity.getJobPosting() != null ? entity.getJobPosting().getId() : null)
                .jobTitle(jobTitle)
                .studentId(studentId)
                .studentName(studentName)
                .cvUrl(entity.getCvUrl())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .appliedAt(entity.getAppliedAt())
                .recruiterNote(entity.getRecruiterNote())
                .build();
    }
}