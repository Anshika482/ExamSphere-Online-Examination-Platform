package com.examsphere;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import com.examsphere.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthorizationAndAdminTests extends IntegrationTestBase {

    // ------------------------------------------------------------------ authorization

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        getAs("/api/student/dashboard", null).andExpect(status().isUnauthorized());
        getAs("/api/admin/users", null).andExpect(status().isUnauthorized());
        getAs("/api/student/dashboard", "not.a.real-token").andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotUseInstructorOrAdminApis() throws Exception {
        String token = tokenFor(student());
        getAs("/api/admin/dashboard", token).andExpect(status().isForbidden());
        getAs("/api/admin/users", token).andExpect(status().isForbidden());
        getAs("/api/instructor/dashboard", token).andExpect(status().isForbidden());
        postJson("/api/exams", examRequest("Not allowed", 0), token).andExpect(status().isForbidden());
    }

    @Test
    void instructorCannotUseAdminOrStudentApis() throws Exception {
        String token = tokenFor(instructor());
        getAs("/api/admin/dashboard", token).andExpect(status().isForbidden());
        postJson("/api/admin/instructors/1/approve", Map.of(), token).andExpect(status().isForbidden());
        getAs("/api/student/dashboard", token).andExpect(status().isForbidden());
    }

    @Test
    void instructorCannotManageAnotherInstructorsExam() throws Exception {
        String owner = tokenFor(instructor());
        String intruder = tokenFor(instructor());
        long examId = createExam(owner, "Private draft", 0);

        getAs("/api/exams/" + examId, intruder).andExpect(status().isForbidden());
        send(put("/api/exams/" + examId), examRequest("Hijacked", 0), intruder).andExpect(status().isForbidden());
        postJson("/api/exams/" + examId + "/questions", questionRequest("Q", 5, 0), intruder).andExpect(status().isForbidden());
        postJson("/api/exams/" + examId + "/publish", Map.of(), intruder).andExpect(status().isForbidden());
        send(delete("/api/exams/" + examId), null, intruder).andExpect(status().isForbidden());
        getAs("/api/instructor/exams/" + examId + "/analytics", intruder).andExpect(status().isForbidden());

        // the owner still can
        getAs("/api/exams/" + examId, owner).andExpect(status().isOk());
    }

    @Test
    void deactivatedUserLosesAccessImmediately() throws Exception {
        User user = student();
        String token = tokenFor(user);
        getAs("/api/student/dashboard", token).andExpect(status().isOk());

        user = userRepository.findById(user.getId()).orElseThrow();
        user.setActive(false);
        userRepository.save(user);
        getAs("/api/student/dashboard", token).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ admin

    @Test
    void adminApprovesVerifiedInstructorWhoCanThenLogin() throws Exception {
        String adminToken = tokenFor(admin());
        User pending = saveUser(Role.INSTRUCTOR, true, ApprovalStatus.PENDING);

        getAs("/api/admin/instructors/pending", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + pending.getId() + ")]").exists());

        postJson("/api/admin/instructors/" + pending.getId() + "/approve", Map.of(), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));
        assertThat(emails.approvalEmails()).contains(pending.getEmail());

        login(pending.getEmail(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unverifiedInstructorCannotBeApproved() throws Exception {
        String adminToken = tokenFor(admin());
        User pending = saveUser(Role.INSTRUCTOR, false, ApprovalStatus.PENDING);
        postJson("/api/admin/instructors/" + pending.getId() + "/approve", Map.of(), adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void adminRejectsInstructorWhoThenCannotLogin() throws Exception {
        String adminToken = tokenFor(admin());
        User pending = saveUser(Role.INSTRUCTOR, true, ApprovalStatus.PENDING);

        postJson("/api/admin/instructors/" + pending.getId() + "/reject", Map.of(), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"));
        assertThat(emails.rejectionEmails()).contains(pending.getEmail());

        login(pending.getEmail(), PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVAL_REJECTED"));
    }

    @Test
    void fullInstructorJourneyRegisterVerifyApproveLogin() throws Exception {
        String email = uniqueEmail("journey");
        postJson("/api/auth/register/instructor", instructorRegistration(email), null).andExpect(status().isCreated());
        postJson("/api/auth/verify-email", Map.of("token", emails.verificationToken(email)), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalRequired").value(true));
        login(email, PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("APPROVAL_PENDING"));

        Long id = userRepository.findByEmail(email).orElseThrow().getId();
        postJson("/api/admin/instructors/" + id + "/approve", Map.of(), tokenFor(admin())).andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.data.user.role").value("INSTRUCTOR"));
    }

    @Test
    void adminCanDeactivateOthersButNotHimself() throws Exception {
        User adminUser = admin();
        String adminToken = tokenFor(adminUser);
        User target = student();

        send(patch("/api/admin/users/" + target.getId() + "/status"), Map.of("active", false), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        login(target.getEmail(), PASSWORD).andExpect(status().isForbidden());

        send(patch("/api/admin/users/" + adminUser.getId() + "/status"), Map.of("active", false), adminToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));
    }

    @Test
    void adminListsAndReportsLoad() throws Exception {
        String adminToken = tokenFor(admin());
        String response = getAs("/api/admin/users?role=ADMIN&size=5", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("passwordHash").doesNotContain("$2a$");

        getAs("/api/admin/dashboard", adminToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").isNumber());
        getAs("/api/admin/reports", adminToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examReport").isArray());
        getAs("/api/admin/exams", adminToken).andExpect(status().isOk());
        getAs("/api/admin/results", adminToken).andExpect(status().isOk());
        getAs("/api/admin/activity", adminToken).andExpect(status().isOk());
    }
}
