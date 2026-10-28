package com.pharmsaas.backend.modules.inventory.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_requests", indexes = {
    @Index(name = "idx_stock_request_tenant_status", columnList = "tenant_id,status"),
    @Index(name = "idx_stock_request_requester", columnList = "requester_user_id")
})
public class StockRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private TenantInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private TenantInventoryBatch batch;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 500)
    private String decisionNote;

    private OffsetDateTime decidedAt;

    @Version
    private Long version;

    public StockRequest() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public TenantInventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(TenantInventoryItem inventoryItem) { this.inventoryItem = inventoryItem; }

    public TenantInventoryBatch getBatch() { return batch; }
    public void setBatch(TenantInventoryBatch batch) { this.batch = batch; }

    public Long getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(Long requesterUserId) { this.requesterUserId = requesterUserId; }

    public Long getApproverUserId() { return approverUserId; }
    public void setApproverUserId(Long approverUserId) { this.approverUserId = approverUserId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDecisionNote() { return decisionNote; }
    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }

    public OffsetDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(OffsetDateTime decidedAt) { this.decidedAt = decidedAt; }
}
