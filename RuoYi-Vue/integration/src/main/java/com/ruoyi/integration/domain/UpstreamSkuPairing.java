package com.ruoyi.integration.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统 SKU 与领星 masterSku 配对。
 */
public class UpstreamSkuPairing extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long skuPairingId;
    private String connectionCode;
    private String masterSku;
    private String systemSku;
    private String systemSkuName;
    private String customerName;

    public Long getSkuPairingId() { return skuPairingId; }
    public void setSkuPairingId(Long skuPairingId) { this.skuPairingId = skuPairingId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getSystemSku() { return systemSku; }
    public void setSystemSku(String systemSku) { this.systemSku = systemSku; }
    public String getSystemSkuName() { return systemSkuName; }
    public void setSystemSkuName(String systemSkuName) { this.systemSkuName = systemSkuName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
