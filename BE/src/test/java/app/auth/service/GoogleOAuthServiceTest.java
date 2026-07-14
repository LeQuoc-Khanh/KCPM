package app.auth.service;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import app.auth.exception.InvalidTokenException;

class GoogleOAuthServiceTest {
    private GoogleIdTokenVerifier verifier;
    private GoogleOAuthService googleOAuthService;

    @BeforeEach
    void setUp() {
        verifier = mock(GoogleIdTokenVerifier.class);
        googleOAuthService = new GoogleOAuthService() {
            @Override
            GoogleIdTokenVerifier createVerifier() {
                return verifier;
            }
        };
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-google-id-token", "a.b.c"})
    void verifyGoogleToken_shouldThrow_whenTokenIsInvalid(String token) throws Exception {
        when(verifier.verify(token)).thenReturn(null);

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> googleOAuthService.verifyGoogleToken(token));

        assertTrue(exception.getMessage().contains("Google token không hợp lệ"));
    }

    @Test
    void verifyGoogleToken_shouldReturnUserInfo_whenTokenIsValid() throws Exception {
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("google-123");
        payload.setEmail("candidate@test.com");
        payload.setEmailVerified(true);
        payload.set("name", "Candidate Test");
        payload.set("picture", "https://example.com/avatar.png");

        when(verifier.verify("valid-token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);

        Map<String, String> result = googleOAuthService.verifyGoogleToken("valid-token");

        assertEquals("google-123", result.get("googleId"));
        assertEquals("candidate@test.com", result.get("email"));
        assertEquals("Candidate Test", result.get("name"));
        assertEquals("https://example.com/avatar.png", result.get("pictureUrl"));
        assertEquals("true", result.get("emailVerified"));
    }

    @Test
    void verifyGoogleToken_shouldThrow_whenVerifierReturnsNull() throws Exception {
        when(verifier.verify("unknown-token")).thenReturn(null);

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> googleOAuthService.verifyGoogleToken("unknown-token"));

        assertTrue(exception.getMessage().contains("Google token không hợp lệ"));
    }

    @Test
    void verifyGoogleToken_shouldWrapException_whenVerificationFails() throws Exception {
        when(verifier.verify("unavailable-token"))
                .thenThrow(new IOException("Google verification unavailable"));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> googleOAuthService.verifyGoogleToken("unavailable-token"));

        assertTrue(exception.getMessage().contains("Google verification unavailable"));
    }
}
