package com.ruoyi.warehouse.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 美国城市字典。
 */
public class UsCity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long cityId;
    private String placeGeoid;
    private String stateCode;
    private String stateName;
    private String cityName;
    private String placeName;
    private String placeType;
    private String status;

    public Long getCityId()
    {
        return cityId;
    }

    public void setCityId(Long cityId)
    {
        this.cityId = cityId;
    }

    public String getPlaceGeoid()
    {
        return placeGeoid;
    }

    public void setPlaceGeoid(String placeGeoid)
    {
        this.placeGeoid = placeGeoid;
    }

    public String getStateCode()
    {
        return stateCode;
    }

    public void setStateCode(String stateCode)
    {
        this.stateCode = stateCode;
    }

    public String getStateName()
    {
        return stateName;
    }

    public void setStateName(String stateName)
    {
        this.stateName = stateName;
    }

    public String getCityName()
    {
        return cityName;
    }

    public void setCityName(String cityName)
    {
        this.cityName = cityName;
    }

    public String getPlaceName()
    {
        return placeName;
    }

    public void setPlaceName(String placeName)
    {
        this.placeName = placeName;
    }

    public String getPlaceType()
    {
        return placeType;
    }

    public void setPlaceType(String placeType)
    {
        this.placeType = placeType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
