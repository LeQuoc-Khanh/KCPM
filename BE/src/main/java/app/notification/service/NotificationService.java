package app.notification.service;

import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.notification.model.Notification;
import app.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;


    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Kiểm tra xem thông báo này có phải của user đang đăng nhập không
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa thông báo này");
        }

        notificationRepository.delete(notification);
    }

    // Nếu gửi thông báo lỗi, transaction cha (Apply) vẫn thành công
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(Long recipientId, String title, String message, String link) {
        User user = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Lưu vào Database
        Notification notification = Notification.builder()
                .recipient(user)
                .title(title)
                .message(message)
                .link(link)
                .type("INFO")
                .build();
        Notification savedNotif = notificationRepository.save(notification);

        // 2. Gửi Real-time qua WebSocket
        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipientId),
                "/queue/notifications",
                savedNotif
        );
    }

    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        Notification notif = notificationRepository.findById(notificationId).orElse(null);
        if (notif != null) {
            notif.setRead(true);
            notificationRepository.save(notif);
        }
    }
}