
package com.pharmsaas.backend.modules.membership.service;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import com.pharmsaas.backend.modules.user.entity.User;
import com.pharmsaas.backend.modules.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class TenantMembershipService {

    private static final Set<MembershipRole> TENANT_ADMIN_ROLES =
            Set.of(
                    MembershipRole.TENANT_OWNER,
                    MembershipRole.PHARMACY_ADMIN
            );

    private static final Set<MembershipRole> ASSIGNABLE_ROLES =
            Set.of(
                    MembershipRole.PHARMACY_ADMIN,
                    MembershipRole.MANAGER,
                    MembershipRole.STORE_KEEPER,
                    MembershipRole.PHARMACIST,
                    MembershipRole.STAFF,
                    MembershipRole.CASHIER
            );

    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public TenantMembershipService(
            TenantMembershipRepository membershipRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository) {

        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    // --------------------------------------------------
    // Validate tenant
    // --------------------------------------------------

    private Tenant requireActiveTenant(Long tenantId) {

        if (tenantId == null || tenantId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid tenant ID is required."
            );
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant not found."
                ));

        if (tenant.getStatus() == null
                || !"Active".equalsIgnoreCase(
                tenant.getStatus().trim())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This pharmacy is not active."
            );
        }

        return tenant;
    }

    // --------------------------------------------------
    // Validate tenant administrator
    // --------------------------------------------------

    private MembershipRole requireTenantAdmin(
            Long tenantId,
            Long actorUserId) {

        if (actorUserId == null || actorUserId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required."
            );
        }

        TenantMembership actorMembership = membershipRepository
                .findByTenant_IdAndUser_IdAndActiveTrue(
                        tenantId,
                        actorUserId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You do not have an active membership in this pharmacy."
                ));

        MembershipRole role = actorMembership.getRole();

        if (role == null || !TENANT_ADMIN_ROLES.contains(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner or Pharmacy Admin can manage memberships."
            );
        }

        return role;
    }

    // --------------------------------------------------
    // Validate assignable role
    // --------------------------------------------------

    private void validateAssignableRole(
            MembershipRole actorRole,
            MembershipRole targetRole) {

        if (targetRole == null
                || !ASSIGNABLE_ROLES.contains(targetRole)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This role cannot be assigned through tenant membership management."
            );
        }

        if (targetRole == MembershipRole.PHARMACY_ADMIN
                && actorRole != MembershipRole.TENANT_OWNER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner can appoint a Pharmacy Admin."
            );
        }
    }

    // --------------------------------------------------
    // Validate that the user belongs to this tenant
    // --------------------------------------------------

    private User requireUserInTenant(
            Long tenantId,
            Long userId) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid user ID is required."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getTenant() == null
                || user.getTenant().getId() == null
                || !tenantId.equals(user.getTenant().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This user does not belong to the specified tenant."
            );
        }

        return user;
    }

    // --------------------------------------------------
    // Add an existing user to a tenant
    // --------------------------------------------------

    @Transactional
    public TenantMembership addMembership(
            Long tenantId,
            Long actorUserId,
            Long userId,
            MembershipRole role) {

        Tenant tenant = requireActiveTenant(tenantId);

        MembershipRole actorRole = requireTenantAdmin(
                tenantId,
                actorUserId
        );

        validateAssignableRole(actorRole, role);

        User user = requireUserInTenant(tenantId, userId);

        if (membershipRepository
                .existsByTenant_IdAndUser_Id(tenantId, userId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This user already has a membership in this tenant."
            );
        }

        TenantMembership membership = new TenantMembership();
        membership.setTenant(tenant);
        membership.setUser(user);
        membership.setRole(role);
        membership.setActive(true);

        return membershipRepository.save(membership);
    }

    // --------------------------------------------------
    // Get all memberships, including inactive members
    // Controller must authorize the requesting user.
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<TenantMembership> getTenantMembers(Long tenantId) {

        if (tenantId == null || tenantId <= 0
                || !tenantRepository.existsById(tenantId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tenant not found."
            );
        }

        return membershipRepository.findByTenant_Id(tenantId);
    }

    // --------------------------------------------------
    // Get a user's active memberships
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<TenantMembership> getUserMemberships(Long userId) {

        if (userId == null || userId <= 0
                || !userRepository.existsById(userId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found."
            );
        }

        return membershipRepository.findByUser_IdAndActiveTrue(userId);
    }

    // --------------------------------------------------
    // Check whether a user has a specific active role
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public boolean hasRole(
            Long tenantId,
            Long userId,
            MembershipRole role) {

        if (tenantId == null || userId == null || role == null) {
            return false;
        }

        return membershipRepository
                .existsByTenant_IdAndUser_IdAndRoleAndActiveTrue(
                        tenantId,
                        userId,
                        role
                );
    }

    // --------------------------------------------------
    // Change a member's active/inactive status safely
    // --------------------------------------------------

    @Transactional
    public TenantMembership changeTenantMemberStatus(
            Long tenantId,
            Long actorUserId,
            Long targetUserId,
            boolean active) {

        requireActiveTenant(tenantId);

        MembershipRole actorRole = requireTenantAdmin(
                tenantId,
                actorUserId
        );

        requireUserInTenant(tenantId, targetUserId);

        TenantMembership membership = membershipRepository
                .findByTenant_IdAndUser_Id(tenantId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership not found."
                ));

        MembershipRole targetRole = membership.getRole();

        if (targetRole == MembershipRole.TENANT_OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The Tenant Owner cannot be disabled here."
            );
        }

        if (targetRole == MembershipRole.PHARMACY_ADMIN
                && actorRole != MembershipRole.TENANT_OWNER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner can change a Pharmacy Admin's status."
            );
        }

        if (actorUserId.equals(targetUserId) && !active) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot disable your own account here."
            );
        }

        membership.setActive(active);

        return membershipRepository.save(membership);
    }

    // --------------------------------------------------
    // Change a member's role
    // --------------------------------------------------

    @Transactional
    public TenantMembership changeMembershipRole(
            Long tenantId,
            Long actorUserId,
            Long targetUserId,
            MembershipRole newRole) {

        requireActiveTenant(tenantId);

        MembershipRole actorRole = requireTenantAdmin(
                tenantId,
                actorUserId
        );

        validateAssignableRole(actorRole, newRole);

        requireUserInTenant(tenantId, targetUserId);

        TenantMembership membership = membershipRepository
                .findByTenant_IdAndUser_IdAndActiveTrue(
                        tenantId,
                        targetUserId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active membership not found."
                ));

        MembershipRole existingRole = membership.getRole();

        if (existingRole == MembershipRole.TENANT_OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The Tenant Owner role cannot be changed through membership management."
            );
        }

        if (existingRole == MembershipRole.PHARMACY_ADMIN
                && actorRole != MembershipRole.TENANT_OWNER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner can change a Pharmacy Admin membership."
            );
        }

        membership.setRole(newRole);

        return membershipRepository.save(membership);
    }

    @Transactional
    public void removeTenantMember(
            Long tenantId,
            Long actorUserId,
            Long targetUserId) {

        requireActiveTenant(tenantId);

        MembershipRole actorRole = requireTenantAdmin(
                tenantId,
                actorUserId
        );

        if (actorUserId.equals(targetUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot remove your own membership."
            );
        }

        requireUserInTenant(tenantId, targetUserId);

        TenantMembership membership = membershipRepository
                .findByTenant_IdAndUser_Id(tenantId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership not found."
                ));

        MembershipRole targetRole = membership.getRole();

        if (targetRole == MembershipRole.TENANT_OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenant Owner membership cannot be removed."
            );
        }

        if (targetRole == MembershipRole.PHARMACY_ADMIN
                && actorRole != MembershipRole.TENANT_OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the Tenant Owner can remove a Pharmacy Admin."
            );
        }

        membershipRepository.delete(membership);
    }
}
