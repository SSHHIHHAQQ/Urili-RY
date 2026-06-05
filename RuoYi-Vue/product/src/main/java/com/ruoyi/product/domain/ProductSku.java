package com.ruoyi.product.domain;

import java.math.BigDecimal;
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
    private String systemSkuCode;
    private String sellerSkuCode;
    private String color;
    private String size;
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
    private String skuStatus;
    private Integer sortOrder;
    private String delFlag;
    private Integer warehouseCount;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getSellerSkuCode() { return sellerSkuCode; }
    public void setSellerSkuCode(String sellerSkuCode) { this.sellerSkuCode = sellerSkuCode; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
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
    public String getSkuStatus() { return skuStatus; }
    public void setSkuStatus(String skuStatus) { this.skuStatus = skuStatus; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Integer getWarehouseCount() { return warehouseCount; }
    public void setWarehouseCount(Integer warehouseCount) { this.warehouseCount = warehouseCount; }
}
