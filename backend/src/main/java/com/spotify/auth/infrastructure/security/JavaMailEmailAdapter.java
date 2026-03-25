package com.spotify.auth.infrastructure.security;

import com.spotify.auth.application.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

/**
 * Infrastructure adapter: Gửi transactional emails dùng Spring Mail.
 * Gửi async (@Async) để không block request thread.
 * Trong dev mode không có SMTP → fallback về logging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailEmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String displayName, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Spotify Clone của bạn");
            helper.setText(buildVerificationHtml(displayName, verificationLink), true);
            mailSender.send(message);
            log.info("[EMAIL] Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String displayName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Yêu cầu đặt lại mật khẩu Spotify Clone");
            helper.setText(buildPasswordResetHtml(displayName, resetLink), true);
            mailSender.send(message);
            log.info("[EMAIL] Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendNewLoginNotification(String toEmail, String displayName, String ipAddress, String userAgent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Thông báo: Đăng nhập từ thiết bị mới");
            helper.setText(buildLoginNotificationHtml(displayName, ipAddress, userAgent), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EMAIL] Failed to send login notification to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationHtml(String name, String link) {
        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #1DB954;">Xin chào %s!</h2>
                <p>Cảm ơn bạn đã đăng ký Spotify Clone. Vui lòng xác thực email của bạn bằng cách nhấn vào nút bên dưới:</p>
                <a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #1DB954; 
                    color: white; text-decoration: none; border-radius: 24px; font-weight: bold;">
                    Xác thực Email
                </a>
                <p style="color: #888; font-size: 12px;">Link có hiệu lực trong 24 giờ.</p>
            </div>
            """, name, link);
    }

    private String buildPasswordResetHtml(String name, String link) {
        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #1DB954;">Đặt lại mật khẩu</h2>
                <p>Xin chào %s,</p>
                <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Nhấn vào nút bên dưới để tiếp tục:</p>
                <a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #1DB954; 
                    color: white; text-decoration: none; border-radius: 24px; font-weight: bold;">
                    Đặt lại mật khẩu
                </a>
                <p style="color: #888; font-size: 12px;">Link có hiệu lực trong 15 phút. Nếu bạn không yêu cầu điều này, hãy bỏ qua email này.</p>
            </div>
            """, name, link);
    }

    private String buildLoginNotificationHtml(String name, String ip, String userAgent) {
        return String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #e74c3c;">⚠️ Đăng nhập từ thiết bị mới</h2>
                <p>Xin chào %s,</p>
                <p>Chúng tôi phát hiện đăng nhập vào tài khoản của bạn từ một thiết bị mới:</p>
                <ul>
                    <li><strong>IP:</strong> %s</li>
                    <li><strong>Trình duyệt:</strong> %s</li>
                </ul>
                <p>Nếu đây không phải bạn, hãy đổi mật khẩu ngay lập tức tại: <a href="https://spotify-clone.app/security">Trung tâm bảo mật</a></p>
            </div>
            """, name, ip, userAgent);
    }
}
