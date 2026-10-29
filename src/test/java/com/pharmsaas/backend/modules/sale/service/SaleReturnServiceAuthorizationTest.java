package com.pharmsaas.backend.modules.sale.service;

import com.pharmsaas.backend.modules.inventory.service.InventoryMovementService;

import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryBatchRepository;
import com.pharmsaas.backend.modules.inventory.repository.TenantInventoryRepository;
import com.pharmsaas.backend.modules.inventory.repository.StoreInventoryStockRepository;
import com.pharmsaas.backend.modules.membership.entity.MembershipRole;
import com.pharmsaas.backend.modules.membership.entity.TenantMembership;
import com.pharmsaas.backend.modules.membership.repository.TenantMembershipRepository;
import com.pharmsaas.backend.modules.sale.dto.SaleReturnCreateRequest;
import com.pharmsaas.backend.modules.sale.repository.SaleItemRepository;
import com.pharmsaas.backend.modules.sale.repository.SaleRepository;
import com.pharmsaas.backend.modules.sale.repository.SaleReturnItemRepository;
import com.pharmsaas.backend.modules.sale.repository.SaleReturnRepository;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import com.pharmsaas.backend.modules.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaleReturnServiceAuthorizationTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final Long SALE_ID = 20L;
    private static final Long RETURN_ID = 30L;
    private static final Long RETURN_ITEM_ID = 40L;

    private SaleRepository saleRepository;
    private SaleItemRepository saleItemRepository;
    private SaleReturnRepository returnRepository;
    private SaleReturnItemRepository returnItemRepository;
    private TenantInventoryBatchRepository batchRepository;
    private TenantInventoryRepository inventoryRepository;
    private StoreInventoryStockRepository storeStockRepository;
    private TenantRepository tenantRepository;
    private TenantMembershipRepository membershipRepository;
    private InventoryMovementService movementService;

    private SaleReturnService service;
    private Tenant tenant;
    private TenantMembership membership;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        saleItemRepository = mock(SaleItemRepository.class);
        returnRepository = mock(SaleReturnRepository.class);
        returnItemRepository = mock(SaleReturnItemRepository.class);
        batchRepository = mock(TenantInventoryBatchRepository.class);
        inventoryRepository = mock(TenantInventoryRepository.class);
        storeStockRepository = mock(StoreInventoryStockRepository.class);
        tenantRepository = mock(TenantRepository.class);
        membershipRepository = mock(TenantMembershipRepository.class);
        movementService = mock(InventoryMovementService.class);

        tenant = mock(Tenant.class);
        membership = mock(TenantMembership.class);

        service = new SaleReturnService(
            saleRepository,
            saleItemRepository,
            returnRepository,
            returnItemRepository,
            batchRepository,
            inventoryRepository,
            storeStockRepository,
            tenantRepository,
            membershipRepository,
            movementService
        );
    }

    private void givenActiveTenantAndRole(MembershipRole role) {
        when(tenantRepository.findById(TENANT_ID))
            .thenReturn(Optional.of(tenant));
        when(tenant.getStatus()).thenReturn("ACTIVE");

        when(membershipRepository.findByTenant_IdAndUser_IdAndActiveTrue(
            TENANT_ID, USER_ID
        )).thenReturn(Optional.of(membership));

        when(membership.getRole()).thenReturn(role);
    }

    private ResponseStatusException assertForbidden(
        org.junit.jupiter.api.function.Executable action
    ) {
        ResponseStatusException exception =
            assertThrows(ResponseStatusException.class, action);

        assertEquals(403, exception.getStatusCode().value());
        return exception;
    }

    @Test
    void staffCannotCreateSaleReturn() {
        givenActiveTenantAndRole(MembershipRole.STAFF);

        SaleReturnCreateRequest request = new SaleReturnCreateRequest(
            "auth-test-staff-create",
            "Authorization test",
            List.of(new SaleReturnCreateRequest.ReturnLineRequest(99L, 1))
        );

        assertForbidden(() ->
            service.create(TENANT_ID, SALE_ID, USER_ID, request)
        );

        verifyNoInteractions(saleRepository, returnRepository);
    }

    @Test
    void staffCannotViewReturnHistory() {
        givenActiveTenantAndRole(MembershipRole.STAFF);

        assertForbidden(() -> service.listReturns(TENANT_ID, USER_ID));

        verifyNoInteractions(returnRepository);
    }

    @Test
    void cashierCannotRestockReturnedStock() {
        givenActiveTenantAndRole(MembershipRole.CASHIER);

        assertForbidden(() ->
            service.restockReturnedItem(
                TENANT_ID, RETURN_ID, RETURN_ITEM_ID, USER_ID
            )
        );

        verifyNoInteractions(returnItemRepository);
    }

    @Test
    void storeKeeperCanViewReturnHistory() {
        givenActiveTenantAndRole(MembershipRole.STORE_KEEPER);

        when(returnRepository.findAllByTenant_IdOrderByCreatedAtDesc(TENANT_ID))
            .thenReturn(List.of());

        assertTrue(service.listReturns(TENANT_ID, USER_ID).isEmpty());

        verify(returnRepository)
            .findAllByTenant_IdOrderByCreatedAtDesc(TENANT_ID);
    }

    @Test
    void managerPassesRoleCheckBeforeRestockItemLookup() {
        givenActiveTenantAndRole(MembershipRole.MANAGER);

        when(returnItemRepository.findForUpdateByIdAndReturnAndTenant(
            RETURN_ITEM_ID, RETURN_ID, TENANT_ID
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.restockReturnedItem(
                TENANT_ID, RETURN_ID, RETURN_ITEM_ID, USER_ID
            )
        );

        assertEquals(404, exception.getStatusCode().value());

        verify(returnItemRepository).findForUpdateByIdAndReturnAndTenant(
            RETURN_ITEM_ID, RETURN_ID, TENANT_ID
        );
        verifyNoInteractions(batchRepository, inventoryRepository);
    }
}
