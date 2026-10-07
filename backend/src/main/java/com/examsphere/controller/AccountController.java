package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.AuthDtos.ChangePasswordRequest;
import com.examsphere.dto.UserDtos.Notification;
import com.examsphere.dto.UserDtos.UpdateProfileRequest;
import com.examsphere.dto.UserDtos.UserProfile;
import com.examsphere.security.AuthUser;
import com.examsphere.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own account, for every role. The profile is also reachable
 * under the role-specific paths (/api/student/profile, ...), which the URL rules restrict by role.
 */
@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping({"/api/account", "/api/student/profile", "/api/instructor/profile", "/api/admin/profile"})
    public ApiResponse<UserProfile> profile(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(accountService.getProfile(user));
    }

    @PutMapping({"/api/account", "/api/student/profile", "/api/instructor/profile", "/api/admin/profile"})
    public ApiResponse<UserProfile> updateProfile(@AuthenticationPrincipal AuthUser user,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Profile updated successfully.", accountService.updateProfile(user, request));
    }

    @PostMapping("/api/account/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthUser user,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(user, request);
        return ApiResponse.message("Password changed successfully.");
    }

    @GetMapping("/api/account/notifications")
    public ApiResponse<List<Notification>> notifications(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(accountService.notifications(user));
    }

    @PostMapping("/api/account/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthUser user) {
        accountService.logout(user);
        return ApiResponse.message("Logged out");
    }
}
