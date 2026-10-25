
        package com.pharmsaas.backend.modules.tenant.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

        @Setter
        @Getter
        @Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(
            name = "tenant_code",
            nullable = false,
            unique = true,
            length = 30
    )
    private String tenantCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(nullable = false, length = 20)
    private String status = "Pending";

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    public Tenant() {
    }

        }
