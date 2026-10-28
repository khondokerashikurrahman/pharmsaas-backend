package com.pharmsaas.backend.modules.purchaseorder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
    String supplierName,
    Long branchId,
    LocalDate orderDate,
    LocalDate expectedDeliveryDate,
    BigDecimal discount,
    BigDecimal tax,
    String notes,
    List<ItemRequest> items
) {
    public record ItemRequest(
        Long inventoryItemId,
        String medicineName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal tax
    ) {}
}
