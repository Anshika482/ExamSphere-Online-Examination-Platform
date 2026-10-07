-- ============================================================
-- ExamSphere - demo data (optional)
--
-- You normally do NOT need this file: on startup the application's DataSeeder
-- inserts exactly the same demo data automatically (unless APP_SEED_ENABLED=false).
-- Use this script only if you prefer to load the demo data with plain SQL:
--
--     mysql -u root -p < database/schema.sql
--     mysql -u root -p < database/seed.sql
--
-- It is safe to run more than once: users are inserted with INSERT IGNORE and the
-- exams/attempts are only created while the exams table is still empty.
-- All rows are flagged demo_account = 1 / demo_data = 1.
--
-- Demo logins (already email-verified):
--     student@examsphere.demo     /  Student@123
--     instructor@examsphere.demo  /  Instructor@123   (approved)
--     admin@examsphere.demo       /  Admin@123
-- Passwords below are BCrypt hashes of those values; no plaintext is stored.
-- ============================================================

USE examsphere;

-- ---------- users ----------
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('ExamSphere Admin', 'admin@examsphere.demo', '+91 90000 00001', '$2a$10$pw4fYh5V24BlfPkH1Y.WXuYInnMVT2R3.EA7Gf.1ocfhjqRLsIDHe', 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, b'1', NOW(6), 'NOT_REQUIRED', b'1', b'1', 0, NOW(6), NOW(6));
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('Dr. Meera Nair', 'instructor@examsphere.demo', '+91 90000 00002', '$2a$10$3yoJRR5OtvtxYfUZx/WqGek/kZ5h0MWwjUQoHMQeW1mMSZnuUizmK', 'INSTRUCTOR', NULL, NULL, NULL, NULL, NULL, 'ExamSphere Demo Institute of Technology', 'Computer Science & Engineering', 'FAC-DEMO-001', 'Associate Professor', b'1', NOW(6), 'APPROVED', b'1', b'1', 0, NOW(6), NOW(6));
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('Prof. Arjun Rao', 'pending.instructor@examsphere.demo', '+91 90000 00003', '$2a$10$3yoJRR5OtvtxYfUZx/WqGek/kZ5h0MWwjUQoHMQeW1mMSZnuUizmK', 'INSTRUCTOR', NULL, NULL, NULL, NULL, NULL, 'ExamSphere Demo Institute of Technology', 'Computer Science & Engineering', 'FAC-DEMO-002', 'Assistant Professor', b'1', NOW(6), 'PENDING', b'1', b'1', 0, NOW(6), NOW(6));
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('Demo Student', 'student@examsphere.demo', '+91 90000 00010', '$2a$10$rnF/Nu0NWP89e3h8Aia.8u9DwDyOpvdoDPHcH16E1r/Eq/pm6OVeG', 'STUDENT', 'ExamSphere Demo Institute of Technology', 'B.Tech', 'Computer Science & Engineering', '4th Year / 7th Semester', 'STU-DEMO-001', NULL, NULL, NULL, NULL, b'1', NOW(6), 'NOT_REQUIRED', b'1', b'1', 0, NOW(6), NOW(6));
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('Aarav Sharma', 'aarav.demo@examsphere.demo', '+91 90000 00011', '$2a$10$rnF/Nu0NWP89e3h8Aia.8u9DwDyOpvdoDPHcH16E1r/Eq/pm6OVeG', 'STUDENT', 'ExamSphere Demo Institute of Technology', 'B.Tech', 'Computer Science & Engineering', '4th Year / 7th Semester', 'STU-DEMO-002', NULL, NULL, NULL, NULL, b'1', NOW(6), 'NOT_REQUIRED', b'1', b'1', 0, NOW(6), NOW(6));
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('Priya Singh', 'priya.demo@examsphere.demo', '+91 90000 00012', '$2a$10$rnF/Nu0NWP89e3h8Aia.8u9DwDyOpvdoDPHcH16E1r/Eq/pm6OVeG', 'STUDENT', 'ExamSphere Demo Institute of Technology', 'B.Tech', 'Computer Science & Engineering', '4th Year / 7th Semester', 'STU-DEMO-003', NULL, NULL, NULL, NULL, b'1', NOW(6), 'NOT_REQUIRED', b'1', b'1', 0, NOW(6), NOW(6));
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role, college, course, branch, year_semester, student_id, institution, department, employee_id, designation, email_verified, email_verified_at, approval_status, active, demo_account, failed_login_attempts, created_at, updated_at)
VALUES ('Rohan Gupta', 'rohan.demo@examsphere.demo', '+91 90000 00013', '$2a$10$rnF/Nu0NWP89e3h8Aia.8u9DwDyOpvdoDPHcH16E1r/Eq/pm6OVeG', 'STUDENT', 'ExamSphere Demo Institute of Technology', 'B.Tech', 'Computer Science & Engineering', '4th Year / 7th Semester', 'STU-DEMO-004', NULL, NULL, NULL, NULL, b'1', NOW(6), 'NOT_REQUIRED', b'1', b'1', 0, NOW(6), NOW(6));

-- ---------- exams, questions, options and sample attempts ----------
DROP PROCEDURE IF EXISTS seed_examsphere_demo;
DELIMITER //
CREATE PROCEDURE seed_examsphere_demo()
BEGIN
    DECLARE v_instructor BIGINT;
    DECLARE v_exam BIGINT;
    DECLARE v_question BIGINT;
    DECLARE v_student BIGINT;
    DECLARE v_attempt BIGINT;
    DECLARE v_started DATETIME(6);

    IF (SELECT COUNT(*) FROM exams) = 0 THEN
        SELECT id INTO v_instructor FROM users WHERE email = 'instructor@examsphere.demo';

        -- Java Fundamentals
        INSERT INTO exams (title, description, category, duration_minutes, total_marks, pass_marks, question_count, status, instructor_id, demo_data, created_at, updated_at)
        VALUES ('Java Fundamentals', 'Core Java concepts: data types, OOP basics, strings, collections and exception handling.', 'Java', 30, 50, 25, 10, 'PUBLISHED', v_instructor, b'1', NOW(6), NOW(6));
        SET v_exam = LAST_INSERT_ID();
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which keyword is used to inherit a class in Java?', 5, 1, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'implements', b'0', 1),
            (v_question, 'extends', b'1', 2),
            (v_question, 'inherits', b'0', 3),
            (v_question, 'super', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What is the size of an int in Java?', 5, 2, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '8 bits', b'0', 1),
            (v_question, '16 bits', b'0', 2),
            (v_question, '32 bits', b'1', 3),
            (v_question, '64 bits', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which of these is NOT a primitive data type in Java?', 5, 3, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'boolean', b'0', 1),
            (v_question, 'char', b'0', 2),
            (v_question, 'double', b'0', 3),
            (v_question, 'String', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which method is the entry point of a standalone Java application?', 5, 4, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'public static void main(String[] args)', b'1', 1),
            (v_question, 'public void start()', b'0', 2),
            (v_question, 'static void run()', b'0', 3),
            (v_question, 'public static int main()', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which collection does not allow duplicate elements?', 5, 5, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'ArrayList', b'0', 1),
            (v_question, 'LinkedList', b'0', 2),
            (v_question, 'HashSet', b'1', 3),
            (v_question, 'Vector', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What does the ''final'' keyword mean when applied to a variable?', 5, 6, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'It can be changed only once per method', b'0', 1),
            (v_question, 'Its value cannot be reassigned after initialisation', b'1', 2),
            (v_question, 'It is visible in all packages', b'0', 3),
            (v_question, 'It is stored on the heap', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which exception is thrown when dividing an integer by zero?', 5, 7, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'ArithmeticException', b'1', 1),
            (v_question, 'NullPointerException', b'0', 2),
            (v_question, 'NumberFormatException', b'0', 3),
            (v_question, 'IllegalStateException', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which of these compares the content of two String objects?', 5, 8, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '==', b'0', 1),
            (v_question, 'equals()', b'1', 2),
            (v_question, 'compare()', b'0', 3),
            (v_question, 'is()', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which interface must a class implement to be used in a for-each loop?', 5, 9, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Comparable', b'0', 1),
            (v_question, 'Serializable', b'0', 2),
            (v_question, 'Runnable', b'0', 3),
            (v_question, 'Iterable', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What is the default value of an instance variable of type boolean?', 5, 10, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'true', b'0', 1),
            (v_question, 'false', b'1', 2),
            (v_question, 'null', b'0', 3),
            (v_question, '0', b'0', 4);

        -- DBMS Essentials
        INSERT INTO exams (title, description, category, duration_minutes, total_marks, pass_marks, question_count, status, instructor_id, demo_data, created_at, updated_at)
        VALUES ('DBMS Essentials', 'Relational model, keys, normalization, SQL and transactions.', 'DBMS', 20, 50, 25, 10, 'PUBLISHED', v_instructor, b'1', NOW(6), NOW(6));
        SET v_exam = LAST_INSERT_ID();
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which key uniquely identifies each row in a table?', 5, 1, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Primary key', b'1', 1),
            (v_question, 'Foreign key', b'0', 2),
            (v_question, 'Candidate index', b'0', 3),
            (v_question, 'Composite attribute', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which normal form removes partial dependency?', 5, 2, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '1NF', b'0', 1),
            (v_question, '2NF', b'1', 2),
            (v_question, '3NF', b'0', 3),
            (v_question, 'BCNF', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which SQL clause is used to filter groups created by GROUP BY?', 5, 3, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'WHERE', b'0', 1),
            (v_question, 'ORDER BY', b'0', 2),
            (v_question, 'HAVING', b'1', 3),
            (v_question, 'DISTINCT', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What does the ''I'' in ACID stand for?', 5, 4, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Integrity', b'0', 1),
            (v_question, 'Indexing', b'0', 2),
            (v_question, 'Inheritance', b'0', 3),
            (v_question, 'Isolation', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which join returns only the rows that match in both tables?', 5, 5, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'INNER JOIN', b'1', 1),
            (v_question, 'LEFT JOIN', b'0', 2),
            (v_question, 'RIGHT JOIN', b'0', 3),
            (v_question, 'FULL OUTER JOIN', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'A foreign key is used to...', 5, 6, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'speed up full table scans', b'0', 1),
            (v_question, 'enforce a link between two tables', b'1', 2),
            (v_question, 'encrypt a column', b'0', 3),
            (v_question, 'store large binary objects', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which command permanently saves a transaction?', 5, 7, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'SAVEPOINT', b'0', 1),
            (v_question, 'ROLLBACK', b'0', 2),
            (v_question, 'COMMIT', b'1', 3),
            (v_question, 'GRANT', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which of these is a DDL statement?', 5, 8, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'SELECT', b'0', 1),
            (v_question, 'INSERT', b'0', 2),
            (v_question, 'UPDATE', b'0', 3),
            (v_question, 'CREATE TABLE', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'An index on a column mainly improves...', 5, 9, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'the speed of lookups on that column', b'1', 1),
            (v_question, 'the speed of inserts', b'0', 2),
            (v_question, 'the storage size', b'0', 3),
            (v_question, 'data integrity', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which aggregate function returns the number of rows?', 5, 10, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'SUM()', b'0', 1),
            (v_question, 'COUNT()', b'1', 2),
            (v_question, 'AVG()', b'0', 3),
            (v_question, 'MAX()', b'0', 4);

        -- OOP Concepts
        INSERT INTO exams (title, description, category, duration_minutes, total_marks, pass_marks, question_count, status, instructor_id, demo_data, created_at, updated_at)
        VALUES ('OOP Concepts', 'Encapsulation, inheritance, polymorphism and abstraction.', 'OOP', 25, 50, 25, 10, 'PUBLISHED', v_instructor, b'1', NOW(6), NOW(6));
        SET v_exam = LAST_INSERT_ID();
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Wrapping data and the methods that operate on it into a single unit is called...', 5, 1, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Encapsulation', b'1', 1),
            (v_question, 'Polymorphism', b'0', 2),
            (v_question, 'Inheritance', b'0', 3),
            (v_question, 'Abstraction', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Method overloading is an example of...', 5, 2, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Run-time polymorphism', b'0', 1),
            (v_question, 'Compile-time polymorphism', b'1', 2),
            (v_question, 'Encapsulation', b'0', 3),
            (v_question, 'Multiple inheritance', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which concept lets a subclass provide its own version of a superclass method?', 5, 3, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Overloading', b'0', 1),
            (v_question, 'Hiding', b'0', 2),
            (v_question, 'Overriding', b'1', 3),
            (v_question, 'Casting', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'An abstract class...', 5, 4, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'cannot have constructors', b'0', 1),
            (v_question, 'must contain only abstract methods', b'0', 2),
            (v_question, 'can be instantiated directly', b'0', 3),
            (v_question, 'cannot be instantiated directly', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which relationship does inheritance represent?', 5, 5, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'is-a', b'1', 1),
            (v_question, 'has-a', b'0', 2),
            (v_question, 'uses-a', b'0', 3),
            (v_question, 'part-of', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Hiding implementation details and showing only essential features is...', 5, 6, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Encapsulation', b'0', 1),
            (v_question, 'Abstraction', b'1', 2),
            (v_question, 'Inheritance', b'0', 3),
            (v_question, 'Composition', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which access modifier makes a member visible only inside its own class?', 5, 7, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'public', b'0', 1),
            (v_question, 'protected', b'0', 2),
            (v_question, 'private', b'1', 3),
            (v_question, 'default', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'A constructor is...', 5, 8, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'a method that returns the object''s hash', b'0', 1),
            (v_question, 'a static method called by the JVM', b'0', 2),
            (v_question, 'a method that destroys an object', b'0', 3),
            (v_question, 'a special method that initialises a new object', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Composition represents which relationship?', 5, 9, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'is-a', b'0', 1),
            (v_question, 'has-a', b'1', 2),
            (v_question, 'kind-of', b'0', 3),
            (v_question, 'extends', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Calling an overridden method through a superclass reference is resolved at...', 5, 10, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'run time', b'1', 1),
            (v_question, 'compile time', b'0', 2),
            (v_question, 'link time', b'0', 3),
            (v_question, 'load time only', b'0', 4);

        -- Web Development Basics
        INSERT INTO exams (title, description, category, duration_minutes, total_marks, pass_marks, question_count, status, instructor_id, demo_data, created_at, updated_at)
        VALUES ('Web Development Basics', 'HTML, CSS, JavaScript and HTTP fundamentals.', 'Web Development', 20, 40, 20, 10, 'PUBLISHED', v_instructor, b'1', NOW(6), NOW(6));
        SET v_exam = LAST_INSERT_ID();
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which HTML element is used for the largest heading?', 4, 1, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '<h1>', b'1', 1),
            (v_question, '<head>', b'0', 2),
            (v_question, '<h6>', b'0', 3),
            (v_question, '<header>', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which CSS property changes the text colour?', 4, 2, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'font-color', b'0', 1),
            (v_question, 'color', b'1', 2),
            (v_question, 'text-style', b'0', 3),
            (v_question, 'foreground', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which HTTP method is normally used to retrieve data without changing it?', 4, 3, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'POST', b'0', 1),
            (v_question, 'PUT', b'0', 2),
            (v_question, 'GET', b'1', 3),
            (v_question, 'DELETE', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which JavaScript keyword declares a block-scoped variable that cannot be reassigned?', 4, 4, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'var', b'0', 1),
            (v_question, 'let', b'0', 2),
            (v_question, 'static', b'0', 3),
            (v_question, 'const', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What does JSON stand for?', 4, 5, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'JavaScript Object Notation', b'1', 1),
            (v_question, 'Java Source Open Network', b'0', 2),
            (v_question, 'JavaScript Online Node', b'0', 3),
            (v_question, 'Joined Script Object Naming', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which HTTP status code means ''Not Found''?', 4, 6, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '200', b'0', 1),
            (v_question, '404', b'1', 2),
            (v_question, '500', b'0', 3),
            (v_question, '302', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which CSS layout module arranges items in a single row or column?', 4, 7, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Grid only', b'0', 1),
            (v_question, 'Float', b'0', 2),
            (v_question, 'Flexbox', b'1', 3),
            (v_question, 'Table', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which browser API is used to make HTTP requests from JavaScript?', 4, 8, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'DOM API', b'0', 1),
            (v_question, 'Canvas API', b'0', 2),
            (v_question, 'Storage API', b'0', 3),
            (v_question, 'Fetch API', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which attribute links a <label> to an input?', 4, 9, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'for', b'1', 1),
            (v_question, 'name', b'0', 2),
            (v_question, 'link', b'0', 3),
            (v_question, 'target', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which HTTP status code means the request was unauthorised (authentication required)?', 4, 10, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '400', b'0', 1),
            (v_question, '401', b'1', 2),
            (v_question, '403', b'0', 3),
            (v_question, '409', b'0', 4);

        -- General Aptitude
        INSERT INTO exams (title, description, category, duration_minutes, total_marks, pass_marks, question_count, status, instructor_id, demo_data, created_at, updated_at)
        VALUES ('General Aptitude', 'Quantitative and logical reasoning warm-up.', 'General Aptitude', 15, 20, 10, 10, 'PUBLISHED', v_instructor, b'1', NOW(6), NOW(6));
        SET v_exam = LAST_INSERT_ID();
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What is 15% of 200?', 2, 1, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '20', b'0', 1),
            (v_question, '30', b'1', 2),
            (v_question, '35', b'0', 3),
            (v_question, '45', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'If a train travels 120 km in 2 hours, what is its average speed?', 2, 2, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '40 km/h', b'0', 1),
            (v_question, '50 km/h', b'0', 2),
            (v_question, '60 km/h', b'1', 3),
            (v_question, '80 km/h', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What is the next number in the series 2, 4, 8, 16, ...?', 2, 3, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '32', b'1', 1),
            (v_question, '24', b'0', 2),
            (v_question, '20', b'0', 3),
            (v_question, '18', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'The average of 10, 20 and 30 is...', 2, 4, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '15', b'0', 1),
            (v_question, '25', b'0', 2),
            (v_question, '30', b'0', 3),
            (v_question, '20', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'If 3x = 27, then x equals...', 2, 5, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '6', b'0', 1),
            (v_question, '9', b'1', 2),
            (v_question, '12', b'0', 3),
            (v_question, '24', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'A shopkeeper buys an item for 400 and sells it for 500. The profit percentage is...', 2, 6, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '20%', b'0', 1),
            (v_question, '22.5%', b'0', 2),
            (v_question, '25%', b'1', 3),
            (v_question, '30%', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which number is a prime?', 2, 7, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '29', b'1', 1),
            (v_question, '21', b'0', 2),
            (v_question, '27', b'0', 3),
            (v_question, '33', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'How many minutes are there in 2.5 hours?', 2, 8, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '120', b'0', 1),
            (v_question, '130', b'0', 2),
            (v_question, '140', b'0', 3),
            (v_question, '150', b'1', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What is the square root of 144?', 2, 9, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, '11', b'0', 1),
            (v_question, '12', b'1', 2),
            (v_question, '13', b'0', 3),
            (v_question, '14', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'If today is Monday, what day will it be after 10 days?', 2, 10, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Tuesday', b'0', 1),
            (v_question, 'Wednesday', b'0', 2),
            (v_question, 'Thursday', b'1', 3),
            (v_question, 'Friday', b'0', 4);

        -- Data Structures Quiz (Draft)
        INSERT INTO exams (title, description, category, duration_minutes, total_marks, pass_marks, question_count, status, instructor_id, demo_data, created_at, updated_at)
        VALUES ('Data Structures Quiz (Draft)', 'Work-in-progress quiz on arrays, stacks and queues. Add more questions, then publish.', 'Data Structures', 15, 15, 5, 3, 'DRAFT', v_instructor, b'1', NOW(6), NOW(6));
        SET v_exam = LAST_INSERT_ID();
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which data structure works on the LIFO principle?', 5, 1, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Stack', b'1', 1),
            (v_question, 'Queue', b'0', 2),
            (v_question, 'Linked list', b'0', 3),
            (v_question, 'Tree', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'Which data structure works on the FIFO principle?', 5, 2, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'Stack', b'0', 1),
            (v_question, 'Queue', b'1', 2),
            (v_question, 'Heap', b'0', 3),
            (v_question, 'Graph', b'0', 4);
        INSERT INTO questions (exam_id, question_text, marks, display_order, created_at, updated_at) VALUES (v_exam, 'What is the time complexity of binary search on a sorted array?', 5, 3, NOW(6), NOW(6));
        SET v_question = LAST_INSERT_ID();
        INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES
            (v_question, 'O(n)', b'0', 1),
            (v_question, 'O(n log n)', b'0', 2),
            (v_question, 'O(log n)', b'1', 3),
            (v_question, 'O(1)', b'0', 4);

        -- sample evaluated attempts (the demo student has none on Java, DBMS and OOP, so those can be taken live)
        SELECT id INTO v_student FROM users WHERE email = 'student@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'Web Development Basics' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 6 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 20 MINUTE, v_started + INTERVAL 15 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 8, 1, 1, 40, 32, 80.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'student@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'General Aptitude' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 3 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 15 MINUTE, v_started + INTERVAL 10 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 4, 5, 1, 20, 8, 40.0, b'0', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'aarav.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'Java Fundamentals' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 9 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 30 MINUTE, v_started + INTERVAL 25 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 9, 0, 1, 50, 45, 90.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'aarav.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'DBMS Essentials' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 8 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 20 MINUTE, v_started + INTERVAL 15 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 7, 2, 1, 50, 35, 70.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'aarav.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'OOP Concepts' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 5 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 25 MINUTE, v_started + INTERVAL 20 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 8, 1, 1, 50, 40, 80.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'aarav.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'Web Development Basics' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 2 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 20 MINUTE, v_started + INTERVAL 15 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 6, 3, 1, 40, 24, 60.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'priya.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'Java Fundamentals' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 7 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 30 MINUTE, v_started + INTERVAL 25 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 6, 3, 1, 50, 30, 60.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'priya.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'DBMS Essentials' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 6 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 20 MINUTE, v_started + INTERVAL 15 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 4, 5, 1, 50, 20, 40.0, b'0', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'priya.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'General Aptitude' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 4 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 15 MINUTE, v_started + INTERVAL 10 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 9, 0, 1, 20, 18, 90.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'priya.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'OOP Concepts' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 1 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 25 MINUTE, v_started + INTERVAL 20 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 5, 4, 1, 50, 25, 50.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'rohan.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'Java Fundamentals' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 5 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 30 MINUTE, v_started + INTERVAL 25 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 3, 6, 1, 50, 15, 30.0, b'0', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'rohan.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'Web Development Basics' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 3 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 20 MINUTE, v_started + INTERVAL 15 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 7, 2, 1, 40, 28, 70.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 4 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'rohan.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'General Aptitude' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 1 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 15 MINUTE, v_started + INTERVAL 10 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 6, 3, 1, 20, 12, 60.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 2 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

        SELECT id INTO v_student FROM users WHERE email = 'rohan.demo@examsphere.demo';
        SELECT id INTO v_exam FROM exams WHERE title = 'DBMS Essentials' AND demo_data = b'1' LIMIT 1;
        SET v_started = NOW(6) - INTERVAL 0 DAY - INTERVAL 2 HOUR;
        INSERT INTO exam_attempts (exam_id, student_id, started_at, deadline_at, submitted_at, submitted, auto_submitted, status, total_questions, attempted_count, correct_count, incorrect_count, unanswered_count, total_marks, marks_obtained, percentage, passed, demo_data, created_at)
        VALUES (v_exam, v_student, v_started, v_started + INTERVAL 20 MINUTE, v_started + INTERVAL 15 MINUTE, b'1', b'0', 'SUBMITTED', 10, 9, 8, 1, 1, 50, 40, 80.0, b'1', b'1', NOW(6));
        SET v_attempt = LAST_INSERT_ID();
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 1;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 2;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 3;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 4), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 4;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 5;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 6;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 3), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 7;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, NULL, b'0', b'0', 0 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 8;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 1), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 9;
        INSERT INTO answers (attempt_id, question_id, selected_option_id, marked_for_review, is_correct, marks_awarded)
            SELECT v_attempt, q.id, (SELECT o.id FROM question_options o WHERE o.question_id = q.id AND o.option_order = 2), b'0', b'1', 5 FROM questions q WHERE q.exam_id = v_exam AND q.display_order = 10;

    END IF;
END //
DELIMITER ;

CALL seed_examsphere_demo();
DROP PROCEDURE seed_examsphere_demo;
