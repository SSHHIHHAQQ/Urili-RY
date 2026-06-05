package com.ruoyi.warehouse.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyi.warehouse.domain.Warehouse;

/**
 * 官方仓同步创建请求。
 */
public class OfficialWarehouseSyncRequest extends Warehouse
{
    private static final long serialVersionUID = 1L;

    private String connectionCode;

    private String upstreamWarehouseCode;

    @NotBlank(message = "主仓接入编号不能为空")
    @Size(max = 64, message = "主仓接入编号长度不能超过64个字符")
    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    @NotBlank(message = "上游仓库编码不能为空")
    @Size(max = 100, message = "上游仓库编码长度不能超过100个字符")
    public String getUpstreamWarehouseCode()
    {
        return upstreamWarehouseCode;
    }

    public void setUpstreamWarehouseCode(String upstreamWarehouseCode)
    {
        this.upstreamWarehouseCode = upstreamWarehouseCode;
    }
}
