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
    List<Item> items,
    Long supplierId,
    Long purchaseOrderId
) {
    public GrnResponse(
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
        this(id, tenantId, grnNumber, supplierName, purchaseOrderNumber,
            supplierInvoiceNumber, receivedDate, notes, status, createdAt,
            items, null, null);
    }

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
