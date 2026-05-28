package app.recruitment.service;

import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.dto.response.RecruiterDashboardResponse;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.mapper.RecruitmentMapper; // Import Mapper
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterDashboardService {

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RecruitmentMapper recruitmentMapper; // Inject Mapper để convert Entity -> DTO

    @Transactional(readOnly = true)
    public RecruiterDashboardResponse getDashboardStats(Long recruiterId) {
        // 1. Đếm số tin đang tuyển (PUBLISHED)
        long activeJobs = jobPostingRepository.countByRecruiterIdAndStatus(recruiterId, JobStatus.PUBLISHED);

        // 2. Tổng số ứng viên
        long totalCandidates = jobApplicationRepository.countByJobPostingRecruiterId(recruiterId);

        // 3. Ứng viên mới trong ngày (từ 00:00 hôm nay)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long newToday = jobApplicationRepository.countByJobPostingRecruiterIdAndAppliedAtAfter(recruiterId, startOfDay);

        // 4. Pipeline stats (Thống kê theo trạng thái)
        List<Object[]> statusCounts = jobApplicationRepository.countApplicationsByStatus(recruiterId);
        Map<String, Long> pipelineMap = new HashMap<>();
        
        // Convert List<Object[]> sang Map<String, Long>
        for (Object[] row : statusCounts) {
            String status = row[0].toString(); // Tên Enum
            Long count = (Long) row[1];
            pipelineMap.put(status, count);
        }

        return RecruiterDashboardResponse.builder()
                .totalActiveJobs(activeJobs)
                .totalCandidates(totalCandidates)
                .newCandidatesToday(newToday)
                .pipelineStats(pipelineMap)
                .build();
    }

    /**
     * Lấy danh sách ứng viên nộp đơn gần đây (Giới hạn 10 bản ghi)
     * Phục vụ cho bảng "Recent Candidates" ở Dashboard
     */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getRecentApplications(Long recruiterId) {
        // Tạo Pageable để lấy 10 bản ghi đầu tiên
        Pageable limit = PageRequest.of(0, 10);
        
        // Gọi Repository để lấy list Entity
        // Lưu ý: Cần đảm bảo method findRecentApplicationsByRecruiter đã có trong Repository
        List<JobApplication> recentApps = jobApplicationRepository.findRecentApplicationsByRecruiter(recruiterId, limit);

        // Convert danh sách Entity sang danh sách DTO response dùng Mapper
        return recentApps.stream()
                .map(recruitmentMapper::toJobApplicationResponse)
                .collect(Collectors.toList());
    }
}