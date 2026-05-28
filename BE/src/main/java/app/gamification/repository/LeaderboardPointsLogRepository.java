package app.gamification.repository;

import app.gamification.dto.response.LeaderboardLogResponse;
import app.gamification.model.LeaderboardPointsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface LeaderboardPointsLogRepository extends JpaRepository<LeaderboardPointsLog, Long> {

    @Query("SELECT COUNT(l) FROM LeaderboardPointsLog l WHERE l.userId = :userId AND l.actionType = :actionType AND l.createdAt >= :startOfDay")
    long countActionsToday(@Param("userId") Long userId,
                           @Param("actionType") String actionType,
                           @Param("startOfDay") OffsetDateTime startOfDay);

    boolean existsByUserIdAndActionTypeAndRefId(Long userId, String actionType, Long refId);

    // --- CẬP NHẬT QUERY: Thêm u.profile_image_url as avatarUrl ---
    @Query(value = """
        SELECT
            l.user_id as userId,
            u.full_name as fullName,
            u.profile_image_url as avatarUrl,
            l.role as role,
            l.action_type as actionType,
            l.points as points,
            l.ref_id as refId,
            l.created_at as createdAt
        FROM leaderboard_points_log l
        JOIN users u ON u.id = l.user_id
        ORDER BY l.created_at DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<LeaderboardLogResponse> findRecentLogs(@Param("limit") int limit);
}