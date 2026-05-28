package app.ai.service;

import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.recruitment.entity.JobPosting;
import app.recruitment.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobFastMatchingService {

    private final JobPostingRepository jobPostingRepository;

    /**
     * TÍNH ĐIỂM NHANH (Jaccard)
     * Chạy trên RAM, không tốn tiền AI.
     */
    public Map<Long, FastMatchResult> calculateBatchCompatibility(List<String> candidateSkills, List<Long> jobIds) {
        Map<Long, FastMatchResult> scores = new HashMap<>();
        
        // 1. XỬ LÝ TRƯỜNG HỢP ỨNG VIÊN KHÔNG CÓ SKILL
        // Trả về list rỗng [] thay vì null để Frontend không bị lỗi map()
        if (candidateSkills == null || candidateSkills.isEmpty()) {
            jobIds.forEach(id -> scores.put(id, new FastMatchResult(0, Collections.emptyList(), Collections.emptyList())));
            return scores;
        }

        // 2. Chuẩn hóa skill ứng viên (Lower case + Trim + Set để tìm nhanh O(1))
        Set<String> userSkillSet = candidateSkills.stream()
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toSet());

        // 3. Query DB lấy Job (Đã tối ưu)
        List<JobPosting> jobs = jobPostingRepository.findAllByIdsWhithSkills(jobIds); // Đảm bảo tên hàm đúng với Repo của bạn

        for (JobPosting job : jobs) {
            // Lấy list skill từ DB (Có thể null hoặc rỗng)
            List<String> jobSkillsRaw = job.getExtractedSkills();
            
            // XỬ LÝ TRƯỜNG HỢP JOB KHÔNG YÊU CẦU SKILL
            if (jobSkillsRaw == null || jobSkillsRaw.isEmpty()) {
                // Job dễ tính, không yêu cầu gì -> Match 100% hoặc 0% tùy policy (thường là 0 để không gây nhiễu)
                scores.put(job.getId(), new FastMatchResult(0, Collections.emptyList(), Collections.emptyList()));
                continue;
            }

            // --- TÍNH TOÁN LOGIC (MATCHED vs MISSING) ---

            // A. Tìm Matched (Màu Xanh): Có trong Job VÀ User cũng có
            List<String> matched = jobSkillsRaw.stream()
                    .filter(skill -> userSkillSet.contains(skill.toLowerCase().trim()))
                    .collect(Collectors.toList());

            // B. Tìm Missing (Màu Đỏ): Có trong Job NHƯNG User không có
            List<String> missing = jobSkillsRaw.stream()
                    .filter(skill -> !userSkillSet.contains(skill.toLowerCase().trim()))
                    .collect(Collectors.toList());

            // C. Tính điểm số (0 - 100)
            // Công thức: (Số skill trùng / Tổng skill Job) * 100
            int score = 0;
            if (!jobSkillsRaw.isEmpty()) {
                score = (int) Math.round(((double) matched.size() / jobSkillsRaw.size()) * 100);
            }

            // 4. Lưu vào Map
            scores.put(job.getId(), new FastMatchResult(score, matched, missing));
        }
        
        return scores;
    }
    
}