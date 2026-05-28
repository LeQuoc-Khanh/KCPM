package app.admin.report.model;

import app.admin.report.model.enums.AdminAction;
import app.admin.report.model.enums.ReportStatus;
import app.admin.report.model.enums.ReportTargetType;
import app.admin.report.model.enums.ViolationReason;
import app.auth.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "violation_reports")
public class ViolationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người report
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 100)
    private ViolationReason reason;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "evidence_url", columnDefinition = "text")
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportStatus status = ReportStatus.PENDING;

    // Admin xử lý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "handled_at")
    private OffsetDateTime handledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_action", length = 50)
    private AdminAction adminAction = AdminAction.NONE;

    @Column(name = "target_label", length = 255)
    private String targetLabel;


    @PrePersist
    public void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
