package com.pharmsaas.backend.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryBatchCreateRequest(
    Long inventoryItemId,
    String batchNumber,
    LocalDate manufactureDate,
    LocalDate expiryDate,
    Integer quantity,
    BigDecimal purchasePrice,
    BigDecimal sellingPrice
) {}
