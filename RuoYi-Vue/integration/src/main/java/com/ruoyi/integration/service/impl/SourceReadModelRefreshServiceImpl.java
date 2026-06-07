package com.ruoyi.integration.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.integration.service.ISourceReadModelRefreshService;

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

    @Override
    @Transactional
    public int refreshOfficialMasterByConnection(String connectionCode)
    {
        int rows = 0;
        rows += sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        rows += sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        return rows;
    }
}
