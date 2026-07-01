package app.payment.controller;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private PaymentController paymentController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void upgradeToVip_shouldReturnBadRequest_whenUserIsAdmin() {
        String email = "admin@gmail.com";
        setAuthentication(email);

        User user = mock(User.class);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserRole()).thenReturn(UserRole.ADMIN);

        ResponseEntity<?> response = paymentController.upgradeToVip();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Admin không cần mua VIP.", response.getBody());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    @Test
    void upgradeToVip_shouldUpgradeCandidateToCandidateVipAndReturnNewTokens() {
        String email = "candidate@gmail.com";
        setAuthentication(email);

        User user = mock(User.class);
        stubUserForSuccessResponse(user, email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserRole()).thenReturn(UserRole.CANDIDATE);
        when(jwtTokenProvider.generateAccessToken(email)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        LocalDateTime beforeCall = LocalDateTime.now().plusDays(30).minusSeconds(2);

        ResponseEntity<?> response = paymentController.upgradeToVip();

        LocalDateTime afterCall = LocalDateTime.now().plusDays(30).plusSeconds(2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(user).setUserRole(UserRole.CANDIDATE_VIP);

        ArgumentCaptor<LocalDateTime> expirationCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(user).setVipExpirationDate(expirationCaptor.capture());

        LocalDateTime actualExpiration = expirationCaptor.getValue();
        assertFalse(actualExpiration.isBefore(beforeCall));
        assertFalse(actualExpiration.isAfter(afterCall));

        verify(userRepository).save(user);
        verify(jwtTokenProvider).generateAccessToken(email);
        verify(jwtTokenProvider).generateRefreshToken(email);
    }

    @Test
    void upgradeToVip_shouldUpgradeRecruiterToRecruiterVipAndReturnNewTokens() {
        String email = "recruiter@gmail.com";
        setAuthentication(email);

        User user = mock(User.class);
        stubUserForSuccessResponse(user, email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserRole()).thenReturn(UserRole.RECRUITER);
        when(jwtTokenProvider.generateAccessToken(email)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        LocalDateTime beforeCall = LocalDateTime.now().plusDays(30).minusSeconds(2);

        ResponseEntity<?> response = paymentController.upgradeToVip();

        LocalDateTime afterCall = LocalDateTime.now().plusDays(30).plusSeconds(2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(user).setUserRole(UserRole.RECRUITER_VIP);

        ArgumentCaptor<LocalDateTime> expirationCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(user).setVipExpirationDate(expirationCaptor.capture());

        LocalDateTime actualExpiration = expirationCaptor.getValue();
        assertFalse(actualExpiration.isBefore(beforeCall));
        assertFalse(actualExpiration.isAfter(afterCall));

        verify(userRepository).save(user);
        verify(jwtTokenProvider).generateAccessToken(email);
        verify(jwtTokenProvider).generateRefreshToken(email);
    }

    @Test
    void upgradeToVip_shouldExtendCandidateVipExpirationDate_whenCurrentVipIsStillActive() {
        String email = "candidate.vip@gmail.com";
        setAuthentication(email);

        LocalDateTime currentExpirationDate = LocalDateTime.now().plusDays(10);

        User user = mock(User.class);
        stubUserForSuccessResponse(user, email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserRole()).thenReturn(UserRole.CANDIDATE_VIP);
        when(user.getVipExpirationDate()).thenReturn(currentExpirationDate);
        when(jwtTokenProvider.generateAccessToken(email)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        ResponseEntity<?> response = paymentController.upgradeToVip();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(user).setUserRole(UserRole.CANDIDATE_VIP);
        verify(user).setVipExpirationDate(currentExpirationDate.plusDays(30));

        verify(userRepository).save(user);
        verify(jwtTokenProvider).generateAccessToken(email);
        verify(jwtTokenProvider).generateRefreshToken(email);
    }

    @Test
    void upgradeToVip_shouldExtendRecruiterVipExpirationDate_whenCurrentVipIsStillActive() {
        String email = "recruiter.vip@gmail.com";
        setAuthentication(email);

        LocalDateTime currentExpirationDate = LocalDateTime.now().plusDays(15);

        User user = mock(User.class);
        stubUserForSuccessResponse(user, email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserRole()).thenReturn(UserRole.RECRUITER_VIP);
        when(user.getVipExpirationDate()).thenReturn(currentExpirationDate);
        when(jwtTokenProvider.generateAccessToken(email)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        ResponseEntity<?> response = paymentController.upgradeToVip();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(user).setUserRole(UserRole.RECRUITER_VIP);
        verify(user).setVipExpirationDate(currentExpirationDate.plusDays(30));

        verify(userRepository).save(user);
        verify(jwtTokenProvider).generateAccessToken(email);
        verify(jwtTokenProvider).generateRefreshToken(email);
    }

    @Test
    void upgradeToVip_shouldRenewVipFromNow_whenCandidateVipIsExpired() {
        String email = "expired.vip@gmail.com";
        setAuthentication(email);

        LocalDateTime expiredDate = LocalDateTime.now().minusDays(5);

        User user = mock(User.class);
        stubUserForSuccessResponse(user, email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.getUserRole()).thenReturn(UserRole.CANDIDATE_VIP);
        when(user.getVipExpirationDate()).thenReturn(expiredDate);
        when(jwtTokenProvider.generateAccessToken(email)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(email)).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600000L);

        LocalDateTime beforeCall = LocalDateTime.now().plusDays(30).minusSeconds(2);

        ResponseEntity<?> response = paymentController.upgradeToVip();

        LocalDateTime afterCall = LocalDateTime.now().plusDays(30).plusSeconds(2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(user).setUserRole(UserRole.CANDIDATE_VIP);

        ArgumentCaptor<LocalDateTime> expirationCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(user).setVipExpirationDate(expirationCaptor.capture());

        LocalDateTime actualExpiration = expirationCaptor.getValue();
        assertFalse(actualExpiration.isBefore(beforeCall));
        assertFalse(actualExpiration.isAfter(afterCall));

        verify(userRepository).save(user);
        verify(jwtTokenProvider).generateAccessToken(email);
        verify(jwtTokenProvider).generateRefreshToken(email);
    }

    @Test
    void upgradeToVip_shouldThrowRuntimeException_whenUserNotFound() {
        String email = "missing@gmail.com";
        setAuthentication(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentController.upgradeToVip());

        assertEquals("User not found", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    private void setAuthentication(String email) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void stubUserForSuccessResponse(User user, String email) {
        when(user.getId()).thenReturn(1L);
        when(user.getFullName()).thenReturn("Test User");
        when(user.getEmail()).thenReturn(email);
        when(user.getProfileImageUrl()).thenReturn("avatar.png");
        when(user.getIsEmailVerified()).thenReturn(true);
        when(user.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(10));
        when(user.getLastLoginAt()).thenReturn(LocalDateTime.now().minusDays(1));
    }
}