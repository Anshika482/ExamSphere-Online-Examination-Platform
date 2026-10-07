package com.examsphere.service;

import com.examsphere.dto.UserDtos.ActivityItem;
import com.examsphere.entity.AuditAction;
import com.examsphere.entity.AuditLog;
import com.examsphere.entity.User;
import com.examsphere.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records the basic activity trail shown to administrators. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Joins the caller's transaction, so an action and its audit entry are saved (or rolled back) together. */
    @Transactional
    public void record(User user, AuditAction action, String description) {
        AuditLog entry = new AuditLog();
        entry.setUser(user);
        entry.setAction(action);
        entry.setDescription(description.length() > 500 ? description.substring(0, 500) : description);
        auditLogRepository.save(entry);
    }

    public static ActivityItem toItem(AuditLog entry) {
        User user = entry.getUser();
        return new ActivityItem(entry.getId(), entry.getAction().name(), entry.getDescription(),
                user == null ? null : user.getFullName(), user == null ? null : user.getEmail(), entry.getCreatedAt());
    }
}
