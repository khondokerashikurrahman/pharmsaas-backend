package com.pharmsaas.backend.modules.subscription.repository;

import com.pharmsaas.backend.modules.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<Subscription> findFirstByTenant_IdAndStatusOrderByEndsAtDesc(
            Long tenantId,
            String status
    );
}
