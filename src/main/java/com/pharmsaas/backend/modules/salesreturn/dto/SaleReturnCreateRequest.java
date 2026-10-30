package com.pharmsaas.backend.modules.salesreturn.dto;

import java.util.List;

public record SaleReturnCreateRequest(
    String requestKey,
    String reason,
    List<ReturnLineRequest> items
) {
    public record ReturnLineRequest(
        Long batchId,
        Integer quantity
    ) {}
}
