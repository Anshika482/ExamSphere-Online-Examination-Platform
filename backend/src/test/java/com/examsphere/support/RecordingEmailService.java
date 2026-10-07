package com.examsphere.support;

import com.examsphere.entity.User;
import com.examsphere.service.EmailService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Test double for EmailService: nothing is sent, but the raw tokens that would
 * have been emailed are kept so the tests can follow the links like a user would.
 */
@Component
@Primary
public class RecordingEmailService implements EmailService {

    private final Map<String, String> verificationTokens = new ConcurrentHashMap<>();
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();
    private final List<String> approvalEmails = new ArrayList<>();
    private final List<String> rejectionEmails = new ArrayList<>();

    @Override
    public boolean isDeliveryConfigured() {
        return true;
    }

    @Override
    public void sendVerificationEmail(User user, String rawToken, boolean resend) {
        verificationTokens.put(user.getEmail(), rawToken);
    }

    @Override
    public void sendPasswordResetEmail(User user, String rawToken) {
        resetTokens.put(user.getEmail(), rawToken);
    }

    @Override
    public void sendInstructorApprovedEmail(User user) {
        approvalEmails.add(user.getEmail());
    }

    @Override
    public void sendInstructorRejectedEmail(User user) {
        rejectionEmails.add(user.getEmail());
    }

    public String verificationToken(String email) {
        return verificationTokens.get(email);
    }

    public String resetToken(String email) {
        return resetTokens.get(email);
    }

    public List<String> approvalEmails() {
        return approvalEmails;
    }

    public List<String> rejectionEmails() {
        return rejectionEmails;
    }
}
