package app.gamification.dto.response;

import java.time.Instant;

public interface LeaderboardLogResponse {
  Long getUserId();
  String getFullName();
  String getRole();
  String getActionType();
  Integer getPoints();
  Long getRefId();
  Instant getCreatedAt();
  String getAvatarUrl();
}
