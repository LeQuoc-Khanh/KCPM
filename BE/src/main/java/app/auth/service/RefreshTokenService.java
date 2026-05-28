
package app.auth.service;

// Lombok: Tự động tạo constructor với các trường final (hoặc @NonNull)
// @RequiredArgsConstructor giúp tránh viết tay constructor cho dependency injection (repository)
import lombok.RequiredArgsConstructor;

// Spring Framework: lấy giá trị cấu hình từ application.properties/yaml
import org.springframework.beans.factory.annotation.Value;

// Spring Framework: đánh dấu lớp là một Spring Service (bean thuộc tầng service)
import org.springframework.stereotype.Service;

// Spring Framework: đảm bảo tính toàn vẹn giao dịch cho các thao tác DB; nếu có lỗi sẽ rollback
import org.springframework.transaction.annotation.Transactional;

// Exception tuỳ chỉnh: quăng lỗi khi token không hợp lệ hoặc hết hạn
import app.auth.exception.InvalidTokenException;
import app.auth.model.RefreshToken;
import app.auth.model.User;
// Repository tương tác với DB (CRUD) cho RefreshToken (ví dụ save, findByToken, deleteByUser, deleteByExpiryDateBefore)
import app.auth.repository.RefreshTokenRepository;

import java.time.Instant; // Kiểu thời gian bất biến (UTC) để so sánh thời hạn token
import java.util.UUID;    // Sinh chuỗi ngẫu nhiên duy nhất cho trường token

// Đánh dấu lớp này là một Service trong Spring Container
@Service
// Lombok: tạo constructor gồm các trường final, giúp Spring inject RefreshTokenRepository
@RequiredArgsConstructor
public class RefreshTokenService {
    
    // Đọc biến cấu hình từ file (ví dụ application.properties) với key: jwt.refresh-token-expiration
    // Giá trị này là số milliseconds thời hạn của refresh token
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenDurationMs;
    
    // Repository được inject qua constructor (nhờ @RequiredArgsConstructor)
    private final RefreshTokenRepository refreshTokenRepository;
    
    /**
     * Tạo mới một RefreshToken cho user truyền vào
     * - token: sinh UUID ngẫu nhiên (giảm trùng lặp)
     * - expiryDate: thời gian hiện tại + refreshTokenDurationMs (milliseconds)
     * - Lưu vào DB bằng repository.save(...)
     * @param user: đối tượng User sở hữu token
     * @return RefreshToken đã được persist (có thể có id do DB sinh ra)
     */
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user) // Gán chủ sở hữu token
                .token(UUID.randomUUID().toString()) // Sinh chuỗi token ngẫu nhiên, duy nhất
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs)) // Thiết lập hạn token
                .build();
        
        // Lưu token vào DB và trả về thực thể đã lưu
        return refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * Kiểm tra thời hạn một RefreshToken
     * - Nếu token đã hết hạn (expiryDate trước thời điểm hiện tại):
     *   + Xoá token khỏi DB để tránh dùng lại
     *   + Ném InvalidTokenException với thông điệp tiếng Việt
     * - Nếu chưa hết hạn: trả về token như cũ
     * @param token: RefreshToken cần kiểm tra
     * @return token: nếu còn hạn
     * @throws InvalidTokenException: nếu token đã hết hạn
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            // Token hết hạn -> xoá khỏi DB để vô hiệu hoá ngay
            refreshTokenRepository.delete(token);
            // Ném lỗi để flow xử lý buộc người dùng đăng nhập lại
            throw new InvalidTokenException("Refresh token đã hết hạn. Vui lòng đăng nhập lại");
        }
        return token;
    }
    
    /**
     * Tìm RefreshToken theo chuỗi token
     * - Sử dụng repository.findByToken(token): trả về Optional
     * - Nếu không có: ném InvalidTokenException
     * @param token: chuỗi token cần tìm
     * @return RefreshToken: nếu tồn tại trong DB
     * @throws InvalidTokenException: nếu token không hợp lệ/không tồn tại
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token không hợp lệ"));
    }
    
    /**
     * Xoá tất cả refresh token thuộc về một User
     * - @Transactional đảm bảo thao tác xoá diễn ra trong một transaction
     * @param user: người dùng cần xoá token
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
    
    /**
     * Dọn dẹp (cleanup) các token đã hết hạn
     * - Xoá các token có expiryDate trước thời điểm hiện tại
     * - @Transactional để đảm bảo tính toàn vẹn nếu có nhiều bản ghi
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
       }
}