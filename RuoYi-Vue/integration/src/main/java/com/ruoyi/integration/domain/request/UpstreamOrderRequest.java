package com.ruoyi.integration.domain.request;

import java.util.List;

/**
 * 主仓接入排序请求。
 */
public class UpstreamOrderRequest
{
    private List<String> connectionCodes;

    public List<String> getConnectionCodes() { return connectionCodes; }
    public void setConnectionCodes(List<String> connectionCodes) { this.connectionCodes = connectionCodes; }
}
