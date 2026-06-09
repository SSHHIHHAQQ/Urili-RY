package com.ruoyi.product.domain;

import java.util.Date;

/**
 * 商品审核快照，backed by product_review_snapshot.
 */
public class ProductReviewSnapshot
{
    private Long snapshotId;
    private Long reviewId;
    private Long itemId;
    private String snapshotRole;
    private String payloadType;
    private String payloadJson;
    private String payloadHash;
    private Date createTime;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getSnapshotRole() { return snapshotRole; }
    public void setSnapshotRole(String snapshotRole) { this.snapshotRole = snapshotRole; }
    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
