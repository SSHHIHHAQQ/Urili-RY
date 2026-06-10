package com.ruoyi.logistics.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 面单包裹/文件。
 */
public class LogisticsLabelPackage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long labelPackageId;

    private Long labelOrderId;

    private String providerPackageNo;

    private String trackingNumber;

    private String labelUrl;

    private String fileType;

    private String status;

    private String sourcePayloadJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getLabelPackageId()
    {
        return labelPackageId;
    }

    public void setLabelPackageId(Long labelPackageId)
    {
        this.labelPackageId = labelPackageId;
    }

    public Long getLabelOrderId()
    {
        return labelOrderId;
    }

    public void setLabelOrderId(Long labelOrderId)
    {
        this.labelOrderId = labelOrderId;
    }

    public String getProviderPackageNo()
    {
        return providerPackageNo;
    }

    public void setProviderPackageNo(String providerPackageNo)
    {
        this.providerPackageNo = providerPackageNo;
    }

    public String getTrackingNumber()
    {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber)
    {
        this.trackingNumber = trackingNumber;
    }

    public String getLabelUrl()
    {
        return labelUrl;
    }

    public void setLabelUrl(String labelUrl)
    {
        this.labelUrl = labelUrl;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSourcePayloadJson()
    {
        return sourcePayloadJson;
    }

    public void setSourcePayloadJson(String sourcePayloadJson)
    {
        this.sourcePayloadJson = sourcePayloadJson;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
