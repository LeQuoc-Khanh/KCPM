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
class AuthCoreFlowTest {

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


    // MP-51: TC 1.1-1.20 Core/Auth main flows.
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

    @Test
    void register_shouldThrowException_whenRoleIsAdmin() {
        candidateRegisterRequest.setUserRole(UserRole.ADMIN);
        when(userRepository.existsByEmail("candidate@test.com")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> {
            authService.register(candidateRegisterRequest, null);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldUseDefaultAvatar_whenAvatarIsNotProvided() {
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

        assertNotNull(response.getUser().getProfileImageUrl());
        verify(cloudinaryService, never()).uploadAvatar(any());
    }

    @Test
    void register_shouldUploadAvatar_whenAvatarIsProvided() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.png", "image/png", "fake-image".getBytes());

        when(userRepository.existsByEmail("candidate@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(cloudinaryService.uploadAvatar(avatar)).thenReturn("https://cdn.test/avatar.png");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken("candidate@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        var response = authService.register(candidateRegisterRequest, avatar);

        assertEquals("https://cdn.test/avatar.png", response.getUser().getProfileImageUrl());
        verify(cloudinaryService).uploadAvatar(avatar);
    }

    @Test
    void register_shouldUseDefaultAvatar_whenAvatarUploadFails() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.png", "image/png", "fake-image".getBytes());

        when(userRepository.existsByEmail("candidate@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(cloudinaryService.uploadAvatar(avatar)).thenThrow(new RuntimeException("upload failed"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken("candidate@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        var response = authService.register(candidateRegisterRequest, avatar);

        assertNotNull(response.getUser().getProfileImageUrl());
        verify(cloudinaryService).uploadAvatar(avatar);
    }

    @Test
    void register_shouldCreateDefaultCompany_whenRecruiterRegisters() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        candidateRegisterRequest.setUserRole(UserRole.RECRUITER);

        when(userRepository.existsByEmail("candidate@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(companyRepository.findByRecruiterId(10L)).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken("candidate@test.com")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        authService.register(candidateRegisterRequest, null);

        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void verifyEmail_shouldThrowException_whenEmailNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.verifyEmail("missing@test.com", "ABC123");
        });
    }

    @Test
    void verifyEmail_shouldThrowException_whenAccountAlreadyVerified() {
        User user = User.builder()
                .email("candidate@test.com")
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .verificationCode(null)
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        assertThrows(AuthException.class, () -> {
            authService.verifyEmail("candidate@test.com", "ABC123");
        });

        verify(userRepository, never()).save(user);
    }

    @Test
    void resendVerification_shouldSendNewCode_whenUserIsPending() {
        User user = User.builder()
                .email("candidate@test.com")
                .status(UserStatus.PENDING_VERIFICATION)
                .isEmailVerified(false)
                .verificationCode("OLD123")
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        authService.resendVerificationCode("candidate@test.com");

        assertNotNull(user.getVerificationCode());
        verify(userRepository).save(user);
        verify(emailService).sendVerificationEmail(eq("candidate@test.com"), anyString());
    }

    @Test
    void resendVerification_shouldThrowException_whenEmailNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.resendVerificationCode("missing@test.com");
        });
    }

    @Test
    void resendVerification_shouldThrowException_whenAccountAlreadyVerified() {
        User user = User.builder()
                .email("candidate@test.com")
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();

        when(userRepository.findByEmail("candidate@test.com")).thenReturn(Optional.of(user));

        assertThrows(AuthException.class, () -> {
            authService.resendVerificationCode("candidate@test.com");
        });

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void login_shouldThrowException_whenEmailNotFoundAfterAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("Password1");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(request);
        });
    }
}
