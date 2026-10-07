package com.examsphere.repository;

import com.examsphere.entity.EmailVerificationToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    /** A newer token replaces all earlier ones, so older links stop working. */
    @Modifying(flushAutomatically = true)
    @Query("update EmailVerificationToken t set t.used = true, t.usedAt = :now where t.user.id = :userId and t.used = false")
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
