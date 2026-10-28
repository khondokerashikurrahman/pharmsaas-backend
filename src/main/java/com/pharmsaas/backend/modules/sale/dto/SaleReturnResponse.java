package com.pharmsaas.backend.modules.sale.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SaleReturnResponse(
    Long id,
    String refundNumber,
    Long saleId,
    String invoiceNumber,
    String customerName,
    String reason,
    BigDecimal refundAmount,
    String status,
    OffsetDateTime createdAt,
    Long cashierId,
    String cashierName,
    List<Item> items
) {
    public record Item(
        Long returnItemId,
        Long productId,
        String productName,
        Long batchId,
        String batchNumber,
        Integer quantity,
        String stockDisposition,
        BigDecimal refundAmount
    ) {}
}
