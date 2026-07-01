package app.notification.controller;

import app.notification.model.Notification;
import app.notification.service.NotificationService;
import app.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void getMyNotifications_shouldReturnOkAndNotificationsOfCurrentUser() {
        Long currentUserId = 1L;
        List<Notification> notifications = List.of(
                Notification.builder().title("Title").message("Message").build()
        );

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(notificationService.getMyNotifications(currentUserId)).thenReturn(notifications);

        ResponseEntity<List<Notification>> response = notificationController.getMyNotifications();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(notifications, response.getBody());
        verify(securityUtils).getCurrentUserId();
        verify(notificationService).getMyNotifications(currentUserId);
    }

    @Test
    void markAsRead_shouldReturnOkAndCallService() {
        Long notificationId = 10L;

        ResponseEntity<Void> response = notificationController.markAsRead(notificationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void markAllAsRead_shouldReturnOkAndCallServiceWithCurrentUserId() {
        Long currentUserId = 1L;
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

        ResponseEntity<Void> response = notificationController.markAllAsRead();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(securityUtils).getCurrentUserId();
        verify(notificationService).markAllAsRead(currentUserId);
    }

    @Test
    void deleteNotification_shouldReturnOkAndCallServiceWithCurrentUserId() {
        Long notificationId = 10L;
        Long currentUserId = 1L;
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

        ResponseEntity<Void> response = notificationController.deleteNotification(notificationId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(securityUtils).getCurrentUserId();
        verify(notificationService).deleteNotification(notificationId, currentUserId);
    }
}