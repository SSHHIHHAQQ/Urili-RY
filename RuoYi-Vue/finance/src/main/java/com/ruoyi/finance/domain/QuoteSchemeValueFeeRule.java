package com.ruoyi.finance.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Quote scheme value-added fee rule.
 */
public class QuoteSchemeValueFeeRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long valueFeeRuleId;

    private Long schemeId;

    private String logisticsChannelCode;

    private String logisticsChannelNameSnapshot;

    private String triggerCode;

    private String calculationMethod;

    private String adjustmentDirection;

    private BigDecimal adjustmentValue;

    private String status;

    private Integer displayOrder;

    public Long getValueFeeRuleId()
    {
        return valueFeeRuleId;
    }

    public void setValueFeeRuleId(Long valueFeeRuleId)
    {
        this.valueFeeRuleId = valueFeeRuleId;
    }

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public String getLogisticsChannelCode()
    {
        return logisticsChannelCode;
    }

    public void setLogisticsChannelCode(String logisticsChannelCode)
    {
        this.logisticsChannelCode = logisticsChannelCode;
    }

    public String getLogisticsChannelNameSnapshot()
    {
        return logisticsChannelNameSnapshot;
    }

    public void setLogisticsChannelNameSnapshot(String logisticsChannelNameSnapshot)
    {
        this.logisticsChannelNameSnapshot = logisticsChannelNameSnapshot;
    }

    public String getTriggerCode()
    {
        return triggerCode;
    }

    public void setTriggerCode(String triggerCode)
    {
        this.triggerCode = triggerCode;
    }

    public String getCalculationMethod()
    {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod)
    {
        this.calculationMethod = calculationMethod;
    }

    public String getAdjustmentDirection()
    {
        return adjustmentDirection;
    }

    public void setAdjustmentDirection(String adjustmentDirection)
    {
        this.adjustmentDirection = adjustmentDirection;
    }

    public BigDecimal getAdjustmentValue()
    {
        return adjustmentValue;
    }

    public void setAdjustmentValue(BigDecimal adjustmentValue)
    {
        this.adjustmentValue = adjustmentValue;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getDisplayOrder()
    {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder)
    {
        this.displayOrder = displayOrder;
    }
}
