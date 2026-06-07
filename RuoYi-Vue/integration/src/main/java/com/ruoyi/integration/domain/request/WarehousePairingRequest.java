package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 仓库配对请求。
 */
public class WarehousePairingRequest
{
    @NotBlank(message = "领星仓库代码不能为空")
    private String upstreamWarehouseCode;

    @NotBlank(message = "系统仓库代码不能为空")
    private String systemWarehouseCode;

    @NotBlank(message = "系统仓库名称不能为空")
    private String systemWarehouseName;

    private String pairingRole;

    private String remark;

    public String getUpstreamWarehouseCode() { return upstreamWarehouseCode; }
    public void setUpstreamWarehouseCode(String upstreamWarehouseCode) { this.upstreamWarehouseCode = upstreamWarehouseCode; }
    public String getSystemWarehouseCode() { return systemWarehouseCode; }
    public void setSystemWarehouseCode(String systemWarehouseCode) { this.systemWarehouseCode = systemWarehouseCode; }
    public String getSystemWarehouseName() { return systemWarehouseName; }
    public void setSystemWarehouseName(String systemWarehouseName) { this.systemWarehouseName = systemWarehouseName; }
    public String getPairingRole() { return pairingRole; }
    public void setPairingRole(String pairingRole) { this.pairingRole = pairingRole; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
