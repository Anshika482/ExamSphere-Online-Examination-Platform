package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.PageResponse;
import com.examsphere.dto.ReportDtos.ExamAnalytics;
import com.examsphere.dto.ReportDtos.ExamStat;
import com.examsphere.dto.ReportDtos.InstructorDashboard;
import com.examsphere.security.AuthUser;
import com.examsphere.service.ExamService;
import com.examsphere.service.InstructorService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ROLE_INSTRUCTOR only (enforced by the /api/instructor/** URL rule). There is no endpoint that edits a result. */
@RestController
@RequestMapping("/api/instructor")
public class InstructorController {

    private final InstructorService instructorService;
    private final ExamService examService;

    public InstructorController(InstructorService instructorService, ExamService examService) {
        this.instructorService = instructorService;
        this.examService = examService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<InstructorDashboard> dashboard(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(instructorService.dashboard(user));
    }

    @GetMapping("/exams")
    public ApiResponse<List<ExamSummary>> exams(@AuthenticationPrincipal AuthUser user,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) String status) {
        return ApiResponse.ok(examService.listForInstructor(user, q, status));
    }

    @GetMapping("/exams/{id}/attempts")
    public ApiResponse<List<AttemptResult>> attempts(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(instructorService.attemptsForExam(user, id));
    }

    @GetMapping("/exams/{id}/analytics")
    public ApiResponse<ExamAnalytics> analytics(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(instructorService.analytics(user, id));
    }

    @GetMapping("/analytics")
    public ApiResponse<List<ExamStat>> overview(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(instructorService.overview(user));
    }

    @GetMapping("/results")
    public ApiResponse<PageResponse<AttemptResult>> results(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String student,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(instructorService.results(user, examId, student, status, from, to, page, size));
    }
}
