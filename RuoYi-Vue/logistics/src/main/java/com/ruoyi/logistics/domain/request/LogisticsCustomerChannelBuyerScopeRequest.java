package com.ruoyi.logistics.domain.request;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 客户渠道买家范围请求。
 */
public class LogisticsCustomerChannelBuyerScopeRequest
{
    @NotBlank(message = "买家范围模式不能为空")
    @Size(max = 16, message = "买家范围模式不能超过16个字符")
    private String buyerScopeMode;

    private List<Long> buyerIds = new ArrayList<>();

    public String getBuyerScopeMode()
    {
        return buyerScopeMode;
    }

    public void setBuyerScopeMode(String buyerScopeMode)
    {
        this.buyerScopeMode = buyerScopeMode;
    }

    public List<Long> getBuyerIds()
    {
        return buyerIds;
    }

    public void setBuyerIds(List<Long> buyerIds)
    {
        this.buyerIds = buyerIds;
    }
}
