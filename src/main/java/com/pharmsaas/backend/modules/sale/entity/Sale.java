package com.pharmsaas.backend.modules.sale.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.store.entity.Store;
import com.pharmsaas.backend.modules.salesman.entity.Salesman;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "tenant_sales",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sale_tenant_invoice",
        columnNames = {"tenant_id", "invoice_number"}
    ),
    indexes = @Index(
        name = "idx_sale_tenant_created",
        columnList = "tenant_id, created_at"
    )
)
public class Sale extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesman_id")
    private Salesman salesman;

    @Column(name = "commission_rate_snapshot", nullable = false, precision = 6, scale = 2)
    private BigDecimal commissionRateSnapshot = BigDecimal.ZERO;

    @Column(name = "commission_base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionBaseAmount = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "invoice_number", nullable = false, length = 60)
    private String invoiceNumber;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discount;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "amount_received", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountReceived;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal changeAmount;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    public Sale() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public Salesman getSalesman() { return salesman; }
    public void setSalesman(Salesman salesman) { this.salesman = salesman; }

    public BigDecimal getCommissionRateSnapshot() { return commissionRateSnapshot; }
    public void setCommissionRateSnapshot(BigDecimal value) {
        this.commissionRateSnapshot = value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal getCommissionBaseAmount() { return commissionBaseAmount; }
    public void setCommissionBaseAmount(BigDecimal value) {
        this.commissionBaseAmount = value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal value) {
        this.commissionAmount = value == null ? BigDecimal.ZERO : value;
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getAmountReceived() { return amountReceived; }
    public void setAmountReceived(BigDecimal amountReceived) { this.amountReceived = amountReceived; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
