package com.examsphere.service;

import com.examsphere.dto.AttemptDtos.AttemptResult;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.dto.ExamDtos.OptionDto;
import com.examsphere.dto.ExamDtos.QuestionDto;
import com.examsphere.dto.UserDtos.UserProfile;
import com.examsphere.dto.UserDtos.UserSummary;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamAttempt;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Question;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import java.time.LocalDateTime;

/** Entity to DTO conversion. Entities never leave the service layer. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserProfile toProfile(User u) {
        return new UserProfile(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().name(),
                u.getCollege(), u.getCourse(), u.getBranch(), u.getYearSemester(), u.getStudentId(),
                u.getInstitution(), u.getDepartment(), u.getEmployeeId(), u.getDesignation(),
                u.getProfilePhoto(), u.isEmailVerified(), u.getApprovalStatus().name(), u.isActive(), u.getCreatedAt());
    }

    public static UserSummary toSummary(User u) {
        boolean student = u.getRole() == Role.STUDENT;
        return new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().name(),
                u.isEmailVerified(), u.getEmailVerifiedAt(), u.getApprovalStatus().name(), u.isActive(),
                u.isDemoAccount(), student ? u.getCollege() : u.getInstitution(),
                student ? u.getBranch() : u.getDepartment(), student ? u.getStudentId() : u.getEmployeeId(),
                u.getDesignation(), u.getCourse(), u.getYearSemester(), u.getCreatedAt());
    }

    /** OPEN / UPCOMING / CLOSED for a published exam, taking the schedule window into account. */
    public static String availability(Exam e) {
        if (e.getStatus() == ExamStatus.CLOSED) {
            return "CLOSED";
        }
        if (e.getStatus() != ExamStatus.PUBLISHED) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (e.getScheduledAt() != null && now.isBefore(e.getScheduledAt())) {
            return "UPCOMING";
        }
        if (e.getClosesAt() != null && !now.isBefore(e.getClosesAt())) {
            return "CLOSED";
        }
        return "OPEN";
    }

    public static ExamSummary toSummary(Exam e, Long attempts) {
        User instructor = e.getInstructor();
        return new ExamSummary(e.getId(), e.getTitle(), e.getDescription(), e.getCategory(), instructor.getId(),
                instructor.getFullName(), e.getDurationMinutes(), e.getTotalMarks(), e.getPassMarks(),
                e.getQuestionCount(), e.getStatus().name(), availability(e), e.getScheduledAt(), e.getClosesAt(),
                e.getCreatedAt(), attempts, e.isDemoData());
    }

    /** includeAnswerKey must be false for anything a student can see before submitting. */
    public static QuestionDto toQuestion(Question q, boolean includeAnswerKey) {
        return new QuestionDto(q.getId(), q.getText(), q.getMarks(), q.getDisplayOrder(),
                q.getOptions().stream()
                        .map(o -> new OptionDto(o.getId(), o.getText(), includeAnswerKey ? o.isCorrect() : null))
                        .toList());
    }

    public static AttemptResult toResult(ExamAttempt a) {
        Exam exam = a.getExam();
        User student = a.getStudent();
        return new AttemptResult(a.getId(), exam.getId(), exam.getTitle(), exam.getCategory(), student.getId(),
                student.getFullName(), student.getEmail(), a.getStatus().name(), a.getStartedAt(), a.getSubmittedAt(),
                a.isAutoSubmitted(), a.getTotalQuestions(), a.getAttemptedCount(), a.getCorrectCount(),
                a.getIncorrectCount(), a.getUnansweredCount(), a.getTotalMarks(), a.getMarksObtained(),
                exam.getPassMarks(), a.getPercentage(), a.getPassed(), a.isDemoData());
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
