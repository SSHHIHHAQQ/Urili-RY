package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.inventory.domain.InventoryOfficialSourceStock;
import com.ruoyi.inventory.domain.InventorySourceSkuKey;
import com.ruoyi.inventory.service.InventorySourceWarehouseStockLookupService;

/**
 * 面向库存模块的来源仓库存只读查询实现。
 */
@Service
public class SourceWarehouseStockInventoryLookupServiceImpl implements InventorySourceWarehouseStockLookupService
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Override
    public List<InventorySourceSkuKey> selectAffectedOfficialMasterSkuKeysByConnection(String connectionCode)
    {
        if (StringUtils.isBlank(connectionCode))
        {
            return new ArrayList<>();
        }
        List<InventorySourceSkuKey> sourceKeys =
            upstreamSystemMapper.selectAffectedOfficialMasterSourceSkuKeysByConnection(connectionCode.trim());
        return sourceKeys == null ? new ArrayList<>() : sourceKeys;
    }

    @Override
    public List<InventoryOfficialSourceStock> selectOfficialMasterStocksBySourceSkuKeys(
            List<InventorySourceSkuKey> sourceKeys)
    {
        if (sourceKeys == null || sourceKeys.isEmpty())
        {
            return new ArrayList<>();
        }
        List<InventoryOfficialSourceStock> sourceStocks =
            upstreamSystemMapper.selectOfficialMasterSourceStocksBySourceSkuKeys(sourceKeys);
        return sourceStocks == null ? new ArrayList<>() : sourceStocks;
    }
}
