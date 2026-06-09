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
import com.ruoyi.integration.domain.SourceProductItem;
import com.ruoyi.integration.domain.query.SourceProductQuery;
import com.ruoyi.integration.service.IUpstreamSystemService;

/**
 * 管理端来源商品库。
 */
@RestController
@RequestMapping("/integration/admin/source-products")
public class AdminSourceProductController extends BaseController
{
    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/list")
    public TableDataInfo list(SourceProductQuery query)
    {
        long total = upstreamSystemService.countSourceProductList(query);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        try
        {
            PageHelper.startPage(pageDomain.getPageNum(), pageDomain.getPageSize(), false)
                .setReasonable(pageDomain.getReasonable());
            List<SourceProductItem> list = upstreamSystemService.selectSourceProductList(query);
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

    @PreAuthorize("@ss.hasPermi('integration:upstream:query')")
    @GetMapping("/group-detail")
    public AjaxResult groupDetail(SourceProductQuery query)
    {
        return AjaxResult.success(upstreamSystemService.selectSourceProductGroupDetail(query));
    }
}
