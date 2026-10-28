package com.pharmsaas.backend.modules.inventory.dto;

public record StockAdjustmentRequest(
    Long batchId,
    Integer quantityChange,
    String reason
) {}
