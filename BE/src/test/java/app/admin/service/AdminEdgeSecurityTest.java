package app.admin.service;

import app.admin.dto.request.CreateAdminUserRequest;
import app.admin.dto.request.UpdateUserRoleRequest;
import app.admin.dto.response.AdminUserResponse;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminEdgeSecurityTest extends BaseAdminServiceTest {

    @Test
    @DisplayName("TC_6.16 Cannot update own role")
    void testCannotUpdateOwnRole() {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .userRole(UserRole.RECRUITER)
                .build();

        assertThrows(IllegalStateException.class, () -> adminUserService.updateUserRole(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.17 Cannot update ADMIN role")
    void testCannotUpdateAdminRole() {
        User adminUser = User.builder()
                .id(3L)
                .fullName("Admin")
                .email("admin2@test.com")
                .userRole(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .userRole(UserRole.RECRUITER)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> adminUserService.updateUserRole(3L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.18 Lock already banned user")
    void testLockAlreadyBannedUser() {
        candidate.setStatus(UserStatus.BANNED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));

        adminUserService.lockUser(2L);

        assertEquals(UserStatus.BANNED, candidate.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.19 Unlock already active user")
    void testUnlockAlreadyActiveUser() {
        candidate.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));

        adminUserService.unlockUser(2L);

        assertEquals(UserStatus.ACTIVE, candidate.getStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.20 Authentication is null")
    void testAuthenticationNull() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.getAllUsers("", null, PageRequest.of(0, 10))
        );
    }

    @Test
    @DisplayName("TC_6.21 Current admin email not found")
    void testCurrentAdminNotFound() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.getAllUsers("", null, PageRequest.of(0, 10))
        );

        verify(userRepository).findByEmail("admin@test.com");
    }

    @Test
    @DisplayName("TC_6.22 Password encoded before save")
    void testPasswordEncodedBeforeSave() {
        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("Encode Test")
                .email("encode@test.com")
                .password("123456")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("ENCODED_PASSWORD");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        adminUserService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("ENCODED_PASSWORD", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("TC_6.23 Email converted to lowercase")
    void testEmailConvertedToLowercase() {
        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("Lower")
                .email("LOWER@TEST.COM")
                .password("123456")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        adminUserService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("lower@test.com", captor.getValue().getEmail());
    }

    @Test
    @DisplayName("TC_6.24 Verify default values")
    void testDefaultValuesWhenCreateAdmin() {
        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("Admin")
                .email("default@test.com")
                .password("123456")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(102L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        adminUserService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertTrue(saved.getIsEmailVerified());
        assertEquals(AuthProvider.LOCAL, saved.getAuthProvider());
    }

    @Test
    @DisplayName("TC_6.25 Verify response mapping")
    void testResponseMapping() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchUsersExcludeId(eq(1L), eq(""), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        Page<AdminUserResponse> result = adminUserService.getAllUsers("", null, pageable);

        assertEquals(1, result.getTotalElements());

        AdminUserResponse dto = result.getContent().get(0);
        assertEquals(candidate.getId(), dto.getId());
        assertEquals(candidate.getFullName(), dto.getFullName());
        assertEquals(candidate.getEmail(), dto.getEmail());
        assertEquals(candidate.getUserRole(), dto.getUserRole());
        assertEquals(candidate.getStatus(), dto.getStatus());
    }
}
