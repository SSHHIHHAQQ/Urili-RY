package com.ruoyi.integration.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 领星 SKU 同步清单。
 */
public class UpstreamSkuSyncItem
{
    private String connectionCode;
    private String masterSku;
    private String masterProductName;
    private String status;
    private String searchText;
    private String syncBatchId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastSeenTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String pairingStatus;
    private Long skuPairingId;
    private String systemSku;
    private String systemSkuName;
    private String customerName;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getMasterProductName() { return masterProductName; }
    public void setMasterProductName(String masterProductName) { this.masterProductName = masterProductName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public String getSyncBatchId() { return syncBatchId; }
    public void setSyncBatchId(String syncBatchId) { this.syncBatchId = syncBatchId; }
    public Date getFirstSeenTime() { return firstSeenTime; }
    public void setFirstSeenTime(Date firstSeenTime) { this.firstSeenTime = firstSeenTime; }
    public Date getLastSeenTime() { return lastSeenTime; }
    public void setLastSeenTime(Date lastSeenTime) { this.lastSeenTime = lastSeenTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getPairingStatus() { return pairingStatus; }
    public void setPairingStatus(String pairingStatus) { this.pairingStatus = pairingStatus; }
    public Long getSkuPairingId() { return skuPairingId; }
    public void setSkuPairingId(Long skuPairingId) { this.skuPairingId = skuPairingId; }
    public String getSystemSku() { return systemSku; }
    public void setSystemSku(String systemSku) { this.systemSku = systemSku; }
    public String getSystemSkuName() { return systemSkuName; }
    public void setSystemSkuName(String systemSkuName) { this.systemSkuName = systemSkuName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
