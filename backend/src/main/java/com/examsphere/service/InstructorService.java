package com.examsphere.service;

import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.PageResponse;
import com.examsphere.dto.ReportDtos.ChartPoint;
import com.examsphere.dto.ReportDtos.ExamAnalytics;
import com.examsphere.dto.ReportDtos.ExamStat;
import com.examsphere.dto.ReportDtos.InstructorDashboard;
import com.examsphere.dto.ReportDtos.QuestionStat;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamAttempt;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Question;
import com.examsphere.exception.ForbiddenOperationException;
import com.examsphere.repository.AnswerRepository;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.repository.QuestionRepository;
import com.examsphere.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Instructor dashboard, results of the instructor's own exams and analytics. Read-only by design. */
@Service
public class InstructorService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final ExamService examService;

    public InstructorService(ExamRepository examRepository, ExamAttemptRepository attemptRepository,
                             AnswerRepository answerRepository, QuestionRepository questionRepository,
                             ExamService examService) {
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.examService = examService;
    }

    /**
     * Filter for submitted attempts. instructorId = null means "all instructors" (admin use).
     *
     * @param status PASSED or FAILED
     */
    public static Specification<ExamAttempt> submittedAttempts(Long instructorId, Long examId, String student,
                                                               String status, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("submitted")));
            if (instructorId != null) {
                predicates.add(cb.equal(root.get("exam").get("instructor").get("id"), instructorId));
            }
            if (examId != null) {
                predicates.add(cb.equal(root.get("exam").get("id"), examId));
            }
            if (StringUtils.hasText(student)) {
                String like = "%" + student.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("student").get("fullName")), like),
                        cb.like(cb.lower(root.get("student").get("email")), like),
                        cb.like(cb.lower(root.get("exam").get("title")), like)));
            }
            if ("PASSED".equalsIgnoreCase(status)) {
                predicates.add(cb.isTrue(root.get("passed")));
            } else if ("FAILED".equalsIgnoreCase(status)) {
                predicates.add(cb.isFalse(root.get("passed")));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("submittedAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("submittedAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public InstructorDashboard dashboard(AuthUser auth) {
        List<Exam> exams = examRepository.findByInstructorIdOrderByCreatedAtDesc(auth.id());
        long published = exams.stream().filter(e -> e.getStatus() == ExamStatus.PUBLISHED).count();
        long drafts = exams.stream().filter(e -> e.getStatus() == ExamStatus.DRAFT).count();
        long closed = exams.stream().filter(e -> e.getStatus() == ExamStatus.CLOSED).count();

        List<ExamStat> stats = ReportSupport.toExamStats(attemptRepository.examStatsForInstructor(auth.id()));
        long totalAttempts = stats.stream().mapToLong(ExamStat::attempts).sum();
        long passed = stats.stream().mapToLong(ExamStat::passCount).sum();

        Map<Long, Long> attemptCounts = examService.attemptCounts();
        List<ExamSummary> recentExams = exams.stream().limit(5)
                .map(e -> DtoMapper.toSummary(e, attemptCounts.getOrDefault(e.getId(), 0L)))
                .toList();
        List<AttemptResult> recentAttempts = attemptRepository
                .findAll(submittedAttempts(auth.id(), null, null, null, null, null),
                        PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "submittedAt")))
                .map(DtoMapper::toResult)
                .getContent();

        return new InstructorDashboard(exams.size(), published, drafts, closed, totalAttempts,
                ReportSupport.weightedAveragePercentage(stats), ReportSupport.percent(passed, totalAttempts),
                recentExams, recentAttempts, stats);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptResult> results(AuthUser auth, Long examId, String student, String status,
                                               LocalDate from, LocalDate to, int page, int size) {
        var spec = submittedAttempts(auth.id(), examId, student, status, from, to);
        var pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "submittedAt"));
        return PageResponse.from(attemptRepository.findAll(spec, pageable), DtoMapper::toResult);
    }

    @Transactional(readOnly = true)
    public List<AttemptResult> attemptsForExam(AuthUser auth, Long examId) {
        requireOwnedExam(auth, examId);
        return attemptRepository
                .findAll(submittedAttempts(auth.id(), examId, null, null, null, null), Sort.by(Sort.Direction.DESC, "submittedAt"))
                .stream().map(DtoMapper::toResult).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamStat> overview(AuthUser auth) {
        return ReportSupport.toExamStats(attemptRepository.examStatsForInstructor(auth.id()));
    }

    /** Summary, score distribution and question-wise accuracy for one exam. */
    @Transactional(readOnly = true)
    public ExamAnalytics analytics(AuthUser auth, Long examId) {
        Exam exam = requireOwnedExam(auth, examId);
        return buildAnalytics(exam);
    }

    public ExamAnalytics buildAnalytics(Exam exam) {
        Long examId = exam.getId();
        List<Object[]> rows = attemptRepository.examStatsForExam(examId);
        ExamStat summary = rows.isEmpty()
                ? new ExamStat(examId, exam.getTitle(), 0, 0, 0, 0, 0, 0, 0, 0, 0, exam.getTotalMarks())
                : ReportSupport.toExamStat(rows.get(0));

        Map<Long, Object[]> perQuestion = new HashMap<>();
        for (Object[] row : answerRepository.questionStatsForExam(examId)) {
            perQuestion.put(ReportSupport.toLong(row[0]), row);
        }
        List<QuestionStat> questions = new ArrayList<>();
        int number = 1;
        for (Question question : questionRepository.findByExamIdOrderByDisplayOrderAscIdAsc(examId)) {
            Object[] row = perQuestion.get(question.getId());
            long attempted = row == null ? 0 : ReportSupport.toLong(row[1]);
            long correct = row == null ? 0 : ReportSupport.toLong(row[2]);
            questions.add(new QuestionStat(question.getId(), number++, question.getText(), question.getMarks(),
                    attempted, correct, attempted - correct, ReportSupport.percent(correct, attempted)));
        }

        String[] labels = {"0-20%", "21-40%", "41-60%", "61-80%", "81-100%"};
        int[] buckets = new int[labels.length];
        for (Double percentage : attemptRepository.percentagesForExam(examId)) {
            int index = percentage <= 20 ? 0 : percentage <= 40 ? 1 : percentage <= 60 ? 2 : percentage <= 80 ? 3 : 4;
            buckets[index]++;
        }
        List<ChartPoint> distribution = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            distribution.add(new ChartPoint(labels[i], buckets[i]));
        }
        return new ExamAnalytics(DtoMapper.toSummary(exam, summary.attempts()), summary, questions, distribution);
    }

    private Exam requireOwnedExam(AuthUser auth, Long examId) {
        Exam exam = examService.requireExam(examId);
        if (!exam.getInstructor().getId().equals(auth.id())) {
            throw new ForbiddenOperationException("You can only view results of your own exams.");
        }
        return exam;
    }

    static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
