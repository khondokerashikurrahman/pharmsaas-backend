package com.pharmsaas.backend.modules.inventory.dto;

public record StockRequestCreate(
    Long inventoryItemId,
    String movementType,
    Integer quantity,
    String reason
) {}
