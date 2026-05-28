
package app.auth.security;

// Servlet Filter: xử lý chuỗi filter cho mỗi request
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Lombok: tự động sinh constructor cho các field final và cung cấp logger
import lombok.RequiredArgsConstructor; // Tạo constructor với các field final
import lombok.extern.slf4j.Slf4j;      // Tạo logger 'log' (log.info, log.error,...)

// Spring annotations & Security core
import org.springframework.lang.NonNull; // Chú thích tham số không được null
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Token xác thực chứa thông tin user
import org.springframework.security.core.context.SecurityContextHolder; // Nơi lưu trữ trạng thái xác thực hiện tại (per-thread)
import org.springframework.security.core.userdetails.UserDetails; // Mô tả thông tin người dùng cho Spring Security
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // Gán chi tiết request vào authentication (IP, session,...)
import org.springframework.stereotype.Component; // Đăng ký bean Spring
import org.springframework.util.StringUtils; // Tiện ích chuỗi (kiểm tra null/empty)

// Filter chạy một lần mỗi request
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter: Filter kiểm tra JWT trong header Authorization cho mỗi request.
 * - @Component: đăng ký bean để Spring tự quản lý.
 * - @RequiredArgsConstructor: tự sinh constructor cho các field final (jwtTokenProvider, userDetailsService).
 * - @Slf4j: cung cấp logger để ghi log.
 * - Kế thừa OncePerRequestFilter: đảm bảo filter chỉ chạy một lần cho mỗi request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    // Provider xử lý JWT (validate, parse claims...)
    private final JwtTokenProvider jwtTokenProvider;
    // Service tải thông tin người dùng từ DB để build UserDetails
    private final CustomUserDetailsService userDetailsService;
    
    /**
     * doFilterInternal: logic chính của filter để:
     * - Lấy JWT từ header Authorization (Bearer {token})
     * - Xác thực token (validate)
     * - Trích xuất email từ token
     * - Tải UserDetails tương ứng và đặt vào SecurityContext (đã xác thực)
     * Luôn gọi filterChain.doFilter để chuyển tiếp xử lý cho các filter/handler tiếp theo.
     */
       @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // Lấy JWT từ request (header Authorization)
            String jwt = getJwtFromRequest(request);
            
            // Kiểm tra có token và token hợp lệ
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Trích xuất email (subject) từ token
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                
                // Tải thông tin người dùng theo email (UserDetails cho Spring Security)
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // Tạo đối tượng Authentication chứa userDetails và quyền (authorities)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,            // principal
                                null,                   // credentials (để null vì dùng JWT)
                                userDetails.getAuthorities() // quyền từ userDetails
                        );
                
                // Gắn chi tiết request (IP, session) vào authentication
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Đặt đối tượng Authentication vào SecurityContext (đánh dấu là đã xác thực)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Nếu có lỗi (ví dụ token không hợp lệ, lỗi parse...), ghi log nhưng vẫn cho request đi tiếp
            log.error("Could not set user authentication in security context", ex);
        }
        
        // Tiếp tục chuỗi filter cho request hiện tại
        filterChain.doFilter(request, response);
    }
    
    /**
     * getJwtFromRequest: Lấy token từ header Authorization theo chuẩn "Bearer <token>".
     * - Trả về token nếu hợp lệ; ngược lại trả về null.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        // Kiểm tra header có nội dung và bắt đầu bằng 'Bearer '
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Cắt bỏ tiền tố 'Bearer ' để lấy phần token thực sự
            return bearerToken.substring(7);
        }
        
        return null;
    }
}