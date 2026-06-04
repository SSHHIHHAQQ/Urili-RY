package com.ruoyi.product.domain.importdata;

/**
 * 商品配置导入单行结果。
 */
public class ProductImportMessage
{
    private Integer rowNum;
    private String action;
    private String status;
    private String message;

    public ProductImportMessage()
    {
    }

    public ProductImportMessage(Integer rowNum, String action, String status, String message)
    {
        this.rowNum = rowNum;
        this.action = action;
        this.status = status;
        this.message = message;
    }

    public Integer getRowNum()
    {
        return rowNum;
    }

    public void setRowNum(Integer rowNum)
    {
        this.rowNum = rowNum;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
