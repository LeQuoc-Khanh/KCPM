package app.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import app.admin.service.SystemSettingService;
import app.auth.dto.request.*;
import app.auth.dto.response.AuthResponse;
import app.auth.dto.response.UserResponse;
import app.auth.exception.*;
import app.auth.model.PasswordResetToken;
import app.auth.model.RefreshToken;
import app.auth.model.User;
import app.auth.model.enums.AuthProvider;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.PasswordResetTokenRepository;
import app.auth.repository.UserRepository;
import app.auth.security.JwtTokenProvider;
import app.service.CloudinaryService;
import app.exception.MaintenanceModeException;
import app.auth.repository.CompanyRepository;
import app.content.model.Company;

// --- MỚI: Import Event & Enum ---
import org.springframework.context.ApplicationEventPublisher;
import app.gamification.event.PointEvent;
import app.gamification.model.UserPointAction;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final GoogleOAuthService googleOAuthService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final SystemSettingService systemSettingService;
    
    // --- THAY ĐỔI: Dùng EventPublisher thay vì LeaderboardService ---
    private final ApplicationEventPublisher eventPublisher;

    
    // --- ĐĂNG KÝ ---
    @Transactional
    public AuthResponse register(RegisterRequest request, MultipartFile avatar) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email đã được sử dụng");
        }

        if (request.getUserRole() == UserRole.ADMIN) {
            throw new UnauthorizedException("Không thể đăng ký vai trò Quản trị viên qua đường dẫn này");
        }
        
        // Sinh mã xác thực 6 ký tự
        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userRole(request.getUserRole())
                .authProvider(AuthProvider.LOCAL)
                .status(UserStatus.PENDING_VERIFICATION)
                .isEmailVerified(false)
                .verificationCode(verificationCode)
                .build();
        
        // Lấy avatar mặc định dựa trên Role
        String defaultAvatar = getDefaultAvatar(request.getUserRole());
        
        // Upload Avatar
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String avatarUrl = cloudinaryService.uploadAvatar(avatar);
                user.setProfileImageUrl(avatarUrl);
            } catch (Exception e) {
                log.error("Lỗi upload avatar khi đăng ký: {}", e.getMessage());
                // Nếu upload lỗi, dùng avatar mặc định theo role
                user.setProfileImageUrl(defaultAvatar);
            }
        } else {
            // Nếu không có file, dùng avatar mặc định theo role
            user.setProfileImageUrl(defaultAvatar);
        }

        user = userRepository.save(user);
        if (user.getUserRole() == UserRole.RECRUITER || user.getUserRole() == UserRole.RECRUITER_VIP) {
            createDefaultCompanyForUser(user);
        }
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        
        log.info("User registered successfully via email, waiting for verification: {}", user.getId());
        
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    // --- XÁC THỰC EMAIL ---
    @Transactional
    public void verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

        if (user.getIsEmailVerified()) {
            throw new AuthException("Tài khoản đã được xác thực trước đó");
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new InvalidTokenException("Mã xác thực không chính xác");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setIsEmailVerified(true);
        user.setVerificationCode(null);
        userRepository.save(user);
        
        log.info("User verified email successfully: {}", email);
    }
    
    // --- [MỚI] GỬI LẠI MÃ XÁC THỰC ---
    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với email: " + email));

        if (user.getStatus() == UserStatus.ACTIVE || user.getIsEmailVerified()) {
            throw new AuthException("Tài khoản này đã được xác thực rồi. Bạn có thể đăng nhập ngay.");
        }

        // Tạo mã mới (Sử dụng cùng logic với hàm register để đồng bộ)
        String newCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        user.setVerificationCode(newCode);
        userRepository.save(user);

        // Gửi lại email
        emailService.sendVerificationEmail(user.getEmail(), newCode);
        
        log.info("Resent verification code to user: {}", email);
    }
    
    // --- ĐĂNG NHẬP LOCAL ---
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt with email: {}", request.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email hoặc mật khẩu không đúng"));
        
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
             throw new UnauthorizedException("Vui lòng xác thực email trước khi đăng nhập");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa hoặc chưa được kích hoạt");
        }
        
        // ✅ CHẶN NON-ADMIN KHI BẢO TRÌ
        if (systemSettingService.isMaintenanceEnabled()
            && user.getUserRole() != UserRole.ADMIN) {
        throw new MaintenanceModeException(systemSettingService.maintenanceMessage());
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // --- MỚI: Logic Gamification - Bắn Event ---
        try {
            eventPublisher.publishEvent(new PointEvent(
                this, 
                user.getId(), 
                user.getUserRole().name(), 
                UserPointAction.LOGIN_DAILY, 
                null // Login không cần RefId
            ));
        } catch (Exception e) {
            log.error("Lỗi bắn event Login: {}", e.getMessage());
        }
        // ----------------------------------------------------
        
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }
    
    // --- ĐĂNG NHẬP GOOGLE ---
    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        log.info("Processing Google Login");

        Map<String, String> googleInfo = googleOAuthService.verifyGoogleToken(request.getGoogleToken());
        
        String email = googleInfo.get("email");
        String googleId = googleInfo.get("googleId");
        String name = googleInfo.get("name");
        String pictureUrl = googleInfo.get("pictureUrl");

        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = false;
        if (user == null) {
            log.info("Creating new user from Google: {}", email);
            isNewUser = true;
            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .password(passwordEncoder.encode("GOOGLE_" + UUID.randomUUID()))
                    .userRole(request.getUserRole() != null ? request.getUserRole() : UserRole.CANDIDATE)
                    .authProvider(AuthProvider.GOOGLE)
                    .googleId(googleId)
                    .profileImageUrl(pictureUrl)
                    .status(UserStatus.ACTIVE)
                    .isEmailVerified(true)
                    .build();
            user = userRepository.save(user);
        } else {
            log.info("Updating existing user with Google info: {}", email);
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
            if ((user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty()) && pictureUrl != null) {
                user.setProfileImageUrl(pictureUrl);
            }
            if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.BANNED) {
                user.setStatus(UserStatus.ACTIVE);
                user.setIsEmailVerified(true);
            }
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            }

            if (isNewUser && (user.getUserRole() == UserRole.RECRUITER || user.getUserRole() == UserRole.RECRUITER_VIP)) {
            createDefaultCompanyForUser(user);
            }
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new UnauthorizedException("Tài khoản đã bị khóa hoặc chưa được kích hoạt");
            }
            

        // ✅ CHẶN NON-ADMIN KHI BẢO TRÌ
        if (systemSettingService.isMaintenanceEnabled()
            && user.getUserRole() != UserRole.ADMIN) {
        throw new MaintenanceModeException(systemSettingService.maintenanceMessage());
        }

        // --- MỚI: Logic Gamification - Bắn Event ---
        try {
            eventPublisher.publishEvent(new PointEvent(
                this, 
                user.getId(), 
                user.getUserRole().name(), 
                UserPointAction.LOGIN_DAILY, 
                null
            ));
        } catch (Exception e) {
            log.error("Lỗi bắn event Google Login: {}", e.getMessage());
        }
        // -----------------------------------------------------------

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    // --- LÀM MỚI TOKEN ---
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        return buildAuthResponse(user, newAccessToken, requestRefreshToken);
    }

    // --- ĐĂNG XUẤT ---
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        refreshTokenService.deleteByUser(user);
    }

    // --- QUÊN MẬT KHẨU ---
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với email này"));
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    // --- ĐẶT LẠI MẬT KHẨU ---
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Token không hợp lệ"));
        
        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Token đã được sử dụng");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token đã hết hạn");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .status(user.getStatus())
                .profileImageUrl(user.getProfileImageUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .vipExpirationDate(user.getVipExpirationDate())
                .build();
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userResponse)
                .build();
    }

    // --- Helper: Lấy Avatar mặc định theo Role ---
    private String getDefaultAvatar(UserRole role) {
        if (role == UserRole.RECRUITER) {
            return "https://res.cloudinary.com/dpym64zg9/image/upload/v1768899002/phantichcv/avatar/mqmr0xwe2dvbwibsfry3.png";
        } else if (role == UserRole.ADMIN) {
            return "https://res.cloudinary.com/dpym64zg9/image/upload/v1768899030/phantichcv/avatar/zvdmchfkluytc6aljtii.png";
        } else {
            // Mặc định là CANDIDATE
            return "https://res.cloudinary.com/dpym64zg9/image/upload/v1768898865/phantichcv/avatar/gqwoyrmv8osjl5hjlygz.png";
        }
    }

    private void createDefaultCompanyForUser(User user) {
        // Kiểm tra xem đã có công ty chưa để tránh lỗi
        if (companyRepository.findByRecruiterId(user.getId()).isEmpty()) {
            Company company = Company.builder()
                    .name("Công ty của " + user.getFullName()) // Tên mặc định
                    .email(user.getEmail()) // Lấy email của recruiter làm email liên hệ công ty
                    .recruiter(user)
                    .description("Thông tin công ty đang được cập nhật...")
                    .build();
            // Logo và Cover đã có giá trị default trong Builder của Entity Company
            companyRepository.save(company);
            log.info("Auto-created company for recruiter: {}", user.getId());
        }
    }
}