package com.ruoyi.product.domain;

/**
 * Buyer-facing product center query.
 */
public class ProductCenterQuery
{
    private String keyword;
    private String productName;
    private String productNameEn;
    private String systemSpuCode;
    private String systemSkuCode;
    private Long categoryId;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductNameEn() { return productNameEn; }
    public void setProductNameEn(String productNameEn) { this.productNameEn = productNameEn; }
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
