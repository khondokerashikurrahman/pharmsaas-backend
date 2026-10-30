package com.pharmsaas.backend.modules.subscription.entity;

import com.pharmsaas.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
    name = "subscription_payments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subscription_payment_reference",
            columnNames = "transaction_reference"
        )
    },
    indexes = {
        @Index(name = "idx_subscription_payment_status", columnList = "status"),
        @Index(name = "idx_subscription_payment_pending", columnList = "pending_registration_id")
    }
)
public class SubscriptionPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pending_registration_id", nullable = false)
    private PendingRegistration pendingRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 80)
    private String transactionReference;

    @Column(name = "gateway_name", nullable = false, length = 40)
    private String gatewayName = "SSLCOMMERZ";

    @Column(name = "gateway_transaction_id", length = 120)
    private String gatewayTransactionId;

    @Column(name = "gateway_session_id", length = 150)
    private String gatewaySessionId;

    @Column(name = "gateway_url", length = 1000)
    private String gatewayUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "BDT";

    @Column(nullable = false, length = 30)
    private String status = "INITIATED";

    @Column(name = "gateway_validation_id", length = 80)
    private String gatewayValidationId;

    @Column(name = "gateway_risk_level", length = 10)
    private String gatewayRiskLevel;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    public SubscriptionPayment() {
    }
}
