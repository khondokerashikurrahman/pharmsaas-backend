package com.pharmsaas.backend.modules.inventory.dto;

import java.math.BigDecimal;

public record InventoryItemRequest(
    String name,
    String genericName,
    String brandName,
    String strength,
    String dosageForm,
    String category,
    String sku,
    String barcode,
    BigDecimal purchasePrice,
    BigDecimal sellingPrice,
    Integer stockQuantity,
    Integer reorderLevel,
    String status,
    String description,
    String productType,
    Long categoryId,
    Long genericId,
    Long dosageFormId,
    Long companyId
) {
    // Backwards-compatible constructor for existing API clients.
    public InventoryItemRequest(
        String name, String genericName, String brandName,
        String strength, String dosageForm, String category,
        String sku, String barcode, BigDecimal purchasePrice,
        BigDecimal sellingPrice, Integer stockQuantity,
        Integer reorderLevel, String status, String description,
        String productType
    ) {
        this(name, genericName, brandName, strength, dosageForm, category,
            sku, barcode, purchasePrice, sellingPrice, stockQuantity,
            reorderLevel, status, description, productType,
            null, null, null, null);
    }

    public InventoryItemRequest(
        String name, String genericName, String brandName,
        String strength, String dosageForm, String category,
        String sku, String barcode, BigDecimal purchasePrice,
        BigDecimal sellingPrice, Integer stockQuantity,
        Integer reorderLevel, String status, String description
    ) {
        this(name, genericName, brandName, strength, dosageForm, category,
            sku, barcode, purchasePrice, sellingPrice, stockQuantity,
            reorderLevel, status, description, "MEDICINE",
            null, null, null, null);
    }
}
