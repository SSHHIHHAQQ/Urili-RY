package com.ruoyi.product.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品 SPU 发货仓库绑定，backed by product_spu_warehouse.
 */
public class ProductSpuWarehouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long spuId;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseKind;
    private String settlementCurrency;
    private Long sellerId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseKind() { return warehouseKind; }
    public void setWarehouseKind(String warehouseKind) { this.warehouseKind = warehouseKind; }
    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}
