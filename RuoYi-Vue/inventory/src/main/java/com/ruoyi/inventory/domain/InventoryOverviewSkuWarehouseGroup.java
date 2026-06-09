package com.ruoyi.inventory.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * SPU 展开层的 SKU + 仓库库存明细。
 */
public class InventoryOverviewSkuWarehouseGroup
{
    private InventoryOverviewItem sku;
    private List<InventorySkuWarehouseStock> warehouses = new ArrayList<>();

    public InventoryOverviewItem getSku()
    {
        return sku;
    }

    public void setSku(InventoryOverviewItem sku)
    {
        this.sku = sku;
    }

    public List<InventorySkuWarehouseStock> getWarehouses()
    {
        return warehouses;
    }

    public void setWarehouses(List<InventorySkuWarehouseStock> warehouses)
    {
        this.warehouses = warehouses;
    }
}
