package com.ruoyi.inventory.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存调整审核策略绑定。
 */
public class InventoryAdjustmentReviewPolicyBinding extends BaseEntity
{
    private Long bindingId;
    private Long policyId;
    private String policyName;
    private String bindingType;
    private Long bindingIdValue;
    private Integer priority;
    private String status;

    public Long getBindingId() { return bindingId; }
    public void setBindingId(Long bindingId) { this.bindingId = bindingId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getBindingType() { return bindingType; }
    public void setBindingType(String bindingType) { this.bindingType = bindingType; }
    public Long getBindingIdValue() { return bindingIdValue; }
    public void setBindingIdValue(Long bindingIdValue) { this.bindingIdValue = bindingIdValue; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
