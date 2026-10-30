package com.pharmsaas.backend.modules.refund.dto;

public record RefundProcessRequest(
    String transactionReference,
    String notes
) {}
