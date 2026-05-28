package app.gamification.event;

import app.gamification.model.UserPointAction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PointEvent extends ApplicationEvent {
    private final Long userId;
    private final String roleGroup; // CANDIDATE or RECRUITER
    private final UserPointAction action;
    private final Long refId; // ID tham chiếu (ví dụ jobId, applicationId)

    public PointEvent(Object source, Long userId, String roleGroup, UserPointAction action, Long refId) {
        super(source);
        this.userId = userId;
        this.roleGroup = roleGroup;
        this.action = action;
        this.refId = refId;
    }
}