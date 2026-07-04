package app.notification.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationTest {

    @Test
    void builder_shouldSetDefaultIsReadFalse() {
        Notification notification = Notification.builder()
                .title("Title")
                .message("Message")
                .build();

        assertFalse(notification.isRead());
    }

    @Test
    void builder_shouldSetCreatedAtAutomatically() {
        Notification notification = Notification.builder()
                .title("Title")
                .message("Message")
                .build();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void onCreate_shouldSetCreatedAt_whenCreatedAtIsNull() {
        Notification notification = new Notification();
        notification.setCreatedAt(null);

        notification.onCreate();

        assertNotNull(notification.getCreatedAt());
    }
}