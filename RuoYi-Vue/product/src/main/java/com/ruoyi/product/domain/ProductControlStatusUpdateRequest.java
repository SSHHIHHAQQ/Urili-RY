package com.ruoyi.product.domain;

import java.util.List;

/**
 * 商品批量管控状态更新请求。
 */
public class ProductControlStatusUpdateRequest
{
    private String ownerType;
    private List<Long> spuIds;
    private List<Long> skuIds;
    private String controlStatus;
    private String reason;

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public List<Long> getSpuIds() { return spuIds; }
    public void setSpuIds(List<Long> spuIds) { this.spuIds = spuIds; }
    public List<Long> getSkuIds() { return skuIds; }
    public void setSkuIds(List<Long> skuIds) { this.skuIds = skuIds; }
    public String getControlStatus() { return controlStatus; }
    public void setControlStatus(String controlStatus) { this.controlStatus = controlStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
