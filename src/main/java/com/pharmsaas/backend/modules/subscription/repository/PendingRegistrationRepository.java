package com.pharmsaas.backend.modules.subscription.repository;

import com.pharmsaas.backend.modules.subscription.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PendingRegistrationRepository
        extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByRegistrationReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PendingRegistration p WHERE p.registrationReference = :reference")
    Optional<PendingRegistration> findByRegistrationReferenceForUpdate(
            @Param("reference") String reference
    );

    boolean existsByRegistrationReference(String reference);

    boolean existsByEmailAndStatusAndExpiresAtAfter(
            String email,
            String status,
            OffsetDateTime now
    );

    Optional<PendingRegistration> findFirstByEmailAndStatusAndExpiresAtAfter(
            String email,
            String status,
            OffsetDateTime now
    );
}
