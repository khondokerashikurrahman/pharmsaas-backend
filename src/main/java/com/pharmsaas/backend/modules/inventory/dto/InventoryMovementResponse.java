package com.pharmsaas.backend.modules.inventory.dto;

import java.time.OffsetDateTime;

public record InventoryMovementResponse(
    Long id,
    Long productId,
    String productName,
    Long batchId,
    String batchNumber,
    String movementType,
    Integer quantityChange,
    Integer quantityAfter,
    String referenceType,
    Long referenceId,
    String referenceNumber,
    String reason,
    Long actorUserId,
    OffsetDateTime createdAt,
    Long storeId,
    String storeName
) {}
