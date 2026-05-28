package app.recruitment.service;

import app.ai.service.JobFastMatchingService;
import app.ai.service.cv.gemini.dto.FastMatchResult;
import app.ai.service.cv.gemini.dto.MatchResult;
import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.candidate.model.CandidateProfile;
import app.candidate.repository.CandidateProfileRepository;
import app.recruitment.dto.request.JobApplicationRequest;
import app.recruitment.dto.response.JobApplicationResponse;
import app.recruitment.entity.CVAnalysisResult;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.ApplicationStatus;
import app.recruitment.repository.CVAnalysisResultRepository;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.notification.service.NotificationService;
import app.notification.service.NotificationService;

import org.springframework.context.ApplicationEventPublisher;
import app.gamification.event.PointEvent;
import app.gamification.model.UserPointAction;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository appRepo;
    private final JobPostingRepository jobRepo;
    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final CVAnalysisResultRepository analysisResultRepo;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    // Service tính toán nhanh
    private final JobFastMatchingService fastMatchingService;

    private final NotificationService notificationService;

    @Override
    @Transactional
    public JobApplication apply(Long candidateId, JobApplicationRequest request) {
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + candidateId));

        JobPosting job = jobRepo.findById(request.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + request.getJobId()));

        if (appRepo.existsByCandidateIdAndJobPostingId(candidateId, job.getId())) {
            throw new IllegalArgumentException("Bạn đã ứng tuyển công việc này rồi.");
        }

        String finalCvUrl = request.getCvUrl();
        if (finalCvUrl == null || finalCvUrl.isEmpty()) {
            CandidateProfile profile = profileRepository.findByUserId(candidateId).orElse(null);
            if (profile != null && profile.getCvFilePath() != null) {
                finalCvUrl = profile.getCvFilePath();
            } else {
                throw new IllegalArgumentException("Vui lòng upload CV hoặc cập nhật hồ sơ trước khi ứng tuyển.");
            }
        }

        JobApplication.JobApplicationBuilder appBuilder = JobApplication.builder()
                .jobPosting(job)
                .candidate(candidate)
                .cvUrl(finalCvUrl)
                .coverLetter(request.getCoverLetter())
                .status(ApplicationStatus.PENDING);

        // Logic copy kết quả AI cũ (nếu có)
        Optional<CVAnalysisResult> existingAnalysis =
                analysisResultRepo.findByUserIdAndJobPostingId(candidateId, job.getId());

        if (existingAnalysis.isPresent()) {
            CVAnalysisResult analysis = existingAnalysis.get();
            appBuilder.matchScore(analysis.getMatchPercentage());
            if (analysis.getAnalysisDetails() != null) {
                try {
                    MatchResult matchResult = objectMapper.readValue(analysis.getAnalysisDetails(), MatchResult.class);
                    if (matchResult != null) {
                        appBuilder.aiEvaluation(matchResult.getEvaluation());
                        appBuilder.matchedSkillsCount(matchResult.getMatchedSkillsCount());
                        appBuilder.missingSkillsCount(matchResult.getMissingSkillsCount());
                        appBuilder.otherHardSkillsCount(matchResult.getOtherHardSkillsCount());
                        appBuilder.otherSoftSkillsCount(matchResult.getOtherSoftSkillsCount());
                        
                        // Convert List -> String để lưu vào Entity
                        if (matchResult.getMissingSkillsList() != null) 
                            appBuilder.missingSkillsList(String.join(", ", matchResult.getMissingSkillsList()));
                        if (matchResult.getMatchedSkillsList() != null) 
                            appBuilder.matchedSkillsList(String.join(", ", matchResult.getMatchedSkillsList()));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse analysisDetails JSON: {}", e.getMessage());
                }
            }
        } else {
            appBuilder.matchScore(0);
        }

        JobApplication saved = appRepo.save(appBuilder.build());
        
        // --- CỘNG ĐIỂM APPLY (Candidate) ---
        try {
            // Thay đổi logic cũ: leaderboardService.addPoints(...) -> Bắn Event
            eventPublisher.publishEvent(new PointEvent(
                this, 
                candidateId, 
                "CANDIDATE", 
                UserPointAction.APPLY, 
                saved.getId()
            ));
        } catch (Exception e) {
            log.error("Lỗi bắn event điểm APPLY: {}", e.getMessage());
        }
        // ------------------------------------

        try {
            notificationService.sendNotification(
                    job.getRecruiter().getId(),
                    "Có ứng viên mới!",
                    candidate.getFullName() + " vừa ứng tuyển vào vị trí " + job.getTitle(),
                    "/applications/" + saved.getId()
            );
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo: " + e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public JobApplication updateStatus(Long recruiterId, Long applicationId, ApplicationStatus newStatus, String recruiterNote) {
        JobApplication application = appRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        if (recruiterId != null && !application.getJobPosting().getRecruiter().getId().equals(recruiterId)) {
            throw new IllegalArgumentException("Không có quyền chỉnh sửa đơn ứng tuyển này.");
        }

        application.setStatus(newStatus);
        application.setRecruiterNote(recruiterNote);
        JobApplication saved = appRepo.save(application);

        // --- LOGIC CỘNG ĐIỂM MỚI (REVIEW_CV / HIRED) ---
        if (recruiterId != null) {
            try {
                UserPointAction action = null;

                switch (newStatus) {
                    // Nhóm 1: Duyệt hồ sơ -> REVIEW_CV
                    case SCREENING:
                    case INTERVIEW:
                    case REJECTED:
                        action = UserPointAction.REVIEW_CV;
                        break;

                    // Nhóm 2: Tuyển thành công -> HIRED
                    case OFFERED:
                        action = UserPointAction.HIRED;
                        break;

                    default:
                        break;
                }

                if (action != null) {
                    // Bắn event thay vì gọi service trực tiếp
                    eventPublisher.publishEvent(new PointEvent(
                        this,
                        recruiterId,
                        "RECRUITER",
                        action,
                        saved.getId()
                    ));
                }
            } catch (Exception e) {
                log.error("Lỗi bắn event điểm Recruiter update status: {}", e.getMessage());
            }
        }
        // ------------------------------------------------

        try {
            Long candidateId = application.getCandidate().getId();
            String jobTitle = application.getJobPosting().getTitle();
            String notifTitle = "Cập nhật trạng thái hồ sơ";
            String notifMessage = "";
            String notifLink = "/my-applications"; // Link FE dẫn tới trang lịch sử ứng tuyển

            switch (newStatus) {
                case SCREENING:
                    notifMessage = "Hồ sơ ứng tuyển vị trí " + jobTitle + " của bạn đang được xem xét.";
                    break;
                case INTERVIEW:
                    notifMessage = "Chúc mừng! Bạn đã được mời phỏng vấn cho vị trí " + jobTitle + ".";
                    break;
                case OFFERED:
                    notifMessage = "Tin vui! Bạn đã nhận được lời mời làm việc cho vị trí " + jobTitle + ".";
                    break;
                case REJECTED:
                    notifMessage = "Hồ sơ cho vị trí " + jobTitle + " của bạn chưa phù hợp lúc này.";
                    break;
                default:
                    notifMessage = "Trạng thái hồ sơ vị trí " + jobTitle + " đã thay đổi thành " + newStatus.name();
            }

            notificationService.sendNotification(candidateId, notifTitle, notifMessage, notifLink);

        } catch (Exception e) {
            log.error("Lỗi gửi thông báo cập nhật trạng thái: {}", e.getMessage());
        }

        return saved;
    }

    // --- HÀM LẤY DANH SÁCH (FAST MATCHING) - KHÔNG ĐỔI ---
    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> listByJob(Long jobId) {
        if (!jobRepo.existsById(jobId)) {
            throw new IllegalArgumentException("Job not found with ID: " + jobId);
        }

        List<JobApplication> apps = appRepo.findByJobPostingId(jobId);

        return apps.stream()
                .map(app -> {
                    // 1. Lấy Skills
                    List<String> candidateSkills = Collections.emptyList();
                    CandidateProfile profile = profileRepository.findByUserId(app.getCandidate().getId()).orElse(null);
                    if (profile != null && profile.getSkills() != null) {
                        candidateSkills = profile.getSkills();
                    }

                    // 2. Gọi hàm tính nhanh
                    Map<Long, FastMatchResult> batchResult = fastMatchingService.calculateBatchCompatibility(
                        candidateSkills, 
                        Collections.singletonList(jobId)
                    );
                    
                    FastMatchResult result = batchResult.get(jobId);
                    
                    int score = (result != null) ? result.getMatchScore() : 0;
                    
                    String matchedStr = (result != null && result.getMatchedSkills() != null) 
                                        ? String.join(", ", result.getMatchedSkills()) : "";
                    String missingStr = (result != null && result.getMissingSkills() != null) 
                                        ? String.join(", ", result.getMissingSkills()) : "";

                    String companyName = (app.getJobPosting().getCompany() != null) 
                                       ? app.getJobPosting().getCompany().getName() : "Unknown Company";
                    String studentName = (app.getCandidate() != null) 
                                       ? app.getCandidate().getFullName() : "Unknown Candidate";

                    return JobApplicationResponse.builder()
                            .id(app.getId())
                            .jobId(jobId)
                            .jobTitle(app.getJobPosting().getTitle())
                            .companyName(companyName)
                            .studentId(app.getCandidate().getId())
                            .studentName(studentName)
                            .cvUrl(app.getCvUrl())
                            .status(app.getStatus().name())
                            .appliedAt(app.getAppliedAt())
                            
                            // Gán dữ liệu Fast Match
                            .matchScore(score)
                            .matchedSkillsList(matchedStr) 
                            .missingSkillsList(missingStr)
                            
                            .aiEvaluation("Đánh giá nhanh từ khóa")
                            .recruiterNote(app.getRecruiterNote())
                            .build();
                })
                .sorted((a, b) -> {
                     int s1 = a.getMatchScore() == null ? 0 : a.getMatchScore();
                     int s2 = b.getMatchScore() == null ? 0 : b.getMatchScore();
                     return s2 - s1;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<JobApplicationResponse> scanAndSuggestCandidates(Long jobId) {
        return listByJob(jobId);
    }

    @Override
    public List<JobApplication> listByCandidateId(Long candidateId) {
        return appRepo.findByCandidateId(candidateId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsByCandidateId(Long candidateId) {
        List<JobApplication> applications = appRepo.findByCandidateId(candidateId);
        return applications.stream().map(app -> {
            JobPosting job = app.getJobPosting();
            String companyName = (job.getCompany() != null) ? job.getCompany().getName() : "Unknown Company";
            return JobApplicationResponse.builder()
                    .id(app.getId())
                    .jobId(job.getId())
                    .jobTitle(job.getTitle())
                    .companyName(companyName)
                    .studentId(app.getCandidate().getId())
                    .studentName(app.getCandidate().getFullName())
                    .cvUrl(app.getCvUrl())
                    .status(app.getStatus().name())
                    .appliedAt(app.getAppliedAt())
                    .recruiterNote(app.getRecruiterNote())
                    .matchScore(app.getMatchScore()) 
                    .aiEvaluation(app.getAiEvaluation())
                    .build();
        }).collect(Collectors.toList());
    }
    
    @Override
    public Optional<JobApplication> getById(Long id) {
        return appRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApplied(Long candidateId, Long jobId) {
        return appRepo.existsByCandidateIdAndJobPostingId(candidateId, jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getDetail(Long id) {
        JobApplication app = appRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ ID: " + id));


        String phoneStr = "---";
        CandidateProfile profile = profileRepository.findByUserId(app.getCandidate().getId()).orElse(null);
        if (profile != null) {
            phoneStr = profile.getPhoneNumber();
        }

        String displayEmail = (profile != null && profile.getEmail() != null)
                ? profile.getEmail()
                : app.getCandidate().getEmail();

        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJobPosting().getId())
                .jobTitle(app.getJobPosting().getTitle())
                .studentId(app.getCandidate().getId())
                .studentName(app.getCandidate().getFullName())
                .email(displayEmail)
                .phone(phoneStr)
                .cvUrl(app.getCvUrl())
                .status(app.getStatus().name())
                .appliedAt(app.getAppliedAt())
                .matchScore(app.getMatchScore())
                .aiEvaluation(app.getAiEvaluation())
                .missingSkillsList(app.getMissingSkillsList())
                .matchedSkillsList(app.getMatchedSkillsList())
                .recruiterNote(app.getRecruiterNote())
                .build();
    }
}