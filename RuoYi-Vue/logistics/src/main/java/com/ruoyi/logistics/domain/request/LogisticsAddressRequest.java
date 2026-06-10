package com.ruoyi.logistics.domain.request;

import jakarta.validation.constraints.Size;

/**
 * 物流收/发件地址请求。
 */
public class LogisticsAddressRequest
{
    @Size(max = 100, message = "姓名不能超过100个字符")
    private String name;

    @Size(max = 200, message = "公司不能超过200个字符")
    private String company;

    @Size(max = 100, message = "电话不能超过100个字符")
    private String telephone;

    @Size(max = 32, message = "国家不能超过32个字符")
    private String country;

    @Size(max = 64, message = "州/省不能超过64个字符")
    private String state;

    @Size(max = 100, message = "城市不能超过100个字符")
    private String city;

    @Size(max = 255, message = "地址1不能超过255个字符")
    private String address1;

    @Size(max = 255, message = "地址2不能超过255个字符")
    private String address2;

    @Size(max = 32, message = "邮编不能超过32个字符")
    private String postcode;

    @Size(max = 64, message = "门牌号不能超过64个字符")
    private String doorplate;

    @Size(max = 100, message = "发件地址编码不能超过100个字符")
    private String shipperCode;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCompany()
    {
        return company;
    }

    public void setCompany(String company)
    {
        this.company = company;
    }

    public String getTelephone()
    {
        return telephone;
    }

    public void setTelephone(String telephone)
    {
        this.telephone = telephone;
    }

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getAddress1()
    {
        return address1;
    }

    public void setAddress1(String address1)
    {
        this.address1 = address1;
    }

    public String getAddress2()
    {
        return address2;
    }

    public void setAddress2(String address2)
    {
        this.address2 = address2;
    }

    public String getPostcode()
    {
        return postcode;
    }

    public void setPostcode(String postcode)
    {
        this.postcode = postcode;
    }

    public String getDoorplate()
    {
        return doorplate;
    }

    public void setDoorplate(String doorplate)
    {
        this.doorplate = doorplate;
    }

    public String getShipperCode()
    {
        return shipperCode;
    }

    public void setShipperCode(String shipperCode)
    {
        this.shipperCode = shipperCode;
    }
}
