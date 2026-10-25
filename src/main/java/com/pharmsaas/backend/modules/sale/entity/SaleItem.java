package com.pharmsaas.backend.modules.sale.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryItem;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tenant_sale_items")
public class SaleItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private TenantInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TenantInventoryBatch batch;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    public SaleItem() {}

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public TenantInventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(TenantInventoryItem inventoryItem) { this.inventoryItem = inventoryItem; }

    public TenantInventoryBatch getBatch() { return batch; }
    public void setBatch(TenantInventoryBatch batch) { this.batch = batch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
