package com.examsphere.service;

import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.PageResponse;
import com.examsphere.dto.ReportDtos.AdminDashboard;
import com.examsphere.dto.ReportDtos.AdminReports;
import com.examsphere.dto.ReportDtos.ChartPoint;
import com.examsphere.dto.ReportDtos.ExamAnalytics;
import com.examsphere.dto.ReportDtos.ExamStat;
import com.examsphere.dto.ReportDtos.StudentStat;
import com.examsphere.dto.UserDtos.ActivityItem;
import com.examsphere.dto.UserDtos.UserSummary;
import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.AuditAction;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.exception.BadRequestException;
import com.examsphere.exception.ForbiddenOperationException;
import com.examsphere.exception.InvalidExamStateException;
import com.examsphere.exception.ResourceNotFoundException;
import com.examsphere.repository.AuditLogRepository;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.repository.UserRepository;
import com.examsphere.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Administration: users, instructor approval, platform-wide exams, results, reports and activity. */
@Service
public class AdminService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final ExamService examService;
    private final InstructorService instructorService;

    public AdminService(UserRepository userRepository, ExamRepository examRepository,
                        ExamAttemptRepository attemptRepository, AuditLogRepository auditLogRepository,
                        AuditService auditService, EmailService emailService, ExamService examService,
                        InstructorService instructorService) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.examService = examService;
        this.instructorService = instructorService;
    }

    // ------------------------------------------------------------------ dashboard

    @Transactional(readOnly = true)
    public AdminDashboard dashboard() {
        long students = userRepository.countByRole(Role.STUDENT);
        long instructors = userRepository.countByRole(Role.INSTRUCTOR);
        long admins = userRepository.countByRole(Role.ADMIN);
        long pending = userRepository.countByRoleAndApprovalStatus(Role.INSTRUCTOR, ApprovalStatus.PENDING);
        long published = examRepository.countByStatus(ExamStatus.PUBLISHED);
        long drafts = examRepository.countByStatus(ExamStatus.DRAFT);
        long closed = examRepository.countByStatus(ExamStatus.CLOSED);
        long attempts = attemptRepository.countBySubmittedTrue();
        long passed = attemptRepository.countBySubmittedTrueAndPassedTrue();
        Double average = attemptRepository.averagePercentage();

        // Submissions per day for the last 14 days (days without submissions show as zero).
        LocalDate today = LocalDate.now();
        Map<LocalDate, Integer> perDay = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0);
        }
        for (LocalDateTime time : attemptRepository.submissionTimesSince(today.minusDays(13).atStartOfDay())) {
            perDay.computeIfPresent(time.toLocalDate(), (day, count) -> count + 1);
        }
        List<ChartPoint> trend = perDay.entrySet().stream()
                .map(entry -> new ChartPoint(entry.getKey().format(DAY), entry.getValue()))
                .toList();

        List<ActivityItem> activity = auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 8))
                .map(AuditService::toItem).getContent();

        return new AdminDashboard(students, instructors, pending, published + drafts + closed, published, drafts, closed,
                attempts, passed, attempts - passed, ReportSupport.percent(passed, attempts),
                DtoMapper.round2(average == null ? 0 : average),
                List.of(new ChartPoint("Students", students), new ChartPoint("Instructors", instructors),
                        new ChartPoint("Admins", admins)),
                List.of(new ChartPoint("Published", published), new ChartPoint("Draft", drafts),
                        new ChartPoint("Closed", closed)),
                trend, activity);
    }

    // ------------------------------------------------------------------ users

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> users(String q, String role, Boolean active, Boolean verified, String approval,
                                           int page, int size) {
        Role roleFilter = parseEnum(Role.class, role, "role");
        ApprovalStatus approvalFilter = parseEnum(ApprovalStatus.class, approval, "approval");
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (roleFilter != null) {
                predicates.add(cb.equal(root.get("role"), roleFilter));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (verified != null) {
                predicates.add(cb.equal(root.get("emailVerified"), verified));
            }
            if (approvalFilter != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalFilter));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        var pageable = PageRequest.of(Math.max(page, 0), InstructorService.clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(userRepository.findAll(spec, pageable), DtoMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public UserSummary user(Long id) {
        return DtoMapper.toSummary(requireUser(id));
    }

    @Transactional
    public UserSummary setActive(AuthUser admin, Long userId, boolean active) {
        if (admin.id().equals(userId) && !active) {
            throw new ForbiddenOperationException("CANNOT_DEACTIVATE_SELF", "You cannot deactivate your own account.");
        }
        User user = requireUser(userId);
        if (user.isActive() != active) {
            user.setActive(active);
            auditService.record(requireUser(admin.id()), active ? AuditAction.USER_ACTIVATED : AuditAction.USER_DEACTIVATED,
                    (active ? "Activated " : "Deactivated ") + user.getEmail());
        }
        return DtoMapper.toSummary(user);
    }

    // ------------------------------------------------------------------ instructor approval

    @Transactional(readOnly = true)
    public List<UserSummary> pendingInstructors() {
        return userRepository.findByRoleAndApprovalStatusOrderByCreatedAtDesc(Role.INSTRUCTOR, ApprovalStatus.PENDING)
                .stream().map(DtoMapper::toSummary).toList();
    }

    @Transactional
    public UserSummary approveInstructor(AuthUser admin, Long userId) {
        User instructor = requireInstructor(userId);
        if (instructor.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new InvalidExamStateException("ALREADY_APPROVED", "This instructor is already approved.");
        }
        if (!instructor.isEmailVerified()) {
            throw new InvalidExamStateException("EMAIL_NOT_VERIFIED",
                    "This instructor has not verified their email yet, so the account cannot be approved.");
        }
        instructor.setApprovalStatus(ApprovalStatus.APPROVED);
        auditService.record(requireUser(admin.id()), AuditAction.INSTRUCTOR_APPROVED,
                "Approved instructor " + instructor.getEmail());
        emailService.sendInstructorApprovedEmail(instructor);
        return DtoMapper.toSummary(instructor);
    }

    @Transactional
    public UserSummary rejectInstructor(AuthUser admin, Long userId) {
        User instructor = requireInstructor(userId);
        if (instructor.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new InvalidExamStateException("NOT_PENDING", "Only pending applications can be rejected.");
        }
        instructor.setApprovalStatus(ApprovalStatus.REJECTED);
        auditService.record(requireUser(admin.id()), AuditAction.INSTRUCTOR_REJECTED,
                "Rejected instructor " + instructor.getEmail());
        emailService.sendInstructorRejectedEmail(instructor);
        return DtoMapper.toSummary(instructor);
    }

    private User requireInstructor(Long userId) {
        User user = requireUser(userId);
        if (user.getRole() != Role.INSTRUCTOR) {
            throw new BadRequestException("This user is not an instructor.");
        }
        return user;
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    // ------------------------------------------------------------------ exams + results

    @Transactional(readOnly = true)
    public List<ExamSummary> exams(String q, String status, String category, Long instructorId) {
        ExamStatus statusFilter = parseEnum(ExamStatus.class, status, "status");
        Specification<Exam> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + q.trim().toLowerCase() + "%"));
            }
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (instructorId != null) {
                predicates.add(cb.equal(root.get("instructor").get("id"), instructorId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Map<Long, Long> attempts = examService.attemptCounts();
        return examRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(e -> DtoMapper.toSummary(e, attempts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> examFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("categories", examRepository.findAllCategories());
        filters.put("instructors", userRepository
                .findByRoleAndApprovalStatusOrderByCreatedAtDesc(Role.INSTRUCTOR, ApprovalStatus.APPROVED).stream()
                .map(u -> Map.of("id", u.getId(), "name", u.getFullName())).toList());
        return filters;
    }

    @Transactional(readOnly = true)
    public ExamAnalytics examAnalytics(Long examId) {
        return instructorService.buildAnalytics(examService.requireExam(examId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptResult> results(Long examId, String q, String status, LocalDate from, LocalDate to,
                                               int page, int size) {
        var spec = InstructorService.submittedAttempts(null, examId, q, status, from, to);
        var pageable = PageRequest.of(Math.max(page, 0), InstructorService.clampSize(size),
                Sort.by(Sort.Direction.DESC, "submittedAt"));
        return PageResponse.from(attemptRepository.findAll(spec, pageable), DtoMapper::toResult);
    }

    // ------------------------------------------------------------------ reports + activity

    @Transactional(readOnly = true)
    public AdminReports reports() {
        long students = userRepository.countByRole(Role.STUDENT);
        long instructors = userRepository.countByRole(Role.INSTRUCTOR);
        long attempts = attemptRepository.countBySubmittedTrue();
        long passed = attemptRepository.countBySubmittedTrueAndPassedTrue();
        Double average = attemptRepository.averagePercentage();
        List<ExamStat> examReport = ReportSupport.toExamStats(attemptRepository.examStatsForAll());
        List<StudentStat> studentReport = attemptRepository.studentStats(PageRequest.of(0, 100)).stream()
                .map(row -> {
                    long count = ReportSupport.toLong(row[3]);
                    long passCount = ReportSupport.toLong(row[5]);
                    return new StudentStat(ReportSupport.toLong(row[0]), (String) row[1], (String) row[2], count,
                            DtoMapper.round2(ReportSupport.toDouble(row[4])), passCount, count - passCount);
                })
                .toList();
        return new AdminReports(userRepository.count(), students, instructors,
                examRepository.countByStatus(ExamStatus.PUBLISHED), attempts,
                DtoMapper.round2(average == null ? 0 : average), ReportSupport.percent(passed, attempts),
                examReport, studentReport);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityItem> activity(int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), InstructorService.clampSize(size));
        return PageResponse.from(auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable), AuditService::toItem);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown " + field + " filter: " + value);
        }
    }
}
