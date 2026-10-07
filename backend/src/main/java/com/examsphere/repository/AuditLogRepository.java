package com.examsphere.repository;

import com.examsphere.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
