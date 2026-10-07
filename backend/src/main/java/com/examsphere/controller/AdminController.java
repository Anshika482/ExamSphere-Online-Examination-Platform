package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.PageResponse;
import com.examsphere.dto.ReportDtos.AdminDashboard;
import com.examsphere.dto.ReportDtos.AdminReports;
import com.examsphere.dto.ReportDtos.ExamAnalytics;
import com.examsphere.dto.UserDtos.ActivityItem;
import com.examsphere.dto.UserDtos.StatusRequest;
import com.examsphere.dto.UserDtos.UserSummary;
import com.examsphere.security.AuthUser;
import com.examsphere.service.AdminService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ROLE_ADMIN only (enforced by the /api/admin/** URL rule). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboard> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserSummary>> users(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) String role,
                                                        @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) Boolean verified,
                                                        @RequestParam(required = false) String approval,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.users(q, role, active, verified, approval, page, size));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserSummary> user(@PathVariable Long id) {
        return ApiResponse.ok(adminService.user(id));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<UserSummary> setStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                              @Valid @RequestBody StatusRequest request) {
        UserSummary updated = adminService.setActive(admin, id, request.active());
        return ApiResponse.ok(updated.active() ? "User activated." : "User deactivated.", updated);
    }

    @GetMapping("/instructors/pending")
    public ApiResponse<List<UserSummary>> pendingInstructors() {
        return ApiResponse.ok(adminService.pendingInstructors());
    }

    @PostMapping("/instructors/{id}/approve")
    public ApiResponse<UserSummary> approve(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return ApiResponse.ok("Instructor approved.", adminService.approveInstructor(admin, id));
    }

    @PostMapping("/instructors/{id}/reject")
    public ApiResponse<UserSummary> reject(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id) {
        return ApiResponse.ok("Instructor application rejected.", adminService.rejectInstructor(admin, id));
    }

    @GetMapping("/exams")
    public ApiResponse<List<ExamSummary>> exams(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) Long instructorId) {
        return ApiResponse.ok(adminService.exams(q, status, category, instructorId));
    }

    @GetMapping("/exams/filters")
    public ApiResponse<Map<String, Object>> examFilters() {
        return ApiResponse.ok(adminService.examFilters());
    }

    @GetMapping("/exams/{id}/analytics")
    public ApiResponse<ExamAnalytics> examAnalytics(@PathVariable Long id) {
        return ApiResponse.ok(adminService.examAnalytics(id));
    }

    @GetMapping("/results")
    public ApiResponse<PageResponse<AttemptResult>> results(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.results(examId, q, status, from, to, page, size));
    }

    @GetMapping("/reports")
    public ApiResponse<AdminReports> reports() {
        return ApiResponse.ok(adminService.reports());
    }

    @GetMapping("/activity")
    public ApiResponse<PageResponse<ActivityItem>> activity(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(adminService.activity(page, size));
    }
}
