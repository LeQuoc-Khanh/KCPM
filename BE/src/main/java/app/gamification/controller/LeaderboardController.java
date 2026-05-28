package app.gamification.controller;

import app.gamification.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<?> getTop(
        @RequestParam(defaultValue = "CANDIDATE") String role,
        @RequestParam(defaultValue = "WEEK") String period,
        @RequestParam(required = false) String periodKey,
        @RequestParam(defaultValue = "50") int limit
    ) {
        var data = leaderboardService.getTopUsers(role, period, periodKey, limit);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", data
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "CANDIDATE") String role,
        @RequestParam(defaultValue = "WEEK") String period,
        @RequestParam(required = false) String periodKey
    ) {
        var data = leaderboardService.getMyRank(userId, role, period, periodKey);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", data != null ? data : Map.of() // Trả về object rỗng nếu chưa có rank thay vì null crash FE
        ));
    }

    @GetMapping("/missions")
    public ResponseEntity<?> getMissions(
        @RequestParam(defaultValue = "CANDIDATE") String role,
        @RequestParam(required = false) Long userId // <--- Thêm tham số này (không bắt buộc để tránh lỗi nếu chưa login)
    ) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", leaderboardService.getMissions(role, userId) // Truyền userId vào service
        ));
    }
    
    // Admin log API (Optional)
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(@RequestParam(defaultValue = "10") int limit) {
         return ResponseEntity.ok(Map.of(
            "success", true,
            "data", leaderboardService.getSystemLogs(limit)
        ));
    }
}