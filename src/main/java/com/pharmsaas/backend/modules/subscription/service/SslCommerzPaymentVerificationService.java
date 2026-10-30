package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPayment;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class SslCommerzPaymentVerificationService {

    private final RestClient restClient;
    private final SubscriptionPaymentRepository paymentRepository;
    private final PaidRegistrationCompletionService completionService;

    @Value("${app.payment.sslcommerz.enabled:false}")
    private boolean enabled;

    @Value("${app.payment.sslcommerz.sandbox:true}")
    private boolean sandbox;

    @Value("${app.payment.sslcommerz.store-id:}")
    private String storeId;

    @Value("${app.payment.sslcommerz.store-password:}")
    private String storePassword;

    public SslCommerzPaymentVerificationService(
        SubscriptionPaymentRepository paymentRepository,
        PaidRegistrationCompletionService completionService
    ) {
        this.restClient = RestClient.create();
        this.paymentRepository = paymentRepository;
        this.completionService = completionService;
    }

    public String verifyAndComplete(String transactionReference, String validationId) {
        if (!enabled || storeId.isBlank() || storePassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment gateway is not configured");
        }

        if (transactionReference == null || transactionReference.isBlank()
            || validationId == null || validationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction reference and validation ID are required");
        }

        SubscriptionPayment payment = paymentRepository
            .findByTransactionReference(transactionReference.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment transaction not found"));

        if (payment.getSubscription() != null) {
            return "ALREADY_COMPLETED";
        }

        String gatewayBase = sandbox
            ? "https://sandbox.sslcommerz.com"
            : "https://securepay.sslcommerz.com";

        String validationUrl = UriComponentsBuilder
            .fromUriString(gatewayBase + "/validator/api/validationserverAPI.php")
            .queryParam("val_id", validationId)
            .queryParam("store_id", storeId)
            .queryParam("store_passwd", storePassword)
            .queryParam("format", "json")
            .build()
            .encode()
            .toUriString();

        Map<?, ?> result;
        try {
            result = restClient.get().uri(validationUrl).retrieve().body(Map.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify payment with SSLCommerz");
        }

        if (result == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from SSLCommerz");
        }

        String gatewayStatus = value(result, "status");
        String gatewayTransaction = value(result, "tran_id");
        String gatewayCurrency = value(result, "currency");
        String riskLevel = value(result, "risk_level");
        String returnedValidationId = value(result, "val_id");
        String gatewayAmount = value(result, "amount");
        String bankTransactionId = value(result, "bank_tran_id");

        boolean validStatus = "VALID".equalsIgnoreCase(gatewayStatus)
            || "VALIDATED".equalsIgnoreCase(gatewayStatus);

        BigDecimal returnedAmount;
        try {
            returnedAmount = new BigDecimal(gatewayAmount == null ? "0" : gatewayAmount);
        } catch (NumberFormatException ex) {
            returnedAmount = BigDecimal.ZERO;
        }

        if (!validStatus
            || !payment.getTransactionReference().equals(gatewayTransaction)
            || !payment.getCurrency().equalsIgnoreCase(gatewayCurrency)
            || payment.getAmount().compareTo(returnedAmount) != 0
            || !validationId.equals(returnedValidationId)) {
            payment.setStatus("VALIDATION_FAILED");
            paymentRepository.save(payment);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gateway verification did not match this payment");
        }

        payment.setGatewayValidationId(returnedValidationId);
        payment.setGatewayRiskLevel(riskLevel);
        payment.setGatewayTransactionId(bankTransactionId);

        if ("1".equals(riskLevel)) {
            payment.setStatus("REVIEW_REQUIRED");
            paymentRepository.save(payment);
            return "REVIEW_REQUIRED";
        }

        payment.setStatus("VALIDATED");
        payment.setVerifiedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        try {
            completionService.completeAfterVerifiedPayment(payment.getTransactionReference());
            return "COMPLETED";
        } catch (Exception ex) {
            return "VERIFIED_PENDING_COMPLETION";
        }
    }

    @Transactional
    public void markUnverifiedPaymentOutcome(String transactionReference, String outcome) {
        if (transactionReference == null || transactionReference.isBlank()) {
            return;
        }

        if (!"FAILED".equals(outcome) && !"CANCELLED".equals(outcome)) {
            throw new IllegalArgumentException("Unsupported payment outcome");
        }

        SubscriptionPayment payment = paymentRepository
            .findByTransactionReferenceForUpdate(transactionReference.trim())
            .orElse(null);

        if (payment == null || payment.getSubscription() != null) {
            return;
        }

        if ("INITIATED".equals(payment.getStatus())) {
            payment.setStatus(outcome);
            paymentRepository.save(payment);
        }
    }

    private String value(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
