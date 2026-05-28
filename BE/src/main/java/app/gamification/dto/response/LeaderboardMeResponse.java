package app.gamification.dto.response;

public interface LeaderboardMeResponse {
    Long getUserId();
    String getFullName();
    Integer getScore();
    Integer getRank();
    String getAvatarUrl();
}
