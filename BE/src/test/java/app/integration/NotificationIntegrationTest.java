package app.integration;

import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.notification.model.Notification;
import app.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feature7-notification;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.cache.type=simple",
        "jwt.secret=26ee5428aaf636fc2c48b269aca43a8913d7b1e4dd08eb4c6ae6b943c4e826f708a9a488feb75fb34e80928bfa0b058ff06282c5829d4fb5777e55a2281f2e80",
        "jwt.access-token-expiration=1800000",
        "jwt.refresh-token-expiration=36000000",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret",
        "gemini.api.keys=test-key",
        "spring.mail.host=localhost",
        "spring.mail.port=2525"
})
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @AfterEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void notificationEndpoints_shouldReadUpdateAndDeletePersistedNotifications() throws Exception {
        User candidate = saveUser("feature7-candidate-notification@example.com", "Feature7 Candidate",
                UserRole.CANDIDATE);
        User otherUser = saveUser("feature7-other-notification@example.com", "Feature7 Other",
                UserRole.CANDIDATE);

        Notification first = notificationRepository.save(Notification.builder()
                .recipient(candidate)
                .title("Interview reminder")
                .message("Your interview starts soon")
                .link("/candidate/interview")
                .type("INFO")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build());
        Notification second = notificationRepository.save(Notification.builder()
                .recipient(candidate)
                .title("Application update")
                .message("A recruiter viewed your application")
                .link("/candidate/my-applications")
                .type("INFO")
                .createdAt(LocalDateTime.now())
                .build());
        notificationRepository.save(Notification.builder()
                .recipient(otherUser)
                .title("Other user notification")
                .message("Should not be returned")
                .type("INFO")
                .build());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[0].title").value("Application update"))
                .andExpect(jsonPath("$[1].id").value(first.getId()));

        mockMvc.perform(put("/api/notifications/{id}/read", first.getId())
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isOk());

        assertThat(notificationRepository.findById(first.getId())).get().extracting(Notification::isRead).isEqualTo(true);
        assertThat(notificationRepository.findById(second.getId())).get().extracting(Notification::isRead).isEqualTo(false);

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isOk());

        assertThat(notificationRepository.countByRecipientIdAndIsReadFalse(candidate.getId())).isZero();

        mockMvc.perform(delete("/api/notifications/{id}", second.getId())
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isOk());

        assertThat(notificationRepository.findById(second.getId())).isEmpty();
    }

    private User saveUser(String email, String fullName, UserRole role) {
        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password("{noop}password")
                .userRole(role)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .profileImageUrl("https://example.com/" + email + ".png")
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user.getEmail());
    }
}
