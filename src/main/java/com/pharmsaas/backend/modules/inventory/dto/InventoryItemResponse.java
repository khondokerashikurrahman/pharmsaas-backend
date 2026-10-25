package com.pharmsaas.backend.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record InventoryItemResponse(
    Long id,
    Long tenantId,
    String name,
    String genericName,
    String brandName,
    String strength,
    String dosageForm,
    String sku,
    String barcode,
    BigDecimal purchasePrice,
    BigDecimal sellingPrice,
    Integer stockQuantity,
    Integer reorderLevel,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
