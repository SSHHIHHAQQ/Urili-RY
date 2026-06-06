package com.ruoyi.integration.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.integration.domain.SourceWarehouseStockItem;
import com.ruoyi.integration.domain.query.SourceWarehouseStockQuery;
import com.ruoyi.integration.service.IUpstreamSystemService;

/**
 * 管理端来源仓库库存只读查询。
 */
@RestController
@RequestMapping("/integration/admin/source-warehouse-stocks")
public class AdminSourceWarehouseStockController extends BaseController
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @PreAuthorize("@ss.hasPermi('inventory:sourceWarehouse:list')")
    @GetMapping("/list")
    public TableDataInfo list(SourceWarehouseStockQuery query)
    {
        startPage();
        List<SourceWarehouseStockItem> list = upstreamSystemService.selectSourceWarehouseStockList(query);
        return getDataTable(list);
    }
}
