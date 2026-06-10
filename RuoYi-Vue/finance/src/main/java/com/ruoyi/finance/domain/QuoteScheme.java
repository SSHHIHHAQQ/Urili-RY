package com.ruoyi.finance.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Quote scheme main record.
 */
public class QuoteScheme extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long schemeId;

    private String schemeCode;

    private String schemeName;

    private String schemeType;

    private String feeSourceMode;

    private String currencyCode;

    private String scopeType;

    private String warehouseScopeMode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    private Integer effectivePriority;

    private String status;

    private String keyword;

    private Integer scopeCount;

    private Integer warehouseCount;

    private Integer channelCount;

    private String scopeSummary;

    private String warehouseSummary;

    private String channelSummary;

    private List<String> buyerLevelCodes;

    private List<Long> buyerIds;

    private List<String> warehouseCodes;

    private List<QuoteSchemeScope> scopes;

    private List<QuoteSchemeWarehouse> warehouses;

    private List<QuoteSchemeChannel> channels;

    private List<QuoteSchemeValueFeeRule> valueFeeRules;

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public String getSchemeCode()
    {
        return schemeCode;
    }

    public void setSchemeCode(String schemeCode)
    {
        this.schemeCode = schemeCode;
    }

    public String getSchemeName()
    {
        return schemeName;
    }

    public void setSchemeName(String schemeName)
    {
        this.schemeName = schemeName;
    }

    public String getSchemeType()
    {
        return schemeType;
    }

    public void setSchemeType(String schemeType)
    {
        this.schemeType = schemeType;
    }

    public String getFeeSourceMode()
    {
        return feeSourceMode;
    }

    public void setFeeSourceMode(String feeSourceMode)
    {
        this.feeSourceMode = feeSourceMode;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public String getScopeType()
    {
        return scopeType;
    }

    public void setScopeType(String scopeType)
    {
        this.scopeType = scopeType;
    }

    public String getWarehouseScopeMode()
    {
        return warehouseScopeMode;
    }

    public void setWarehouseScopeMode(String warehouseScopeMode)
    {
        this.warehouseScopeMode = warehouseScopeMode;
    }

    public Date getEffectiveTime()
    {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime)
    {
        this.effectiveTime = effectiveTime;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Integer getEffectivePriority()
    {
        return effectivePriority;
    }

    public void setEffectivePriority(Integer effectivePriority)
    {
        this.effectivePriority = effectivePriority;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public Integer getScopeCount()
    {
        return scopeCount;
    }

    public void setScopeCount(Integer scopeCount)
    {
        this.scopeCount = scopeCount;
    }

    public Integer getWarehouseCount()
    {
        return warehouseCount;
    }

    public void setWarehouseCount(Integer warehouseCount)
    {
        this.warehouseCount = warehouseCount;
    }

    public Integer getChannelCount()
    {
        return channelCount;
    }

    public void setChannelCount(Integer channelCount)
    {
        this.channelCount = channelCount;
    }

    public String getScopeSummary()
    {
        return scopeSummary;
    }

    public void setScopeSummary(String scopeSummary)
    {
        this.scopeSummary = scopeSummary;
    }

    public String getWarehouseSummary()
    {
        return warehouseSummary;
    }

    public void setWarehouseSummary(String warehouseSummary)
    {
        this.warehouseSummary = warehouseSummary;
    }

    public String getChannelSummary()
    {
        return channelSummary;
    }

    public void setChannelSummary(String channelSummary)
    {
        this.channelSummary = channelSummary;
    }

    public List<String> getBuyerLevelCodes()
    {
        return buyerLevelCodes;
    }

    public void setBuyerLevelCodes(List<String> buyerLevelCodes)
    {
        this.buyerLevelCodes = buyerLevelCodes;
    }

    public List<Long> getBuyerIds()
    {
        return buyerIds;
    }

    public void setBuyerIds(List<Long> buyerIds)
    {
        this.buyerIds = buyerIds;
    }

    public List<String> getWarehouseCodes()
    {
        return warehouseCodes;
    }

    public void setWarehouseCodes(List<String> warehouseCodes)
    {
        this.warehouseCodes = warehouseCodes;
    }

    public List<QuoteSchemeScope> getScopes()
    {
        return scopes;
    }

    public void setScopes(List<QuoteSchemeScope> scopes)
    {
        this.scopes = scopes;
    }

    public List<QuoteSchemeWarehouse> getWarehouses()
    {
        return warehouses;
    }

    public void setWarehouses(List<QuoteSchemeWarehouse> warehouses)
    {
        this.warehouses = warehouses;
    }

    public List<QuoteSchemeChannel> getChannels()
    {
        return channels;
    }

    public void setChannels(List<QuoteSchemeChannel> channels)
    {
        this.channels = channels;
    }

    public List<QuoteSchemeValueFeeRule> getValueFeeRules()
    {
        return valueFeeRules;
    }

    public void setValueFeeRules(List<QuoteSchemeValueFeeRule> valueFeeRules)
    {
        this.valueFeeRules = valueFeeRules;
    }
}
