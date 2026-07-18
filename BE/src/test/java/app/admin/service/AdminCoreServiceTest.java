package app.admin.service;

import app.admin.dto.request.CreateAdminUserRequest;
import app.admin.dto.request.UpdateUserRoleRequest;
import app.admin.dto.response.AdminUserResponse;
import app.admin.dto.response.CreateAdminUserResponse;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminCoreServiceTest extends BaseAdminServiceTest {

    @Test
    @DisplayName("TC_6.1 Get all users successfully")
    void testGetAllUsersSuccessfully() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(candidate));

        when(userRepository.searchUsersExcludeId(eq(1L), eq(""), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<AdminUserResponse> result = adminUserService.getAllUsers("", null, pageable);

        assertEquals(1, result.getTotalElements());

        AdminUserResponse dto = result.getContent().get(0);
        assertEquals(candidate.getId(), dto.getId());
        assertEquals(candidate.getEmail(), dto.getEmail());
        assertEquals(candidate.getFullName(), dto.getFullName());
        assertEquals(candidate.getUserRole(), dto.getUserRole());

        verify(userRepository).searchUsersExcludeId(1L, "", null, pageable);
    }

    @Test
    @DisplayName("TC_6.2 Search users by keyword")
    void testSearchUsersSuccessfully() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchUsersExcludeId(1L, "nguyen", null, pageable))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        Page<AdminUserResponse> result =
                adminUserService.getAllUsers("nguyen", null, pageable);
        Page<AdminUserResponse> result = adminUserService.getAllUsers("nguyen", null, pageable);

        assertFalse(result.isEmpty());

        verify(userRepository).searchUsersExcludeId(1L, "nguyen", null, pageable);
    }

    @Test
    @DisplayName("TC_6.3 Filter users by role")
    void testFilterUserByRole() {
        Pageable pageable = PageRequest.of(0, 5);

        when(userRepository.searchUsersExcludeId(1L, "", UserRole.CANDIDATE, pageable))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        Page<AdminUserResponse> result =
                adminUserService.getAllUsers("", UserRole.CANDIDATE, pageable);
        Page<AdminUserResponse> result = adminUserService.getAllUsers("", UserRole.CANDIDATE, pageable);

        assertEquals(UserRole.CANDIDATE, result.getContent().get(0).getUserRole());
    }

    @Test
    @DisplayName("TC_6.4 Lock user successfully")
    void testLockUserSuccessfully() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));

        adminUserService.lockUser(2L);

        assertEquals(UserStatus.BANNED, candidate.getStatus());

        verify(userRepository).save(candidate);
    }

    @Test
    @DisplayName("TC_6.5 Unlock user successfully")
    void testUnlockUserSuccessfully() {
        candidate.setStatus(UserStatus.BANNED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));

        adminUserService.unlockUser(2L);

        assertEquals(UserStatus.ACTIVE, candidate.getStatus());

        verify(userRepository).save(candidate);
    }

    @Test
    @DisplayName("TC_6.6 Create admin user successfully")
    void testCreateAdminUserSuccessfully() {
        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("New Admin")
                .email("NEWADMIN@TEST.COM")
                .password("123456")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail("newadmin@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("123456"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(10L);
                    u.setCreatedAt(LocalDateTime.now());
                    return u;
                });
        when(userRepository.existsByEmail("newadmin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        CreateAdminUserResponse response = adminUserService.createUser(request);

        assertNotNull(response);
        assertEquals("newadmin@test.com", response.getUser().getEmail());
        assertNull(response.getGeneratedPassword());

        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.7 Create admin with generated password")
    void testCreateAdminWithGeneratedPassword() {
        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("Generated Admin")
                .email("admin2@test.com")
                .password("")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail(any(String.class)))
                .thenReturn(false);

        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(20L);
                    u.setCreatedAt(LocalDateTime.now());
                    return u;
                });
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(20L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        CreateAdminUserResponse response = adminUserService.createUser(request);

        assertNotNull(response);
        assertNotNull(response.getGeneratedPassword());
        assertEquals("admin2@test.com", response.getUser().getEmail());

        verify(passwordEncoder).encode(any(String.class));
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.8 Update user role successfully")
    void testUpdateUserRoleSuccessfully() {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .userRole(UserRole.RECRUITER)
                .build();

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(candidate));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminUserService.updateUserRole(2L, request);

        assertNotNull(response);
        assertEquals(UserRole.RECRUITER, response.getUserRole());

        verify(userRepository).save(candidate);
    }

    @Test
    @DisplayName("TC_6.9 Create admin failed when email exists")
    void testCreateAdminDuplicateEmail() {
        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("Admin")
                .email("admin@test.com")
                .password("123456")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.createUser(request));

        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> adminUserService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.10 Cannot lock own account")
    void testCannotLockOwnAccount() {
        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.lockUser(1L));

        assertThrows(IllegalStateException.class, () -> adminUserService.lockUser(1L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.11 Cannot unlock own account")
    void testCannotUnlockOwnAccount() {
        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.unlockUser(1L));

        assertThrows(IllegalStateException.class, () -> adminUserService.unlockUser(1L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.12 Cannot lock ADMIN account")
    void testCannotLockAdminAccount() {
        User anotherAdmin = User.builder()
                .id(3L)
                .fullName("Another Admin")
                .email("admin2@test.com")
                .userRole(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(anotherAdmin));

        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.lockUser(3L));

        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdmin));

        assertThrows(IllegalStateException.class, () -> adminUserService.lockUser(3L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.13 Lock user not found")
    void testLockUserNotFound() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> adminUserService.lockUser(99L));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminUserService.lockUser(99L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.14 Unlock user not found")
    void testUnlockUserNotFound() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> adminUserService.unlockUser(99L));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminUserService.unlockUser(99L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC_6.15 Update role user not found")
    void testUpdateRoleUserNotFound() {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .userRole(UserRole.RECRUITER)
                .build();

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> adminUserService.updateUserRole(99L, request));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminUserService.updateUserRole(99L, request));
        verify(userRepository, never()).save(any(User.class));
    }
}
