package com.examsphere.entity;

import jakarta.persistence.*;

/** One answer choice of an MCQ question (the OPTION entity of the design). */
@Entity
@Table(name = "question_options", indexes = @Index(name = "idx_options_question", columnList = "question_id"))
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, foreignKey = @ForeignKey(name = "fk_options_question"))
    private Question question;

    @Column(name = "option_text", nullable = false, length = 1000)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    public QuestionOption() {
    }

    public QuestionOption(String text, boolean correct, int optionOrder) {
        this.text = text;
        this.correct = correct;
        this.optionOrder = optionOrder;
    }

    // ---- getters and setters ----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getOptionOrder() {
        return optionOrder;
    }

    public void setOptionOrder(int optionOrder) {
        this.optionOrder = optionOrder;
    }
}
