package app.admin.service;

import app.admin.model.SystemSetting;
import app.admin.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SystemSettingService {
    public static final String KEY_ENABLED = "MAINTENANCE_ENABLED";
    public static final String KEY_MESSAGE = "MAINTENANCE_MESSAGE";

    private final SystemSettingRepository repo;

    public SystemSettingService(SystemSettingRepository repo) {
        this.repo = repo;
    }

    public boolean isMaintenanceEnabled() {
        return Boolean.parseBoolean(get(KEY_ENABLED, "false"));
    }

    public String maintenanceMessage() {
        return get(KEY_MESSAGE, "Hệ thống đang bảo trì, vui lòng thử lại sau.");
    }

    public String get(String key, String defaultValue) {
        return repo.findById(key).map(SystemSetting::getValue).orElse(defaultValue);
    }

    @Transactional
    public void set(String key, String value) {
        SystemSetting s = repo.findById(key).orElseGet(SystemSetting::new);
        s.setKey(key);
        s.setValue(value);
        s.setUpdatedAt(OffsetDateTime.now());
        repo.save(s);
    }

    @Transactional
    public void setMaintenance(boolean enabled, String message) {
        set(KEY_ENABLED, String.valueOf(enabled));
        if (message != null) set(KEY_MESSAGE, message);
    }
}
