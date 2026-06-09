package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品审核单，backed by product_review_request.
 */
public class ProductReviewRequest extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long reviewId;
    private String reviewNo;
    private String reviewType;
    private String reviewStatus;
    private Long spuId;
    private String systemSpuCode;
    private Long sellerId;
    private String sellerName;
    private Long categoryId;
    private String categoryName;
    private String productNameBefore;
    private String productNameAfter;
    private String mainImageUrlBefore;
    private String mainImageUrlAfter;
    private String submitTerminal;
    private Long submitSubjectId;
    private Long submitAccountId;
    private String submitUserName;
    private Date submitTime;
    private Long reviewerId;
    private String reviewerName;
    private Date reviewTime;
    private String reviewReason;
    private String riskLevel;
    private String riskSummary;
    private Integer itemCount;
    private Integer skuCount;
    private BigDecimal priceBeforeMin;
    private BigDecimal priceBeforeMax;
    private BigDecimal priceAfterMin;
    private BigDecimal priceAfterMax;
    private String currencySummary;
    private String warehouseSummary;
    private String diffSummary;
    private String activePendingKey;
    private String delFlag;
    private String keyword;
    private String reviewTypes;
    private List<ProductReviewItem> items;
    private List<ProductReviewSnapshot> snapshots;
    private List<ProductReviewOperationLog> logs;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getReviewNo() { return reviewNo; }
    public void setReviewNo(String reviewNo) { this.reviewNo = reviewNo; }
    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getProductNameBefore() { return productNameBefore; }
    public void setProductNameBefore(String productNameBefore) { this.productNameBefore = productNameBefore; }
    public String getProductNameAfter() { return productNameAfter; }
    public void setProductNameAfter(String productNameAfter) { this.productNameAfter = productNameAfter; }
    public String getMainImageUrlBefore() { return mainImageUrlBefore; }
    public void setMainImageUrlBefore(String mainImageUrlBefore) { this.mainImageUrlBefore = mainImageUrlBefore; }
    public String getMainImageUrlAfter() { return mainImageUrlAfter; }
    public void setMainImageUrlAfter(String mainImageUrlAfter) { this.mainImageUrlAfter = mainImageUrlAfter; }
    public String getSubmitTerminal() { return submitTerminal; }
    public void setSubmitTerminal(String submitTerminal) { this.submitTerminal = submitTerminal; }
    public Long getSubmitSubjectId() { return submitSubjectId; }
    public void setSubmitSubjectId(Long submitSubjectId) { this.submitSubjectId = submitSubjectId; }
    public Long getSubmitAccountId() { return submitAccountId; }
    public void setSubmitAccountId(Long submitAccountId) { this.submitAccountId = submitAccountId; }
    public String getSubmitUserName() { return submitUserName; }
    public void setSubmitUserName(String submitUserName) { this.submitUserName = submitUserName; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public Date getReviewTime() { return reviewTime; }
    public void setReviewTime(Date reviewTime) { this.reviewTime = reviewTime; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Integer getSkuCount() { return skuCount; }
    public void setSkuCount(Integer skuCount) { this.skuCount = skuCount; }
    public BigDecimal getPriceBeforeMin() { return priceBeforeMin; }
    public void setPriceBeforeMin(BigDecimal priceBeforeMin) { this.priceBeforeMin = priceBeforeMin; }
    public BigDecimal getPriceBeforeMax() { return priceBeforeMax; }
    public void setPriceBeforeMax(BigDecimal priceBeforeMax) { this.priceBeforeMax = priceBeforeMax; }
    public BigDecimal getPriceAfterMin() { return priceAfterMin; }
    public void setPriceAfterMin(BigDecimal priceAfterMin) { this.priceAfterMin = priceAfterMin; }
    public BigDecimal getPriceAfterMax() { return priceAfterMax; }
    public void setPriceAfterMax(BigDecimal priceAfterMax) { this.priceAfterMax = priceAfterMax; }
    public String getCurrencySummary() { return currencySummary; }
    public void setCurrencySummary(String currencySummary) { this.currencySummary = currencySummary; }
    public String getWarehouseSummary() { return warehouseSummary; }
    public void setWarehouseSummary(String warehouseSummary) { this.warehouseSummary = warehouseSummary; }
    public String getDiffSummary() { return diffSummary; }
    public void setDiffSummary(String diffSummary) { this.diffSummary = diffSummary; }
    public String getActivePendingKey() { return activePendingKey; }
    public void setActivePendingKey(String activePendingKey) { this.activePendingKey = activePendingKey; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getReviewTypes() { return reviewTypes; }
    public void setReviewTypes(String reviewTypes) { this.reviewTypes = reviewTypes; }
    public List<ProductReviewItem> getItems() { return items; }
    public void setItems(List<ProductReviewItem> items) { this.items = items; }
    public List<ProductReviewSnapshot> getSnapshots() { return snapshots; }
    public void setSnapshots(List<ProductReviewSnapshot> snapshots) { this.snapshots = snapshots; }
    public List<ProductReviewOperationLog> getLogs() { return logs; }
    public void setLogs(List<ProductReviewOperationLog> logs) { this.logs = logs; }
}
