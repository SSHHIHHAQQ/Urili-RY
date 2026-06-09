package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.service.ISourceReadModelRefreshService;
import com.ruoyi.inventory.service.IInventoryOverviewService;

/**
 * 来源读模型刷新 facade，避免外部模块直接依赖 integration 内部实现类。
 */
@Service
public class SourceReadModelRefreshServiceImpl implements ISourceReadModelRefreshService
{
    @Autowired
    private SourceProductReadModelService sourceProductReadModelService;

    @Autowired
    private SourceWarehouseStockReadModelService sourceWarehouseStockReadModelService;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private ObjectProvider<IInventoryOverviewService> inventoryOverviewService;

    @Override
    @Transactional
    public int refreshOfficialMasterByConnection(String connectionCode)
    {
        List<Long> affectedSpuIds = selectSourceInventoryOverviewSpuIds(connectionCode);
        int rows = 0;
        rows += sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        rows += sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        refreshSourceInventoryOverview(connectionCode, affectedSpuIds);
        return rows;
    }

    @Override
    @Transactional
    public int refreshOfficialMasterSkuPairingByConnection(String connectionCode)
    {
        List<Long> affectedSpuIds = selectSourceInventoryOverviewSpuIds(connectionCode);
        int rows = 0;
        rows += sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        rows += upstreamSystemMapper.refreshInventorySnapshotSkuPairingByConnection(connectionCode);
        rows += sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        refreshSourceInventoryOverview(connectionCode, affectedSpuIds);
        return rows;
    }

    private void refreshSourceInventoryOverview(String connectionCode, List<Long> affectedSpuIds)
    {
        IInventoryOverviewService overviewService = inventoryOverviewService.getIfAvailable();
        if (overviewService != null)
        {
            overviewService.refreshSourceInventoryOverviewByConnection(connectionCode, affectedSpuIds);
        }
    }

    private List<Long> selectSourceInventoryOverviewSpuIds(String connectionCode)
    {
        IInventoryOverviewService overviewService = inventoryOverviewService.getIfAvailable();
        if (overviewService == null)
        {
            return new ArrayList<>();
        }
        return overviewService.selectSourceInventoryOverviewSpuIdsByConnection(connectionCode);
    }
}
