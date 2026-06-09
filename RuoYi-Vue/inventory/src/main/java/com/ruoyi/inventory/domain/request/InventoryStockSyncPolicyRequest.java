package com.ruoyi.inventory.domain.request;

import java.util.List;

/**
 * 库存同步策略设置请求。
 */
public class InventoryStockSyncPolicyRequest
{
    private String scopeType;
    private String syncMode;
    private Long sellerId;
    private String warehouseKey;
    private List<String> warehouseKeys;
    private String warehouseName;
    private Long spuId;
    private Long skuId;
    private Long stockId;
    private Boolean confirmed;
    private String remark;

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getWarehouseKey() { return warehouseKey; }
    public void setWarehouseKey(String warehouseKey) { this.warehouseKey = warehouseKey; }
    public List<String> getWarehouseKeys() { return warehouseKeys; }
    public void setWarehouseKeys(List<String> warehouseKeys) { this.warehouseKeys = warehouseKeys; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
