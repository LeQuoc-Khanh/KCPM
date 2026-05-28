package app.admin.service.impl;

import app.admin.dto.response.AdminUserResponse;
import app.admin.service.AdminUserService;
import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import app.admin.dto.request.CreateAdminUserRequest;
import app.admin.dto.request.UpdateUserRoleRequest;
import app.admin.dto.response.CreateAdminUserResponse;
import app.auth.model.enums.AuthProvider;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String PW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    public AdminUserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Không xác định được user đang đăng nhập");
        }

        String email = auth.getName(); // thường là email
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Không tìm thấy user theo email đăng nhập: " + email))
                .getId();
    }

    private String generateTempPassword(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
        sb.append(PW_CHARS.charAt(SECURE_RANDOM.nextInt(PW_CHARS.length())));
    }
    return sb.toString();
    }

    @Override
    public Page<AdminUserResponse> getAllUsers(String keyword, UserRole role, Pageable pageable) {
        Long excludeId = getCurrentAdminId();

        String k = (keyword == null) ? "" : keyword.trim();

        // ✅ loại trừ chính admin đang đăng nhập, vẫn search + paging
        Page<User> usersPage = userRepository.searchUsersExcludeId(excludeId, k, role, pageable);

        // ✅ dùng PageImpl để tránh lỗi type inference trong VS Code
        List<AdminUserResponse> responseList = usersPage.getContent().stream()
                .map(user -> AdminUserResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .userRole(user.getUserRole()) // ✅ khớp DTO của bạn
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList();

        return new PageImpl<>(responseList, pageable, usersPage.getTotalElements());
    }

    @Override
    public void lockUser(Long userId) {
        Long currentAdminId = getCurrentAdminId();
        if (userId.equals(currentAdminId)) {
            throw new IllegalStateException("Không thể tự khóa chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (user.getUserRole() == UserRole.ADMIN) {
            throw new IllegalStateException("Không thể khóa tài khoản ADMIN");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            return;
        }

        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
    }

    @Override
    public void unlockUser(Long userId) {
        Long currentAdminId = getCurrentAdminId();
        if (userId.equals(currentAdminId)) {
            throw new IllegalStateException("Không thể tự mở khóa chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public CreateAdminUserResponse createUser(CreateAdminUserRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email đã tồn tại trong hệ thống");
        }

        String rawPassword = request.getPassword();
        boolean generated = false;

        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateTempPassword(12);
            generated = true;
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .userRole(request.getUserRole())
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)          // admin tạo -> active luôn
                .isEmailVerified(true)              // khỏi phải xác thực email
                .build();

        User saved = userRepository.save(user);

        AdminUserResponse dto = AdminUserResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .userRole(saved.getUserRole())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();

        return CreateAdminUserResponse.builder()
                .user(dto)
                .generatedPassword(generated ? rawPassword : null)
                .build();
    }

    // Đổi vai trò người dùng
    @Override
    public AdminUserResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        Long currentAdminId = getCurrentAdminId();
        if (userId.equals(currentAdminId)) {
            throw new IllegalStateException("Không thể tự đổi vai trò của chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Không cho đổi role ADMIN (bạn có thể nới nếu muốn)
        if (user.getUserRole() == UserRole.ADMIN) {
            throw new IllegalStateException("Không thể đổi vai trò của tài khoản ADMIN");
        }

        user.setUserRole(request.getUserRole());
        User saved = userRepository.save(user);

        return AdminUserResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .userRole(saved.getUserRole())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
