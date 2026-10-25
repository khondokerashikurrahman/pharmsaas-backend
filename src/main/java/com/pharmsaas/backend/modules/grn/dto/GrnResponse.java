package com.pharmsaas.backend.modules.grn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record GrnResponse(
    Long id,
    Long tenantId,
    String grnNumber,
    String supplierName,
    String purchaseOrderNumber,
    String supplierInvoiceNumber,
    LocalDate receivedDate,
    String notes,
    String status,
    OffsetDateTime createdAt,
    List<Item> items
) {
    public record Item(
        Long id,
        Long inventoryItemId,
        String medicineName,
        Long inventoryBatchId,
        String batchNumber,
        LocalDate manufactureDate,
        LocalDate expiryDate,
        Integer quantity,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        BigDecimal lineTotal
    ) {}
}
