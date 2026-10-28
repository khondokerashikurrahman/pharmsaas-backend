package com.pharmsaas.backend.modules.inventory.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.store.entity.Store;
import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_inventory_movements",
    indexes = {
        @Index(
            name = "idx_inv_movement_tenant_date",
            columnList = "tenant_id,created_at"
        ),
        @Index(
            name = "idx_inv_movement_product_date",
            columnList = "inventory_item_id,created_at"
        ),
        @Index(
            name = "idx_inv_movement_reference",
            columnList = "reference_type,reference_id"
        )
    }
)
public class InventoryMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private TenantInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private TenantInventoryBatch batch;

    @Column(name = "movement_type", nullable = false, length = 40)
    private String movementType;

    // Negative values reduce stock; positive values increase stock.
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    // Batch quantity after the operation, or inventory quantity if no batch applies.
    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(length = 250)
    private String reason;

    // Keep the actor ID as historical data even if the user is later removed.
    @Column(name = "actor_user_id")
    private Long actorUserId;

    public InventoryMovement() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public TenantInventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(TenantInventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public TenantInventoryBatch getBatch() { return batch; }
    public void setBatch(TenantInventoryBatch batch) { this.batch = batch; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
}
