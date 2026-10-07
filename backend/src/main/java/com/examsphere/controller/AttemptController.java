package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.AttemptDtos.AnswerRequest;
import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.AttemptDtos.AttemptReview;
import com.examsphere.dto.AttemptDtos.AttemptStart;
import com.examsphere.dto.AttemptDtos.AttemptView;
import com.examsphere.dto.AttemptDtos.SubmitRequest;
import com.examsphere.security.AuthUser;
import com.examsphere.service.AttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Taking an exam. Only students can start, answer and submit; ownership is checked in the service. */
@RestController
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping("/api/exams/{examId}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AttemptStart> start(@AuthenticationPrincipal AuthUser user, @PathVariable Long examId) {
        return ApiResponse.ok("Exam started", attemptService.start(user, examId));
    }

    @GetMapping("/api/attempts/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttemptView> get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(attemptService.getAttempt(user, id));
    }

    @PutMapping("/api/attempts/{id}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Void> saveAnswer(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                        @Valid @RequestBody AnswerRequest request) {
        attemptService.saveAnswer(user, id, request);
        return ApiResponse.message("Answer saved");
    }

    @PostMapping("/api/attempts/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttemptResult> submit(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                             @Valid @RequestBody(required = false) SubmitRequest request) {
        return ApiResponse.ok("Exam submitted successfully.", attemptService.submit(user, id, request));
    }

    /** Result: the owning student, the exam's instructor or an admin. */
    @GetMapping("/api/attempts/{id}/result")
    public ApiResponse<AttemptResult> result(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(attemptService.getResult(user, id));
    }

    /** Answer review with the correct options; refused until the attempt is submitted. */
    @GetMapping("/api/attempts/{id}/answers")
    public ApiResponse<AttemptReview> review(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(attemptService.getReview(user, id));
    }
}
