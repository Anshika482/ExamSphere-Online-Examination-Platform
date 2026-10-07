package com.examsphere.repository;

import com.examsphere.entity.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    /** A newer token replaces all earlier ones, so older links stop working. */
    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetToken t set t.used = true, t.usedAt = :now where t.user.id = :userId and t.used = false")
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
