package com.ruoyi.integration.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
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

    @PreAuthorize("@ss.hasPermi('product:list:list')")
    @GetMapping("/list")
    public TableDataInfo list(SourceProductQuery query)
    {
        startPage();
        List<SourceProductItem> list = upstreamSystemService.selectSourceProductList(query);
        return getDataTable(list);
    }
}
