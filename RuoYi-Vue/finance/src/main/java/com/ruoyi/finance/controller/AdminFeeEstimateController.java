package com.ruoyi.finance.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.finance.domain.FeeEstimateSkuSnapshot;
import com.ruoyi.finance.domain.query.FeeEstimateSkuQuery;
import com.ruoyi.finance.domain.request.FeeEstimateRequest;
import com.ruoyi.finance.service.IFeeEstimateService;

@RestController
@RequestMapping("/finance/admin/fee-estimate")
public class AdminFeeEstimateController extends BaseController
{
    @Autowired
    private IFeeEstimateService feeEstimateService;

    @PreAuthorize("@ss.hasPermi('finance:feeEstimate:query')")
    @GetMapping("/options")
    public AjaxResult options(@RequestParam(value = "schemeId", required = false) Long schemeId)
    {
        return success(feeEstimateService.selectOptions(schemeId));
    }

    @PreAuthorize("@ss.hasPermi('finance:feeEstimate:query')")
    @GetMapping("/skus/list")
    public TableDataInfo skus(FeeEstimateSkuQuery query)
    {
        startPage();
        List<FeeEstimateSkuSnapshot> list = feeEstimateService.selectSkuSnapshots(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:feeEstimate:calculate')")
    @Log(title = "费用试算", businessType = BusinessType.OTHER, isSaveRequestData = false,
        isSaveResponseData = false)
    @PostMapping("/calculate")
    public AjaxResult calculate(@RequestBody FeeEstimateRequest request)
    {
        return success(feeEstimateService.calculate(request));
    }
}
