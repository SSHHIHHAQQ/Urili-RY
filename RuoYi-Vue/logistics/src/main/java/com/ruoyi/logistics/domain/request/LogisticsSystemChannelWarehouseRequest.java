package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 系统渠道仓库绑定请求。
 */
public class LogisticsSystemChannelWarehouseRequest
{
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Size(max = 16, message = "状态不能超过16个字符")
    private String status;

    @Size(max = 32, message = "发货地址模式不能超过32个字符")
    private String shipperAddressMode;

    @Size(max = 100, message = "外部发货地址编码不能超过100个字符")
    private String externalShipperCode;

    @Size(max = 200, message = "发货公司不能超过200个字符")
    private String shipperCompanyName;

    @Size(max = 100, message = "发货联系人不能超过100个字符")
    private String shipperContactName;

    @Size(max = 64, message = "发货电话不能超过64个字符")
    private String shipperContactPhone;

    @Size(max = 128, message = "发货邮箱不能超过128个字符")
    private String shipperContactEmail;

    @Size(max = 32, message = "发货国家/地区不能超过32个字符")
    private String shipperCountryCode;

    @Size(max = 100, message = "发货州/省不能超过100个字符")
    private String shipperStateProvince;

    @Size(max = 100, message = "发货城市不能超过100个字符")
    private String shipperCity;

    @Size(max = 32, message = "发货邮编不能超过32个字符")
    private String shipperPostalCode;

    @Size(max = 255, message = "发货地址1不能超过255个字符")
    private String shipperAddressLine1;

    @Size(max = 255, message = "发货地址2不能超过255个字符")
    private String shipperAddressLine2;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getShipperAddressMode()
    {
        return shipperAddressMode;
    }

    public void setShipperAddressMode(String shipperAddressMode)
    {
        this.shipperAddressMode = shipperAddressMode;
    }

    public String getExternalShipperCode()
    {
        return externalShipperCode;
    }

    public void setExternalShipperCode(String externalShipperCode)
    {
        this.externalShipperCode = externalShipperCode;
    }

    public String getShipperCompanyName()
    {
        return shipperCompanyName;
    }

    public void setShipperCompanyName(String shipperCompanyName)
    {
        this.shipperCompanyName = shipperCompanyName;
    }

    public String getShipperContactName()
    {
        return shipperContactName;
    }

    public void setShipperContactName(String shipperContactName)
    {
        this.shipperContactName = shipperContactName;
    }

    public String getShipperContactPhone()
    {
        return shipperContactPhone;
    }

    public void setShipperContactPhone(String shipperContactPhone)
    {
        this.shipperContactPhone = shipperContactPhone;
    }

    public String getShipperContactEmail()
    {
        return shipperContactEmail;
    }

    public void setShipperContactEmail(String shipperContactEmail)
    {
        this.shipperContactEmail = shipperContactEmail;
    }

    public String getShipperCountryCode()
    {
        return shipperCountryCode;
    }

    public void setShipperCountryCode(String shipperCountryCode)
    {
        this.shipperCountryCode = shipperCountryCode;
    }

    public String getShipperStateProvince()
    {
        return shipperStateProvince;
    }

    public void setShipperStateProvince(String shipperStateProvince)
    {
        this.shipperStateProvince = shipperStateProvince;
    }

    public String getShipperCity()
    {
        return shipperCity;
    }

    public void setShipperCity(String shipperCity)
    {
        this.shipperCity = shipperCity;
    }

    public String getShipperPostalCode()
    {
        return shipperPostalCode;
    }

    public void setShipperPostalCode(String shipperPostalCode)
    {
        this.shipperPostalCode = shipperPostalCode;
    }

    public String getShipperAddressLine1()
    {
        return shipperAddressLine1;
    }

    public void setShipperAddressLine1(String shipperAddressLine1)
    {
        this.shipperAddressLine1 = shipperAddressLine1;
    }

    public String getShipperAddressLine2()
    {
        return shipperAddressLine2;
    }

    public void setShipperAddressLine2(String shipperAddressLine2)
    {
        this.shipperAddressLine2 = shipperAddressLine2;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
