package com.pharmsaas.backend.modules.inventory.entity;

import com.pharmsaas.backend.common.BaseEntity;
import com.pharmsaas.backend.modules.store.entity.Store;
import com.pharmsaas.backend.modules.tenant.entity.Tenant;
import jakarta.persistence.*;

@Entity
@Table(
    name = "store_inventory_stock",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_store_inventory_batch",
        columnNames = {"store_id", "batch_id"}
    ),
    indexes = {
        @Index(
            name = "idx_store_stock_tenant_store",
            columnList = "tenant_id,store_id"
        ),
        @Index(
            name = "idx_store_stock_batch",
            columnList = "tenant_id,batch_id"
        )
    }
)
public class StoreInventoryStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private TenantInventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TenantInventoryBatch batch;

    @Column(nullable = false)
    private Integer quantity = 0;

    public StoreInventoryStock() {}

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public TenantInventoryItem getInventoryItem() { return inventoryItem; }
    public void setInventoryItem(TenantInventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public TenantInventoryBatch getBatch() { return batch; }
    public void setBatch(TenantInventoryBatch batch) { this.batch = batch; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
