package com.ruoyi.warehouse.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.finance.domain.FinanceCurrencyOption;
import com.ruoyi.finance.service.IFinanceCurrencyService;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.support.UpstreamSystemConstants;
import com.ruoyi.warehouse.domain.UsCity;
import com.ruoyi.warehouse.domain.UsState;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.domain.WarehouseOption;
import com.ruoyi.warehouse.domain.WarehouseSyncCandidate;
import com.ruoyi.warehouse.domain.WarehouseSyncConnection;
import com.ruoyi.warehouse.domain.request.OfficialWarehouseSyncRequest;
import com.ruoyi.warehouse.domain.request.WarehouseStatusRequest;
import com.ruoyi.warehouse.mapper.WarehouseMapper;
import com.ruoyi.warehouse.service.IWarehouseService;

/**
 * 仓库服务实现。
 */
@Service
public class WarehouseServiceImpl implements IWarehouseService
{
    private static final String KIND_OFFICIAL = "official";
    private static final String KIND_THIRD_PARTY = "third_party";
    private static final String STATUS_NORMAL = "0";
    private static final String STATUS_DISABLED = "1";

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @Override
    public List<Warehouse> selectOfficialWarehouseList(Warehouse query)
    {
        query = normalizeQuery(query, KIND_OFFICIAL);
        return warehouseMapper.selectWarehouseList(query);
    }

    @Override
    public List<Warehouse> selectThirdPartyWarehouseList(Warehouse query)
    {
        query = normalizeQuery(query, KIND_THIRD_PARTY);
        return warehouseMapper.selectWarehouseList(query);
    }

    @Override
    public Warehouse selectWarehouseById(Long warehouseId)
    {
        if (warehouseId == null)
        {
            throw new ServiceException("仓库ID不能为空");
        }
        Warehouse warehouse = warehouseMapper.selectWarehouseById(warehouseId);
        if (warehouse == null)
        {
            throw new ServiceException("仓库不存在");
        }
        return warehouse;
    }

    @Override
    public Warehouse selectOfficialWarehouseById(Long warehouseId)
    {
        return selectWarehouseByIdAndKind(warehouseId, KIND_OFFICIAL);
    }

    @Override
    public Warehouse selectThirdPartyWarehouseById(Long warehouseId)
    {
        return selectWarehouseByIdAndKind(warehouseId, KIND_THIRD_PARTY);
    }

    @Override
    @Transactional
    public int insertOfficialWarehouse(Warehouse warehouse)
    {
        normalizeWarehouse(warehouse, KIND_OFFICIAL, true);
        warehouse.setCreateBy(SecurityUtils.getUsername());
        checkWarehouseCodeUnique(warehouse);
        int rows = warehouseMapper.insertWarehouse(warehouse);
        warehouseMapper.insertOfficialWarehouse(warehouse);
        return rows;
    }

    @Override
    @Transactional
    public int insertThirdPartyWarehouse(Warehouse warehouse)
    {
        normalizeWarehouse(warehouse, KIND_THIRD_PARTY, true);
        validateNormalSeller(warehouse.getSellerId());
        warehouse.setCreateBy(SecurityUtils.getUsername());
        checkWarehouseCodeUnique(warehouse);
        int rows = warehouseMapper.insertWarehouse(warehouse);
        warehouseMapper.insertThirdPartyWarehouse(warehouse);
        return rows;
    }

    @Override
    @Transactional
    public int updateOfficialWarehouse(Warehouse warehouse)
    {
        Warehouse current = selectOfficialWarehouseById(warehouse.getWarehouseId());
        warehouse.setWarehouseCode(current.getWarehouseCode());
        normalizeWarehouse(warehouse, KIND_OFFICIAL, false);
        warehouse.setUpdateBy(SecurityUtils.getUsername());
        int rows = warehouseMapper.updateWarehouse(warehouse);
        warehouseMapper.updateOfficialWarehouse(warehouse);
        return rows;
    }

    @Override
    @Transactional
    public int updateThirdPartyWarehouse(Warehouse warehouse)
    {
        Warehouse current = selectThirdPartyWarehouseById(warehouse.getWarehouseId());
        warehouse.setWarehouseCode(current.getWarehouseCode());
        normalizeWarehouse(warehouse, KIND_THIRD_PARTY, false);
        validateNormalSeller(warehouse.getSellerId());
        warehouse.setUpdateBy(SecurityUtils.getUsername());
        int rows = warehouseMapper.updateWarehouse(warehouse);
        warehouseMapper.updateThirdPartyWarehouse(warehouse);
        return rows;
    }

    @Override
    @Transactional
    public int updateOfficialWarehouseStatus(WarehouseStatusRequest request)
    {
        selectOfficialWarehouseById(request.getWarehouseId());
        assertStatus(request.getStatus());
        return warehouseMapper.updateWarehouseStatus(request.getWarehouseId(), request.getStatus(), SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int updateThirdPartyWarehouseStatus(WarehouseStatusRequest request)
    {
        selectThirdPartyWarehouseById(request.getWarehouseId());
        assertStatus(request.getStatus());
        return warehouseMapper.updateWarehouseStatus(request.getWarehouseId(), request.getStatus(), SecurityUtils.getUsername());
    }

    @Override
    public List<WarehouseSyncConnection> selectSyncConnections(String keyword)
    {
        UpstreamSystemConnection query = new UpstreamSystemConnection();
        query.setStatus(UpstreamSystemConstants.STATUS_ENABLED);
        return upstreamSystemService.selectConnectionList(query).stream()
            .filter(item -> StringUtils.isBlank(keyword)
                || StringUtils.containsIgnoreCase(item.getConnectionCode(), keyword)
                || StringUtils.containsIgnoreCase(item.getMasterWarehouseName(), keyword))
            .map(this::toSyncConnection)
            .collect(Collectors.toList());
    }

    @Override
    public List<WarehouseSyncCandidate> selectSyncCandidates(String connectionCode, String keyword)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode);
        Map<String, UpstreamWarehousePairing> pairingMap = upstreamSystemService.selectWarehousePairingList(connectionCode)
            .stream()
            .collect(Collectors.toMap(UpstreamWarehousePairing::getUpstreamWarehouseCode, Function.identity(), (left, right) -> left));
        return upstreamSystemService.selectWarehouseSyncList(connectionCode, null).stream()
            .filter(item -> StringUtils.isBlank(keyword)
                || StringUtils.containsIgnoreCase(item.getWarehouseCode(), keyword)
                || StringUtils.containsIgnoreCase(item.getWarehouseName(), keyword))
            .map(item -> toSyncCandidate(connection, item, pairingMap.get(item.getWarehouseCode())))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int syncOfficialWarehouse(OfficialWarehouseSyncRequest request)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(request.getConnectionCode());
        String upstreamWarehouseCode = trimRequired(request.getUpstreamWarehouseCode(), "上游仓库编码不能为空");
        UpstreamWarehouseSyncItem candidate = selectSyncCandidate(connection.getConnectionCode(), upstreamWarehouseCode);
        if (!UpstreamSystemConstants.STATUS_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("上游仓库不是可同步状态");
        }
        if (isUpstreamWarehousePaired(connection.getConnectionCode(), upstreamWarehouseCode))
        {
            throw new ServiceException("该上游仓库已配对，不能重复同步");
        }

        normalizeWarehouse(request, KIND_OFFICIAL, true);
        request.setCreateBy(SecurityUtils.getUsername());
        checkWarehouseCodeUnique(request);

        int rows = warehouseMapper.insertWarehouse(request);
        warehouseMapper.insertOfficialWarehouse(request);

        WarehousePairingRequest pairingRequest = new WarehousePairingRequest();
        pairingRequest.setUpstreamWarehouseCode(upstreamWarehouseCode);
        pairingRequest.setSystemWarehouseCode(request.getWarehouseCode());
        pairingRequest.setSystemWarehouseName(request.getWarehouseName());
        pairingRequest.setRemark("官方仓同步自动配对");
        upstreamSystemService.insertWarehousePairing(connection.getConnectionCode(), pairingRequest);
        return rows;
    }

    @Override
    public List<UsState> selectUsStateList(String keyword)
    {
        return warehouseMapper.selectUsStateList(StringUtils.trimToNull(keyword));
    }

    @Override
    public List<UsCity> selectUsCityList(String stateName, String keyword)
    {
        return warehouseMapper.selectUsCityList(StringUtils.trimToNull(stateName), StringUtils.trimToNull(keyword));
    }

    @Override
    public List<WarehouseOption> selectNormalSellerOptions(String keyword)
    {
        return warehouseMapper.selectNormalSellerOptions(StringUtils.trimToNull(keyword));
    }

    @Override
    public List<FinanceCurrencyOption> selectEnabledCurrencyOptions()
    {
        return financeCurrencyService.selectEnabledCurrencyOptions();
    }

    private Warehouse normalizeQuery(Warehouse query, String kind)
    {
        if (query == null)
        {
            query = new Warehouse();
        }
        query.setWarehouseKind(kind);
        query.setWarehouseCode(StringUtils.trimToNull(query.getWarehouseCode()));
        query.setWarehouseName(StringUtils.trimToNull(query.getWarehouseName()));
        query.setCountryCode(StringUtils.trimToNull(query.getCountryCode()));
        query.setStateProvince(StringUtils.trimToNull(query.getStateProvince()));
        query.setCity(StringUtils.trimToNull(query.getCity()));
        query.setStatus(StringUtils.trimToNull(query.getStatus()));
        query.setSellerKeyword(StringUtils.trimToNull(query.getSellerKeyword()));
        return query;
    }

    private Warehouse selectWarehouseByIdAndKind(Long warehouseId, String kind)
    {
        if (warehouseId == null)
        {
            throw new ServiceException("仓库ID不能为空");
        }
        Warehouse warehouse = warehouseMapper.selectWarehouseById(warehouseId);
        if (warehouse == null || !kind.equals(warehouse.getWarehouseKind()))
        {
            throw new ServiceException("仓库不存在");
        }
        return warehouse;
    }

    private void normalizeWarehouse(Warehouse warehouse, String kind, boolean create)
    {
        if (warehouse == null)
        {
            throw new ServiceException("仓库不能为空");
        }
        warehouse.setWarehouseKind(kind);
        warehouse.setWarehouseCode(trimRequired(warehouse.getWarehouseCode(), "仓库编码不能为空"));
        warehouse.setWarehouseName(trimRequired(warehouse.getWarehouseName(), "仓库名称不能为空"));
        warehouse.setCountryCode(trimRequired(warehouse.getCountryCode(), "国家/地区不能为空").toUpperCase());
        warehouse.setStateProvince(trimRequired(warehouse.getStateProvince(), "州/省不能为空"));
        warehouse.setCity(trimRequired(warehouse.getCity(), "城市不能为空"));
        warehouse.setPostalCode(trimRequired(warehouse.getPostalCode(), "邮编不能为空"));
        warehouse.setAddressLine1(trimRequired(warehouse.getAddressLine1(), "地址1不能为空"));
        warehouse.setAddressLine2(StringUtils.trimToEmpty(warehouse.getAddressLine2()));
        warehouse.setContactName(trimRequired(warehouse.getContactName(), "联系人不能为空"));
        warehouse.setContactPhone(StringUtils.trimToEmpty(warehouse.getContactPhone()));
        warehouse.setContactEmail(trimRequired(warehouse.getContactEmail(), "联系邮箱不能为空"));
        warehouse.setCompanyName(StringUtils.trimToEmpty(warehouse.getCompanyName()));
        warehouse.setSettlementCurrency(trimRequired(warehouse.getSettlementCurrency(), "结算币种不能为空").toUpperCase());
        warehouse.setRemark(StringUtils.trimToEmpty(warehouse.getRemark()));
        warehouse.setStatus(StringUtils.defaultIfBlank(warehouse.getStatus(), STATUS_NORMAL));
        assertStatus(warehouse.getStatus());
        validateCurrency(warehouse.getSettlementCurrency());
        if (!create && warehouse.getWarehouseId() == null)
        {
            throw new ServiceException("仓库ID不能为空");
        }
    }

    private void validateCurrency(String currencyCode)
    {
        boolean enabled = financeCurrencyService.selectEnabledCurrencyOptions().stream()
            .anyMatch(item -> currencyCode.equalsIgnoreCase(item.getValue()));
        if (!enabled)
        {
            throw new ServiceException("结算币种未启用");
        }
    }

    private void validateNormalSeller(Long sellerId)
    {
        if (sellerId == null)
        {
            throw new ServiceException("归属卖家不能为空");
        }
        if (warehouseMapper.countNormalSellerById(sellerId) <= 0)
        {
            throw new ServiceException("归属卖家不存在");
        }
    }

    private void checkWarehouseCodeUnique(Warehouse warehouse)
    {
        if (warehouseMapper.countWarehouseCode(warehouse.getWarehouseCode(), warehouse.getWarehouseId()) > 0)
        {
            throw new ServiceException("仓库编码已存在");
        }
    }

    private void assertStatus(String status)
    {
        if (!STATUS_NORMAL.equals(status) && !STATUS_DISABLED.equals(status))
        {
            throw new ServiceException("仓库状态不正确");
        }
    }

    private UpstreamSystemConnection selectEnabledConnection(String connectionCode)
    {
        String normalizedCode = trimRequired(connectionCode, "主仓接入编号不能为空");
        UpstreamSystemConnection connection = upstreamSystemService.selectConnectionByCode(normalizedCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步仓库");
        }
        return connection;
    }

    private UpstreamWarehouseSyncItem selectSyncCandidate(String connectionCode, String upstreamWarehouseCode)
    {
        return upstreamSystemService.selectWarehouseSyncList(connectionCode, null).stream()
            .filter(item -> upstreamWarehouseCode.equals(item.getWarehouseCode()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("上游仓库不在同步清单中，请先同步仓库"));
    }

    private boolean isUpstreamWarehousePaired(String connectionCode, String upstreamWarehouseCode)
    {
        return upstreamSystemService.selectWarehousePairingList(connectionCode).stream()
            .anyMatch(item -> upstreamWarehouseCode.equals(item.getUpstreamWarehouseCode()));
    }

    private WarehouseSyncConnection toSyncConnection(UpstreamSystemConnection connection)
    {
        WarehouseSyncConnection option = new WarehouseSyncConnection();
        option.setConnectionCode(connection.getConnectionCode());
        option.setMasterWarehouseName(connection.getMasterWarehouseName());
        option.setSystemKind(connection.getSystemKind());
        return option;
    }

    private WarehouseSyncCandidate toSyncCandidate(UpstreamSystemConnection connection, UpstreamWarehouseSyncItem item,
        UpstreamWarehousePairing pairing)
    {
        WarehouseSyncCandidate candidate = new WarehouseSyncCandidate();
        candidate.setConnectionCode(connection.getConnectionCode());
        candidate.setMasterWarehouseName(connection.getMasterWarehouseName());
        candidate.setWarehouseCode(item.getWarehouseCode());
        candidate.setWarehouseName(item.getWarehouseName());
        candidate.setCountryCode(item.getCountryCode());
        candidate.setStatus(item.getStatus());
        candidate.setPaired(pairing != null);
        if (pairing != null)
        {
            candidate.setSystemWarehouseCode(pairing.getSystemWarehouseCode());
            candidate.setSystemWarehouseName(pairing.getSystemWarehouseName());
        }
        return candidate;
    }

    private String trimRequired(String value, String message)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed))
        {
            throw new ServiceException(message);
        }
        return trimmed;
    }
}
