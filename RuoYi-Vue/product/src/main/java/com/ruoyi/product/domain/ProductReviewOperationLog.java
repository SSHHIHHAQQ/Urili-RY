package com.ruoyi.product.domain;

import java.util.Date;

/**
 * 商品审核操作日志，backed by product_review_operation_log.
 */
public class ProductReviewOperationLog
{
    private Long logId;
    private Long reviewId;
    private Long spuId;
    private String operationType;
    private String beforeStatus;
    private String afterStatus;
    private String operatorTerminal;
    private Long operatorId;
    private String operatorName;
    private Date operationTime;
    private String reason;
    private String remark;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getBeforeStatus() { return beforeStatus; }
    public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }
    public String getAfterStatus() { return afterStatus; }
    public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
    public String getOperatorTerminal() { return operatorTerminal; }
    public void setOperatorTerminal(String operatorTerminal) { this.operatorTerminal = operatorTerminal; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Date getOperationTime() { return operationTime; }
    public void setOperationTime(Date operationTime) { this.operationTime = operationTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
