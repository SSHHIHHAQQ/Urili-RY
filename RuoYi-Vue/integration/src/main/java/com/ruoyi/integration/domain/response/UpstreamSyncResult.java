package com.ruoyi.integration.domain.response;

/**
 * 上游同步结果。
 */
public class UpstreamSyncResult
{
    private int warehouseCount;
    private int logisticsChannelCount;
    private int skuCount;
    private String syncBatchId;

    public int getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(int warehouseCount) { this.warehouseCount = warehouseCount; }
    public int getLogisticsChannelCount() { return logisticsChannelCount; }
    public void setLogisticsChannelCount(int logisticsChannelCount) { this.logisticsChannelCount = logisticsChannelCount; }
    public int getSkuCount() { return skuCount; }
    public void setSkuCount(int skuCount) { this.skuCount = skuCount; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
}
