package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.store.service.StoreService;
import com.pharmsaas.backend.modules.subscription.entity.PendingRegistration;
import com.pharmsaas.backend.modules.subscription.entity.Subscription;
import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPayment;
import com.pharmsaas.backend.modules.subscription.repository.PendingRegistrationRepository;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPaymentRepository;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.user.entity.SystemRole;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaidRegistrationCompletionService {

    private final SubscriptionPaymentRepository paymentRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StoreService storeService;

    public PaidRegistrationCompletionService(
            SubscriptionPaymentRepository paymentRepository,
            PendingRegistrationRepository pendingRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            SubscriptionRepository subscriptionRepository,
            StoreService storeService) {

        this.paymentRepository = paymentRepository;
        this.pendingRepository = pendingRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.storeService = storeService;
    }

    /**
     * This method must only be called after the payment gateway response
     * has been verified server-to-server.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Tenant completeAfterVerifiedPayment(String transactionReference) {

        if (transactionReference == null || transactionReference.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment transaction reference is required."
            );
        }

        SubscriptionPayment payment = paymentRepository
                .findByTransactionReferenceForUpdate(transactionReference.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment transaction was not found."
                ));

        // Idempotency: do not create a second tenant for a completed payment.
        if (payment.getSubscription() != null) {
            return payment.getSubscription().getTenant();
        }

        if (!"VALIDATED".equalsIgnoreCase(payment.getStatus())
                || payment.getVerifiedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has not been verified by the payment gateway."
            );
        }

        PendingRegistration pending = pendingRepository
                .findById(payment.getPendingRegistration().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pending registration was not found."
                ));

        OffsetDateTime now = OffsetDateTime.now();

        if (!pending.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Email verification is required."
            );
        }

        if (!"PENDING_PAYMENT".equalsIgnoreCase(pending.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Registration is not awaiting payment completion."
            );
        }

        if (pending.getExpiresAt() == null || !pending.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Registration has expired. Please register again."
            );
        }

        if (userRepository.existsByEmail(pending.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        if (payment.getAmount() == null
                || payment.getAmount().compareTo(pending.getPlan().getPrice()) != 0
                || !pending.getPlan().getCurrency().equalsIgnoreCase(payment.getCurrency())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment amount or currency does not match the selected plan."
            );
        }

        Tenant tenant = new Tenant();
        tenant.setTenantCode(generateTenantCode(pending.getPharmacyName()));
        tenant.setName(pending.getPharmacyName());
        tenant.setOwnerName(pending.getOwnerName());
        tenant.setEmail(pending.getEmail());
        tenant.setPhone(pending.getPhone());
        tenant.setAddress(pending.getAddress());
        tenant.setStatus("Pending");

        Tenant savedTenant = tenantRepository.save(tenant);

        User owner = new User();
        owner.setFullName(pending.getOwnerName());
        owner.setEmail(pending.getEmail());
        owner.setPasswordHash(pending.getPasswordHash());
        owner.setEnabled(true);
        owner.setSystemRole(SystemRole.USER);
        owner.setTenant(savedTenant);

        User savedOwner = userRepository.save(owner);

        TenantMembership membership = new TenantMembership();
        membership.setTenant(savedTenant);
        membership.setUser(savedOwner);
        membership.setRole(MembershipRole.TENANT_OWNER);
        membership.setActive(true);
        membershipRepository.save(membership);

        storeService.ensureDefaultStore(savedTenant);

        Subscription subscription = new Subscription();
        subscription.setTenant(savedTenant);
        subscription.setPlan(pending.getPlan());
        subscription.setStartsAt(now);
        subscription.setEndsAt(now.plusDays(pending.getPlan().getDurationDays()));
        subscription.setStatus("PENDING_APPROVAL");
        subscription.setAutoRenew(false);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        payment.setSubscription(savedSubscription);
        paymentRepository.save(payment);

        pending.setStatus("COMPLETED");
        pending.setCompletedAt(now);
        pendingRepository.save(pending);

        return savedTenant;
    }

    private String generateTenantCode(String pharmacyName) {

        String base = pharmacyName == null ? "PHARMACY" :
                pharmacyName.trim()
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");

        if (base.isBlank()) {
            base = "PHARMACY";
        }

        if (base.length() > 18) {
            base = base.substring(0, 18);
        }

        String code;
        do {
            String suffix = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);

            code = base + "-" + suffix;
        } while (tenantRepository.existsByTenantCode(code));

        return code;
    }
}
