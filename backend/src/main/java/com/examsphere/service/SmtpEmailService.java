package com.examsphere.service;

import com.examsphere.entity.User;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends the branded HTML emails through SMTP.
 * When MAIL_HOST is empty the application still starts: the missing configuration
 * is logged and nothing is sent. Tokens are never written to the log unless the
 * developer explicitly opts in with APP_DEV_LOG_EMAIL_LINKS=true.
 */
@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean configured;
    private final String from;
    private final String frontendUrl;
    private final boolean devLogLinks;
    private final int verificationExpiryMinutes;
    private final int resetExpiryMinutes;

    public SmtpEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                            @Value("${spring.mail.host:}") String host,
                            @Value("${app.mail.from}") String from,
                            @Value("${app.frontend-url}") String frontendUrls,
                            @Value("${app.mail.dev-log-links:false}") boolean devLogLinks,
                            @Value("${app.verification.expiry-minutes:30}") int verificationExpiryMinutes,
                            @Value("${app.reset.expiry-minutes:30}") int resetExpiryMinutes) {
        this.mailSenderProvider = mailSenderProvider;
        this.configured = StringUtils.hasText(host);
        this.from = from;
        String first = frontendUrls.split(",")[0].trim();
        this.frontendUrl = first.endsWith("/") ? first.substring(0, first.length() - 1) : first;
        this.devLogLinks = devLogLinks;
        this.verificationExpiryMinutes = verificationExpiryMinutes;
        this.resetExpiryMinutes = resetExpiryMinutes;
        if (!configured) {
            log.warn("Email is not configured (MAIL_HOST is empty). The application runs, but no emails will be sent. "
                    + "Use the seeded demo accounts, or configure SMTP in .env to exercise real email verification.");
        }
    }

    @Override
    public boolean isDeliveryConfigured() {
        return configured;
    }

    @Override
    public void sendVerificationEmail(User user, String rawToken, boolean resend) {
        String link = frontendUrl + "/#/verify-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String body = paragraph("Hi " + escape(user.getFullName()) + ",")
                + paragraph(resend
                ? "Here is your new verification link. Any earlier link has been cancelled."
                : "Welcome to ExamSphere. Please confirm your email address to activate your account.")
                + button("Verify Email", link)
                + paragraph("This link can be used once and expires in " + verificationExpiryMinutes + " minutes.")
                + fallback(link);
        send(user.getEmail(), resend ? "Your new ExamSphere verification link" : "Verify your ExamSphere email",
                "Verify your email", body, link);
    }

    @Override
    public void sendPasswordResetEmail(User user, String rawToken) {
        String link = frontendUrl + "/#/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String body = paragraph("Hi " + escape(user.getFullName()) + ",")
                + paragraph("We received a request to reset your ExamSphere password.")
                + button("Reset Password", link)
                + paragraph("This link can be used once and expires in " + resetExpiryMinutes
                + " minutes. If you did not request this, you can ignore this email; your password stays unchanged.")
                + fallback(link);
        send(user.getEmail(), "Reset your ExamSphere password", "Reset your password", body, link);
    }

    @Override
    public void sendInstructorApprovedEmail(User user) {
        String link = frontendUrl + "/#/login";
        String body = paragraph("Welcome, " + escape(user.getFullName()) + ".")
                + paragraph("Your instructor account has been approved.")
                + paragraph("You can now log in and create exams.")
                + button("Log In", link);
        send(user.getEmail(), "Your ExamSphere Instructor Account Has Been Approved", "Account approved", body, null);
    }

    @Override
    public void sendInstructorRejectedEmail(User user) {
        String body = paragraph("Hi " + escape(user.getFullName()) + ",")
                + paragraph("Your instructor application has been rejected.")
                + paragraph("If you believe this is a mistake, please contact your institution's ExamSphere administrator.");
        send(user.getEmail(), "Your ExamSphere Instructor Application", "Application update", body, null);
    }

    private void send(String to, String subject, String heading, String bodyHtml, String devLink) {
        if (!configured) {
            if (devLogLinks && devLink != null) {
                log.warn("[DEV ONLY] Email to {} was not sent (SMTP not configured). Link: {}", to, devLink);
            } else {
                log.warn("Email '{}' to {} was not sent because SMTP is not configured.", subject, to);
            }
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("Email '{}' could not be sent: no mail sender is available.", subject);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(layout(heading, bodyHtml), true);
            sender.send(message);
            log.info("Email '{}' sent to {}", subject, to);
        } catch (Exception ex) {
            // A mail outage must not break registration or approval; the user can request a resend.
            log.error("Failed to send email '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }

    // ---- small HTML template helpers (inline styles, because email clients ignore stylesheets) ----

    private String layout(String heading, String bodyHtml) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#050706;font-family:Segoe UI,Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#050706;padding:32px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;background:#0e1311;border:1px solid #1f2a25;border-radius:16px;\">"
                + "<tr><td style=\"padding:28px 32px 8px 32px;\"><div style=\"font-size:20px;font-weight:700;color:#ffffff;\">Exam<span style=\"color:#34d399;\">Sphere</span></div>"
                + "<div style=\"font-size:12px;color:#8a9a92;margin-top:2px;\">Smarter Examinations. Brighter Futures.</div></td></tr>"
                + "<tr><td style=\"padding:16px 32px 8px 32px;\"><h1 style=\"margin:0;font-size:22px;color:#ffffff;\">" + heading + "</h1></td></tr>"
                + "<tr><td style=\"padding:8px 32px 28px 32px;\">" + bodyHtml + "</td></tr>"
                + "<tr><td style=\"padding:16px 32px;border-top:1px solid #1f2a25;font-size:12px;color:#6f7f77;\">This is an automated message from ExamSphere. Please do not reply.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String paragraph(String html) {
        return "<p style=\"margin:0 0 14px 0;font-size:15px;line-height:1.6;color:#cfd8d3;\">" + html + "</p>";
    }

    private String button(String label, String link) {
        return "<p style=\"margin:22px 0;\"><a href=\"" + link + "\" style=\"display:inline-block;background:#34d399;color:#04110b;"
                + "font-weight:700;font-size:15px;text-decoration:none;padding:12px 26px;border-radius:10px;\">" + label + "</a></p>";
    }

    private String fallback(String link) {
        return "<p style=\"margin:0;font-size:12px;line-height:1.6;color:#8a9a92;word-break:break-all;\">"
                + "If the button does not work, copy this link into your browser:<br>" + link + "</p>";
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
