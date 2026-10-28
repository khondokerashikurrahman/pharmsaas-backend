package com.pharmsaas.backend.modules.supplier.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "suppliers", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_supplier_tenant_code",
        columnNames = {"tenant_id", "supplier_code"}
    )
})
public class Supplier extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "supplier_code", nullable = false, length = 20)
    private String supplierCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "company_name", length = 180)
    private String companyName;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "credit_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "Active";

    public Supplier() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
