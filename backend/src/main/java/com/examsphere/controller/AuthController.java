package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.AuthDtos.AuthResponse;
import com.examsphere.dto.AuthDtos.EmailRequest;
import com.examsphere.dto.AuthDtos.InstructorRegisterRequest;
import com.examsphere.dto.AuthDtos.LoginRequest;
import com.examsphere.dto.AuthDtos.RegistrationResponse;
import com.examsphere.dto.AuthDtos.ResendResponse;
import com.examsphere.dto.AuthDtos.ResetPasswordRequest;
import com.examsphere.dto.AuthDtos.StudentRegisterRequest;
import com.examsphere.dto.AuthDtos.TokenRequest;
import com.examsphere.dto.AuthDtos.VerificationResponse;
import com.examsphere.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints. Controllers only translate HTTP to service calls. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegistrationResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ApiResponse.ok("Account created. Please check your email to verify your account.",
                authService.registerStudent(request));
    }

    @PostMapping("/register/instructor")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegistrationResponse> registerInstructor(@Valid @RequestBody InstructorRegisterRequest request) {
        return ApiResponse.ok("Account created. Verify your email, then wait for admin approval.",
                authService.registerInstructor(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/verify-email")
    public ApiResponse<VerificationResponse> verifyEmail(@Valid @RequestBody TokenRequest request) {
        VerificationResponse response = authService.verifyEmail(request.token());
        String message = "VERIFIED".equals(response.status())
                ? "Email Verified Successfully" : "This email address is already verified.";
        return ApiResponse.ok(message, response);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<ResendResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        return ApiResponse.ok("Verification email sent.", authService.resendVerification(request.email()));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.message("If an account exists for this email, a password reset link has been sent.");
    }

    @PostMapping("/reset-password/validate")
    public ApiResponse<Void> validateResetToken(@Valid @RequestBody TokenRequest request) {
        authService.validateResetToken(request.token());
        return ApiResponse.message("Token is valid");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password updated successfully. You can now log in.");
    }
}
