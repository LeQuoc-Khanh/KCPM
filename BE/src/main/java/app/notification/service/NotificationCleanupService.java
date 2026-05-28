package app.notification.service;

import app.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    /**
     * Chạy định kỳ vào lúc 3:00 AM mỗi ngày
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Bắt đầu dọn dẹp thông báo cũ...");

        // Tính mốc thời gian: Xóa thông báo cũ hơn 30 ngày
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        try {
            notificationRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("Đã xóa các thông báo được tạo trước: {}", cutoffDate);
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp thông báo: {}", e.getMessage());
        }
    }
}