
package app.auth.service;

// Google API Client: Thư viện xác thực Google ID Token (OAuth2/OpenID Connect)
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;            // Đại diện cho ID Token đã được Google phát hành
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;   // Trình xác thực ID Token (kiểm audience, chữ ký...)
import com.google.api.client.http.javanet.NetHttpTransport;                  // HTTP transport default dùng khi gọi xác thực
import com.google.api.client.json.gson.GsonFactory;                          // JSON factory (Gson) để parse token

// Lombok: @Slf4j tạo logger (log.info, log.error,...)
import lombok.extern.slf4j.Slf4j;

// Spring: @Value để inject cấu hình, @Service để đăng ký bean
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Exception tuỳ chỉnh khi token không hợp lệ
import app.auth.exception.InvalidTokenException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * GoogleOAuthService: Xác thực Google ID Token và trích xuất thông tin người dùng.
 * - @Service: đăng ký bean để có thể inject vào AuthService.
 * - @Slf4j: hỗ trợ ghi log, đặc biệt khi xác thực thất bại.
 */
@Service
@Slf4j
public class GoogleOAuthService {
    
    /**
     * googleClientId: Client ID của ứng dụng (OAuth 2.0) do Google cấp.
     * - Inject từ cấu hình (application.properties/yml) với key 'google.client-id'.
     * - Dùng để kiểm tra 'audience' của ID Token khớp với ứng dụng của bạn.
     */
    @Value("${google.client-id}")
    private String googleClientId;
    
    /**
     * verifyGoogleToken: Xác thực ID Token do Google trả về và trích xuất thông tin người dùng.
     * Quy trình:
     * 1) Tạo GoogleIdTokenVerifier với HTTP transport + JSON factory.
     * 2) Set audience là clientId của ứng dụng (chỉ chấp nhận token cấp cho app này).
     * 3) Gọi verifier.verify(token) để kiểm tra chữ ký, thời hạn, issuer,... 
     * 4) Nếu hợp lệ: lấy payload, trích xuất googleId (subject), email, name, picture, emailVerified.
     * 5) Nếu không: ném InvalidTokenException.
     *
     * @param token ID Token từ phía client (Google Sign-In).
     * @return Map thông tin người dùng: googleId, email, name, pictureUrl, emailVerified.
     * @throws InvalidTokenException nếu token không hợp lệ hoặc xác thực thất bại.
     */
    public Map<String, String> verifyGoogleToken(String token) {
        try {
            // Khởi tạo verifier với HTTP transport và JSON factory (Gson)
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
                )
                // Chỉ chấp nhận token có 'audience' khớp clientId của ứng dụng bạn
                .setAudience(Collections.singletonList(googleClientId))
                .build();
            
            // Xác thực token (kiểm tra chữ ký, issuer, expiry, audience...)
            GoogleIdToken idToken = verifier.verify(token);
            
            // Nếu không parse được token hợp lệ, ném lỗi
            if (idToken == null) {
                throw new InvalidTokenException("Google token không hợp lệ");
            }
            
            // Lấy payload (chứa các claim tiêu chuẩn và tuỳ chỉnh)
            GoogleIdToken.Payload payload = idToken.getPayload();
            
            // Trích xuất thông tin người dùng đưa vào Map trả về
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("googleId", payload.getSubject());                 // 'sub' - định danh duy nhất tài khoản Google
            userInfo.put("email", payload.getEmail());                      // email người dùng
            userInfo.put("name", (String) payload.get("name"));             // tên hiển thị (claim 'name')
            userInfo.put("pictureUrl", (String) payload.get("picture"));    // avatar (claim 'picture')
            userInfo.put("emailVerified", String.valueOf(payload.getEmailVerified())); // đã xác thực email hay chưa
            
            return userInfo;
            
        } catch (Exception e) {
            // Ghi log và trả về lỗi tuỳ chỉnh để service phía trên xử lý
            log.error("Error verifying Google token", e);
            throw new InvalidTokenException("Xác thực Google thất bại: " + e.getMessage());
        }
    }
}