package com.pharmsaas.backend.modules.inventory.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StoreInventoryStockResponse(
    Long id,
    Long tenantId,
    Long storeId,
    String storeName,
    Long inventoryItemId,
    String medicineName,
    String sku,
    Long batchId,
    String batchNumber,
    LocalDate expiryDate,
    Integer quantity,
    String batchStatus,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
