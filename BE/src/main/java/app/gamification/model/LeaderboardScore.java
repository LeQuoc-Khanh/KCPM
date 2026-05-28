package app.gamification.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "leaderboard_scores",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_ls_user_role_period",
            columnNames = {"user_id", "role", "period_type", "period_key"}
        )
    },
    indexes = {
        @Index(name = "idx_ls_leaderboard_top", columnList = "role,period_type,period_key,score"),
        @Index(name = "idx_ls_user_lookup", columnList = "user_id,role,period_type,period_key")
    }
)
public class LeaderboardScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="role", nullable=false, length=32)
    private String role; // CANDIDATE / RECRUITER

    @Column(name="period_type", nullable=false, length=16)
    private String periodType; // WEEK / MONTH / ALL_TIME

    @Column(name="period_key", nullable=false, length=16)
    private String periodKey; // 2026-W05 / 2026-01 / ALL

    @Column(name="score", nullable=false)
    private Integer score = 0;

    @UpdateTimestamp
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;

    // getters/setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
