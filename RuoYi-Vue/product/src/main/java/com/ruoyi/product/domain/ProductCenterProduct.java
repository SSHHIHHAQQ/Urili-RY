package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Buyer-facing product center response without seller, cost, or admin-only fields.
 */
public class ProductCenterProduct
{
    private Long spuId;
    private String systemSpuCode;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String productName;
    private String productNameEn;
    private String sellingPoint;
    private String mainImageUrl;
    private String detailContent;
    private String spuStatus;
    private Integer skuCount;
    private List<String> visibleSystemSkuCodes;
    private BigDecimal salePriceMin;
    private BigDecimal salePriceMax;
    private String currencySummary;
    private String warehouseKindSummary;
    private Long availableStock;
    private Integer warehouseCount;
    private String inventoryStatus;
    private Date stockUpdateTime;
    private List<String> galleryUrls;
    private List<ProductCenterSku> skus;
    private List<ProductCenterWarehouse> warehouses;
    private List<ProductCenterAttribute> attributes;

    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductNameEn() { return productNameEn; }
    public void setProductNameEn(String productNameEn) { this.productNameEn = productNameEn; }
    public String getSellingPoint() { return sellingPoint; }
    public void setSellingPoint(String sellingPoint) { this.sellingPoint = sellingPoint; }
    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }
    public String getDetailContent() { return detailContent; }
    public void setDetailContent(String detailContent) { this.detailContent = detailContent; }
    public String getSpuStatus() { return spuStatus; }
    public void setSpuStatus(String spuStatus) { this.spuStatus = spuStatus; }
    public Integer getSkuCount() { return skuCount; }
    public void setSkuCount(Integer skuCount) { this.skuCount = skuCount; }
    public List<String> getVisibleSystemSkuCodes() { return visibleSystemSkuCodes; }
    public void setVisibleSystemSkuCodes(List<String> visibleSystemSkuCodes) { this.visibleSystemSkuCodes = visibleSystemSkuCodes; }
    public BigDecimal getSalePriceMin() { return salePriceMin; }
    public void setSalePriceMin(BigDecimal salePriceMin) { this.salePriceMin = salePriceMin; }
    public BigDecimal getSalePriceMax() { return salePriceMax; }
    public void setSalePriceMax(BigDecimal salePriceMax) { this.salePriceMax = salePriceMax; }
    public String getCurrencySummary() { return currencySummary; }
    public void setCurrencySummary(String currencySummary) { this.currencySummary = currencySummary; }
    public String getWarehouseKindSummary() { return warehouseKindSummary; }
    public void setWarehouseKindSummary(String warehouseKindSummary) { this.warehouseKindSummary = warehouseKindSummary; }
    public Long getAvailableStock() { return availableStock; }
    public void setAvailableStock(Long availableStock) { this.availableStock = availableStock; }
    public Integer getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(Integer warehouseCount) { this.warehouseCount = warehouseCount; }
    public String getInventoryStatus() { return inventoryStatus; }
    public void setInventoryStatus(String inventoryStatus) { this.inventoryStatus = inventoryStatus; }
    public Date getStockUpdateTime() { return stockUpdateTime; }
    public void setStockUpdateTime(Date stockUpdateTime) { this.stockUpdateTime = stockUpdateTime; }
    public List<String> getGalleryUrls() { return galleryUrls; }
    public void setGalleryUrls(List<String> galleryUrls) { this.galleryUrls = galleryUrls; }
    public List<ProductCenterSku> getSkus() { return skus; }
    public void setSkus(List<ProductCenterSku> skus) { this.skus = skus; }
    public List<ProductCenterWarehouse> getWarehouses() { return warehouses; }
    public void setWarehouses(List<ProductCenterWarehouse> warehouses) { this.warehouses = warehouses; }
    public List<ProductCenterAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<ProductCenterAttribute> attributes) { this.attributes = attributes; }
}
