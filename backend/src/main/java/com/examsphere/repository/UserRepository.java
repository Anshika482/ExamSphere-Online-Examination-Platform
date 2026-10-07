package com.examsphere.repository;

import com.examsphere.entity.ApprovalStatus;
import com.examsphere.entity.Role;
import com.examsphere.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByEmployeeId(String employeeId);

    long countByRole(Role role);

    long countByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);

    List<User> findByRoleAndApprovalStatusOrderByCreatedAtDesc(Role role, ApprovalStatus approvalStatus);
}
