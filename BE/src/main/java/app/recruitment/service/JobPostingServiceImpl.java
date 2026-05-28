package app.recruitment.service;

import app.ai.service.cv.gemini.GeminiService;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import app.recruitment.dto.request.JobPostingRequest;
import app.recruitment.dto.response.JobPostingResponse;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.mapper.RecruitmentMapper;
import app.recruitment.repository.JobApplicationRepository; // MỚI: Import Repo này
import app.recruitment.repository.JobPostingRepository;
import app.auth.repository.CompanyRepository;
import app.content.model.Company;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.gamification.event.PointEvent;
import app.gamification.model.UserPointAction;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    private static final Logger log = LoggerFactory.getLogger(JobPostingServiceImpl.class);

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    // MỚI: Thêm Repository này để đếm số lượng đơn ứng tuyển
    private final JobApplicationRepository jobApplicationRepository; 
    private final RecruitmentMapper recruitmentMapper;
    private final GeminiService geminiService;
    private final ApplicationEventPublisher eventPublisher;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public JobPosting create(Long recruiterId, JobPostingRequest request) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new IllegalArgumentException("Recruiter not found: " + recruiterId));
        
        if (recruiter.getUserRole() != UserRole.RECRUITER && recruiter.getUserRole() != UserRole.RECRUITER_VIP) {
             throw new RuntimeException("Only recruiter can create job postings");
        }
        Company company = companyRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng cập nhật thông tin công ty trước khi đăng bài!"));
        List<String> skills = new ArrayList<>();
        try {
            skills = geminiService.extractSkillsFromJob(request.getDescription(), request.getRequirements());
        } catch (Exception e) {
            log.error("Lỗi khi trích xuất kỹ năng bằng AI (Vẫn tiếp tục tạo Job): {}", e.getMessage());
        }

        LocalDateTime expiryDateTime = request.getExpiryDate().atTime(LocalTime.MAX);

        JobPosting j = JobPosting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .salaryRange(request.getSalaryRange())
                .location(request.getLocation())
                .expiryDate(expiryDateTime)
                .extractedSkills(skills)
                .recruiter(recruiter)
                .company(company)
                .status(JobStatus.PENDING)
                .build();

        JobPosting savedJob = jobPostingRepository.save(j);

        try {
            eventPublisher.publishEvent(new PointEvent(
                this,
                recruiterId,
                "RECRUITER",
                UserPointAction.JOB_POST_APPROVED,
                savedJob.getId()
            ));
        } catch (Exception e) {
            log.error("Lỗi bắn event tính điểm JOB_POST_APPROVED: {}", e.getMessage());
        }

        return savedJob;
    }

    @Override
    @Transactional
    public JobPosting update(Long recruiterId, Long jobId, JobPostingRequest request) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new IllegalArgumentException("Unauthorized: cannot edit job of another recruiter");
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setSalaryRange(request.getSalaryRange());
        job.setLocation(request.getLocation());

        if (request.getExpiryDate() != null) {
            job.setExpiryDate(request.getExpiryDate().atTime(LocalTime.MAX));
        }

        try {
            List<String> newSkills = geminiService.extractSkillsFromJob(request.getDescription(), request.getRequirements());
            job.setExtractedSkills(newSkills);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật kỹ năng bằng AI (Giữ nguyên kỹ năng cũ): {}", e.getMessage());
        }

        if (request.getStatus() != null) {
            try {
                job.setStatus(JobStatus.valueOf(request.getStatus()));
            } catch (Exception e) {
                log.warn("Invalid job status: {}", request.getStatus());
            }
        }
        return jobPostingRepository.save(job);
    }

    @Override
    @Transactional
    public void delete(Long recruiterId, Long jobId) {
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw new IllegalArgumentException("Unauthorized: cannot delete job of another recruiter");
        }
        job.setStatus(JobStatus.DELETED); 
        jobPostingRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobPosting> getById(Long id) {
        return jobPostingRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingResponse> listByRecruiter(Long recruiterId) {
       List<JobPosting> jobs = jobPostingRepository.findByRecruiterIdAndStatusNot(recruiterId, JobStatus.DELETED);
        return jobs.stream()
                .map(job -> {
                    JobPostingResponse res = recruitmentMapper.toJobPostingResponse(job);
                    // MỚI: Đếm số lượng hồ sơ cho từng job trong danh sách
                    int count = (int) jobApplicationRepository.countByJobPostingId(job.getId());
                    res.setApplicationCount(count);
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPosting> searchByTitle(String keyword) {
        return jobPostingRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingResponse> getAllJobPostings() {
        return jobPostingRepository.findAll().stream()
                .map(recruitmentMapper::toJobPostingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingResponse> searchJobs(String keyword) {
        List<JobPosting> jobs;
        if (keyword == null || keyword.trim().isEmpty()) {
            jobs = jobPostingRepository.findTop10ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED);
        } else {
            jobs = jobPostingRepository.searchJobs(keyword.trim());
        }
        return jobs.stream()
                .map(recruitmentMapper::toJobPostingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostingResponse getJobDetailPublic(Long id) {
        JobPosting job = jobPostingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        // if (job.getStatus() == JobStatus.DELETED || job.getStatus() == JobStatus.HIDDEN || job.getStatus() == JobStatus.BLOCKED) {
        //      throw new IllegalArgumentException("Công việc này chưa được công khai hoặc đã bị đóng.");
        // }
        JobPostingResponse response = recruitmentMapper.toJobPostingResponse(job);

        int applicationCount = (int) jobApplicationRepository.countByJobPostingId(id);
        response.setApplicationCount(applicationCount);

        return response;
    }
}