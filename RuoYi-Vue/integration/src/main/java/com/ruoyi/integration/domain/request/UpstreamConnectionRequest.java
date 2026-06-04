package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 主仓接入保存请求。
 */
public class UpstreamConnectionRequest
{
    private String connectionCode;

    @Size(max = 32, message = "上游系统类型长度不能超过32个字符")
    private String systemKind;

    @NotBlank(message = "主仓名称不能为空")
    @Size(max = 200, message = "主仓名称长度不能超过200个字符")
    private String masterWarehouseName;

    @NotBlank(message = "结算类型不能为空")
    @Size(max = 32, message = "结算类型长度不能超过32个字符")
    private String settlementType;

    @NotBlank(message = "appKey不能为空")
    private String appKey;

    @NotBlank(message = "appSecret不能为空")
    private String appSecret;

    private String remark;

    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSystemKind() { return systemKind; }
    public void setSystemKind(String systemKind) { this.systemKind = systemKind; }
    public String getMasterWarehouseName() { return masterWarehouseName; }
    public void setMasterWarehouseName(String masterWarehouseName) { this.masterWarehouseName = masterWarehouseName; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
    public String getAppKey() { return appKey; }
    public void setAppKey(String appKey) { this.appKey = appKey; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
