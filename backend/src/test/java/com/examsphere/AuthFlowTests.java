package com.examsphere;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.EmailVerificationToken;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.repository.EmailVerificationTokenRepository;
import com.examsphere.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthFlowTests extends IntegrationTestBase {

    @Autowired
    private EmailVerificationTokenRepository verificationTokens;

    @Test
    void studentRegistrationCreatesUnverifiedAccountWithHashedPassword() throws Exception {
        String email = uniqueEmail("student");
        String response = postJson("/api/auth/register/student", studentRegistration(email.toUpperCase()), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(PASSWORD).doesNotContain("passwordHash");

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(emails.verificationToken(email)).isNotBlank();
    }

    @Test
    void instructorRegistrationIsPendingApproval() throws Exception {
        String email = uniqueEmail("instructor");
        postJson("/api/auth/register/instructor", instructorRegistration(email), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail("dup");
        postJson("/api/auth/register/student", studentRegistration(email), null).andExpect(status().isCreated());
        postJson("/api/auth/register/student", studentRegistration(email), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        Map<String, Object> body = studentRegistration(uniqueEmail("weak"));
        body.put("password", "short");
        body.put("confirmPassword", "short");
        postJson("/api/auth/register/student", body, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void passwordMismatchIsRejected() throws Exception {
        Map<String, Object> body = studentRegistration(uniqueEmail("mismatch"));
        body.put("confirmPassword", PASSWORD + "x");
        postJson("/api/auth/register/student", body, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.confirmPassword").exists());
    }

    @Test
    void invalidEmailAndMissingTermsAreRejected() throws Exception {
        Map<String, Object> body = studentRegistration("not-an-email");
        body.put("acceptTerms", false);
        postJson("/api/auth/register/student", body, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.acceptTerms").exists());
    }

    @Test
    void unverifiedLoginIsBlockedUntilEmailIsVerified() throws Exception {
        String email = uniqueEmail("verify");
        postJson("/api/auth/register/student", studentRegistration(email), null).andExpect(status().isCreated());

        login(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        postJson("/api/auth/verify-email", Map.of("token", emails.verificationToken(email)), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
        assertThat(userRepository.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();

        String response = login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("passwordHash").doesNotContain(PASSWORD);

        // the same link a second time: the account is already verified
        postJson("/api/auth/verify-email", Map.of("token", emails.verificationToken(email)), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ALREADY_VERIFIED"));
    }

    @Test
    void invalidVerificationTokenIsRejected() throws Exception {
        postJson("/api/auth/verify-email", Map.of("token", "this-token-does-not-exist"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void expiredVerificationTokenIsRejected() throws Exception {
        String email = uniqueEmail("expired");
        postJson("/api/auth/register/student", studentRegistration(email), null).andExpect(status().isCreated());
        User user = userRepository.findByEmail(email).orElseThrow();
        EmailVerificationToken token = verificationTokens.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId()).orElseThrow();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        verificationTokens.save(token);

        postJson("/api/auth/verify-email", Map.of("token", emails.verificationToken(email)), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        assertThat(userRepository.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    void resendIsThrottledAndReplacesTheOldToken() throws Exception {
        String email = uniqueEmail("resend");
        postJson("/api/auth/register/student", studentRegistration(email), null).andExpect(status().isCreated());
        String firstToken = emails.verificationToken(email);

        // immediately after registration the cooldown is still running
        postJson("/api/auth/resend-verification", Map.of("email", email), null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        // pretend the first email was sent two minutes ago
        User user = userRepository.findByEmail(email).orElseThrow();
        EmailVerificationToken token = verificationTokens.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId()).orElseThrow();
        backdate(token.getId()); // created_at is not updatable through JPA, so plain SQL is used

        postJson("/api/auth/resend-verification", Map.of("email", email), null).andExpect(status().isOk());
        String secondToken = emails.verificationToken(email);
        assertThat(secondToken).isNotEqualTo(firstToken);

        // the older link was cancelled by the resend, the new one works
        postJson("/api/auth/verify-email", Map.of("token", firstToken), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_USED"));
        postJson("/api/auth/verify-email", Map.of("token", secondToken), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private void backdate(Long tokenId) {
        jdbc.update("update email_verification_tokens set created_at = ? where id = ?",
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusMinutes(2)), tokenId);
    }

    @Test
    void wrongPasswordGivesGenericError() throws Exception {
        User user = student();
        login(user.getEmail(), "Wrong-pass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        login("nobody@test.examsphere", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void pendingInstructorCannotLogin() throws Exception {
        User pending = saveUser(Role.INSTRUCTOR, true, ApprovalStatus.PENDING);
        login(pending.getEmail(), PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVAL_PENDING"));
    }

    @Test
    void inactiveAccountCannotLogin() throws Exception {
        User user = student();
        user.setActive(false);
        userRepository.save(user);
        login(user.getEmail(), PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void passwordResetLinkWorksOnce() throws Exception {
        User user = student();
        postJson("/api/auth/forgot-password", Map.of("email", user.getEmail()), null).andExpect(status().isOk());
        String token = emails.resetToken(user.getEmail());
        assertThat(token).isNotBlank();

        Map<String, Object> reset = Map.of("token", token, "newPassword", "Brand-new1", "confirmPassword", "Brand-new1");
        postJson("/api/auth/reset-password", reset, null).andExpect(status().isOk());

        login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        login(user.getEmail(), "Brand-new1").andExpect(status().isOk());

        postJson("/api/auth/reset-password", reset, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_USED"));
    }

    @Test
    void forgotPasswordDoesNotRevealWhetherAnAccountExists() throws Exception {
        postJson("/api/auth/forgot-password", Map.of("email", "ghost@test.examsphere"), null)
                .andExpect(status().isOk());
        assertThat(emails.resetToken("ghost@test.examsphere")).isNull();
    }
}
