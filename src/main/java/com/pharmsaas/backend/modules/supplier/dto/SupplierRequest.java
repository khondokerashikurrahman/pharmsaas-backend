package com.pharmsaas.backend.modules.supplier.dto;

import java.math.BigDecimal;

public record SupplierRequest(
    String name,
    String companyName,
    String contactPerson,
    String phone,
    String email,
    String address,
    BigDecimal openingBalance,
    BigDecimal creditLimit,
    String status
) {}
