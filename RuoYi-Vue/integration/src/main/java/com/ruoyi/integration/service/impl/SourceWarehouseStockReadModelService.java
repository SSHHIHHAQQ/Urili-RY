package com.ruoyi.integration.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;

/**
 * 来源仓库库存读模型构建服务。
 */
@Service
public class SourceWarehouseStockReadModelService
{
    static final String REPOSITORY_SCOPE_OFFICIAL_MASTER = "OFFICIAL_MASTER";

    static final String INVENTORY_SCOPE_COMPREHENSIVE = "COMPREHENSIVE";

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Transactional
    public int rebuildOfficialMaster()
    {
        int rows = 0;
        rows += upstreamSystemMapper.deleteAllSourceWarehouseStockFilterMetrics();
        rows += upstreamSystemMapper.deleteAllSourceWarehouseStockGroups();
        rows += upstreamSystemMapper.deleteAllSourceWarehouseStockDetails();
        rows += upstreamSystemMapper.insertAllSourceWarehouseStockDetails(null);
        rows += upstreamSystemMapper.insertAllSourceWarehouseStockGroups(null);
        rows += upstreamSystemMapper.insertAllSourceWarehouseStockFilterMetrics(null);
        return rows;
    }

    @Transactional
    public int rebuildOfficialMasterByConnection(String connectionCode)
    {
        if (StringUtils.isBlank(connectionCode))
        {
            return 0;
        }
        int rows = 0;
        rows += upstreamSystemMapper.deleteSourceWarehouseStockFilterMetricsByConnection(connectionCode);
        rows += upstreamSystemMapper.deleteSourceWarehouseStockGroupsByConnection(connectionCode);
        rows += upstreamSystemMapper.deleteSourceWarehouseStockDetailsByConnection(connectionCode);
        rows += upstreamSystemMapper.insertAllSourceWarehouseStockDetails(connectionCode);
        rows += upstreamSystemMapper.insertAllSourceWarehouseStockGroups(connectionCode);
        rows += upstreamSystemMapper.insertAllSourceWarehouseStockFilterMetrics(connectionCode);
        return rows;
    }
}
