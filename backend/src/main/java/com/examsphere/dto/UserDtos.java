package com.examsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    /** The signed-in user's own profile. Never contains the password hash. */
    public record UserProfile(Long id, String fullName, String email, String phone, String role,
                              String college, String course, String branch, String yearSemester, String studentId,
                              String institution, String department, String employeeId, String designation,
                              String profilePhoto, boolean emailVerified, String approvalStatus, boolean active,
                              LocalDateTime createdAt) {
    }

    /** Row of the admin user tables (no photo, to keep list responses small). */
    public record UserSummary(Long id, String fullName, String email, String phone, String role,
                              boolean emailVerified, LocalDateTime emailVerifiedAt, String approvalStatus,
                              boolean active, boolean demoAccount, String organisation, String department,
                              String idNumber, String designation, String course, String yearSemester,
                              LocalDateTime createdAt) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be 2-100 characters") String fullName,
            @NotBlank(message = "Phone number is required") @Pattern(regexp = AuthDtos.PHONE_PATTERN, message = AuthDtos.PHONE_MESSAGE) String phone,
            @Size(max = 150, message = "Must be at most 150 characters") String college,
            @Size(max = 100, message = "Must be at most 100 characters") String course,
            @Size(max = 100, message = "Must be at most 100 characters") String branch,
            @Size(max = 50, message = "Must be at most 50 characters") String yearSemester,
            @Size(max = 50, message = "Must be at most 50 characters") String studentId,
            @Size(max = 150, message = "Must be at most 150 characters") String institution,
            @Size(max = 100, message = "Must be at most 100 characters") String department,
            @Size(max = 50, message = "Must be at most 50 characters") String employeeId,
            @Size(max = 100, message = "Must be at most 100 characters") String designation,
            @Size(max = AuthDtos.MAX_PHOTO_LENGTH, message = "Profile photo is too large") String profilePhoto,
            Boolean removePhoto) {
    }

    public record StatusRequest(@NotNull(message = "active is required") Boolean active) {
    }

    public record Notification(String type, String title, String message, LocalDateTime time, String link) {
    }

    public record ActivityItem(Long id, String action, String description, String userName, String userEmail,
                               LocalDateTime createdAt) {
    }
}
