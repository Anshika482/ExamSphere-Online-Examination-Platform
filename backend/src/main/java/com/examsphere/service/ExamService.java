package com.examsphere.service;

import com.examsphere.dto.ExamDtos.ExamDetail;
import com.examsphere.dto.ExamDtos.ExamRequest;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.ExamDtos.OptionRequest;
import com.examsphere.dto.ExamDtos.QuestionDto;
import com.examsphere.dto.ExamDtos.QuestionRequest;
import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.AuditAction;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Question;
import com.examsphere.entity.QuestionOption;
import com.examsphere.entity.User;
import com.examsphere.exception.BadRequestException;
import com.examsphere.exception.ForbiddenOperationException;
import com.examsphere.exception.InvalidExamStateException;
import com.examsphere.exception.ResourceNotFoundException;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.repository.QuestionRepository;
import com.examsphere.repository.UserRepository;
import com.examsphere.security.AuthUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Exam lifecycle (DRAFT -> PUBLISHED -> CLOSED) and question management.
 * Questions can only change while the exam is a draft, so attempts that already
 * exist can never be corrupted by a later edit.
 */
@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ExamService(ExamRepository examRepository, QuestionRepository questionRepository,
                       ExamAttemptRepository attemptRepository, UserRepository userRepository,
                       AuditService auditService) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ lookups + ownership

    public Exam requireExam(Long id) {
        return examRepository.findWithInstructorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found."));
    }

    /** Instructor A must never manage Instructor B's exam. */
    private Exam requireOwnedExam(Long id, AuthUser auth) {
        Exam exam = requireExam(id);
        if (!exam.getInstructor().getId().equals(auth.id())) {
            throw new ForbiddenOperationException("You can only manage your own exams.");
        }
        return exam;
    }

    private Exam requireOwnedDraft(Long id, AuthUser auth) {
        Exam exam = requireOwnedExam(id, auth);
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new InvalidExamStateException("Only draft exams can be modified. This exam is "
                    + exam.getStatus().name().toLowerCase() + ".");
        }
        return exam;
    }

    // ------------------------------------------------------------------ exam CRUD

    @Transactional
    public ExamDetail create(AuthUser auth, ExamRequest request) {
        User instructor = userRepository.findById(auth.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (instructor.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ForbiddenOperationException("Your instructor account must be approved before creating exams.");
        }
        validateSchedule(request);
        Exam exam = new Exam();
        exam.setInstructor(instructor);
        exam.setStatus(ExamStatus.DRAFT);
        apply(exam, request);
        examRepository.save(exam);
        auditService.record(instructor, AuditAction.EXAM_CREATED, "Exam created: " + exam.getTitle());
        return toDetail(exam, true);
    }

    @Transactional
    public ExamDetail update(AuthUser auth, Long examId, ExamRequest request) {
        Exam exam = requireOwnedDraft(examId, auth);
        validateSchedule(request);
        if (exam.getQuestionCount() > 0 && request.passMarks() > exam.getTotalMarks()) {
            throw new BadRequestException("passMarks", "Pass marks cannot exceed total marks (" + exam.getTotalMarks() + ")");
        }
        apply(exam, request);
        auditService.record(exam.getInstructor(), AuditAction.EXAM_UPDATED, "Exam updated: " + exam.getTitle());
        return toDetail(exam, true);
    }

    @Transactional
    public void delete(AuthUser auth, Long examId) {
        Exam exam = requireOwnedDraft(examId, auth);
        if (attemptRepository.existsByExamId(examId)) {
            throw new InvalidExamStateException("This exam already has attempts and cannot be deleted.");
        }
        auditService.record(exam.getInstructor(), AuditAction.EXAM_DELETED, "Draft exam deleted: " + exam.getTitle());
        examRepository.delete(exam);
    }

    private void apply(Exam exam, ExamRequest request) {
        exam.setTitle(request.title().trim());
        exam.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        exam.setCategory(request.category().trim());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setPassMarks(request.passMarks());
        exam.setScheduledAt(request.scheduledAt());
        exam.setClosesAt(request.closesAt());
    }

    private void validateSchedule(ExamRequest request) {
        if (request.scheduledAt() != null && request.closesAt() != null
                && !request.closesAt().isAfter(request.scheduledAt())) {
            throw new BadRequestException("closesAt", "Closing time must be after the scheduled start");
        }
    }

    // ------------------------------------------------------------------ state transitions

    @Transactional
    public ExamDetail publish(AuthUser auth, Long examId) {
        Exam exam = requireOwnedDraft(examId, auth);
        List<Question> questions = exam.getQuestions();
        if (questions.isEmpty()) {
            throw new InvalidExamStateException("NO_QUESTIONS", "Add at least one question before publishing.");
        }
        int number = 1;
        for (Question question : questions) {
            long correct = question.getOptions().stream().filter(QuestionOption::isCorrect).count();
            if (question.getOptions().size() < 2 || correct != 1 || question.getMarks() < 1) {
                throw new InvalidExamStateException("INVALID_QUESTION",
                        "Question " + number + " is invalid: it needs at least 2 options, exactly one correct answer and marks of 1 or more.");
            }
            number++;
        }
        recalculateTotals(exam);
        if (exam.getDurationMinutes() < 1) {
            throw new InvalidExamStateException("Duration must be at least 1 minute.");
        }
        if (exam.getPassMarks() < 0 || exam.getPassMarks() > exam.getTotalMarks()) {
            throw new InvalidExamStateException("INVALID_PASS_MARKS",
                    "Pass marks must be between 0 and the total marks (" + exam.getTotalMarks() + ").");
        }
        if (exam.getClosesAt() != null && !exam.getClosesAt().isAfter(java.time.LocalDateTime.now())) {
            throw new InvalidExamStateException("The closing time is already in the past. Update the schedule first.");
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        auditService.record(exam.getInstructor(), AuditAction.EXAM_PUBLISHED, "Exam published: " + exam.getTitle());
        return toDetail(exam, true);
    }

    /** Back to draft for corrections, but only while nobody has started the exam. */
    @Transactional
    public ExamDetail unpublish(AuthUser auth, Long examId) {
        Exam exam = requireOwnedExam(examId, auth);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new InvalidExamStateException("Only published exams can be moved back to draft.");
        }
        if (attemptRepository.existsByExamId(examId)) {
            throw new InvalidExamStateException("HAS_ATTEMPTS",
                    "Students have already started this exam, so it can no longer be edited. You can close it instead.");
        }
        exam.setStatus(ExamStatus.DRAFT);
        auditService.record(exam.getInstructor(), AuditAction.EXAM_UNPUBLISHED, "Exam moved back to draft: " + exam.getTitle());
        return toDetail(exam, true);
    }

    /** The owning instructor or an admin can close a published exam; no new attempts afterwards. */
    @Transactional
    public ExamDetail close(AuthUser auth, Long examId) {
        Exam exam = auth.isAdmin() ? requireExam(examId) : requireOwnedExam(examId, auth);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new InvalidExamStateException("Only published exams can be closed.");
        }
        exam.setStatus(ExamStatus.CLOSED);
        User actor = userRepository.findById(auth.id()).orElse(exam.getInstructor());
        auditService.record(actor, AuditAction.EXAM_CLOSED, "Exam closed: " + exam.getTitle());
        return toDetail(exam, true);
    }

    // ------------------------------------------------------------------ reads

    /** Management view including the answer key: owner or admin only. */
    @Transactional(readOnly = true)
    public ExamDetail getForManagement(AuthUser auth, Long examId) {
        Exam exam = auth.isAdmin() ? requireExam(examId) : requireOwnedExam(examId, auth);
        return toDetail(exam, true);
    }

    @Transactional(readOnly = true)
    public List<ExamSummary> listForInstructor(AuthUser auth, String q, String status) {
        Map<Long, Long> attempts = attemptCounts();
        String needle = StringUtils.hasText(q) ? q.trim().toLowerCase() : null;
        return examRepository.findByInstructorIdOrderByCreatedAtDesc(auth.id()).stream()
                .filter(e -> !StringUtils.hasText(status) || e.getStatus().name().equalsIgnoreCase(status))
                .filter(e -> needle == null || e.getTitle().toLowerCase().contains(needle)
                        || e.getCategory().toLowerCase().contains(needle))
                .map(e -> DtoMapper.toSummary(e, attempts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public Map<Long, Long> attemptCounts() {
        return attemptRepository.countAttemptsPerExam().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    private ExamDetail toDetail(Exam exam, boolean includeAnswerKey) {
        boolean hasAttempts = exam.getId() != null && attemptRepository.existsByExamId(exam.getId());
        List<QuestionDto> questions = exam.getQuestions().stream()
                .map(q -> DtoMapper.toQuestion(q, includeAnswerKey))
                .toList();
        return new ExamDetail(DtoMapper.toSummary(exam, null), questions,
                exam.getStatus() == ExamStatus.DRAFT, hasAttempts);
    }

    // ------------------------------------------------------------------ questions

    @Transactional
    public QuestionDto addQuestion(AuthUser auth, Long examId, QuestionRequest request) {
        Exam exam = requireOwnedDraft(examId, auth);
        validateOptions(request.options());
        Question question = new Question();
        question.setText(request.text().trim());
        question.setMarks(request.marks());
        int nextOrder = exam.getQuestions().stream().mapToInt(Question::getDisplayOrder).max().orElse(0) + 1;
        question.setDisplayOrder(request.displayOrder() != null && request.displayOrder() > 0 ? request.displayOrder() : nextOrder);
        applyOptions(question, request.options());
        exam.addQuestion(question);
        questionRepository.save(question);
        recalculateTotals(exam);
        return DtoMapper.toQuestion(question, true);
    }

    @Transactional
    public QuestionDto updateQuestion(AuthUser auth, Long questionId, QuestionRequest request) {
        Question question = requireQuestion(questionId);
        Exam exam = requireOwnedDraft(question.getExam().getId(), auth);
        validateOptions(request.options());
        question.setText(request.text().trim());
        question.setMarks(request.marks());
        if (request.displayOrder() != null && request.displayOrder() > 0) {
            question.setDisplayOrder(request.displayOrder());
        }
        question.getOptions().clear(); // orphanRemoval deletes the old rows
        questionRepository.flush();
        applyOptions(question, request.options());
        questionRepository.saveAndFlush(question);
        recalculateTotals(exam);
        return DtoMapper.toQuestion(question, true);
    }

    @Transactional
    public void deleteQuestion(AuthUser auth, Long questionId) {
        Question question = requireQuestion(questionId);
        Exam exam = requireOwnedDraft(question.getExam().getId(), auth);
        exam.getQuestions().removeIf(q -> q.getId().equals(questionId)); // orphanRemoval deletes it
        recalculateTotals(exam);
        if (exam.getPassMarks() > exam.getTotalMarks()) {
            exam.setPassMarks(exam.getTotalMarks());
        }
    }

    /** Applies a new display order; the list must contain exactly the exam's question ids. */
    @Transactional
    public List<QuestionDto> reorderQuestions(AuthUser auth, Long examId, List<Long> questionIds) {
        Exam exam = requireOwnedDraft(examId, auth);
        Map<Long, Question> byId = exam.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (questionIds.size() != byId.size() || !new HashSet<>(questionIds).equals(byId.keySet())) {
            throw new BadRequestException("The question order does not match this exam's questions.");
        }
        List<Question> ordered = new ArrayList<>();
        int order = 1;
        for (Long id : questionIds) {
            Question question = byId.get(id);
            question.setDisplayOrder(order++);
            ordered.add(question);
        }
        return ordered.stream().map(q -> DtoMapper.toQuestion(q, true)).toList();
    }

    private Question requireQuestion(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Question not found."));
    }

    private void validateOptions(List<OptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new BadRequestException("options", "A question needs at least 2 options");
        }
        long correct = options.stream().filter(OptionRequest::correct).count();
        if (correct != 1) {
            throw new BadRequestException("options", "Exactly one option must be marked as correct");
        }
        long distinct = options.stream().map(o -> o.text().trim().toLowerCase()).distinct().count();
        if (distinct != options.size()) {
            throw new BadRequestException("options", "Options must be different from each other");
        }
    }

    private void applyOptions(Question question, List<OptionRequest> options) {
        int order = 1;
        for (OptionRequest option : options) {
            question.addOption(new QuestionOption(option.text().trim(), option.correct(), order++));
        }
    }

    /** Total marks and question count are derived data, always computed on the server. */
    private void recalculateTotals(Exam exam) {
        exam.setTotalMarks(exam.getQuestions().stream().mapToInt(Question::getMarks).sum());
        exam.setQuestionCount(exam.getQuestions().size());
    }
}
