package com.ruoyi.product.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.product.domain.ProductConfigChangeLog;
import com.ruoyi.product.service.ProductConfigChangeLogService;

/**
 * 管理端商品配置修改记录。
 */
@RestController
@RequestMapping("/product/admin/change-logs")
public class AdminProductConfigChangeLogController extends BaseController
{
    @Autowired
    private ProductConfigChangeLogService changeLogService;

    @PreAuthorize("@ss.hasAnyPermi('product:category:list,product:attribute:list,product:categoryAttribute:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductConfigChangeLog query)
    {
        startPage();
        List<ProductConfigChangeLog> list = changeLogService.selectChangeLogList(query);
        return getDataTable(list);
    }
}
