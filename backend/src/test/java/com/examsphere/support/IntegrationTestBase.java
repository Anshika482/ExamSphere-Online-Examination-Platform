package com.examsphere.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Boots the whole application against in-memory H2 and drives it through the real
 * HTTP layer (security filters included), so each test covers controller, service,
 * repository and database together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Passw0rd!";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected RecordingEmailService emails;

    protected String uniqueEmail(String prefix) {
        return prefix + COUNTER.incrementAndGet() + "@test.examsphere";
    }

    // ---- HTTP helpers ----

    protected ResultActions postJson(String url, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder request = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        return mvc.perform(withToken(request, token));
    }

    protected ResultActions send(MockHttpServletRequestBuilder request, Object body, String token) throws Exception {
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(withToken(request, token));
    }

    protected ResultActions getAs(String url, String token) throws Exception {
        return mvc.perform(withToken(get(url), token));
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    protected JsonNode body(ResultActions actions) throws Exception {
        MvcResult result = actions.andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    // ---- data helpers ----

    protected Map<String, Object> studentRegistration(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Test Student");
        body.put("email", email);
        body.put("phone", "+91 98765 43210");
        body.put("password", PASSWORD);
        body.put("confirmPassword", PASSWORD);
        body.put("college", "Test College");
        body.put("course", "B.Tech");
        body.put("branch", "CSE");
        body.put("yearSemester", "4th Year");
        body.put("studentId", "SID-" + COUNTER.incrementAndGet());
        body.put("acceptTerms", true);
        return body;
    }

    protected Map<String, Object> instructorRegistration(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Test Instructor");
        body.put("email", email);
        body.put("phone", "9876543210");
        body.put("password", PASSWORD);
        body.put("confirmPassword", PASSWORD);
        body.put("institution", "Test College");
        body.put("department", "CSE");
        body.put("employeeId", "EID-" + COUNTER.incrementAndGet());
        body.put("designation", "Assistant Professor");
        body.put("acceptTerms", true);
        return body;
    }

    /** Inserts a ready-to-use account directly, for tests that are not about registration. */
    protected User saveUser(Role role, boolean verified, ApprovalStatus approval) {
        int n = COUNTER.incrementAndGet();
        User user = new User();
        user.setRole(role);
        user.setFullName(role.name().charAt(0) + role.name().substring(1).toLowerCase() + " " + n);
        user.setEmail(role.name().toLowerCase() + n + "@test.examsphere");
        user.setPhone("9000000000");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setEmailVerified(verified);
        user.setEmailVerifiedAt(verified ? LocalDateTime.now() : null);
        user.setApprovalStatus(approval);
        user.setActive(true);
        if (role == Role.STUDENT) {
            user.setStudentId("SID-" + n);
        } else if (role == Role.INSTRUCTOR) {
            user.setEmployeeId("EID-" + n);
        }
        return userRepository.save(user);
    }

    protected User student() {
        return saveUser(Role.STUDENT, true, ApprovalStatus.NOT_REQUIRED);
    }

    protected User instructor() {
        return saveUser(Role.INSTRUCTOR, true, ApprovalStatus.APPROVED);
    }

    protected User admin() {
        return saveUser(Role.ADMIN, true, ApprovalStatus.NOT_REQUIRED);
    }

    protected ResultActions login(String email, String password) throws Exception {
        return postJson("/api/auth/login", Map.of("email", email, "password", password), null);
    }

    protected String tokenFor(User user) throws Exception {
        JsonNode response = body(login(user.getEmail(), PASSWORD).andExpect(status().isOk()));
        return response.get("data").get("token").asText();
    }

    protected Map<String, Object> examRequest(String title, int passMarks) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "Created by a test");
        body.put("category", "Java");
        body.put("durationMinutes", 30);
        body.put("passMarks", passMarks);
        return body;
    }

    /** Four options; the one at correctIndex is the right answer. */
    protected Map<String, Object> questionRequest(String text, int marks, int correctIndex) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            options.add(Map.of("text", text + " option " + i, "correct", i == correctIndex));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("marks", marks);
        body.put("options", options);
        return body;
    }

    protected long createExam(String instructorToken, String title, int passMarks) throws Exception {
        JsonNode response = body(postJson("/api/exams", examRequest(title, passMarks), instructorToken)
                .andExpect(status().isCreated()));
        return response.get("data").get("exam").get("id").asLong();
    }

    /** Creates a published exam with the given number of 5-mark questions (correct option is always index 1). */
    protected long publishedExam(String instructorToken, int questions, int passMarks) throws Exception {
        long examId = createExam(instructorToken, "Exam " + COUNTER.incrementAndGet(), passMarks);
        for (int i = 0; i < questions; i++) {
            postJson("/api/exams/" + examId + "/questions", questionRequest("Question " + i, 5, 1), instructorToken)
                    .andExpect(status().isCreated());
        }
        postJson("/api/exams/" + examId + "/publish", Map.of(), instructorToken).andExpect(status().isOk());
        return examId;
    }

    /** Reads the exam as its instructor, which includes the answer key. */
    protected JsonNode examQuestions(String instructorToken, long examId) throws Exception {
        return body(getAs("/api/exams/" + examId, instructorToken).andExpect(status().isOk()))
                .get("data").get("questions");
    }

    protected long optionId(JsonNode question, boolean correct) {
        for (JsonNode option : question.get("options")) {
            if (option.get("correct").asBoolean() == correct) {
                return option.get("id").asLong();
            }
        }
        throw new IllegalStateException("No matching option");
    }

    protected Map<String, Object> answer(long questionId, Long optionId) {
        Map<String, Object> answer = new HashMap<>();
        answer.put("questionId", questionId);
        answer.put("selectedOptionId", optionId);
        answer.put("markedForReview", false);
        return answer;
    }
}
