package app.admin.bva;

import app.admin.controller.AdminUserController;
import app.admin.dto.request.CreateAdminUserRequest;
import app.admin.service.impl.AdminUserServiceImpl;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.repository.UserRepository;
import app.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasLength;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminCreateUserBvaTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(userRepository, passwordEncoder);
        AdminUserController controller = new AdminUserController(service);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            user.setCreatedAt(LocalDateTime.of(2026, 7, 17, 12, 0));
            return user;
        });
    }

    static Stream<Arguments> bvaCases() {
        return Stream.of(
                arguments("TC01 Nominal", repeat('A', 50), repeat('P', 39), 200),
                arguments("TC02 fullName min- = 0", "", repeat('P', 39), 400),
                arguments("TC03 fullName min = 1", repeat('A', 1), repeat('P', 39), 200),
                arguments("TC04 fullName min+ = 2", repeat('A', 2), repeat('P', 39), 200),
                arguments("TC05 fullName max- = 99", repeat('A', 99), repeat('P', 39), 200),
                arguments("TC06 fullName max = 100", repeat('A', 100), repeat('P', 39), 200),
                arguments("TC07 fullName max+ = 101", repeat('A', 101), repeat('P', 39), 400),
                arguments("TC08 password min- = 5", repeat('A', 50), repeat('P', 5), 400),
                arguments("TC09 password min = 6", repeat('A', 50), repeat('P', 6), 200),
                arguments("TC10 password min+ = 7", repeat('A', 50), repeat('P', 7), 200),
                arguments("TC11 password max- = 71", repeat('A', 50), repeat('P', 71), 200),
                arguments("TC12 password max = 72", repeat('A', 50), repeat('P', 72), 200),
                arguments("TC13 password max+ = 73", repeat('A', 50), repeat('P', 73), 400)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bvaCases")
    void createUserBoundaryCases(String testCase, String fullName, String password, int expectedStatus)
            throws Exception {
        CreateAdminUserRequest request = validRequest(fullName, "tc@example.com", password, UserRole.CANDIDATE);

        var result = mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus));

        if (expectedStatus == 200) {
            result.andExpect(jsonPath("$.user.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.generatedPassword").isEmpty());
            verify(userRepository).save(any(User.class));
        } else {
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    @DisplayName("TC14 email null")
    void rejectNullEmail() throws Exception {
        assertRejected(validRequest(repeat('A', 50), null, repeat('P', 39), UserRole.CANDIDATE), 400);
    }

    @Test
    @DisplayName("TC15 email sai định dạng")
    void rejectMalformedEmail() throws Exception {
        assertRejected(validRequest(repeat('A', 50), "abc", repeat('P', 39), UserRole.CANDIDATE), 400);
    }

    @Test
    @DisplayName("TC16 email dài 101")
    void rejectEmailLongerThanMaximum() throws Exception {
        String email101 = repeat('a', 60) + "@" + repeat('b', 36) + ".com";
        assertRejected(validRequest(repeat('A', 50), email101, repeat('P', 39), UserRole.CANDIDATE), 400);
    }

    @Test
    @DisplayName("TC17 email đã tồn tại")
    void rejectDuplicateEmailUsingCurrentSourceBehavior() throws Exception {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        CreateAdminUserRequest request = validRequest(
                repeat('A', 50),
                "EXISTING@EXAMPLE.COM",
                repeat('P', 39),
                UserRole.CANDIDATE
        );

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("TC18 password null tự sinh")
    void generatePasswordWhenPasswordIsNull() throws Exception {
        CreateAdminUserRequest request = validRequest(
                repeat('A', 50),
                "generated@example.com",
                null,
                UserRole.CANDIDATE
        );

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedPassword", hasLength(12)))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("TC19 userRole null")
    void rejectNullRole() throws Exception {
        assertRejected(validRequest(repeat('A', 50), "role-null@example.com", repeat('P', 39), null), 400);
    }

    @Test
    @DisplayName("TC20 userRole không tồn tại")
    void rejectUnknownRoleUsingCurrentSourceBehavior() throws Exception {
        String json = """
                {
                  "fullName": "Nguyen Van A",
                  "email": "invalid-role@example.com",
                  "password": "123456",
                  "userRole": "SUPER_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());

        verify(userRepository, never()).save(any(User.class));
    }

    private void assertRejected(CreateAdminUserRequest request, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus));

        verify(userRepository, never()).save(any(User.class));
    }

    private static CreateAdminUserRequest validRequest(
            String fullName,
            String email,
            String password,
            UserRole role
    ) {
        return CreateAdminUserRequest.builder()
                .fullName(fullName)
                .email(email)
                .password(password)
                .userRole(role)
                .build();
    }

    private static String repeat(char value, int length) {
        return String.valueOf(value).repeat(length);
    }
}
