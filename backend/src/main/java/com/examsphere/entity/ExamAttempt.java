package com.examsphere.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One student's attempt at one exam. The evaluated result (marks, percentage,
 * pass/fail and the per-category counts) is stored on the same row because it
 * is strictly one-to-one with the attempt.
 */
@Entity
@Table(name = "exam_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_exam_student", columnNames = {"exam_id", "student_id"}),
        indexes = {
                @Index(name = "idx_attempts_student", columnList = "student_id"),
                @Index(name = "idx_attempts_exam", columnList = "exam_id"),
                @Index(name = "idx_attempts_status_deadline", columnList = "status, deadline_at")
        })
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attempts_exam"))
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attempts_student"))
    private User student;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** startedAt + exam duration, fixed by the server when the attempt starts. */
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private boolean submitted;

    @Column(name = "auto_submitted", nullable = false)
    private boolean autoSubmitted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "attempted_count", nullable = false)
    private int attemptedCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private int incorrectCount;

    @Column(name = "unanswered_count", nullable = false)
    private int unansweredCount;

    @Column(name = "total_marks", nullable = false)
    private int totalMarks;

    @Column(name = "marks_obtained", nullable = false)
    private int marksObtained;

    @Column(nullable = false)
    private double percentage;

    /** Null until the attempt has been evaluated. */
    private Boolean passed;

    @Column(name = "demo_data", nullable = false)
    private boolean demoData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ---- getters and setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public boolean isAutoSubmitted() {
        return autoSubmitted;
    }

    public void setAutoSubmitted(boolean autoSubmitted) {
        this.autoSubmitted = autoSubmitted;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getAttemptedCount() {
        return attemptedCount;
    }

    public void setAttemptedCount(int attemptedCount) {
        this.attemptedCount = attemptedCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(int incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public int getUnansweredCount() {
        return unansweredCount;
    }

    public void setUnansweredCount(int unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(int marksObtained) {
        this.marksObtained = marksObtained;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public boolean isDemoData() {
        return demoData;
    }

    public void setDemoData(boolean demoData) {
        this.demoData = demoData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
