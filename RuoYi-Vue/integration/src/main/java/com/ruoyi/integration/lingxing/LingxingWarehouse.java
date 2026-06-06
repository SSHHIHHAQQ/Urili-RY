package com.ruoyi.integration.lingxing;

public class LingxingWarehouse
{
    private String warehouseCode;
    private String warehouseName;
    private String countryCode;
    private String sourcePayloadJson;
    private String sourcePayloadHash;

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getSourcePayloadJson() { return sourcePayloadJson; }
    public void setSourcePayloadJson(String sourcePayloadJson) { this.sourcePayloadJson = sourcePayloadJson; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
}
