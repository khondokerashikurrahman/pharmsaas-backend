package com.pharmsaas.backend.modules.branchtransfer.dto;

import java.util.List;

public record BranchTransferActionRequest(
    String note,
    List<ReceiptItem> items
) {
    public record ReceiptItem(
        Long transferItemId,
        Integer receivedQuantity
    ) {}
}
