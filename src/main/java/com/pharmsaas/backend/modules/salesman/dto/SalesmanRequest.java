package com.pharmsaas.backend.modules.salesman.dto;

import java.math.BigDecimal;

public record SalesmanRequest(
    String employeeId,
    String name,
    String phone,
    String email,
    String address,
    String territory,
    BigDecimal commissionRate,
    String status
) {}
