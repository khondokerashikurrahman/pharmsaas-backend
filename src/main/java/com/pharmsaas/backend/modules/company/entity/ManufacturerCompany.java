package com.pharmsaas.backend.modules.company.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_companies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_company_tenant_code",
            columnNames = {"tenant_id", "company_code"}
        )
    }
)
public class ManufacturerCompany extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "company_code", nullable = false, length = 30)
    private String companyCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(nullable = false, length = 20)
    private String status = "Active";

    public ManufacturerCompany() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
