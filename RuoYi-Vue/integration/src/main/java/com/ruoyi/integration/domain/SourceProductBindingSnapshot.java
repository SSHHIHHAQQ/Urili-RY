package com.ruoyi.integration.domain;

import java.math.BigDecimal;

/**
 * Integration-owned source SKU snapshot for product binding.
 */
public class SourceProductBindingSnapshot
{
    private String sourceScope;
    private String sourceSkuGroupKey;
    private String sourceDimensionGroupKey;
    private String masterSku;
    private String masterProductNameSnapshot;
    private String sourcePayloadHash;
    private String wmsPayloadHash;
    private BigDecimal measureLengthCm;
    private BigDecimal measureWidthCm;
    private BigDecimal measureHeightCm;
    private BigDecimal measureWeightKg;
    private String measureSource;
    private String currencyCode;
    private String sourceWarehouseNames;
    private Integer sourceWarehouseCount;

    public String getSourceScope() { return sourceScope; }
    public void setSourceScope(String sourceScope) { this.sourceScope = sourceScope; }
    public String getSourceSkuGroupKey() { return sourceSkuGroupKey; }
    public void setSourceSkuGroupKey(String sourceSkuGroupKey) { this.sourceSkuGroupKey = sourceSkuGroupKey; }
    public String getSourceDimensionGroupKey() { return sourceDimensionGroupKey; }
    public void setSourceDimensionGroupKey(String sourceDimensionGroupKey) { this.sourceDimensionGroupKey = sourceDimensionGroupKey; }
    public String getMasterSku() { return masterSku; }
    public void setMasterSku(String masterSku) { this.masterSku = masterSku; }
    public String getMasterProductNameSnapshot() { return masterProductNameSnapshot; }
    public void setMasterProductNameSnapshot(String masterProductNameSnapshot) { this.masterProductNameSnapshot = masterProductNameSnapshot; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
    public String getWmsPayloadHash() { return wmsPayloadHash; }
    public void setWmsPayloadHash(String wmsPayloadHash) { this.wmsPayloadHash = wmsPayloadHash; }
    public BigDecimal getMeasureLengthCm() { return measureLengthCm; }
    public void setMeasureLengthCm(BigDecimal measureLengthCm) { this.measureLengthCm = measureLengthCm; }
    public BigDecimal getMeasureWidthCm() { return measureWidthCm; }
    public void setMeasureWidthCm(BigDecimal measureWidthCm) { this.measureWidthCm = measureWidthCm; }
    public BigDecimal getMeasureHeightCm() { return measureHeightCm; }
    public void setMeasureHeightCm(BigDecimal measureHeightCm) { this.measureHeightCm = measureHeightCm; }
    public BigDecimal getMeasureWeightKg() { return measureWeightKg; }
    public void setMeasureWeightKg(BigDecimal measureWeightKg) { this.measureWeightKg = measureWeightKg; }
    public String getMeasureSource() { return measureSource; }
    public void setMeasureSource(String measureSource) { this.measureSource = measureSource; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getSourceWarehouseNames() { return sourceWarehouseNames; }
    public void setSourceWarehouseNames(String sourceWarehouseNames) { this.sourceWarehouseNames = sourceWarehouseNames; }
    public Integer getSourceWarehouseCount() { return sourceWarehouseCount; }
    public void setSourceWarehouseCount(Integer sourceWarehouseCount) { this.sourceWarehouseCount = sourceWarehouseCount; }
}
