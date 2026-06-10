package com.ruoyi.finance.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Quote scheme warehouse scope detail.
 */
public class QuoteSchemeWarehouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long schemeWarehouseId;

    private Long schemeId;

    private String warehouseCode;

    private String warehouseNameSnapshot;

    private String warehouseKindSnapshot;

    public Long getSchemeWarehouseId()
    {
        return schemeWarehouseId;
    }

    public void setSchemeWarehouseId(Long schemeWarehouseId)
    {
        this.schemeWarehouseId = schemeWarehouseId;
    }

    public Long getSchemeId()
    {
        return schemeId;
    }

    public void setSchemeId(Long schemeId)
    {
        this.schemeId = schemeId;
    }

    public String getWarehouseCode()
    {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode)
    {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseNameSnapshot()
    {
        return warehouseNameSnapshot;
    }

    public void setWarehouseNameSnapshot(String warehouseNameSnapshot)
    {
        this.warehouseNameSnapshot = warehouseNameSnapshot;
    }

    public String getWarehouseKindSnapshot()
    {
        return warehouseKindSnapshot;
    }

    public void setWarehouseKindSnapshot(String warehouseKindSnapshot)
    {
        this.warehouseKindSnapshot = warehouseKindSnapshot;
    }
}
