package com.ruoyi.warehouse.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 美国州字典。
 */
public class UsState extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long stateId;
    private String stateCode;
    private String stateName;
    private String status;

    public Long getStateId()
    {
        return stateId;
    }

    public void setStateId(Long stateId)
    {
        this.stateId = stateId;
    }

    public String getStateCode()
    {
        return stateCode;
    }

    public void setStateCode(String stateCode)
    {
        this.stateCode = stateCode;
    }

    public String getStateName()
    {
        return stateName;
    }

    public void setStateName(String stateName)
    {
        this.stateName = stateName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
