package app.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import app.auth.dto.request.ChangePasswordRequest;
import app.auth.dto.response.UserResponse;
import app.auth.exception.InvalidCredentialsException;
import app.auth.exception.UserNotFoundException;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "candidate@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn(EMAIL);

        user = User.builder()
                .id(1L)
                .fullName("Candidate Test")
                .email(EMAIL)
                .password("encoded-old-password")
                .userRole(UserRole.CANDIDATE)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .profileImageUrl("avatar.png")
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_shouldReturnResponse_whenUserExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser();

        assertEquals(user.getId(), response.getId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getUserRole(), response.getUserRole());
    }

    @Test
    void getCurrentUser_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, userService::getCurrentUser);
    }

    @Test
    void updateProfile_shouldUpdateProvidedFieldsAndSave() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateProfile("Updated Name", "new-avatar.png");

        assertEquals("Updated Name", user.getFullName());
        assertEquals("new-avatar.png", user.getProfileImageUrl());
        assertEquals("Updated Name", response.getFullName());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldKeepExistingFields_whenInputsAreBlankOrNull() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateProfile("   ", null);

        assertEquals("Candidate Test", user.getFullName());
        assertEquals("avatar.png", user.getProfileImageUrl());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateProfile("Updated Name", "new-avatar.png"));
        verify(userRepository, never()).save(user);
    }

    @Test
    void changePassword_shouldEncodeSaveAndRevokeTokens_whenOldPasswordMatches() {
        ChangePasswordRequest request = changePasswordRequest();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPass1")).thenReturn("encoded-new-password");

        userService.changePassword(request);

        assertEquals("encoded-new-password", user.getPassword());
        verify(userRepository).save(user);
        verify(refreshTokenService).deleteByUser(user);
    }

    @Test
    void changePassword_shouldThrow_whenOldPasswordDoesNotMatch() {
        ChangePasswordRequest request = changePasswordRequest();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1", user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(request));
        verify(userRepository, never()).save(user);
        verify(refreshTokenService, never()).deleteByUser(user);
    }

    @Test
    void changePassword_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.changePassword(changePasswordRequest()));
        verify(passwordEncoder, never()).encode("NewPass1");
    }

    @Test
    void deleteAccount_shouldRevokeTokensAndDeleteUser_whenUserExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        userService.deleteAccount();

        verify(refreshTokenService).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, userService::deleteAccount);
        verify(refreshTokenService, never()).deleteByUser(user);
        verify(userRepository, never()).delete(user);
    }

    private ChangePasswordRequest changePasswordRequest() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass1");
        request.setNewPassword("NewPass1");
        return request;
    }
}
