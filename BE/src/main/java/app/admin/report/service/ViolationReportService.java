package app.admin.report.service;

import app.admin.report.dto.request.CreateViolationReportRequest;
import app.admin.report.dto.request.UpdateReportStatusRequest;
import app.admin.report.dto.response.ViolationReportResponse;
import app.admin.report.model.ViolationReport;
import app.admin.report.model.enums.AdminAction;
import app.admin.report.model.enums.ReportStatus;
import app.admin.report.model.enums.ReportTargetType;
import app.admin.report.repository.ViolationReportRepository;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.notification.service.NotificationService;
import app.recruitment.entity.JobApplication;
import app.recruitment.entity.JobPosting;
import app.recruitment.entity.enums.ApplicationStatus;
import app.recruitment.entity.enums.JobStatus;
import app.recruitment.repository.JobApplicationRepository;
import app.recruitment.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViolationReportService {

    private final ViolationReportRepository reportRepo;
    private final UserRepository userRepo;
    private final JobPostingRepository jobPostingRepo;
    private final JobApplicationRepository jobAppRepo;
    private final NotificationService notificationService;

    @Transactional
    public ViolationReportResponse create(Long reporterId, CreateViolationReportRequest req) {
        User reporter = userRepo.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found: " + reporterId));

        validateTargetExists(req.targetType(), req.targetId());

        ViolationReport r = new ViolationReport();
        r.setReporter(reporter);
        r.setTargetType(req.targetType());
        r.setTargetId(req.targetId());
        r.setReason(req.reason());
        r.setDescription(req.description());
        r.setEvidenceUrl(req.evidenceUrl());
        r.setStatus(ReportStatus.PENDING);

        try {
            List<User> admins = userRepo.findByUserRole(UserRole.ADMIN);
            String title = "⚠️ Báo cáo vi phạm mới: " + req.targetType();
            String message = "User " + reporter.getFullName() + " vừa báo cáo một nội dung. Lý do: " + req.reason();
            String link = "/admin/violation-reports"; // Link trang quản lý reports

            for (User admin : admins) {
                notificationService.sendNotification(admin.getId(), title, message, link);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo cho Admin: " + e.getMessage());
        }

        return toResponse(reportRepo.save(r));
    }

    public long countPending() {
        return reportRepo.countByStatus(ReportStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Page<ViolationReportResponse> list(ReportStatus status, Pageable pageable) {
        if (status == null) return reportRepo.findAll(pageable).map(this::toResponse);
        return reportRepo.findByStatus(status, pageable).map(this::toResponse);
    }

    /**
     * Quy ước:
     * - INVALID: report sai -> không action
     * - VALID: report đúng -> chỉ xác nhận, chưa action
     * - RESOLVED: đã action xong (lock/hide/delete)
     */
    @Transactional
    public ViolationReportResponse updateStatus(Long reportId, Long adminId, UpdateReportStatusRequest req) {
        ViolationReport r = reportRepo.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));

        // audit
        r.setStatus(req.status());
        r.setAdminNote(req.adminNote());
        r.setHandledBy(admin);
        r.setHandledAt(OffsetDateTime.now());

        // nếu entity ViolationReport đã có field adminAction thì lưu (khuyến khích)
        try { r.setAdminAction(req.action()); } catch (Exception ignored) {}

        // INVALID -> kết thúc
        if (req.status() == ReportStatus.INVALID) {
            return toResponse(r);
        }

        // VALID -> chỉ xác nhận đúng, không can thiệp dữ liệu
        if (req.status() == ReportStatus.VALID) {
            return toResponse(r);
        }

        // RESOLVED -> thực thi action
        if (req.status() == ReportStatus.RESOLVED) {
            applyAction(r, req.action(), req.adminNote());
            return toResponse(r);
        }

        // PENDING (ít dùng) -> trả về
        return toResponse(r);
    }

    private void validateTargetExists(ReportTargetType type, Long targetId) {
        switch (type) {
            case JOB_POSTING -> jobPostingRepo.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("JobPosting not found: " + targetId));
            case JOB_APPLICATION -> jobAppRepo.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("JobApplication not found: " + targetId));
            case USER -> userRepo.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + targetId));
            case COMPANY -> {
                // companyRepo.findById(targetId).orElseThrow(...)
            }
            default -> throw new IllegalArgumentException("Unsupported targetType: " + type);
        }
    }

    private void applyAction(ViolationReport r, AdminAction action, String adminNote) {
        if (action == null) action = AdminAction.NONE;

        switch (action) {
            case NONE, WARN -> {
                // Không can thiệp entity
            }

            // USER
            case LOCK_USER -> {
                ensureTarget(r, ReportTargetType.USER);
                User u = userRepo.findById(r.getTargetId())
                        .orElseThrow(() -> new RuntimeException("User not found: " + r.getTargetId()));
                u.setStatus(UserStatus.BANNED);
            }
            case UNLOCK_USER -> {
                ensureTarget(r, ReportTargetType.USER);
                User u = userRepo.findById(r.getTargetId())
                        .orElseThrow(() -> new RuntimeException("User not found: " + r.getTargetId()));
                u.setStatus(UserStatus.ACTIVE);
            }

            case LOCK_COMPANY, UNLOCK_COMPANY -> {
                ensureTarget(r, ReportTargetType.COMPANY);
                // - LOCK: company.status = BLOCKED
                // - UNLOCK: company.status = ACTIVE
                // - optional: LOCK_COMPANY thì set job của company -> HIDDEN/BLOCKED
            }

            // JOB
            case HIDE_JOB -> {
                ensureTarget(r, ReportTargetType.JOB_POSTING);
                JobPosting job = jobPostingRepo.findById(r.getTargetId())
                        .orElseThrow(() -> new RuntimeException("JobPosting not found: " + r.getTargetId()));

                // ✅ Ẩn job đúng nghĩa theo enum của bạn
                job.setStatus(JobStatus.HIDDEN);
            }
            case DELETE_JOB -> {
                ensureTarget(r, ReportTargetType.JOB_POSTING);
                jobPostingRepo.deleteById(r.getTargetId());
            }

            // APPLICATION
            case HIDE_APPLICATION -> {
                ensureTarget(r, ReportTargetType.JOB_APPLICATION);
                JobApplication app = jobAppRepo.findById(r.getTargetId())
                        .orElseThrow(() -> new RuntimeException("JobApplication not found: " + r.getTargetId()));

                // ✅ MVP: Ẩn application = REJECTED (không cần sửa enum)
                app.setStatus(ApplicationStatus.REJECTED);

                // note để phân biệt recruiter reject vs admin hidden
                String note = "[ADMIN] Hidden due to violation report #" + r.getId();
                if (adminNote != null && !adminNote.isBlank()) note += " - " + adminNote;
                app.setRecruiterNote(note);
            }
            case DELETE_APPLICATION -> {
                ensureTarget(r, ReportTargetType.JOB_APPLICATION);
                jobAppRepo.deleteById(r.getTargetId());
            }

            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    private void ensureTarget(ViolationReport r, ReportTargetType expected) {
        if (r.getTargetType() != expected) {
            throw new IllegalArgumentException(
                    "Action không khớp targetType. Expected " + expected + " but got " + r.getTargetType()
            );
        }
    }

    private ViolationReportResponse toResponse(ViolationReport r) {
        return new ViolationReportResponse(
                r.getId(),
                r.getReporter().getId(),
                safeName(r.getReporter()),
                r.getTargetType(),
                r.getTargetId(),
                r.getReason(),
                r.getDescription(),
                r.getEvidenceUrl(),
                r.getStatus(),
                r.getHandledBy() != null ? r.getHandledBy().getId() : null,
                r.getHandledBy() != null ? safeName(r.getHandledBy()) : null,
                r.getAdminNote(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getHandledAt()
        );
    }

    private String safeName(User u) {
        return u.getFullName();
    }
}
