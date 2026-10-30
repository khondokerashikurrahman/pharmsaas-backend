package com.pharmsaas.backend.modules.subscription.controller;

import com.pharmsaas.backend.modules.subscription.service.SslCommerzPaymentVerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/public/subscription/payments")
public class SslCommerzCallbackController {

    private final SslCommerzPaymentVerificationService verificationService;

    @Value("${app.payment.sslcommerz.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public SslCommerzCallbackController(SslCommerzPaymentVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/callback/success")
    public RedirectView success(
        @RequestParam(name = "tran_id", required = false) String transactionReference,
        @RequestParam(name = "val_id", required = false) String validationId
    ) {
        String result = "verification-failed";
        try {
            String status = verificationService.verifyAndComplete(transactionReference, validationId);
            result = status.equals("COMPLETED") || status.equals("ALREADY_COMPLETED")
                ? "success"
                : status.equals("REVIEW_REQUIRED") ? "review"
                : status.equals("VERIFIED_PENDING_COMPLETION") ? "pending"
                : "verification-failed";
        } catch (Exception ignored) {
            result = "verification-failed";
        }
        return redirect(result, transactionReference);
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> ipn(
        @RequestParam(name = "tran_id", required = false) String transactionReference,
        @RequestParam(name = "val_id", required = false) String validationId
    ) {
        try {
            verificationService.verifyAndComplete(transactionReference, validationId);
            return ResponseEntity.ok("OK");
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body("NOT_VERIFIED");
        }
    }

    @PostMapping("/callback/fail")
    public RedirectView fail(@RequestParam(name = "tran_id", required = false) String reference) {
        verificationService.markUnverifiedPaymentOutcome(reference, "FAILED");
        return redirect("failed", reference);
    }

    @PostMapping("/callback/cancel")
    public RedirectView cancel(@RequestParam(name = "tran_id", required = false) String reference) {
        verificationService.markUnverifiedPaymentOutcome(reference, "CANCELLED");
        return redirect("cancelled", reference);
    }

    private RedirectView redirect(String result, String reference) {
        String url = frontendUrl.replaceAll("/+$", "") + "/payment-success?result=" + result;
        if (reference != null && !reference.isBlank()) {
            url += "&reference=" + java.net.URLEncoder.encode(reference, java.nio.charset.StandardCharsets.UTF_8);
        }
        return new RedirectView(url);
    }
}
