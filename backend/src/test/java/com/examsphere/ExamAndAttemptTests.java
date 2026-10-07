package com.examsphere;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examsphere.entity.ExamAttempt;
import com.examsphere.repository.AnswerRepository;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExamAndAttemptTests extends IntegrationTestBase {

    @Autowired
    private ExamRepository examRepository;
    @Autowired
    private ExamAttemptRepository attemptRepository;
    @Autowired
    private AnswerRepository answerRepository;

    // ------------------------------------------------------------------ exam management

    @Test
    void instructorCreatesUpdatesAndDeletesDraftExam() throws Exception {
        String token = tokenFor(instructor());
        long examId = createExam(token, "Draft exam", 0);
        assertThat(examRepository.findById(examId).orElseThrow().getStatus().name()).isEqualTo("DRAFT");

        send(put("/api/exams/" + examId), examRequest("Renamed exam", 0), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.title").value("Renamed exam"));

        send(delete("/api/exams/" + examId), null, token).andExpect(status().isOk());
        assertThat(examRepository.findById(examId)).isEmpty();
    }

    @Test
    void examValidationRejectsBadInput() throws Exception {
        String token = tokenFor(instructor());
        Map<String, Object> body = examRequest("", 0);
        body.put("durationMinutes", 0);
        postJson("/api/exams", body, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.durationMinutes").exists());
    }

    @Test
    void publishWithoutQuestionsIsRejected() throws Exception {
        String token = tokenFor(instructor());
        long examId = createExam(token, "Empty exam", 0);
        postJson("/api/exams/" + examId + "/publish", Map.of(), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_QUESTIONS"));
    }

    @Test
    void questionsDriveTotalMarksAndPublishing() throws Exception {
        String token = tokenFor(instructor());
        long examId = createExam(token, "Marks exam", 5);
        postJson("/api/exams/" + examId + "/questions", questionRequest("Q1", 5, 0), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.options.length()").value(4));
        postJson("/api/exams/" + examId + "/questions", questionRequest("Q2", 3, 2), token)
                .andExpect(status().isCreated());

        postJson("/api/exams/" + examId + "/publish", Map.of(), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.exam.totalMarks").value(8))
                .andExpect(jsonPath("$.data.exam.questionCount").value(2));

        // a published exam is locked
        postJson("/api/exams/" + examId + "/questions", questionRequest("Q3", 1, 0), token)
                .andExpect(status().isConflict());
        send(delete("/api/exams/" + examId), null, token).andExpect(status().isConflict());
    }

    @Test
    void questionNeedsExactlyOneCorrectOption() throws Exception {
        String token = tokenFor(instructor());
        long examId = createExam(token, "Option rules", 0);

        Map<String, Object> noneCorrect = questionRequest("None correct", 5, -1);
        postJson("/api/exams/" + examId + "/questions", noneCorrect, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.options").exists());

        Map<String, Object> twoCorrect = new HashMap<>(questionRequest("Two correct", 5, 0));
        twoCorrect.put("options", List.of(
                Map.of("text", "A", "correct", true), Map.of("text", "B", "correct", true),
                Map.of("text", "C", "correct", false), Map.of("text", "D", "correct", false)));
        postJson("/api/exams/" + examId + "/questions", twoCorrect, token).andExpect(status().isBadRequest());

        Map<String, Object> oneOption = new HashMap<>(questionRequest("One option", 5, 0));
        oneOption.put("options", List.of(Map.of("text", "A", "correct", true)));
        postJson("/api/exams/" + examId + "/questions", oneOption, token).andExpect(status().isBadRequest());

        Map<String, Object> zeroMarks = questionRequest("Zero marks", 0, 0);
        postJson("/api/exams/" + examId + "/questions", zeroMarks, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.marks").exists());
    }

    @Test
    void questionCanBeEditedAndDeletedWhileDraft() throws Exception {
        String token = tokenFor(instructor());
        long examId = createExam(token, "Editable", 0);
        long questionId = body(postJson("/api/exams/" + examId + "/questions", questionRequest("Old", 5, 0), token))
                .get("data").get("id").asLong();

        send(put("/api/questions/" + questionId), questionRequest("New text", 7, 3), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").value("New text"))
                .andExpect(jsonPath("$.data.marks").value(7))
                .andExpect(jsonPath("$.data.options[3].correct").value(true));
        assertThat(examRepository.findById(examId).orElseThrow().getTotalMarks()).isEqualTo(7);

        send(delete("/api/questions/" + questionId), null, token).andExpect(status().isOk());
        assertThat(examRepository.findById(examId).orElseThrow().getQuestionCount()).isZero();
    }

    // ------------------------------------------------------------------ attempts

    @Test
    void studentSeesPublishedExamAndStartsAttemptWithoutAnswerKey() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 3, 5);
        String studentToken = tokenFor(student());

        String list = getAs("/api/student/exams", studentToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("\"id\":" + examId);

        long attemptId = body(postJson("/api/exams/" + examId + "/attempts", Map.of(), studentToken)
                .andExpect(status().isCreated())).get("data").get("attemptId").asLong();

        String view = getAs("/api/attempts/" + attemptId, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.questions.length()").value(3))
                .andExpect(jsonPath("$.data.remainingSeconds").isNumber())
                .andReturn().getResponse().getContentAsString();
        // the answer key must not be present in any form while the exam is running
        assertThat(view).doesNotContain("correct");

        // the review endpoint is closed until submission
        getAs("/api/attempts/" + attemptId + "/answers", studentToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOT_SUBMITTED"));

        // starting again resumes the same attempt instead of creating a second one
        long again = body(postJson("/api/exams/" + examId + "/attempts", Map.of(), studentToken)
                .andExpect(status().isCreated())).get("data").get("attemptId").asLong();
        assertThat(again).isEqualTo(attemptId);
    }

    @Test
    void evaluationCountsCorrectIncorrectAndUnansweredAndPasses() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 4, 10); // 4 x 5 marks, pass at 10
        JsonNode questions = examQuestions(instructorToken, examId);
        String studentToken = tokenFor(student());
        long attemptId = startAttempt(examId, studentToken);

        List<Map<String, Object>> answers = new ArrayList<>();
        answers.add(answer(questions.get(0).get("id").asLong(), optionId(questions.get(0), true)));
        answers.add(answer(questions.get(1).get("id").asLong(), optionId(questions.get(1), true)));
        answers.add(answer(questions.get(2).get("id").asLong(), optionId(questions.get(2), false)));
        // question 4 is left unanswered

        Map<String, Object> submission = new HashMap<>();
        submission.put("answers", answers);
        submission.put("marksObtained", 999); // a tampered client value: must be ignored
        submission.put("passed", true);

        postJson("/api/attempts/" + attemptId + "/submit", submission, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuestions").value(4))
                .andExpect(jsonPath("$.data.attempted").value(3))
                .andExpect(jsonPath("$.data.correct").value(2))
                .andExpect(jsonPath("$.data.incorrect").value(1))
                .andExpect(jsonPath("$.data.unanswered").value(1))
                .andExpect(jsonPath("$.data.totalMarks").value(20))
                .andExpect(jsonPath("$.data.marksObtained").value(10))
                .andExpect(jsonPath("$.data.percentage").value(50.0))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.autoSubmitted").value(false));

        ExamAttempt stored = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(stored.isSubmitted()).isTrue();
        assertThat(stored.getMarksObtained()).isEqualTo(10);
        assertThat(answerRepository.findByAttemptId(attemptId)).hasSize(4);

        // after submission the review shows the answer key
        getAs("/api/attempts/" + attemptId + "/answers", studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[0].outcome").value("CORRECT"))
                .andExpect(jsonPath("$.data.items[2].outcome").value("INCORRECT"))
                .andExpect(jsonPath("$.data.items[3].outcome").value("UNANSWERED"))
                .andExpect(jsonPath("$.data.items[3].correctOptionId").isNumber());

        getAs("/api/student/attempts", studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void scoreBelowPassMarksFails() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 2, 10); // both must be right to pass
        JsonNode questions = examQuestions(instructorToken, examId);
        String studentToken = tokenFor(student());
        long attemptId = startAttempt(examId, studentToken);

        Map<String, Object> submission = Map.of("answers", List.of(
                answer(questions.get(0).get("id").asLong(), optionId(questions.get(0), true)),
                answer(questions.get(1).get("id").asLong(), optionId(questions.get(1), false))));
        postJson("/api/attempts/" + attemptId + "/submit", submission, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marksObtained").value(5))
                .andExpect(jsonPath("$.data.passed").value(false));
    }

    @Test
    void duplicateSubmissionIsRejected() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 1, 0);
        String studentToken = tokenFor(student());
        long attemptId = startAttempt(examId, studentToken);

        postJson("/api/attempts/" + attemptId + "/submit", Map.of("answers", List.of()), studentToken)
                .andExpect(status().isOk());
        postJson("/api/attempts/" + attemptId + "/submit", Map.of("answers", List.of()), studentToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Exam has already been submitted."));

        // and the exam cannot be started a second time
        postJson("/api/exams/" + examId + "/attempts", Map.of(), studentToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ATTEMPTED"));
    }

    @Test
    void lateSubmissionIsAutoSubmittedFromSavedAnswersOnly() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 2, 5);
        JsonNode questions = examQuestions(instructorToken, examId);
        String studentToken = tokenFor(student());
        long attemptId = startAttempt(examId, studentToken);

        // saved in time
        send(put("/api/attempts/" + attemptId + "/answers"),
                answer(questions.get(0).get("id").asLong(), optionId(questions.get(0), true)), studentToken)
                .andExpect(status().isOk());

        // the server-side deadline passes
        ExamAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.setDeadlineAt(LocalDateTime.now().minusMinutes(5));
        attemptRepository.save(attempt);

        // saving after the deadline is refused
        send(put("/api/attempts/" + attemptId + "/answers"),
                answer(questions.get(1).get("id").asLong(), optionId(questions.get(1), true)), studentToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIME_EXPIRED"));

        // a late submit cannot smuggle in new answers, whatever the browser's own timer claims
        Map<String, Object> late = Map.of("answers", List.of(
                answer(questions.get(1).get("id").asLong(), optionId(questions.get(1), true))));
        postJson("/api/attempts/" + attemptId + "/submit", late, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoSubmitted").value(true))
                .andExpect(jsonPath("$.data.correct").value(1))
                .andExpect(jsonPath("$.data.unanswered").value(1))
                .andExpect(jsonPath("$.data.marksObtained").value(5));
    }

    @Test
    void expiredAttemptIsEvaluatedWhenItIsOpenedAgain() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 1, 0);
        String studentToken = tokenFor(student());
        long attemptId = startAttempt(examId, studentToken);

        ExamAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.setDeadlineAt(LocalDateTime.now().minusMinutes(1));
        attemptRepository.save(attempt);

        getAs("/api/attempts/" + attemptId, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        getAs("/api/attempts/" + attemptId + "/result", studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autoSubmitted").value(true));
    }

    @Test
    void studentCannotTouchAnotherStudentsAttempt() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 1, 0);
        long attemptId = startAttempt(examId, tokenFor(student()));
        String otherStudent = tokenFor(student());

        getAs("/api/attempts/" + attemptId, otherStudent).andExpect(status().isForbidden());
        postJson("/api/attempts/" + attemptId + "/submit", Map.of("answers", List.of()), otherStudent)
                .andExpect(status().isForbidden());
        assertThat(attemptRepository.findById(attemptId).orElseThrow().isSubmitted()).isFalse();
    }

    @Test
    void draftExamCannotBeStarted() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = createExam(instructorToken, "Still a draft", 0);
        postJson("/api/exams/" + examId + "/attempts", Map.of(), tokenFor(student()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_AVAILABLE"));
    }

    @Test
    void instructorAnalyticsUseRealAttemptData() throws Exception {
        String instructorToken = tokenFor(instructor());
        long examId = publishedExam(instructorToken, 2, 5);
        JsonNode questions = examQuestions(instructorToken, examId);
        String studentToken = tokenFor(student());
        long attemptId = startAttempt(examId, studentToken);
        postJson("/api/attempts/" + attemptId + "/submit", Map.of("answers", List.of(
                answer(questions.get(0).get("id").asLong(), optionId(questions.get(0), true)),
                answer(questions.get(1).get("id").asLong(), optionId(questions.get(1), false)))), studentToken)
                .andExpect(status().isOk());

        getAs("/api/instructor/exams/" + examId + "/analytics", instructorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.attempts").value(1))
                .andExpect(jsonPath("$.data.summary.averageScore").value(5.0))
                .andExpect(jsonPath("$.data.summary.passRate").value(100.0))
                .andExpect(jsonPath("$.data.questions[0].accuracy").value(100.0))
                .andExpect(jsonPath("$.data.questions[1].accuracy").value(0.0));

        getAs("/api/instructor/exams/" + examId + "/attempts", instructorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        getAs("/api/instructor/dashboard", instructorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAttempts").value(1));
    }

    private long startAttempt(long examId, String studentToken) throws Exception {
        return body(postJson("/api/exams/" + examId + "/attempts", Map.of(), studentToken)
                .andExpect(status().isCreated())).get("data").get("attemptId").asLong();
    }
}
