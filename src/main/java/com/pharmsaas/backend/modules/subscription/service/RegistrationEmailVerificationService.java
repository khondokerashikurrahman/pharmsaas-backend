package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.dto.EmailVerificationResponse;
import com.pharmsaas.backend.modules.subscription.entity.PendingRegistration;
import com.pharmsaas.backend.modules.subscription.repository.PendingRegistrationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationEmailVerificationService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_OTP_SENDS = 5;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final PendingRegistrationRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RestClient restClient = RestClient.create();

    @Value("${app.email.brevo.enabled:false}")
    private boolean brevoEnabled;

    @Value("${app.email.brevo.api-key:}")
    private String apiKey;

    @Value("${app.email.brevo.sender-email:}")
    private String senderEmail;

    @Value("${app.email.brevo.sender-name:PharmSaaS}")
    private String senderName;

    public RegistrationEmailVerificationService(
            PendingRegistrationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EmailVerificationResponse sendVerification(String reference) {
        PendingRegistration registration = getRegistration(reference);
        OffsetDateTime now = OffsetDateTime.now();

        if (registration.isEmailVerified()) {
            return new EmailVerificationResponse(
                    true, true, registration.getStatus(),
                    "Email is already verified."
            );
        }

        if (!"PENDING_EMAIL_VERIFICATION".equals(registration.getStatus())
                || !registration.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "Registration has expired or is no longer awaiting email verification."
            );
        }

        if (registration.getEmailVerificationLastSentAt() != null
                && registration.getEmailVerificationLastSentAt()
                .plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait 60 seconds before requesting another OTP."
            );
        }

        ensureBrevoConfigured();

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        registration.setEmailVerificationTokenHash(hash(otp));
        registration.setEmailVerificationExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        registration.setEmailVerificationLastSentAt(now);
        registration.setEmailVerificationAttempts(0);
        registration.setEmailVerificationSendCount(
                registration.getEmailVerificationSendCount() + 1
        );
        repository.save(registration);

        sendBrevoEmail(registration.getEmail(), registration.getOwnerName(), otp);

        return new EmailVerificationResponse(
                true, false, registration.getStatus(),
                "Verification code sent. It expires in 5 minutes."
        );
    }

    @Transactional
    public EmailVerificationResponse verifyEmail(
            String reference, String submittedOtp) {

        PendingRegistration registration = getRegistration(reference);
        OffsetDateTime now = OffsetDateTime.now();

        if (registration.isEmailVerified()) {
            return new EmailVerificationResponse(
                    true, true, registration.getStatus(),
                    "Email is already verified."
            );
        }

        if (!"PENDING_EMAIL_VERIFICATION".equals(registration.getStatus())
                || !registration.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "Registration has expired or is no longer awaiting verification."
            );
        }

        if (registration.getEmailVerificationTokenHash() == null
                || registration.getEmailVerificationExpiresAt() == null
                || !registration.getEmailVerificationExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "OTP has expired. Please request a new code."
            );
        }

        if (registration.getEmailVerificationAttempts() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum verification attempts reached. Request a new OTP."
            );
        }

        String submittedHash = hash(submittedOtp);
        String savedHash = registration.getEmailVerificationTokenHash();

        if (!MessageDigest.isEqual(
                savedHash.getBytes(StandardCharsets.UTF_8),
                submittedHash.getBytes(StandardCharsets.UTF_8))) {

            int attempts = registration.getEmailVerificationAttempts() + 1;
            registration.setEmailVerificationAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                registration.setEmailVerificationTokenHash(null);
                registration.setEmailVerificationExpiresAt(null);
            }

            repository.save(registration);

            return new EmailVerificationResponse(
                    false, false, registration.getStatus(),
                    attempts >= MAX_ATTEMPTS
                            ? "Maximum attempts reached. Request a new OTP."
                            : "Incorrect OTP. Attempts remaining: " + (MAX_ATTEMPTS - attempts)
            );
        }

        registration.setEmailVerified(true);
        registration.setStatus("PENDING_PAYMENT");
        registration.setEmailVerificationTokenHash(null);
        registration.setEmailVerificationExpiresAt(null);
        registration.setEmailVerificationAttempts(0);
        repository.save(registration);

        return new EmailVerificationResponse(
                true, true, registration.getStatus(),
                "Email verified successfully. You may now proceed to payment."
        );
    }

    private PendingRegistration getRegistration(String reference) {
        return repository.findByRegistrationReferenceForUpdate(reference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Registration not found."
                ));
    }

    private void ensureBrevoConfigured() {
        if (!brevoEnabled || apiKey == null || apiKey.isBlank()
                || senderEmail == null || senderEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification service is not configured."
            );
        }
    }

    private void sendBrevoEmail(String email, String ownerName, String otp) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto;padding:24px;color:#172033">
                  <h2>Verify your PharmSaaS email</h2>
                  <p>Hello %s,</p>
                  <p>Use the verification code below to verify your email address:</p>
                  <div style="font-size:32px;font-weight:bold;letter-spacing:8px;padding:18px;background:#f2f5f9;text-align:center;border-radius:8px">%s</div>
                  <p>This code expires in 5 minutes. Do not share this code with anyone.</p>
                  <p>If you did not request this registration, you can ignore this email.</p>
                  <p>PharmSaaS Team</p>
                </div>
                """.formatted(escapeHtml(ownerName), otp);

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", email)),
                "subject", "Your PharmSaaS email verification code",
                "htmlContent", html
        );

        try {
            restClient.post()
                    .uri("https://api.brevo.com/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not send verification email. Please try again later."
            );
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
