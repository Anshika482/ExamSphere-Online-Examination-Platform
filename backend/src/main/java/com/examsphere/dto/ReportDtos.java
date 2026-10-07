package com.examsphere.dto;

import java.util.List;

/** Dashboard, analytics and report payloads. Every number is computed from the database. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record ChartPoint(String label, double value) {
    }

    public record ExamStat(Long examId, String title, long attempts, double averageScore, int highestScore,
                           int lowestScore, double averagePercentage, long passCount, long failCount,
                           double passRate, double failRate, int totalMarks) {
    }

    public record QuestionStat(Long questionId, int number, String text, int marks, long timesAttempted,
                               long correctCount, long incorrectCount, double accuracy) {
    }

    public record StudentStat(Long studentId, String name, String email, long attempts, double averagePercentage,
                              long passCount, long failCount) {
    }

    public record StudentDashboard(long availableExams, long examsAttempted, long examsPassed, long examsFailed,
                                   double averagePercentage, List<ChartPoint> scoreTrend,
                                   List<AttemptDtos.AttemptResult> recentAttempts,
                                   List<ExamDtos.StudentExam> availableExamList) {
    }

    public record InstructorDashboard(long totalExams, long publishedExams, long draftExams, long closedExams,
                                      long totalAttempts, double averagePercentage, double passRate,
                                      List<ExamDtos.ExamSummary> recentExams,
                                      List<AttemptDtos.AttemptResult> recentAttempts,
                                      List<ExamStat> examPerformance) {
    }

    public record ExamAnalytics(ExamDtos.ExamSummary exam, ExamStat summary, List<QuestionStat> questions,
                                List<ChartPoint> distribution) {
    }

    public record AdminDashboard(long totalStudents, long totalInstructors, long pendingInstructors, long totalExams,
                                 long publishedExams, long draftExams, long closedExams, long totalAttempts,
                                 long passCount, long failCount, double passRate, double averagePercentage,
                                 List<ChartPoint> userDistribution, List<ChartPoint> examStatus,
                                 List<ChartPoint> attemptTrend, List<UserDtos.ActivityItem> recentActivity) {
    }

    public record AdminReports(long totalUsers, long students, long instructors, long publishedExams,
                               long totalAttempts, double averagePercentage, double passRate,
                               List<ExamStat> examReport, List<StudentStat> studentReport) {
    }
}
