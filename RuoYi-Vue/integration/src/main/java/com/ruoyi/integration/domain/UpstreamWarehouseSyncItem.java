package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 领星仓库同步清单。
 */
public class UpstreamWarehouseSyncItem
{
    private String connectionCode;

    private String warehouseCode;

    private String warehouseName;

    private String countryCode;

    private String status;

    private String syncBatchId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public Date getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Date firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Date getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Date lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
