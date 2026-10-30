package com.pharmsaas.backend.modules.subscription.controller;

import com.pharmsaas.backend.modules.subscription.dto.PaymentStatusResponse;
import com.pharmsaas.backend.modules.subscription.service.PublicPaymentStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/subscription/payments")
public class PublicPaymentStatusController {

    private final PublicPaymentStatusService statusService;

    public PublicPaymentStatusController(
        PublicPaymentStatusService statusService
    ) {
        this.statusService = statusService;
    }

    @GetMapping("/{reference}/status")
    public ResponseEntity<PaymentStatusResponse> getStatus(
        @PathVariable String reference
    ) {
        return ResponseEntity.ok(statusService.getStatus(reference));
    }
}
