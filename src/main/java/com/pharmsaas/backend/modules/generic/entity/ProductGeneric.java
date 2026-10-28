package com.pharmsaas.backend.modules.generic.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;

@Entity
@Table(name = "product_generics")
public class ProductGeneric extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    public ProductGeneric() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
