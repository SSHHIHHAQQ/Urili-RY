package com.ruoyi.finance.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product SKU snapshot used by the finance fee estimate tool.
 */
public class FeeEstimateSkuSnapshot
{
    private Long skuId;

    private String systemSkuCode;

    private String sellerSkuCode;

    private String productName;

    private String productNameEn;

    private String masterSku;

    private BigDecimal measureLengthCm;

    private BigDecimal measureWidthCm;

    private BigDecimal measureHeightCm;

    private BigDecimal measureWeightKg;

    private String measureSource;

    private String sourceWarehouseNames;

    private List<String> sourceWarehouseCodes;

    private Integer sourceWarehouseCount;

    private Long availableStock;

    private Integer quantity;

    private String label;

    private String searchText;

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public String getSystemSkuCode()
    {
        return systemSkuCode;
    }

    public void setSystemSkuCode(String systemSkuCode)
    {
        this.systemSkuCode = systemSkuCode;
    }

    public String getSellerSkuCode()
    {
        return sellerSkuCode;
    }

    public void setSellerSkuCode(String sellerSkuCode)
    {
        this.sellerSkuCode = sellerSkuCode;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getProductNameEn()
    {
        return productNameEn;
    }

    public void setProductNameEn(String productNameEn)
    {
        this.productNameEn = productNameEn;
    }

    public String getMasterSku()
    {
        return masterSku;
    }

    public void setMasterSku(String masterSku)
    {
        this.masterSku = masterSku;
    }

    public BigDecimal getMeasureLengthCm()
    {
        return measureLengthCm;
    }

    public void setMeasureLengthCm(BigDecimal measureLengthCm)
    {
        this.measureLengthCm = measureLengthCm;
    }

    public BigDecimal getMeasureWidthCm()
    {
        return measureWidthCm;
    }

    public void setMeasureWidthCm(BigDecimal measureWidthCm)
    {
        this.measureWidthCm = measureWidthCm;
    }

    public BigDecimal getMeasureHeightCm()
    {
        return measureHeightCm;
    }

    public void setMeasureHeightCm(BigDecimal measureHeightCm)
    {
        this.measureHeightCm = measureHeightCm;
    }

    public BigDecimal getMeasureWeightKg()
    {
        return measureWeightKg;
    }

    public void setMeasureWeightKg(BigDecimal measureWeightKg)
    {
        this.measureWeightKg = measureWeightKg;
    }

    public String getMeasureSource()
    {
        return measureSource;
    }

    public void setMeasureSource(String measureSource)
    {
        this.measureSource = measureSource;
    }

    public String getSourceWarehouseNames()
    {
        return sourceWarehouseNames;
    }

    public void setSourceWarehouseNames(String sourceWarehouseNames)
    {
        this.sourceWarehouseNames = sourceWarehouseNames;
    }

    public List<String> getSourceWarehouseCodes()
    {
        return sourceWarehouseCodes;
    }

    public void setSourceWarehouseCodes(List<String> sourceWarehouseCodes)
    {
        this.sourceWarehouseCodes = sourceWarehouseCodes;
    }

    public Integer getSourceWarehouseCount()
    {
        return sourceWarehouseCount;
    }

    public void setSourceWarehouseCount(Integer sourceWarehouseCount)
    {
        this.sourceWarehouseCount = sourceWarehouseCount;
    }

    public Long getAvailableStock()
    {
        return availableStock;
    }

    public void setAvailableStock(Long availableStock)
    {
        this.availableStock = availableStock;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getSearchText()
    {
        return searchText;
    }

    public void setSearchText(String searchText)
    {
        this.searchText = searchText;
    }
}
