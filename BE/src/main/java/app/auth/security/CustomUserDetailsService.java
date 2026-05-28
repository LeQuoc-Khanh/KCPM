package app.auth.security;

import app.auth.model.User;
import app.auth.model.enums.UserRole;
import app.auth.model.enums.UserStatus;
import app.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional // Quan trọng: Để update DB nếu hết hạn
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // --- LOGIC KIỂM TRA HẾT HẠN VIP ---
        if (user.getVipExpirationDate() != null && user.getVipExpirationDate().isBefore(LocalDateTime.now())) {
            String roleName = user.getUserRole().name();
            
            // Nếu role hiện tại có đuôi _VIP thì cắt bỏ để về role thường
            if (roleName.endsWith("_VIP")) {
                String normalRoleName = roleName.replace("_VIP", "");
                try {
                    user.setUserRole(UserRole.valueOf(normalRoleName));
                    user.setVipExpirationDate(null); // Reset ngày hết hạn
                    userRepository.save(user);       // Lưu xuống DB ngay lập tức
                } catch (IllegalArgumentException e) {
                    // Log error nếu cần, giữ nguyên role nếu không map được
                }
            }
        }
        // --- Nếu user bị khoá, ném exception ---
        if (user.getStatus() == UserStatus.BANNED) {
            throw new DisabledException("Tài khoản đã bị khoá.");
        }
        // ----------------------------------

        return UserPrincipal.create(user);
    }
}