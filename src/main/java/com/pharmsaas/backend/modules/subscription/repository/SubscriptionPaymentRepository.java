package com.pharmsaas.backend.modules.subscription.repository;

import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionPaymentRepository
        extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByTransactionReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SubscriptionPayment p WHERE p.transactionReference = :reference")
    Optional<SubscriptionPayment> findByTransactionReferenceForUpdate(
        @Param("reference") String reference
    );

    Optional<SubscriptionPayment> findByGatewayTransactionId(String gatewayTransactionId);

    boolean existsByGatewayTransactionId(String gatewayTransactionId);

    Optional<SubscriptionPayment> findFirstByPendingRegistration_RegistrationReferenceAndStatusOrderByIdDesc(
        String registrationReference,
        String status
    );

    Optional<SubscriptionPayment> findFirstByPendingRegistration_RegistrationReferenceOrderByIdDesc(
        String registrationReference
    );
}
