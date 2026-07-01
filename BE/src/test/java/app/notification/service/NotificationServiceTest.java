package app.notification.service;

import app.auth.model.User;
import app.auth.repository.UserRepository;
import app.notification.model.Notification;
import app.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getMyNotifications_shouldReturnNotificationsOrderedByCreatedAtDesc() {
        Long userId = 1L;
        List<Notification> expectedNotifications = List.of(
                Notification.builder().title("Notification 1").message("Message 1").build(),
                Notification.builder().title("Notification 2").message("Message 2").build()
        );

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId))
                .thenReturn(expectedNotifications);

        List<Notification> actualNotifications = notificationService.getMyNotifications(userId);

        assertEquals(expectedNotifications, actualNotifications);
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void markAsRead_shouldSetReadTrueAndSave_whenNotificationExists() {
        Long notificationId = 10L;
        Notification notification = Notification.builder()
                .title("Title")
                .message("Message")
                .build();
        notification.setRead(false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldNotSave_whenNotificationDoesNotExist() {
        Long notificationId = 999L;

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        notificationService.markAsRead(notificationId);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_shouldCallRepositoryMarkAllAsRead() {
        Long userId = 1L;

        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsRead(userId);
    }

    @Test
    void deleteNotification_shouldDeleteNotification_whenNotificationBelongsToUser() {
        Long notificationId = 10L;
        Long userId = 1L;
        User recipient = mock(User.class);
        when(recipient.getId()).thenReturn(userId);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title("Title")
                .message("Message")
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(notificationId, userId);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_shouldThrowException_whenNotificationDoesNotExist() {
        Long notificationId = 999L;
        Long userId = 1L;

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.deleteNotification(notificationId, userId));

        assertEquals("Notification not found", exception.getMessage());
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void deleteNotification_shouldThrowException_whenNotificationBelongsToAnotherUser() {
        Long notificationId = 10L;
        Long currentUserId = 1L;
        Long ownerUserId = 2L;

        User recipient = mock(User.class);
        when(recipient.getId()).thenReturn(ownerUserId);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title("Title")
                .message("Message")
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.deleteNotification(notificationId, currentUserId));

        assertEquals("Bạn không có quyền xóa thông báo này", exception.getMessage());
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void sendNotification_shouldSaveNotificationAndSendWebSocketMessage_whenUserExists() {
        Long recipientId = 1L;
        String title = "New notification";
        String message = "You have a new message";
        String link = "/notifications/1";

        User user = mock(User.class);
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.sendNotification(recipientId, title, message, link);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(user, savedNotification.getRecipient());
        assertEquals(title, savedNotification.getTitle());
        assertEquals(message, savedNotification.getMessage());
        assertEquals(link, savedNotification.getLink());
        assertEquals("INFO", savedNotification.getType());
        assertFalse(savedNotification.isRead());
        assertNotNull(savedNotification.getCreatedAt());

        verify(messagingTemplate).convertAndSendToUser(
                eq(String.valueOf(recipientId)),
                eq("/queue/notifications"),
                same(savedNotification)
        );
    }

    @Test
    void sendNotification_shouldThrowExceptionAndNotSave_whenUserDoesNotExist() {
        Long recipientId = 999L;

        when(userRepository.findById(recipientId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.sendNotification(
                        recipientId,
                        "Title",
                        "Message",
                        "/link"
                ));

        assertEquals("User not found", exception.getMessage());
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}