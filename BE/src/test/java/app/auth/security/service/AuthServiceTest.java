package app.auth.service;

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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import app.admin.service.SystemSettingService;
import app.auth.dto.request.ForgotPasswordRequest;
import app.auth.dto.request.LoginRequest;
import app.auth.dto.request.RefreshTokenRequest;
import app.auth.dto.request.RegisterRequest;
import app.auth.exception.EmailAlreadyExistsException;
import app.auth.exception.InvalidTokenException;
import app.auth.model.PasswordResetToken;
import app.auth.model.RefreshToken;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.CompanyRepository;
import app.auth.repository.PasswordResetTokenRepository;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.service.CloudinaryService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @Test
    void register_shouldCreateUser_whenEmailNotExists() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(userRepository.existsByEmail("candidate@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        when(jwtTokenProvider.generateAccessToken("candidate@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        var response = authService.register(candidateRegisterRequest, null);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(UserRole.CANDIDATE, response.getUser().getUserRole());
        assertEquals(UserStatus.PENDING_VERIFICATION, response.getUser().getStatus());

        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("candidate@test.com"), anyString());
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("candidate@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(candidateRegisterRequest, null);
        });

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void verifyEmail_shouldActivateUser_whenCodeIsCorrect() {
        User user = User.builder()
                .email("candidate@test.com")
                .verificationCode("ABC123")
                .status(UserStatus.PENDING_VERIFICATION)
                .isEmailVerified(false)
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        authService.verifyEmail("candidate@test.com", "ABC123");

        assertTrue(user.getIsEmailVerified());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getVerificationCode());

        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_shouldThrowException_whenCodeIsWrong() {
        User user = User.builder()
                .email("candidate@test.com")
                .verificationCode("ABC123")
                .status(UserStatus.PENDING_VERIFICATION)
                .isEmailVerified(false)
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidTokenException.class, () -> {
            authService.verifyEmail("candidate@test.com", "WRONG");
        });

        verify(userRepository, never()).save(user);
    }

    @Test
    void logout_shouldDeleteRefreshTokenByUser() {
        User user = User.builder()
                .email("candidate@test.com")
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        authService.logout("candidate@test.com");

        verify(refreshTokenService).deleteByUser(user);
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("candidate@test.com");
        request.setPassword("Password1");

        Authentication authentication = mock(Authentication.class);

        User user = User.builder()
                .id(1L)
                .fullName("Test Candidate")
                .email("candidate@test.com")
                .password("encoded-password")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(user);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(systemSettingService.isMaintenanceEnabled()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        var response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(UserRole.CANDIDATE, response.getUser().getUserRole());

        verify(authenticationManager).authenticate(any());
        verify(userRepository).save(user);
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void login_shouldThrowException_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("candidate@test.com");
        request.setPassword("WrongPassword1");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            authService.login(request);
        });

        verify(userRepository, never()).findByEmail(anyString());
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    void refreshToken_shouldReturnNewAccessToken_whenRefreshTokenIsValid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-refresh-token");

        User user = User.builder()
                .id(1L)
                .fullName("Test Candidate")
                .email("candidate@test.com")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("old-refresh-token");
        refreshToken.setUser(user);

        when(refreshTokenService.findByToken("old-refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtTokenProvider.generateAccessToken("candidate@test.com")).thenReturn("new-access-token");

        var response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("old-refresh-token", response.getRefreshToken());

        verify(refreshTokenService).findByToken("old-refresh-token");
        verify(refreshTokenService).verifyExpiration(refreshToken);
    }

    @Test
    void forgotPassword_shouldCreateResetTokenAndSendEmail_whenEmailExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("candidate@test.com");

        User user = User.builder()
                .id(1L)
                .fullName("Test Candidate")
                .email("candidate@test.com")
                .userRole(UserRole.CANDIDATE)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendResetPasswordEmail(eq("candidate@test.com"), anyString());
    }
}