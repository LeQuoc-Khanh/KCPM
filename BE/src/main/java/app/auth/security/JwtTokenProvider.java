
package app.auth.security;

// JJWT (io.jsonwebtoken): thư viện tạo, ký, parse và xác thực JWT
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

// Lombok: @Slf4j tạo logger (log.info, log.error, ...)
import lombok.extern.slf4j.Slf4j;

// Spring: @Value để inject giá trị từ cấu hình (application.yml/properties)
import org.springframework.beans.factory.annotation.Value;

// Spring Security: Authentication chứa principal (UserDetails) sau khi xác thực
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

// Spring: @Component đăng ký bean để Spring quản lý
import org.springframework.stereotype.Component;

// Java Crypto: SecretKey đại diện cho khóa bí mật dùng ký/verify JWT
import javax.crypto.SecretKey;

// Chuẩn charset để chuyển chuỗi secret sang bytes
import java.nio.charset.StandardCharsets;

// Ngày giờ kiểu Date (phù hợp với JJWT builder)
import java.util.Date;

/**
 * JwtTokenProvider: Cung cấp các tiện ích làm việc với JWT:
 * - Sinh access token / refresh token
 * - Trích xuất email (subject) từ token
 * - Xác thực token (chữ ký, hạn dùng, cấu trúc)
 *
 * @Component: đăng ký bean để có thể inject vào filter/service.
 * @Slf4j: hỗ trợ ghi log khi validate thất bại.
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    /**
     * jwtSecret: Chuỗi bí mật dùng để tạo khóa ký HMAC (HS256/HS512).
     * - Inject từ cấu hình: application.properties/yml với key 'jwt.secret'.
     * - Yêu cầu đủ độ dài (nhất là với HS512), nên là chuỗi ngẫu nhiên dài.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    /**
     * accessTokenExpiration: thời gian sống (ms) của access token.
     * - Inject từ cấu hình: 'jwt.access-token-expiration'.
     */
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    
    /**
     * refreshTokenExpiration: thời gian sống (ms) của refresh token.
     * - Inject từ cấu hình: 'jwt.refresh-token-expiration'.
     */
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    
    /**
     * Tạo khóa ký HMAC từ chuỗi secret.
     * - Keys.hmacShaKeyFor: sinh SecretKey phù hợp từ bytes.
     * - Dùng UTF-8 để chuyển chuỗi secret thành mảng byte.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Sinh access token từ đối tượng Authentication (đã có principal là UserDetails).
     * - Lấy username (email) từ principal.
     * - Gọi generateToken với hạn của access token.
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), accessTokenExpiration);
    }
    
    /**
     * Sinh access token trực tiếp từ email (subject).
     */
    public String generateAccessToken(String email) {
        return generateToken(email, accessTokenExpiration);
    }
    
    /**
     * Sinh refresh token trực tiếp từ email (subject).
     */
    public String generateRefreshToken(String email) {
        return generateToken(email, refreshTokenExpiration);
    }
    
    /**
     * generateToken: Hàm chung để tạo JWT với subject là email và thời gian hết hạn.
     * - issuedAt: thời điểm phát hành.
     * - expiration: thời điểm hết hạn (now + expiration mills).
     * - signWith: ký bằng SecretKey (HMAC). Thuật toán sẽ được chọn dựa trên loại key.
     * - compact: trả về chuỗi JWT (header.payload.signature).
     */
    private String generateToken(String email, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * getEmailFromToken: Parse token để lấy payload (Claims) và trả về subject (email).
     * - verifyWith(getSigningKey): xác thực chữ ký bằng cùng secret.
     * - parseSignedClaims(token): parse và verify token đã ký.
     * - getPayload(): lấy Claims.
     * - getSubject(): subject (email) đã set khi tạo token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }
    
    /**
     * validateToken: Kiểm tra token hợp lệ hay không.
     * - Parse và verify token: nếu thành công trả về true.
     * - Bắt các ngoại lệ thông dụng của JJWT:
     *   + SecurityException: chữ ký không hợp lệ.
     *   + MalformedJwtException: token không đúng định dạng.
     *   + ExpiredJwtException: token đã hết hạn.
     *   + UnsupportedJwtException: token không được hỗ trợ.
     *   + IllegalArgumentException: chuỗi claims rỗng.
     * - Ghi log lỗi và trả về false nếu có bất kỳ ngoại lệ nào.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
    
    /**
     * Getter thời gian hết hạn của access token (ms).
     * - Hữu ích khi cần trả về cho client trong AuthResponse.expiresIn.
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
       }
}