package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * SKU 配对请求。
 */
public class SkuPairingRequest
{
    @NotBlank(message = "领星masterSku不能为空")
    private String masterSku;

    @NotBlank(message = "系统SKU不能为空")
    private String systemSku;

    @NotBlank(message = "系统SKU名称不能为空")
    private String systemSkuName;

    private String customerName;

    private String remark;

    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getSystemSku() { return systemSku; }
    public void setSystemSku(String systemSku) { this.systemSku = systemSku; }
    public String getSystemSkuName() { return systemSkuName; }
    public void setSystemSkuName(String systemSkuName) { this.systemSkuName = systemSkuName; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
