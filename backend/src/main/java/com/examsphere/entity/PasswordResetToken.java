package com.examsphere.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "idx_prt_user", columnList = "user_id"))
public class PasswordResetToken extends BaseToken {
}
