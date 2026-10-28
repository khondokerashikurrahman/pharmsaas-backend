package com.pharmsaas.backend.modules.purchaseorder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PurchaseOrderResponse(
    Long id,
    Long tenantId,
    String orderNumber,
    String supplierName,
    Long branchId,
    LocalDate orderDate,
    LocalDate expectedDeliveryDate,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal tax,
    BigDecimal totalAmount,
    String status,
    String notes,
    Long createdBy,
    Long approvedBy,
    OffsetDateTime approvedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<Item> items,
    Long supplierId
) {
    public PurchaseOrderResponse(
        Long id,
        Long tenantId,
        String orderNumber,
        String supplierName,
        Long branchId,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal totalAmount,
        String status,
        String notes,
        Long createdBy,
        Long approvedBy,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<Item> items
    ) {
        this(id, tenantId, orderNumber, supplierName, branchId, orderDate,
            expectedDeliveryDate, subtotal, discount, tax, totalAmount,
            status, notes, createdBy, approvedBy, approvedAt, createdAt,
            updatedAt, items, null);
    }

    public record Item(
        Long id,
        Long inventoryItemId,
        String medicineName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal total
    ) {}
}
