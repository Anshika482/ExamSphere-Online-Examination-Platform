package com.examsphere.service;

import com.examsphere.dto.AuthDtos.AuthResponse;
import com.examsphere.dto.AuthDtos.InstructorRegisterRequest;
import com.examsphere.dto.AuthDtos.LoginRequest;
import com.examsphere.dto.AuthDtos.RegistrationResponse;
import com.examsphere.dto.AuthDtos.ResendResponse;
import com.examsphere.dto.AuthDtos.ResetPasswordRequest;
import com.examsphere.dto.AuthDtos.StudentRegisterRequest;
import com.examsphere.dto.AuthDtos.VerificationResponse;
import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.AuditAction;
import com.examsphere.entity.EmailVerificationToken;
import com.examsphere.entity.PasswordResetToken;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.exception.AccountStatusException;
import com.examsphere.exception.ApiException;
import com.examsphere.exception.BadRequestException;
import com.examsphere.exception.DuplicateResourceException;
import com.examsphere.exception.InvalidCredentialsException;
import com.examsphere.exception.InvalidTokenException;
import com.examsphere.exception.TooManyRequestsException;
import com.examsphere.repository.EmailVerificationTokenRepository;
import com.examsphere.repository.PasswordResetTokenRepository;
import com.examsphere.repository.UserRepository;
import com.examsphere.security.JwtService;
import com.examsphere.util.TokenUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, email verification, login and password reset. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.verification.expiry-minutes:30}")
    private int verificationExpiryMinutes;
    @Value("${app.verification.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;
    @Value("${app.verification.max-per-day:5}")
    private int maxVerificationEmailsPerDay;
    @Value("${app.reset.expiry-minutes:30}")
    private int resetExpiryMinutes;
    @Value("${app.login.max-failed-attempts:5}")
    private int maxFailedAttempts;
    @Value("${app.login.lock-minutes:15}")
    private int lockMinutes;

    public AuthService(UserRepository userRepository,
                       EmailVerificationTokenRepository verificationTokenRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ registration

    @Transactional
    public RegistrationResponse registerStudent(StudentRegisterRequest request) {
        String email = normalizeEmail(request.email());
        String studentId = request.studentId().trim();
        requireMatchingPasswords(request.password(), request.confirmPassword());
        requireUniqueEmail(email);
        if (userRepository.existsByStudentId(studentId)) {
            throw new DuplicateResourceException("studentId", "This student ID is already registered.");
        }

        User user = new User();
        user.setRole(Role.STUDENT);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCollege(request.college().trim());
        user.setCourse(request.course().trim());
        user.setBranch(request.branch().trim());
        user.setYearSemester(request.yearSemester().trim());
        user.setStudentId(studentId);
        user.setProfilePhoto(AccountService.validatePhoto(request.profilePhoto()));
        user.setEmailVerified(false);
        user.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
        user.setActive(true);
        return completeRegistration(user);
    }

    @Transactional
    public RegistrationResponse registerInstructor(InstructorRegisterRequest request) {
        String email = normalizeEmail(request.email());
        String employeeId = request.employeeId().trim();
        requireMatchingPasswords(request.password(), request.confirmPassword());
        requireUniqueEmail(email);
        if (userRepository.existsByEmployeeId(employeeId)) {
            throw new DuplicateResourceException("employeeId", "This employee / faculty ID is already registered.");
        }

        User user = new User();
        user.setRole(Role.INSTRUCTOR);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setInstitution(request.institution().trim());
        user.setDepartment(request.department().trim());
        user.setEmployeeId(employeeId);
        user.setDesignation(request.designation().trim());
        user.setProfilePhoto(AccountService.validatePhoto(request.profilePhoto()));
        user.setEmailVerified(false);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        user.setActive(true);
        return completeRegistration(user);
    }

    private RegistrationResponse completeRegistration(User user) {
        userRepository.save(user);
        issueVerificationToken(user, false);
        auditService.record(user, AuditAction.REGISTRATION,
                user.getRole().name().toLowerCase(Locale.ROOT) + " account registered: " + user.getEmail());
        log.info("Registered {} account id={}", user.getRole(), user.getId());
        return new RegistrationResponse(user.getEmail(), user.getRole().name(),
                emailService.isDeliveryConfigured(), resendCooldownSeconds);
    }

    private void requireUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("email", "An account with this email already exists.");
        }
    }

    private void requireMatchingPasswords(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("confirmPassword", "Passwords do not match");
        }
    }

    // ------------------------------------------------------------------ email verification

    /** Creates a fresh single-use token, cancels all earlier ones and emails the link. */
    private void issueVerificationToken(User user, boolean resend) {
        verificationTokenRepository.invalidateAllForUser(user.getId(), LocalDateTime.now());
        String rawToken = TokenUtil.generateToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(TokenUtil.sha256(rawToken));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(verificationExpiryMinutes));
        verificationTokenRepository.save(token);
        emailService.sendVerificationEmail(user, rawToken, resend);
    }

    /**
     * Outcomes: VERIFIED, ALREADY_VERIFIED, or an InvalidTokenException with code
     * INVALID_TOKEN / TOKEN_USED / TOKEN_EXPIRED.
     */
    @Transactional
    public VerificationResponse verifyEmail(String rawToken) {
        EmailVerificationToken token = verificationTokenRepository.findByTokenHash(TokenUtil.sha256(rawToken.trim()))
                .orElseThrow(() -> new InvalidTokenException("INVALID_TOKEN", "This verification link is invalid."));
        User user = token.getUser();
        boolean approvalRequired = user.getRole() == Role.INSTRUCTOR
                && user.getApprovalStatus() != ApprovalStatus.APPROVED;

        if (user.isEmailVerified()) {
            return new VerificationResponse("ALREADY_VERIFIED", user.getRole().name(), approvalRequired);
        }
        if (token.isUsed()) {
            throw new InvalidTokenException("TOKEN_USED",
                    "This verification link has already been used or was replaced by a newer one.");
        }
        if (token.isExpired()) {
            throw new InvalidTokenException("TOKEN_EXPIRED", "This verification link has expired. Please request a new one.");
        }

        token.markUsed();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        auditService.record(user, AuditAction.EMAIL_VERIFIED, "Email verified: " + user.getEmail());
        return new VerificationResponse("VERIFIED", user.getRole().name(), approvalRequired);
    }

    /**
     * Sends a new verification email. Throttled by a cooldown and a daily cap.
     * The response is the same whether or not the address exists, so it cannot be used to probe for accounts.
     */
    @Transactional
    public ResendResponse resendVerification(String rawEmail) {
        Optional<User> found = userRepository.findByEmail(normalizeEmail(rawEmail));
        if (found.isEmpty() || found.get().isEmailVerified() || !found.get().isActive()) {
            return new ResendResponse(resendCooldownSeconds);
        }
        User user = found.get();
        LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerificationToken> latest =
                verificationTokenRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        if (latest.isPresent()) {
            long elapsed = Duration.between(latest.get().getCreatedAt(), now).getSeconds();
            if (elapsed < resendCooldownSeconds) {
                long wait = resendCooldownSeconds - elapsed;
                throw new TooManyRequestsException("Please wait " + wait + " seconds before requesting another email.", wait);
            }
        }
        long sentToday = verificationTokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(24));
        if (sentToday >= maxVerificationEmailsPerDay) {
            throw new TooManyRequestsException(
                    "You have reached the limit of verification emails for today. Please try again tomorrow.", 3600);
        }
        issueVerificationToken(user, true);
        return new ResendResponse(resendCooldownSeconds);
    }

    // ------------------------------------------------------------------ login

    /**
     * noRollbackFor keeps the failed-attempt counter even though the method ends with an exception.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));
        LocalDateTime now = LocalDateTime.now();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long wait = Math.max(1, Duration.between(now, user.getLockedUntil()).getSeconds());
            throw new TooManyRequestsException("Too many failed login attempts. Please try again later.", wait);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int failures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failures);
            if (failures >= maxFailedAttempts) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(now.plusMinutes(lockMinutes));
                log.warn("Account id={} temporarily locked after repeated failed logins", user.getId());
            }
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        // Account-state messages are only revealed after the password was proven correct.
        if (!user.isActive()) {
            throw new AccountStatusException("ACCOUNT_INACTIVE", "Your account is inactive.");
        }
        if (!user.isEmailVerified()) {
            throw new AccountStatusException("EMAIL_NOT_VERIFIED", "Please verify your email before logging in.");
        }
        if (user.getRole() == Role.INSTRUCTOR) {
            if (user.getApprovalStatus() == ApprovalStatus.PENDING) {
                throw new AccountStatusException("APPROVAL_PENDING", "Your instructor account is awaiting admin approval.");
            }
            if (user.getApprovalStatus() == ApprovalStatus.REJECTED) {
                throw new AccountStatusException("APPROVAL_REJECTED", "Your instructor application was rejected.");
            }
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        auditService.record(user, AuditAction.LOGIN, user.getFullName() + " logged in");
        return new AuthResponse(jwtService.generateToken(user), "Bearer", jwtService.getExpirationSeconds(),
                DtoMapper.toProfile(user));
    }

    // ------------------------------------------------------------------ password reset

    /** Always answers the same way; an email is only sent when the account exists. */
    @Transactional
    public void forgotPassword(String rawEmail) {
        Optional<User> found = userRepository.findByEmail(normalizeEmail(rawEmail));
        if (found.isEmpty() || !found.get().isActive()) {
            return;
        }
        User user = found.get();
        LocalDateTime now = LocalDateTime.now();
        Optional<PasswordResetToken> latest = resetTokenRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        if (latest.isPresent() && Duration.between(latest.get().getCreatedAt(), now).getSeconds() < resendCooldownSeconds) {
            return; // silently throttled
        }
        if (resetTokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(24)) >= maxVerificationEmailsPerDay) {
            return;
        }
        resetTokenRepository.invalidateAllForUser(user.getId(), now);
        String rawToken = TokenUtil.generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(TokenUtil.sha256(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(resetExpiryMinutes));
        resetTokenRepository.save(token);
        emailService.sendPasswordResetEmail(user, rawToken);
    }

    /** Lets the reset page show "invalid / expired / used" before the user types a new password. */
    @Transactional(readOnly = true)
    public void validateResetToken(String rawToken) {
        loadUsableResetToken(rawToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = loadUsableResetToken(request.token());
        requireMatchingPasswords(request.newPassword(), request.confirmPassword());
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        token.markUsed();
        auditService.record(user, AuditAction.PASSWORD_RESET, "Password reset via email link");
    }

    private PasswordResetToken loadUsableResetToken(String rawToken) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenUtil.sha256(rawToken.trim()))
                .orElseThrow(() -> new InvalidTokenException("INVALID_TOKEN", "This password reset link is invalid."));
        if (token.isUsed()) {
            throw new InvalidTokenException("TOKEN_USED", "This password reset link has already been used.");
        }
        if (token.isExpired()) {
            throw new InvalidTokenException("TOKEN_EXPIRED", "This password reset link has expired. Please request a new one.");
        }
        return token;
    }
}
