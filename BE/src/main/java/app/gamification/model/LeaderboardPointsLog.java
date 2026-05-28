package app.gamification.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "leaderboard_points_log",
    indexes = {
        @Index(name = "idx_lpl_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_lpl_role_created", columnList = "role,created_at"),
        @Index(name = "idx_lpl_action_created", columnList = "action_type,created_at")
    }
)
public class LeaderboardPointsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Lưu role group: CANDIDATE / RECRUITER (gộp VIP)
    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "ref_id")
    private Long refId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // getters/setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
