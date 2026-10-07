package com.examsphere.config;

import com.examsphere.entity.Answer;
import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.AttemptStatus;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamAttempt;
import com.examsphere.entity.ExamStatus;
import com.examsphere.entity.Question;
import com.examsphere.entity.QuestionOption;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.repository.AnswerRepository;
import com.examsphere.repository.ExamAttemptRepository;
import com.examsphere.repository.ExamRepository;
import com.examsphere.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the demonstration data once. Every step first checks whether the record
 * already exists, so restarting the application never duplicates anything.
 * All seeded rows are flagged (demo_account / demo_data) to keep them
 * distinguishable from real records. Disable with APP_SEED_ENABLED=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** studentIndex, examIndex, daysAgo, number of correct answers (out of 10). */
    private static final int[][] DEMO_ATTEMPTS = {
            {0, 3, 6, 8}, {0, 4, 3, 4},
            {1, 0, 9, 9}, {1, 1, 8, 7}, {1, 2, 5, 8}, {1, 3, 2, 6},
            {2, 0, 7, 6}, {2, 1, 6, 4}, {2, 4, 4, 9}, {2, 2, 1, 5},
            {3, 0, 5, 3}, {3, 3, 3, 7}, {3, 4, 1, 6}, {3, 1, 0, 8}
    };

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(UserRepository userRepository, ExamRepository examRepository,
                      ExamAttemptRepository attemptRepository, AnswerRepository answerRepository,
                      PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    record SeedQuestion(String text, List<String> options, int answer) {
    }

    record SeedExam(String title, String description, String category, int durationMinutes, int passMarks,
                    int marksPerQuestion, String status, List<SeedQuestion> questions) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedUser("admin@examsphere.demo", "Admin@123", "ExamSphere Admin", "+91 90000 00001", Role.ADMIN,
                ApprovalStatus.NOT_REQUIRED, null, null);
        User instructor = seedUser("instructor@examsphere.demo", "Instructor@123", "Dr. Meera Nair", "+91 90000 00002",
                Role.INSTRUCTOR, ApprovalStatus.APPROVED, "FAC-DEMO-001", "Associate Professor");
        // A verified instructor still waiting for approval, so the approval screen has something to show.
        seedUser("pending.instructor@examsphere.demo", "Instructor@123", "Prof. Arjun Rao", "+91 90000 00003",
                Role.INSTRUCTOR, ApprovalStatus.PENDING, "FAC-DEMO-002", "Assistant Professor");

        List<User> students = new ArrayList<>();
        students.add(seedUser("student@examsphere.demo", "Student@123", "Demo Student", "+91 90000 00010",
                Role.STUDENT, ApprovalStatus.NOT_REQUIRED, "STU-DEMO-001", null));
        students.add(seedUser("aarav.demo@examsphere.demo", "Student@123", "Aarav Sharma", "+91 90000 00011",
                Role.STUDENT, ApprovalStatus.NOT_REQUIRED, "STU-DEMO-002", null));
        students.add(seedUser("priya.demo@examsphere.demo", "Student@123", "Priya Singh", "+91 90000 00012",
                Role.STUDENT, ApprovalStatus.NOT_REQUIRED, "STU-DEMO-003", null));
        students.add(seedUser("rohan.demo@examsphere.demo", "Student@123", "Rohan Gupta", "+91 90000 00013",
                Role.STUDENT, ApprovalStatus.NOT_REQUIRED, "STU-DEMO-004", null));

        if (examRepository.count() == 0) {
            List<Exam> exams = seedExams(instructor);
            seedAttempts(students, exams);
            log.info("Seeded {} demo exams with sample attempts", exams.size());
        }
    }

    private User seedUser(String email, String rawPassword, String name, String phone, Role role,
                          ApprovalStatus approval, String idNumber, String designation) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(name);
            user.setPhone(phone);
            user.setRole(role);
            user.setApprovalStatus(approval);
            user.setEmailVerified(true); // demo accounts skip the email step; normal registrations never do
            user.setEmailVerifiedAt(LocalDateTime.now());
            user.setActive(true);
            user.setDemoAccount(true);
            if (role == Role.STUDENT) {
                user.setCollege("ExamSphere Demo Institute of Technology");
                user.setCourse("B.Tech");
                user.setBranch("Computer Science & Engineering");
                user.setYearSemester("4th Year / 7th Semester");
                user.setStudentId(idNumber);
            } else if (role == Role.INSTRUCTOR) {
                user.setInstitution("ExamSphere Demo Institute of Technology");
                user.setDepartment("Computer Science & Engineering");
                user.setEmployeeId(idNumber);
                user.setDesignation(designation);
            }
            log.info("Seeded demo {} account {}", role, email);
            return userRepository.save(user);
        });
    }

    private List<Exam> seedExams(User instructor) throws Exception {
        List<SeedExam> seedExams;
        try (InputStream in = new ClassPathResource("seed/demo-exams.json").getInputStream()) {
            seedExams = objectMapper.readValue(in, new TypeReference<List<SeedExam>>() {
            });
        }
        List<Exam> exams = new ArrayList<>();
        for (SeedExam seed : seedExams) {
            Exam exam = new Exam();
            exam.setInstructor(instructor);
            exam.setTitle(seed.title());
            exam.setDescription(seed.description());
            exam.setCategory(seed.category());
            exam.setDurationMinutes(seed.durationMinutes());
            exam.setPassMarks(seed.passMarks());
            exam.setStatus(ExamStatus.valueOf(seed.status()));
            exam.setDemoData(true);
            int order = 1;
            for (SeedQuestion seedQuestion : seed.questions()) {
                Question question = new Question();
                question.setText(seedQuestion.text());
                question.setMarks(seed.marksPerQuestion());
                question.setDisplayOrder(order++);
                for (int i = 0; i < seedQuestion.options().size(); i++) {
                    question.addOption(new QuestionOption(seedQuestion.options().get(i), i == seedQuestion.answer(), i + 1));
                }
                exam.addQuestion(question);
            }
            exam.setQuestionCount(seed.questions().size());
            exam.setTotalMarks(seed.questions().size() * seed.marksPerQuestion());
            exams.add(examRepository.save(exam));
        }
        return exams;
    }

    /**
     * Creates already-evaluated sample attempts so dashboards and analytics are not empty.
     * The demo student is deliberately left without attempts on Java, DBMS and OOP,
     * so those exams can be taken live during a demonstration.
     */
    private void seedAttempts(List<User> students, List<Exam> exams) {
        LocalDateTime now = LocalDateTime.now();
        for (int[] plan : DEMO_ATTEMPTS) {
            User student = students.get(plan[0]);
            Exam exam = exams.get(plan[1]);
            int correctTarget = plan[3];
            int seed = plan[0] * 3 + plan[1];

            ExamAttempt attempt = new ExamAttempt();
            attempt.setExam(exam);
            attempt.setStudent(student);
            LocalDateTime startedAt = now.minusDays(plan[2]).minusHours(2);
            attempt.setStartedAt(startedAt);
            attempt.setDeadlineAt(startedAt.plusMinutes(exam.getDurationMinutes()));
            attempt.setSubmittedAt(startedAt.plusMinutes(Math.max(1, exam.getDurationMinutes() - 5)));
            attempt.setSubmitted(true);
            attempt.setStatus(AttemptStatus.SUBMITTED);
            attempt.setDemoData(true);
            attemptRepository.save(attempt);

            int attempted = 0;
            int correct = 0;
            int marks = 0;
            List<Answer> answers = new ArrayList<>();
            List<Question> questions = exam.getQuestions();
            for (int qi = 0; qi < questions.size(); qi++) {
                Question question = questions.get(qi);
                List<QuestionOption> options = question.getOptions();
                int correctIndex = 0;
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i).isCorrect()) {
                        correctIndex = i;
                    }
                }
                int position = (qi * 7 + seed) % 10;
                Answer answer = new Answer(attempt, question);
                if (position < correctTarget) {
                    answer.setSelectedOption(options.get(correctIndex));
                    answer.setCorrect(true);
                    answer.setMarksAwarded(question.getMarks());
                    attempted++;
                    correct++;
                    marks += question.getMarks();
                } else if (position == 9) {
                    answer.setCorrect(false); // left unanswered
                } else {
                    answer.setSelectedOption(options.get((correctIndex + 1) % options.size()));
                    answer.setCorrect(false);
                    attempted++;
                }
                answers.add(answer);
            }
            answerRepository.saveAll(answers);

            attempt.setTotalQuestions(questions.size());
            attempt.setAttemptedCount(attempted);
            attempt.setCorrectCount(correct);
            attempt.setIncorrectCount(attempted - correct);
            attempt.setUnansweredCount(questions.size() - attempted);
            attempt.setTotalMarks(exam.getTotalMarks());
            attempt.setMarksObtained(marks);
            attempt.setPercentage(exam.getTotalMarks() == 0 ? 0 : Math.round(marks * 10000.0 / exam.getTotalMarks()) / 100.0);
            attempt.setPassed(marks >= exam.getPassMarks());
            attemptRepository.save(attempt);
        }
    }
}
