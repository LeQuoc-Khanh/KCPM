package app.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import app.auth.exception.InvalidTokenException;

class GoogleOAuthServiceTest {
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-google-id-token", "a.b.c"})
    void verifyGoogleToken_shouldThrow_whenTokenIsInvalid(String token) {
        ReflectionTestUtils.setField(googleOAuthService, "googleClientId", "test-client-id");

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> googleOAuthService.verifyGoogleToken(token));

        assertTrue(exception.getMessage().startsWith("Xác thực Google thất bại:"));
    }
}
