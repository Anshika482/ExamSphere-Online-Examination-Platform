package com.examsphere.service;

import com.examsphere.dto.AttemptDtos.AnswerRequest;
import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.AttemptDtos.AttemptReview;
import com.examsphere.dto.AttemptDtos.AttemptStart;
import com.examsphere.dto.AttemptDtos.AttemptView;
import com.examsphere.dto.AttemptDtos.ReviewItem;
import com.examsphere.dto.AttemptDtos.SavedAnswer;
import com.examsphere.dto.AttemptDtos.SubmitRequest;
import com.examsphere.dto.ExamDtos.OptionDto;
import com.examsphere.dto.ExamDtos.QuestionDto;
import com.examsphere.entity.Answer;
import com.examsphere.entity.AttemptStatus;
import com.examsphere.entity.AuditAction;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamAttempt;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Question;
import com.examsphere.entity.QuestionOption;
import com.examsphere.entity.User;
import com.examsphere.exception.BadRequestException;
import com.examsphere.exception.DuplicateSubmissionException;
import com.examsphere.exception.ForbiddenOperationException;
import com.examsphere.exception.InvalidExamStateException;
import com.examsphere.exception.ResourceNotFoundException;
import com.examsphere.repository.AnswerRepository;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.QuestionRepository;
import com.examsphere.repository.UserRepository;
import com.examsphere.security.AuthUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The timed attempt and its automatic evaluation.
 *
 * Trust model: the server stores startedAt and deadlineAt when the attempt begins.
 * The browser countdown is only a display; whether a submission is on time, and every
 * mark, is decided here. Answers are saved as the student works, so an attempt whose
 * deadline passes is evaluated from the saved answers even if the browser is gone.
 */
@Service
public class AttemptService {

    private static final Logger log = LoggerFactory.getLogger(AttemptService.class);

    private final ExamAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamService examService;
    private final AuditService auditService;

    /** Allowance for network latency when the browser auto-submits exactly at zero. */
    @Value("${app.attempt.grace-seconds:10}")
    private int graceSeconds;

    public AttemptService(ExamAttemptRepository attemptRepository, AnswerRepository answerRepository,
                          QuestionRepository questionRepository, UserRepository userRepository,
                          ExamService examService, AuditService auditService) {
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.examService = examService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ start

    /** Starts the attempt, or returns the unfinished one so a refresh can resume it. One attempt per exam. */
    @Transactional
    public AttemptStart start(AuthUser auth, Long examId) {
        Exam exam = examService.requireExam(examId);
        Optional<ExamAttempt> existing = attemptRepository.findByExamIdAndStudentId(examId, auth.id());
        if (existing.isPresent()) {
            ExamAttempt attempt = existing.get();
            if (attempt.isSubmitted()) {
                throw new InvalidExamStateException("ALREADY_ATTEMPTED", "You have already attempted this exam.");
            }
            return new AttemptStart(attempt.getId(), attempt.getStatus().name());
        }

        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new InvalidExamStateException("EXAM_NOT_AVAILABLE", "This exam is not available.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getScheduledAt() != null && now.isBefore(exam.getScheduledAt())) {
            throw new InvalidExamStateException("EXAM_NOT_STARTED", "This exam has not opened yet.");
        }
        if (exam.getClosesAt() != null && !now.isBefore(exam.getClosesAt())) {
            throw new InvalidExamStateException("EXAM_CLOSED", "This exam is closed.");
        }

        User student = userRepository.findById(auth.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        ExamAttempt attempt = new ExamAttempt();
        attempt.setExam(exam);
        attempt.setStudent(student);
        attempt.setStartedAt(now);
        attempt.setDeadlineAt(now.plusMinutes(exam.getDurationMinutes()));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setTotalQuestions(exam.getQuestionCount());
        attempt.setTotalMarks(exam.getTotalMarks());
        // The unique (exam_id, student_id) constraint rejects a simultaneous second start.
        attemptRepository.saveAndFlush(attempt);
        auditService.record(student, AuditAction.EXAM_STARTED, "Started exam: " + exam.getTitle());
        return new AttemptStart(attempt.getId(), attempt.getStatus().name());
    }

    // ------------------------------------------------------------------ in-progress view

    @Transactional
    public AttemptView getAttempt(AuthUser auth, Long attemptId) {
        ExamAttempt attempt = lockOwnedAttempt(auth, attemptId);
        LocalDateTime now = LocalDateTime.now();
        if (!attempt.isSubmitted() && now.isAfter(attempt.getDeadlineAt())) {
            evaluate(attempt, true); // time ran out while the student was away
        }
        Exam exam = attempt.getExam();
        if (attempt.isSubmitted()) {
            return new AttemptView(attempt.getId(), attempt.getStatus().name(), exam.getId(), exam.getTitle(),
                    exam.getCategory(), exam.getDurationMinutes(), attempt.getTotalMarks(), attempt.getStartedAt(),
                    attempt.getDeadlineAt(), 0, List.of(), List.of());
        }
        // includeAnswerKey = false: the correct option never reaches the browser during the exam.
        List<QuestionDto> questions = questionRepository.findByExamIdOrderByDisplayOrderAscIdAsc(exam.getId()).stream()
                .map(q -> DtoMapper.toQuestion(q, false))
                .toList();
        List<SavedAnswer> saved = answerRepository.findByAttemptId(attemptId).stream()
                .map(a -> new SavedAnswer(a.getQuestion().getId(),
                        a.getSelectedOption() == null ? null : a.getSelectedOption().getId(), a.isMarkedForReview()))
                .toList();
        long remaining = Math.max(0, Duration.between(now, attempt.getDeadlineAt()).getSeconds());
        return new AttemptView(attempt.getId(), attempt.getStatus().name(), exam.getId(), exam.getTitle(),
                exam.getCategory(), exam.getDurationMinutes(), attempt.getTotalMarks(), attempt.getStartedAt(),
                attempt.getDeadlineAt(), remaining, questions, saved);
    }

    /** Saves one answer (or its mark-for-review flag) while the exam is running. */
    @Transactional
    public void saveAnswer(AuthUser auth, Long attemptId, AnswerRequest request) {
        ExamAttempt attempt = lockOwnedAttempt(auth, attemptId);
        if (attempt.isSubmitted()) {
            throw new DuplicateSubmissionException("Exam has already been submitted.");
        }
        if (LocalDateTime.now().isAfter(attempt.getDeadlineAt().plusSeconds(graceSeconds))) {
            throw new InvalidExamStateException("TIME_EXPIRED", "The time for this exam has expired.");
        }
        Map<Long, Question> questions = questionsById(attempt.getExam().getId());
        Map<Long, Answer> answers = answersByQuestion(attemptId);
        applyAnswer(attempt, request, questions, answers);
    }

    // ------------------------------------------------------------------ submit

    /**
     * Manual or automatic submission. The row lock makes this safe against double clicks and
     * parallel requests: the second caller waits, then sees submitted = true and is rejected.
     * Saving the final answers, evaluating and storing the result all happen in one transaction.
     */
    @Transactional
    public AttemptResult submit(AuthUser auth, Long attemptId, SubmitRequest request) {
        ExamAttempt attempt = lockOwnedAttempt(auth, attemptId);
        if (attempt.isSubmitted()) {
            throw new DuplicateSubmissionException("Exam has already been submitted.");
        }
        boolean late = LocalDateTime.now().isAfter(attempt.getDeadlineAt().plusSeconds(graceSeconds));
        if (!late && request != null && request.answers() != null) {
            Map<Long, Question> questions = questionsById(attempt.getExam().getId());
            Map<Long, Answer> answers = answersByQuestion(attemptId);
            for (AnswerRequest answer : request.answers()) {
                applyAnswer(attempt, answer, questions, answers);
            }
            answerRepository.flush();
        }
        // A late request cannot add answers: only what was saved before the deadline counts.
        boolean auto = late || (request != null && request.autoSubmit());
        evaluate(attempt, auto);
        return DtoMapper.toResult(attempt);
    }

    /** Safety net: evaluates attempts whose deadline passed without any request from the browser. */
    @Scheduled(fixedDelayString = "${app.attempt.sweep-interval-ms:60000}", initialDelay = 30000)
    @Transactional
    public void autoSubmitExpiredAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(graceSeconds);
        List<Long> ids = attemptRepository.findIdsByStatusAndDeadlineBefore(AttemptStatus.IN_PROGRESS, cutoff);
        for (Long id : ids) {
            attemptRepository.findByIdForUpdate(id)
                    .filter(attempt -> !attempt.isSubmitted())
                    .ifPresent(attempt -> evaluate(attempt, true));
        }
        if (!ids.isEmpty()) {
            log.info("Auto-submitted {} expired attempt(s)", ids.size());
        }
    }

    // ------------------------------------------------------------------ evaluation

    /**
     * Automatic MCQ evaluation. For every question the selected option is compared with the
     * correct option; the totals, percentage and pass/fail are stored on the attempt.
     */
    private void evaluate(ExamAttempt attempt, boolean autoSubmitted) {
        Exam exam = attempt.getExam();
        List<Question> questions = questionRepository.findByExamIdOrderByDisplayOrderAscIdAsc(exam.getId());
        Map<Long, Answer> answers = answersByQuestion(attempt.getId());

        int attempted = 0;
        int correct = 0;
        int incorrect = 0;
        int totalMarks = 0;
        int marksObtained = 0;
        List<Answer> toSave = new ArrayList<>();

        for (Question question : questions) {
            totalMarks += question.getMarks();
            // Unanswered questions also get a row, which keeps the review and the analytics simple.
            Answer answer = answers.computeIfAbsent(question.getId(), id -> new Answer(attempt, question));
            QuestionOption selected = answer.getSelectedOption();
            boolean isCorrect = selected != null && selected.isCorrect();
            if (selected != null) {
                attempted++;
                if (isCorrect) {
                    correct++;
                    marksObtained += question.getMarks();
                } else {
                    incorrect++;
                }
            }
            answer.setCorrect(isCorrect);
            answer.setMarksAwarded(isCorrect ? question.getMarks() : 0);
            toSave.add(answer);
        }
        answerRepository.saveAll(toSave);

        double percentage = totalMarks == 0 ? 0 : DtoMapper.round2(marksObtained * 100.0 / totalMarks);
        attempt.setTotalQuestions(questions.size());
        attempt.setAttemptedCount(attempted);
        attempt.setCorrectCount(correct);
        attempt.setIncorrectCount(incorrect);
        attempt.setUnansweredCount(questions.size() - attempted);
        attempt.setTotalMarks(totalMarks);
        attempt.setMarksObtained(marksObtained);
        attempt.setPercentage(percentage);
        attempt.setPassed(marksObtained >= exam.getPassMarks());
        attempt.setSubmitted(true);
        attempt.setAutoSubmitted(autoSubmitted);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);
        auditService.record(attempt.getStudent(), AuditAction.EXAM_SUBMITTED,
                (autoSubmitted ? "Auto-submitted exam: " : "Submitted exam: ") + exam.getTitle()
                        + " (" + marksObtained + "/" + totalMarks + ")");
    }

    private void applyAnswer(ExamAttempt attempt, AnswerRequest request, Map<Long, Question> questions,
                             Map<Long, Answer> answers) {
        Question question = questions.get(request.questionId());
        if (question == null) {
            throw new BadRequestException("Question " + request.questionId() + " does not belong to this exam.");
        }
        QuestionOption selected = null;
        if (request.selectedOptionId() != null) {
            selected = question.getOptions().stream()
                    .filter(option -> option.getId().equals(request.selectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("The selected option does not belong to the question."));
        }
        Answer answer = answers.get(question.getId());
        if (answer == null) {
            answer = new Answer(attempt, question);
            answers.put(question.getId(), answer);
        }
        answer.setSelectedOption(selected);
        answer.setMarkedForReview(request.markedForReview());
        answerRepository.save(answer);
    }

    private Map<Long, Question> questionsById(Long examId) {
        Map<Long, Question> map = new HashMap<>();
        for (Question question : questionRepository.findByExamIdOrderByDisplayOrderAscIdAsc(examId)) {
            map.put(question.getId(), question);
        }
        return map;
    }

    private Map<Long, Answer> answersByQuestion(Long attemptId) {
        Map<Long, Answer> map = new HashMap<>();
        for (Answer answer : answerRepository.findByAttemptId(attemptId)) {
            map.put(answer.getQuestion().getId(), answer);
        }
        return map;
    }

    // ------------------------------------------------------------------ result + review

    @Transactional
    public AttemptResult getResult(AuthUser auth, Long attemptId) {
        return DtoMapper.toResult(requireSubmittedAttempt(auth, attemptId));
    }

    /** The answer key is released here and only here: after the attempt has been submitted. */
    @Transactional
    public AttemptReview getReview(AuthUser auth, Long attemptId) {
        ExamAttempt attempt = requireSubmittedAttempt(auth, attemptId);
        Map<Long, Answer> answers = answersByQuestion(attemptId);
        List<ReviewItem> items = new ArrayList<>();
        int number = 1;
        for (Question question : questionRepository.findByExamIdOrderByDisplayOrderAscIdAsc(attempt.getExam().getId())) {
            Answer answer = answers.get(question.getId());
            Long selectedId = answer == null || answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId();
            Long correctId = question.getOptions().stream().filter(QuestionOption::isCorrect)
                    .map(QuestionOption::getId).findFirst().orElse(null);
            String outcome = selectedId == null ? "UNANSWERED" : selectedId.equals(correctId) ? "CORRECT" : "INCORRECT";
            List<OptionDto> options = question.getOptions().stream()
                    .map(o -> new OptionDto(o.getId(), o.getText(), o.isCorrect()))
                    .toList();
            items.add(new ReviewItem(question.getId(), number++, question.getText(), question.getMarks(), options,
                    selectedId, correctId, outcome, answer == null ? 0 : answer.getMarksAwarded(),
                    answer != null && answer.isMarkedForReview()));
        }
        return new AttemptReview(DtoMapper.toResult(attempt), items);
    }

    private ExamAttempt requireSubmittedAttempt(AuthUser auth, Long attemptId) {
        ExamAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found."));
        requireCanView(auth, attempt);
        if (!attempt.isSubmitted()) {
            if (LocalDateTime.now().isAfter(attempt.getDeadlineAt())) {
                evaluate(attempt, true);
            } else {
                throw new InvalidExamStateException("NOT_SUBMITTED", "This exam has not been submitted yet.");
            }
        }
        return attempt;
    }

    /** The student who owns the attempt, the instructor who owns the exam, or an admin. */
    private void requireCanView(AuthUser auth, ExamAttempt attempt) {
        boolean owner = auth.isStudent() && attempt.getStudent().getId().equals(auth.id());
        boolean examOwner = auth.isInstructor() && attempt.getExam().getInstructor().getId().equals(auth.id());
        if (!owner && !examOwner && !auth.isAdmin()) {
            throw new ForbiddenOperationException("You do not have access to this attempt.");
        }
    }

    /** Loads the attempt with a row lock and checks that it belongs to the calling student. */
    private ExamAttempt lockOwnedAttempt(AuthUser auth, Long attemptId) {
        ExamAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found."));
        if (!attempt.getStudent().getId().equals(auth.id())) {
            throw new ForbiddenOperationException("This attempt belongs to another student.");
        }
        return attempt;
    }
}
