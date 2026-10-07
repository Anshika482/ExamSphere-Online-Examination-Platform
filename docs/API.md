# ExamSphere REST API

Base URL: `http://localhost:8080`. All bodies are JSON.

**Authentication** – send `Authorization: Bearer <token>` (the token comes from `POST /api/auth/login`).

**Success envelope**

```json
{ "message": "Exam created successfully.", "data": { } }
```

**Error envelope** (`errors` only for validation failures, `retryAfterSeconds` only for throttling)

```json
{ "message": "Validation failed", "status": 400, "code": "VALIDATION_FAILED",
  "errors": { "email": "Invalid email address" }, "timestamp": "2026-01-01T10:00:00" }
```

| Status | Meaning |
|---|---|
| 400 | Validation failed, malformed request, invalid / expired / used token |
| 401 | Missing, invalid or expired JWT; wrong email or password |
| 403 | Wrong role, not the owner, or account not allowed to log in (`EMAIL_NOT_VERIFIED`, `APPROVAL_PENDING`, `APPROVAL_REJECTED`, `ACCOUNT_INACTIVE`) |
| 404 | Resource not found |
| 409 | Duplicate email / ID, invalid exam state, duplicate submission (`ALREADY_SUBMITTED`) |
| 429 | Resend cooldown or too many failed logins |

## Auth (public)

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/auth/register/student` | fullName, email, phone, password, confirmPassword, college, course, branch, yearSemester, studentId, acceptTerms, profilePhoto? | 201. Account is unverified; verification email sent |
| POST | `/api/auth/register/instructor` | fullName, email, phone, password, confirmPassword, institution, department, employeeId, designation, acceptTerms, profilePhoto? | 201. Unverified + approval `PENDING` |
| POST | `/api/auth/login` | email, password | Returns `token`, `expiresInSeconds`, `user` |
| POST | `/api/auth/verify-email` | token | `data.status` = `VERIFIED` or `ALREADY_VERIFIED`; errors `INVALID_TOKEN`, `TOKEN_EXPIRED`, `TOKEN_USED` |
| POST | `/api/auth/resend-verification` | email | 60 s cooldown (429 with `retryAfterSeconds`), max 5 per day; older links are cancelled |
| POST | `/api/auth/forgot-password` | email | Always the same answer |
| POST | `/api/auth/reset-password/validate` | token | Lets the UI show invalid / expired / used before the form |
| POST | `/api/auth/reset-password` | token, newPassword, confirmPassword | Single-use, 30 min |

## Account (any signed-in role)

| Method | Path | Notes |
|---|---|---|
| GET / PUT | `/api/account` | Own profile. Also at `/api/student/profile`, `/api/instructor/profile`, `/api/admin/profile` |
| POST | `/api/account/password` | currentPassword, newPassword, confirmPassword |
| GET | `/api/account/notifications` | Built from live data for the caller's role |
| POST | `/api/account/logout` | Records the logout; the client discards the token |

## Public catalogue

| Method | Path | Notes |
|---|---|---|
| GET | `/api/public/exams?q=&category=&sort=` | Published, open or upcoming exams (summaries only) |
| GET | `/api/public/exams/{id}` | One published exam summary |

## Student (`ROLE_STUDENT`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/student/dashboard` | Stats, score trend, recent attempts, open exams |
| GET | `/api/student/exams?q=&category=&duration=&status=&sort=` | duration `SHORT/MEDIUM/LONG`; status `AVAILABLE/UPCOMING/IN_PROGRESS/COMPLETED`; sort `newest/title/duration` |
| GET | `/api/student/exams/categories` | Categories of published exams |
| GET | `/api/student/exams/{id}` | Details + the caller's attempt state |
| GET | `/api/student/attempts?examId=&status=&from=&to=&sort=` | History. `/api/student/results` is an alias |
| POST | `/api/exams/{id}/attempts` | Start (or resume). Server stores `startedAt` and `deadlineAt` |
| GET | `/api/attempts/{id}` | Questions **without** the answer key, saved answers, `remainingSeconds` from the server |
| PUT | `/api/attempts/{id}/answers` | questionId, selectedOptionId, markedForReview – saves one answer |
| POST | `/api/attempts/{id}/submit` | answers[], autoSubmit – evaluates on the server; second call → 409 |
| GET | `/api/attempts/{id}/result` | Owner, the exam's instructor, or admin |
| GET | `/api/attempts/{id}/answers` | Review with correct options – only after submission |

## Instructor (`ROLE_INSTRUCTOR`, own exams only)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/instructor/dashboard` | |
| GET | `/api/instructor/exams?q=&status=` | |
| POST | `/api/exams` | title, description, category, durationMinutes, passMarks, scheduledAt?, closesAt? → `DRAFT` |
| GET | `/api/exams/{id}` | With questions and answer key (owner or admin) |
| PUT / DELETE | `/api/exams/{id}` | Draft only |
| POST | `/api/exams/{id}/publish` | Validates questions, recalculates total marks |
| POST | `/api/exams/{id}/unpublish` | Only while there are no attempts |
| POST | `/api/exams/{id}/close` | Owner or admin |
| POST | `/api/exams/{id}/questions` | text, marks, options[{text, correct}] (2–6 options, exactly one correct) |
| PUT | `/api/exams/{id}/questions/order` | questionIds[] |
| PUT / DELETE | `/api/questions/{id}` | Draft only |
| GET | `/api/instructor/exams/{id}/attempts` | |
| GET | `/api/instructor/results?examId=&student=&status=&from=&to=&page=&size=` | Paged |
| GET | `/api/instructor/analytics` | Per-exam summary |
| GET | `/api/instructor/exams/{id}/analytics` | Summary, score distribution, question-wise accuracy |

## Admin (`ROLE_ADMIN`)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/dashboard` | |
| GET | `/api/admin/users?q=&role=&active=&verified=&approval=&page=&size=` | Paged |
| GET | `/api/admin/users/{id}` | |
| PATCH | `/api/admin/users/{id}/status` | { "active": false } – an admin cannot deactivate themselves |
| GET | `/api/admin/instructors/pending` | |
| POST | `/api/admin/instructors/{id}/approve` | Email must be verified; sends approval email |
| POST | `/api/admin/instructors/{id}/reject` | Sends rejection email |
| GET | `/api/admin/exams?q=&status=&category=&instructorId=` | |
| GET | `/api/admin/exams/filters` | Categories and instructors for the filter dropdowns |
| GET | `/api/admin/exams/{id}/analytics` | |
| GET | `/api/admin/results?examId=&q=&status=&from=&to=&page=&size=` | Paged |
| GET | `/api/admin/reports` | Platform overview, exam report, student report |
| GET | `/api/admin/activity?page=&size=` | Audit trail, paged |
