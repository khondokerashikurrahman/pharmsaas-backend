package com.pharmsaas.backend.modules.subscription.service;

import com.pharmsaas.backend.modules.subscription.entity.SubscriptionPlan;
import com.pharmsaas.backend.modules.subscription.repository.SubscriptionPlanRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SubscriptionPlanSeeder implements ApplicationRunner {

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlanSeeder(SubscriptionPlanRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("BASIC", "Basic",
                "Essential tools for a single pharmacy.",
                "999.00");

        seed("STANDARD", "Standard",
                "Advanced pharmacy management for growing businesses.",
                "1999.00");

        seed("PREMIUM", "Premium",
                "Advanced tools for larger pharmacy operations.",
                "3999.00");
    }

    private void seed(
            String code,
            String name,
            String description,
            String price) {

        if (repository.existsByCode(code)) {
            return;
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setDescription(description);
        plan.setPrice(new BigDecimal(price));
        plan.setCurrency("BDT");
        plan.setDurationDays(30);
        plan.setActive(true);

        repository.save(plan);
    }
}
