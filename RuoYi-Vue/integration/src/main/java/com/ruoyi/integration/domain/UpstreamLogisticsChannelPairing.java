package com.ruoyi.integration.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统物流渠道与领星物流渠道配对。
 */
public class UpstreamLogisticsChannelPairing extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long logisticsChannelPairingId;
    private String connectionCode;
    private String systemWarehouseCode;
    private String upstreamWarehouseCode;
    private String upstreamChannelCode;
    private String upstreamChannelName;
    private String systemChannelCode;
    private String systemChannelName;
    private String pairingRole;
    private String status;

    public Long getLogisticsChannelPairingId() { return logisticsChannelPairingId; }
    public void setLogisticsChannelPairingId(Long logisticsChannelPairingId) { this.logisticsChannelPairingId = logisticsChannelPairingId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSystemWarehouseCode() { return systemWarehouseCode; }
    public void setSystemWarehouseCode(String systemWarehouseCode) { this.systemWarehouseCode = systemWarehouseCode; }
    public String getUpstreamWarehouseCode() { return upstreamWarehouseCode; }
    public void setUpstreamWarehouseCode(String upstreamWarehouseCode) { this.upstreamWarehouseCode = upstreamWarehouseCode; }
    public String getUpstreamChannelCode() { return upstreamChannelCode; }
    public void setUpstreamChannelCode(String upstreamChannelCode) { this.upstreamChannelCode = upstreamChannelCode; }
    public String getUpstreamChannelName() { return upstreamChannelName; }
    public void setUpstreamChannelName(String upstreamChannelName) { this.upstreamChannelName = upstreamChannelName; }
    public String getSystemChannelCode() { return systemChannelCode; }
    public void setSystemChannelCode(String systemChannelCode) { this.systemChannelCode = systemChannelCode; }
    public String getSystemChannelName() { return systemChannelName; }
    public void setSystemChannelName(String systemChannelName) { this.systemChannelName = systemChannelName; }
    public String getPairingRole() { return pairingRole; }
    public void setPairingRole(String pairingRole) { this.pairingRole = pairingRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
