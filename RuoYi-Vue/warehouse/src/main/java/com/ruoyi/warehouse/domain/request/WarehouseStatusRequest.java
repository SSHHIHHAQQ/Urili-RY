package com.ruoyi.warehouse.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 仓库启停请求。
 */
public class WarehouseStatusRequest
{
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @NotBlank(message = "状态不能为空")
    private String status;

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
