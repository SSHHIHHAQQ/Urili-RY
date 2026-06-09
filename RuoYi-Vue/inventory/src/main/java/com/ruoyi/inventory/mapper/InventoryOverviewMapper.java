package com.ruoyi.inventory.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.inventory.domain.InventoryOfficialSourceStock;
import com.ruoyi.inventory.domain.InventoryProductSkuSnapshot;
import com.ruoyi.inventory.domain.InventoryProductSourceBindingSnapshot;
import com.ruoyi.inventory.domain.InventoryProductWarehouseSnapshot;
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventoryOverviewSellerOption;
import com.ruoyi.inventory.domain.InventoryOverviewWarehouseOption;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;

/**
 * 库存总览 Mapper。
 */
public interface InventoryOverviewMapper
{
    List<InventoryOverviewItem> selectSpuList(InventoryOverviewItem query);

    List<InventoryOverviewItem> selectSkuList(InventoryOverviewItem query);

    List<InventorySkuWarehouseStock> selectWarehouseStockList(InventorySkuWarehouseStock query);

    List<InventoryOverviewWarehouseOption> selectWarehouseOptions();

    List<InventoryOverviewWarehouseOption> selectOfficialWarehouseOptions();

    List<InventoryOverviewSellerOption> selectSellerOptions();

    List<InventorySkuWarehouseStock> selectWarehouseStockListBySkuId(@Param("skuId") Long skuId);

    List<InventorySkuWarehouseStock> selectWarehouseStockListBySpuId(@Param("spuId") Long spuId);

    InventorySkuWarehouseStock selectWarehouseStockById(@Param("stockId") Long stockId);

    int updateWarehouseStock(InventorySkuWarehouseStock stock);

    int insertLedger(InventoryStockLedger ledger);

    List<InventoryStockLedger> selectLedgerList(InventoryStockLedger query);

    int deleteObsoleteSkuWarehouseStocksBySpuId(@Param("spuId") Long spuId,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots,
        @Param("sourceBindings") List<InventoryProductSourceBindingSnapshot> sourceBindings,
        @Param("warehouseSnapshots") List<InventoryProductWarehouseSnapshot> warehouseSnapshots,
        @Param("sourceStocks") List<InventoryOfficialSourceStock> sourceStocks);

    int upsertOfficialMasterSkuWarehouseStocksBySpuId(@Param("spuId") Long spuId, @Param("operator") String operator,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots,
        @Param("sourceBindings") List<InventoryProductSourceBindingSnapshot> sourceBindings,
        @Param("sourceStocks") List<InventoryOfficialSourceStock> sourceStocks);

    int upsertSourceUnboundSkuWarehouseStocksBySpuId(@Param("spuId") Long spuId, @Param("operator") String operator,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots,
        @Param("sourceBindings") List<InventoryProductSourceBindingSnapshot> sourceBindings,
        @Param("warehouseSnapshots") List<InventoryProductWarehouseSnapshot> warehouseSnapshots);

    int upsertUnmatchedOfficialSkuWarehouseStocksBySpuId(@Param("spuId") Long spuId, @Param("operator") String operator,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots,
        @Param("sourceBindings") List<InventoryProductSourceBindingSnapshot> sourceBindings,
        @Param("sourceStocks") List<InventoryOfficialSourceStock> sourceStocks);

    int upsertThirdPartySkuWarehouseStocksBySpuId(@Param("spuId") Long spuId, @Param("operator") String operator,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots,
        @Param("warehouseSnapshots") List<InventoryProductWarehouseSnapshot> warehouseSnapshots);

    int upsertNoWarehouseSkuWarehouseStocksBySpuId(@Param("spuId") Long spuId, @Param("operator") String operator,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots,
        @Param("sourceBindings") List<InventoryProductSourceBindingSnapshot> sourceBindings,
        @Param("warehouseSnapshots") List<InventoryProductWarehouseSnapshot> warehouseSnapshots);

    int refreshSkuReadModel(@Param("skuId") Long skuId,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots);

    int refreshSpuReadModel(@Param("spuId") Long spuId,
        @Param("skuSnapshots") List<InventoryProductSkuSnapshot> skuSnapshots);
}
