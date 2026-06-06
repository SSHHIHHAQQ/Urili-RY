package com.ruoyi.buyer.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.system.domain.PartnerProfile;

/**
 * Buyer profile, backed by buyer.
 */
public class Buyer extends PartnerProfile
{
    private static final long serialVersionUID = 1L;

    private Long buyerId;

    private String buyerNo;

    private String buyerCode;

    private String buyerName;

    private String buyerShortName;

    private String buyerType;

    private String buyerLevel;

    private String companyName;

    public Long getBuyerId()
    {
        return buyerId;
    }

    public void setBuyerId(Long buyerId)
    {
        this.buyerId = buyerId;
    }

    public String getBuyerNo()
    {
        return buyerNo;
    }

    public void setBuyerNo(String buyerNo)
    {
        this.buyerNo = buyerNo;
    }

    @NotBlank(message = "买家代码不能为空")
    @Size(min = 0, max = 64, message = "买家代码长度不能超过64个字符")
    public String getBuyerCode()
    {
        return buyerCode;
    }

    public void setBuyerCode(String buyerCode)
    {
        this.buyerCode = buyerCode;
    }

    @NotBlank(message = "买家全称不能为空")
    @Size(min = 0, max = 200, message = "买家全称长度不能超过200个字符")
    public String getBuyerName()
    {
        return buyerName;
    }

    public void setBuyerName(String buyerName)
    {
        this.buyerName = buyerName;
    }

    @NotBlank(message = "买家简称不能为空")
    @Size(min = 0, max = 100, message = "买家简称长度不能超过100个字符")
    public String getBuyerShortName()
    {
        return buyerShortName;
    }

    public void setBuyerShortName(String buyerShortName)
    {
        this.buyerShortName = buyerShortName;
    }

    @NotBlank(message = "主体类型不能为空")
    @Size(min = 0, max = 32, message = "主体类型长度不能超过32个字符")
    public String getBuyerType()
    {
        return buyerType;
    }

    public void setBuyerType(String buyerType)
    {
        this.buyerType = buyerType;
    }

    @NotBlank(message = "买家等级不能为空")
    @Size(min = 0, max = 32, message = "买家等级长度不能超过32个字符")
    public String getBuyerLevel()
    {
        return buyerLevel;
    }

    public void setBuyerLevel(String buyerLevel)
    {
        this.buyerLevel = buyerLevel;
    }

    public String getCompanyName()
    {
        return StringUtils.defaultIfBlank(companyName, StringUtils.defaultIfBlank(buyerName, buyerShortName));
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("buyerId", getBuyerId())
            .append("buyerNo", getBuyerNo())
            .append("buyerCode", getBuyerCode())
            .append("buyerName", getBuyerName())
            .append("buyerShortName", getBuyerShortName())
            .append("username", getUsername())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
