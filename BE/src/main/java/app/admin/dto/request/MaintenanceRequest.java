package app.admin.dto.request;

public class MaintenanceRequest {
    private boolean enabled;
    private String message;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
