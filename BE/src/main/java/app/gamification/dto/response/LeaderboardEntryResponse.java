package app.gamification.dto.response;

public interface LeaderboardEntryResponse {
    Long getUserId();
    String getFullName();
    Integer getScore();
    Integer getRank();
    String getAvatarUrl();
}
