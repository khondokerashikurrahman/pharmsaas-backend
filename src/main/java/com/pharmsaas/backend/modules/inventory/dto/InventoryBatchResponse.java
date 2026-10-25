package com.pharmsaas.backend.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryBatchResponse(
    Long id,
    Long tenantId,
    Long inventoryItemId,
    String medicineName,
    String batchNumber,
    LocalDate manufactureDate,
    LocalDate expiryDate,
    Integer quantity,
    BigDecimal purchasePrice,
    String status
) {}
