package com.pharmsaas.backend.modules.inventory.entity;

import com.pharmsaas.backend.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "tenant_inventory_batches",
    indexes = {
        @Index(
            name = "idx_batch_item_expiry",
            columnList = "inventory_item_id,expiry_date"
        ),
        @Index(
            name = "idx_batch_tenant_expiry",
            columnList = "tenant_id,expiry_date"
        )
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_batch_item_number",
            columnNames = {"inventory_item_id", "batch_number"}
        )
    }
)
public class TenantInventoryBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private com.pharmsaas.backend.modules.tenant.entity.Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private TenantInventoryItem inventoryItem;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public TenantInventoryBatch() {}

    public com.pharmsaas.backend.modules.tenant.entity.Tenant getTenant() {
        return tenant;
    }

    public void setTenant(
            com.pharmsaas.backend.modules.tenant.entity.Tenant tenant) {
        this.tenant = tenant;
    }

    public TenantInventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(TenantInventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
