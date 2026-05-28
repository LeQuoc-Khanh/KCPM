package app.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email xác thực tài khoản khi đăng ký
     */
    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        try {
            log.info("Starting to send verification email to: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("CareerMate Support <noreply@careermate.com>");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản CareerMate");

            String content = "<div style=\"font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; border-radius: 10px;\">"
                    + "<h2 style=\"color: #2c3e50;\">Chào mừng đến với CareerMate!</h2>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản. Đây là mã xác thực của bạn:</p>"
                    + "<h1 style=\"color: #3498db; letter-spacing: 5px;\">" + code + "</h1>"
                    + "<p>Mã này có hiệu lực trong vòng 15 phút.</p>"
                    + "<p style=\"font-size: 12px; color: #888;\">Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>"
                    + "</div>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            // QUAN TRỌNG: Bắt Exception để không làm rollback giao dịch đăng ký nếu gửi mail lỗi
            // Hãy kiểm tra log console để xem lỗi chi tiết (thường là sai mật khẩu ứng dụng)
            log.error("FAILED to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Gửi email reset mật khẩu
     */
    @Async
    public void sendResetPasswordEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("CareerMate Support <noreply@careermate.com>");
            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu đặt lại mật khẩu - CareerMate");

            String content = "<p>Xin chào,</p>"
                    + "<p>Bạn đã yêu cầu đặt lại mật khẩu.</p>"
                    + "<p>Sử dụng mã Token sau đây để đặt lại mật khẩu:</p>"
                    + "<h3>" + token + "</h3>";

            helper.setText(content, true);
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}", toEmail, e);
        }
    }
}