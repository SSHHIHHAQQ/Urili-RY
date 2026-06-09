package com.ruoyi.product.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.product.domain.ProductBatchStatusUpdateRequest;
import com.ruoyi.product.domain.ProductControlStatusUpdateRequest;
import com.ruoyi.product.domain.ProductDistributionOperationLog;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSalePriceUpdateRequest;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductStatusUpdateRequest;
import com.ruoyi.product.service.IProductDistributionService;
import com.ruoyi.product.service.IProductReviewService;

/**
 * 管理端商城商品列表。
 */
@RestController
@RequestMapping("/product/admin/distribution-products")
public class AdminProductDistributionController extends BaseController
{
    private static final String STATUS_DRAFT = "DRAFT";

    @Autowired
    private IProductDistributionService productDistributionService;

    @Autowired
    private IProductReviewService productReviewService;

    @PreAuthorize("@ss.hasPermi('product:distribution:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProductSpu query)
    {
        startPage();
        List<ProductSpu> list = productDistributionService.selectProductList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:list')")
    @GetMapping("/skus/list")
    public TableDataInfo skuList(ProductSku query)
    {
        startPage();
        List<ProductSku> list = productDistributionService.selectSkuPageList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:query')")
    @GetMapping("/{spuId}")
    public AjaxResult get(@PathVariable("spuId") Long spuId)
    {
        return success(productDistributionService.selectProductById(spuId));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:query')")
    @GetMapping("/{spuId}/latest-rejected-submission")
    public AjaxResult latestRejectedSubmission(@PathVariable("spuId") Long spuId)
    {
        return success(productReviewService.selectLatestRejectedReusableSubmission(spuId));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:add')")
    @Log(title = "商城商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ProductSpu product)
    {
        int rows = productDistributionService.insertProduct(product);
        return rows > 0 ? success(product) : error();
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:edit')")
    @Log(title = "商城商品", businessType = BusinessType.UPDATE)
    @PutMapping("/{spuId}")
    public AjaxResult edit(@PathVariable("spuId") Long spuId, @Validated @RequestBody ProductSpu product)
    {
        product.setSpuId(spuId);
        ProductSpu current = productDistributionService.selectProductById(spuId);
        if (!STATUS_DRAFT.equals(current.getSpuStatus()))
        {
            int rows = productReviewService.submitProductEditReview(product);
            return rows > 0 ? success("已提交审核") : error();
        }
        return toAjax(productDistributionService.updateProduct(product));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:remove')")
    @Log(title = "商城商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{spuId}")
    public AjaxResult remove(@PathVariable("spuId") Long spuId)
    {
        return toAjax(productDistributionService.deleteDraftProduct(spuId));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:status')")
    @Log(title = "商城商品状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{spuId}/status")
    public AjaxResult updateSpuStatus(@PathVariable("spuId") Long spuId,
        @RequestBody ProductStatusUpdateRequest request)
    {
        return toAjax(productDistributionService.updateSpuStatus(spuId, request.getStatus(), request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:edit')")
    @Log(title = "商城商品提交审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{spuId}/submit-review")
    public AjaxResult submitReview(@PathVariable("spuId") Long spuId)
    {
        return toAjax(productReviewService.submitNewProductReview(spuId));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:status')")
    @Log(title = "商城商品批量状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status/batch")
    public AjaxResult batchUpdateStatus(@RequestBody ProductBatchStatusUpdateRequest request)
    {
        if ("SKU".equalsIgnoreCase(request.getOwnerType()))
        {
            return toAjax(productDistributionService.batchUpdateSkuStatus(request.getSkuIds(), request.getStatus(),
                request.getReason()));
        }
        boolean syncSkuStatus = request.getSyncSkuStatus() == null || request.getSyncSkuStatus();
        return toAjax(productDistributionService.batchUpdateSpuStatus(request.getSpuIds(), request.getStatus(),
            syncSkuStatus, request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:status')")
    @Log(title = "商城商品管控状态", businessType = BusinessType.UPDATE)
    @PutMapping("/control-status/batch")
    public AjaxResult batchUpdateControlStatus(@RequestBody ProductControlStatusUpdateRequest request)
    {
        if ("SKU".equalsIgnoreCase(request.getOwnerType()))
        {
            return toAjax(productDistributionService.batchUpdateSkuControlStatus(request.getSkuIds(),
                request.getControlStatus(), request.getReason()));
        }
        return toAjax(productDistributionService.batchUpdateSpuControlStatus(request.getSpuIds(),
            request.getControlStatus(), request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:price')")
    @Log(title = "商城商品SKU销售价", businessType = BusinessType.UPDATE)
    @PutMapping("/skus/sale-prices")
    public AjaxResult batchUpdateSkuSalePrice(@RequestBody ProductSkuSalePriceUpdateRequest request)
    {
        return toAjax(productDistributionService.batchUpdateSkuSalePrice(request));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:status')")
    @Log(title = "商城商品SKU状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{spuId}/skus/{skuId}/status")
    public AjaxResult updateSkuStatus(@PathVariable("spuId") Long spuId, @PathVariable("skuId") Long skuId,
        @RequestBody ProductStatusUpdateRequest request)
    {
        return toAjax(productDistributionService.updateSkuStatus(spuId, skuId, request.getStatus(),
            request.getReason()));
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:log')")
    @GetMapping("/operation-logs/list")
    public TableDataInfo operationLogs(ProductDistributionOperationLog query)
    {
        startPage();
        List<ProductDistributionOperationLog> list = productDistributionService.selectOperationLogList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('product:distribution:query')")
    @GetMapping("/{spuId}/skus")
    public AjaxResult skus(@PathVariable("spuId") Long spuId)
    {
        List<ProductSku> skus = productDistributionService.selectSkuList(spuId);
        return success(skus);
    }
}
