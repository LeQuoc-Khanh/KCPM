package app.gamification.repository;

import app.gamification.model.LeaderboardScore;
import app.gamification.dto.response.LeaderboardEntryResponse;
import app.gamification.dto.response.LeaderboardMeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LeaderboardScoreRepository extends JpaRepository<LeaderboardScore, Long> {

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO leaderboard_scores (user_id, role, period_type, period_key, score, updated_at)
        VALUES (:userId, :role, :periodType, :periodKey, :points, NOW())
        ON CONFLICT (user_id, role, period_type, period_key)
        DO UPDATE SET
            score = leaderboard_scores.score + EXCLUDED.score,
            updated_at = NOW();
    """, nativeQuery = true)
    void upsertScore(@Param("userId") Long userId,
                     @Param("role") String role,
                     @Param("periodType") String periodType,
                     @Param("periodKey") String periodKey,
                     @Param("points") int points);

    // --- CẬP NHẬT QUERY: Thêm u.profile_image_url as avatarUrl ---
    @Query(value = """
        SELECT
            ls.user_id as userId,
            u.full_name as fullName,
            u.profile_image_url as avatarUrl, 
            ls.score as score,
            CAST(RANK() OVER (ORDER BY ls.score DESC, ls.updated_at ASC) AS INTEGER) as rank
        FROM leaderboard_scores ls
        JOIN users u ON u.id = ls.user_id
        WHERE ls.role = :role
          AND ls.period_type = :periodType
          AND ls.period_key = :periodKey
        ORDER BY ls.score DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<LeaderboardEntryResponse> findTopRankings(@Param("role") String role,
                                                   @Param("periodType") String periodType,
                                                   @Param("periodKey") String periodKey,
                                                   @Param("limit") int limit);

    // --- CẬP NHẬT QUERY: Thêm u.profile_image_url as avatarUrl ---
    @Query(value = """
        WITH RankedScores AS (
            SELECT
                ls.user_id,
                ls.score,
                RANK() OVER (ORDER BY ls.score DESC, ls.updated_at ASC) as rk
            FROM leaderboard_scores ls
            WHERE ls.role = :role
              AND ls.period_type = :periodType
              AND ls.period_key = :periodKey
        )
        SELECT
            rs.user_id as userId,
            u.full_name as fullName,
            u.profile_image_url as avatarUrl,
            rs.score as score,
            CAST(rs.rk AS INTEGER) as rank
        FROM RankedScores rs
        JOIN users u ON u.id = rs.user_id
        WHERE rs.user_id = :userId
    """, nativeQuery = true)
    LeaderboardMeResponse findMyRank(@Param("userId") Long userId,
                                     @Param("role") String role,
                                     @Param("periodType") String periodType,
                                     @Param("periodKey") String periodKey);
}