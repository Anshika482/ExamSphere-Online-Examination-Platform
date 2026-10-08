# ExamSphere
## 🚀 Live Demo
**Live Application:** https://examsphere-5vy7.onrender.com

> The live demo is deployed on Render and uses a cloud-hosted MySQL database.

**Smarter Examinations. Brighter Futures.**

ExamSphere is a full-stack online examination platform with three roles: students take timed
multiple-choice exams and get an instant result, instructors build and publish exams and study the
outcomes, and administrators approve instructors and oversee the platform.

- Backend: Java 17+, Spring Boot 3.3 (Web, Data JPA / Hibernate, Security, Validation, Mail), JWT, BCrypt, Maven
- Database: MySQL 8+
- Frontend: HTML5, CSS3 and plain JavaScript (Fetch API), no framework and no build step

> **Build status of this delivery.** The environment this project was written in had no access to
> Maven Central, so the **backend has not been compiled or run yet**. What *was* checked is listed
> under [What has been verified](#what-has-been-verified). Run `mvn test` first (step 4 below); if
> the compiler or a test reports anything, it will be a small fix.

---

## Contents

1. [Quick start](#quick-start)
2. [Demo accounts](#demo-accounts)
3. [Features by role](#features-by-role)
4. [Architecture](#architecture)
5. [Database design](#database-design)
6. [How the main flows work](#how-the-main-flows-work)
7. [Security](#security)
8. [API overview](#api-overview)
9. [Environment variables](#environment-variables)
10. [Email (SMTP) setup](#email-smtp-setup)
11. [Testing](#testing)
12. [What has been verified](#what-has-been-verified)
13. [Troubleshooting](#troubleshooting)
14. [Screenshots](#screenshots)
15. [Known limitations](#known-limitations)
16. [Future improvements](#future-improvements)
17. [Interview talking points](#interview-talking-points)

---

## Quick start

### Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 17 or newer (17 or 21 recommended) | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |

In VS Code, install the recommended extensions when prompted (Extension Pack for Java, Spring Boot Tools).

### 1. Open the project

Extract the ZIP and open the `ExamSphere` folder in VS Code (`File > Open Folder`).

### 2. Create the database

Either let the application do it (the default JDBC URL contains `createDatabaseIfNotExist=true` and
Hibernate creates the tables), or create it yourself:

```sql
CREATE DATABASE examsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Optional, if you want to create the tables and demo data with plain SQL instead:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

### 3. Configure `.env`

```bash
cp .env.example .env        # Windows: copy .env.example .env
```

Edit `.env` and set at least `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` (any long random
string). Leave the `MAIL_*` values empty for now if you only want to use the demo accounts.

### 4. Run the tests, then start the backend

```bash
cd backend
mvn test                # 41 integration tests against in-memory H2, no MySQL needed
mvn spring-boot:run
```

On first start the application creates the tables and seeds the demo data.

### 5. Open the frontend

Spring Boot serves the UI itself:

**http://localhost:8080**

Log in with a [demo account](#demo-accounts).

#### Running the frontend separately (optional)

The `frontend/` folder is static, so any static server works, for example VS Code **Live Server**
or:

```bash
npx serve frontend -l 5500
```

Then add that origin to `.env` so CORS and email links match, and restart the backend:

```
FRONTEND_URL=http://localhost:5500
```

When the page is not served from port 8080, `frontend/js/config.js` sends API calls to
`http://localhost:8080`. Change `API_BASE` there if your backend runs elsewhere. Opening
`index.html` directly from disk (`file://`) is not supported because browsers block API calls from it.

---

## Demo accounts

Seeded automatically and already email-verified, so a demo never has to wait for an email.

| Role | Email | Password |
|---|---|---|
| Student | `student@examsphere.demo` | `Student@123` |
| Instructor (approved) | `instructor@examsphere.demo` | `Instructor@123` |
| Admin | `admin@examsphere.demo` | `Admin@123` |

Also seeded: three more demo students with evaluated attempts (so charts are not empty), one
verified instructor who is still **pending approval** (`pending.instructor@examsphere.demo`, for
the approval screen), five published exams (Java, DBMS, OOP, Web Development, General Aptitude,
10 questions each) and one draft exam. The demo student has no attempt on Java, DBMS or OOP, so
those can be taken live.

Seeded rows carry `demo_account = 1` / `demo_data = 1` and are labelled "Demo" in the admin
screens. Normal registrations never skip email verification. For a real deployment set
`APP_SEED_ENABLED=false`, or change these passwords immediately. There is no public admin
registration: the admin exists only through seeding.

### Suggested demo script

1. **Student** – log in → Dashboard → Available Exams → Java Fundamentals → Start Exam → answer,
   mark a question for review, jump around with the palette → Submit → result → Review Answers →
   My Attempts.
2. **Instructor** – log in → Create Exam → add questions → Publish → Results → Analytics.
3. **Admin** – log in → Dashboard → Instructor Approvals (approve the pending instructor) → Users →
   Exams → Reports → Activity.
4. **Registration** – register a student → verification email → verify → log in. Register an
   instructor → verify → log in is refused until the admin approves. This needs SMTP, see
   [Email setup](#email-smtp-setup).

---

## Features by role

**Student** – register, verify email, log in, forgot / reset password, profile, browse / search /
filter / sort exams, exam details and instructions, timed attempt with question palette, mark for
review, change and clear answers, answers saved as you go, manual submit with a summary,
automatic submit when time runs out, instant result with pass / fail, answer review after
submission, attempt history with filters, performance dashboard.

**Instructor** – register, verify email, wait for approval, dashboard, create / edit / delete draft
exams, add / edit / delete / reorder questions (2–6 options, exactly one correct), publish, schedule
(start and closing time), move back to draft while nobody has started, close, view attempts and
results with filters, analytics: attempts, average / highest / lowest score, pass and fail rate,
score distribution, question-wise accuracy.

**Admin** – dashboard with platform totals and charts, user list with search and filters, activate /
deactivate users (not yourself), approve / reject instructors (emails sent), all exams with details
and answer key, close an exam, all results, platform / exam / student reports, activity log.

---

## Architecture

```
Browser (HTML + CSS + JavaScript, Fetch API)
        │  JSON over HTTP, "Authorization: Bearer <JWT>"
        ▼
Spring Security filter chain  ── JwtAuthenticationFilter (who are you?) + URL role rules
        ▼
Controller   (HTTP ↔ DTO, @Valid, @PreAuthorize)        com.examsphere.controller
        ▼
Service      (business rules, ownership checks, @Transactional)   com.examsphere.service
        ▼
Repository   (Spring Data JPA, JPQL, Specifications)    com.examsphere.repository
        ▼
MySQL
```

```
ExamSphere/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/examsphere/
│       │   ├── config/        SecurityConfig, DataSeeder
│       │   ├── controller/    Auth, Account, PublicExam, Student, Attempt, Exam, Instructor, Admin
│       │   ├── dto/           request / response records (entities are never returned)
│       │   ├── entity/        JPA entities and enums
│       │   ├── exception/     ApiException hierarchy + GlobalExceptionHandler
│       │   ├── repository/    Spring Data repositories
│       │   ├── security/      JwtService, JwtAuthenticationFilter, AuthUser
│       │   ├── service/       Auth, Account, Exam, Attempt, Student, Instructor, Admin, Email, Audit
│       │   ├── util/          TokenUtil (random tokens, SHA-256)
│       │   └── validation/    @StrongPassword custom constraint
│       ├── main/resources/    application.properties, seed/demo-exams.json
│       └── test/              integration tests (H2)
├── frontend/
│   ├── index.html             single page, hash routes such as #/student/dashboard
│   ├── css/                   tokens (design variables), components, layout, pages
│   ├── js/                    config, core (API + router), ui (components + charts), pages per role
│   └── assets/
├── database/                  schema.sql, seed.sql
├── docs/API.md                full endpoint reference
├── .env.example
└── README.md
```

Design decisions worth knowing:

- **DTOs everywhere.** Controllers accept and return Java records. Entities stay inside the service
  layer, which avoids lazy-loading errors, JSON recursion and leaking fields such as `passwordHash`.
- **`spring.jpa.open-in-view=false`.** All database access happens inside `@Transactional` service
  methods; list screens use entity graphs / fetch joins and a batch size to avoid N+1 queries.
- **One page, hash routing.** The frontend is static files, so it can be served by Spring Boot or by
  any static server, and deep links such as `/#/verify-email?token=…` work without server rewrites.

---

## Database design

Ten tables, all with a surrogate `BIGINT AUTO_INCREMENT` primary key. Full DDL is in
[`database/schema.sql`](database/schema.sql).

```
users 1 ──< exams 1 ──< questions 1 ──< question_options
  │            │             │                  │
  │            └──< exam_attempts >── users     │
  │                      │   (student)          │
  │                      └──< answers >─────────┘ (selected_option_id, question_id)
  ├──< email_verification_tokens
  ├──< password_reset_tokens
  └──< audit_logs
```

| Table | Purpose | Notable constraints |
|---|---|---|
| `users` | Accounts of all three roles | `UNIQUE(email)`, `UNIQUE(student_id)`, `UNIQUE(employee_id)`, index on `role` |
| `exams` | Exam owned by an instructor | FK `instructor_id → users`, indexes on `instructor_id`, `status` |
| `questions` | MCQ question | FK `exam_id → exams` |
| `question_options` | Answer choices (`is_correct`) | FK `question_id → questions` |
| `exam_attempts` | One attempt per student per exam, plus its result | FKs to `exams` and `users`, `UNIQUE(exam_id, student_id)` |
| `answers` | Selected option per question | FKs, `UNIQUE(attempt_id, question_id)` |
| `email_verification_tokens`, `password_reset_tokens` | Single-use expiring tokens | `UNIQUE(token_hash)` (SHA-256 of the token) |
| `audit_logs` | Activity trail | index on `created_at` |

Notes on the modelling choices:

- **Role** is an enum column on `users` rather than a separate table: there are exactly three fixed
  roles and each user has one, so a lookup table would add a join without adding information.
- **Result** lives on `exam_attempts`. A result is strictly one-to-one with an attempt and is written
  in the same transaction, so a separate `results` table would only duplicate the key.
- **OPTION** is named `question_options` because `OPTION` is a reserved word in SQL.
- `exams.total_marks` and `question_count` are derived values, recalculated by the server whenever
  questions change. They are stored so list screens do not need to aggregate questions.
- Student-only and instructor-only profile columns are nullable columns of `users`. With two small
  profile shapes this stays simpler than two extra tables; MySQL allows many `NULL`s in a unique index.

---

## How the main flows work

### Authentication

1. `POST /api/auth/login` checks the BCrypt hash, then account state (active, email verified,
   instructor approved) and returns a signed JWT (HMAC-SHA256, default lifetime 2 hours).
2. The browser stores the token (session storage, or local storage with "Remember me") and sends it
   as `Authorization: Bearer …`.
3. `JwtAuthenticationFilter` validates the token on every request and reloads the user, so a
   deactivated account loses access immediately.
4. When the token expires the API answers 401; the frontend clears the session, shows "Your session
   has expired" and returns to the login page.

### Email verification

Register → a 256-bit random token is generated → only its **SHA-256 hash** is stored, with a
30-minute expiry → the link `FRONTEND_URL/#/verify-email?token=…` is emailed → the page posts the
token to `/api/auth/verify-email`.

Outcomes handled: valid, expired, invalid, already used (or replaced by a resend) and already
verified. Resending cancels older tokens, has a 60-second cooldown with a visible countdown and a
limit of five emails per day.

### Instructor approval

Register → verify email → status `PENDING` → the admin approves (only possible once the email is
verified) or rejects → an email is sent → login works only when `email_verified = true` **and**
`approval_status = APPROVED`.

### Exam lifecycle

`DRAFT` (private, fully editable) → `PUBLISHED` (visible to students, questions locked) → `CLOSED`
(no new attempts). A published exam with a future start time shows as "Upcoming". Publishing
validates that there is at least one question, each with exactly one correct option, and that pass
marks do not exceed the total. A published exam can go back to draft only while it has no attempts,
so existing results can never be corrupted by an edit.

### Timed attempt

- Starting an attempt stores `started_at` and `deadline_at = started_at + duration` on the server.
- The page receives `remainingSeconds` from the server and only *displays* a countdown.
- Every answer change is saved (`PUT /api/attempts/{id}/answers`), so a refresh resumes the attempt.
- At zero the browser auto-submits. Independently, the server refuses answers after the deadline
  (plus a 10-second network allowance), evaluates expired attempts when they are next touched, and
  a scheduled job sweeps any that were abandoned.

### Evaluation

On submit, inside one transaction and under a row lock:

```
for each question:  correct = selected option is the correct option
marksObtained = Σ marks of correct answers
percentage    = marksObtained / totalMarks × 100
passed        = marksObtained ≥ passMarks
```

The request contains only question and option ids. Marks sent by a client are ignored. The answer
key is never included in any response until the attempt is submitted.

---

## Security

| Concern | How it is handled |
|---|---|
| Passwords | BCrypt; never stored, logged or returned. 8–72 characters with a letter and a digit |
| Sessions | Stateless JWT; secret from `JWT_SECRET` (a random per-run secret is generated if it is missing) |
| Authorization | URL rules per role + `@PreAuthorize` + ownership checks in services (instructor ↔ exam, student ↔ attempt) |
| Tokens | Verification / reset tokens are random, single-use, expiring, stored only as hashes |
| Brute force | 5 failed logins lock the account for 15 minutes; resend and reset emails are throttled |
| Enumeration | Login, resend and forgot-password answer the same way whether or not the account exists |
| SQL injection | JPA with bound parameters only (JPQL, Criteria); no string-built SQL |
| XSS | Every value rendered by the frontend goes through an HTML-escaping helper; emails escape names |
| CSRF | Not applicable to bearer-token APIs (nothing is sent automatically by the browser), so it is disabled |
| CORS | Only the origins listed in `FRONTEND_URL` |
| Answer key | Released by the backend only after submission; not hidden by JavaScript |
| Secrets | Read from environment / `.env`; `.env` is git-ignored; nothing secret in the frontend |

---

## API overview

The complete reference with bodies and parameters is in [`docs/API.md`](docs/API.md).

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register/student`, `/register/instructor`, `/login`, `/verify-email`, `/resend-verification`, `/forgot-password`, `/reset-password` |
| Account | `GET/PUT /api/account`, `POST /api/account/password`, `GET /api/account/notifications` |
| Public | `GET /api/public/exams`, `GET /api/public/exams/{id}` |
| Student | `GET /api/student/dashboard`, `/exams`, `/exams/{id}`, `/attempts`, `/results`, `/profile` |
| Attempts | `POST /api/exams/{id}/attempts`, `GET /api/attempts/{id}`, `PUT /api/attempts/{id}/answers`, `POST /api/attempts/{id}/submit`, `GET /api/attempts/{id}/result`, `GET /api/attempts/{id}/answers` |
| Exams | `POST /api/exams`, `GET/PUT/DELETE /api/exams/{id}`, `POST /api/exams/{id}/publish`, `/unpublish`, `/close` |
| Questions | `POST /api/exams/{id}/questions`, `PUT /api/exams/{id}/questions/order`, `PUT/DELETE /api/questions/{id}` |
| Instructor | `GET /api/instructor/dashboard`, `/exams`, `/results`, `/analytics`, `/exams/{id}/attempts`, `/exams/{id}/analytics` |
| Admin | `GET /api/admin/dashboard`, `/users`, `/instructors/pending`, `/exams`, `/results`, `/reports`, `/activity`; `POST /api/admin/instructors/{id}/approve`, `/reject`; `PATCH /api/admin/users/{id}/status` |

---

## Environment variables

Set them in `.env` (project root, loaded automatically) or as real environment variables.

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `DB_URL` | no | `jdbc:mysql://localhost:3306/examsphere?...` | JDBC URL |
| `DB_USERNAME` | yes | `root` | MySQL user |
| `DB_PASSWORD` | yes | empty | MySQL password |
| `JWT_SECRET` | recommended | random per run | Signing secret; without it everyone is logged out on restart |
| `JWT_EXPIRATION` | no | `7200000` | Token lifetime in milliseconds |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | for real emails | empty | SMTP settings |
| `MAIL_PROTOCOL`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | no | `smtp`, `true`, `true` | SMTP options |
| `APP_DEV_LOG_EMAIL_LINKS` | no | `false` | Development only: print email links to the console when SMTP is not configured |
| `FRONTEND_URL` | no | `http://localhost:8080` | Used in email links and as the allowed CORS origin(s), comma-separated |
| `SERVER_PORT` | no | `8080` | Backend port |
| `APP_SEED_ENABLED` | no | `true` | Seed demo data on startup |
| `DB_DDL_AUTO` | no | `update` | Hibernate schema mode (`validate` or `none` if you manage the schema yourself) |

---

## Email (SMTP) setup

Without SMTP the application still starts and the demo accounts work, but new registrations cannot
be verified because no email leaves the server (the UI says so on the "check your email" screen).

**Option A – a test inbox (recommended for demos).** Create a free [Mailtrap](https://mailtrap.io)
sandbox inbox and copy its SMTP credentials:

```
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=<from Mailtrap>
MAIL_PASSWORD=<from Mailtrap>
MAIL_FROM=ExamSphere <no-reply@examsphere.test>
```

**Option B – Gmail.** Turn on 2-step verification, create an *App password*, then:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=<16-character app password>
MAIL_FROM=ExamSphere <you@gmail.com>
```

**Option C – no SMTP, local development only.** Set `APP_DEV_LOG_EMAIL_LINKS=true`; the verification
or reset link is then written to the backend console instead of being emailed. Never enable this on
a shared or production server.

Emails sent: verify email, resend verification, password reset, instructor approved, instructor rejected.

---

## Testing

```bash
cd backend
mvn test
```

The tests start the whole application against in-memory H2 (MySQL compatibility mode) and call the
real HTTP endpoints through MockMvc, with the security filters active. Email is replaced by a
recording test double so the tests can follow verification and reset links.

| Class | Covers |
|---|---|
| `AuthFlowTests` | student / instructor registration, duplicate email, weak password, password mismatch, invalid email and terms, verification, expired / invalid / replaced token, resend throttling, login, unverified / pending / inactive login blocked, password reset single use, no account enumeration |
| `ExamAndAttemptTests` | create / update / delete draft, validation, publish, publish without questions rejected, total marks, locked after publish, option rules, edit / delete question, start attempt without answer key, evaluation (correct, incorrect, unanswered, pass, fail), tampered marks ignored, duplicate submission rejected, late submission auto-submitted from saved answers, expired attempt evaluated on open, other student's attempt refused, analytics |
| `AuthorizationAndAdminTests` | 401 without token, student and instructor blocked from other roles' APIs, instructor ownership, deactivated user loses access, approve / reject instructor, unverified instructor cannot be approved, full instructor journey, admin cannot deactivate himself, admin lists and reports |

Manual frontend checklist: login, registration, verification page states, dashboards, exam list and
search, exam details, timer, navigation, mark for review, submit, result, review, instructor CRUD,
admin approval, mobile layout.

---

## What has been verified

Being exact about this, since the backend could not be built where the project was written:

| Item | Status |
|---|---|
| Java sources | Parsed with `javac` (no syntax errors) and checked for consistency between the project's own classes (entities, DTOs, services, controllers, tests). **Not compiled against Spring / Hibernate / JJWT, not started, tests not run.** |
| `database/schema.sql`, `database/seed.sql` | Executed on MySQL 8.0: tables, keys and constraints create cleanly; the seed is re-runnable; seeded marks agree with the seeded answers; the BCrypt hashes match the demo passwords |
| Frontend | Every page and the main flows of all three roles were driven in Chromium at desktop and phone width against a throw-away stand-in server that returns the same JSON shapes as the DTOs: no JavaScript errors, no horizontal overflow, timer, auto-submit, reload-resume, validation and empty / error states work. **Not yet run against the real backend.** |

So the first `mvn test` on your machine is the real check of the backend. If it reports a
compilation error or a failing test, the message will point at the exact line.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Access denied for user 'root'@'localhost'` | Wrong `DB_USERNAME` / `DB_PASSWORD` in `.env`. The file must be in the `ExamSphere` root (or in `backend/`) and you must start from the `backend` folder |
| `Communications link failure` | MySQL is not running or not on port 3306. Start the service; adjust `DB_URL` |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in `DB_URL` (it is in the default) |
| `Unknown database 'examsphere'` | Create it (step 2) or keep `createDatabaseIfNotExist=true` in the URL |
| `Port 8080 was already in use` | Set `SERVER_PORT=8081` in `.env` and open `http://localhost:8081` (also set `FRONTEND_URL` to match) |
| Blank page or 404 at `http://localhost:8080` | Start with `mvn spring-boot:run` from the `backend` folder so the `frontend` folder is picked up |
| "Cannot reach the server" in the UI | The backend is not running, or the page is served from another port and `FRONTEND_URL` does not list that origin (CORS) |
| Logged out after every restart | Set `JWT_SECRET` in `.env` |
| Verification email never arrives | SMTP is not configured or the credentials are wrong; check the backend log for "Failed to send email" |
| Demo login says invalid password | The demo users were created earlier with different data. Drop the database and restart |
| Account locked | Five wrong passwords lock the account for 15 minutes |
| Schema changed after an update | For a clean start: `DROP DATABASE examsphere;` then restart |
| `release version 17 not supported` | Your JDK is older than 17. Install JDK 17 or 21 |

---

## Screenshots

Add your own captures here for the report or portfolio (suggested: landing page, student dashboard,
exam screen, result, question manager, analytics, admin dashboard). Put the images in
`docs/screenshots/` and reference them like this:

```markdown
![Student dashboard](docs/screenshots/student-dashboard.png)
```

---

## Known limitations

- Only single-answer multiple-choice questions. No negative marking, no sections.
- One attempt per student per exam.
- Date-times are stored and shown in the server's local time zone (no per-user time zones).
- Profile photos are stored as small data URLs in the database, not in object storage.
- Logging out discards the token in the browser; the token itself stays valid until it expires
  (there is no server-side revocation list). Deactivating a user does take effect immediately.
- Rate limiting is per account and kept in the database; there is no per-IP limiting.
- No proctoring (tab-switch detection, webcam). The exam screen is distraction-free, not locked down.
- Schema changes rely on Hibernate `ddl-auto=update`; a real deployment should use Flyway or Liquibase.

## Future improvements

PDF and CSV export of results, question bank with random selection, negative marking, multiple
attempts with best-score policy, descriptive questions with manual grading, refresh tokens,
email change with re-verification, per-IP rate limiting, Docker Compose setup, CI pipeline.

---

## Interview talking points

**Java / OOP**
- *Encapsulation*: entities expose state only through getters and setters; services hide repositories.
- *Abstraction and polymorphism*: `EmailService` is an interface with an SMTP implementation; the
  tests swap in `RecordingEmailService` without touching the callers.
- *Inheritance*: `EmailVerificationToken` and `PasswordResetToken` extend `BaseToken`
  (`@MappedSuperclass`); every business error extends `ApiException`.
- Records for DTOs, enums for role and status, `Optional`, streams, a custom annotation with its validator.

**Spring Boot**
- Constructor injection everywhere, which makes dependencies explicit and classes testable.
- Thin controllers, rules in services, `@RestControllerAdvice` for one consistent error format.
- Bean Validation on request records plus service-level rules that need the database.

**JPA / Hibernate**
- Why `LAZY` is the default here and where entity graphs / fetch joins are used to avoid N+1.
- `open-in-view=false` and why DTO mapping happens inside the transaction.
- `orphanRemoval` for questions and options; `@PrePersist` / `@PreUpdate` timestamps.
- Specifications for optional search filters instead of string-built queries.

**DBMS**
- Primary and foreign keys, unique constraints doing real work (`UNIQUE(exam_id, student_id)` is
  what stops a double start; `UNIQUE(attempt_id, question_id)` stops duplicate answers).
- Why the listed indexes exist (the columns used in `WHERE` and `JOIN`).
- Normalisation, and two deliberate denormalisations (`total_marks`, `question_count`) with the reason.
- Aggregate queries with `GROUP BY` for analytics.

**Transactions and concurrency**
- Submission is one transaction: save answers, evaluate, store the result, or nothing at all.
- `SELECT … FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`) serialises two simultaneous submits; the second
  sees `submitted = true` and gets HTTP 409.
- `noRollbackFor` on login so the failed-attempt counter is kept even though the call fails.

**Security**
- BCrypt: salted and deliberately slow. JWT: what is in it, how it is signed, why it is stateless.
- Authentication vs authorization vs ownership: three separate checks on every protected call.
- Why tokens are hashed in the database and why responses do not reveal whether an email exists.
- Why CSRF protection is unnecessary for bearer tokens but needed for cookie sessions.

**Never trust the client**
- The timer is a display; the deadline is a database column.
- The browser sends option ids, never marks. The answer key is not in the page until after submission.

**Frontend**
- Fetch wrapper that adds the token, parses the error envelope and handles 401 globally.
- Hash-based router with role guards (convenience only; the server enforces everything again).
- DOM rendering with escaped template strings; design tokens as CSS variables; responsive layout
  with a collapsible sidebar; charts drawn with plain SVG and CSS from API data.
