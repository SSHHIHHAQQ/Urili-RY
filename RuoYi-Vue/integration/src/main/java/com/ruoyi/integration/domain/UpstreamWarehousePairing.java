package com.ruoyi.integration.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统仓库与领星仓库配对。
 */
public class UpstreamWarehousePairing extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long warehousePairingId;
    private String connectionCode;
    private String upstreamWarehouseCode;
    private String upstreamWarehouseName;
    private String systemWarehouseCode;
    private String systemWarehouseName;
    private String status;

    public Long getWarehousePairingId() { return warehousePairingId; }
    public void setWarehousePairingId(Long warehousePairingId) { this.warehousePairingId = warehousePairingId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getUpstreamWarehouseCode() { return upstreamWarehouseCode; }
    public void setUpstreamWarehouseCode(String upstreamWarehouseCode) { this.upstreamWarehouseCode = upstreamWarehouseCode; }
    public String getUpstreamWarehouseName() { return upstreamWarehouseName; }
    public void setUpstreamWarehouseName(String upstreamWarehouseName) { this.upstreamWarehouseName = upstreamWarehouseName; }
    public String getSystemWarehouseCode() { return systemWarehouseCode; }
    public void setSystemWarehouseCode(String systemWarehouseCode) { this.systemWarehouseCode = systemWarehouseCode; }
    public String getSystemWarehouseName() { return systemWarehouseName; }
    public void setSystemWarehouseName(String systemWarehouseName) { this.systemWarehouseName = systemWarehouseName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
