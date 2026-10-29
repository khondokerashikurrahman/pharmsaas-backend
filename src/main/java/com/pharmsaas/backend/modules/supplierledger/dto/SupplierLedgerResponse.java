package com.pharmsaas.backend.modules.supplierledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SupplierLedgerResponse(
    Long supplierId,
    String supplierCode,
    String supplierName,
    BigDecimal openingBalance,
    BigDecimal totalPurchases,
    BigDecimal totalPayments,
    BigDecimal outstandingBalance,
    List<Transaction> transactions
) {
    public record Transaction(
        String id,
        LocalDate date,
        String type,
        String reference,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal balance,
        String status
    ) {}
}
