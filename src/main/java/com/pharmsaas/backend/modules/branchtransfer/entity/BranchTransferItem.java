package com.pharmsaas.backend.modules.branchtransfer.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import jakarta.persistence.*;

@Entity
@Table(
    name = "branch_transfer_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_transfer_batch",
        columnNames = {"transfer_id", "batch_id"}
    ),
    indexes = @Index(name = "idx_transfer_item_batch", columnList = "batch_id")
)
public class BranchTransferItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private BranchTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private TenantInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TenantInventoryBatch batch;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    public BranchTransferItem() {}

    public BranchTransfer getTransfer() { return transfer; }
    public void setTransfer(BranchTransfer transfer) { this.transfer = transfer; }

    public TenantInventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(TenantInventoryItem inventoryItem) { this.inventoryItem = inventoryItem; }

    public TenantInventoryBatch getBatch() { return batch; }
    public void setBatch(TenantInventoryBatch batch) { this.batch = batch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
}
