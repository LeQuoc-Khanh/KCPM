package app.admin.controller;

import app.admin.dto.request.MaintenanceRequest;
import app.admin.service.SystemSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

    private final SystemSettingService settingService;

    public AdminSettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/maintenance")
    public ResponseEntity<?> getMaintenance() {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", settingService.isMaintenanceEnabled());
        data.put("message", settingService.maintenanceMessage());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OK",
                "data", data
        ));
    }

    @PutMapping("/maintenance")
    public ResponseEntity<?> updateMaintenance(@RequestBody MaintenanceRequest req) {
        settingService.setMaintenance(req.isEnabled(), req.getMessage());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật chế độ bảo trì thành công"
        ));
    }
}
