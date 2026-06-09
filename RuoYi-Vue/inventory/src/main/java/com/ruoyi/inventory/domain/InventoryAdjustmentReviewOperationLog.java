package com.ruoyi.inventory.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存调整审核操作日志。
 */
public class InventoryAdjustmentReviewOperationLog extends BaseEntity
{
    private Long logId;
    private Long reviewId;
    private String reviewNo;
    private String operationType;
    private String beforeStatus;
    private String afterStatus;
    private String operationReason;
    private Long operatorId;
    private String operatorName;
    private Date operateTime;
    private String changeSummary;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getReviewNo() { return reviewNo; }
    public void setReviewNo(String reviewNo) { this.reviewNo = reviewNo; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getBeforeStatus() { return beforeStatus; }
    public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }
    public String getAfterStatus() { return afterStatus; }
    public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
    public String getOperationReason() { return operationReason; }
    public void setOperationReason(String operationReason) { this.operationReason = operationReason; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Date getOperateTime() { return operateTime; }
    public void setOperateTime(Date operateTime) { this.operateTime = operateTime; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
}
