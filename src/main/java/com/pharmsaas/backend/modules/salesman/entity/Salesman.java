package com.pharmsaas.backend.modules.salesman.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "salesmen",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_salesman_tenant_employee",
            columnNames = {"tenant_id", "employee_id"}
        )
    }
)
public class Salesman extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "employee_id", nullable = false, length = 30)
    private String employeeId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 120)
    private String territory;

    @Column(name = "commission_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "Active";

    public Salesman() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTerritory() { return territory; }
    public void setTerritory(String territory) { this.territory = territory; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
