package app.gamification.dto.response;

import java.time.OffsetDateTime;

public interface LeaderboardLogResponse {
  Long getUserId();
  String getFullName();
  String getRole();
  String getActionType();
  Integer getPoints();
  Long getRefId();
  OffsetDateTime getCreatedAt();
  String getAvatarUrl();
}
