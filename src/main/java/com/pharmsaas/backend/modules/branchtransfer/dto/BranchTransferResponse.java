package com.pharmsaas.backend.modules.branchtransfer.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record BranchTransferResponse(
    Long id,
    Long tenantId,
    String transferNumber,
    Long sourceStoreId,
    String sourceStoreName,
    Long destinationStoreId,
    String destinationStoreName,
    String status,
    String reason,
    String actionNote,
    Long requestedBy,
    Long approvedBy,
    Long rejectedBy,
    Long dispatchedBy,
    Long receivedBy,
    OffsetDateTime createdAt,
    OffsetDateTime approvedAt,
    OffsetDateTime rejectedAt,
    OffsetDateTime dispatchedAt,
    OffsetDateTime receivedAt,
    List<Item> items
) {
    public record Item(
        Long id,
        Long inventoryItemId,
        String medicineName,
        String sku,
        Long batchId,
        String batchNumber,
        LocalDate expiryDate,
        Integer quantity,
        Integer receivedQuantity,
        Integer damagedQuantity
    ) {}
}
