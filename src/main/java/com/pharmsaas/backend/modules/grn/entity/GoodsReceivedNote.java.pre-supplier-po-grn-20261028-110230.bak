package com.pharmsaas.backend.modules.grn.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "goods_received_notes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_grn_tenant_invoice",
        columnNames = {"tenant_id", "supplier_invoice_number"}
    ),
    indexes = @Index(name = "idx_grn_tenant_received", columnList = "tenant_id,received_date")
)
public class GoodsReceivedNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "grn_number", nullable = false, length = 40)
    private String grnNumber;

    @Column(name = "supplier_name", nullable = false, length = 150)
    private String supplierName;

    @Column(name = "purchase_order_number", nullable = false, length = 60)
    private String purchaseOrderNumber;

    @Column(name = "supplier_invoice_number", nullable = false, length = 100)
    private String supplierInvoiceNumber;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, length = 20)
    private String status = "RECEIVED";

    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceivedNoteItem> items = new ArrayList<>();

    public GoodsReceivedNote() {}

    public void addItem(GoodsReceivedNoteItem item) {
        items.add(item);
        item.setGrn(this);
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getGrnNumber() { return grnNumber; }
    public void setGrnNumber(String grnNumber) { this.grnNumber = grnNumber; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
    public void setPurchaseOrderNumber(String purchaseOrderNumber) { this.purchaseOrderNumber = purchaseOrderNumber; }

    public String getSupplierInvoiceNumber() { return supplierInvoiceNumber; }
    public void setSupplierInvoiceNumber(String supplierInvoiceNumber) { this.supplierInvoiceNumber = supplierInvoiceNumber; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<GoodsReceivedNoteItem> getItems() { return items; }
}
