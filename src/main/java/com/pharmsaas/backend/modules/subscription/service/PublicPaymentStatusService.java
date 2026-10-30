package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.dto.PaymentStatusResponse;
import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPayment;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicPaymentStatusService {

    private final SubscriptionPaymentRepository paymentRepository;

    public PublicPaymentStatusService(
        SubscriptionPaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String reference) {

        if (reference == null || reference.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Payment reference is required"
            );
        }

        SubscriptionPayment payment = paymentRepository
            .findByTransactionReference(reference.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment transaction not found"
            ));

        String planName = payment.getPendingRegistration() != null
            && payment.getPendingRegistration().getPlan() != null
            ? payment.getPendingRegistration().getPlan().getName()
            : null;

        return new PaymentStatusResponse(
            payment.getTransactionReference(),
            payment.getStatus(),
            payment.getAmount(),
            payment.getCurrency(),
            planName,
            payment.getVerifiedAt(),
            payment.getSubscription() != null
        );
    }
}
