package app.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void sendVerificationEmail_shouldCreateAndSendMessage_whenInputIsValid() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendVerificationEmail("candidate@test.com", "123456"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendVerificationEmail_shouldSwallowException_whenMailCreationFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail unavailable"));

        assertDoesNotThrow(() ->
                emailService.sendVerificationEmail("candidate@test.com", "123456"));

        verify(mailSender, never()).send(mimeMessage);
    }

    @Test
    void sendResetPasswordEmail_shouldCreateAndSendMessage_whenInputIsValid() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendResetPasswordEmail("candidate@test.com", "reset-token"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendResetPasswordEmail_shouldSwallowException_whenMailCreationFails() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail unavailable"));

        assertDoesNotThrow(() ->
                emailService.sendResetPasswordEmail("candidate@test.com", "reset-token"));

        verify(mailSender, never()).send(mimeMessage);
    }
}
