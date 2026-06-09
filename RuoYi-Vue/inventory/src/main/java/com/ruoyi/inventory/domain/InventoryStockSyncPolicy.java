package com.ruoyi.inventory.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 平台库存同步策略。
 */
public class InventoryStockSyncPolicy extends BaseEntity
{
    private Long policyId;
    private String policyKey;
    private Long sellerId;
    private String scopeType;
    private String warehouseKey;
    private String warehouseName;
    private Long spuId;
    private Long skuId;
    private Long stockId;
    private String syncMode;
    private String enabled;
    private Integer version;
    private Date lastApplyTime;

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getWarehouseKey() { return warehouseKey; }
    public void setWarehouseKey(String warehouseKey) { this.warehouseKey = warehouseKey; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Date getLastApplyTime() { return lastApplyTime; }
    public void setLastApplyTime(Date lastApplyTime) { this.lastApplyTime = lastApplyTime; }
}
