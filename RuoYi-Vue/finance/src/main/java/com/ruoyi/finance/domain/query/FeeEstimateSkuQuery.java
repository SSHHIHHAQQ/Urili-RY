package com.ruoyi.finance.domain.query;

/**
 * Structured SKU filters for fee estimate product selection.
 */
public class FeeEstimateSkuQuery
{
    private String sourceWarehouseCode;

    private String skuCode;

    private String productName;

    public String getSourceWarehouseCode()
    {
        return sourceWarehouseCode;
    }

    public void setSourceWarehouseCode(String sourceWarehouseCode)
    {
        this.sourceWarehouseCode = sourceWarehouseCode;
    }

    public String getSkuCode()
    {
        return skuCode;
    }

    public void setSkuCode(String skuCode)
    {
        this.skuCode = skuCode;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }
}
