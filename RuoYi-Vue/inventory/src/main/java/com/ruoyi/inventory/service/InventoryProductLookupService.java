package com.ruoyi.inventory.service;

import java.util.List;
import com.ruoyi.inventory.domain.InventoryProductSkuSnapshot;
import com.ruoyi.inventory.domain.InventoryProductSourceBindingSnapshot;
import com.ruoyi.inventory.domain.InventoryProductWarehouseSnapshot;
import com.ruoyi.inventory.domain.InventorySourceSkuKey;

/**
 * Inventory-owned read port for product SKU/SPU identity lookups.
 */
public interface InventoryProductLookupService
{
    List<Long> selectSkuIdsBySpuId(Long spuId);

    List<InventoryProductSkuSnapshot> selectSkuSnapshotsBySpuId(Long spuId);

    List<InventoryProductSkuSnapshot> selectSkuSnapshotsBySkuIds(List<Long> skuIds);

    List<InventoryProductSourceBindingSnapshot> selectSourceBindingSnapshotsBySpuId(Long spuId);

    List<InventoryProductWarehouseSnapshot> selectWarehouseSnapshotsBySpuId(Long spuId);

    List<InventorySourceSkuKey> selectSourceSkuKeysBySpuId(Long spuId);

    List<Long> selectSpuIdsBySourceSkuKeys(List<InventorySourceSkuKey> sourceKeys);
}
