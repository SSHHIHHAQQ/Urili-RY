package com.ruoyi.integration.domain.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 上游同步结果。
 */
public class UpstreamSyncResult
{
    private int warehouseCount;
    private int logisticsChannelCount;
    private int skuCount;
    private int skuDimensionCount;
    private int warehouseStockCount;
    private String syncBatchId;
    private String requestNo;
    private Long requestId;
    private int taskCount;
    private List<UpstreamSyncItemResult> items = new ArrayList<>();

    public int getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(int warehouseCount) { this.warehouseCount = warehouseCount; }
    public int getLogisticsChannelCount() { return logisticsChannelCount; }
    public void setLogisticsChannelCount(int logisticsChannelCount) { this.logisticsChannelCount = logisticsChannelCount; }
    public int getSkuCount() { return skuCount; }
    public void setSkuCount(int skuCount) { this.skuCount = skuCount; }
    public int getSkuDimensionCount() { return skuDimensionCount; }
    public void setSkuDimensionCount(int skuDimensionCount) { this.skuDimensionCount = skuDimensionCount; }
    public int getWarehouseStockCount() { return warehouseStockCount; }
    public void setWarehouseStockCount(int warehouseStockCount) { this.warehouseStockCount = warehouseStockCount; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }
    public List<UpstreamSyncItemResult> getItems() { return items; }
    public void setItems(List<UpstreamSyncItemResult> items) { this.items = items; }
}
