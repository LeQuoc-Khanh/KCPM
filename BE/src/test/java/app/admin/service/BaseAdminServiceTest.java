package app.admin.service;

import app.admin.service.impl.AdminUserServiceImpl;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
abstract class BaseAdminServiceTest {

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected PasswordEncoder passwordEncoder;

    @Mock
    protected Authentication authentication;

    @Mock
    protected SecurityContext securityContext;

    @InjectMocks
    protected AdminUserServiceImpl adminUserService;

    protected User admin;
    protected User candidate;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
