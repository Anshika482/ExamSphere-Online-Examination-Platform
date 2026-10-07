package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.StudentExam;
import com.examsphere.dto.ReportDtos.StudentDashboard;
import com.examsphere.security.AuthUser;
import com.examsphere.service.StudentService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ROLE_STUDENT only (enforced by the /api/student/** URL rule). */
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StudentDashboard> dashboard(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(studentService.dashboard(user));
    }

    @GetMapping("/exams")
    public ApiResponse<List<StudentExam>> exams(@AuthenticationPrincipal AuthUser user,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String duration,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String sort) {
        return ApiResponse.ok(studentService.listExams(user, q, category, duration, status, sort));
    }

    @GetMapping("/exams/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(studentService.categories());
    }

    @GetMapping("/exams/{id}")
    public ApiResponse<StudentExam> exam(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(studentService.getExam(user, id));
    }

    /** Attempt history; "results" is the same data restricted to what the caller asks for via filters. */
    @GetMapping({"/attempts", "/results"})
    public ApiResponse<List<AttemptResult>> attempts(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(studentService.listAttempts(user, examId, status, from, to, sort));
    }
}
