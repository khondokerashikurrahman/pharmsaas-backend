package com.pharmsaas.backend.modules.inventory.dto;

import java.time.OffsetDateTime;

public record StockRequestResponse(
    Long id,
    Long tenantId,
    Long inventoryItemId,
    String medicineName,
    String sku,
    Long requesterUserId,
    Long approverUserId,
    String movementType,
    Integer quantity,
    String reason,
    String status,
    String decisionNote,
    OffsetDateTime createdAt,
    OffsetDateTime decidedAt
) {}
