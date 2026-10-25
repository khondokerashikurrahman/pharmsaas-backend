
package com.pharmsaas.backend.modules.membership.entity;

public enum MembershipRole {

    // Global system role
    SUPER_ADMIN,

    // Tenant administration roles
    TENANT_OWNER,
    PHARMACY_ADMIN,

    // Operational roles
    MANAGER,
    STORE_KEEPER,
    PHARMACIST,
    STAFF,
    CASHIER
}
