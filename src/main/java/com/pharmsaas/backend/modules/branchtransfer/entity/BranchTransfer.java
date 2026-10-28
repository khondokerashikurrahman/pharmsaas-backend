package com.pharmsaas.backend.modules.branchtransfer.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.store.entity.Store;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "branch_transfers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_branch_transfer_tenant_number",
        columnNames = {"tenant_id", "transfer_number"}
    ),
    indexes = {
        @Index(name = "idx_transfer_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "idx_transfer_source", columnList = "source_store_id"),
        @Index(name = "idx_transfer_destination", columnList = "destination_store_id")
    }
)
public class BranchTransfer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_store_id", nullable = false)
    private Store sourceStore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_store_id", nullable = false)
    private Store destinationStore;

    @Column(name = "transfer_number", nullable = false, length = 50)
    private String transferNumber;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "action_note", length = 500)
    private String actionNote;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "dispatched_by")
    private Long dispatchedBy;

    @Column(name = "received_by")
    private Long receivedBy;

    private OffsetDateTime approvedAt;
    private OffsetDateTime rejectedAt;
    private OffsetDateTime dispatchedAt;
    private OffsetDateTime receivedAt;

    @Version
    private Long version;

    @OneToMany(
        mappedBy = "transfer",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<BranchTransferItem> items = new ArrayList<>();

    public BranchTransfer() {}

    public void addItem(BranchTransferItem item) {
        item.setTransfer(this);
        items.add(item);
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Store getSourceStore() { return sourceStore; }
    public void setSourceStore(Store sourceStore) { this.sourceStore = sourceStore; }

    public Store getDestinationStore() { return destinationStore; }
    public void setDestinationStore(Store destinationStore) { this.destinationStore = destinationStore; }

    public String getTransferNumber() { return transferNumber; }
    public void setTransferNumber(String transferNumber) { this.transferNumber = transferNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getActionNote() { return actionNote; }
    public void setActionNote(String actionNote) { this.actionNote = actionNote; }

    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public Long getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(Long rejectedBy) { this.rejectedBy = rejectedBy; }

    public Long getDispatchedBy() { return dispatchedBy; }
    public void setDispatchedBy(Long dispatchedBy) { this.dispatchedBy = dispatchedBy; }

    public Long getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Long receivedBy) { this.receivedBy = receivedBy; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public OffsetDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(OffsetDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public OffsetDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(OffsetDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(OffsetDateTime receivedAt) { this.receivedAt = receivedAt; }

    public List<BranchTransferItem> getItems() { return items; }
}
