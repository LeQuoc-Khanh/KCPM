package app.notification.service;

import app.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCleanupService notificationCleanupService;

    @Test
    void cleanupOldNotifications_shouldDeleteNotificationsOlderThan30Days() {
        LocalDateTime beforeCall = LocalDateTime.now().minusDays(30).minusSeconds(2);

        notificationCleanupService.cleanupOldNotifications();

        ArgumentCaptor<LocalDateTime> cutoffDateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteByCreatedAtBefore(cutoffDateCaptor.capture());

        LocalDateTime afterCall = LocalDateTime.now().minusDays(30).plusSeconds(2);
        LocalDateTime actualCutoffDate = cutoffDateCaptor.getValue();

        assertFalse(actualCutoffDate.isBefore(beforeCall));
        assertFalse(actualCutoffDate.isAfter(afterCall));
    }

    @Test
    void cleanupOldNotifications_shouldNotThrowException_whenRepositoryFails() {
        doThrow(new RuntimeException("Database error"))
                .when(notificationRepository)
                .deleteByCreatedAtBefore(any(LocalDateTime.class));

        assertDoesNotThrow(() -> notificationCleanupService.cleanupOldNotifications());
    }
}