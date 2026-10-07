package com.examsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ExamDtos {

    private ExamDtos() {
    }

    /** Used for both create and update of an exam. Total marks are never accepted from the client. */
    public record ExamRequest(
            @NotBlank(message = "Exam title is required") @Size(max = 150, message = "Title must be at most 150 characters") String title,
            @Size(max = 2000, message = "Description must be at most 2000 characters") String description,
            @NotBlank(message = "Category is required") @Size(max = 80, message = "Category must be at most 80 characters") String category,
            @NotNull(message = "Duration is required") @Min(value = 1, message = "Duration must be at least 1 minute") @Max(value = 600, message = "Duration must be at most 600 minutes") Integer durationMinutes,
            @NotNull(message = "Pass marks are required") @Min(value = 0, message = "Pass marks cannot be negative") Integer passMarks,
            LocalDateTime scheduledAt,
            LocalDateTime closesAt) {
    }

    public record OptionRequest(
            @NotBlank(message = "Option text is required") @Size(max = 1000, message = "Option must be at most 1000 characters") String text,
            boolean correct) {
    }

    public record QuestionRequest(
            @NotBlank(message = "Question text is required") @Size(max = 2000, message = "Question must be at most 2000 characters") String text,
            @NotNull(message = "Marks are required") @Min(value = 1, message = "Marks must be at least 1") @Max(value = 100, message = "Marks must be at most 100") Integer marks,
            Integer displayOrder,
            @NotNull(message = "Options are required") @Size(min = 2, max = 6, message = "A question needs 2 to 6 options") @Valid List<OptionRequest> options) {
    }

    public record ReorderRequest(@NotEmpty(message = "Question order is required") List<Long> questionIds) {
    }

    /** "correct" is null (and therefore absent from the JSON) whenever the caller must not see the answer key. */
    public record OptionDto(Long id, String text, Boolean correct) {
    }

    public record QuestionDto(Long id, String text, int marks, int displayOrder, List<OptionDto> options) {
    }

    /** availability is OPEN, UPCOMING or CLOSED for published exams, otherwise null. */
    public record ExamSummary(Long id, String title, String description, String category, Long instructorId,
                              String instructorName, int durationMinutes, int totalMarks, int passMarks,
                              int questionCount, String status, String availability, LocalDateTime scheduledAt,
                              LocalDateTime closesAt, LocalDateTime createdAt, Long attempts, boolean demoData) {
    }

    /** attemptState is NOT_STARTED, IN_PROGRESS or COMPLETED. */
    public record StudentExam(ExamSummary exam, String attemptState, Long attemptId, Boolean passed,
                              Double percentage) {
    }

    public record ExamDetail(ExamSummary exam, List<QuestionDto> questions, boolean editable, boolean hasAttempts) {
    }
}
