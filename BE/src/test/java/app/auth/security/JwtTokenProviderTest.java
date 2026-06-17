package app.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();

        ReflectionTestUtils.setField(
                jwtTokenProvider,
                "jwtSecret",
                "1234567890123456789012345678901212345678901234567890123456789012"
        );
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 1800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 36000000L);
    }

    @Test
    void generateAccessToken_shouldReturnValidToken() {
        String token = jwtTokenProvider.generateAccessToken("candidate@test.com");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("candidate@test.com", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() {
        boolean result = jwtTokenProvider.validateToken("invalid.token.value");

        assertFalse(result);
    }
}