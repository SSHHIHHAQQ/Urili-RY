package com.ruoyi.inventory.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;

/**
 * 库存总览 Mapper。
 */
public interface InventoryOverviewMapper
{
    List<InventoryOverviewItem> selectSpuList(InventoryOverviewItem query);

    List<InventoryOverviewItem> selectSkuList(InventoryOverviewItem query);

    List<InventorySkuWarehouseStock> selectWarehouseStockListBySkuId(@Param("skuId") Long skuId);

    InventorySkuWarehouseStock selectWarehouseStockById(@Param("stockId") Long stockId);

    int updateWarehouseStock(InventorySkuWarehouseStock stock);

    int insertLedger(InventoryStockLedger ledger);

    List<InventoryStockLedger> selectLedgerList(InventoryStockLedger query);

    int refreshSkuReadModel(@Param("skuId") Long skuId);

    int refreshSpuReadModel(@Param("spuId") Long spuId);
}
