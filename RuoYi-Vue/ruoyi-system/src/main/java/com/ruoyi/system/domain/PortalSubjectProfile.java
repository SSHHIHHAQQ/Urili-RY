package com.ruoyi.system.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Seller/buyer terminal visible subject profile.
 */
public class PortalSubjectProfile implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String terminal;

    private Long subjectId;

    private String subjectNo;

    private String subjectCode;

    private String subjectName;

    private String subjectShortName;

    private String subjectType;

    private String subjectLevel;

    private String status;

    private String countryCode;

    private String stateProvince;

    private String city;

    private String postalCode;

    private String addressLine1;

    private String addressLine2;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private PartnerProfile.Attachment attachment;

    private BigDecimal accountBalance;

    private String balanceCurrency;

    public String getTerminal()
    {
        return terminal;
    }

    public void setTerminal(String terminal)
    {
        this.terminal = terminal;
    }

    public Long getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(Long subjectId)
    {
        this.subjectId = subjectId;
    }

    public String getSubjectNo()
    {
        return subjectNo;
    }

    public void setSubjectNo(String subjectNo)
    {
        this.subjectNo = subjectNo;
    }

    public String getSubjectCode()
    {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode)
    {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName()
    {
        return subjectName;
    }

    public void setSubjectName(String subjectName)
    {
        this.subjectName = subjectName;
    }

    public String getSubjectShortName()
    {
        return subjectShortName;
    }

    public void setSubjectShortName(String subjectShortName)
    {
        this.subjectShortName = subjectShortName;
    }

    public String getSubjectType()
    {
        return subjectType;
    }

    public void setSubjectType(String subjectType)
    {
        this.subjectType = subjectType;
    }

    public String getSubjectLevel()
    {
        return subjectLevel;
    }

    public void setSubjectLevel(String subjectLevel)
    {
        this.subjectLevel = subjectLevel;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getCountryCode()
    {
        return countryCode;
    }

    public void setCountryCode(String countryCode)
    {
        this.countryCode = countryCode;
    }

    public String getStateProvince()
    {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince)
    {
        this.stateProvince = stateProvince;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getPostalCode()
    {
        return postalCode;
    }

    public void setPostalCode(String postalCode)
    {
        this.postalCode = postalCode;
    }

    public String getAddressLine1()
    {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1)
    {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2()
    {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2)
    {
        this.addressLine2 = addressLine2;
    }

    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail()
    {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail)
    {
        this.contactEmail = contactEmail;
    }

    public PartnerProfile.Attachment getAttachment()
    {
        return attachment;
    }

    public void setAttachment(PartnerProfile.Attachment attachment)
    {
        this.attachment = attachment;
    }

    public BigDecimal getAccountBalance()
    {
        return accountBalance;
    }

    public void setAccountBalance(BigDecimal accountBalance)
    {
        this.accountBalance = accountBalance;
    }

    public String getBalanceCurrency()
    {
        return balanceCurrency;
    }

    public void setBalanceCurrency(String balanceCurrency)
    {
        this.balanceCurrency = balanceCurrency;
    }
}
