package app.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import app.auth.exception.InvalidTokenException;
import app.auth.model.RefreshToken;
import app.auth.model.User;
import app.auth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 3_600_000L);
    }

    @Test
    void createRefreshToken_shouldSaveTokenWithUserAndFutureExpiry() {
        User user = User.builder().id(1L).email("candidate@test.com").build();
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertSame(user, result.getUser());
        assertFalse(result.getToken().isBlank());
        assertTrue(result.getExpiryDate().isAfter(before));
        assertFalse(result.getExpiryDate().isAfter(Instant.now().plusMillis(3_600_000L)));
        verify(refreshTokenRepository).save(result);
    }

    @Test
    void verifyExpiration_shouldReturnSameToken_whenTokenIsValid() {
        RefreshToken token = RefreshToken.builder()
                .token("valid-token").expiryDate(Instant.now().plusSeconds(60)).build();

        assertSame(token, refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_shouldDeleteAndThrow_whenTokenIsExpired() {
        RefreshToken token = RefreshToken.builder()
                .token("expired-token").expiryDate(Instant.now().minusSeconds(1)).build();

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> refreshTokenService.verifyExpiration(token));

        assertTrue(exception.getMessage().contains("hết hạn"));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void findByToken_shouldReturnToken_whenTokenExists() {
        RefreshToken token = RefreshToken.builder().token("stored-token").build();
        when(refreshTokenRepository.findByToken("stored-token")).thenReturn(Optional.of(token));

        assertSame(token, refreshTokenService.findByToken("stored-token"));
    }

    @Test
    void findByToken_shouldThrow_whenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> refreshTokenService.findByToken("missing-token"));

        assertEquals("Refresh token không hợp lệ", exception.getMessage());
    }

    @Test
    void deleteByUser_shouldDelegateToRepository() {
        User user = User.builder().id(2L).email("user@test.com").build();

        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void deleteExpiredTokens_shouldUseCurrentTime() {
        Instant before = Instant.now();

        refreshTokenService.deleteExpiredTokens();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteByExpiryDateBefore(captor.capture());
        assertFalse(captor.getValue().isBefore(before));
        assertFalse(captor.getValue().isAfter(Instant.now()));
    }
}
