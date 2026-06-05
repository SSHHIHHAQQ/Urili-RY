package com.ruoyi.product.domain;

import java.util.List;

/**
 * 商品批量销售状态更新请求。
 */
public class ProductBatchStatusUpdateRequest
{
    private String ownerType;
    private List<Long> spuIds;
    private List<Long> skuIds;
    private String status;
    private Boolean syncSkuStatus;

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public List<Long> getSpuIds() { return spuIds; }
    public void setSpuIds(List<Long> spuIds) { this.spuIds = spuIds; }
    public List<Long> getSkuIds() { return skuIds; }
    public void setSkuIds(List<Long> skuIds) { this.skuIds = skuIds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getSyncSkuStatus() { return syncSkuStatus; }
    public void setSyncSkuStatus(Boolean syncSkuStatus) { this.syncSkuStatus = syncSkuStatus; }
}
