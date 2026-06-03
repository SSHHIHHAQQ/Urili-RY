package com.ruoyi.seller.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.system.domain.PartnerProfile;

/**
 * Seller profile, backed by seller.
 */
public class Seller extends PartnerProfile
{
    private static final long serialVersionUID = 1L;

    private Long sellerId;

    private String sellerNo;

    private String sellerCode;

    private String sellerName;

    private String sellerShortName;

    private String sellerType;

    private String sellerLevel;

    public Long getSellerId()
    {
        return sellerId;
    }

    public void setSellerId(Long sellerId)
    {
        this.sellerId = sellerId;
    }

    public String getSellerNo()
    {
        return sellerNo;
    }

    public void setSellerNo(String sellerNo)
    {
        this.sellerNo = sellerNo;
    }

    @NotBlank(message = "卖家代码不能为空")
    @Size(min = 0, max = 64, message = "卖家代码长度不能超过64个字符")
    public String getSellerCode()
    {
        return sellerCode;
    }

    public void setSellerCode(String sellerCode)
    {
        this.sellerCode = sellerCode;
    }

    @NotBlank(message = "卖家全称不能为空")
    @Size(min = 0, max = 200, message = "卖家全称长度不能超过200个字符")
    public String getSellerName()
    {
        return sellerName;
    }

    public void setSellerName(String sellerName)
    {
        this.sellerName = sellerName;
    }

    @NotBlank(message = "卖家简称不能为空")
    @Size(min = 0, max = 100, message = "卖家简称长度不能超过100个字符")
    public String getSellerShortName()
    {
        return sellerShortName;
    }

    public void setSellerShortName(String sellerShortName)
    {
        this.sellerShortName = sellerShortName;
    }

    @NotBlank(message = "主体类型不能为空")
    @Size(min = 0, max = 32, message = "主体类型长度不能超过32个字符")
    public String getSellerType()
    {
        return sellerType;
    }

    public void setSellerType(String sellerType)
    {
        this.sellerType = sellerType;
    }

    @NotBlank(message = "卖家等级不能为空")
    @Size(min = 0, max = 32, message = "卖家等级长度不能超过32个字符")
    public String getSellerLevel()
    {
        return sellerLevel;
    }

    public void setSellerLevel(String sellerLevel)
    {
        this.sellerLevel = sellerLevel;
    }

    public String getCompanyName()
    {
        return StringUtils.defaultIfBlank(sellerName, sellerShortName);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("sellerId", getSellerId())
            .append("sellerNo", getSellerNo())
            .append("sellerCode", getSellerCode())
            .append("sellerName", getSellerName())
            .append("sellerShortName", getSellerShortName())
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
