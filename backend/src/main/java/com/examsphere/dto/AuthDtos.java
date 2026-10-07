package com.examsphere.dto;

import com.examsphere.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response shapes of the authentication API. */
public final class AuthDtos {

    public static final String PHONE_PATTERN = "^\\+?[0-9][0-9\\s-]{6,17}$";
    public static final String PHONE_MESSAGE = "Enter a valid phone number (7-18 digits, optional +)";
    public static final int MAX_PHOTO_LENGTH = 400_000;

    private AuthDtos() {
    }

    public record StudentRegisterRequest(
            @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be 2-100 characters") String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Invalid email address") @Size(max = 150, message = "Email is too long") String email,
            @NotBlank(message = "Phone number is required") @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phone,
            @NotBlank(message = "Password is required") @StrongPassword String password,
            @NotBlank(message = "Please confirm your password") String confirmPassword,
            @NotBlank(message = "College / institution is required") @Size(max = 150, message = "Must be at most 150 characters") String college,
            @NotBlank(message = "Course / program is required") @Size(max = 100, message = "Must be at most 100 characters") String course,
            @NotBlank(message = "Branch / department is required") @Size(max = 100, message = "Must be at most 100 characters") String branch,
            @NotBlank(message = "Year / semester is required") @Size(max = 50, message = "Must be at most 50 characters") String yearSemester,
            @NotBlank(message = "Student ID / enrollment number is required") @Size(max = 50, message = "Must be at most 50 characters") String studentId,
            @NotNull(message = "You must accept the Terms & Conditions") @AssertTrue(message = "You must accept the Terms & Conditions") Boolean acceptTerms,
            @Size(max = MAX_PHOTO_LENGTH, message = "Profile photo is too large") String profilePhoto) {
    }

    public record InstructorRegisterRequest(
            @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be 2-100 characters") String fullName,
            @NotBlank(message = "Official email is required") @Email(message = "Invalid email address") @Size(max = 150, message = "Email is too long") String email,
            @NotBlank(message = "Phone number is required") @Pattern(regexp = PHONE_PATTERN, message = PHONE_MESSAGE) String phone,
            @NotBlank(message = "Password is required") @StrongPassword String password,
            @NotBlank(message = "Please confirm your password") String confirmPassword,
            @NotBlank(message = "Institution is required") @Size(max = 150, message = "Must be at most 150 characters") String institution,
            @NotBlank(message = "Department is required") @Size(max = 100, message = "Must be at most 100 characters") String department,
            @NotBlank(message = "Employee / faculty ID is required") @Size(max = 50, message = "Must be at most 50 characters") String employeeId,
            @NotBlank(message = "Designation is required") @Size(max = 100, message = "Must be at most 100 characters") String designation,
            @NotNull(message = "You must accept the Terms & Conditions") @AssertTrue(message = "You must accept the Terms & Conditions") Boolean acceptTerms,
            @Size(max = MAX_PHOTO_LENGTH, message = "Profile photo is too large") String profilePhoto) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record TokenRequest(@NotBlank(message = "Token is required") String token) {
    }

    public record EmailRequest(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required") String token,
            @NotBlank(message = "New password is required") @StrongPassword String newPassword,
            @NotBlank(message = "Please confirm your password") String confirmPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required") @StrongPassword String newPassword,
            @NotBlank(message = "Please confirm your password") String confirmPassword) {
    }

    public record RegistrationResponse(String email, String role, boolean emailDeliveryConfigured,
                                       int resendCooldownSeconds) {
    }

    /** status is VERIFIED or ALREADY_VERIFIED. */
    public record VerificationResponse(String status, String role, boolean approvalRequired) {
    }

    public record ResendResponse(int cooldownSeconds) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserDtos.UserProfile user) {
    }
}
