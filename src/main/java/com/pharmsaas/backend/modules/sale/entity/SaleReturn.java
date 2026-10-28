package com.pharmsaas.backend.modules.sale.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "tenant_sale_returns",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sale_return_tenant_request",
            columnNames = {"tenant_id", "request_key"}
        ),
        @UniqueConstraint(
            name = "uk_sale_return_number",
            columnNames = {"refund_number"}
        )
    }
)
public class SaleReturn extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "request_key", nullable = false, length = 100)
    private String requestKey;

    @Column(name = "refund_number", nullable = false, length = 60)
    private String refundNumber;

    @Column(nullable = false, length = 250)
    private String reason;

    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    public SaleReturn() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }

    public String getRefundNumber() { return refundNumber; }
    public void setRefundNumber(String refundNumber) { this.refundNumber = refundNumber; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
