package com.ruoyi.inventory.domain;

/**
 * 来源 SKU 身份键，用于库存模块把来源库存影响范围转换为平台 SPU。
 */
public class InventorySourceSkuKey
{
    private String sourceScope;
    private String masterSku;
    private String masterProductName;

    public String getSourceScope()
    {
        return sourceScope;
    }

    public void setSourceScope(String sourceScope)
    {
        this.sourceScope = sourceScope;
    }

    public String getMasterSku()
    {
        return masterSku;
    }

    public void setMasterSku(String masterSku)
    {
        this.masterSku = masterSku;
    }

    public String getMasterProductName()
    {
        return masterProductName;
    }

    public void setMasterProductName(String masterProductName)
    {
        this.masterProductName = masterProductName;
    }
}
