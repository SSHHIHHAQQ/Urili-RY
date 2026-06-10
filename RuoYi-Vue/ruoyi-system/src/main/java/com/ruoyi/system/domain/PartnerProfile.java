package com.ruoyi.system.domain;

import java.io.Serializable;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Buyer/seller shared profile fields.
 */
public abstract class PartnerProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private String status;

    private String username;

    private String legalId;

    private String businessLicenseNo;

    private String countryCode;

    private String stateProvince;

    private String city;

    private String postalCode;

    private String addressLine1;

    private String addressLine2;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String attachmentFileName;

    private String attachmentMimeType;

    private Long attachmentSizeBytes;

    private String attachmentFileUrl;

    private Attachment attachment;

    private Integer accountCount;

    private Date lastLoginTime;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Size(min = 0, max = 30, message = "用户名长度不能超过30个字符")
    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    @Size(min = 0, max = 100, message = "法人证件号长度不能超过100个字符")
    public String getLegalId()
    {
        return legalId;
    }

    public void setLegalId(String legalId)
    {
        this.legalId = legalId;
    }

    @Size(min = 0, max = 100, message = "营业执照号码长度不能超过100个字符")
    public String getBusinessLicenseNo()
    {
        return businessLicenseNo;
    }

    public void setBusinessLicenseNo(String businessLicenseNo)
    {
        this.businessLicenseNo = businessLicenseNo;
    }

    @NotBlank(message = "国家/地区不能为空")
    @Size(min = 0, max = 32, message = "国家/地区长度不能超过32个字符")
    public String getCountryCode()
    {
        return countryCode;
    }

    public void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }

    @Size(min = 0, max = 100, message = "省/州长度不能超过100个字符")
    public String getStateProvince()
    {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince)
    {
        this.stateProvince = stateProvince;
    }

    public String getState()
    {
        return stateProvince;
    }

    public void setState(String state)
    {
        this.stateProvince = state;
    }

    @NotBlank(message = "城市不能为空")
    @Size(min = 0, max = 100, message = "城市长度不能超过100个字符")
    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    @NotBlank(message = "邮编不能为空")
    @Size(min = 0, max = 32, message = "邮编长度不能超过32个字符")
    public String getPostalCode()
    {
        return postalCode;
    }

    public void setPostalCode(String postalCode)
    {
        this.postalCode = postalCode;
    }

    @NotBlank(message = "地址1不能为空")
    @Size(min = 0, max = 255, message = "地址1长度不能超过255个字符")
    public String getAddressLine1()
    {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1)
    {
        this.addressLine1 = addressLine1;
    }

    public String getAddress1()
    {
        return addressLine1;
    }

    public void setAddress1(String address1)
    {
        this.addressLine1 = address1;
    }

    @Size(min = 0, max = 255, message = "地址2长度不能超过255个字符")
    public String getAddressLine2()
    {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2)
    {
        this.addressLine2 = addressLine2;
    }

    public String getAddress2()
    {
        return addressLine2;
    }

    public void setAddress2(String address2)
    {
        this.addressLine2 = address2;
    }

    @NotBlank(message = "联系人不能为空")
    @Size(min = 0, max = 100, message = "联系人长度不能超过100个字符")
    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    @NotBlank(message = "手机号不能为空")
    @Size(min = 0, max = 64, message = "手机号长度不能超过64个字符")
    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    public String getPhone()
    {
        return contactPhone;
    }

    public void setPhone(String phone)
    {
        this.contactPhone = phone;
    }

    @Size(min = 0, max = 128, message = "邮箱长度不能超过128个字符")
    public String getContactEmail()
    {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail)
    {
        this.contactEmail = contactEmail;
    }

    public String getEmail()
    {
        return contactEmail;
    }

    public void setEmail(String email)
    {
        this.contactEmail = email;
    }

    public String getAttachmentFileName()
    {
        return attachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName)
    {
        this.attachmentFileName = attachmentFileName;
    }

    public String getAttachmentMimeType()
    {
        return attachmentMimeType;
    }

    public void setAttachmentMimeType(String attachmentMimeType)
    {
        this.attachmentMimeType = attachmentMimeType;
    }

    public Long getAttachmentSizeBytes()
    {
        return attachmentSizeBytes;
    }

    public void setAttachmentSizeBytes(Long attachmentSizeBytes)
    {
        this.attachmentSizeBytes = attachmentSizeBytes;
    }

    public String getAttachmentFileUrl()
    {
        return attachmentFileUrl;
    }

    public void setAttachmentFileUrl(String attachmentFileUrl)
    {
        this.attachmentFileUrl = attachmentFileUrl;
    }

    public Attachment getAttachment()
    {
        if (attachment != null)
        {
            return attachment;
        }
        if (StringUtils.isBlank(attachmentFileName) || StringUtils.isBlank(attachmentFileUrl))
        {
            return null;
        }
        Attachment value = new Attachment();
        value.setFileName(attachmentFileName);
        value.setMimeType(attachmentMimeType);
        value.setSizeBytes(attachmentSizeBytes);
        value.setFileUrl(attachmentFileUrl);
        return value;
    }

    public void setAttachment(Attachment attachment)
    {
        this.attachment = attachment;
        if (attachment == null)
        {
            this.attachmentFileName = "";
            this.attachmentMimeType = "";
            this.attachmentSizeBytes = null;
            this.attachmentFileUrl = "";
            return;
        }
        this.attachmentFileName = attachment.getFileName();
        this.attachmentMimeType = attachment.getMimeType();
        this.attachmentSizeBytes = attachment.getSizeBytes();
        this.attachmentFileUrl = attachment.getFileUrl();
    }

    public Integer getAccountCount()
    {
        return accountCount;
    }

    public void setAccountCount(Integer accountCount)
    {
        this.accountCount = accountCount;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public static class Attachment implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private String fileName;

        private String mimeType;

        private Long sizeBytes;

        private String fileUrl;

        public String getFileName()
        {
            return fileName;
        }

        public void setFileName(String fileName)
        {
            this.fileName = fileName;
        }

        public String getMimeType()
        {
            return mimeType;
        }

        public void setMimeType(String mimeType)
        {
            this.mimeType = mimeType;
        }

        public Long getSizeBytes()
        {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes)
        {
            this.sizeBytes = sizeBytes;
        }

        public String getFileUrl()
        {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl)
        {
            this.fileUrl = fileUrl;
        }

    }
}
