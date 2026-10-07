package com.examsphere.service;

import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.ExamDtos.StudentExam;
import com.examsphere.dto.ReportDtos.ChartPoint;
import com.examsphere.dto.ReportDtos.StudentDashboard;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamAttempt;
import com.examsphere.entity.ExamStatus;
import com.examsphere.exception.ResourceNotFoundException;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.security.AuthUser;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** What a student sees: the exam catalogue, their dashboard and their attempt history. */
@Service
public class StudentService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;

    public StudentService(ExamRepository examRepository, ExamAttemptRepository attemptRepository) {
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
    }

    /** Search (title, category, instructor) and category filter run in the database. */
    public static Specification<Exam> publishedExams(String q, String category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ExamStatus.PUBLISHED));
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<Object, Object> instructor = root.join("instructor", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("category")), like),
                        cb.like(cb.lower(instructor.get("fullName")), like)));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Sort examSort(String sort) {
        if ("title".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "title");
        }
        if ("duration".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "durationMinutes").and(Sort.by("title"));
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    /**
     * @param duration SHORT (up to 20 min), MEDIUM (21-45) or LONG (over 45)
     * @param status   AVAILABLE, UPCOMING, IN_PROGRESS or COMPLETED
     */
    @Transactional(readOnly = true)
    public List<StudentExam> listExams(AuthUser auth, String q, String category, String duration, String status,
                                       String sort) {
        Map<Long, ExamAttempt> attempts = attemptsByExam(auth.id());
        return examRepository.findAll(publishedExams(q, category), examSort(sort)).stream()
                .map(exam -> toStudentExam(exam, attempts.get(exam.getId())))
                // an exam whose window has ended stays listed only for students who attempted it
                .filter(se -> !"CLOSED".equals(se.exam().availability()) || se.attemptId() != null)
                .filter(se -> matchesDuration(se.exam(), duration))
                .filter(se -> matchesStatus(se, status))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentExam getExam(AuthUser auth, Long examId) {
        Exam exam = examRepository.findWithInstructorById(examId)
                .filter(e -> e.getStatus() != ExamStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found."));
        ExamAttempt attempt = attemptRepository.findByExamIdAndStudentId(examId, auth.id()).orElse(null);
        if (exam.getStatus() == ExamStatus.CLOSED && attempt == null) {
            throw new ResourceNotFoundException("Exam not found.");
        }
        return toStudentExam(exam, attempt);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return examRepository.findPublishedCategories();
    }

    /**
     * @param status PASSED or FAILED
     * @param sort   newest (default), oldest or score
     */
    @Transactional(readOnly = true)
    public List<AttemptResult> listAttempts(AuthUser auth, Long examId, String status, LocalDate from, LocalDate to,
                                            String sort) {
        Comparator<AttemptResult> comparator = Comparator.comparing(AttemptResult::startedAt).reversed();
        if ("oldest".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(AttemptResult::startedAt);
        } else if ("score".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparingDouble(AttemptResult::percentage).reversed();
        }
        return attemptRepository.findByStudentId(auth.id()).stream()
                .filter(a -> examId == null || a.getExam().getId().equals(examId))
                .filter(a -> !StringUtils.hasText(status)
                        || (a.isSubmitted() && Boolean.valueOf("PASSED".equalsIgnoreCase(status)).equals(a.getPassed())))
                .filter(a -> from == null || !a.getStartedAt().toLocalDate().isBefore(from))
                .filter(a -> to == null || !a.getStartedAt().toLocalDate().isAfter(to))
                .map(DtoMapper::toResult)
                .sorted(comparator)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDashboard dashboard(AuthUser auth) {
        Map<Long, ExamAttempt> attempts = attemptsByExam(auth.id());
        List<StudentExam> open = examRepository.findAll(publishedExams(null, null), examSort("newest")).stream()
                .map(exam -> toStudentExam(exam, attempts.get(exam.getId())))
                .filter(se -> !"COMPLETED".equals(se.attemptState()))
                .filter(se -> !"CLOSED".equals(se.exam().availability()))
                .toList();

        List<ExamAttempt> submitted = attempts.values().stream()
                .filter(ExamAttempt::isSubmitted)
                .sorted(Comparator.comparing(ExamAttempt::getSubmittedAt))
                .toList();
        long passed = submitted.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double average = DtoMapper.round2(submitted.stream().mapToDouble(ExamAttempt::getPercentage).average().orElse(0));
        List<ChartPoint> trend = submitted.stream()
                .map(a -> new ChartPoint(a.getExam().getTitle() + " (" + a.getSubmittedAt().format(DAY) + ")", a.getPercentage()))
                .toList();
        List<AttemptResult> recent = new ArrayList<>(submitted.stream().map(DtoMapper::toResult).toList());
        java.util.Collections.reverse(recent);

        return new StudentDashboard(open.size(), submitted.size(), passed, submitted.size() - passed, average, trend,
                recent.stream().limit(5).toList(), open.stream().limit(4).toList());
    }

    private Map<Long, ExamAttempt> attemptsByExam(Long studentId) {
        return attemptRepository.findByStudentId(studentId).stream()
                .collect(Collectors.toMap(a -> a.getExam().getId(), Function.identity(), (first, second) -> first));
    }

    private StudentExam toStudentExam(Exam exam, ExamAttempt attempt) {
        ExamSummary summary = DtoMapper.toSummary(exam, null);
        if (attempt == null) {
            return new StudentExam(summary, "NOT_STARTED", null, null, null);
        }
        if (!attempt.isSubmitted()) {
            return new StudentExam(summary, "IN_PROGRESS", attempt.getId(), null, null);
        }
        return new StudentExam(summary, "COMPLETED", attempt.getId(), attempt.getPassed(), attempt.getPercentage());
    }

    private boolean matchesDuration(ExamSummary exam, String duration) {
        if (!StringUtils.hasText(duration)) {
            return true;
        }
        int minutes = exam.durationMinutes();
        return switch (duration.toUpperCase()) {
            case "SHORT" -> minutes <= 20;
            case "MEDIUM" -> minutes > 20 && minutes <= 45;
            case "LONG" -> minutes > 45;
            default -> true;
        };
    }

    private boolean matchesStatus(StudentExam exam, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return switch (status.toUpperCase()) {
            case "AVAILABLE" -> "OPEN".equals(exam.exam().availability()) && "NOT_STARTED".equals(exam.attemptState());
            case "UPCOMING" -> "UPCOMING".equals(exam.exam().availability());
            case "IN_PROGRESS" -> "IN_PROGRESS".equals(exam.attemptState());
            case "COMPLETED" -> "COMPLETED".equals(exam.attemptState());
            default -> true;
        };
    }
}
