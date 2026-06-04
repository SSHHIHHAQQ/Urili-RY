package com.ruoyi.integration.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 主仓接入信息更新请求，不包含凭证。
 */
public class UpstreamConnectionInfoRequest
{
    @NotBlank(message = "主仓名称不能为空")
    @Size(max = 200, message = "主仓名称长度不能超过200个字符")
    private String masterWarehouseName;

    @NotBlank(message = "结算类型不能为空")
    @Size(max = 32, message = "结算类型长度不能超过32个字符")
    private String settlementType;

    private String remark;

    public String getMasterWarehouseName() { return masterWarehouseName; }
    public void setMasterWarehouseName(String masterWarehouseName) { this.masterWarehouseName = masterWarehouseName; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
