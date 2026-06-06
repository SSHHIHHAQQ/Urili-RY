package com.ruoyi.integration.domain;

/**
 * 来源商品库列表项。
 */
public class SourceProductItem extends UpstreamSkuSyncItem
{
    private String sourceGroupKey;

    private String sourceSkuGroupKey;

    private String sourceDimensionGroupKey;

    private String systemKind;

    private String systemKindLabel;

    private String masterWarehouseName;

    private String sourceConnectionCodes;

    private String sourceWarehouseNames;

    private Integer warehouseCount;

    private Integer sourceRowCount;

    public String getSourceGroupKey()
    {
        return sourceGroupKey;
    }

    public void setSourceGroupKey(String sourceGroupKey)
    {
        this.sourceGroupKey = sourceGroupKey;
    }

    public String getSourceSkuGroupKey()
    {
        return sourceSkuGroupKey;
    }

    public void setSourceSkuGroupKey(String sourceSkuGroupKey)
    {
        this.sourceSkuGroupKey = sourceSkuGroupKey;
    }

    public String getSourceDimensionGroupKey()
    {
        return sourceDimensionGroupKey;
    }

    public void setSourceDimensionGroupKey(String sourceDimensionGroupKey)
    {
        this.sourceDimensionGroupKey = sourceDimensionGroupKey;
    }

    public String getSystemKind()
    {
        return systemKind;
    }

    public void setSystemKind(String systemKind)
    {
        this.systemKind = systemKind;
    }

    public String getSystemKindLabel()
    {
        return systemKindLabel;
    }

    public void setSystemKindLabel(String systemKindLabel)
    {
        this.systemKindLabel = systemKindLabel;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }

    public String getSourceConnectionCodes()
    {
        return sourceConnectionCodes;
    }

    public void setSourceConnectionCodes(String sourceConnectionCodes)
    {
        this.sourceConnectionCodes = sourceConnectionCodes;
    }

    public String getSourceWarehouseNames()
    {
        return sourceWarehouseNames;
    }

    public void setSourceWarehouseNames(String sourceWarehouseNames)
    {
        this.sourceWarehouseNames = sourceWarehouseNames;
    }

    public Integer getWarehouseCount()
    {
        return warehouseCount;
    }

    public void setWarehouseCount(Integer warehouseCount)
    {
        this.warehouseCount = warehouseCount;
    }

    public Integer getSourceRowCount()
    {
        return sourceRowCount;
    }

    public void setSourceRowCount(Integer sourceRowCount)
    {
        this.sourceRowCount = sourceRowCount;
    }
}
