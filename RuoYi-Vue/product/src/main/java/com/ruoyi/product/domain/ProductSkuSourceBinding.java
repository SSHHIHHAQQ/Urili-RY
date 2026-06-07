package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城 SKU 来源绑定主事实，backed by product_sku_source_binding.
 */
public class ProductSkuSourceBinding extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long bindingId;
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String systemSkuCode;
    private String sourceScope;
    private String sourceSkuGroupKey;
    private String sourceDimensionGroupKey;
    private String masterSku;
    private String masterProductNameSnapshot;
    private String systemSkuNameSnapshot;
    private String sellerNameSnapshot;
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
    private String bindingStatus;
    private String lockStatus;
    private Date lockedTime;
    private String lockedBy;
    private String releaseReason;
    private String replaceReason;
    private Long activeSkuKey;
    private String activeSourceKey;

    public Long getBindingId() { return bindingId; }
    public void setBindingId(Long bindingId) { this.bindingId = bindingId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
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
    public String getSystemSkuNameSnapshot() { return systemSkuNameSnapshot; }
    public void setSystemSkuNameSnapshot(String systemSkuNameSnapshot) { this.systemSkuNameSnapshot = systemSkuNameSnapshot; }
    public String getSellerNameSnapshot() { return sellerNameSnapshot; }
    public void setSellerNameSnapshot(String sellerNameSnapshot) { this.sellerNameSnapshot = sellerNameSnapshot; }
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
    public String getBindingStatus() { return bindingStatus; }
    public void setBindingStatus(String bindingStatus) { this.bindingStatus = bindingStatus; }
    public String getLockStatus() { return lockStatus; }
    public void setLockStatus(String lockStatus) { this.lockStatus = lockStatus; }
    public Date getLockedTime() { return lockedTime; }
    public void setLockedTime(Date lockedTime) { this.lockedTime = lockedTime; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String releaseReason) { this.releaseReason = releaseReason; }
    public String getReplaceReason() { return replaceReason; }
    public void setReplaceReason(String replaceReason) { this.replaceReason = replaceReason; }
    public Long getActiveSkuKey() { return activeSkuKey; }
    public void setActiveSkuKey(Long activeSkuKey) { this.activeSkuKey = activeSkuKey; }
    public String getActiveSourceKey() { return activeSourceKey; }
    public void setActiveSourceKey(String activeSourceKey) { this.activeSourceKey = activeSourceKey; }
}
