package com.ruoyi.integration.service.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.integration.domain.IntegrationOption;
import com.ruoyi.integration.domain.SourceProductItem;
import com.ruoyi.integration.domain.SourceWarehouseStockGroupItem;
import com.ruoyi.integration.domain.SourceWarehouseStockItem;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelPairing;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelSyncItem;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuPairing;
import com.ruoyi.integration.domain.UpstreamSkuPairingAuditEvent;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSyncState;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.query.SourceProductQuery;
import com.ruoyi.integration.domain.query.SourceWarehouseStockQuery;
import com.ruoyi.integration.domain.request.LogisticsChannelPairingRequest;
import com.ruoyi.integration.domain.request.SkuPairingRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionInfoRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionRequest;
import com.ruoyi.integration.domain.request.UpstreamCredentialRequest;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.domain.response.SourceProductGroupDetail;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.service.IUpstreamSystemService;
import com.ruoyi.integration.support.UpstreamMaskUtils;
import com.ruoyi.integration.support.UpstreamSystemConstants;
import com.ruoyi.integration.sync.UpstreamLingxingClientFactory;
import com.ruoyi.system.service.support.SecretCipherSupport;

/**
 * 上游系统管理服务实现。
 */
@Service
public class UpstreamSystemServiceImpl implements IUpstreamSystemService
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Autowired
    private SourceProductReadModelService sourceProductReadModelService;

    @Autowired
    private SourceWarehouseStockReadModelService sourceWarehouseStockReadModelService;

    @Autowired
    private SecretCipherSupport secretCipherSupport;

    @Autowired
    private UpstreamLingxingClientFactory lingxingClientFactory;

    @Override
    public List<UpstreamSystemConnection> selectConnectionList(UpstreamSystemConnection query)
    {
        return upstreamSystemMapper.selectConnectionList(query);
    }

    @Override
    public UpstreamSystemConnection selectConnectionByCode(String connectionCode)
    {
        UpstreamSystemConnection connection = upstreamSystemMapper.selectConnectionByCode(connectionCode);
        if (connection == null)
        {
            throw new ServiceException("主仓接入不存在");
        }
        return connection;
    }

    @Override
    @Transactional
    public int insertConnection(UpstreamConnectionRequest request)
    {
        String systemKind = normalizeSystemKind(request.getSystemKind());
        String connectionCode = StringUtils.defaultIfBlank(trimOptional(request.getConnectionCode()),
            generateConnectionCode(request.getMasterWarehouseName()));
        if (upstreamSystemMapper.selectConnectionByCode(connectionCode) != null)
        {
            throw new ServiceException("主仓接入编号已存在");
        }
        String appKey = trimRequired(request.getAppKey(), "appKey不能为空");
        String appSecret = trimRequired(request.getAppSecret(), "appSecret不能为空");
        checkCredentials(connectionCode, appKey, appSecret);

        UpstreamSystemConnection connection = new UpstreamSystemConnection();
        connection.setConnectionCode(connectionCode);
        connection.setSystemKind(systemKind);
        connection.setMasterWarehouseName(trimRequired(request.getMasterWarehouseName(), "主仓名称不能为空"));
        connection.setSettlementType(normalizeSettlementType(request.getSettlementType()));
        connection.setAppKeyMask(UpstreamMaskUtils.mask(appKey));
        connection.setAppSecretMask(UpstreamMaskUtils.mask(appSecret));
        connection.setAppKeyCiphertext(secretCipherSupport.encrypt(appKey));
        connection.setAppSecretCiphertext(secretCipherSupport.encrypt(appSecret));
        connection.setCredentialKeyId(secretCipherSupport.getEncryptionKeyId());
        connection.setStatus(UpstreamSystemConstants.STATUS_ENABLED);
        connection.setCredentialStatus(UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED);
        connection.setEnabledCapabilities(UpstreamSystemConstants.DEFAULT_CAPABILITIES);
        Integer maxOrder = upstreamSystemMapper.selectMaxDisplayOrder();
        connection.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        connection.setLastAuthorizedTime(new Date());
        connection.setRequestLogCount(0);
        connection.setCreateBy(SecurityUtils.getUsername());
        connection.setRemark(trimOptional(request.getRemark()));
        return upstreamSystemMapper.insertConnection(connection);
    }

    @Override
    @Transactional
    public int updateConnectionInfo(String connectionCode, UpstreamConnectionInfoRequest request)
    {
        selectConnectionByCode(connectionCode);
        UpstreamSystemConnection connection = new UpstreamSystemConnection();
        connection.setConnectionCode(connectionCode);
        connection.setMasterWarehouseName(trimRequired(request.getMasterWarehouseName(), "主仓名称不能为空"));
        connection.setSettlementType(normalizeSettlementType(request.getSettlementType()));
        connection.setUpdateBy(SecurityUtils.getUsername());
        connection.setRemark(trimOptional(request.getRemark()));
        int rows = upstreamSystemMapper.updateConnectionInfo(connection);
        sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
        return rows;
    }

    @Override
    @Transactional
    public int updateConnectionCredentials(String connectionCode, UpstreamCredentialRequest request)
    {
        selectConnectionByCode(connectionCode);
        String appKey = trimRequired(request.getAppKey(), "appKey不能为空");
        String appSecret = trimRequired(request.getAppSecret(), "appSecret不能为空");
        checkCredentials(connectionCode, appKey, appSecret);

        UpstreamSystemConnection connection = new UpstreamSystemConnection();
        connection.setConnectionCode(connectionCode);
        connection.setAppKeyMask(UpstreamMaskUtils.mask(appKey));
        connection.setAppSecretMask(UpstreamMaskUtils.mask(appSecret));
        connection.setAppKeyCiphertext(secretCipherSupport.encrypt(appKey));
        connection.setAppSecretCiphertext(secretCipherSupport.encrypt(appSecret));
        connection.setCredentialKeyId(secretCipherSupport.getEncryptionKeyId());
        connection.setCredentialStatus(UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED);
        connection.setLastAuthorizedTime(new Date());
        connection.setUpdateBy(SecurityUtils.getUsername());
        return upstreamSystemMapper.updateConnectionCredentials(connection);
    }

    @Override
    public int updateConnectionStatus(String connectionCode, String status)
    {
        selectConnectionByCode(connectionCode);
        String normalizedStatus = trimRequired(status, "状态不能为空").toUpperCase();
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(normalizedStatus)
            && !UpstreamSystemConstants.STATUS_DISABLED.equals(normalizedStatus))
        {
            throw new ServiceException("接入状态只能是 ENABLED 或 DISABLED");
        }
        return upstreamSystemMapper.updateConnectionStatus(connectionCode, normalizedStatus, SecurityUtils.getUsername());
    }

    @Override
    @Transactional
    public int updateConnectionOrder(List<String> connectionCodes)
    {
        if (connectionCodes == null || connectionCodes.isEmpty())
        {
            throw new ServiceException("排序主仓不能为空");
        }
        Set<String> seenCodes = new HashSet<>();
        int rows = 0;
        int displayOrder = 1;
        for (String rawCode : connectionCodes)
        {
            String connectionCode = trimRequired(rawCode, "主仓接入编号不能为空");
            if (!seenCodes.add(connectionCode))
            {
                throw new ServiceException("排序主仓不能重复：" + connectionCode);
            }
            selectConnectionByCode(connectionCode);
            rows += upstreamSystemMapper.updateConnectionDisplayOrder(connectionCode, displayOrder++, SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    public int authorize(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        LingxingOpenApiClient client = lingxingClientFactory.createClient(connection);
        try
        {
            client.checkWarehouseAccess(UUID.randomUUID().toString());
            connection.setCredentialStatus(UpstreamSystemConstants.CREDENTIAL_STATUS_CONFIGURED);
            connection.setLastAuthorizedTime(new Date());
            connection.setUpdateBy(SecurityUtils.getUsername());
            upstreamSystemMapper.updateConnectionCredentials(connection);
            return 1;
        }
        catch (RuntimeException ex)
        {
            upstreamSystemMapper.updateConnectionStatus(connectionCode, connection.getStatus(), SecurityUtils.getUsername());
            throw lingxingClientFactory.toServiceException(ex);
        }
    }

    @Override
    public List<SourceProductItem> selectSourceProductList(SourceProductQuery query)
    {
        SourceProductQuery normalized = normalizeSourceProductQuery(query);
        List<SourceProductItem> list = upstreamSystemMapper.selectSourceProductList(normalized);
        fillSourceProductLabels(list);
        return list;
    }

    @Override
    public long countSourceProductList(SourceProductQuery query)
    {
        SourceProductQuery normalized = normalizeSourceProductQuery(query);
        return upstreamSystemMapper.countSourceProductList(normalized);
    }

    @Override
    public SourceProductGroupDetail selectSourceProductGroupDetail(SourceProductQuery query)
    {
        SourceProductQuery normalized = normalizeSourceProductQuery(query);
        normalized.setSourceSkuGroupKey(trimRequired(normalized.getSourceSkuGroupKey(), "来源SKU组不能为空"));
        SourceProductItem group = upstreamSystemMapper.selectSourceProductGroupSummary(normalized);
        if (group == null)
        {
            throw new ServiceException("来源SKU组不存在或已失效");
        }
        group.setSystemKindLabel(systemKindLabel(group.getSystemKind()));

        List<SourceProductItem> dimensionGroups = upstreamSystemMapper.selectSourceProductList(normalized);
        List<SourceProductItem> warehouses = upstreamSystemMapper.selectSourceProductWarehouseDetailList(normalized);
        fillSourceProductLabels(dimensionGroups);
        fillSourceProductLabels(warehouses);

        SourceProductGroupDetail detail = new SourceProductGroupDetail();
        detail.setGroup(group);
        detail.setDimensionGroups(dimensionGroups);
        detail.setWarehouses(warehouses);
        return detail;
    }

    @Override
    public List<SourceWarehouseStockItem> selectSourceWarehouseStockList(SourceWarehouseStockQuery query)
    {
        SourceWarehouseStockQuery normalized = normalizeSourceWarehouseStockQuery(query);
        List<SourceWarehouseStockItem> list = upstreamSystemMapper.selectSourceWarehouseStockList(normalized);
        fillSourceWarehouseStockLabels(list);
        return list;
    }

    @Override
    public long countSourceWarehouseStockGroupList(SourceWarehouseStockQuery query)
    {
        return upstreamSystemMapper.countSourceWarehouseStockGroupList(normalizeSourceWarehouseStockQuery(query));
    }

    @Override
    public List<SourceWarehouseStockGroupItem> selectSourceWarehouseStockGroupList(SourceWarehouseStockQuery query)
    {
        return upstreamSystemMapper.selectSourceWarehouseStockGroupList(normalizeSourceWarehouseStockQuery(query));
    }

    @Override
    public List<SourceWarehouseStockItem> selectSourceWarehouseStockGroupDetailList(SourceWarehouseStockQuery query)
    {
        SourceWarehouseStockQuery normalized = normalizeSourceWarehouseStockQuery(query);
        List<SourceWarehouseStockItem> list = upstreamSystemMapper.selectSourceWarehouseStockGroupDetailList(normalized);
        fillSourceWarehouseStockLabels(list);
        return list;
    }

    @Override
    public List<IntegrationOption> selectSourceWarehouseStockMasterWarehouseOptions(SourceWarehouseStockQuery query)
    {
        return upstreamSystemMapper.selectSourceWarehouseStockMasterWarehouseOptions(normalizeSourceWarehouseStockQuery(query));
    }

    @Override
    public List<IntegrationOption> selectSourceWarehouseStockUpstreamWarehouseOptions(SourceWarehouseStockQuery query)
    {
        return upstreamSystemMapper.selectSourceWarehouseStockUpstreamWarehouseOptions(normalizeSourceWarehouseStockQuery(query));
    }

    @Override
    public UpstreamInventorySyncState selectInventorySyncState(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        UpstreamInventorySyncState state = upstreamSystemMapper.selectInventorySyncState(connectionCode);
        if (state != null)
        {
            return state;
        }
        UpstreamInventorySyncState never = new UpstreamInventorySyncState();
        never.setConnectionCode(connectionCode);
        never.setStatus(UpstreamSystemConstants.SYNC_STATUS_NEVER);
        never.setTotalCount(0);
        never.setActiveCount(0);
        never.setMissingCount(0);
        return never;
    }

    @Override
    public List<UpstreamWarehouseSyncItem> selectWarehouseSyncList(String connectionCode, String status)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectWarehouseSyncList(connectionCode, trimOptional(status));
    }

    @Override
    public List<UpstreamWarehousePairing> selectWarehousePairingList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectWarehousePairingList(connectionCode);
    }

    @Override
    @Transactional
    public int insertWarehousePairing(String connectionCode, WarehousePairingRequest request)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        String pairingRole = normalizePairingRole(connection, request.getPairingRole());
        String upstreamWarehouseCode = trimRequired(request.getUpstreamWarehouseCode(), "领星仓库代码不能为空");
        UpstreamWarehouseSyncItem candidate = upstreamSystemMapper.selectWarehouseSyncItem(connectionCode, upstreamWarehouseCode);
        if (candidate == null)
        {
            throw new ServiceException("领星仓库不在同步清单中，请先同步仓库");
        }
        if (!UpstreamSystemConstants.STATUS_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("领星仓库不是可配对状态");
        }
        UpstreamWarehousePairing pairing = new UpstreamWarehousePairing();
        pairing.setConnectionCode(connectionCode);
        pairing.setUpstreamWarehouseCode(upstreamWarehouseCode);
        pairing.setUpstreamWarehouseName(candidate.getWarehouseName());
        pairing.setSystemWarehouseCode(trimRequired(request.getSystemWarehouseCode(), "系统仓库代码不能为空"));
        pairing.setSystemWarehouseName(trimRequired(request.getSystemWarehouseName(), "系统仓库名称不能为空"));
        pairing.setPairingRole(pairingRole);
        pairing.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
        pairing.setCreateBy(SecurityUtils.getUsername());
        pairing.setRemark(trimOptional(request.getRemark()));
        try
        {
            return upstreamSystemMapper.insertWarehousePairing(pairing);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("仓库配对重复：" + pairingRoleLabel(pairingRole) + "仓已经绑定，不能重复配对");
        }
    }

    @Override
    public int deleteWarehousePairing(String connectionCode, Long warehousePairingId)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.deleteWarehousePairing(connectionCode, warehousePairingId);
    }

    @Override
    public List<UpstreamLogisticsChannelSyncItem> selectLogisticsChannelSyncList(String connectionCode, String status)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectLogisticsChannelSyncList(connectionCode, trimOptional(status));
    }

    @Override
    public List<UpstreamLogisticsChannelPairing> selectLogisticsChannelPairingList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectLogisticsChannelPairingList(connectionCode);
    }

    @Override
    @Transactional
    public int insertLogisticsChannelPairing(String connectionCode, LogisticsChannelPairingRequest request)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        String pairingRole = normalizePairingRole(connection, request.getPairingRole());
        String upstreamWarehouseCode = trimRequired(request.getUpstreamWarehouseCode(), "领星仓库代码不能为空");
        String systemWarehouseCode = trimRequired(request.getSystemWarehouseCode(), "系统仓库代码不能为空");
        assertWarehousePairingExists(connectionCode, upstreamWarehouseCode, systemWarehouseCode, pairingRole);
        String upstreamChannelCode = trimRequired(request.getUpstreamChannelCode(), "领星渠道代码不能为空");
        UpstreamLogisticsChannelSyncItem candidate = upstreamSystemMapper.selectLogisticsChannelSyncItem(connectionCode,
            upstreamWarehouseCode, upstreamChannelCode);
        if (candidate == null)
        {
            throw new ServiceException("领星物流渠道不在同步清单中，请先同步物流渠道");
        }
        if (!UpstreamSystemConstants.STATUS_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("领星物流渠道不是可配对状态");
        }
        UpstreamLogisticsChannelPairing pairing = new UpstreamLogisticsChannelPairing();
        pairing.setConnectionCode(connectionCode);
        pairing.setSystemWarehouseCode(systemWarehouseCode);
        pairing.setUpstreamWarehouseCode(upstreamWarehouseCode);
        pairing.setUpstreamChannelCode(upstreamChannelCode);
        pairing.setUpstreamChannelName(candidate.getChannelName());
        pairing.setSystemChannelCode(trimRequired(request.getSystemChannelCode(), "系统渠道代码不能为空"));
        pairing.setSystemChannelName(trimRequired(request.getSystemChannelName(), "系统渠道名称不能为空"));
        pairing.setPairingRole(pairingRole);
        pairing.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
        pairing.setCreateBy(SecurityUtils.getUsername());
        pairing.setRemark(trimOptional(request.getRemark()));
        try
        {
            return upstreamSystemMapper.insertLogisticsChannelPairing(pairing);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("物流渠道配对重复：" + pairingRoleLabel(pairingRole) + "渠道已经绑定，不能重复配对");
        }
    }

    @Override
    public int deleteLogisticsChannelPairing(String connectionCode, Long logisticsChannelPairingId)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.deleteLogisticsChannelPairing(connectionCode, logisticsChannelPairingId);
    }

    @Override
    public List<UpstreamSkuSyncItem> selectSkuSyncList(String connectionCode, String status, String pairingStatus,
        String dimensionStatus, String field, String keyword)
    {
        return upstreamSystemMapper.selectSkuSyncList(connectionCode, trimOptional(status),
            trimOptional(pairingStatus), trimOptional(dimensionStatus), trimOptional(field), trimOptional(keyword));
    }

    @Override
    public List<UpstreamSkuPairing> selectSkuPairingList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectSkuPairingList(connectionCode);
    }

    @Override
    @Transactional
    public int insertSkuPairing(String connectionCode, SkuPairingRequest request)
    {
        selectConnectionByCode(connectionCode);
        String masterSku = trimRequired(request.getMasterSku(), "领星masterSku不能为空");
        UpstreamSkuSyncItem candidate = upstreamSystemMapper.selectSkuSyncItem(connectionCode, masterSku);
        if (candidate == null)
        {
            throw new ServiceException("领星SKU不在同步清单中，请先同步SKU");
        }
        UpstreamSkuPairing pairing = new UpstreamSkuPairing();
        pairing.setConnectionCode(connectionCode);
        pairing.setMasterSku(masterSku);
        pairing.setSystemSku(trimRequired(request.getSystemSku(), "系统SKU不能为空"));
        pairing.setSystemSkuName(trimRequired(request.getSystemSkuName(), "系统SKU名称不能为空"));
        pairing.setCustomerName(trimOptional(request.getCustomerName()));
        pairing.setCreateBy(SecurityUtils.getUsername());
        pairing.setRemark(trimOptional(request.getRemark()));
        try
        {
            int rows = upstreamSystemMapper.insertSkuPairing(pairing);
            insertSkuAudit("PAIR", pairing, null, pairing, request.getRemark());
            sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
            return rows;
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("SKU配对重复：系统SKU或领星masterSku已经配对");
        }
    }

    @Override
    @Transactional
    public int deleteSkuPairing(String connectionCode, Long skuPairingId)
    {
        selectConnectionByCode(connectionCode);
        UpstreamSkuPairing current = upstreamSystemMapper.selectSkuPairingById(connectionCode, skuPairingId);
        if (current == null)
        {
            return 0;
        }
        int rows = upstreamSystemMapper.deleteSkuPairing(connectionCode, skuPairingId);
        insertSkuAudit("UNPAIR", current, current, null, "解除SKU配对");
        sourceProductReadModelService.rebuildOfficialMasterByConnection(current.getConnectionCode());
        return rows;
    }

    @Override
    public UpstreamSkuSyncState selectSkuSyncState(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectSkuSyncState(connectionCode);
    }

    @Override
    public List<UpstreamSyncState> selectSyncStateList(String connectionCode)
    {
        selectConnectionByCode(connectionCode);
        return upstreamSystemMapper.selectSyncStateList(connectionCode);
    }

    @Override
    public List<UpstreamRequestLog> selectRequestLogList(String connectionCode)
    {
        return upstreamSystemMapper.selectRequestLogList(connectionCode);
    }

    private String normalizePairingRole(UpstreamSystemConnection connection, String requestedRole)
    {
        String expectedRole = pairingRoleForSettlement(connection.getSettlementType());
        String pairingRole = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(requestedRole).toUpperCase(), expectedRole);
        if (!UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT.equals(pairingRole)
            && !UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(pairingRole))
        {
            throw new ServiceException("配对用途不正确");
        }
        if (!expectedRole.equals(pairingRole))
        {
            throw new ServiceException("结算类型与配对用途不匹配：" + connection.getMasterWarehouseName()
                + "只能作为" + pairingRoleLabel(expectedRole) + "仓");
        }
        return pairingRole;
    }

    private String pairingRoleForSettlement(String settlementType)
    {
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(settlementType))
        {
            return UpstreamSystemConstants.PAIRING_ROLE_QUOTE;
        }
        return UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT;
    }

    private String pairingRoleLabel(String pairingRole)
    {
        return UpstreamSystemConstants.PAIRING_ROLE_QUOTE.equals(pairingRole) ? "报价" : "履约";
    }

    private void assertWarehousePairingExists(String connectionCode, String upstreamWarehouseCode,
        String systemWarehouseCode, String pairingRole)
    {
        boolean matched = upstreamSystemMapper.selectWarehousePairingList(connectionCode).stream()
            .anyMatch(item -> pairingRole.equals(item.getPairingRole())
                && UpstreamSystemConstants.STATUS_ACTIVE.equals(item.getStatus())
                && upstreamWarehouseCode.equals(item.getUpstreamWarehouseCode())
                && systemWarehouseCode.equals(item.getSystemWarehouseCode()));
        if (!matched)
        {
            throw new ServiceException("请先完成对应" + pairingRoleLabel(pairingRole) + "仓配对，再配对物流渠道");
        }
    }

    private SourceProductQuery normalizeSourceProductQuery(SourceProductQuery query)
    {
        SourceProductQuery normalized = query == null ? new SourceProductQuery() : query;
        normalized.setRepositoryScope(trimOptional(normalized.getRepositoryScope()));
        if (StringUtils.isBlank(normalized.getRepositoryScope()))
        {
            normalized.setRepositoryScope(SourceProductReadModelService.REPOSITORY_SCOPE_OFFICIAL_MASTER);
        }
        normalized.setSourceSkuGroupKey(trimOptional(normalized.getSourceSkuGroupKey()));
        normalized.setConnectionCode(trimOptional(normalized.getConnectionCode()));
        normalized.setSystemKind(normalizeSystemKindOptional(normalized.getSystemKind()));
        normalized.setMasterWarehouseName(trimOptional(normalized.getMasterWarehouseName()));
        normalized.setMasterSku(trimOptional(normalized.getMasterSku()));
        normalized.setProductName(trimOptional(normalized.getProductName()));
        normalized.setIdentifyCodeKeyword(trimOptional(normalized.getIdentifyCodeKeyword()));
        normalized.setCategoryKeyword(trimOptional(normalized.getCategoryKeyword()));
        normalized.setApproveStatus(trimOptional(normalized.getApproveStatus()));
        normalized.setStatus(trimOptional(normalized.getStatus()));
        normalized.setPairingStatus(trimOptional(normalized.getPairingStatus()));
        normalized.setKeyword(trimOptional(normalized.getKeyword()));
        return normalized;
    }

    private void fillSourceProductLabels(List<SourceProductItem> list)
    {
        for (SourceProductItem item : list)
        {
            item.setSystemKindLabel(systemKindLabel(item.getSystemKind()));
        }
    }

    private void fillSourceWarehouseStockLabels(List<SourceWarehouseStockItem> list)
    {
        for (SourceWarehouseStockItem item : list)
        {
            item.setSystemKindLabel(systemKindLabel(item.getSystemKind()));
        }
    }

    private SourceWarehouseStockQuery normalizeSourceWarehouseStockQuery(SourceWarehouseStockQuery query)
    {
        SourceWarehouseStockQuery normalized = query == null ? new SourceWarehouseStockQuery() : query;
        normalized.setSourceStockGroupKey(trimOptional(normalized.getSourceStockGroupKey()));
        normalized.setRepositoryScope(trimOptional(normalized.getRepositoryScope()));
        if (StringUtils.isBlank(normalized.getRepositoryScope()))
        {
            normalized.setRepositoryScope(SourceWarehouseStockReadModelService.REPOSITORY_SCOPE_OFFICIAL_MASTER);
        }
        normalized.setConnectionCode(trimOptional(normalized.getConnectionCode()));
        normalized.setKeyword(trimOptional(normalized.getKeyword()));
        normalized.setStatus(trimOptional(normalized.getStatus()));
        normalized.setWarehousePairingStatus(trimOptional(normalized.getWarehousePairingStatus()));
        normalized.setSkuPairingStatus(trimOptional(normalized.getSkuPairingStatus()));
        normalized.setInventoryScope(trimOptional(normalized.getInventoryScope()));
        normalized.setInventoryAttribute(trimOptional(normalized.getInventoryAttribute()));
        normalized.setMasterWarehouseName(trimOptional(normalized.getMasterWarehouseName()));
        normalized.setUpstreamWarehouseCode(trimOptional(normalized.getUpstreamWarehouseCode()));
        normalized.setMasterWarehouseKeyword(trimOptional(normalized.getMasterWarehouseKeyword()));
        normalized.setWarehouseKeyword(trimOptional(normalized.getWarehouseKeyword()));
        return normalized;
    }

    private String normalizeSystemKindOptional(String value)
    {
        String trimmed = trimOptional(value);
        if (StringUtils.isBlank(trimmed))
        {
            return null;
        }
        if (UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS_LEGACY.equals(trimmed))
        {
            return UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS;
        }
        return trimmed;
    }

    private String systemKindLabel(String systemKind)
    {
        String normalized = normalizeSystemKindOptional(systemKind);
        if (UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS.equals(normalized))
        {
            return "领星WMS";
        }
        return StringUtils.defaultIfBlank(systemKind, "未知来源");
    }

    private void checkCredentials(String connectionCode, String appKey, String appSecret)
    {
        lingxingClientFactory.checkWarehouseAccess(connectionCode, appKey, appSecret);
    }

    private void insertSkuAudit(String eventType, UpstreamSkuPairing reference, UpstreamSkuPairing before,
        UpstreamSkuPairing after, String remark)
    {
        UpstreamSkuPairingAuditEvent event = new UpstreamSkuPairingAuditEvent();
        event.setConnectionCode(reference.getConnectionCode());
        event.setMasterSku(reference.getMasterSku());
        event.setSystemSku(reference.getSystemSku());
        event.setEventType(eventType);
        event.setOperator(SecurityUtils.getUsername());
        event.setBeforeSnapshot(before == null ? "" : JSON.toJSONString(before));
        event.setAfterSnapshot(after == null ? "" : JSON.toJSONString(after));
        event.setRemark(trimOptional(remark));
        upstreamSystemMapper.insertSkuPairingAuditEvent(event);
    }

    private String generateConnectionCode(String masterWarehouseName)
    {
        String seed = StringUtils.defaultIfBlank(masterWarehouseName, "MASTER")
            .trim()
            .replaceAll("\\s+", "-")
            .replaceAll("[^0-9A-Za-z\\u4e00-\\u9fa5-]", "");
        if (seed.length() > 24)
        {
            seed = seed.substring(0, 24);
        }
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "LX-" + StringUtils.defaultIfBlank(seed, "MASTER") + "-" + suffix;
    }

    private String normalizeSystemKind(String value)
    {
        String trimmed = StringUtils.defaultIfBlank(value, UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS).trim();
        if (UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS.equals(trimmed)
            || UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS_LEGACY.equals(trimmed))
        {
            return UpstreamSystemConstants.SYSTEM_KIND_LINGXING_WMS;
        }
        throw new ServiceException("暂不支持的上游系统类型：" + trimmed);
    }

    private String normalizeSettlementType(String value)
    {
        String trimmed = trimRequired(value, "结算类型不能为空");
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE.equals(trimmed)
            || "UPSTREAM_PAYABLE".equals(trimmed))
        {
            return UpstreamSystemConstants.SETTLEMENT_TYPE_UPSTREAM_PAYABLE;
        }
        if (UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(trimmed)
            || "PLATFORM_ADVANCE".equals(trimmed))
        {
            return UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE;
        }
        throw new ServiceException("暂不支持的结算类型：" + trimmed);
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

    private String trimOptional(String value)
    {
        return StringUtils.trimToEmpty(value);
    }

}
