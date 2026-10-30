package com.pharmsaas.backend.modules.subscription.controller;

import com.pharmsaas.backend.modules.subscription.dto.PaymentInitiationResponse;
import com.pharmsaas.backend.modules.subscription.service.SslCommerzPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/subscription/payments")
public class PublicSubscriptionPaymentController {

    private final SslCommerzPaymentService paymentService;

    public PublicSubscriptionPaymentController(SslCommerzPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{registrationReference}/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
        @PathVariable String registrationReference
    ) {
        return ResponseEntity.ok(
            paymentService.initiatePayment(registrationReference)
        );
    }
}
