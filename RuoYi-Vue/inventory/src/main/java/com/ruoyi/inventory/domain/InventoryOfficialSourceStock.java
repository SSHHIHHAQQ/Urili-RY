package com.ruoyi.inventory.domain;

import java.util.Date;

/**
 * 官方仓来源库存聚合行，按来源 SKU + 来源主仓名粒度提供给库存模块。
 */
public class InventoryOfficialSourceStock
{
    private String sourceScope;
    private String masterSku;
    private String masterProductName;
    private String masterWarehouseName;
    private Long sourceTotalQty;
    private Long sourceAvailableQty;
    private Long sourceInTransitQty;
    private Date sourceSnapshotTime;

    public String getSourceScope()
    {
        return sourceScope;
    }

    public void setSourceScope(String sourceScope)
    {
        this.sourceScope = sourceScope;
    }

    public String getMasterSku()
    {
        return masterSku;
    }

    public void setMasterSku(String masterSku)
    {
        this.masterSku = masterSku;
    }

    public String getMasterProductName()
    {
        return masterProductName;
    }

    public void setMasterProductName(String masterProductName)
    {
        this.masterProductName = masterProductName;
    }

    public String getMasterWarehouseName()
    {
        return masterWarehouseName;
    }

    public void setMasterWarehouseName(String masterWarehouseName)
    {
        this.masterWarehouseName = masterWarehouseName;
    }

    public Long getSourceTotalQty()
    {
        return sourceTotalQty;
    }

    public void setSourceTotalQty(Long sourceTotalQty)
    {
        this.sourceTotalQty = sourceTotalQty;
    }

    public Long getSourceAvailableQty()
    {
        return sourceAvailableQty;
    }

    public void setSourceAvailableQty(Long sourceAvailableQty)
    {
        this.sourceAvailableQty = sourceAvailableQty;
    }

    public Long getSourceInTransitQty()
    {
        return sourceInTransitQty;
    }

    public void setSourceInTransitQty(Long sourceInTransitQty)
    {
        this.sourceInTransitQty = sourceInTransitQty;
    }

    public Date getSourceSnapshotTime()
    {
        return sourceSnapshotTime;
    }

    public void setSourceSnapshotTime(Date sourceSnapshotTime)
    {
        this.sourceSnapshotTime = sourceSnapshotTime;
    }
}
