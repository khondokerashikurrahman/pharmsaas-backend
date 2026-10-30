package com.pharmsaas.backend.modules.refund.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.salesreturn.entity.SaleReturn;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tenant_sale_refunds",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refund_tenant_request", columnNames = {"tenant_id", "request_key"}),
        @UniqueConstraint(name = "uk_refund_number", columnNames = {"refund_number"})
    },
    indexes = {
        @Index(name = "idx_refund_tenant_return", columnList = "tenant_id,sale_return_id"),
        @Index(name = "idx_refund_tenant_status", columnList = "tenant_id,status")
    })
public class Refund extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_return_id", nullable = false)
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    @Column(name = "refund_number", nullable = false, length = 60)
    private String refundNumber;

    @Column(name = "request_key", nullable = false, length = 100)
    private String requestKey;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "transaction_reference", length = 150)
    private String transactionReference;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public Refund() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant value) { tenant = value; }
    public SaleReturn getSaleReturn() { return saleReturn; }
    public void setSaleReturn(SaleReturn value) { saleReturn = value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { createdBy = value; }
    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User value) { processedBy = value; }
    public String getRefundNumber() { return refundNumber; }
    public void setRefundNumber(String value) { refundNumber = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { paymentMethod = value; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String value) { transactionReference = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(OffsetDateTime value) { processedAt = value; }
}
