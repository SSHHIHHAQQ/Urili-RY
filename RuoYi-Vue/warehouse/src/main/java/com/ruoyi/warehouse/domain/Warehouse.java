package com.ruoyi.warehouse.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 仓库主数据。
 */
public class Warehouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseKind;
    private String countryCode;
    private String stateProvince;
    private String city;
    private String postalCode;
    private String addressLine1;
    private String addressLine2;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String companyName;
    private String settlementCurrency;
    private String status;

    private Long sellerId;
    private String sellerNo;
    private String sellerCode;
    private String sellerName;
    private String sellerShortName;
    private String sellerKeyword;

    private Long warehousePairingId;
    private String connectionCode;
    private String masterWarehouseName;
    private String upstreamWarehouseCode;
    private String upstreamWarehouseName;
    private String pairingStatus;
    private String pairingRole;

    private Long quoteWarehousePairingId;
    private String quoteConnectionCode;
    private String quoteMasterWarehouseName;
    private String quoteUpstreamWarehouseCode;
    private String quoteUpstreamWarehouseName;
    private String quotePairingStatus;

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 64, message = "仓库编码长度不能超过64个字符")
    public String getWarehouseCode()
    {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode)
    {
        this.warehouseCode = warehouseCode;
    }

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 200, message = "仓库名称长度不能超过200个字符")
    public String getWarehouseName()
    {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName)
    {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseKind()
    {
        return warehouseKind;
    }

    public void setWarehouseKind(String warehouseKind)
    {
        this.warehouseKind = warehouseKind;
    }

    @NotBlank(message = "国家/地区不能为空")
    @Size(max = 32, message = "国家/地区代码长度不能超过32个字符")
    public String getCountryCode()
    {
        return countryCode;
    }

    public void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }

    @NotBlank(message = "州/省不能为空")
    @Size(max = 100, message = "州/省长度不能超过100个字符")
    public String getStateProvince()
    {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince)
    {
        this.stateProvince = stateProvince;
    }

    @NotBlank(message = "城市不能为空")
    @Size(max = 100, message = "城市长度不能超过100个字符")
    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    @NotBlank(message = "邮编不能为空")
    @Size(max = 32, message = "邮编长度不能超过32个字符")
    public String getPostalCode()
    {
        return postalCode;
    }

    public void setPostalCode(String postalCode)
    {
        this.postalCode = postalCode;
    }

    @NotBlank(message = "地址1不能为空")
    @Size(max = 255, message = "地址1长度不能超过255个字符")
    public String getAddressLine1()
    {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1)
    {
        this.addressLine1 = addressLine1;
    }

    @Size(max = 255, message = "地址2长度不能超过255个字符")
    public String getAddressLine2()
    {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2)
    {
        this.addressLine2 = addressLine2;
    }

    @NotBlank(message = "联系人不能为空")
    @Size(max = 100, message = "联系人长度不能超过100个字符")
    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    @Size(max = 64, message = "联系电话长度不能超过64个字符")
    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    @NotBlank(message = "联系邮箱不能为空")
    @Email(message = "联系邮箱格式不正确")
    @Size(max = 128, message = "联系邮箱长度不能超过128个字符")
    public String getContactEmail()
    {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail)
    {
        this.contactEmail = contactEmail;
    }

    @Size(max = 200, message = "公司名称长度不能超过200个字符")
    public String getCompanyName()
    {
        return companyName;
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    @NotBlank(message = "结算币种不能为空")
    @Size(max = 16, message = "结算币种长度不能超过16个字符")
    public String getSettlementCurrency()
    {
        return settlementCurrency;
    }

    public void setSettlementCurrency(String settlementCurrency)
    {
        this.settlementCurrency = settlementCurrency;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

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

    public String getSellerCode()
    {
        return sellerCode;
    }

    public void setSellerCode(String sellerCode)
    {
        this.sellerCode = sellerCode;
    }

    public String getSellerName()
    {
        return sellerName;
    }

    public void setSellerName(String sellerName)
    {
        this.sellerName = sellerName;
    }

    public String getSellerShortName()
    {
        return sellerShortName;
    }

    public void setSellerShortName(String sellerShortName)
    {
        this.sellerShortName = sellerShortName;
    }

    public String getSellerKeyword()
    {
        return sellerKeyword;
    }

    public void setSellerKeyword(String sellerKeyword)
    {
        this.sellerKeyword = sellerKeyword;
    }

    public Long getWarehousePairingId()
    {
        return warehousePairingId;
    }

    public void setWarehousePairingId(Long warehousePairingId)
    {
        this.warehousePairingId = warehousePairingId;
    }

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }

    public String getUpstreamWarehouseCode()
    {
        return upstreamWarehouseCode;
    }

    public void setUpstreamWarehouseCode(String upstreamWarehouseCode)
    {
        this.upstreamWarehouseCode = upstreamWarehouseCode;
    }

    public String getUpstreamWarehouseName()
    {
        return upstreamWarehouseName;
    }

    public void setUpstreamWarehouseName(String upstreamWarehouseName)
    {
        this.upstreamWarehouseName = upstreamWarehouseName;
    }

    public String getPairingStatus()
    {
        return pairingStatus;
    }

    public void setPairingStatus(String pairingStatus)
    {
        this.pairingStatus = pairingStatus;
    }

    public String getPairingRole()
    {
        return pairingRole;
    }

    public void setPairingRole(String pairingRole)
    {
        this.pairingRole = pairingRole;
    }

    public Long getQuoteWarehousePairingId()
    {
        return quoteWarehousePairingId;
    }

    public void setQuoteWarehousePairingId(Long quoteWarehousePairingId)
    {
        this.quoteWarehousePairingId = quoteWarehousePairingId;
    }

    public String getQuoteConnectionCode()
    {
        return quoteConnectionCode;
    }

    public void setQuoteConnectionCode(String quoteConnectionCode)
    {
        this.quoteConnectionCode = quoteConnectionCode;
    }

    public String getQuoteMasterWarehouseName()
    {
        return quoteMasterWarehouseName;
    }

    public void setQuoteMasterWarehouseName(String quoteMasterWarehouseName)
    {
        this.quoteMasterWarehouseName = quoteMasterWarehouseName;
    }

    public String getQuoteUpstreamWarehouseCode()
    {
        return quoteUpstreamWarehouseCode;
    }

    public void setQuoteUpstreamWarehouseCode(String quoteUpstreamWarehouseCode)
    {
        this.quoteUpstreamWarehouseCode = quoteUpstreamWarehouseCode;
    }

    public String getQuoteUpstreamWarehouseName()
    {
        return quoteUpstreamWarehouseName;
    }

    public void setQuoteUpstreamWarehouseName(String quoteUpstreamWarehouseName)
    {
        this.quoteUpstreamWarehouseName = quoteUpstreamWarehouseName;
    }

    public String getQuotePairingStatus()
    {
        return quotePairingStatus;
    }

    public void setQuotePairingStatus(String quotePairingStatus)
    {
        this.quotePairingStatus = quotePairingStatus;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("warehouseId", getWarehouseId())
            .append("warehouseCode", getWarehouseCode())
            .append("warehouseName", getWarehouseName())
            .append("warehouseKind", getWarehouseKind())
            .append("countryCode", getCountryCode())
            .append("stateProvince", getStateProvince())
            .append("city", getCity())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
