package com.ruoyi.warehouse.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.warehouse.domain.UsCity;
import com.ruoyi.warehouse.domain.UsState;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.domain.WarehouseOption;

/**
 * 仓库 Mapper。
 */
public interface WarehouseMapper
{
    List<Warehouse> selectWarehouseList(Warehouse query);

    Warehouse selectWarehouseById(Long warehouseId);

    Warehouse selectWarehouseByCode(String warehouseCode);

    int countWarehouseCode(@Param("warehouseCode") String warehouseCode, @Param("excludeWarehouseId") Long excludeWarehouseId);

    int insertWarehouse(Warehouse warehouse);

    int updateWarehouse(Warehouse warehouse);

    int updateWarehouseStatus(@Param("warehouseId") Long warehouseId, @Param("status") String status,
        @Param("updateBy") String updateBy);

    int insertOfficialWarehouse(Warehouse warehouse);

    int updateOfficialWarehouse(Warehouse warehouse);

    int insertThirdPartyWarehouse(Warehouse warehouse);

    int updateThirdPartyWarehouse(Warehouse warehouse);

    List<UsState> selectUsStateList(@Param("keyword") String keyword);

    List<UsCity> selectUsCityList(@Param("stateName") String stateName, @Param("keyword") String keyword);

    List<WarehouseOption> selectNormalSellerOptions(@Param("keyword") String keyword);
}
