package com.ruoyi.inventory.domain;

/**
 * Product-owned active source binding snapshot consumed by inventory refresh.
 */
public class InventoryProductSourceBindingSnapshot
{
    private Long skuId;
    private String sourceScope;
    private String masterSku;
    private String masterProductName;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public String getSourceScope() { return sourceScope; }
    public void setSourceScope(String sourceScope) { this.sourceScope = sourceScope; }

    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }

    public String getMasterProductName() { return masterProductName; }
    public void setMasterProductName(String masterProductName) { this.masterProductName = masterProductName; }
}
