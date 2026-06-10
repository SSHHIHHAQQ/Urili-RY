package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建面单请求。
 */
public class LogisticsCreateLabelRequest extends LogisticsQuoteRequest
{
    @NotBlank(message = "业务单号不能为空")
    @Size(max = 100, message = "业务单号不能超过100个字符")
    private String businessOrderNo;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Size(max = 100, message = "PO号不能超过100个字符")
    private String poCode;

    @Size(max = 100, message = "VAT号不能超过100个字符")
    private String vatCode;

    @Size(max = 100, message = "仓库代码不能超过100个字符")
    private String warehouseCode;

    @Size(max = 32, message = "预计交仓时间不能超过32个字符")
    private String deliveryTime;

    public String getBusinessOrderNo()
    {
        return businessOrderNo;
    }

    public void setBusinessOrderNo(String businessOrderNo)
    {
        this.businessOrderNo = businessOrderNo;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getPoCode()
    {
        return poCode;
    }

    public void setPoCode(String poCode)
    {
        this.poCode = poCode;
    }

    public String getVatCode()
    {
        return vatCode;
    }

    public void setVatCode(String vatCode)
    {
        this.vatCode = vatCode;
    }

    public String getWarehouseCode()
    {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode)
    {
        this.warehouseCode = warehouseCode;
    }

    public String getDeliveryTime()
    {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime)
    {
        this.deliveryTime = deliveryTime;
    }
}
