package app.auth.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import app.admin.service.SystemSettingService;
import app.auth.dto.request.ForgotPasswordRequest;
import app.auth.dto.request.GoogleAuthRequest;
import app.auth.dto.request.LoginRequest;
import app.auth.dto.request.RefreshTokenRequest;
import app.auth.dto.request.RegisterRequest;
import app.auth.dto.request.ResetPasswordRequest;
import app.auth.exception.AuthException;
import app.auth.exception.EmailAlreadyExistsException;
import app.auth.exception.InvalidCredentialsException;
import app.auth.exception.InvalidTokenException;
import app.auth.exception.UnauthorizedException;
import app.auth.exception.UserNotFoundException;
import app.auth.model.PasswordResetToken;
import app.auth.model.RefreshToken;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.CompanyRepository;
import app.auth.repository.PasswordResetTokenRepository;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.content.model.Company;
import app.exception.MaintenanceModeException;
import app.service.CloudinaryService;

@ExtendWith(MockitoExtension.class)
class AuthEdgeSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest candidateRegisterRequest;

    @BeforeEach
    void setUp() {
        candidateRegisterRequest = new RegisterRequest();
        candidateRegisterRequest.setFullName("Test Candidate");
        candidateRegisterRequest.setEmail("candidate@test.com");
        candidateRegisterRequest.setPassword("Password1");
        candidateRegisterRequest.setUserRole(UserRole.CANDIDATE);
    }


    // MP-55: TC 1.21-1.41 Edge/Security cases.
    @Test
    void login_shouldThrowException_whenEmailIsNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setEmail("candidate@test.com");
        request.setPassword("Password1");

        User user = User.builder()
                .email("candidate@test.com")
                .status(UserStatus.PENDING_VERIFICATION)
                .isEmailVerified(false)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void login_shouldThrowException_whenUserIsBanned() {
        LoginRequest request = new LoginRequest();
        request.setEmail("candidate@test.com");
        request.setPassword("Password1");

        User user = User.builder()
                .email("candidate@test.com")
                .status(UserStatus.BANNED)
                .isEmailVerified(true)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void login_shouldThrowMaintenanceModeException_whenNonAdminLogsInDuringMaintenance() {
        LoginRequest request = new LoginRequest();
        request.setEmail("candidate@test.com");
        request.setPassword("Password1");

        User user = User.builder()
                .email("candidate@test.com")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(systemSettingService.isMaintenanceEnabled()).thenReturn(true);
        when(systemSettingService.maintenanceMessage()).thenReturn("maintenance");

        assertThrows(MaintenanceModeException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    void login_shouldAllowAdmin_whenMaintenanceEnabled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("Password1");

        Authentication authentication = mock(Authentication.class);
        User user = User.builder()
                .id(99L)
                .fullName("Admin")
                .email("admin@test.com")
                .userRole(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("admin-refresh-token");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(systemSettingService.isMaintenanceEnabled()).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("admin-access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        var response = authService.login(request);

        assertEquals("admin-access-token", response.getAccessToken());
        assertEquals(UserRole.ADMIN, response.getUser().getUserRole());
    }

    @Test
    void googleAuth_shouldCreateNewCandidate_whenEmailNotExists() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setGoogleToken("valid-google-token");
        request.setUserRole(UserRole.CANDIDATE);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(googleOAuthService.verifyGoogleToken("valid-google-token")).thenReturn(Map.of(
                "email", "google@test.com",
                "googleId", "google-id",
                "name", "Google User",
                "pictureUrl", "https://cdn.test/google.png"
        ));
        when(userRepository.findByEmail("google@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-google-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(systemSettingService.isMaintenanceEnabled()).thenReturn(false);
        when(jwtTokenProvider.generateAccessToken("google@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        var response = authService.googleAuth(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals(UserRole.CANDIDATE, response.getUser().getUserRole());
        assertTrue(response.getUser().getIsEmailVerified());
    }

    @Test
    void googleAuth_shouldUpdateExistingUser_whenGoogleIdIsMissing() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setGoogleToken("valid-google-token");

        User user = User.builder()
                .id(2L)
                .fullName("Google User")
                .email("google@test.com")
                .userRole(UserRole.CANDIDATE)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(googleOAuthService.verifyGoogleToken("valid-google-token")).thenReturn(Map.of(
                "email", "google@test.com",
                "googleId", "google-id",
                "name", "Google User",
                "pictureUrl", "https://cdn.test/google.png"
        ));
        when(userRepository.findByEmail("google@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(systemSettingService.isMaintenanceEnabled()).thenReturn(false);
        when(jwtTokenProvider.generateAccessToken("google@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        var response = authService.googleAuth(request);

        assertEquals("google-id", user.getGoogleId());
        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("access-token", response.getAccessToken());
    }

    @Test
    void googleAuth_shouldThrowException_whenExistingUserIsBanned() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setGoogleToken("valid-google-token");

        User user = User.builder()
                .email("google@test.com")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.BANNED)
                .isEmailVerified(true)
                .build();

        when(googleOAuthService.verifyGoogleToken("valid-google-token")).thenReturn(Map.of(
                "email", "google@test.com",
                "googleId", "google-id",
                "name", "Google User",
                "pictureUrl", "https://cdn.test/google.png"
        ));
        when(userRepository.findByEmail("google@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        assertThrows(UnauthorizedException.class, () -> {
            authService.googleAuth(request);
        });
    }

    @Test
    void googleAuth_shouldCreateDefaultCompany_whenNewRecruiterLogsIn() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setGoogleToken("valid-google-token");
        request.setUserRole(UserRole.RECRUITER);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(googleOAuthService.verifyGoogleToken("valid-google-token")).thenReturn(Map.of(
                "email", "recruiter@test.com",
                "googleId", "google-id",
                "name", "Recruiter User",
                "pictureUrl", "https://cdn.test/google.png"
        ));
        when(userRepository.findByEmail("recruiter@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-google-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });
        when(companyRepository.findByRecruiterId(3L)).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemSettingService.isMaintenanceEnabled()).thenReturn(false);
        when(jwtTokenProvider.generateAccessToken("recruiter@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        authService.googleAuth(request);

        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void refreshToken_shouldThrowException_whenTokenIsInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        when(refreshTokenService.findByToken("invalid-refresh-token"))
                .thenThrow(new InvalidTokenException("invalid"));

        assertThrows(InvalidTokenException.class, () -> {
            authService.refreshToken(request);
        });
    }

    @Test
    void refreshToken_shouldThrowException_whenTokenIsExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-refresh-token");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("expired-refresh-token");

        when(refreshTokenService.findByToken("expired-refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken))
                .thenThrow(new InvalidTokenException("expired"));

        assertThrows(InvalidTokenException.class, () -> {
            authService.refreshToken(request);
        });
    }

    @Test
    void logout_shouldThrowException_whenEmailNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.logout("missing@test.com");
        });
    }

    @Test
    void forgotPassword_shouldThrowException_whenEmailNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@test.com");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.forgotPassword(request);
        });

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("NewPassword1");

        User user = User.builder()
                .email("candidate@test.com")
                .password("old-password")
                .build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("reset-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded-new-password");

        authService.resetPassword(request);

        assertEquals("encoded-new-password", user.getPassword());
        assertTrue(resetToken.getUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenIsInvalid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("NewPassword1");

        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> {
            authService.resetPassword(request);
        });
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenWasUsed() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("NewPassword1");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("used-token")
                .user(User.builder().email("candidate@test.com").build())
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(resetToken));

        assertThrows(InvalidTokenException.class, () -> {
            authService.resetPassword(request);
        });
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenIsExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("NewPassword1");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token("expired-token")
                .user(User.builder().email("candidate@test.com").build())
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

        assertThrows(InvalidTokenException.class, () -> {
            authService.resetPassword(request);
        });
    }
}
