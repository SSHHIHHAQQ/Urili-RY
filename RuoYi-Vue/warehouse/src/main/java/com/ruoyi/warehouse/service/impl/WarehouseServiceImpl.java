package com.ruoyi.warehouse.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.finance.domain.FinanceCurrencyOption;
import com.ruoyi.finance.service.IFinanceCurrencyService;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairingSnapshot;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.service.IUpstreamWarehousePairingProjectionService;
import com.ruoyi.integration.support.UpstreamSystemConstants;
import com.ruoyi.warehouse.domain.UsCity;
import com.ruoyi.warehouse.domain.UsState;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.domain.WarehouseOption;
import com.ruoyi.warehouse.domain.WarehouseSellerProfile;
import com.ruoyi.warehouse.domain.WarehouseSyncCandidate;
import com.ruoyi.warehouse.domain.WarehouseSyncConnection;
import com.ruoyi.warehouse.domain.request.OfficialWarehousePairingRequest;
import com.ruoyi.warehouse.domain.request.OfficialWarehouseSyncRequest;
import com.ruoyi.warehouse.domain.request.WarehouseStatusRequest;
import com.ruoyi.warehouse.mapper.WarehouseMapper;
import com.ruoyi.warehouse.service.IWarehouseService;
import com.ruoyi.warehouse.service.WarehouseSellerLookupService;

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
    private static final String NO_PAIRING_VALUE = "__NO_PAIRING__";

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @Autowired
    private IUpstreamSystemService upstreamSystemService;

    @Autowired
    private ObjectProvider<WarehouseSellerLookupService> sellerLookupServiceProvider;

    @Autowired
    private ObjectProvider<IUpstreamWarehousePairingProjectionService> warehousePairingProjectionServiceProvider;

    @Override
    public List<Warehouse> selectOfficialWarehouseList(Warehouse query)
    {
        query = normalizeQuery(query, KIND_OFFICIAL);
        List<Warehouse> warehouses = warehouseMapper.selectWarehouseList(query);
        enrichWarehousePairings(warehouses);
        return warehouses;
    }

    @Override
    public List<Warehouse> selectThirdPartyWarehouseList(Warehouse query)
    {
        query = normalizeQuery(query, KIND_THIRD_PARTY);
        if (StringUtils.isNotBlank(query.getSellerKeyword()) && !Boolean.TRUE.equals(query.getSellerKeywordPrepared()))
        {
            throw new ServiceException("第三方仓库卖家筛选未预处理");
        }
        List<Warehouse> warehouses = warehouseMapper.selectWarehouseList(query);
        enrichWarehousePairings(warehouses);
        enrichSellerProfiles(warehouses);
        return warehouses;
    }

    @Override
    public boolean prepareThirdPartyWarehouseQuery(Warehouse query)
    {
        if (query == null)
        {
            return true;
        }
        query.setSellerKeywordPrepared(Boolean.FALSE);
        query.setSellerIds(null);
        String sellerKeyword = StringUtils.trimToNull(query.getSellerKeyword());
        query.setSellerKeyword(sellerKeyword);
        if (sellerKeyword == null)
        {
            return true;
        }

        List<WarehouseSellerProfile> sellers = sellerLookupService().selectSellerProfilesByKeyword(sellerKeyword);
        query.setSellerKeywordPrepared(Boolean.TRUE);
        if (sellers.isEmpty())
        {
            return false;
        }
        if (query.getSellerId() != null)
        {
            return sellers.stream().anyMatch(item -> Objects.equals(query.getSellerId(), item.getSellerId()));
        }
        query.setSellerIds(sellers.stream()
            .map(WarehouseSellerProfile::getSellerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
        return !query.getSellerIds().isEmpty();
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
        enrichWarehousePairing(warehouse);
        enrichSellerProfile(warehouse);
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
        return selectOfficialSyncConnections(keyword);
    }

    @Override
    public List<WarehouseSyncCandidate> selectSyncCandidates(String connectionCode, String keyword)
    {
        UpstreamSystemConnection connection = selectEnabledSyncConnection(connectionCode);
        String pairingRole = pairingRoleForSettlementType(connection.getSettlementType());
        return selectCandidatesForConnection(connection, pairingRole, keyword);
    }

    @Override
    @Transactional
    public int syncOfficialWarehouse(OfficialWarehouseSyncRequest request)
    {
        UpstreamSystemConnection connection = selectEnabledSyncConnection(request.getConnectionCode());
        String pairingRole = pairingRoleForSettlementType(connection.getSettlementType());
        String upstreamWarehouseCode = trimRequired(request.getUpstreamWarehouseCode(), "上游仓库编码不能为空");
        UpstreamWarehouseSyncItem candidate = selectSyncCandidate(connection.getConnectionCode(), upstreamWarehouseCode);
        if (!UpstreamSystemConstants.STATUS_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("上游仓库不是可同步状态");
        }
        if (isUpstreamWarehousePaired(connection.getConnectionCode(), upstreamWarehouseCode, pairingRole))
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
        pairingRequest.setPairingRole(pairingRole);
        pairingRequest.setRemark("官方仓同步自动配对：" + pairingRoleLabel(pairingRole) + "仓");
        upstreamSystemService.insertWarehousePairing(connection.getConnectionCode(), pairingRequest);
        return rows;
    }

    @Override
    public List<WarehouseSyncConnection> selectPairingConnections(String pairingRole, String keyword)
    {
        return selectConnectionsByPairingRole(normalizePairingRole(pairingRole), keyword);
    }

    @Override
    public List<WarehouseSyncCandidate> selectPairingCandidates(String pairingRole, String connectionCode, String keyword)
    {
        return selectCandidatesByPairingRole(normalizePairingRole(pairingRole), connectionCode, keyword);
    }

    @Override
    @Transactional
    public int pairOfficialWarehouse(Long warehouseId, OfficialWarehousePairingRequest request)
    {
        Warehouse warehouse = selectOfficialWarehouseById(warehouseId);
        String pairingRole = normalizePairingRole(request.getPairingRole());
        Long existingPairingId = UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(pairingRole)
            ? warehouse.getQuoteWarehousePairingId()
            : warehouse.getWarehousePairingId();
        String existingConnectionCode = UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(pairingRole)
            ? warehouse.getQuoteConnectionCode()
            : warehouse.getConnectionCode();
        if (isUnpairRequest(request))
        {
            if (existingPairingId == null)
            {
                return 1;
            }
            return upstreamSystemService.deleteWarehousePairing(existingConnectionCode, existingPairingId);
        }

        UpstreamSystemConnection connection = selectEnabledConnection(request.getConnectionCode(), pairingRole);
        String upstreamWarehouseCode = trimRequired(request.getUpstreamWarehouseCode(), "主仓仓库编码不能为空");
        UpstreamWarehouseSyncItem candidate = selectSyncCandidate(connection.getConnectionCode(), upstreamWarehouseCode);
        if (!UpstreamSystemConstants.STATUS_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("主仓仓库不是可配对状态");
        }

        if (existingPairingId != null)
        {
            upstreamSystemService.deleteWarehousePairing(existingConnectionCode, existingPairingId);
        }

        WarehousePairingRequest pairingRequest = new WarehousePairingRequest();
        pairingRequest.setUpstreamWarehouseCode(upstreamWarehouseCode);
        pairingRequest.setSystemWarehouseCode(warehouse.getWarehouseCode());
        pairingRequest.setSystemWarehouseName(warehouse.getWarehouseName());
        pairingRequest.setPairingRole(pairingRole);
        pairingRequest.setRemark("官方仓手工配对");
        return upstreamSystemService.insertWarehousePairing(connection.getConnectionCode(), pairingRequest);
    }

    private boolean isUnpairRequest(OfficialWarehousePairingRequest request)
    {
        return Boolean.TRUE.equals(request.getUnpair())
            || NO_PAIRING_VALUE.equals(request.getConnectionCode())
            || NO_PAIRING_VALUE.equals(request.getUpstreamWarehouseCode());
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
        return sellerLookupService().selectNormalSellerOptions(StringUtils.trimToNull(keyword)).stream()
            .map(this::toWarehouseOption)
            .collect(Collectors.toList());
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
        enrichWarehousePairing(warehouse);
        enrichSellerProfile(warehouse);
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
        if (!sellerLookupService().isNormalSeller(sellerId))
        {
            throw new ServiceException("归属卖家不存在");
        }
    }

    private void enrichSellerProfiles(List<Warehouse> warehouses)
    {
        if (warehouses == null || warehouses.isEmpty())
        {
            return;
        }
        Set<Long> sellerIds = warehouses.stream()
            .map(Warehouse::getSellerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (sellerIds.isEmpty())
        {
            return;
        }
        Map<Long, WarehouseSellerProfile> sellerMap = sellerLookupService().selectSellerProfilesByIds(sellerIds)
            .stream()
            .collect(Collectors.toMap(WarehouseSellerProfile::getSellerId, Function.identity(), (left, right) -> left));
        for (Warehouse warehouse : warehouses)
        {
            applySellerProfile(warehouse, sellerMap.get(warehouse.getSellerId()));
        }
    }

    private void enrichWarehousePairings(List<Warehouse> warehouses)
    {
        if (warehouses == null || warehouses.isEmpty())
        {
            return;
        }
        List<String> warehouseCodes = warehouses.stream()
            .map(Warehouse::getWarehouseCode)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .collect(Collectors.toList());
        if (warehouseCodes.isEmpty())
        {
            return;
        }
        Map<String, UpstreamWarehousePairingSnapshot> pairingMap =
            warehousePairingProjectionService().selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(
                    warehouseCodes)
                .stream()
                .filter(item -> StringUtils.isNotBlank(item.getSystemWarehouseCode()))
                .filter(item -> StringUtils.isNotBlank(item.getPairingRole()))
                .collect(Collectors.toMap(
                    item -> pairingKey(item.getSystemWarehouseCode(), item.getPairingRole()),
                    Function.identity(),
                    (left, right) -> left));
        for (Warehouse warehouse : warehouses)
        {
            applyFulfillmentPairing(warehouse, pairingMap.get(pairingKey(warehouse.getWarehouseCode(),
                UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT)));
            applyQuotePairing(warehouse, pairingMap.get(pairingKey(warehouse.getWarehouseCode(),
                UpstreamSystemConstants.PAIRING_ROLE_QUOTE)));
        }
    }

    private void enrichWarehousePairing(Warehouse warehouse)
    {
        if (warehouse == null)
        {
            return;
        }
        enrichWarehousePairings(List.of(warehouse));
    }

    private void applyFulfillmentPairing(Warehouse warehouse, UpstreamWarehousePairingSnapshot pairing)
    {
        if (warehouse == null || pairing == null)
        {
            return;
        }
        warehouse.setWarehousePairingId(pairing.getWarehousePairingId());
        warehouse.setConnectionCode(pairing.getConnectionCode());
        warehouse.setMasterWarehouseName(pairing.getMasterWarehouseName());
        warehouse.setUpstreamWarehouseCode(pairing.getUpstreamWarehouseCode());
        warehouse.setUpstreamWarehouseName(pairing.getUpstreamWarehouseName());
        warehouse.setPairingRole(pairing.getPairingRole());
        warehouse.setPairingStatus(pairing.getStatus());
    }

    private void applyQuotePairing(Warehouse warehouse, UpstreamWarehousePairingSnapshot pairing)
    {
        if (warehouse == null || pairing == null)
        {
            return;
        }
        warehouse.setQuoteWarehousePairingId(pairing.getWarehousePairingId());
        warehouse.setQuoteConnectionCode(pairing.getConnectionCode());
        warehouse.setQuoteMasterWarehouseName(pairing.getMasterWarehouseName());
        warehouse.setQuoteUpstreamWarehouseCode(pairing.getUpstreamWarehouseCode());
        warehouse.setQuoteUpstreamWarehouseName(pairing.getUpstreamWarehouseName());
        warehouse.setQuotePairingStatus(pairing.getStatus());
    }

    private String pairingKey(String warehouseCode, String pairingRole)
    {
        return StringUtils.trimToEmpty(warehouseCode) + "::" + StringUtils.trimToEmpty(pairingRole);
    }

    private void enrichSellerProfile(Warehouse warehouse)
    {
        if (warehouse == null || warehouse.getSellerId() == null)
        {
            return;
        }
        applySellerProfile(warehouse, sellerLookupService().selectSellerProfile(warehouse.getSellerId()));
    }

    private void applySellerProfile(Warehouse warehouse, WarehouseSellerProfile seller)
    {
        if (warehouse == null || seller == null)
        {
            return;
        }
        warehouse.setSellerNo(seller.getSellerNo());
        warehouse.setSellerCode(seller.getSellerCode());
        warehouse.setSellerName(seller.getSellerName());
        warehouse.setSellerShortName(seller.getSellerShortName());
    }

    private WarehouseOption toWarehouseOption(WarehouseSellerProfile seller)
    {
        WarehouseOption option = new WarehouseOption();
        String sellerCode = StringUtils.defaultString(seller.getSellerCode());
        String sellerName = StringUtils.defaultIfBlank(seller.getSellerShortName(), seller.getSellerName());
        option.setLabel(StringUtils.isBlank(sellerCode) ? sellerName : sellerCode + " - " + sellerName);
        option.setValue(seller.getSellerId());
        option.setCode(seller.getSellerCode());
        option.setName(seller.getSellerName());
        option.setSearchText(String.join(" ", nonNullSellerOptionParts(seller)));
        return option;
    }

    private List<String> nonNullSellerOptionParts(WarehouseSellerProfile seller)
    {
        return java.util.stream.Stream.of(seller.getSellerNo(), seller.getSellerCode(), seller.getSellerName(),
                seller.getSellerShortName())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private WarehouseSellerLookupService sellerLookupService()
    {
        WarehouseSellerLookupService service = sellerLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("卖家资料查询服务不可用");
        }
        return service;
    }

    private IUpstreamWarehousePairingProjectionService warehousePairingProjectionService()
    {
        IUpstreamWarehousePairingProjectionService service = warehousePairingProjectionServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("上游仓库配对投影服务不可用");
        }
        return service;
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

    private List<WarehouseSyncConnection> selectConnectionsByPairingRole(String pairingRole, String keyword)
    {
        String settlementType = settlementTypeForPairingRole(pairingRole);
        UpstreamSystemConnection query = new UpstreamSystemConnection();
        query.setStatus(UpstreamSystemConstants.STATUS_ENABLED);
        return upstreamSystemService.selectConnectionList(query).stream()
            .filter(item -> settlementType.equals(normalizeSettlementType(item.getSettlementType())))
            .filter(item -> StringUtils.isBlank(keyword)
                || StringUtils.containsIgnoreCase(item.getConnectionCode(), keyword)
                || StringUtils.containsIgnoreCase(item.getMasterWarehouseName(), keyword))
            .map(this::toSyncConnection)
            .collect(Collectors.toList());
    }

    private List<WarehouseSyncConnection> selectOfficialSyncConnections(String keyword)
    {
        UpstreamSystemConnection query = new UpstreamSystemConnection();
        query.setStatus(UpstreamSystemConstants.STATUS_ENABLED);
        return upstreamSystemService.selectConnectionList(query).stream()
            .filter(item -> isOfficialSyncSettlementType(item.getSettlementType()))
            .filter(item -> StringUtils.isBlank(keyword)
                || StringUtils.containsIgnoreCase(item.getConnectionCode(), keyword)
                || StringUtils.containsIgnoreCase(item.getMasterWarehouseName(), keyword))
            .map(this::toSyncConnection)
            .collect(Collectors.toList());
    }

    private List<WarehouseSyncCandidate> selectCandidatesByPairingRole(String pairingRole, String connectionCode,
        String keyword)
    {
        UpstreamSystemConnection connection = selectEnabledConnection(connectionCode, pairingRole);
        return selectCandidatesForConnection(connection, pairingRole, keyword);
    }

    private List<WarehouseSyncCandidate> selectCandidatesForConnection(UpstreamSystemConnection connection,
        String pairingRole, String keyword)
    {
        String connectionCode = connection.getConnectionCode();
        Map<String, UpstreamWarehousePairing> pairingMap = upstreamSystemService.selectWarehousePairingList(connectionCode)
            .stream()
            .filter(item -> pairingRole.equals(item.getPairingRole()))
            .filter(item -> UpstreamSystemConstants.STATUS_ACTIVE.equals(item.getStatus()))
            .collect(Collectors.toMap(UpstreamWarehousePairing::getUpstreamWarehouseCode, Function.identity(), (left, right) -> left));
        return upstreamSystemService.selectWarehouseSyncList(connectionCode, null).stream()
            .filter(item -> StringUtils.isBlank(keyword)
                || StringUtils.containsIgnoreCase(item.getWarehouseCode(), keyword)
                || StringUtils.containsIgnoreCase(item.getWarehouseName(), keyword))
            .map(item -> toSyncCandidate(connection, item, pairingMap.get(item.getWarehouseCode()), pairingRole))
            .collect(Collectors.toList());
    }

    private UpstreamSystemConnection selectEnabledConnection(String connectionCode, String pairingRole)
    {
        String normalizedCode = trimRequired(connectionCode, "主仓接入编号不能为空");
        UpstreamSystemConnection connection = upstreamSystemService.selectConnectionByCode(normalizedCode);
        if (connection == null || !UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步仓库");
        }
        String expectedSettlementType = settlementTypeForPairingRole(pairingRole);
        if (!expectedSettlementType.equals(normalizeSettlementType(connection.getSettlementType())))
        {
            throw new ServiceException("该主仓接入不能作为" + pairingRoleLabel(pairingRole) + "仓");
        }
        return connection;
    }

    private UpstreamSystemConnection selectEnabledSyncConnection(String connectionCode)
    {
        String normalizedCode = trimRequired(connectionCode, "主仓接入编号不能为空");
        UpstreamSystemConnection connection = upstreamSystemService.selectConnectionByCode(normalizedCode);
        if (connection == null || !UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步仓库");
        }
        pairingRoleForSettlementType(connection.getSettlementType());
        return connection;
    }

    private UpstreamWarehouseSyncItem selectSyncCandidate(String connectionCode, String upstreamWarehouseCode)
    {
        return upstreamSystemService.selectWarehouseSyncList(connectionCode, null).stream()
            .filter(item -> upstreamWarehouseCode.equals(item.getWarehouseCode()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("上游仓库不在同步清单中，请先同步仓库"));
    }

    private boolean isUpstreamWarehousePaired(String connectionCode, String upstreamWarehouseCode, String pairingRole)
    {
        return upstreamSystemService.selectWarehousePairingList(connectionCode).stream()
            .anyMatch(item -> pairingRole.equals(item.getPairingRole())
                && UpstreamSystemConstants.STATUS_ACTIVE.equals(item.getStatus())
                && upstreamWarehouseCode.equals(item.getUpstreamWarehouseCode()));
    }

    private String normalizePairingRole(String pairingRole)
    {
        String normalized = trimRequired(pairingRole, "配对仓库类型不能为空").toUpperCase();
        if (!UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT.equals(normalized)
            && !UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(normalized))
        {
            throw new ServiceException("配对仓库类型不正确");
        }
        return normalized;
    }

    private String settlementTypeForPairingRole(String pairingRole)
    {
        return UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(pairingRole)
            ? UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE
            : UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE;
    }

    private String pairingRoleForSettlementType(String settlementType)
    {
        String normalized = normalizeSettlementType(settlementType);
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE.equals(normalized))
        {
            return UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT;
        }
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(normalized))
        {
            return UpstreamSystemConstants.PAIRING_ROLE_QUOTE;
        }
        throw new ServiceException("主仓接入结算类型不支持同步仓库");
    }

    private boolean isOfficialSyncSettlementType(String settlementType)
    {
        String normalized = normalizeSettlementType(settlementType);
        return UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE.equals(normalized)
            || UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(normalized);
    }

    private String normalizeSettlementType(String value)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE.equalsIgnoreCase(trimmed)
            || "UPSTREAM_PAYABLE".equalsIgnoreCase(trimmed))
        {
            return UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE;
        }
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equalsIgnoreCase(trimmed)
            || "PLATFORM_ADVANCE".equalsIgnoreCase(trimmed))
        {
            return UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE;
        }
        return trimmed;
    }

    private String pairingRoleLabel(String pairingRole)
    {
        return UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(pairingRole) ? "报价" : "履约";
    }

    private WarehouseSyncConnection toSyncConnection(UpstreamSystemConnection connection)
    {
        WarehouseSyncConnection option = new WarehouseSyncConnection();
        option.setConnectionCode(connection.getConnectionCode());
        option.setMasterWarehouseName(connection.getMasterWarehouseName());
        option.setSystemKind(connection.getSystemKind());
        option.setSettlementType(normalizeSettlementType(connection.getSettlementType()));
        return option;
    }

    private WarehouseSyncCandidate toSyncCandidate(UpstreamSystemConnection connection, UpstreamWarehouseSyncItem item,
        UpstreamWarehousePairing pairing, String pairingRole)
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
            candidate.setPairingRole(pairing.getPairingRole());
            candidate.setSystemWarehouseCode(pairing.getSystemWarehouseCode());
            candidate.setSystemWarehouseName(pairing.getSystemWarehouseName());
        }
        else
        {
            candidate.setPairingRole(pairingRole);
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
