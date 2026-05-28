package app.notification.controller;

import app.notification.model.Notification;
import app.notification.service.NotificationService;
import app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils; // Class tiện ích lấy User ID hiện tại

    /**
     * API lấy toàn bộ thông báo của người dùng đang đăng nhập
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        // Lấy ID user từ token đang đăng nhập
        Long currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUserId));
    }

    /**
     * API đánh dấu một thông báo là đã đọc
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long currentUserId = securityUtils.getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        notificationService.deleteNotification(id, currentUserId);
        return ResponseEntity.ok().build();
    }
}