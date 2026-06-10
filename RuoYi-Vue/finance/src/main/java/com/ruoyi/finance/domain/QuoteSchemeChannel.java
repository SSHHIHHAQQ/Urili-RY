package com.ruoyi.finance.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Quote scheme customer channel detail.
 */
public class QuoteSchemeChannel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long schemeChannelId;

    private Long schemeId;

    private String customerChannelCode;

    private String customerChannelNameSnapshot;

    private String operationFeeCode;

    private String operationFeeNameSnapshot;

    private String freightFeeCode;

    private String freightFeeNameSnapshot;

    private String status;

    private Integer displayOrder;

    public Long getSchemeChannelId()
    {
        return schemeChannelId;
    }

    public void setSchemeChannelId(Long schemeChannelId)
    {
        this.schemeChannelId = schemeChannelId;
    }

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public String getCustomerChannelCode()
    {
        return customerChannelCode;
    }

    public void setCustomerChannelCode(String customerChannelCode)
    {
        this.customerChannelCode = customerChannelCode;
    }

    public String getCustomerChannelNameSnapshot()
    {
        return customerChannelNameSnapshot;
    }

    public void setCustomerChannelNameSnapshot(String customerChannelNameSnapshot)
    {
        this.customerChannelNameSnapshot = customerChannelNameSnapshot;
    }

    public String getOperationFeeCode()
    {
        return operationFeeCode;
    }

    public void setOperationFeeCode(String operationFeeCode)
    {
        this.operationFeeCode = operationFeeCode;
    }

    public String getOperationFeeNameSnapshot()
    {
        return operationFeeNameSnapshot;
    }

    public void setOperationFeeNameSnapshot(String operationFeeNameSnapshot)
    {
        this.operationFeeNameSnapshot = operationFeeNameSnapshot;
    }

    public String getFreightFeeCode()
    {
        return freightFeeCode;
    }

    public void setFreightFeeCode(String freightFeeCode)
    {
        this.freightFeeCode = freightFeeCode;
    }

    public String getFreightFeeNameSnapshot()
    {
        return freightFeeNameSnapshot;
    }

    public void setFreightFeeNameSnapshot(String freightFeeNameSnapshot)
    {
        this.freightFeeNameSnapshot = freightFeeNameSnapshot;
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
