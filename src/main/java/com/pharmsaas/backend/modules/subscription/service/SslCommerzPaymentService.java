package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.dto.PaymentInitiationResponse;
import com.pharmsaas.backend.modules.subscription.entity.PendingRegistration;
import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPayment;
import com.pharmsaas.backend.modules.subscription.repository.PendingRegistrationRepository;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class SslCommerzPaymentService {

    private final RestClient restClient;
    private final PendingRegistrationRepository registrationRepository;
    private final SubscriptionPaymentRepository paymentRepository;

    @Value("${app.payment.sslcommerz.enabled:false}")
    private boolean enabled;

    @Value("${app.payment.sslcommerz.sandbox:true}")
    private boolean sandbox;

    @Value("${app.payment.sslcommerz.store-id:}")
    private String storeId;

    @Value("${app.payment.sslcommerz.store-password:}")
    private String storePassword;

    @Value("${app.payment.sslcommerz.backend-url:http://localhost:9090}")
    private String backendUrl;

    @Value("${app.payment.sslcommerz.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public SslCommerzPaymentService(
        PendingRegistrationRepository registrationRepository,
        SubscriptionPaymentRepository paymentRepository
    ) {
        this.restClient = RestClient.create();
        this.registrationRepository = registrationRepository;
        this.paymentRepository = paymentRepository;
    }

    public PaymentInitiationResponse initiatePayment(String registrationReference) {
        if (!enabled || storeId.isBlank() || storePassword.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SSLCommerz payment is not configured"
            );
        }

        PendingRegistration registration = registrationRepository
            .findByRegistrationReference(registrationReference)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Registration not found"
            ));

        OffsetDateTime now = OffsetDateTime.now();

        if (!registration.isEmailVerified()
            || !"PENDING_PAYMENT".equalsIgnoreCase(registration.getStatus())
            || registration.getExpiresAt() == null
            || !registration.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Registration is not eligible for payment"
            );
        }

        SubscriptionPayment recentPayment = paymentRepository
            .findFirstByPendingRegistration_RegistrationReferenceAndStatusOrderByIdDesc(
                registrationReference, "INITIATED"
            )
            .orElse(null);

        if (recentPayment != null
            && recentPayment.getGatewayUrl() != null
            && !recentPayment.getGatewayUrl().isBlank()
            && recentPayment.getCreatedAt() != null
            && recentPayment.getCreatedAt().isAfter(now.minusMinutes(10))) {
            return new PaymentInitiationResponse(
                recentPayment.getTransactionReference(),
                recentPayment.getGatewayUrl(),
                recentPayment.getAmount(),
                recentPayment.getCurrency(),
                recentPayment.getStatus()
            );
        }

        registration.setExpiresAt(now.plusHours(2));
        registrationRepository.save(registration);

        BigDecimal amount = registration.getPlan().getPrice();
        String currency = registration.getPlan().getCurrency();

        if (amount == null || amount.compareTo(BigDecimal.TEN) < 0
            || amount.compareTo(new BigDecimal("500000")) > 0
            || !"BDT".equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid subscription amount or currency"
            );
        }

        String transactionReference = "PHARM" +
            UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setPendingRegistration(registration);
        payment.setTransactionReference(transactionReference);
        payment.setAmount(amount);
        payment.setCurrency(currency.toUpperCase());
        payment.setStatus("INITIATED");
        paymentRepository.save(payment);

        String gatewayBase = sandbox
            ? "https://sandbox.sslcommerz.com"
            : "https://securepay.sslcommerz.com";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", storeId);
        form.add("store_passwd", storePassword);
        form.add("total_amount", amount.toPlainString());
        form.add("currency", currency.toUpperCase());
        form.add("tran_id", transactionReference);
        form.add("success_url", trimSlash(backendUrl) + "/api/public/subscription/payments/callback/success");
        form.add("fail_url", trimSlash(backendUrl) + "/api/public/subscription/payments/callback/fail");
        form.add("cancel_url", trimSlash(backendUrl) + "/api/public/subscription/payments/callback/cancel");
        form.add("ipn_url", trimSlash(backendUrl) + "/api/public/subscription/payments/ipn");
        form.add("product_name", registration.getPlan().getName());
        form.add("product_category", "Software Subscription");
        form.add("product_profile", "non-physical-goods");
        form.add("cus_name", registration.getOwnerName());
        form.add("cus_email", registration.getEmail());
        form.add("cus_phone", registration.getPhone() == null ? "01700000000" : registration.getPhone());
        form.add("cus_add1", registration.getAddress() == null ? "Bangladesh" : registration.getAddress());
        form.add("cus_city", "Dhaka");
        form.add("cus_country", "Bangladesh");
        form.add("shipping_method", "NO");
        form.add("num_of_item", "1");
        form.add("value_a", registrationReference);

        try {
            Map<?, ?> response = restClient.post()
                .uri(gatewayBase + "/gwprocess/v4/api.php")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

            if (response == null || !"SUCCESS".equalsIgnoreCase(
                String.valueOf(response.get("status")))) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "SSLCommerz could not create a payment session"
                );
            }

            Object gatewayUrlValue = response.get("GatewayPageURL");
            if (gatewayUrlValue == null) {
                gatewayUrlValue = response.get("redirectGatewayURL");
            }

            String gatewayUrl = gatewayUrlValue == null ? "" : gatewayUrlValue.toString();

            String expectedHost = sandbox
                ? "https://sandbox.sslcommerz.com/"
                : "https://securepay.sslcommerz.com/";

            if (!gatewayUrl.startsWith(expectedHost)) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid payment gateway URL received"
                );
            }

            payment.setGatewayUrl(gatewayUrl);

            Object sessionKey = response.get("sessionkey");
            if (sessionKey != null) {
                payment.setGatewaySessionId(sessionKey.toString());
            }

            paymentRepository.save(payment);

            return new PaymentInitiationResponse(
                transactionReference,
                gatewayUrl,
                amount,
                currency.toUpperCase(),
                payment.getStatus()
            );

        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Unable to connect to SSLCommerz"
            );
        }
    }

    private String trimSlash(String value) {
        return value.endsWith("/")
            ? value.substring(0, value.length() - 1)
            : value;
    }
}
