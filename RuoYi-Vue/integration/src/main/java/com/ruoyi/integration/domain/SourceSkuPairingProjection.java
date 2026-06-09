package com.ruoyi.integration.domain;

/**
 * Product-owned SKU binding projected into upstream SKU pairing.
 */
public class SourceSkuPairingProjection
{
    private String sourceDimensionGroupKey;
    private String systemSkuCode;
    private String systemSkuName;
    private String sellerName;
    private String updateBy;

    public String getSourceDimensionGroupKey() { return sourceDimensionGroupKey; }
    public void setSourceDimensionGroupKey(String sourceDimensionGroupKey) { this.sourceDimensionGroupKey = sourceDimensionGroupKey; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getSystemSkuName() { return systemSkuName; }
    public void setSystemSkuName(String systemSkuName) { this.systemSkuName = systemSkuName; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
