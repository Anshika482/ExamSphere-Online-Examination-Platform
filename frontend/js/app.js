/* Route table and start-up. */
(function (ES) {
    "use strict";
    const r = ES.router, p = ES.pages;

    if (localStorage.getItem("examsphere.reduceMotion") === "1") document.documentElement.classList.add("reduce-motion");

    // ---- public ----
    r.add("/", { layout: "public", guestOnly: true, render: p.landing });
    r.add("/login", { layout: "public", guestOnly: true, title: "Login", render: p.landing });
    r.add("/register/student", { layout: "public", guestOnly: true, title: "Student registration", render: p.registerStudent });
    r.add("/register/instructor", { layout: "public", guestOnly: true, title: "Instructor registration", render: p.registerInstructor });
    r.add("/verify-email", { layout: "public", title: "Verify your email", render: p.verifyEmail });
    r.add("/resend-verification", { layout: "public", title: "Resend verification email", render: p.resendVerification });
    r.add("/forgot-password", { layout: "public", title: "Forgot password", render: p.forgotPassword });
    r.add("/reset-password", { layout: "public", title: "Reset password", render: p.resetPassword });

    // ---- student ----
    r.add("/student/dashboard", { layout: "app", role: "STUDENT", title: "Dashboard", render: p.studentDashboard });
    r.add("/student/exams", { layout: "app", role: "STUDENT", title: "Available Exams", render: p.studentExams });
    r.add("/student/exam/:id", { layout: "app", role: "STUDENT", title: "Exam Details", nav: "/student/exams", render: p.studentExam });
    r.add("/student/attempt/:id", { layout: "bare", role: "STUDENT", title: "Exam in progress", render: p.studentAttempt });
    r.add("/student/result/:id", { layout: "app", role: "STUDENT", title: "Result", nav: "/student/results", render: p.studentResult });
    r.add("/student/review/:id", { layout: "app", role: "STUDENT", title: "Answer Review", nav: "/student/results", render: p.studentReview });
    r.add("/student/attempts", { layout: "app", role: "STUDENT", title: "My Attempts", render: p.studentAttempts });
    r.add("/student/results", { layout: "app", role: "STUDENT", title: "My Results", render: p.studentResults });
    r.add("/student/profile", { layout: "app", role: "STUDENT", title: "Profile", render: p.profile });
    r.add("/student/settings", { layout: "app", role: "STUDENT", title: "Settings", render: p.settings });

    // ---- instructor ----
    r.add("/instructor/dashboard", { layout: "app", role: "INSTRUCTOR", title: "Dashboard", render: p.instructorDashboard });
    r.add("/instructor/exams", { layout: "app", role: "INSTRUCTOR", title: "My Exams", render: p.instructorExams });
    r.add("/instructor/exams/new", { layout: "app", role: "INSTRUCTOR", title: "Create Exam", render: p.instructorExamForm });
    r.add("/instructor/exam/:id/edit", { layout: "app", role: "INSTRUCTOR", title: "Edit Exam", nav: "/instructor/exams", render: p.instructorExamForm });
    r.add("/instructor/exam/:id/questions", { layout: "app", role: "INSTRUCTOR", title: "Question Manager", nav: "/instructor/questions", render: p.instructorQuestionManager });
    r.add("/instructor/questions", { layout: "app", role: "INSTRUCTOR", title: "Question Management", render: p.instructorQuestions });
    r.add("/instructor/results", { layout: "app", role: "INSTRUCTOR", title: "Results", render: p.instructorResults });
    r.add("/instructor/analytics", { layout: "app", role: "INSTRUCTOR", title: "Analytics", render: p.instructorAnalytics });
    r.add("/instructor/profile", { layout: "app", role: "INSTRUCTOR", title: "Profile", render: p.profile });
    r.add("/instructor/settings", { layout: "app", role: "INSTRUCTOR", title: "Settings", render: p.settings });

    // ---- admin ----
    r.add("/admin/dashboard", { layout: "app", role: "ADMIN", title: "Admin Dashboard", render: p.adminDashboard });
    r.add("/admin/users", { layout: "app", role: "ADMIN", title: "Users", render: p.adminUsers });
    r.add("/admin/approvals", { layout: "app", role: "ADMIN", title: "Instructor Approvals", render: p.adminApprovals });
    r.add("/admin/exams", { layout: "app", role: "ADMIN", title: "Exams", render: p.adminExams });
    r.add("/admin/exam/:id", { layout: "app", role: "ADMIN", title: "Exam Details", nav: "/admin/exams", render: p.adminExam });
    r.add("/admin/results", { layout: "app", role: "ADMIN", title: "Results", render: p.adminResults });
    r.add("/admin/reports", { layout: "app", role: "ADMIN", title: "Reports", render: p.adminReports });
    r.add("/admin/activity", { layout: "app", role: "ADMIN", title: "Activity", render: p.adminActivity });
    r.add("/admin/profile", { layout: "app", role: "ADMIN", title: "Profile", nav: "/admin/settings", render: p.profile });
    r.add("/admin/settings", { layout: "app", role: "ADMIN", title: "Settings", render: p.settings });

    r.start();
})(window.ES);
