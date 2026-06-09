package com.ruoyi.product.domain;

import java.util.Date;

/**
 * 商品审核对象明细，backed by product_review_item.
 */
public class ProductReviewItem
{
    private Long itemId;
    private Long reviewId;
    private String itemType;
    private String changeType;
    private Long spuId;
    private Long skuId;
    private String systemSkuCode;
    private String sellerSkuCode;
    private String itemStatus;
    private String beforeHash;
    private String afterHash;
    private String diffSummary;
    private String riskSummary;
    private Integer sortOrder;
    private Date createTime;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public String getSellerSkuCode() { return sellerSkuCode; }
    public void setSellerSkuCode(String sellerSkuCode) { this.sellerSkuCode = sellerSkuCode; }
    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }
    public String getBeforeHash() { return beforeHash; }
    public void setBeforeHash(String beforeHash) { this.beforeHash = beforeHash; }
    public String getAfterHash() { return afterHash; }
    public void setAfterHash(String afterHash) { this.afterHash = afterHash; }
    public String getDiffSummary() { return diffSummary; }
    public void setDiffSummary(String diffSummary) { this.diffSummary = diffSummary; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
