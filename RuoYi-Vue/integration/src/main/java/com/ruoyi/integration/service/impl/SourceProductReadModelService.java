package com.ruoyi.integration.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;

/**
 * 来源商品库读模型构建服务。
 */
@Service
public class SourceProductReadModelService
{
    static final String REPOSITORY_SCOPE_OFFICIAL_MASTER = "OFFICIAL_MASTER";

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Transactional
    public int rebuildOfficialMaster()
    {
        int rows = 0;
        rows += upstreamSystemMapper.deleteAllSourceProductWarehouseDetails();
        rows += upstreamSystemMapper.deleteAllSourceProductDimensionGroups();
        rows += upstreamSystemMapper.deleteAllSourceProductGroups();
        rows += upstreamSystemMapper.insertAllSourceProductGroups(null);
        rows += upstreamSystemMapper.insertAllSourceProductDimensionGroups(null);
        rows += upstreamSystemMapper.insertAllSourceProductWarehouseDetails(null);
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
        rows += upstreamSystemMapper.deleteSourceProductWarehouseDetailsByConnection(connectionCode);
        rows += upstreamSystemMapper.deleteSourceProductDimensionGroupsByConnection(connectionCode);
        rows += upstreamSystemMapper.deleteSourceProductGroupsByConnection(connectionCode);
        rows += upstreamSystemMapper.insertSourceProductGroupsByConnection(connectionCode);
        rows += upstreamSystemMapper.insertSourceProductDimensionGroupsByConnection(connectionCode);
        rows += upstreamSystemMapper.insertSourceProductWarehouseDetailsByConnection(connectionCode);
        return rows;
    }
}
