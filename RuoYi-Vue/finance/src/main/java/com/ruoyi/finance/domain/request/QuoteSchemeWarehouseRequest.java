package com.ruoyi.finance.domain.request;

import java.util.List;

public class QuoteSchemeWarehouseRequest
{
    private List<String> warehouseCodes;

    public List<String> getWarehouseCodes()
    {
        return warehouseCodes;
    }

    public void setWarehouseCodes(List<String> warehouseCodes)
    {
        this.warehouseCodes = warehouseCodes;
    }
}
