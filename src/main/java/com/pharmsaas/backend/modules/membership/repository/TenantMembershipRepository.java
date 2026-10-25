
package com.pharmsaas.backend.modules.membership.repository;

import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantMembershipRepository
        extends JpaRepository<TenantMembership, Long> {

    boolean existsByTenant_IdAndUser_Id(
            Long tenantId,
            Long userId
    );

    @EntityGraph(attributePaths = {"tenant", "user"})
    List<TenantMembership> findByUser_IdAndActiveTrue(Long userId);

    @EntityGraph(attributePaths = {"tenant", "user"})
    Optional<TenantMembership> findFirstByUser_IdAndActiveTrue(
            Long userId
    );

    // Active এবং inactive—সব membership
    @EntityGraph(attributePaths = {"tenant", "user"})
    List<TenantMembership> findByTenant_Id(Long tenantId);

    @EntityGraph(attributePaths = {"tenant", "user"})
    List<TenantMembership> findByTenant_IdAndActiveTrue(Long tenantId);

    @EntityGraph(attributePaths = {"tenant", "user"})
    Optional<TenantMembership> findByTenant_IdAndUser_Id(
            Long tenantId,
            Long userId
    );

    @EntityGraph(attributePaths = {"tenant", "user"})
    Optional<TenantMembership> findByTenant_IdAndUser_IdAndActiveTrue(
            Long tenantId,
            Long userId
    );

    boolean existsByTenant_IdAndUser_IdAndRoleAndActiveTrue(
            Long tenantId,
            Long userId,
            MembershipRole role
    );
}
