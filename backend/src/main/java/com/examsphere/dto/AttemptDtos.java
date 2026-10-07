package com.examsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class AttemptDtos {

    private AttemptDtos() {
    }

    public record AnswerRequest(@NotNull(message = "questionId is required") Long questionId,
                                Long selectedOptionId, boolean markedForReview) {
    }

    /** Marks are never part of a submission: the server computes them. */
    public record SubmitRequest(@Valid List<AnswerRequest> answers, boolean autoSubmit) {
    }

    public record AttemptStart(Long attemptId, String status) {
    }

    public record SavedAnswer(Long questionId, Long selectedOptionId, boolean markedForReview) {
    }

    /**
     * Everything the exam screen needs. Questions carry no answer key, and
     * remainingSeconds is computed by the server from the stored start time.
     */
    public record AttemptView(Long attemptId, String status, Long examId, String examTitle, String category,
                              int durationMinutes, int totalMarks, LocalDateTime startedAt, LocalDateTime deadlineAt,
                              long remainingSeconds, List<ExamDtos.QuestionDto> questions, List<SavedAnswer> answers) {
    }

    public record AttemptResult(Long attemptId, Long examId, String examTitle, String category, Long studentId,
                                String studentName, String studentEmail, String status, LocalDateTime startedAt,
                                LocalDateTime submittedAt, boolean autoSubmitted, int totalQuestions, int attempted,
                                int correct, int incorrect, int unanswered, int totalMarks, int marksObtained,
                                int passMarks, double percentage, Boolean passed, boolean demoData) {
    }

    /** outcome is CORRECT, INCORRECT or UNANSWERED. */
    public record ReviewItem(Long questionId, int number, String text, int marks, List<ExamDtos.OptionDto> options,
                             Long selectedOptionId, Long correctOptionId, String outcome, int marksAwarded,
                             boolean markedForReview) {
    }

    public record AttemptReview(AttemptResult result, List<ReviewItem> items) {
    }
}
