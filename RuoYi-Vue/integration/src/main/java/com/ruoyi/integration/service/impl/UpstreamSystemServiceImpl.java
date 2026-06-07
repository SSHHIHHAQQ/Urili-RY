package com.ruoyi.integration.service.impl;

import java.util.ArrayList;
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
import com.ruoyi.integration.domain.request.SkuDimensionSelectedSyncRequest;
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
        LingxingOpenApiClient client = createClient(connection);
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
            throw toServiceException(ex);
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

    private UpstreamSyncItemResult executeManualSyncItem(LingxingOpenApiClient client, String connectionCode,
        String syncType)
    {
        return executeSyncItem(client, connectionCode, syncType, UpstreamSystemConstants.SYNC_MODE_MANUAL);
    }

    private UpstreamSyncItemResult executeSyncItem(LingxingOpenApiClient client, String connectionCode,
        String syncType, String mode)
    {
        String syncBatchId = UUID.randomUUID().toString();
        Date startedTime = new Date();
        int rateLimitMs = UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType)
            ? SKU_DIMENSION_FULL_RATE_LIMIT_MS : 0;
        recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_SYNCING, syncBatchId,
            startedTime, null, null, null, 0, 0, 0, "", "", mode, rateLimitMs);
        recordLegacySyncingState(connectionCode, syncType, syncBatchId, startedTime);
        insertSyncBatch(syncBatchId, connectionCode, syncType, mode, startedTime);

        try
        {
            UpstreamSyncItemResult item;
            if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(syncType))
            {
                item = syncWarehouses(client, connectionCode, syncBatchId);
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(syncType))
            {
                item = syncLogisticsChannels(client, connectionCode, syncBatchId);
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
            {
                item = syncSkus(client, connectionCode, syncBatchId);
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
            {
                List<UpstreamSkuSyncItem> existingSkus = upstreamSystemMapper.selectSkuSyncList(connectionCode,
                    UpstreamSystemConstants.STATUS_ACTIVE, null, null, null, null);
                item = syncSkuDimensions(client, connectionCode, existingSkus, syncBatchId,
                    UpstreamSystemConstants.OP_SKU_DIMENSION_FULL_SYNC, SKU_DIMENSION_FULL_RATE_LIMIT_MS);
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
            {
                UpstreamInventorySyncState previousState = upstreamSystemMapper.selectInventorySyncState(connectionCode);
                int count = syncWarehouseStocks(client, connectionCode, syncBatchId, previousState, startedTime);
                item = newSyncItemResult(syncType, count);
            }
            else
            {
                throw new ServiceException("不支持的同步类型：" + syncType);
            }

            Date finishedTime = new Date();
            if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
            {
                sourceProductReadModelService.rebuildOfficialMaster();
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
            {
                sourceProductReadModelService.rebuildOfficialMasterByConnection(connectionCode);
            }
            else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
            {
                sourceWarehouseStockReadModelService.rebuildOfficialMasterByConnection(connectionCode);
            }
            recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_FRESH, syncBatchId,
                startedTime, finishedTime, finishedTime, nextSyncTime(syncType), item.getPulledCount(),
                item.getInsertedCount() + item.getChangedCount(), 0, "", "",
                mode, rateLimitMs);
            recordLegacySuccessState(connectionCode, syncType, syncBatchId, startedTime, finishedTime, item.getCount());
            updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FRESH, item, finishedTime, "");
            insertSyncExecutionLog(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
                "SUCCESS", item, "");
            return item;
        }
        catch (RuntimeException ex)
        {
            Date finishedTime = new Date();
            String message = StringUtils.left(ex.getMessage(), 500);
            recordSyncState(connectionCode, syncType, UpstreamSystemConstants.SYNC_STATUS_FAILED, syncBatchId,
                startedTime, finishedTime, null, nextSyncTime(syncType), 0, 0, 1, "", message, mode, rateLimitMs);
            recordLegacyFailedState(connectionCode, syncType, syncBatchId, startedTime, finishedTime, message);
            UpstreamSyncItemResult failed = new UpstreamSyncItemResult();
            failed.setSyncType(syncType);
            failed.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            failed.setErrorMessage(message);
            updateSyncBatch(syncBatchId, UpstreamSystemConstants.SYNC_STATUS_FAILED, failed, finishedTime, message);
            insertSyncExecutionLog(connectionCode, syncType, mode, syncBatchId, startedTime, finishedTime,
                "FAILURE", failed, message);
            throw toServiceException(ex);
        }
    }

    private UpstreamSyncItemResult syncWarehouses(LingxingOpenApiClient client, String connectionCode, String syncBatchId)
    {
        List<LingxingWarehouse> warehouses = client.listWarehouses(UUID.randomUUID().toString());
        List<UpstreamWarehouseSyncItem> items = new ArrayList<>();
        for (LingxingWarehouse warehouse : warehouses)
        {
            UpstreamWarehouseSyncItem item = new UpstreamWarehouseSyncItem();
            item.setConnectionCode(connectionCode);
            item.setWarehouseCode(warehouse.getWarehouseCode());
            item.setWarehouseName(warehouse.getWarehouseName());
            item.setCountryCode(warehouse.getCountryCode());
            item.setSourcePayloadJson(warehouse.getSourcePayloadJson());
            item.setSourcePayloadHash(warehouse.getSourcePayloadHash());
            item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
            item.setSyncBatchId(syncBatchId);
            items.add(item);
        }
        if (!items.isEmpty())
        {
            upstreamSystemMapper.batchInsertWarehouseStage(items);
        }
        int inserted = upstreamSystemMapper.insertNewWarehousesFromStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedWarehousesFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedWarehousesFromStage(connectionCode, syncBatchId);
        int disabled = upstreamSystemMapper.disableMissingWarehousesFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupWarehouseStage(connectionCode, syncBatchId);
        return newDiffSyncItemResult(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE, items.size(), inserted, changed,
            unchanged, disabled);
    }

    private UpstreamSyncItemResult syncLogisticsChannels(LingxingOpenApiClient client, String connectionCode,
        String syncBatchId)
    {
        List<UpstreamWarehouseSyncItem> warehouses = upstreamSystemMapper.selectWarehouseSyncList(connectionCode,
            UpstreamSystemConstants.STATUS_ACTIVE);
        if (warehouses == null || warehouses.isEmpty())
        {
            throw new ServiceException("请先同步仓库，再同步物流渠道");
        }
        List<UpstreamLogisticsChannelSyncItem> items = new ArrayList<>();
        for (UpstreamWarehouseSyncItem warehouse : warehouses)
        {
            List<LingxingLogisticsChannel> channels = client.listLogisticsChannels(warehouse.getWarehouseCode(),
                UUID.randomUUID().toString());
            for (LingxingLogisticsChannel channel : channels)
            {
                UpstreamLogisticsChannelSyncItem item = new UpstreamLogisticsChannelSyncItem();
                item.setConnectionCode(connectionCode);
                item.setWarehouseCode(channel.getWarehouseCode());
                item.setChannelCode(channel.getChannelCode());
                item.setChannelName(channel.getChannelName());
                item.setSourcePayloadJson(channel.getSourcePayloadJson());
                item.setSourcePayloadHash(channel.getSourcePayloadHash());
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSyncBatchId(syncBatchId);
                items.add(item);
            }
        }
        if (!items.isEmpty())
        {
            upstreamSystemMapper.batchInsertLogisticsChannelStage(items);
        }
        int inserted = upstreamSystemMapper.insertNewLogisticsChannelsFromStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedLogisticsChannelsFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedLogisticsChannelsFromStage(connectionCode, syncBatchId);
        int disabled = upstreamSystemMapper.disableMissingLogisticsChannelsFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupLogisticsChannelStage(connectionCode, syncBatchId);
        return newDiffSyncItemResult(UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, items.size(), inserted,
            changed, unchanged, disabled);
    }

    private UpstreamSyncItemResult syncSkus(LingxingOpenApiClient client, String connectionCode, String syncBatchId)
    {
        int current = 1;
        int total = Integer.MAX_VALUE;
        int synced = 0;
        while ((current - 1) * SKU_PAGE_SIZE < total)
        {
            LingxingProductPage page = client.listProductSkuPage(current, SKU_PAGE_SIZE, UUID.randomUUID().toString());
            total = page.getTotal();
            List<LingxingProductSku> records = page.getRecords();
            if (records == null || records.isEmpty())
            {
                break;
            }
            List<UpstreamSkuSyncItem> items = new ArrayList<>();
            for (LingxingProductSku sku : records)
            {
                UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
                item.setConnectionCode(connectionCode);
                item.setMasterSku(sku.getSku());
                item.setMasterProductName(sku.getProductName());
                copyLingxingSkuFields(item, sku);
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSearchText(buildSkuSearchText(sku));
                item.setSyncBatchId(syncBatchId);
                items.add(item);
                synced++;
            }
            upstreamSystemMapper.batchInsertSkuStage(items);
            current++;
        }
        int inserted = upstreamSystemMapper.insertNewSkusFromStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedSkusFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedSkusFromStage(connectionCode, syncBatchId);
        int disabled = upstreamSystemMapper.disableMissingSkusFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupSkuStage(connectionCode, syncBatchId);
        return newDiffSyncItemResult(UpstreamSystemConstants.SYNC_TYPE_SKU, synced, inserted, changed, unchanged,
            disabled);
    }

    private int syncSkuDimensions(LingxingOpenApiClient client, String connectionCode, List<UpstreamSkuSyncItem> skus)
    {
        return syncSkuDimensions(client, connectionCode, skus, UUID.randomUUID().toString(),
            UpstreamSystemConstants.OP_SKU_DIMENSION_FULL_SYNC, SKU_DIMENSION_FULL_RATE_LIMIT_MS).getCount();
    }

    private UpstreamSyncItemResult syncSkuDimensions(LingxingOpenApiClient client, String connectionCode,
        List<UpstreamSkuSyncItem> skus, String syncBatchId, String operation, int rateLimitMs)
    {
        if (skus == null || skus.isEmpty())
        {
            return newDiffSyncItemResult(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, 0, 0, 0, 0, 0);
        }
        int pulled = 0;
        int start = 0;
        while (start < skus.size())
        {
            int end = Math.min(start + SKU_DIMENSION_BATCH_SIZE, skus.size());
            List<String> skuCodes = new ArrayList<>();
            for (int i = start; i < end; i++)
            {
                String masterSku = skus.get(i).getMasterSku();
                if (StringUtils.isNotBlank(masterSku))
                {
                    skuCodes.add(masterSku);
                }
            }
            if (!skuCodes.isEmpty())
            {
                pulled += syncSkuDimensionBatch(client, connectionCode, syncBatchId, skuCodes, operation);
                sleepRateLimit(rateLimitMs);
            }
            start = end;
        }
        int inserted = upstreamSystemMapper.insertNewSkusFromDimensionStage(connectionCode, syncBatchId);
        int changed = upstreamSystemMapper.updateChangedSkuDimensionsFromStage(connectionCode, syncBatchId);
        int unchanged = upstreamSystemMapper.touchUnchangedSkuDimensionsFromStage(connectionCode, syncBatchId);
        upstreamSystemMapper.cleanupSkuDimensionStage(connectionCode, syncBatchId);
        return newDiffSyncItemResult(UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, pulled, inserted, changed,
            unchanged, 0);
    }

    private UpstreamSyncItemResult syncSkuDimensionsByCodes(LingxingOpenApiClient client, String connectionCode, List<String> skuCodes,
        String syncBatchId, String operation, int rateLimitMs)
    {
        List<UpstreamSkuSyncItem> items = new ArrayList<>();
        for (String skuCode : skuCodes)
        {
            UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
            item.setConnectionCode(connectionCode);
            item.setMasterSku(skuCode);
            items.add(item);
        }
        return syncSkuDimensions(client, connectionCode, items, syncBatchId, operation, rateLimitMs);
    }

    private int syncSkuDimensionBatch(LingxingOpenApiClient client, String connectionCode, String syncBatchId,
        List<String> skuCodes, String operation)
    {
        List<UpstreamSkuSyncItem> items = new ArrayList<>();
        LingxingProductPage page = client.listProductSkuPageBySkuList(skuCodes, operation, UUID.randomUUID().toString());
        List<LingxingProductSku> records = page.getRecords();
        if (records == null)
        {
            return 0;
        }
        for (LingxingProductSku sku : records)
        {
            if (!hasAnyWmsDimension(sku))
            {
                continue;
            }
            UpstreamSkuSyncItem item = new UpstreamSkuSyncItem();
            item.setConnectionCode(connectionCode);
            item.setMasterSku(sku.getSku());
            item.setMasterProductName(sku.getProductName());
            copyLingxingSkuFields(item, sku);
            item.setSyncBatchId(syncBatchId);
            items.add(item);
        }
        if (!items.isEmpty())
        {
            upstreamSystemMapper.batchInsertSkuDimensionStage(items);
        }
        return items.size();
    }

    private int syncWarehouseStocks(LingxingOpenApiClient client, String connectionCode, String syncBatchId,
        UpstreamInventorySyncState previousState, Date startedTime)
    {
        Map<String, UpstreamWarehousePairing> warehousePairingMap = buildWarehousePairingMap(connectionCode);
        Map<String, UpstreamWarehouseSyncItem> warehouseSyncMap = buildWarehouseSyncMap(connectionCode);
        Map<String, UpstreamSkuPairing> skuPairingMap = buildSkuPairingMap(connectionCode);
        String startTime = inventorySyncStartTime(previousState);
        String endTime = StringUtils.isBlank(startTime) ? "" : formatLingxingDateTime(startedTime);
        int current = 1;
        int total = Integer.MAX_VALUE;
        int synced = 0;
        while ((current - 1) * SKU_PAGE_SIZE < total)
        {
            LingxingInventoryProductPage page = client.listInventoryProductPage(current, SKU_PAGE_SIZE,
                startTime, endTime, UUID.randomUUID().toString());
            total = page.getTotal();
            List<LingxingInventoryProductStock> records = page.getRecords();
            if (records == null || records.isEmpty())
            {
                break;
            }
            for (LingxingInventoryProductStock stock : records)
            {
                SourceWarehouseStockItem item = new SourceWarehouseStockItem();
                item.setConnectionCode(connectionCode);
                copyLingxingInventoryStockFields(item, stock);
                UpstreamWarehouseSyncItem warehouse = warehouseSyncMap.get(item.getUpstreamWarehouseCode());
                if (StringUtils.isBlank(item.getUpstreamWarehouseName()) && warehouse != null)
                {
                    item.setUpstreamWarehouseName(warehouse.getWarehouseName());
                }
                UpstreamWarehousePairing warehousePairing = warehousePairingMap.get(item.getUpstreamWarehouseCode());
                if (warehousePairing != null)
                {
                    item.setSystemWarehouseCode(warehousePairing.getSystemWarehouseCode());
                    item.setSystemWarehouseName(warehousePairing.getSystemWarehouseName());
                }
                UpstreamSkuPairing skuPairing = skuPairingMap.get(item.getMasterSku());
                if (skuPairing != null)
                {
                    item.setSystemSku(skuPairing.getSystemSku());
                    item.setSystemSkuName(skuPairing.getSystemSkuName());
                    item.setCustomerName(skuPairing.getCustomerName());
                }
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSyncBatchId(syncBatchId);
                upstreamSystemMapper.upsertSourceWarehouseStock(item);
                synced++;
            }
            current++;
        }
        return synced;
    }

    private List<String> normalizeSyncTypes(List<String> syncTypes)
    {
        List<String> defaults = Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, UpstreamSystemConstants.SYNC_TYPE_SKU);
        List<String> source = syncTypes == null || syncTypes.isEmpty() ? defaults : syncTypes;
        List<String> ordered = Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, UpstreamSystemConstants.SYNC_TYPE_SKU,
            UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, UpstreamSystemConstants.SYNC_TYPE_INVENTORY);
        Set<String> requested = new HashSet<>();
        for (String syncType : source)
        {
            String normalized = StringUtils.trimToEmpty(syncType).toUpperCase();
            if (StringUtils.isNotBlank(normalized))
            {
                requested.add(normalized);
            }
        }
        List<String> result = new ArrayList<>();
        for (String syncType : ordered)
        {
            if (requested.contains(syncType))
            {
                result.add(syncType);
            }
        }
        return result;
    }

    private String normalizeSingleSyncType(String syncType)
    {
        String normalized = StringUtils.trimToEmpty(syncType).toUpperCase();
        List<String> allowed = Arrays.asList(UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE,
            UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL, UpstreamSystemConstants.SYNC_TYPE_SKU,
            UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION, UpstreamSystemConstants.SYNC_TYPE_INVENTORY);
        if (!allowed.contains(normalized))
        {
            throw new ServiceException("不支持的同步类型：" + syncType);
        }
        return normalized;
    }

    private List<String> normalizeSelectedSkuList(List<String> skuList)
    {
        if (skuList == null || skuList.isEmpty())
        {
            throw new ServiceException("请选择或输入需要获取尺寸重量的SKU");
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String sku : skuList)
        {
            String normalized = StringUtils.trimToEmpty(sku);
            if (StringUtils.isBlank(normalized))
            {
                continue;
            }
            if (seen.add(normalized))
            {
                result.add(normalized);
            }
            if (result.size() > SKU_DIMENSION_SELECTED_LIMIT)
            {
                throw new ServiceException("指定SKU一次最多支持" + SKU_DIMENSION_SELECTED_LIMIT + "个");
            }
        }
        if (result.isEmpty())
        {
            throw new ServiceException("请选择或输入需要获取尺寸重量的SKU");
        }
        return result;
    }

    private UpstreamSystemConnection selectEnabledConnection(String connectionCode)
    {
        UpstreamSystemConnection connection = selectConnectionByCode(connectionCode);
        if (!UpstreamSystemConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("主仓接入已停用，不能同步");
        }
        return connection;
    }

    private void acquireSyncLock(String connectionCode)
    {
        if (!syncingConnectionCodes.add(connectionCode))
        {
            throw new ServiceException("该主仓正在同步，请稍后再试");
        }
    }

    private void releaseSyncLock(String connectionCode)
    {
        syncingConnectionCodes.remove(connectionCode);
    }

    private void mergeSyncItemResult(UpstreamSyncResult result, UpstreamSyncItemResult item)
    {
        if (item == null)
        {
            return;
        }
        result.getItems().add(item);
        if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(item.getSyncType()))
        {
            result.setWarehouseCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(item.getSyncType()))
        {
            result.setLogisticsChannelCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(item.getSyncType()))
        {
            result.setSkuCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(item.getSyncType()))
        {
            result.setSkuDimensionCount(item.getCount());
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(item.getSyncType()))
        {
            result.setWarehouseStockCount(item.getCount());
        }
    }

    private UpstreamSyncItemResult newSyncItemResult(String syncType, int count)
    {
        return newDiffSyncItemResult(syncType, count, count, 0, 0, 0);
    }

    private UpstreamSyncItemResult newDiffSyncItemResult(String syncType, int pulledCount, int insertedCount,
        int changedCount, int unchangedCount, int disabledCount)
    {
        UpstreamSyncItemResult item = new UpstreamSyncItemResult();
        item.setSyncType(syncType);
        item.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
        item.setPulledCount(pulledCount);
        item.setInsertedCount(insertedCount);
        item.setChangedCount(changedCount);
        item.setUnchangedCount(unchangedCount);
        item.setDisabledCount(disabledCount);
        item.setCount(insertedCount + changedCount);
        return item;
    }

    private void recordSyncState(String connectionCode, String syncType, String status, String syncBatchId,
        Date startedTime, Date finishedTime, Date successTime, Date nextSyncTime, int totalCount, int successCount,
        int failedCount, String errorCode, String errorMessage, String mode, int rateLimitMs)
    {
        UpstreamSyncState state = new UpstreamSyncState();
        state.setConnectionCode(connectionCode);
        state.setSyncType(syncType);
        state.setStatus(status);
        state.setSyncBatchId(syncBatchId);
        state.setLastStartedTime(startedTime);
        state.setLastFinishedTime(finishedTime);
        state.setLastSuccessTime(successTime);
        state.setNextSyncTime(nextSyncTime);
        state.setTotalCount(totalCount);
        state.setSuccessCount(successCount);
        state.setFailedCount(failedCount);
        state.setLastErrorCode(errorCode);
        state.setLastErrorMessage(StringUtils.left(StringUtils.defaultString(errorMessage), 500));
        state.setLastMode(mode);
        state.setRateLimitMs(rateLimitMs);
        upstreamSystemMapper.upsertSyncState(state);
    }

    private void recordLegacySyncingState(String connectionCode, String syncType, String syncBatchId, Date startedTime)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType)
            || UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            UpstreamSkuSyncState state = new UpstreamSkuSyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            upstreamSystemMapper.upsertSkuSyncState(state);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState previous = upstreamSystemMapper.selectInventorySyncState(connectionCode);
            UpstreamInventorySyncState state = new UpstreamInventorySyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastSuccessTime(previous == null ? null : previous.getLastSuccessTime());
            state.setNextSyncTime(previous == null ? null : previous.getNextSyncTime());
            state.setTotalCount(previous == null ? 0 : previous.getTotalCount());
            state.setActiveCount(previous == null ? 0 : previous.getActiveCount());
            state.setMissingCount(previous == null ? 0 : previous.getMissingCount());
            state.setLastErrorMessage("");
            upstreamSystemMapper.upsertInventorySyncState(state);
        }
    }

    private void recordLegacySuccessState(String connectionCode, String syncType, String syncBatchId, Date startedTime,
        Date finishedTime, int count)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType)
            || UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            UpstreamSkuSyncState state = new UpstreamSkuSyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastSuccessTime(finishedTime);
            state.setNextSyncTime(nextSyncTime(syncType));
            state.setLastErrorMessage("");
            upstreamSystemMapper.upsertSkuSyncState(state);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState state = new UpstreamInventorySyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FRESH);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastSuccessTime(finishedTime);
            state.setNextSyncTime(nextSyncTime(syncType));
            state.setTotalCount(count);
            state.setActiveCount(count);
            state.setMissingCount(0);
            state.setLastErrorMessage("");
            upstreamSystemMapper.upsertInventorySyncState(state);
        }
    }

    private void recordLegacyFailedState(String connectionCode, String syncType, String syncBatchId, Date startedTime,
        Date finishedTime, String message)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType)
            || UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            UpstreamSkuSyncState state = new UpstreamSkuSyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastErrorMessage(StringUtils.left(message, 500));
            upstreamSystemMapper.upsertSkuSyncState(state);
        }
        else if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            UpstreamInventorySyncState previous = upstreamSystemMapper.selectInventorySyncState(connectionCode);
            UpstreamInventorySyncState state = new UpstreamInventorySyncState();
            state.setConnectionCode(connectionCode);
            state.setStatus(UpstreamSystemConstants.SYNC_STATUS_FAILED);
            state.setSyncBatchId(syncBatchId);
            state.setLastStartedTime(startedTime);
            state.setLastFinishedTime(finishedTime);
            state.setLastSuccessTime(previous == null ? null : previous.getLastSuccessTime());
            state.setNextSyncTime(previous == null ? null : previous.getNextSyncTime());
            state.setTotalCount(previous == null ? 0 : previous.getTotalCount());
            state.setActiveCount(previous == null ? 0 : previous.getActiveCount());
            state.setMissingCount(previous == null ? 0 : previous.getMissingCount());
            state.setLastErrorMessage(StringUtils.left(message, 500));
            upstreamSystemMapper.upsertInventorySyncState(state);
        }
    }

    private void insertSyncBatch(String syncBatchId, String connectionCode, String syncType, String mode,
        Date startedTime)
    {
        UpstreamSyncBatch batch = new UpstreamSyncBatch();
        batch.setSyncBatchId(syncBatchId);
        batch.setConnectionCode(connectionCode);
        batch.setSyncType(syncType);
        batch.setMode(mode);
        batch.setStatus(UpstreamSystemConstants.SYNC_STATUS_SYNCING);
        batch.setStartedTime(startedTime);
        upstreamSystemMapper.insertSyncBatch(batch);
    }

    private void updateSyncBatch(String syncBatchId, String status, UpstreamSyncItemResult item, Date finishedTime,
        String errorMessage)
    {
        UpstreamSyncBatch batch = new UpstreamSyncBatch();
        batch.setSyncBatchId(syncBatchId);
        batch.setStatus(status);
        batch.setPulledCount(item.getPulledCount());
        batch.setInsertedCount(item.getInsertedCount());
        batch.setChangedCount(item.getChangedCount());
        batch.setUnchangedCount(item.getUnchangedCount());
        batch.setDisabledCount(item.getDisabledCount());
        batch.setFailedCount(UpstreamSystemConstants.SYNC_STATUS_FAILED.equals(status) ? 1 : 0);
        batch.setFinishedTime(finishedTime);
        batch.setErrorMessage(errorMessage);
        upstreamSystemMapper.updateSyncBatch(batch);
    }

    private void insertSyncExecutionLog(String connectionCode, String syncType, String mode, String syncBatchId,
        Date startedTime, Date finishedTime, String status, UpstreamSyncItemResult item, String errorMessage)
    {
        UpstreamRequestLog log = new UpstreamRequestLog();
        log.setConnectionCode(connectionCode);
        log.setTraceId(syncBatchId);
        log.setOperation(syncExecutionOperation(syncType));
        log.setEndpoint("upstream-sync://" + StringUtils.defaultString(mode).toLowerCase() + "/" + syncType);
        log.setRequestTime(startedTime);
        log.setResponseTime(finishedTime);
        log.setDurationMs(Math.max(0L, finishedTime.getTime() - startedTime.getTime()));
        log.setRequestPayloadRedacted(JSON.toJSONString(syncExecutionRequestPayload(syncBatchId, syncType, mode)));
        log.setResponsePayloadRedacted(JSON.toJSONString(syncExecutionResponsePayload(status, item, errorMessage)));
        log.setExternalErrorCode("SUCCESS".equals(status) ? "" : syncType + "_SYNC_FAILED");
        log.setExternalErrorMessage(StringUtils.left(StringUtils.defaultString(errorMessage), 500));
        log.setStatus(status);
        upstreamSystemMapper.insertRequestLog(log);
    }

    private Map<String, Object> syncExecutionRequestPayload(String syncBatchId, String syncType, String mode)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("syncBatchId", syncBatchId);
        payload.put("syncType", syncType);
        payload.put("mode", mode);
        return payload;
    }

    private Map<String, Object> syncExecutionResponsePayload(String status, UpstreamSyncItemResult item,
        String errorMessage)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        if (item != null)
        {
            payload.put("pulledCount", item.getPulledCount());
            payload.put("insertedCount", item.getInsertedCount());
            payload.put("changedCount", item.getChangedCount());
            payload.put("unchangedCount", item.getUnchangedCount());
            payload.put("disabledCount", item.getDisabledCount());
            payload.put("count", item.getCount());
        }
        if (StringUtils.isNotBlank(errorMessage))
        {
            payload.put("errorMessage", StringUtils.left(errorMessage, 500));
        }
        return payload;
    }

    private String syncExecutionOperation(String syncType)
    {
        if (UpstreamSystemConstants.SYNC_TYPE_WAREHOUSE.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_WAREHOUSE_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_LOGISTICS_CHANNEL.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_LOGISTICS_CHANNEL_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_SKU_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_SKU_DIMENSION_SYNC;
        }
        if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            return UpstreamSystemConstants.OP_TASK_INVENTORY_SYNC;
        }
        return syncType + "_SYNC_TASK";
    }

    private Date nextSyncTime(String syncType)
    {
        long now = System.currentTimeMillis();
        if (UpstreamSystemConstants.SYNC_TYPE_INVENTORY.equals(syncType))
        {
            return new Date(now + TEN_MINUTES_MS);
        }
        if (UpstreamSystemConstants.SYNC_TYPE_SKU_DIMENSION.equals(syncType))
        {
            return nextDailyTime();
        }
        return new Date(now + ONE_DAY_MS);
    }

    private Date nextDailyTime()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis())
        {
            calendar.add(Calendar.DATE, 1);
        }
        return calendar.getTime();
    }

    private void sleepRateLimit(int rateLimitMs)
    {
        if (rateLimitMs <= 0)
        {
            return;
        }
        try
        {
            Thread.sleep(rateLimitMs);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new ServiceException("同步限速等待被中断");
        }
    }

    private String inventorySyncStartTime(UpstreamInventorySyncState previousState)
    {
        if (previousState == null || previousState.getLastSuccessTime() == null)
        {
            return "";
        }
        long startAt = Math.max(0L, previousState.getLastSuccessTime().getTime() - INVENTORY_SYNC_OVERLAP_MS);
        return formatLingxingDateTime(new Date(startAt));
    }

    private String formatLingxingDateTime(Date value)
    {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    private Map<String, UpstreamWarehousePairing> buildWarehousePairingMap(String connectionCode)
    {
        Map<String, UpstreamWarehousePairing> result = new HashMap<>();
        List<UpstreamWarehousePairing> pairings = upstreamSystemMapper.selectWarehousePairingList(connectionCode);
        for (UpstreamWarehousePairing pairing : pairings)
        {
            if (UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT.equals(pairing.getPairingRole())
                && UpstreamSystemConstants.STATUS_ACTIVE.equals(pairing.getStatus())
                && StringUtils.isNotBlank(pairing.getUpstreamWarehouseCode()))
            {
                result.put(pairing.getUpstreamWarehouseCode(), pairing);
            }
        }
        return result;
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

    private Map<String, UpstreamWarehouseSyncItem> buildWarehouseSyncMap(String connectionCode)
    {
        Map<String, UpstreamWarehouseSyncItem> result = new HashMap<>();
        List<UpstreamWarehouseSyncItem> warehouses = upstreamSystemMapper.selectWarehouseSyncList(connectionCode, null);
        for (UpstreamWarehouseSyncItem warehouse : warehouses)
        {
            if (StringUtils.isNotBlank(warehouse.getWarehouseCode()))
            {
                result.put(warehouse.getWarehouseCode(), warehouse);
            }
        }
        return result;
    }

    private Map<String, UpstreamSkuPairing> buildSkuPairingMap(String connectionCode)
    {
        Map<String, UpstreamSkuPairing> result = new HashMap<>();
        List<UpstreamSkuPairing> pairings = upstreamSystemMapper.selectSkuPairingList(connectionCode);
        for (UpstreamSkuPairing pairing : pairings)
        {
            if (StringUtils.isNotBlank(pairing.getMasterSku()))
            {
                result.put(pairing.getMasterSku(), pairing);
            }
        }
        return result;
    }

    private void copyLingxingInventoryStockFields(SourceWarehouseStockItem item, LingxingInventoryProductStock stock)
    {
        item.setUpstreamWarehouseCode(StringUtils.trimToEmpty(stock.getWarehouseCode()));
        item.setUpstreamWarehouseName(StringUtils.trimToEmpty(stock.getWarehouseName()));
        item.setMasterSku(StringUtils.trimToEmpty(stock.getSku()));
        item.setMasterProductName(StringUtils.trimToEmpty(stock.getProductName()));
        item.setInventoryScope(StringUtils.defaultIfBlank(StringUtils.trimToEmpty(stock.getInventoryScope()),
            "COMPREHENSIVE"));
        item.setInventoryAttribute(StringUtils.trimToEmpty(stock.getInventoryAttribute()));
        item.setBatchNo(StringUtils.trimToEmpty(stock.getBatchNo()));
        item.setLocationCode(StringUtils.trimToEmpty(stock.getLocationCode()));
        item.setTotalQuantity(zeroIfNull(stock.getTotalQuantity()));
        item.setAvailableQuantity(zeroIfNull(stock.getAvailableQuantity()));
        item.setLockedQuantity(zeroIfNull(stock.getLockedQuantity()));
        item.setInTransitQuantity(zeroIfNull(stock.getInTransitQuantity()));
        item.setBoxedQuantity(stock.getBoxedQuantity());
        item.setUnboxedQuantity(stock.getUnboxedQuantity());
        item.setSourcePayloadJson(stock.getSourcePayloadJson());
        item.setSourcePayloadHash(stock.getSourcePayloadHash());
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

    private void copyLingxingSkuFields(UpstreamSkuSyncItem item, LingxingProductSku sku)
    {
        item.setProductAliasName(sku.getProductAliasName());
        item.setApproveStatus(sku.getApproveStatus());
        item.setProductType(sku.getProductType());
        item.setProductDescription(sku.getProductDescription());
        item.setImageUrl(sku.getImageUrl());
        item.setMainCode(sku.getMainCode());
        item.setOtherCode(sku.getOtherCode());
        item.setFnsku(sku.getFnsku());
        item.setCountryOfOriginName(sku.getCountryOfOriginName());
        item.setCurrencyCode(sku.getCurrencyCode());
        item.setCustomhouseCode(sku.getCustomhouseCode());
        item.setDangerousCargo(sku.getDangerousCargo());
        item.setDeclareNameCn(sku.getDeclareNameCn());
        item.setDeclareNameEn(sku.getDeclareNameEn());
        item.setDeclarePrice(sku.getDeclarePrice());
        item.setHeight(sku.getHeight());
        item.setHeightBs(sku.getHeightBs());
        item.setLength(sku.getLength());
        item.setLengthBs(sku.getLengthBs());
        item.setWeight(sku.getWeight());
        item.setWeightBs(sku.getWeightBs());
        item.setWidth(sku.getWidth());
        item.setWidthBs(sku.getWidthBs());
        item.setWmsHeight(sku.getWmsHeight());
        item.setWmsHeightBs(sku.getWmsHeightBs());
        item.setWmsLength(sku.getWmsLength());
        item.setWmsLengthBs(sku.getWmsLengthBs());
        item.setWmsWeight(sku.getWmsWeight());
        item.setWmsWeightBs(sku.getWmsWeightBs());
        item.setWmsWidth(sku.getWmsWidth());
        item.setWmsWidthBs(sku.getWmsWidthBs());
        item.setCat1Name(sku.getCat1Name());
        item.setCat2Name(sku.getCat2Name());
        item.setCat3Name(sku.getCat3Name());
        item.setPlatformSkuInfoJson(sku.getPlatformSkuInfoJson());
        item.setBrazilTaxInfoJson(sku.getBrazilTaxInfoJson());
        item.setSourcePayloadJson(sku.getSourcePayloadJson());
        item.setSourcePayloadHash(sku.getSourcePayloadHash());
    }

    private String buildSkuSearchText(LingxingProductSku sku)
    {
        List<String> parts = new ArrayList<>();
        addSearchPart(parts, sku.getSku());
        addSearchPart(parts, sku.getProductName());
        addSearchPart(parts, sku.getProductAliasName());
        addSearchPart(parts, sku.getMainCode());
        addSearchPart(parts, sku.getOtherCode());
        addSearchPart(parts, sku.getFnsku());
        addSearchPart(parts, sku.getDeclareNameCn());
        addSearchPart(parts, sku.getDeclareNameEn());
        addSearchPart(parts, sku.getCustomhouseCode());
        addSearchPart(parts, sku.getCountryOfOriginName());
        addSearchPart(parts, sku.getCat1Name());
        addSearchPart(parts, sku.getCat2Name());
        addSearchPart(parts, sku.getCat3Name());
        return String.join(" ", parts);
    }

    private boolean hasAnyWmsDimension(LingxingProductSku sku)
    {
        return sku.getWmsHeight() != null || sku.getWmsLength() != null || sku.getWmsWidth() != null
            || sku.getWmsWeight() != null || sku.getWmsHeightBs() != null || sku.getWmsLengthBs() != null
            || sku.getWmsWidthBs() != null || sku.getWmsWeightBs() != null;
    }

    private void addSearchPart(List<String> parts, String value)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isNotBlank(trimmed))
        {
            parts.add(trimmed);
        }
    }

    private void checkCredentials(String connectionCode, String appKey, String appSecret)
    {
        LingxingOpenApiClient client = new LingxingOpenApiClient(new LingxingCredentials(appKey, appSecret),
            entry -> insertRequestLog(connectionCode, entry));
        try
        {
            client.checkWarehouseAccess(UUID.randomUUID().toString());
        }
        catch (RuntimeException ex)
        {
            throw toServiceException(ex);
        }
    }

    private LingxingOpenApiClient createClient(UpstreamSystemConnection connection)
    {
        String appKey = secretCipherSupport.decrypt(connection.getAppKeyCiphertext());
        String appSecret = secretCipherSupport.decrypt(connection.getAppSecretCiphertext());
        return new LingxingOpenApiClient(new LingxingCredentials(appKey, appSecret),
            entry -> insertRequestLog(connection.getConnectionCode(), entry));
    }

    private void insertRequestLog(String connectionCode, LingxingRequestLogEntry entry)
    {
        UpstreamRequestLog log = new UpstreamRequestLog();
        log.setConnectionCode(connectionCode);
        log.setTraceId(entry.getTraceId());
        log.setOperation(entry.getOperation());
        log.setEndpoint(entry.getEndpoint());
        log.setRequestTime(entry.getRequestTime());
        log.setResponseTime(entry.getResponseTime());
        log.setDurationMs(entry.getDurationMs());
        log.setRequestPayloadRedacted(entry.getRequestPayloadRedacted());
        log.setResponsePayloadRedacted(entry.getResponsePayloadRedacted());
        log.setExternalErrorCode(entry.getExternalErrorCode());
        log.setExternalErrorMessage(entry.getExternalErrorMessage());
        log.setStatus(entry.getStatus());
        upstreamSystemMapper.insertRequestLog(log);
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

    private ServiceException toServiceException(RuntimeException ex)
    {
        if (ex instanceof ServiceException serviceException)
        {
            return serviceException;
        }
        if (ex instanceof LingxingClientException clientException)
        {
            return new ServiceException("领星接口调用失败：" + clientException.getMessage());
        }
        return new ServiceException(StringUtils.defaultIfBlank(ex.getMessage(), "上游系统处理失败"));
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

    private Long zeroIfNull(Long value)
    {
        return value == null ? 0L : value;
    }
}
