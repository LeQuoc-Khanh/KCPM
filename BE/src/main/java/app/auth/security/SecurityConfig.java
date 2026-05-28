package app.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import app.admin.service.SystemSettingService;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final AuthEntryPoint authEntryPoint;
    private final SystemSettingService systemSettingService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF vì dùng JWT (Stateless)
            .csrf(AbstractHttpConfigurer::disable)
            // Cấu hình CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Xử lý lỗi 401 Unauthorized
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authEntryPoint)
            )
            // Quản lý session stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ======================================================
                // 1. PUBLIC ENDPOINTS (Không cần đăng nhập)
                // ======================================================
                .requestMatchers(
                    "/api/auth/**",       // Login, Register, Refresh Token
                    "/api/public/**",     // Các API public chung
                    "/swagger-ui/**",     // Swagger UI
                    "/v3/api-docs/**", 
                    "/api/recruiter/jobs/public/**",   // API Docs
                    "/api/chat/ask"
                ).permitAll()
                // ======================================================
                // 2. PAYMENT (Cho phép mọi User đã login thực hiện thanh toán)
                // ======================================================
                .requestMatchers("/api/payment/**").authenticated()
                .requestMatchers("/api/leaderboard/**").authenticated()
                // ======================================================
                // 3. TÍNH NĂNG AI & VIP (CHỈ DÀNH CHO ROLE VIP & ADMIN)
                // ======================================================
                
                // Phỏng vấn ảo (Interview): Chỉ Candidate VIP
                .requestMatchers("/api/interview/**").hasAnyAuthority("CANDIDATE_VIP", "ADMIN")
                
                // Phân tích CV: Chỉ Candidate VIP và Recruiter VIP
                .requestMatchers("/api/cv-analysis/**").hasAnyAuthority("CANDIDATE_VIP", "RECRUITER_VIP", "ADMIN")

                // Chat AI & Tư vấn nghề nghiệp: Dành cho cả 2 loại VIP
                .requestMatchers("/api/career-advice/**", "/api/chat/**")
                    .hasAnyAuthority("CANDIDATE_VIP", "RECRUITER_VIP", "ADMIN")

                // AI Matching cho Recruiter (Lọc ứng viên tự động): Chỉ Recruiter VIP
                .requestMatchers("/api/ai/recruiter/**").hasAnyAuthority("RECRUITER_VIP", "ADMIN")
                
                // AI Matching cho Candidate (Tìm việc phù hợp): Chỉ Candidate VIP
                .requestMatchers("/api/ai/candidate/**").hasAnyAuthority("CANDIDATE_VIP", "ADMIN")

                // ======================================================
                // 4. QUẢN TRỊ VIÊN (ADMIN)
                // ======================================================
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                // Quản lý Content (Bài viết) - Admin
                .requestMatchers("/api/content/admin/**").hasAuthority("ADMIN")

                // ======================================================
                // 5. CHỨC NĂNG CƠ BẢN (Cả Role Thường và VIP đều dùng được)
                // ======================================================
                
                // --- KHU VỰC NHÀ TUYỂN DỤNG ---
                // Bao gồm: Đăng tin, Quản lý Job, Xem hồ sơ ứng tuyển
                // Quan trọng: Phải cấp quyền cho cả RECRUITER và RECRUITER_VIP
                .requestMatchers("/api/recruiter/**")
                    .hasAnyAuthority("RECRUITER", "RECRUITER_VIP", "ADMIN")

                // --- KHU VỰC ỨNG VIÊN ---
                // Bao gồm: Profile, Upload CV, Xem Job
                // Quan trọng: Phải cấp quyền cho cả CANDIDATE và CANDIDATE_VIP
                .requestMatchers("/api/candidate/**")
                    .hasAnyAuthority("CANDIDATE", "CANDIDATE_VIP", "ADMIN")

                // Trong SecurityConfig.java
                .requestMatchers("/api/recruiter/jobs/public/**").permitAll() 

                // ======================================================
                // 6. CHỨC NĂNG TUYỂN DỤNG CHUNG (/api/recruitment)
                // ======================================================
                
                // Tìm kiếm ứng viên (Candidate Search): Dành cho Recruiter (Thường + VIP)
                .requestMatchers("/api/recruitment/candidates/**")
                    .hasAnyAuthority("RECRUITER", "RECRUITER_VIP", "ADMIN")

                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/companies/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/company/**").permitAll()
                
                // Các chức năng nộp đơn, xem đơn: Cần đăng nhập là dùng được
                .requestMatchers("/api/recruitment/**").authenticated()
                
                // Xem bài viết (Blog/Tin tức): Public hoặc Authenticated tuỳ logic
                .requestMatchers("/api/content/**").permitAll()

                // ======================================================
                // 7. API THỬ NGHIỆM / DEBUG (Khóa chặt)
                // ======================================================
                // Chỉ Admin mới được gọi test Gemini hoặc các API test khác
                .requestMatchers("/api/gemini/test/**", "/api/test/**").hasAnyAuthority("ADMIN", "RECRUITER_VIP", "CANDIDATE_VIP")

                // ======================================================
                // 8. MẶC ĐỊNH
                // ======================================================
                
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new MaintenanceModeFilter(systemSettingService), JwtAuthenticationFilter.class);

        
        return http.build();
    }
    
    // --- Các Bean cấu hình Authentication (Giữ nguyên) ---

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // --- Cấu hình CORS (Cho phép FE gọi API) ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000", // Next.js
            "http://localhost:5173", // Vite
            "http://localhost:8081"  // Khác
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH",  "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}