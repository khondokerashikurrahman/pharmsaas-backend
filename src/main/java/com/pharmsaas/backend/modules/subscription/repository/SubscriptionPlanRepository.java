package com.pharmsaas.backend.modules.subscription.repository;

import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByCodeAndActiveTrue(String code);

    List<SubscriptionPlan> findByActiveTrueOrderByPriceAsc();

    boolean existsByCode(String code);
}
