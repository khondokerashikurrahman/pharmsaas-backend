package com.pharmsaas.backend.modules.subscription.controller;

import com.pharmsaas.backend.modules.subscription.dto.SubscriptionPlanResponse;
import com.pharmsaas.backend.modules.subscription.service.SubscriptionPlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/subscription-plans")
public class PublicSubscriptionPlanController {

    private final SubscriptionPlanService planService;

    public PublicSubscriptionPlanController(
            SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<SubscriptionPlanResponse> getPlans() {
        return planService.getPublicPlans();
    }
}
