package com.ruoyi.integration.domain.query;

/**
 * 来源商品库查询条件。
 */
public class SourceProductQuery
{
    private String connectionCode;

    private String systemKind;

    private String masterWarehouseName;

    private String masterSku;

    private String productName;

    private String identifyCodeKeyword;

    private String categoryKeyword;

    private String approveStatus;

    private Integer dangerousCargo;

    private String status;

    private String pairingStatus;

    private String keyword;

    public String getConnectionCode()
    {
        return connectionCode;
    }

    public void setConnectionCode(String connectionCode)
    {
        this.connectionCode = connectionCode;
    }

    public String getSystemKind()
    {
        return systemKind;
    }

    public void setSystemKind(String systemKind)
    {
        this.systemKind = systemKind;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }

    public String getMasterSku()
    {
        return masterSku;
    }

    public void setMasterSku(String masterSku)
    {
        this.masterSku = masterSku;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getIdentifyCodeKeyword()
    {
        return identifyCodeKeyword;
    }

    public void setIdentifyCodeKeyword(String identifyCodeKeyword)
    {
        this.identifyCodeKeyword = identifyCodeKeyword;
    }

    public String getCategoryKeyword()
    {
        return categoryKeyword;
    }

    public void setCategoryKeyword(String categoryKeyword)
    {
        this.categoryKeyword = categoryKeyword;
    }

    public String getApproveStatus()
    {
        return approveStatus;
    }

    public void setApproveStatus(String approveStatus)
    {
        this.approveStatus = approveStatus;
    }

    public Integer getDangerousCargo()
    {
        return dangerousCargo;
    }

    public void setDangerousCargo(Integer dangerousCargo)
    {
        this.dangerousCargo = dangerousCargo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPairingStatus()
    {
        return pairingStatus;
    }

    public void setPairingStatus(String pairingStatus)
    {
        this.pairingStatus = pairingStatus;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }
}
