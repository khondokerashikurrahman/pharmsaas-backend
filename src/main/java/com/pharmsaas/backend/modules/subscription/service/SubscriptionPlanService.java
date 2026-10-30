package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.dto.SubscriptionPlanResponse;
import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPlan;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlanService(SubscriptionPlanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPublicPlans() {
        return repository.findByActiveTrueOrderByPriceAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getDurationDays()
        );
    }
}
