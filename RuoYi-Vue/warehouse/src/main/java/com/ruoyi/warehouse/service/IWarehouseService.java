package com.ruoyi.warehouse.service;

import java.util.List;
import com.ruoyi.finance.domain.FinanceCurrencyOption;
import com.ruoyi.warehouse.domain.UsCity;
import com.ruoyi.warehouse.domain.UsState;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.domain.WarehouseOption;
import com.ruoyi.warehouse.domain.WarehouseSyncCandidate;
import com.ruoyi.warehouse.domain.WarehouseSyncConnection;
import com.ruoyi.warehouse.domain.request.OfficialWarehouseSyncRequest;
import com.ruoyi.warehouse.domain.request.WarehouseStatusRequest;

/**
 * 仓库服务。
 */
public interface IWarehouseService
{
    List<Warehouse> selectOfficialWarehouseList(Warehouse query);

    List<Warehouse> selectThirdPartyWarehouseList(Warehouse query);

    Warehouse selectOfficialWarehouseById(Long warehouseId);

    Warehouse selectThirdPartyWarehouseById(Long warehouseId);

    int insertOfficialWarehouse(Warehouse warehouse);

    int insertThirdPartyWarehouse(Warehouse warehouse);

    int updateOfficialWarehouse(Warehouse warehouse);

    int updateThirdPartyWarehouse(Warehouse warehouse);

    int updateOfficialWarehouseStatus(WarehouseStatusRequest request);

    int updateThirdPartyWarehouseStatus(WarehouseStatusRequest request);

    List<WarehouseSyncConnection> selectSyncConnections(String keyword);

    List<WarehouseSyncCandidate> selectSyncCandidates(String connectionCode, String keyword);

    int syncOfficialWarehouse(OfficialWarehouseSyncRequest request);

    List<UsState> selectUsStateList(String keyword);

    List<UsCity> selectUsCityList(String stateName, String keyword);

    List<WarehouseOption> selectNormalSellerOptions(String keyword);

    List<FinanceCurrencyOption> selectEnabledCurrencyOptions();
}
