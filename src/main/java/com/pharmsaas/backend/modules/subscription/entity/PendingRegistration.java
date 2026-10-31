package com.pharmsaas.backend.modules.subscription.entity;

import com.pharmsaas.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
    name = "pending_registrations",
    indexes = {
        @Index(name = "idx_pending_registration_email", columnList = "email"),
        @Index(name = "idx_pending_registration_status", columnList = "status")
    }
)
public class PendingRegistration extends BaseEntity {

    @Column(name = "registration_reference", nullable = false, unique = true, length = 60)
    private String registrationReference;

    @Column(name = "pharmacy_name", nullable = false, length = 150)
    private String pharmacyName;

    @Column(name = "owner_name", nullable = false, length = 150)
    private String ownerName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(nullable = false, length = 30)
    private String status = "PENDING_EMAIL_VERIFICATION";

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "welcome_email_sent_at")
    private OffsetDateTime welcomeEmailSentAt;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verification_token_hash", length = 64)
    private String emailVerificationTokenHash;

    @Column(name = "email_verification_expires_at")
    private OffsetDateTime emailVerificationExpiresAt;

    @Column(name = "email_verification_last_sent_at")
    private OffsetDateTime emailVerificationLastSentAt;

    @Column(name = "email_verification_attempts", nullable = false)
    private int emailVerificationAttempts = 0;

    @Column(name = "email_verification_send_count", nullable = false)
    private int emailVerificationSendCount = 0;

    public PendingRegistration() {
    }
}
