package com.pharmsaas.backend.modules.branchtransfer.dto;

import java.util.List;

public record BranchTransferCreateRequest(
    Long sourceStoreId,
    Long destinationStoreId,
    String reason,
    List<Item> items
) {
    public record Item(Long batchId, Integer quantity) {}
}
