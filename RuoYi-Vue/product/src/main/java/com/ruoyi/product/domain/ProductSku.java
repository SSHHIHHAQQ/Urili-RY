package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品 SKU，backed by product_sku.
 */
public class ProductSku extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Long spuId;
    private Long sellerId;
    private String systemSpuCode;
    private String sellerSpuCode;
    private String sellerName;
    private Long categoryId;
    private String categoryName;
    private String productName;
    private String productNameEn;
    private String spuStatus;
    private String keyword;
    private String skuCode;
    private String sourceWarehouseCode;
    private String systemSkuCode;
    private String sellerSkuCode;
    private String color;
    private String size;
    private String lengthValue;
    private String widthValue;
    private String heightValue;
    private String weight;
    private String material;
    private String style;
    private String model;
    private String packageQuantity;
    private String capacity;
    private String skuImageUrl;
    private BigDecimal supplyPrice;
    private BigDecimal salePrice;
    private String currencyCode;
    private String warehouseKindSummary;
    private String skuStatus;
    private String controlStatus;
    private String spuControlStatus;
    private String controlReason;
    private String controlBy;
    private Date controlTime;
    private String recoverBy;
    private Date recoverTime;
    private Integer sortOrder;
    private String delFlag;
    private Long availableStock;
    private Integer warehouseCount;
    private String inventoryStatus;
    private Date stockUpdateTime;
    private Long latestReviewId;
    private String latestReviewNo;
    private String latestReviewStatus;
    private String latestReviewFeedback;
    private Date latestReviewTime;
    private Long sourceBindingId;
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
    private String sourceWarehouseNames;
    private Integer sourceWarehouseCount;
    private String bindingStatus;
    private String lockStatus;
    private Date lockedTime;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public String getSellerSpuCode() { return sellerSpuCode; }
    public void setSellerSpuCode(String sellerSpuCode) { this.sellerSpuCode = sellerSpuCode; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductNameEn() { return productNameEn; }
    public void setProductNameEn(String productNameEn) { this.productNameEn = productNameEn; }
    public String getSpuStatus() { return spuStatus; }
    public void setSpuStatus(String spuStatus) { this.spuStatus = spuStatus; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSourceWarehouseCode() { return sourceWarehouseCode; }
    public void setSourceWarehouseCode(String sourceWarehouseCode) { this.sourceWarehouseCode = sourceWarehouseCode; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getSellerSkuCode() { return sellerSkuCode; }
    public void setSellerSkuCode(String sellerSkuCode) { this.sellerSkuCode = sellerSkuCode; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getLengthValue() { return lengthValue; }
    public void setLengthValue(String lengthValue) { this.lengthValue = lengthValue; }
    public String getWidthValue() { return widthValue; }
    public void setWidthValue(String widthValue) { this.widthValue = widthValue; }
    public String getHeightValue() { return heightValue; }
    public void setHeightValue(String heightValue) { this.heightValue = heightValue; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPackageQuantity() { return packageQuantity; }
    public void setPackageQuantity(String packageQuantity) { this.packageQuantity = packageQuantity; }
    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }
    public String getSkuImageUrl() { return skuImageUrl; }
    public void setSkuImageUrl(String skuImageUrl) { this.skuImageUrl = skuImageUrl; }
    public BigDecimal getSupplyPrice() { return supplyPrice; }
    public void setSupplyPrice(BigDecimal supplyPrice) { this.supplyPrice = supplyPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getWarehouseKindSummary() { return warehouseKindSummary; }
    public void setWarehouseKindSummary(String warehouseKindSummary) { this.warehouseKindSummary = warehouseKindSummary; }
    public String getSkuStatus() { return skuStatus; }
    public void setSkuStatus(String skuStatus) { this.skuStatus = skuStatus; }
    public String getControlStatus() { return controlStatus; }
    public void setControlStatus(String controlStatus) { this.controlStatus = controlStatus; }
    public String getSpuControlStatus() { return spuControlStatus; }
    public void setSpuControlStatus(String spuControlStatus) { this.spuControlStatus = spuControlStatus; }
    public String getControlReason() { return controlReason; }
    public void setControlReason(String controlReason) { this.controlReason = controlReason; }
    public String getControlBy() { return controlBy; }
    public void setControlBy(String controlBy) { this.controlBy = controlBy; }
    public Date getControlTime() { return controlTime; }
    public void setControlTime(Date controlTime) { this.controlTime = controlTime; }
    public String getRecoverBy() { return recoverBy; }
    public void setRecoverBy(String recoverBy) { this.recoverBy = recoverBy; }
    public Date getRecoverTime() { return recoverTime; }
    public void setRecoverTime(Date recoverTime) { this.recoverTime = recoverTime; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Long getAvailableStock() { return availableStock; }
    public void setAvailableStock(Long availableStock) { this.availableStock = availableStock; }
    public Integer getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(Integer warehouseCount) { this.warehouseCount = warehouseCount; }
    public String getInventoryStatus() { return inventoryStatus; }
    public void setInventoryStatus(String inventoryStatus) { this.inventoryStatus = inventoryStatus; }
    public Date getStockUpdateTime() { return stockUpdateTime; }
    public void setStockUpdateTime(Date stockUpdateTime) { this.stockUpdateTime = stockUpdateTime; }
    public Long getLatestReviewId() { return latestReviewId; }
    public void setLatestReviewId(Long latestReviewId) { this.latestReviewId = latestReviewId; }
    public String getLatestReviewNo() { return latestReviewNo; }
    public void setLatestReviewNo(String latestReviewNo) { this.latestReviewNo = latestReviewNo; }
    public String getLatestReviewStatus() { return latestReviewStatus; }
    public void setLatestReviewStatus(String latestReviewStatus) { this.latestReviewStatus = latestReviewStatus; }
    public String getLatestReviewFeedback() { return latestReviewFeedback; }
    public void setLatestReviewFeedback(String latestReviewFeedback) { this.latestReviewFeedback = latestReviewFeedback; }
    public Date getLatestReviewTime() { return latestReviewTime; }
    public void setLatestReviewTime(Date latestReviewTime) { this.latestReviewTime = latestReviewTime; }
    public Long getSourceBindingId() { return sourceBindingId; }
    public void setSourceBindingId(Long sourceBindingId) { this.sourceBindingId = sourceBindingId; }
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
}
