-- ============================================================
-- ExamSphere - MySQL 8+ schema
--
-- The application can create these tables by itself on first start
-- (spring.jpa.hibernate.ddl-auto=update), so running this script is optional.
-- It documents the exact relational design and lets you create the
-- database by hand:   mysql -u root -p < database/schema.sql
-- Table and column names match the JPA entities one-to-one.
-- ============================================================

CREATE DATABASE IF NOT EXISTS examsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE examsphere;

-- ------------------------------------------------------------
-- USERS: one row per account, role decides which profile columns are used
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    full_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(150) NOT NULL,
    phone                 VARCHAR(20)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,              -- BCrypt, never plaintext
    role                  VARCHAR(20)  NOT NULL,              -- STUDENT | INSTRUCTOR | ADMIN
    college               VARCHAR(150) NULL,                  -- student
    course                VARCHAR(100) NULL,                  -- student
    branch                VARCHAR(100) NULL,                  -- student
    year_semester         VARCHAR(50)  NULL,                  -- student
    student_id            VARCHAR(50)  NULL,                  -- student
    institution           VARCHAR(150) NULL,                  -- instructor
    department            VARCHAR(100) NULL,                  -- instructor
    employee_id           VARCHAR(50)  NULL,                  -- instructor
    designation           VARCHAR(100) NULL,                  -- instructor
    profile_photo         MEDIUMTEXT   NULL,
    email_verified        BIT(1)       NOT NULL DEFAULT b'0',
    email_verified_at     DATETIME(6)  NULL,
    approval_status       VARCHAR(20)  NOT NULL DEFAULT 'NOT_REQUIRED', -- NOT_REQUIRED | PENDING | APPROVED | REJECTED
    active                BIT(1)       NOT NULL DEFAULT b'1',
    demo_account          BIT(1)       NOT NULL DEFAULT b'0',
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME(6)  NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email       UNIQUE (email),
    CONSTRAINT uk_users_student_id  UNIQUE (student_id),
    CONSTRAINT uk_users_employee_id UNIQUE (employee_id),
    INDEX idx_users_role (role),
    INDEX idx_users_approval_status (approval_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- EXAMS: owned by one instructor (users 1 -> many exams)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exams (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    title            VARCHAR(150)  NOT NULL,
    description      VARCHAR(2000) NULL,
    category         VARCHAR(80)   NOT NULL,
    duration_minutes INT           NOT NULL,
    total_marks      INT           NOT NULL DEFAULT 0,       -- derived from the questions
    pass_marks       INT           NOT NULL DEFAULT 0,
    question_count   INT           NOT NULL DEFAULT 0,       -- derived from the questions
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT', -- DRAFT | PUBLISHED | CLOSED
    scheduled_at     DATETIME(6)   NULL,
    closes_at        DATETIME(6)   NULL,
    instructor_id    BIGINT        NOT NULL,
    demo_data        BIT(1)        NOT NULL DEFAULT b'0',
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_exams_instructor FOREIGN KEY (instructor_id) REFERENCES users (id),
    INDEX idx_exams_instructor (instructor_id),
    INDEX idx_exams_status (status),
    INDEX idx_exams_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- QUESTIONS (exam 1 -> many questions)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS questions (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    exam_id       BIGINT        NOT NULL,
    question_text VARCHAR(2000) NOT NULL,
    marks         INT           NOT NULL,
    display_order INT           NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
    INDEX idx_questions_exam (exam_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- QUESTION_OPTIONS: the OPTION entity (question 1 -> many options).
-- Named question_options because OPTION is a reserved word in SQL.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS question_options (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    question_id  BIGINT        NOT NULL,
    option_text  VARCHAR(1000) NOT NULL,
    is_correct   BIT(1)        NOT NULL DEFAULT b'0',
    option_order INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions (id),
    INDEX idx_options_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- EXAM_ATTEMPTS: one attempt per student per exam; also holds the evaluated RESULT
-- (the result is strictly 1:1 with the attempt, so it lives on the same row)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exam_attempts (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    exam_id          BIGINT      NOT NULL,
    student_id       BIGINT      NOT NULL,
    started_at       DATETIME(6) NOT NULL,
    deadline_at      DATETIME(6) NOT NULL,                       -- started_at + duration, fixed by the server
    submitted_at     DATETIME(6) NULL,
    submitted        BIT(1)      NOT NULL DEFAULT b'0',
    auto_submitted   BIT(1)      NOT NULL DEFAULT b'0',
    status           VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS | SUBMITTED
    total_questions  INT         NOT NULL DEFAULT 0,
    attempted_count  INT         NOT NULL DEFAULT 0,
    correct_count    INT         NOT NULL DEFAULT 0,
    incorrect_count  INT         NOT NULL DEFAULT 0,
    unanswered_count INT         NOT NULL DEFAULT 0,
    total_marks      INT         NOT NULL DEFAULT 0,
    marks_obtained   INT         NOT NULL DEFAULT 0,
    percentage       DOUBLE      NOT NULL DEFAULT 0,
    passed           BIT(1)      NULL,                           -- NULL until evaluated
    demo_data        BIT(1)      NOT NULL DEFAULT b'0',
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_attempts_exam    FOREIGN KEY (exam_id)    REFERENCES exams (id),
    CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_attempt_exam_student UNIQUE (exam_id, student_id),
    INDEX idx_attempts_student (student_id),
    INDEX idx_attempts_exam (exam_id),
    INDEX idx_attempts_status_deadline (status, deadline_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- ANSWERS (attempt 1 -> many answers, question 1 -> many answers)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS answers (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id         BIGINT NOT NULL,
    question_id        BIGINT NOT NULL,
    selected_option_id BIGINT NULL,                        -- NULL = unanswered
    marked_for_review  BIT(1) NOT NULL DEFAULT b'0',
    is_correct         BIT(1) NULL,                        -- set by evaluation
    marks_awarded      INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_answers_attempt  FOREIGN KEY (attempt_id)         REFERENCES exam_attempts (id),
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id)        REFERENCES questions (id),
    CONSTRAINT fk_answers_option   FOREIGN KEY (selected_option_id) REFERENCES question_options (id),
    CONSTRAINT uk_answer_attempt_question UNIQUE (attempt_id, question_id),
    INDEX idx_answers_attempt (attempt_id),
    INDEX idx_answers_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Single-use, expiring tokens. Only a SHA-256 hash of the token is stored.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used       BIT(1)      NOT NULL DEFAULT b'0',
    used_at    DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_evt_token_hash UNIQUE (token_hash),
    INDEX idx_evt_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used       BIT(1)      NOT NULL DEFAULT b'0',
    used_at    DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_prt_token_hash UNIQUE (token_hash),
    INDEX idx_prt_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- AUDIT_LOGS: basic activity trail shown to admins
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NULL,
    action      VARCHAR(40)  NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
