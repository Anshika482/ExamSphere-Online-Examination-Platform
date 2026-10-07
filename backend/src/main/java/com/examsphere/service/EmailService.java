package com.examsphere.service;

import com.examsphere.entity.User;

/**
 * Outgoing email of the platform. Coding against this interface keeps the
 * business services independent of SMTP (the tests plug in a recording implementation).
 */
public interface EmailService {

    /** True when an SMTP host is configured, i.e. emails will really be delivered. */
    boolean isDeliveryConfigured();

    void sendVerificationEmail(User user, String rawToken, boolean resend);

    void sendPasswordResetEmail(User user, String rawToken);

    void sendInstructorApprovedEmail(User user);

    void sendInstructorRejectedEmail(User user);
}
