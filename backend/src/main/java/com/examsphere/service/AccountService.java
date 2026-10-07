package com.examsphere.service;

import com.examsphere.dto.AuthDtos.ChangePasswordRequest;
import com.examsphere.dto.UserDtos.Notification;
import com.examsphere.dto.UserDtos.UpdateProfileRequest;
import com.examsphere.dto.UserDtos.UserProfile;
import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.AuditAction;
import com.examsphere.entity.ExamAttempt;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.exception.BadRequestException;
import com.examsphere.exception.DuplicateResourceException;
import com.examsphere.exception.ResourceNotFoundException;
import com.examsphere.repository.AuditLogRepository;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.repository.UserRepository;
import com.examsphere.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** The signed-in user's own account: profile, password, notifications, logout. */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountService(UserRepository userRepository, ExamRepository examRepository,
                          ExamAttemptRepository attemptRepository, AuditLogRepository auditLogRepository,
                          PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /** Accepts only small image data URLs (the browser resizes the picture before upload). */
    public static String validatePhoto(String photo) {
        if (!StringUtils.hasText(photo)) {
            return null;
        }
        if (!photo.matches("^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$")) {
            throw new BadRequestException("profilePhoto", "Profile photo must be a PNG, JPEG or WebP image");
        }
        return photo;
    }

    public User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(AuthUser auth) {
        return DtoMapper.toProfile(requireUser(auth.id()));
    }

    /** Email is deliberately not editable: changing it would need a fresh verification round. */
    @Transactional
    public UserProfile updateProfile(AuthUser auth, UpdateProfileRequest request) {
        User user = requireUser(auth.id());
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());

        if (user.getRole() == Role.STUDENT) {
            String studentId = required("studentId", "Student ID", request.studentId());
            if (!studentId.equals(user.getStudentId()) && userRepository.existsByStudentId(studentId)) {
                throw new DuplicateResourceException("studentId", "This student ID is already registered.");
            }
            user.setCollege(required("college", "College", request.college()));
            user.setCourse(required("course", "Course", request.course()));
            user.setBranch(required("branch", "Branch", request.branch()));
            user.setYearSemester(required("yearSemester", "Year / semester", request.yearSemester()));
            user.setStudentId(studentId);
        } else if (user.getRole() == Role.INSTRUCTOR) {
            String employeeId = required("employeeId", "Employee ID", request.employeeId());
            if (!employeeId.equals(user.getEmployeeId()) && userRepository.existsByEmployeeId(employeeId)) {
                throw new DuplicateResourceException("employeeId", "This employee / faculty ID is already registered.");
            }
            user.setInstitution(required("institution", "Institution", request.institution()));
            user.setDepartment(required("department", "Department", request.department()));
            user.setEmployeeId(employeeId);
            user.setDesignation(required("designation", "Designation", request.designation()));
        }

        if (Boolean.TRUE.equals(request.removePhoto())) {
            user.setProfilePhoto(null);
        } else if (StringUtils.hasText(request.profilePhoto())) {
            user.setProfilePhoto(validatePhoto(request.profilePhoto()));
        }
        auditService.record(user, AuditAction.PROFILE_UPDATED, "Profile updated");
        return DtoMapper.toProfile(user);
    }

    private String required(String field, String label, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(field, label + " is required");
        }
        return value.trim();
    }

    @Transactional
    public void changePassword(AuthUser auth, ChangePasswordRequest request) {
        User user = requireUser(auth.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("currentPassword", "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("confirmPassword", "Passwords do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("newPassword", "New password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        auditService.record(user, AuditAction.PASSWORD_CHANGED, "Password changed");
    }

    /** JWTs are stateless, so logout is a client-side action; the server only records it. */
    @Transactional
    public void logout(AuthUser auth) {
        User user = requireUser(auth.id());
        auditService.record(user, AuditAction.LOGOUT, user.getFullName() + " logged out");
    }

    /** Role-specific notifications, built from live data (nothing is stored or faked). */
    @Transactional(readOnly = true)
    public List<Notification> notifications(AuthUser auth) {
        List<Notification> items = new ArrayList<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        if (auth.role() == Role.STUDENT) {
            List<ExamAttempt> attempts = attemptRepository.findByStudentId(auth.id());
            attempts.stream()
                    .filter(a -> !a.isSubmitted())
                    .forEach(a -> items.add(new Notification("warning", "Exam in progress",
                            "You have an unfinished attempt for " + a.getExam().getTitle() + ".",
                            a.getStartedAt(), "#/student/attempt/" + a.getId())));
            attempts.stream()
                    .filter(a -> a.isSubmitted() && a.getSubmittedAt() != null && a.getSubmittedAt().isAfter(weekAgo))
                    .forEach(a -> items.add(new Notification(Boolean.TRUE.equals(a.getPassed()) ? "success" : "danger",
                            "Result available",
                            a.getExam().getTitle() + ": " + a.getMarksObtained() + " / " + a.getTotalMarks(),
                            a.getSubmittedAt(), "#/student/result/" + a.getId())));
            List<Long> attemptedExamIds = attempts.stream().map(a -> a.getExam().getId()).toList();
            examRepository.findAll(publishedSince(weekAgo), Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                    .filter(e -> !attemptedExamIds.contains(e.getId()))
                    .limit(5)
                    .forEach(e -> items.add(new Notification("info", "New exam available",
                            e.getTitle() + " (" + e.getCategory() + ")", e.getUpdatedAt(), "#/student/exam/" + e.getId())));
        } else if (auth.role() == Role.INSTRUCTOR) {
            Specification<ExamAttempt> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("exam").get("instructor").get("id"), auth.id()),
                    cb.isTrue(root.get("submitted")),
                    cb.greaterThanOrEqualTo(root.<LocalDateTime>get("submittedAt"), weekAgo));
            attemptRepository.findAll(spec, PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "submittedAt")))
                    .forEach(a -> items.add(new Notification("info", "New submission",
                            a.getStudent().getFullName() + " submitted " + a.getExam().getTitle() + " ("
                                    + a.getMarksObtained() + " / " + a.getTotalMarks() + ")",
                            a.getSubmittedAt(), "#/instructor/results?examId=" + a.getExam().getId())));
        } else {
            long pending = userRepository.countByRoleAndApprovalStatus(Role.INSTRUCTOR, ApprovalStatus.PENDING);
            if (pending > 0) {
                items.add(new Notification("warning", "Instructor approvals",
                        pending + " instructor application" + (pending == 1 ? " is" : "s are") + " waiting for review.",
                        LocalDateTime.now(), "#/admin/approvals"));
            }
            auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 6))
                    .forEach(entry -> items.add(new Notification("info", readable(entry.getAction().name()),
                            entry.getDescription(), entry.getCreatedAt(), "#/admin/activity")));
        }
        Comparator<Notification> newestFirst = (a, b) -> b.time().compareTo(a.time());
        items.sort(newestFirst);
        return items.stream().limit(10).toList();
    }

    private Specification<com.examsphere.entity.Exam> publishedSince(LocalDateTime since) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ExamStatus.PUBLISHED));
            predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("updatedAt"), since));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String readable(String action) {
        String lower = action.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
