package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.ExamDtos.ExamDetail;
import com.examsphere.dto.ExamDtos.ExamRequest;
import com.examsphere.dto.ExamDtos.QuestionDto;
import com.examsphere.dto.ExamDtos.QuestionRequest;
import com.examsphere.dto.ExamDtos.ReorderRequest;
import com.examsphere.security.AuthUser;
import com.examsphere.service.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exam and question management. Role checks are declared here with @PreAuthorize;
 * "is this the instructor's own exam?" is checked in ExamService.
 */
@RestController
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping("/api/exams")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExamDetail> create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.ok("Exam created successfully.", examService.create(user, request));
    }

    @GetMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ApiResponse<ExamDetail> get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(examService.getForManagement(user, id));
    }

    @PutMapping("/api/exams/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<ExamDetail> update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                          @Valid @RequestBody ExamRequest request) {
        return ApiResponse.ok("Exam updated successfully.", examService.update(user, id, request));
    }

    @DeleteMapping("/api/exams/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        examService.delete(user, id);
        return ApiResponse.message("Exam deleted successfully.");
    }

    @PostMapping("/api/exams/{id}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<ExamDetail> publish(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok("Exam published successfully.", examService.publish(user, id));
    }

    @PostMapping("/api/exams/{id}/unpublish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<ExamDetail> unpublish(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok("Exam moved back to draft.", examService.unpublish(user, id));
    }

    @PostMapping("/api/exams/{id}/close")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ApiResponse<ExamDetail> close(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok("Exam closed.", examService.close(user, id));
    }

    // ---- questions ----

    @PostMapping("/api/exams/{id}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuestionDto> addQuestion(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok("Question added successfully.", examService.addQuestion(user, id, request));
    }

    @PutMapping("/api/exams/{id}/questions/order")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<List<QuestionDto>> reorder(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                  @Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok("Question order updated.", examService.reorderQuestions(user, id, request.questionIds()));
    }

    @PutMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<QuestionDto> updateQuestion(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                   @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok("Question updated successfully.", examService.updateQuestion(user, id, request));
    }

    @DeleteMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ApiResponse<Void> deleteQuestion(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        examService.deleteQuestion(user, id);
        return ApiResponse.message("Question deleted successfully.");
    }
}
