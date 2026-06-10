package com.ruoyi.logistics.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统渠道绑定仓库及该仓库的发货地址覆写。
 */
public class LogisticsSystemChannelWarehouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long bindingId;
    private String systemChannelCode;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseKind;
    private String status;
    private String shipperAddressMode;
    private String externalShipperCode;
    private String shipperCompanyName;
    private String shipperContactName;
    private String shipperContactPhone;
    private String shipperContactEmail;
    private String shipperCountryCode;
    private String shipperStateProvince;
    private String shipperCity;
    private String shipperPostalCode;
    private String shipperAddressLine1;
    private String shipperAddressLine2;

    public Long getBindingId()
    {
        return bindingId;
    }

    public void setBindingId(Long bindingId)
    {
        this.bindingId = bindingId;
    }

    public String getSystemChannelCode()
    {
        return systemChannelCode;
    }

    public void setSystemChannelCode(String systemChannelCode)
    {
        this.systemChannelCode = systemChannelCode;
    }

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode()
    {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode)
    {
        this.warehouseCode = warehouseCode;
    }

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
}
