package com.ruoyi.inventory.domain;

/**
 * Product-owned SKU/SPU identity snapshot consumed by inventory read-model refresh.
 */
public class InventoryProductSkuSnapshot
{
    private Long spuId;
    private Long skuId;
    private Long sellerId;
    private String sellerNo;
    private String sellerName;
    private String systemSpuCode;
    private String sellerSpuCode;
    private String systemSkuCode;
    private String sellerSkuCode;
    private String productName;
    private String productNameEn;
    private String mainImageUrl;
    private String skuImageUrl;
    private String color;
    private String size;
    private String model;

    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerNo() { return sellerNo; }
    public void setSellerNo(String sellerNo) { this.sellerNo = sellerNo; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }

    public String getSellerSpuCode() { return sellerSpuCode; }
    public void setSellerSpuCode(String sellerSpuCode) { this.sellerSpuCode = sellerSpuCode; }

    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }

    public String getSellerSkuCode() { return sellerSkuCode; }
    public void setSellerSkuCode(String sellerSkuCode) { this.sellerSkuCode = sellerSkuCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductNameEn() { return productNameEn; }
    public void setProductNameEn(String productNameEn) { this.productNameEn = productNameEn; }

    public String getMainImageUrl() { return mainImageUrl; }
    public void setMainImageUrl(String mainImageUrl) { this.mainImageUrl = mainImageUrl; }

    public String getSkuImageUrl() { return skuImageUrl; }
    public void setSkuImageUrl(String skuImageUrl) { this.skuImageUrl = skuImageUrl; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
