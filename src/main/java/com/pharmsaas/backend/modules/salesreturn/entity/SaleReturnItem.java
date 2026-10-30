package com.pharmsaas.backend.modules.salesreturn.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.sale.entity.SaleItem;
import com.pharmsaas.backend.modules.inventory.entity.TenantInventoryBatch;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "tenant_sale_return_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sale_return_item_line",
        columnNames = {"sale_return_id", "sale_item_id"}
    )
)
public class SaleReturnItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_return_id", nullable = false)
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_item_id", nullable = false)
    private SaleItem saleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TenantInventoryBatch batch;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "refund_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "commission_reversed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionReversedAmount = BigDecimal.ZERO;

    @Column(name = "stock_disposition", nullable = false, length = 20)
    private String stockDisposition = "QUARANTINED";

    public SaleReturnItem() {}

    public SaleReturn getSaleReturn() { return saleReturn; }
    public void setSaleReturn(SaleReturn saleReturn) { this.saleReturn = saleReturn; }

    public SaleItem getSaleItem() { return saleItem; }
    public void setSaleItem(SaleItem saleItem) { this.saleItem = saleItem; }

    public TenantInventoryBatch getBatch() { return batch; }
    public void setBatch(TenantInventoryBatch batch) { this.batch = batch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getCommissionReversedAmount() {
        return commissionReversedAmount == null ? BigDecimal.ZERO : commissionReversedAmount;
    }
    public void setCommissionReversedAmount(BigDecimal value) {
        this.commissionReversedAmount = value == null ? BigDecimal.ZERO : value;
    }

    public String getStockDisposition() { return stockDisposition; }
    public void setStockDisposition(String stockDisposition) { this.stockDisposition = stockDisposition; }
}
