package com.pharmsaas.backend.modules.tenant.repository;

import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository
        extends JpaRepository<Tenant, Long> {

    boolean existsByTenantCode(String tenantCode);

    boolean existsByTenantCodeAndIdNot(
            String tenantCode,
            Long id
    );
}
