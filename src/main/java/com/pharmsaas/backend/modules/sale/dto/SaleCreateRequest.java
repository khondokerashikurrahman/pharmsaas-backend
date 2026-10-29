package com.pharmsaas.backend.modules.sale.dto;

import java.math.BigDecimal;
import java.util.List;

public record SaleCreateRequest(
    String customerName,
    Long storeId,
    String paymentMethod,
    String discountType,
    BigDecimal discountValue,
    BigDecimal taxRate,
    BigDecimal amountReceived,
    List<SaleLineRequest> items
) {
    public record SaleLineRequest(
        Long productId,
        Long batchId,
        Integer quantity,
        BigDecimal discountAmount
    ) {
        public SaleLineRequest(Long productId, Long batchId, Integer quantity) {
            this(productId, batchId, quantity, BigDecimal.ZERO);
        }
    }
}
