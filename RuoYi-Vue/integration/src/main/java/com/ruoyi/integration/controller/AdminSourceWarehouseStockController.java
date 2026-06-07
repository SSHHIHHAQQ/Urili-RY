package com.ruoyi.integration.controller;

import java.util.List;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.integration.domain.SourceWarehouseStockGroupItem;
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

    @PreAuthorize("@ss.hasPermi('inventory:sourceWarehouse:list')")
    @GetMapping("/groups/list")
    public TableDataInfo groupList(SourceWarehouseStockQuery query)
    {
        long total = upstreamSystemService.countSourceWarehouseStockGroupList(query);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        try
        {
            PageHelper.startPage(pageDomain.getPageNum(), pageDomain.getPageSize(), false)
                .setReasonable(pageDomain.getReasonable());
            List<SourceWarehouseStockGroupItem> list = upstreamSystemService.selectSourceWarehouseStockGroupList(query);
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            rspData.setRows(list);
            rspData.setTotal(total);
            return rspData;
        }
        finally
        {
            clearPage();
        }
    }

    @PreAuthorize("@ss.hasPermi('inventory:sourceWarehouse:list')")
    @GetMapping("/groups/detail")
    public AjaxResult groupDetail(SourceWarehouseStockQuery query)
    {
        return AjaxResult.success(upstreamSystemService.selectSourceWarehouseStockGroupDetailList(query));
    }

    @PreAuthorize("@ss.hasPermi('inventory:sourceWarehouse:list')")
    @GetMapping("/options/master-warehouses")
    public AjaxResult masterWarehouseOptions(SourceWarehouseStockQuery query)
    {
        return AjaxResult.success(upstreamSystemService.selectSourceWarehouseStockMasterWarehouseOptions(query));
    }

    @PreAuthorize("@ss.hasPermi('inventory:sourceWarehouse:list')")
    @GetMapping("/options/source-warehouses")
    public AjaxResult sourceWarehouseOptions(SourceWarehouseStockQuery query)
    {
        return AjaxResult.success(upstreamSystemService.selectSourceWarehouseStockUpstreamWarehouseOptions(query));
    }
}
