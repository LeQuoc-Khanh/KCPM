package app.admin.service;

import app.admin.dto.request.CreateAdminUserRequest;
import app.admin.dto.request.UpdateUserRoleRequest;
import app.admin.dto.response.AdminUserResponse;
import app.admin.dto.response.CreateAdminUserResponse;
import app.admin.service.impl.AdminUserServiceImpl;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User admin;
    private User candidate;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.setContext(securityContext);

        lenient().when(securityContext.getAuthentication())
                .thenReturn(authentication);

        lenient().when(authentication.getName())
                .thenReturn("admin@test.com");

        admin = User.builder()
                .id(1L)
                .fullName("System Admin")
                .email("admin@test.com")
                .password("123")
                .userRole(UserRole.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        candidate = User.builder()
                .id(2L)
                .fullName("Nguyen Van A")
                .email("user@test.com")
                .password("123")
                .userRole(UserRole.CANDIDATE)
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));
    }

    // ==================================================
    // TC_6.1 Get all users successfully
    // ==================================================

    @Test
    @DisplayName("TC_6.1 Get all users successfully")
    void testGetAllUsersSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<User> page = new PageImpl<>(List.of(candidate));

        when(userRepository.searchUsersExcludeId(
                eq(1L),
                eq(""),
                isNull(),
                eq(pageable)))
                .thenReturn(page);

        Page<AdminUserResponse> result =
                adminUserService.getAllUsers("", null, pageable);

        assertEquals(1, result.getTotalElements());

        AdminUserResponse dto = result.getContent().get(0);

        assertEquals(candidate.getId(), dto.getId());
        assertEquals(candidate.getEmail(), dto.getEmail());
        assertEquals(candidate.getFullName(), dto.getFullName());
        assertEquals(candidate.getUserRole(), dto.getUserRole());

        verify(userRepository)
                .searchUsersExcludeId(1L, "", null, pageable);
    }

    // ==================================================
    // TC_6.2 Search user by keyword
    // ==================================================

    @Test
    @DisplayName("TC_6.2 Search users by keyword")
    void testSearchUsersSuccessfully() {

        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchUsersExcludeId(
                1L,
                "nguyen",
                null,
                pageable))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        Page<AdminUserResponse> result =
                adminUserService.getAllUsers(
                        "nguyen",
                        null,
                        pageable);

        assertFalse(result.isEmpty());

        verify(userRepository)
                .searchUsersExcludeId(
                        1L,
                        "nguyen",
                        null,
                        pageable);
    }

    // ==================================================
    // TC_6.3 Filter by role
    // ==================================================

    @Test
    @DisplayName("TC_6.3 Filter users by role")
    void testFilterUserByRole() {

        Pageable pageable = PageRequest.of(0, 5);

        when(userRepository.searchUsersExcludeId(
                1L,
                "",
                UserRole.CANDIDATE,
                pageable))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        Page<AdminUserResponse> result =
                adminUserService.getAllUsers(
                        "",
                        UserRole.CANDIDATE,
                        pageable);

        assertEquals(
                UserRole.CANDIDATE,
                result.getContent().get(0).getUserRole());
    }

        // ==================================================
    // TC_6.4 Lock user successfully
    // ==================================================

    @Test
    @DisplayName("TC_6.4 Lock user successfully")
    void testLockUserSuccessfully() {

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));

        adminUserService.lockUser(2L);

        assertEquals(UserStatus.BANNED, candidate.getStatus());

        verify(userRepository).save(candidate);
    }

    // ==================================================
    // TC_6.5 Unlock user successfully
    // ==================================================

    @Test
    @DisplayName("TC_6.5 Unlock user successfully")
    void testUnlockUserSuccessfully() {

        candidate.setStatus(UserStatus.BANNED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));

        adminUserService.unlockUser(2L);

        assertEquals(UserStatus.ACTIVE, candidate.getStatus());

        verify(userRepository).save(candidate);
    }

    // ==================================================
    // TC_6.6 Create admin user successfully
    // ==================================================

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

        CreateAdminUserResponse response =
                adminUserService.createUser(request);

        assertNotNull(response);
        assertEquals("newadmin@test.com",
                response.getUser().getEmail());
        assertNull(response.getGeneratedPassword());

        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
    }

    // ==================================================
    // TC_6.7 Create admin with generated password
    // ==================================================

    @Test
    @DisplayName("TC_6.7 Create admin with generated password")
    void testCreateAdminWithGeneratedPassword() {

        CreateAdminUserRequest request =
                CreateAdminUserRequest.builder()
                        .fullName("Generated Admin")
                        .email("admin2@test.com")
                        .password("")
                        .userRole(UserRole.ADMIN)
                        .build();

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(20L);
                    u.setCreatedAt(LocalDateTime.now());
                    return u;
                });

        CreateAdminUserResponse response =
                adminUserService.createUser(request);

        assertNotNull(response);
        assertNotNull(response.getGeneratedPassword());
        assertEquals("admin2@test.com",
                response.getUser().getEmail());

        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(any(User.class));
    }

    // ==================================================
    // TC_6.8 Update user role successfully
    // ==================================================

    @Test
    @DisplayName("TC_6.8 Update user role successfully")
    void testUpdateUserRoleSuccessfully() {

        UpdateUserRoleRequest request =
                UpdateUserRoleRequest.builder()
                        .userRole(UserRole.RECRUITER)
                        .build();

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response =
                adminUserService.updateUserRole(2L, request);

        assertNotNull(response);
        assertEquals(UserRole.RECRUITER,
                response.getUserRole());

        verify(userRepository).save(candidate);
    }

        // ==================================================
    // TC_6.9 Create admin failed when email exists
    // ==================================================

    @Test
    @DisplayName("TC_6.9 Create admin failed when email exists")
    void testCreateAdminDuplicateEmail() {

        CreateAdminUserRequest request =
                CreateAdminUserRequest.builder()
                        .fullName("Admin")
                        .email("admin@test.com")
                        .password("123456")
                        .userRole(UserRole.ADMIN)
                        .build();

        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminUserService.createUser(request));

        assertEquals(
                "Email đã tồn tại trong hệ thống",
                ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.10 Cannot lock own account
    // ==================================================

    @Test
    @DisplayName("TC_6.10 Cannot lock own account")
    void testCannotLockOwnAccount() {

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminUserService.lockUser(1L));

        assertEquals(
                "Không thể tự khóa chính mình",
                ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.11 Cannot unlock own account
    // ==================================================

    @Test
    @DisplayName("TC_6.11 Cannot unlock own account")
    void testCannotUnlockOwnAccount() {

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminUserService.unlockUser(1L));

        assertEquals(
                "Không thể tự mở khóa chính mình",
                ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.12 Cannot lock ADMIN account
    // ==================================================

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

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminUserService.lockUser(3L));

        assertEquals(
                "Không thể khóa tài khoản ADMIN",
                ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.13 Lock user not found
    // ==================================================

    @Test
    @DisplayName("TC_6.13 Lock user not found")
    void testLockUserNotFound() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> adminUserService.lockUser(99L));

        verify(userRepository, never()).save(any(User.class));
    }

        // ==================================================
    // TC_6.14 Unlock user not found
    // ==================================================

    @Test
    @DisplayName("TC_6.14 Unlock user not found")
    void testUnlockUserNotFound() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> adminUserService.unlockUser(99L));

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.15 Update role user not found
    // ==================================================

    @Test
    @DisplayName("TC_6.15 Update role user not found")
    void testUpdateRoleUserNotFound() {

        UpdateUserRoleRequest request =
                UpdateUserRoleRequest.builder()
                        .userRole(UserRole.RECRUITER)
                        .build();

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> adminUserService.updateUserRole(99L, request));

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.16 Cannot update own role
    // ==================================================

    @Test
    @DisplayName("TC_6.16 Cannot update own role")
    void testCannotUpdateOwnRole() {

        UpdateUserRoleRequest request =
                UpdateUserRoleRequest.builder()
                        .userRole(UserRole.RECRUITER)
                        .build();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminUserService.updateUserRole(1L, request));

        assertEquals(
                "Không thể tự đổi vai trò của chính mình",
                ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.17 Cannot update ADMIN role
    // ==================================================

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

        UpdateUserRoleRequest request =
                UpdateUserRoleRequest.builder()
                        .userRole(UserRole.RECRUITER)
                        .build();

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(adminUser));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminUserService.updateUserRole(3L, request));

        assertEquals(
                "Không thể đổi vai trò của tài khoản ADMIN",
                ex.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

        // ==================================================
    // TC_6.18 Lock already banned user
    // ==================================================

    @Test
    @DisplayName("TC_6.18 Lock already banned user")
    void testLockAlreadyBannedUser() {

        candidate.setStatus(UserStatus.BANNED);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));

        adminUserService.lockUser(2L);

        assertEquals(UserStatus.BANNED, candidate.getStatus());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.19 Unlock already active user
    // ==================================================

    @Test
    @DisplayName("TC_6.19 Unlock already active user")
    void testUnlockAlreadyActiveUser() {

        candidate.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(candidate));

        adminUserService.unlockUser(2L);

        assertEquals(UserStatus.ACTIVE, candidate.getStatus());

        verify(userRepository, never()).save(any(User.class));
    }

    // ==================================================
    // TC_6.20 Authentication is null
    // ==================================================

    @Test
    @DisplayName("TC_6.20 Authentication is null")
    void testAuthenticationNull() {

        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.getAllUsers(
                        "",
                        null,
                        PageRequest.of(0, 10))
        );
    }

    // ==================================================
    // TC_6.21 Current admin email not found
    // ==================================================

    @Test
    @DisplayName("TC_6.21 Current admin email not found")
    void testCurrentAdminNotFound() {

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> adminUserService.getAllUsers(
                        "",
                        null,
                        PageRequest.of(0, 10))
        );

        verify(userRepository)
                .findByEmail("admin@test.com");
    }

        // ==================================================
    // TC_6.22 Verify password encoded before save
    // ==================================================

    @Test
    @DisplayName("TC_6.22 Password encoded before save")
    void testPasswordEncodedBeforeSave() {

        CreateAdminUserRequest request = CreateAdminUserRequest.builder()
                .fullName("Encode Test")
                .email("encode@test.com")
                .password("123456")
                .userRole(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode("123456"))
                .thenReturn("ENCODED_PASSWORD");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(100L);
                    user.setCreatedAt(LocalDateTime.now());
                    return user;
                });

        adminUserService.createUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals(
                "ENCODED_PASSWORD",
                captor.getValue().getPassword());
    }

    // ==================================================
    // TC_6.23 Verify email converted to lowercase
    // ==================================================

    @Test
    @DisplayName("TC_6.23 Email converted to lowercase")
    void testEmailConvertedToLowercase() {

        CreateAdminUserRequest request =
                CreateAdminUserRequest.builder()
                        .fullName("Lower")
                        .email("LOWER@TEST.COM")
                        .password("123456")
                        .userRole(UserRole.ADMIN)
                        .build();

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(101L);
                    user.setCreatedAt(LocalDateTime.now());
                    return user;
                });

        adminUserService.createUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals(
                "lower@test.com",
                captor.getValue().getEmail());
    }

    // ==================================================
    // TC_6.24 Verify default values when create admin
    // ==================================================

    @Test
    @DisplayName("TC_6.24 Verify default values")
    void testDefaultValuesWhenCreateAdmin() {

        CreateAdminUserRequest request =
                CreateAdminUserRequest.builder()
                        .fullName("Admin")
                        .email("default@test.com")
                        .password("123456")
                        .userRole(UserRole.ADMIN)
                        .build();

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(102L);
                    user.setCreatedAt(LocalDateTime.now());
                    return user;
                });

        adminUserService.createUser(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();

        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertTrue(saved.getIsEmailVerified());
        assertEquals(AuthProvider.LOCAL, saved.getAuthProvider());
    }

    // ==================================================
    // TC_6.25 Verify response mapping
    // ==================================================

    @Test
    @DisplayName("TC_6.25 Verify response mapping")
    void testResponseMapping() {

        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.searchUsersExcludeId(
                eq(1L),
                eq(""),
                isNull(),
                eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(candidate)));

        Page<AdminUserResponse> result =
                adminUserService.getAllUsers(
                        "",
                        null,
                        pageable);

        assertEquals(1, result.getTotalElements());

        AdminUserResponse dto =
                result.getContent().get(0);

        assertEquals(candidate.getId(), dto.getId());
        assertEquals(candidate.getFullName(), dto.getFullName());
        assertEquals(candidate.getEmail(), dto.getEmail());
        assertEquals(candidate.getUserRole(), dto.getUserRole());
        assertEquals(candidate.getStatus(), dto.getStatus());
    }

}