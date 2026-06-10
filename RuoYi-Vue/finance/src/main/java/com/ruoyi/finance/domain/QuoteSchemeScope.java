package com.ruoyi.finance.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Quote scheme buyer scope detail.
 */
public class QuoteSchemeScope extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long scopeId;

    private Long schemeId;

    private String scopeType;

    private String scopeKey;

    private String buyerLevelCode;

    private String buyerLevelNameSnapshot;

    private Long buyerId;

    private String buyerCodeSnapshot;

    private String buyerNameSnapshot;

    private String buyerShortNameSnapshot;

    public Long getScopeId()
    {
        return scopeId;
    }

    public void setScopeId(Long scopeId)
    {
        this.scopeId = scopeId;
    }

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public String getScopeType()
    {
        return scopeType;
    }

    public void setScopeType(String scopeType)
    {
        this.scopeType = scopeType;
    }

    public String getScopeKey()
    {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey)
    {
        this.scopeKey = scopeKey;
    }

    public String getBuyerLevelCode()
    {
        return buyerLevelCode;
    }

    public void setBuyerLevelCode(String buyerLevelCode)
    {
        this.buyerLevelCode = buyerLevelCode;
    }

    public String getBuyerLevelNameSnapshot()
    {
        return buyerLevelNameSnapshot;
    }

    public void setBuyerLevelNameSnapshot(String buyerLevelNameSnapshot)
    {
        this.buyerLevelNameSnapshot = buyerLevelNameSnapshot;
    }

    public Long getBuyerId()
    {
        return buyerId;
    }

    public void setBuyerId(Long buyerId)
    {
        this.buyerId = buyerId;
    }

    public String getBuyerCodeSnapshot()
    {
        return buyerCodeSnapshot;
    }

    public void setBuyerCodeSnapshot(String buyerCodeSnapshot)
    {
        this.buyerCodeSnapshot = buyerCodeSnapshot;
    }

    public String getBuyerNameSnapshot()
    {
        return buyerNameSnapshot;
    }

    public void setBuyerNameSnapshot(String buyerNameSnapshot)
    {
        this.buyerNameSnapshot = buyerNameSnapshot;
    }

    public String getBuyerShortNameSnapshot()
    {
        return buyerShortNameSnapshot;
    }

    public void setBuyerShortNameSnapshot(String buyerShortNameSnapshot)
    {
        this.buyerShortNameSnapshot = buyerShortNameSnapshot;
    }
}
