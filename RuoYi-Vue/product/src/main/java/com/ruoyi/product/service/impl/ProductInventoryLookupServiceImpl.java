package com.ruoyi.product.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.inventory.domain.InventoryProductSkuSnapshot;
import com.ruoyi.inventory.domain.InventoryProductSourceBindingSnapshot;
import com.ruoyi.inventory.domain.InventoryProductWarehouseSnapshot;
import com.ruoyi.inventory.domain.InventorySourceSkuKey;
import com.ruoyi.inventory.service.InventoryProductLookupService;
import com.ruoyi.product.mapper.ProductDistributionMapper;

/**
 * Product implementation of the inventory-owned SKU/SPU lookup port.
 */
@Service
public class ProductInventoryLookupServiceImpl implements InventoryProductLookupService
{
    @Autowired
    private ProductDistributionMapper productDistributionMapper;

    @Override
    public List<Long> selectSkuIdsBySpuId(Long spuId)
    {
        if (spuId == null)
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventorySkuIdsBySpuId(spuId);
    }

    @Override
    public List<InventoryProductSkuSnapshot> selectSkuSnapshotsBySpuId(Long spuId)
    {
        if (spuId == null)
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventorySkuSnapshotsBySpuId(spuId);
    }

    @Override
    public List<InventoryProductSkuSnapshot> selectSkuSnapshotsBySkuIds(List<Long> skuIds)
    {
        if (skuIds == null || skuIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventorySkuSnapshotsBySkuIds(skuIds);
    }

    @Override
    public List<InventoryProductSourceBindingSnapshot> selectSourceBindingSnapshotsBySpuId(Long spuId)
    {
        if (spuId == null)
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventorySourceBindingSnapshotsBySpuId(spuId);
    }

    @Override
    public List<InventoryProductWarehouseSnapshot> selectWarehouseSnapshotsBySpuId(Long spuId)
    {
        if (spuId == null)
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventoryWarehouseSnapshotsBySpuId(spuId);
    }

    @Override
    public List<InventorySourceSkuKey> selectSourceSkuKeysBySpuId(Long spuId)
    {
        if (spuId == null)
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventorySourceSkuKeysBySpuId(spuId);
    }

    @Override
    public List<Long> selectSpuIdsBySourceSkuKeys(List<InventorySourceSkuKey> sourceKeys)
    {
        if (sourceKeys == null || sourceKeys.isEmpty())
        {
            return new ArrayList<>();
        }
        return productDistributionMapper.selectInventorySpuIdsBySourceSkuKeys(sourceKeys);
    }
}
