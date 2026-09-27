package com.courtly.service.mail;

import com.courtly.common.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gui email cua he thong.
 *
 * <p>Chay sau khi transaction commit va o luong khac: gui mail la loi goi ra ngoai,
 * khong duoc giu transaction ghi database va khong duoc bat nguoi dung cho SMTP tra loi.
 *
 * <p>Gui that bai thi chi ghi log. Nguoi dung da nhan phan hoi thanh cong tu truoc do va
 * van co the bam "Gui lai ma"; bao loi luc nay cung khong giup ho lam gi khac.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetCodeIssued(PasswordResetCodeIssued event) {
        if (!properties.enabled()) {
            // Moi truong chua cau hinh SMTP (hoac dang chay test): in ma ra log de con thu duoc.
            log.warn("MAIL TAT - ma dat lai mat khau cho {} la {}", event.email(), event.code());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart = true: bat buoc khi gui kem ca ban text thuan lan ban HTML.
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(event.email());
            helper.setSubject(event.code() + " la ma xac minh Courtly cua ban");
            helper.setText(plainTextBody(event), htmlBody(event));

            mailSender.send(message);
            log.info("Da gui ma dat lai mat khau toi {}", event.email());
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            // Bat rong: day la luong chay nen, loi thoat ra ngoai chi thanh mot dong
            // stacktrace vo thua vi va khong ai xu ly duoc.
            log.error("Khong gui duoc email dat lai mat khau toi {}", event.email(), e);
        }
    }

    private String plainTextBody(PasswordResetCodeIssued event) {
        return """
                Xin chao %s,

                Ma xac minh de dat lai mat khau Courtly cua ban la: %s
                Ma co hieu luc trong %d phut.

                Neu ban khong yeu cau dat lai mat khau, hay bo qua email nay.
                Khong chia se ma nay voi bat ky ai, ke ca nguoi tu xung la nhan vien Courtly.
                """.formatted(event.fullName(), event.code(), event.ttlMinutes());
    }

    private String htmlBody(PasswordResetCodeIssued event) {
        return """
                <div style="margin:0;padding:24px;background:#f5f5f4;font-family:'Segoe UI',Roboto,Arial,sans-serif">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e7e5e4">
                    <div style="background:#047857;padding:20px 28px">
                      <span style="color:#ffffff;font-size:18px;font-weight:700;letter-spacing:.5px">Courtly</span>
                    </div>
                    <div style="padding:28px">
                      <p style="margin:0 0 6px;font-size:15px;color:#1c1917">Xin ch&#224;o <strong>%s</strong>,</p>
                      <p style="margin:0 0 20px;font-size:14px;line-height:22px;color:#57534e">
                        D&#249;ng m&#227; b&#234;n dưới để x&#225;c minh danh t&#237;nh v&#224; đặt lại mật khẩu.
                      </p>
                      <div style="margin:0 0 20px;padding:18px;text-align:center;background:#ecfdf5;border:1px solid #a7f3d0;border-radius:10px">
                        <span style="font-size:34px;font-weight:700;letter-spacing:10px;color:#065f46">%s</span>
                      </div>
                      <p style="margin:0 0 18px;font-size:13px;color:#57534e">
                        M&#227; c&#243; hiệu lực trong <strong>%d ph&#250;t</strong>.
                      </p>
                      <p style="margin:0;padding:14px;background:#fffbeb;border:1px solid #fde68a;border-radius:8px;font-size:12px;line-height:20px;color:#92400e">
                        Nếu bạn kh&#244;ng y&#234;u cầu đặt lại mật khẩu, h&#227;y bỏ qua email n&#224;y.
                        Kh&#244;ng chia sẻ m&#227; cho bất kỳ ai, kể cả người tự xưng l&#224; nh&#226;n vi&#234;n Courtly.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(escape(event.fullName()), event.code(), event.ttlMinutes());
    }

    /** Ten nguoi dung do nguoi dung tu nhap, khong duoc chen thang vao HTML. */
    private static String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
