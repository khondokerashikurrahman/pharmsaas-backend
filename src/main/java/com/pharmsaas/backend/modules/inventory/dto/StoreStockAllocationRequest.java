package com.pharmsaas.backend.modules.inventory.dto;

public record StoreStockAllocationRequest(
    Long batchId,
    Integer quantity
) {}
