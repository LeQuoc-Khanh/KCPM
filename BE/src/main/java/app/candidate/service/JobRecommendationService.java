package app.candidate.service;

import app.ai.service.JobFastMatchingService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.entity.JobPosting;
import app.recruitment.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.recruitment.entity.enums.JobStatus;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobRecommendationService {


    private final CandidateProfileRepository profileRepository;
    private final JobPostingRepository jobRepository;
    private final JobFastMatchingService fastMatchingService;

    // Lấy tất cả JOB
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllJobs() {
        List<JobPosting> jobs = jobRepository.findByStatus(JobStatus.PUBLISHED);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (JobPosting job : jobs) {
            resultList.add(mapJobToBasicInfo(job));
        }
        return resultList;
    }

    // --- HÀM 1: LẤY 10 JOB MỚI NHẤT (KHÔNG TÍNH ĐIỂM) ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentJobs() {
        // ✅ Sửa: Sử dụng hàm có sẵn trong Repository để lấy Top 10 Published mới nhất
        List<JobPosting> jobs = jobRepository.findTop10ByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED);

        List<Map<String, Object>> recentJobs = new ArrayList<>();
        for (JobPosting job : jobs) {
            recentJobs.add(mapJobToBasicInfo(job));
        }
        return recentJobs;
    }

    // --- HÀM 2: LỌC JOB CÓ ĐỘ TƯƠNG THÍCH >= 50% ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMatchingJobs(Long userId) {
        // 1. Lấy Profile & Skill
        Optional<CandidateProfile> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isEmpty() || profileOpt.get().getSkills().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> candidateSkills = profileOpt.get().getSkills();

        // 2. Lấy tất cả Job ID đanng PUBLISHED
        List<Long> allJobIds = jobRepository.findByStatus(JobStatus.PUBLISHED).stream()
                .map(JobPosting::getId)
                .collect(Collectors.toList());
        if (allJobIds.isEmpty()) return Collections.emptyList();

        // 3. Tính điểm nhanh (Batch)
        Map<Long, FastMatchResult> scores = fastMatchingService.calculateBatchCompatibility(candidateSkills, allJobIds);

        // 4. Lọc Job có điểm >= 50
        List<Long> passedJobIds = scores.entrySet().stream()
                .filter(entry -> entry.getValue().getMatchScore() >= 50) // Sử dụng getScore() hoặc getMatchScore() tùy DTO của bạn
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (passedJobIds.isEmpty()) return Collections.emptyList();

        // 5. Lấy thông tin chi tiết các Job đã lọc
        List<JobPosting> passedJobs = jobRepository.findAllById(passedJobIds);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (JobPosting job : passedJobs) {
            FastMatchResult result = scores.get(job.getId());
            if (result != null) {
                Map<String, Object> jobMap = mapJobToBasicInfo(job);
                // Gán thêm thông tin điểm số
                jobMap.put("matchScore", result.getMatchScore());
                jobMap.put("skillsFound", result.getMatchedSkills());
                jobMap.put("skillsMissing", result.getMissingSkills());
                resultList.add(jobMap);
            }
        }

        // 6. Sắp xếp điểm giảm dần
        resultList.sort((j1, j2) -> {
            Integer s1 = (Integer) j1.get("matchScore");
            Integer s2 = (Integer) j2.get("matchScore");
            return s2.compareTo(s1);
        });

        return resultList;
    }

    // Helper map dữ liệu cơ bản để tránh lặp code
    private Map<String, Object> mapJobToBasicInfo(JobPosting job) {
        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put("id", job.getId());
        jobMap.put("title", job.getTitle() != null ? job.getTitle() : "Chưa có tiêu đề");
        jobMap.put("company", job.getCompany() != null ? job.getCompany().getName() : "Công ty ẩn danh");
        jobMap.put("companyLogo", job.getCompany() != null ? job.getCompany().getLogoUrl() : null);
        jobMap.put("location", job.getLocation() != null ? job.getLocation() : "Remote");
        jobMap.put("salary", job.getSalaryRange() != null ? job.getSalaryRange() : "Thỏa thuận");
        jobMap.put("description", job.getDescription());
        jobMap.put("requirements", job.getRequirements());
        jobMap.put("createdAt", job.getCreatedAt()); // Để hiển thị ngày đăng
        return jobMap;
    }
}