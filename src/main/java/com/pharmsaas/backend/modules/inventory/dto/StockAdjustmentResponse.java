package com.pharmsaas.backend.modules.inventory.dto;

import java.time.OffsetDateTime;

public record StockAdjustmentResponse(
    Long movementId,
    String adjustmentNumber,
    Long tenantId,
    Long inventoryItemId,
    String medicineName,
    Long batchId,
    String batchNumber,
    Integer quantityChange,
    Integer batchQuantityAfter,
    Integer inventoryQuantityAfter,
    String reason,
    Long actorUserId,
    OffsetDateTime createdAt
) {}
