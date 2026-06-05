package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品业务操作日志，backed by product_distribution_operation_log.
 */
public class ProductDistributionOperationLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long logId;
    private String batchNo;
    private String operationType;
    private String ownerType;
    private Long spuId;
    private Long skuId;
    private String systemSpuCode;
    private String systemSkuCode;
    private Long sellerId;
    private String sellerName;
    private String beforeSalesStatus;
    private String afterSalesStatus;
    private String beforeControlStatus;
    private String afterControlStatus;
    private BigDecimal beforeSalePrice;
    private BigDecimal afterSalePrice;
    private String currencyCode;
    private String reason;
    private String changeSummary;
    private String diffJson;
    private String operatorName;
    private Date operationTime;
    private String operationSource;
    private String keyword;
    private String operationTypes;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSystemSpuCode() { return systemSpuCode; }
    public void setSystemSpuCode(String systemSpuCode) { this.systemSpuCode = systemSpuCode; }
    public String getSystemSkuCode() { return systemSkuCode; }
    public void setSystemSkuCode(String systemSkuCode) { this.systemSkuCode = systemSkuCode; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getBeforeSalesStatus() { return beforeSalesStatus; }
    public void setBeforeSalesStatus(String beforeSalesStatus) { this.beforeSalesStatus = beforeSalesStatus; }
    public String getAfterSalesStatus() { return afterSalesStatus; }
    public void setAfterSalesStatus(String afterSalesStatus) { this.afterSalesStatus = afterSalesStatus; }
    public String getBeforeControlStatus() { return beforeControlStatus; }
    public void setBeforeControlStatus(String beforeControlStatus) { this.beforeControlStatus = beforeControlStatus; }
    public String getAfterControlStatus() { return afterControlStatus; }
    public void setAfterControlStatus(String afterControlStatus) { this.afterControlStatus = afterControlStatus; }
    public BigDecimal getBeforeSalePrice() { return beforeSalePrice; }
    public void setBeforeSalePrice(BigDecimal beforeSalePrice) { this.beforeSalePrice = beforeSalePrice; }
    public BigDecimal getAfterSalePrice() { return afterSalePrice; }
    public void setAfterSalePrice(BigDecimal afterSalePrice) { this.afterSalePrice = afterSalePrice; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    public String getDiffJson() { return diffJson; }
    public void setDiffJson(String diffJson) { this.diffJson = diffJson; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Date getOperationTime() { return operationTime; }
    public void setOperationTime(Date operationTime) { this.operationTime = operationTime; }
    public String getOperationSource() { return operationSource; }
    public void setOperationSource(String operationSource) { this.operationSource = operationSource; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getOperationTypes() { return operationTypes; }
    public void setOperationTypes(String operationTypes) { this.operationTypes = operationTypes; }
}
