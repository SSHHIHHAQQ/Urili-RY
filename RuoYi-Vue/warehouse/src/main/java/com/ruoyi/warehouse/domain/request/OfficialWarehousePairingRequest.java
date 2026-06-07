package com.ruoyi.warehouse.domain.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 官方仓库手工配对请求。
 */
public class OfficialWarehousePairingRequest
{
    @NotBlank(message = "配对仓库类型不能为空")
    private String pairingRole;

    private String connectionCode;

    private String upstreamWarehouseCode;

    private Boolean unpair;

    public String getPairingRole()
    {
        return pairingRole;
    }

    public void setPairingRole(String pairingRole)
    {
        this.pairingRole = pairingRole;
    }

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getUpstreamWarehouseCode()
    {
        return upstreamWarehouseCode;
    }

    public void setUpstreamWarehouseCode(String upstreamWarehouseCode)
    {
        this.upstreamWarehouseCode = upstreamWarehouseCode;
    }

    public Boolean getUnpair()
    {
        return unpair;
    }

    public void setUnpair(Boolean unpair)
    {
        this.unpair = unpair;
    }
}
