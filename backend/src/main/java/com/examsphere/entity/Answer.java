package com.examsphere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_answer_attempt_question", columnNames = {"attempt_id", "question_id"}),
        indexes = {
                @Index(name = "idx_answers_attempt", columnList = "attempt_id"),
                @Index(name = "idx_answers_question", columnList = "question_id")
        })
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, foreignKey = @ForeignKey(name = "fk_answers_attempt"))
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_answers_question"))
    private Question question;

    /** Null when the question was left unanswered. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id", foreignKey = @ForeignKey(name = "fk_answers_option"))
    private QuestionOption selectedOption;

    @Column(name = "marked_for_review", nullable = false)
    private boolean markedForReview;

    /** Filled in by the evaluation step; null while the attempt is in progress. */
    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "marks_awarded", nullable = false)
    private int marksAwarded;

    public Answer() {
    }

    public Answer(ExamAttempt attempt, Question question) {
        this.attempt = attempt;
        this.question = question;
    }

    // ---- getters and setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public QuestionOption getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(QuestionOption selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isMarkedForReview() {
        return markedForReview;
    }

    public void setMarkedForReview(boolean markedForReview) {
        this.markedForReview = markedForReview;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public int getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(int marksAwarded) {
        this.marksAwarded = marksAwarded;
    }
}
