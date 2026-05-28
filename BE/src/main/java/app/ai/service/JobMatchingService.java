package app.ai.service;

import app.ai.service.cv.CVAnalysisService;
import app.ai.service.cv.gemini.GeminiService;
import app.ai.service.cv.gemini.dto.MatchResult;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.entity.CVAnalysisResult;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchingService {
    private final CVAnalysisService cvAnalysisService;
    private final GeminiService geminiService;
    private final JobApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CandidateProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CVAnalysisResultRepository analysisRepository;

    /**
     * LUỒNG 1: Preview cho ứng viên (Giữ nguyên logic dùng Cache CVAnalysisResult)
     * Vì CVAnalysisResult vẫn giữ cấu trúc cũ (AnalysisDetails JSON) nên hàm này không đổi.
     */
    @Transactional
    public MatchResult matchCandidateWithJobAI(Long userId, String cvContent, Long jobId, String cvUrl) {
        // 1. Kiểm tra Cache
        Optional<CVAnalysisResult> existing = analysisRepository.findByUserIdAndJobPostingId(userId, jobId);
        
        // Chuẩn bị thông tin ứng viên
        CandidateProfile profile = profileRepository.findByUserId(userId).orElse(null);
        String cName = (profile != null) ? profile.getFullName() : "Ứng viên";

        if (existing.isPresent()) {
            try {
                MatchResult cachedResult = objectMapper.readValue(existing.get().getAnalysisDetails(), MatchResult.class);
                
                // Gán thông tin bổ sung
                if (existing.get().getJobPosting() != null) {
                    cachedResult.setJobTitle(existing.get().getJobPosting().getTitle());
                    cachedResult.setCompany(existing.get().getJobPosting().getCompany().getName());
                }
                cachedResult.setCandidateName(cName);
                
                return cachedResult;
            } catch (Exception e) {
                log.warn("Lỗi đọc cache AI. Sẽ phân tích lại.");
            }
        }

        // 2. Chuẩn bị dữ liệu nếu chưa có Cache
        JobPosting job = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 3. Gọi AI
        MatchResult result = geminiService.matchCVWithJob(
            cvContent,
            StringUtils.hasText(job.getDescription()) ? job.getDescription() : "",
            StringUtils.hasText(job.getRequirements()) ? job.getRequirements() : ""
        );

        // Gán thông tin bổ sung cho Result trả về
        result.setJobTitle(job.getTitle());
        if (job.getCompany() != null) {
            result.setCompany(job.getCompany().getName());
        }
        result.setCandidateName(cName);

        // 4. Lưu Cache vào CVAnalysisResult (Vẫn lưu Full JSON để ứng viên xem chi tiết)
        try {
            CVAnalysisResult entity = existing.orElse(new CVAnalysisResult());
            entity.setUser(user);
            entity.setJobPosting(job);
            entity.setMatchPercentage(result.getMatchPercentage());
            entity.setCvUrlUsed(cvUrl);
            entity.setAnalysisDetails(objectMapper.writeValueAsString(result));
            entity.setAnalyzedAt(java.time.LocalDateTime.now());

            analysisRepository.save(entity);
        } catch (Exception e) {
            log.error("Không lưu được kết quả AI vào Cache: {}", e.getMessage());
        }

        return result;
    }

    /**
     * LUỒNG 2: Sàng lọc hồ sơ (Bulk Screening)
     * [CẬP NHẬT] Map đúng vào Entity JobApplication mới (Bỏ LearningPath)
     */
    @Async
    @Transactional
    public void screenApplications(Long jobId) {
        log.info("Bắt đầu sàng lọc hồ sơ cho Job ID: {}", jobId);

        List<JobApplication> applications = applicationRepository.findByJobPostingId(jobId);
        if (applications.isEmpty()) return;

        JobPosting job = jobPostingRepository.findById(jobId).orElseThrow();
        String jobDesc = StringUtils.hasText(job.getDescription()) ? job.getDescription() : "";
        String jobReq = StringUtils.hasText(job.getRequirements()) ? job.getRequirements() : "";

        for (JobApplication app : applications) {
            // Chỉ chạy nếu chưa có điểm
            if (app.getMatchScore() == null || app.getMatchScore() == 0) {
                try {
                    CandidateProfile profile = profileRepository.findByUserId(app.getCandidate().getId()).orElse(null);

                    if (profile != null) {
                        String candidateData = buildCandidateDataForAI(profile);

                        // Gọi AI
                        MatchResult result = geminiService.matchCVWithJob(candidateData, jobDesc, jobReq);

                        // --- [FIX] MAP DỮ LIỆU VÀO ENTITY MỚI ---
                        app.setMatchScore(result.getMatchPercentage());
                        app.setAiEvaluation(result.getEvaluation()); // Nhận xét ngắn gọn
                        app.setMatchedSkillsCount(result.getMatchedSkillsCount());
                        app.setMissingSkillsCount(result.getMissingSkillsCount());

                        // Lưu danh sách skill thiếu dạng chuỗi "A, B, C"
                        if (result.getMissingSkillsList() != null && !result.getMissingSkillsList().isEmpty()) {
                            app.setMissingSkillsList(String.join(", ", result.getMissingSkillsList()));
                        } else {
                            app.setMissingSkillsList("");
                        }
                        
                        // [ĐÃ BỎ] Không setLearningPath, ExtraSkillsCount nữa
                        
                        applicationRepository.save(app);
                        log.info("Đã chấm điểm xong đơn ID: {}", app.getId());
                    }
                } catch (Exception e) {
                    log.error("Lỗi chấm điểm đơn ID {}: {}", app.getId(), e.getMessage());
                }
            }
        }
        log.info("Hoàn tất sàng lọc Job ID: {}", jobId);
    }

    /**
     * LUỒNG 3: Lấy danh sách xếp hạng
     */
    public List<JobApplication> getRankedApplications(Long jobId, Integer minScore) {
        int scoreThreshold = (minScore != null) ? minScore : 0;
        return applicationRepository.findByJobPostingIdAndMatchScoreGreaterThanEqualOrderByMatchScoreDesc(
                jobId, scoreThreshold
        );
    }

    /**
     * LUỒNG 4: Xem chi tiết (Recruiter View)
     * [CẬP NHẬT] Reconstruct từ các cột có sẵn, không parse JSON nữa
     */
    @Transactional(readOnly = true)
    public MatchResult getApplicationAnalysis(Long applicationId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Reconstruct MatchResult từ dữ liệu đơn giản trong DB
        List<String> missingSkills = new ArrayList<>();
        if (StringUtils.hasText(app.getMissingSkillsList())) {
            // Tách chuỗi "A, B, C" thành List
            missingSkills = Arrays.asList(app.getMissingSkillsList().split(",\\s*"));
        }

        return MatchResult.builder()
                .matchPercentage(app.getMatchScore() != null ? app.getMatchScore() : 0)
                .evaluation(app.getAiEvaluation())
                .matchedSkillsCount(app.getMatchedSkillsCount())
                .missingSkillsCount(app.getMissingSkillsCount())
                .missingSkillsList(missingSkills)
                // Các trường khác Recruiter không cần xem (LearningPath...) để null hoặc rỗng
                .learningPath(null) 
                .careerAdvice(null)
                .build();
    }
    /**
     * LUỒNG 5: Xem chi tiết (Recruiter View)
     * [CẬP NHẬT] Reconstruct từ các cột có sẵn, không parse JSON nữa
     */
    @Transactional
    public MatchResult analyzeOneApplication(Long applicationId) {
        // 1. Lấy thông tin đơn ứng tuyển
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        User candidate = app.getCandidate();
        JobPosting job = app.getJobPosting();

        // 2. [QUAN TRỌNG] Kiểm tra xem đã có kết quả phân tích trong bảng chuyên dụng (CVAnalysisResult) chưa
        // Bảng này chứa JSON chi tiết (learningPath, chi tiết skills...) mà JobApplication không lưu hết.
        Optional<CVAnalysisResult> existingAnalysis = analysisRepository.findByUserIdAndJobPostingId(candidate.getId(), job.getId());

        if (existingAnalysis.isPresent()) {
            try {
                // Nếu có cache, parse JSON trả về ngay -> Tiết kiệm tiền AI và thời gian
                log.info("Tìm thấy cache CVAnalysisResult cho Application ID: {}", applicationId);
                return objectMapper.readValue(existingAnalysis.get().getAnalysisDetails(), MatchResult.class);
            } catch (Exception e) {
                log.warn("Lỗi đọc JSON cache cũ, sẽ tiến hành phân tích lại.");
            }
        }

        // 3. Nếu chưa có -> Bắt đầu quy trình phân tích mới
        log.info("Bắt đầu gọi AI phân tích cho Application ID: {}", applicationId);

        // A. Lấy dữ liệu đầu vào (Ưu tiên Profile DB -> Fallback File CV)
        String cvText = "";
        
        // Thử lấy từ Profile database trước (Dữ liệu sạch hơn)
        try {
            CandidateProfile profile = profileRepository.findByUserId(candidate.getId()).orElse(null);
            if (profile != null) {
                cvText = buildCandidateDataForAI(profile);
            }
        } catch (Exception e) {
            log.warn("Lỗi lấy profile: {}", e.getMessage());
        }

        // Nếu không có Profile, dùng Extractor đọc file CV từ URL
        if (!StringUtils.hasText(cvText) || cvText.length() < 50) {
             try {
                cvText = cvAnalysisService.getTextFromUrl(app.getCvUrl());
            } catch (Exception e) {
                throw new RuntimeException("Không thể đọc nội dung CV: " + e.getMessage());
            }
        }

        if (!StringUtils.hasText(cvText)) {
            throw new RuntimeException("Nội dung CV quá ngắn hoặc không thể trích xuất.");
        }

        // B. Lấy thông tin Job
        String jobDesc = StringUtils.hasText(job.getDescription()) ? job.getDescription() : "";
        String jobReq = StringUtils.hasText(job.getRequirements()) ? job.getRequirements() : "";

        // C. Gọi Gemini AI
        MatchResult result = geminiService.matchCVWithJob(cvText, jobDesc, jobReq);

        // D. [QUAN TRỌNG] LƯU KẾT QUẢ VÀO CV_ANALYSIS_RESULT (Bảng chứa JSON chi tiết)
        try {
            CVAnalysisResult analysis = existingAnalysis.orElse(new CVAnalysisResult());
            analysis.setUser(candidate);
            analysis.setJobPosting(job);
            analysis.setMatchPercentage(result.getMatchPercentage());
            analysis.setCvUrlUsed(app.getCvUrl());
            // Lưu toàn bộ object Result xuống dạng JSON
            analysis.setAnalysisDetails(objectMapper.writeValueAsString(result)); 
            analysis.setAnalyzedAt(java.time.LocalDateTime.now());
            
            analysisRepository.save(analysis);
            log.info("Đã lưu kết quả chi tiết vào bảng CVAnalysisResult");
        } catch (Exception e) {
            log.error("Lỗi lưu CVAnalysisResult: {}", e.getMessage());
        }

        // E. ĐỒNG BỘ SANG JOB_APPLICATION (Bảng danh sách)
        // Vẫn cần lưu các chỉ số này để Recruiter có thể Filter/Sort ở danh sách bên ngoài
        app.setMatchScore(result.getMatchPercentage());
        app.setAiEvaluation(result.getEvaluation());
        app.setMatchedSkillsCount(result.getMatchedSkillsCount());
        app.setMissingSkillsCount(result.getMissingSkillsCount());

        if (result.getMissingSkillsList() != null) {
            app.setMissingSkillsList(String.join(", ", result.getMissingSkillsList()));
        }
        if (result.getMatchedSkillsList() != null) {
             app.setMatchedSkillsList(String.join(", ", result.getMatchedSkillsList()));
        }

        applicationRepository.save(app);

        // F. Trả về kết quả
        return result;
    }

    // --- PRIVATE HELPER (Giữ nguyên) ---
    private String buildCandidateDataForAI(CandidateProfile profile) throws Exception {
        Map<String, Object> aiInputMap = new HashMap<>();
        aiInputMap.put("fullName", profile.getFullName());
        aiInputMap.put("aboutMe", StringUtils.hasText(profile.getAboutMe()) ? profile.getAboutMe() : "Không có mô tả"); 
        
        if (StringUtils.hasText(profile.getEducationJson())) {
            try {
                aiInputMap.put("education", objectMapper.readTree(profile.getEducationJson()));
            } catch (Exception e) {
                aiInputMap.put("education", profile.getEducationJson()); 
            }
        }

        if (profile.getSkills() != null) aiInputMap.put("skills", profile.getSkills());

        if (profile.getExperiences() != null) {
            var expList = profile.getExperiences().stream().map(exp -> {
                Map<String, Object> eMap = new HashMap<>();
                eMap.put("company", exp.getCompany());
                eMap.put("role", exp.getRole());
                eMap.put("description", exp.getDescription());
                eMap.put("startDate", exp.getStartDate() != null ? exp.getStartDate().toString() : "");
                eMap.put("endDate", exp.getEndDate() != null ? exp.getEndDate().toString() : "Present");
                return eMap;
            }).collect(Collectors.toList());
            aiInputMap.put("experiences", expList);
        }
        return objectMapper.writeValueAsString(aiInputMap);
    }
}