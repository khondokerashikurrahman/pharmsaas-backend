package com.pharmsaas.backend.modules.sale.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SaleResponse(
    Long id,
    String invoiceNumber,
    String customerName,
    String paymentMethod,
    BigDecimal subtotal,
    BigDecimal discount,
    String discountType,
    BigDecimal discountValue,
    BigDecimal tax,
    BigDecimal total,
    BigDecimal amountReceived,
    BigDecimal changeAmount,
    String status,
    OffsetDateTime createdAt,
    Long cashierId,
    String cashierName,
    Long storeId,
    String storeName,
    String storeCode,
    List<Item> items
) {
    public record Item(
        Long productId,
        String productName,
        String sku,
        Long batchId,
        String batchNumber,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}
