package com.ruoyi.logistics.domain.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 物流包裹尺寸重量请求。
 */
public class LogisticsBoxRequest
{
    @NotNull(message = "箱长不能为空")
    @DecimalMin(value = "0.01", message = "箱长必须大于0")
    private BigDecimal length;

    @NotNull(message = "箱宽不能为空")
    @DecimalMin(value = "0.01", message = "箱宽必须大于0")
    private BigDecimal width;

    @NotNull(message = "箱高不能为空")
    @DecimalMin(value = "0.01", message = "箱高必须大于0")
    private BigDecimal height;

    @NotNull(message = "实重不能为空")
    @DecimalMin(value = "0.01", message = "实重必须大于0")
    private BigDecimal actualWeight;

    @Size(max = 200, message = "箱备注不能超过200个字符")
    private String remark;

    public BigDecimal getLength()
    {
        return length;
    }

    public void setLength(BigDecimal length)
    {
        this.length = length;
    }

    public BigDecimal getWidth()
    {
        return width;
    }

    public void setWidth(BigDecimal width)
    {
        this.width = width;
    }

    public BigDecimal getHeight()
    {
        return height;
    }

    public void setHeight(BigDecimal height)
    {
        this.height = height;
    }

    public BigDecimal getActualWeight()
    {
        return actualWeight;
    }

    public void setActualWeight(BigDecimal actualWeight)
    {
        this.actualWeight = actualWeight;
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
