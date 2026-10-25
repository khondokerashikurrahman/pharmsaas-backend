package com.pharmsaas.backend.modules.inventory.dto;

import java.math.BigDecimal;

public record InventoryItemRequest(
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
    String status
) {}
