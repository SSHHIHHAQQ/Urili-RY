package com.ruoyi.integration.domain;

import java.io.Serializable;

/**
 * Integration-owned read snapshot for system warehouse pairing profiles.
 */
public class UpstreamWarehousePairingSnapshot implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long warehousePairingId;
    private String connectionCode;
    private String systemWarehouseCode;
    private String systemWarehouseName;
    private String upstreamWarehouseCode;
    private String upstreamWarehouseName;
    private String pairingRole;
    private String status;
    private String systemKind;
    private String masterWarehouseName;
    private String settlementType;

    public Long getWarehousePairingId() { return warehousePairingId; }
    public void setWarehousePairingId(Long warehousePairingId) { this.warehousePairingId = warehousePairingId; }
    public String getConnectionCode() { return connectionCode; }
    public void setConnectionCode(String connectionCode) { this.connectionCode = connectionCode; }
    public String getSystemWarehouseCode() { return systemWarehouseCode; }
    public void setSystemWarehouseCode(String systemWarehouseCode) { this.systemWarehouseCode = systemWarehouseCode; }
    public String getSystemWarehouseName() { return systemWarehouseName; }
    public void setSystemWarehouseName(String systemWarehouseName) { this.systemWarehouseName = systemWarehouseName; }
    public String getUpstreamWarehouseCode() { return upstreamWarehouseCode; }
    public void setUpstreamWarehouseCode(String upstreamWarehouseCode) { this.upstreamWarehouseCode = upstreamWarehouseCode; }
    public String getUpstreamWarehouseName() { return upstreamWarehouseName; }
    public void setUpstreamWarehouseName(String upstreamWarehouseName) { this.upstreamWarehouseName = upstreamWarehouseName; }
    public String getPairingRole() { return pairingRole; }
    public void setPairingRole(String pairingRole) { this.pairingRole = pairingRole; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSystemKind() { return systemKind; }
    public void setSystemKind(String systemKind) { this.systemKind = systemKind; }
    public String getMasterWarehouseName() { return masterWarehouseName; }
    public void setMasterWarehouseName(String masterWarehouseName) { this.masterWarehouseName = masterWarehouseName; }
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
}
