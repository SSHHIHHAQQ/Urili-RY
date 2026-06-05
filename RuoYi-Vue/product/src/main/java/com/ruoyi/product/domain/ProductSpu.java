package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品 SPU，backed by product_spu.
 */
public class ProductSpu extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long spuId;
    private String systemSpuCode;
    private String sellerSpuCode;
    private Long sellerId;
    private String sellerNo;
    private String sellerName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String productName;
    private String productNameEn;
    private String sellingPoint;
    private String mainImageUrl;
    private String detailContent;
    private String spuStatus;
    private String sourceType;
    private String sourceRefType;
    private String sourceRefId;
    private String delFlag;
    private String keyword;
    private String systemSkuCode;
    private String sellerSkuCode;
    private Integer skuCount;
    private BigDecimal supplyPriceMin;
    private BigDecimal supplyPriceMax;
    private BigDecimal salePriceMin;
    private BigDecimal salePriceMax;
    private String currencySummary;
    private Integer warehouseCount;
    private List<ProductSku> skus;
    private List<ProductAttributeValue> attributeValues;
    private List<ProductImage> images;

    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public String getSellerSpuCode() { return sellerSpuCode; }
    public void setSellerSpuCode(String sellerSpuCode) { this.sellerSpuCode = sellerSpuCode; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerNo() { return sellerNo; }
    public void setSellerNo(String sellerNo) { this.sellerNo = sellerNo; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
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
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceRefType() { return sourceRefType; }
    public void setSourceRefType(String sourceRefType) { this.sourceRefType = sourceRefType; }
    public String getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(String sourceRefId) { this.sourceRefId = sourceRefId; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getSellerSkuCode() { return sellerSkuCode; }
    public void setSellerSkuCode(String sellerSkuCode) { this.sellerSkuCode = sellerSkuCode; }
    public Integer getSkuCount() { return skuCount; }
    public void setSkuCount(Integer skuCount) { this.skuCount = skuCount; }
    public BigDecimal getSupplyPriceMin() { return supplyPriceMin; }
    public void setSupplyPriceMin(BigDecimal supplyPriceMin) { this.supplyPriceMin = supplyPriceMin; }
    public BigDecimal getSupplyPriceMax() { return supplyPriceMax; }
    public void setSupplyPriceMax(BigDecimal supplyPriceMax) { this.supplyPriceMax = supplyPriceMax; }
    public BigDecimal getSalePriceMin() { return salePriceMin; }
    public void setSalePriceMin(BigDecimal salePriceMin) { this.salePriceMin = salePriceMin; }
    public BigDecimal getSalePriceMax() { return salePriceMax; }
    public void setSalePriceMax(BigDecimal salePriceMax) { this.salePriceMax = salePriceMax; }
    public String getCurrencySummary() { return currencySummary; }
    public void setCurrencySummary(String currencySummary) { this.currencySummary = currencySummary; }
    public Integer getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(Integer warehouseCount) { this.warehouseCount = warehouseCount; }
    public List<ProductSku> getSkus() { return skus; }
    public void setSkus(List<ProductSku> skus) { this.skus = skus; }
    public List<ProductAttributeValue> getAttributeValues() { return attributeValues; }
    public void setAttributeValues(List<ProductAttributeValue> attributeValues) { this.attributeValues = attributeValues; }
    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }
}
