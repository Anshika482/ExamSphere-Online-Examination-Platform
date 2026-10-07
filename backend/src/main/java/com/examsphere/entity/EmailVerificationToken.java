package com.examsphere.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_tokens", indexes = @Index(name = "idx_evt_user", columnList = "user_id"))
public class EmailVerificationToken extends BaseToken {
}
