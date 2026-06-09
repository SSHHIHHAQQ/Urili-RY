package com.ruoyi.inventory.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.inventory.domain.InventoryProductSkuSnapshot;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.InventoryStockSyncPolicy;
import com.ruoyi.inventory.domain.InventoryStockSyncPolicyPreviewResult;
import com.ruoyi.inventory.domain.InventoryStockSyncPolicyPreviewRow;
import com.ruoyi.inventory.domain.request.InventoryStockSyncPolicyRequest;
import com.ruoyi.inventory.mapper.InventoryOverviewMapper;
import com.ruoyi.inventory.mapper.InventoryStockSyncPolicyMapper;
import com.ruoyi.inventory.service.IInventoryStockSyncPolicyService;
import com.ruoyi.inventory.service.InventoryProductLookupService;

/**
 * 库存同步策略服务实现。
 */
@Service
public class InventoryStockSyncPolicyServiceImpl implements IInventoryStockSyncPolicyService
{
    private static final String SCOPE_SELLER = "SELLER";
    private static final String SCOPE_WAREHOUSE = "WAREHOUSE";
    private static final String SCOPE_SPU = "SPU";
    private static final String SCOPE_SKU = "SKU";
    private static final String SCOPE_SKU_WAREHOUSE = "SKU_WAREHOUSE";
    private static final String SCOPE_SYSTEM = "SYSTEM";
    private static final String SYNC_MODE_MANUAL = "MANUAL";
    private static final String SYNC_MODE_AUTO_SOURCE_AVAILABLE = "AUTO_SOURCE_AVAILABLE";
    private static final String WAREHOUSE_OFFICIAL = "official";
    private static final String REF_OFFICIAL_MASTER = "OFFICIAL_MASTER";
    private static final String REF_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String REF_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_IN_STOCK = "IN_STOCK";
    private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String STATUS_NO_SOURCE = "NO_SOURCE";
    private static final String STATUS_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String STATUS_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_SOURCE_ONLY_IN_TRANSIT = "SOURCE_ONLY_IN_TRANSIT";
    private static final String SYNC_STATUS_NORMAL = "NORMAL";
    private static final String SYNC_STATUS_UNSUPPORTED = "UNSUPPORTED";
    private static final String SYNC_STATUS_SOURCE_INSUFFICIENT = "SOURCE_INSUFFICIENT";

    @Autowired
    private InventoryOverviewMapper inventoryOverviewMapper;

    @Autowired
    private InventoryStockSyncPolicyMapper inventoryStockSyncPolicyMapper;

    @Autowired
    private ObjectProvider<InventoryProductLookupService> productLookupService;

    @Override
    public InventoryStockSyncPolicyPreviewResult previewSyncPolicy(InventoryStockSyncPolicyRequest request)
    {
        List<SyncPolicyContext> contexts = buildContexts(request, true);
        return mergePreview(contexts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryStockSyncPolicyPreviewResult confirmSyncPolicy(InventoryStockSyncPolicyRequest request)
    {
        List<SyncPolicyContext> contexts = buildContexts(request, true);
        InventoryStockSyncPolicyPreviewResult preview = mergePreview(contexts);
        if (!Boolean.TRUE.equals(preview.getAllowed()))
        {
            throw new ServiceException(preview.getMessage());
        }
        if (!Boolean.TRUE.equals(request.getConfirmed()))
        {
            throw new ServiceException("请先预览并确认库存同步方式变更");
        }

        String operator = currentOperator();
        List<SyncPolicyContext> applyContexts = new ArrayList<>();
        for (SyncPolicyContext context : contexts)
        {
            InventoryStockSyncPolicy policy = context.getCandidatePolicy();
            policy.setCreateBy(operator);
            policy.setUpdateBy(operator);
            inventoryStockSyncPolicyMapper.upsertPolicy(policy);
            InventoryStockSyncPolicy savedPolicy = inventoryStockSyncPolicyMapper.selectPolicyByKey(policy.getPolicyKey());
            if (savedPolicy == null)
            {
                throw new ServiceException("库存同步策略保存失败，请刷新后重试");
            }
            applyContexts.add(buildContext(context.getRequest(), false, savedPolicy));
        }
        for (SyncPolicyContext applyContext : applyContexts)
        {
            applyRows(applyContext, operator, "ADMIN", "INVENTORY_SYNC_POLICY_APPLY");
        }
        return mergePreview(applyContexts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAutoSyncForSpu(Long spuId, String operator)
    {
        if (spuId == null)
        {
            return;
        }
        List<InventorySkuWarehouseStock> rows = inventoryOverviewMapper.selectWarehouseStockListBySpuId(spuId);
        if (rows.isEmpty())
        {
            return;
        }
        List<InventoryStockSyncPolicy> policies = inventoryStockSyncPolicyMapper.selectEnabledPoliciesBySpuId(spuId);
        SyncPolicyContext context = buildContextForExistingPolicies(rows, policies);
        applyRows(context, operator == null || operator.trim().isEmpty() ? "system" : operator,
            "SYSTEM_SYNC", "SOURCE_INVENTORY_AUTO_SYNC");
    }

    private SyncPolicyContext buildContext(InventoryStockSyncPolicyRequest request, boolean includeCandidate)
    {
        return buildContext(request, includeCandidate, null);
    }

    private List<SyncPolicyContext> buildContexts(InventoryStockSyncPolicyRequest request, boolean includeCandidate)
    {
        InventoryStockSyncPolicyRequest normalizedRequest = normalizeRequest(request);
        List<InventoryStockSyncPolicyRequest> requests = expandWarehouseRequests(normalizedRequest);
        List<SyncPolicyContext> contexts = new ArrayList<>();
        for (InventoryStockSyncPolicyRequest expandedRequest : requests)
        {
            contexts.add(buildContext(expandedRequest, includeCandidate));
        }
        return contexts;
    }

    private SyncPolicyContext buildContext(InventoryStockSyncPolicyRequest request, boolean includeCandidate,
        InventoryStockSyncPolicy savedCandidate)
    {
        InventoryStockSyncPolicyRequest normalizedRequest = normalizeRequest(request);
        List<InventorySkuWarehouseStock> rows = selectAffectedRows(normalizedRequest);
        if (rows.isEmpty())
        {
            InventoryStockSyncPolicyPreviewResult empty = new InventoryStockSyncPolicyPreviewResult();
            empty.setAllowed(false);
            empty.setConfirmationRequired(false);
            empty.setScopeType(normalizedRequest.getScopeType());
            empty.setSyncMode(normalizedRequest.getSyncMode());
            empty.setMessage("当前设置范围下没有库存明细行");
            empty.setAffectedRowCount(0L);
            empty.setEligibleRowCount(0L);
            empty.setSkippedRowCount(0L);
            empty.setChangedRowCount(0L);
            return new SyncPolicyContext(normalizedRequest, null, rows, new ArrayList<>(), empty);
        }

        fillRequestTargetSnapshots(normalizedRequest, rows);
        InventoryStockSyncPolicy candidate = savedCandidate == null
            ? buildPolicy(normalizedRequest, null)
            : savedCandidate;
        List<InventoryStockSyncPolicy> policies = selectPoliciesForRows(rows);
        if (includeCandidate || savedCandidate != null)
        {
            policies = mergeCandidatePolicy(policies, candidate);
        }
        InventoryStockSyncPolicyPreviewResult preview = buildPreview(normalizedRequest, candidate, rows, policies);
        return new SyncPolicyContext(normalizedRequest, candidate, rows, policies, preview);
    }

    private SyncPolicyContext buildContextForExistingPolicies(List<InventorySkuWarehouseStock> rows,
        List<InventoryStockSyncPolicy> policies)
    {
        InventoryStockSyncPolicyPreviewResult preview = buildPreview(null, null, rows, policies);
        return new SyncPolicyContext(null, null, rows, policies, preview);
    }

    private InventoryStockSyncPolicyPreviewResult mergePreview(List<SyncPolicyContext> contexts)
    {
        InventoryStockSyncPolicyPreviewResult merged = new InventoryStockSyncPolicyPreviewResult();
        merged.setAllowed(true);
        merged.setConfirmationRequired(true);
        merged.setAffectedRowCount(0L);
        merged.setEligibleRowCount(0L);
        merged.setSkippedRowCount(0L);
        merged.setChangedRowCount(0L);
        merged.setBeforePlatformTotalQty(0L);
        merged.setAfterPlatformTotalQty(0L);
        merged.setBeforeAvailableQty(0L);
        merged.setAfterAvailableQty(0L);

        List<String> blockedMessages = new ArrayList<>();
        for (SyncPolicyContext context : contexts)
        {
            InventoryStockSyncPolicyPreviewResult preview = context.getPreview();
            if (!Boolean.TRUE.equals(preview.getAllowed()))
            {
                merged.setAllowed(false);
                blockedMessages.add(preview.getMessage());
            }
            merged.setScopeType(preview.getScopeType());
            merged.setSyncMode(preview.getSyncMode());
            merged.setAffectedRowCount(qty(merged.getAffectedRowCount()) + qty(preview.getAffectedRowCount()));
            merged.setEligibleRowCount(qty(merged.getEligibleRowCount()) + qty(preview.getEligibleRowCount()));
            merged.setSkippedRowCount(qty(merged.getSkippedRowCount()) + qty(preview.getSkippedRowCount()));
            merged.setChangedRowCount(qty(merged.getChangedRowCount()) + qty(preview.getChangedRowCount()));
            merged.setBeforePlatformTotalQty(qty(merged.getBeforePlatformTotalQty()) + qty(preview.getBeforePlatformTotalQty()));
            merged.setAfterPlatformTotalQty(qty(merged.getAfterPlatformTotalQty()) + qty(preview.getAfterPlatformTotalQty()));
            merged.setBeforeAvailableQty(qty(merged.getBeforeAvailableQty()) + qty(preview.getBeforeAvailableQty()));
            merged.setAfterAvailableQty(qty(merged.getAfterAvailableQty()) + qty(preview.getAfterAvailableQty()));
            merged.getRows().addAll(preview.getRows());
        }

        if (!Boolean.TRUE.equals(merged.getAllowed()))
        {
            merged.setConfirmationRequired(false);
            merged.setMessage(blockedMessages.isEmpty() ? "当前设置不能保存" : String.join("；", blockedMessages));
        }
        else
        {
            merged.setMessage("本次预计更新" + merged.getChangedRowCount() + "条库存明细，可同步"
                + merged.getEligibleRowCount() + "条，跳过" + merged.getSkippedRowCount() + "条");
        }
        return merged;
    }

    private InventoryStockSyncPolicyPreviewResult buildPreview(InventoryStockSyncPolicyRequest request,
        InventoryStockSyncPolicy candidate, List<InventorySkuWarehouseStock> rows, List<InventoryStockSyncPolicy> policies)
    {
        InventoryStockSyncPolicyPreviewResult result = new InventoryStockSyncPolicyPreviewResult();
        result.setAllowed(true);
        result.setConfirmationRequired(candidate != null);
        result.setScopeType(candidate == null ? null : candidate.getScopeType());
        result.setSyncMode(candidate == null ? null : candidate.getSyncMode());

        long affected = 0;
        long eligible = 0;
        long skipped = 0;
        long changed = 0;
        long beforeTotal = 0;
        long afterTotal = 0;
        long beforeAvailable = 0;
        long afterAvailable = 0;
        boolean exactUnsupported = false;

        for (InventorySkuWarehouseStock row : rows)
        {
            InventoryStockSyncPolicy resolved = resolvePolicy(row, policies);
            boolean candidateEffective = candidate == null
                || (resolved != null && Objects.equals(resolved.getPolicyKey(), candidate.getPolicyKey()));
            StockSimulation simulation = simulate(row, resolved, candidateEffective);
            InventoryStockSyncPolicyPreviewRow previewRow = buildPreviewRow(row, resolved, simulation);
            result.getRows().add(previewRow);

            affected++;
            beforeTotal += qty(row.getPlatformTotalQty());
            afterTotal += qty(previewRow.getAfterPlatformTotalQty());
            beforeAvailable += qty(row.getPlatformAvailableQty());
            afterAvailable += qty(previewRow.getAfterAvailableQty());
            if (Boolean.TRUE.equals(previewRow.getChanged()))
            {
                changed++;
            }
            if (Boolean.TRUE.equals(previewRow.getEligible()))
            {
                eligible++;
            }
            else
            {
                skipped++;
                if (candidate != null && SCOPE_SKU_WAREHOUSE.equals(candidate.getScopeType()) && candidateEffective)
                {
                    exactUnsupported = true;
                }
            }
        }

        result.setAffectedRowCount(affected);
        result.setEligibleRowCount(eligible);
        result.setSkippedRowCount(skipped);
        result.setChangedRowCount(changed);
        result.setBeforePlatformTotalQty(beforeTotal);
        result.setAfterPlatformTotalQty(afterTotal);
        result.setBeforeAvailableQty(beforeAvailable);
        result.setAfterAvailableQty(afterAvailable);

        if (candidate != null && exactUnsupported)
        {
            result.setAllowed(false);
            result.setConfirmationRequired(false);
            result.setMessage("当前明细行不支持自动同步WMS库存，不能保存");
        }
        else if (candidate != null && changed == 0)
        {
            result.setAllowed(false);
            result.setConfirmationRequired(false);
            result.setMessage("当前设置不会影响任何库存明细");
        }
        else
        {
            result.setAllowed(true);
            result.setMessage(buildPreviewMessage(candidate, eligible, skipped, changed));
        }
        return result;
    }

    private InventoryStockSyncPolicyPreviewRow buildPreviewRow(InventorySkuWarehouseStock row,
        InventoryStockSyncPolicy resolved, StockSimulation simulation)
    {
        InventoryStockSyncPolicyPreviewRow preview = new InventoryStockSyncPolicyPreviewRow();
        preview.setStockId(row.getStockId());
        preview.setSpuId(row.getSpuId());
        preview.setSkuId(row.getSkuId());
        preview.setSellerId(row.getSellerId());
        preview.setSellerName(row.getSellerName());
        preview.setSystemSkuCode(row.getSystemSkuCode());
        preview.setProductName(row.getProductName());
        preview.setSkuName(row.getSkuName());
        preview.setWarehouseName(row.getWarehouseName());
        preview.setWarehouseKind(row.getWarehouseKind());
        preview.setWarehouseRefType(row.getWarehouseRefType());
        preview.setBeforeSyncMode(defaultString(row.getSyncMode(), SYNC_MODE_MANUAL));
        preview.setAfterSyncMode(simulation.getSyncMode());
        preview.setBeforePolicyScope(defaultString(row.getSyncPolicyScope(), SCOPE_SYSTEM));
        preview.setAfterPolicyScope(resolved == null ? SCOPE_SYSTEM : resolved.getScopeType());
        preview.setSourceAvailableQty(qty(row.getSourceAvailableQty()));
        preview.setPendingSourceDeductionQty(qty(row.getPendingSourceDeductionQty()));
        preview.setBeforePlatformTotalQty(qty(row.getPlatformTotalQty()));
        preview.setAfterPlatformTotalQty(simulation.getPlatformTotalQty());
        preview.setPlatformTotalDeltaQty(simulation.getPlatformTotalQty() - qty(row.getPlatformTotalQty()));
        preview.setBeforeAvailableQty(qty(row.getPlatformAvailableQty()));
        preview.setAfterAvailableQty(simulation.getPlatformAvailableQty());
        preview.setReservedQty(qty(row.getPlatformReservedQty()));
        preview.setSyncStatus(simulation.getSyncStatus());
        preview.setEligible(simulation.isEligible());
        preview.setChanged(simulation.isChanged());
        preview.setMessage(simulation.getMessage());
        return preview;
    }

    private String buildPreviewMessage(InventoryStockSyncPolicy candidate, long eligible, long skipped, long changed)
    {
        if (candidate == null)
        {
            return "来源库存刷新已按当前策略重新计算库存";
        }
        String modeLabel = SYNC_MODE_AUTO_SOURCE_AVAILABLE.equals(candidate.getSyncMode())
            ? "自动同步WMS库存"
            : "手动设置平台库存";
        StringBuilder message = new StringBuilder();
        message.append("本次将把同步方式设置为").append(modeLabel)
            .append("，预计更新").append(changed).append("条库存明细");
        if (eligible > 0)
        {
            message.append("，其中").append(eligible).append("条可自动同步库存");
        }
        if (skipped > 0)
        {
            message.append("，").append(skipped).append("条不支持或被更高优先级设置覆盖");
        }
        return message.toString();
    }

    private void applyRows(SyncPolicyContext context, String operator, String operationSource, String bizType)
    {
        Map<Long, InventoryStockSyncPolicyPreviewRow> previewRows = new LinkedHashMap<>();
        for (InventoryStockSyncPolicyPreviewRow row : context.getPreview().getRows())
        {
            previewRows.put(row.getStockId(), row);
        }

        Set<Long> skuIds = new LinkedHashSet<>();
        Set<Long> spuIds = new LinkedHashSet<>();
        for (InventorySkuWarehouseStock before : context.getRows())
        {
            InventoryStockSyncPolicyPreviewRow preview = previewRows.get(before.getStockId());
            if (preview == null || !Boolean.TRUE.equals(preview.getChanged()))
            {
                continue;
            }

            InventorySkuWarehouseStock after = copyStock(before);
            InventoryStockSyncPolicy resolved = resolvePolicy(before, context.getPolicies());
            applySimulation(after, resolved, preview, operator);
            int updatedRows = inventoryOverviewMapper.updateWarehouseStock(after);
            if (updatedRows != 1)
            {
                throw new ServiceException("库存数据已变更，请刷新后重试");
            }
            if (!Objects.equals(qty(before.getPlatformTotalQty()), qty(after.getPlatformTotalQty())))
            {
                inventoryOverviewMapper.insertLedger(buildAutoSyncLedger(before, after, preview, operator,
                    operationSource, bizType));
            }
            skuIds.add(after.getSkuId());
            spuIds.add(after.getSpuId());
        }

        for (Long skuId : skuIds)
        {
            refreshSkuReadModel(skuId);
        }
        for (Long spuId : spuIds)
        {
            refreshSpuReadModel(spuId);
        }
    }

    private void refreshSkuReadModel(Long skuId)
    {
        if (skuId == null)
        {
            return;
        }
        List<InventoryProductSkuSnapshot> skuSnapshots = emptyIfNull(
            requireProductLookupService().selectSkuSnapshotsBySkuIds(Collections.singletonList(skuId)));
        inventoryOverviewMapper.refreshSkuReadModel(skuId, skuSnapshots);
    }

    private void refreshSpuReadModel(Long spuId)
    {
        if (spuId == null)
        {
            return;
        }
        List<InventoryProductSkuSnapshot> skuSnapshots =
            emptyIfNull(requireProductLookupService().selectSkuSnapshotsBySpuId(spuId));
        inventoryOverviewMapper.refreshSpuReadModel(spuId, skuSnapshots);
    }

    private InventoryProductLookupService requireProductLookupService()
    {
        InventoryProductLookupService lookupService = productLookupService.getIfAvailable();
        if (lookupService == null)
        {
            throw new ServiceException("商品库存查询服务不可用");
        }
        return lookupService;
    }

    private <T> List<T> emptyIfNull(List<T> rows)
    {
        return rows == null ? new ArrayList<>() : rows;
    }

    private void applySimulation(InventorySkuWarehouseStock stock, InventoryStockSyncPolicy policy,
        InventoryStockSyncPolicyPreviewRow preview, String operator)
    {
        stock.setPlatformTotalQty(preview.getAfterPlatformTotalQty());
        stock.setSyncMode(preview.getAfterSyncMode());
        stock.setSyncPolicyId(policy == null ? null : policy.getPolicyId());
        stock.setSyncPolicyScope(policy == null ? SCOPE_SYSTEM : policy.getScopeType());
        stock.setSyncPolicyKey(policy == null ? "" : policy.getPolicyKey());
        stock.setSyncStatus(preview.getSyncStatus());
        if (SYNC_MODE_AUTO_SOURCE_AVAILABLE.equals(preview.getAfterSyncMode()) && Boolean.TRUE.equals(preview.getEligible()))
        {
            stock.setLastAutoSyncTime(new Date());
        }
        recalculate(stock);
        stock.setUpdateBy(operator);
    }

    private StockSimulation simulate(InventorySkuWarehouseStock row, InventoryStockSyncPolicy policy,
        boolean candidateEffective)
    {
        String currentMode = defaultString(row.getSyncMode(), SYNC_MODE_MANUAL);
        String currentScope = defaultString(row.getSyncPolicyScope(), SCOPE_SYSTEM);
        String currentKey = defaultString(row.getSyncPolicyKey(), "");

        if (!candidateEffective)
        {
            return new StockSimulation(false, false, currentMode, qty(row.getPlatformTotalQty()),
                qty(row.getPlatformAvailableQty()), defaultString(row.getSyncStatus(), SYNC_STATUS_NORMAL),
                "已有更高优先级设置，本次不会覆盖");
        }

        String targetMode = policy == null ? SYNC_MODE_MANUAL : policy.getSyncMode();
        String targetScope = policy == null ? SCOPE_SYSTEM : policy.getScopeType();
        String targetKey = policy == null ? "" : policy.getPolicyKey();
        long targetTotal = qty(row.getPlatformTotalQty());
        String syncStatus = SYNC_STATUS_NORMAL;
        boolean eligible = true;
        String message;

        if (SYNC_MODE_AUTO_SOURCE_AVAILABLE.equals(targetMode))
        {
            eligible = supportsAutoSync(row);
            if (eligible)
            {
                long effectiveSourceAvailable = effectiveSourceAvailable(row);
                targetTotal = Math.max(effectiveSourceAvailable, qty(row.getPlatformReservedQty()));
                syncStatus = effectiveSourceAvailable < qty(row.getPlatformReservedQty())
                    ? SYNC_STATUS_SOURCE_INSUFFICIENT
                    : SYNC_STATUS_NORMAL;
                message = "将按来源可用库存同步平台总库存";
            }
            else
            {
                syncStatus = SYNC_STATUS_UNSUPPORTED;
                message = autoUnsupportedReason(row);
            }
        }
        else
        {
            message = "切换为手动设置，保留当前平台总库存";
        }

        InventorySkuWarehouseStock simulated = copyStock(row);
        simulated.setPlatformTotalQty(targetTotal);
        simulated.setSyncMode(targetMode);
        simulated.setSyncPolicyScope(targetScope);
        simulated.setSyncPolicyKey(targetKey);
        simulated.setSyncStatus(syncStatus);
        recalculate(simulated);

        boolean changed = !Objects.equals(currentMode, targetMode)
            || !Objects.equals(currentScope, targetScope)
            || !Objects.equals(currentKey, targetKey)
            || !Objects.equals(defaultString(row.getSyncStatus(), SYNC_STATUS_NORMAL), syncStatus)
            || qty(row.getPlatformTotalQty()) != qty(simulated.getPlatformTotalQty())
            || qty(row.getPlatformAvailableQty()) != qty(simulated.getPlatformAvailableQty());
        return new StockSimulation(eligible, changed, targetMode, qty(simulated.getPlatformTotalQty()),
            qty(simulated.getPlatformAvailableQty()), syncStatus, message);
    }

    private InventoryStockLedger buildAutoSyncLedger(InventorySkuWarehouseStock before, InventorySkuWarehouseStock after,
        InventoryStockSyncPolicyPreviewRow preview, String operator, String operationSource, String bizType)
    {
        InventoryStockLedger ledger = new InventoryStockLedger();
        ledger.setStockId(after.getStockId());
        ledger.setStockKey(after.getStockKey());
        ledger.setSpuId(after.getSpuId());
        ledger.setSkuId(after.getSkuId());
        ledger.setSellerId(after.getSellerId());
        ledger.setSyncPolicyId(after.getSyncPolicyId());
        ledger.setSyncPolicyScope(after.getSyncPolicyScope());
        ledger.setSyncPolicyKey(after.getSyncPolicyKey());
        ledger.setWarehouseKind(after.getWarehouseKind());
        ledger.setWarehouseRefType(after.getWarehouseRefType());
        ledger.setWarehouseName(after.getWarehouseName());
        ledger.setOperationType("AUTO_SOURCE_SYNC");
        ledger.setOperationSource(operationSource);
        ledger.setBizType(bizType);
        ledger.setBizNo("");
        ledger.setDeltaQty(preview.getAfterPlatformTotalQty() - preview.getBeforePlatformTotalQty());
        ledger.setBeforePlatformTotalQty(qty(before.getPlatformTotalQty()));
        ledger.setAfterPlatformTotalQty(qty(after.getPlatformTotalQty()));
        ledger.setBeforeAvailableQty(qty(before.getPlatformAvailableQty()));
        ledger.setAfterAvailableQty(qty(after.getPlatformAvailableQty()));
        ledger.setBeforeReservedQty(qty(before.getPlatformReservedQty()));
        ledger.setAfterReservedQty(qty(after.getPlatformReservedQty()));
        ledger.setBeforeInTransitQty(qty(before.getPlatformInTransitQty()));
        ledger.setAfterInTransitQty(qty(after.getPlatformInTransitQty()));
        ledger.setRiskConfirmed("Y");
        ledger.setRiskMessage(preview.getMessage());
        ledger.setReason("自动同步WMS库存");
        ledger.setOperatorId(safeUserId());
        ledger.setOperatorName(operator);
        ledger.setOperateTime(new Date());
        return ledger;
    }

    private InventoryStockSyncPolicy resolvePolicy(InventorySkuWarehouseStock row, List<InventoryStockSyncPolicy> policies)
    {
        InventoryStockSyncPolicy resolved = null;
        int resolvedPriority = -1;
        for (InventoryStockSyncPolicy policy : policies)
        {
            if (!matches(row, policy))
            {
                continue;
            }
            int priority = priority(policy.getScopeType());
            if (priority > resolvedPriority)
            {
                resolved = policy;
                resolvedPriority = priority;
            }
        }
        return resolved;
    }

    private boolean matches(InventorySkuWarehouseStock row, InventoryStockSyncPolicy policy)
    {
        if (policy == null || !"Y".equals(defaultString(policy.getEnabled(), "Y")))
        {
            return false;
        }
        if (!Objects.equals(row.getSellerId(), policy.getSellerId()))
        {
            return false;
        }
        if (SCOPE_SELLER.equals(policy.getScopeType()))
        {
            return true;
        }
        if (SCOPE_WAREHOUSE.equals(policy.getScopeType()))
        {
            return Objects.equals(warehouseKey(row), policy.getWarehouseKey());
        }
        if (SCOPE_SPU.equals(policy.getScopeType()))
        {
            return Objects.equals(row.getSpuId(), policy.getSpuId());
        }
        if (SCOPE_SKU.equals(policy.getScopeType()))
        {
            return Objects.equals(row.getSkuId(), policy.getSkuId());
        }
        if (SCOPE_SKU_WAREHOUSE.equals(policy.getScopeType()))
        {
            return Objects.equals(row.getStockId(), policy.getStockId());
        }
        return false;
    }

    private int priority(String scopeType)
    {
        if (SCOPE_SKU_WAREHOUSE.equals(scopeType))
        {
            return 50;
        }
        if (SCOPE_WAREHOUSE.equals(scopeType))
        {
            return 40;
        }
        if (SCOPE_SKU.equals(scopeType))
        {
            return 30;
        }
        if (SCOPE_SPU.equals(scopeType))
        {
            return 20;
        }
        if (SCOPE_SELLER.equals(scopeType))
        {
            return 10;
        }
        return 0;
    }

    private List<InventoryStockSyncPolicy> mergeCandidatePolicy(List<InventoryStockSyncPolicy> policies,
        InventoryStockSyncPolicy candidate)
    {
        List<InventoryStockSyncPolicy> merged = new ArrayList<>();
        for (InventoryStockSyncPolicy policy : policies)
        {
            if (!Objects.equals(policy.getPolicyKey(), candidate.getPolicyKey()))
            {
                merged.add(policy);
            }
        }
        merged.add(candidate);
        return merged;
    }

    private List<InventoryStockSyncPolicy> selectPoliciesForRows(List<InventorySkuWarehouseStock> rows)
    {
        List<Long> sellerIds = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (InventorySkuWarehouseStock row : rows)
        {
            if (row.getSellerId() != null && seen.add(row.getSellerId()))
            {
                sellerIds.add(row.getSellerId());
            }
        }
        if (sellerIds.isEmpty())
        {
            return new ArrayList<>();
        }
        return inventoryStockSyncPolicyMapper.selectEnabledPoliciesBySellerIds(sellerIds);
    }

    private List<InventoryStockSyncPolicyRequest> expandWarehouseRequests(InventoryStockSyncPolicyRequest request)
    {
        if (!SCOPE_WAREHOUSE.equals(request.getScopeType()))
        {
            List<InventoryStockSyncPolicyRequest> single = new ArrayList<>();
            single.add(request);
            return single;
        }

        List<String> warehouseKeys = normalizeWarehouseKeys(request);
        if (warehouseKeys.isEmpty())
        {
            throw new ServiceException("仓库设置必须选择仓库");
        }

        List<InventoryStockSyncPolicyRequest> expanded = new ArrayList<>();
        for (String warehouseKey : warehouseKeys)
        {
            InventoryStockSyncPolicyRequest item = copyRequest(request);
            item.setWarehouseKey(warehouseKey);
            item.setWarehouseKeys(null);
            expanded.add(item);
        }
        return expanded;
    }

    private List<String> normalizeWarehouseKeys(InventoryStockSyncPolicyRequest request)
    {
        Set<String> seen = new LinkedHashSet<>();
        if (request.getWarehouseKeys() != null)
        {
            for (String warehouseKey : request.getWarehouseKeys())
            {
                String normalized = trimToEmpty(warehouseKey);
                if (!normalized.isEmpty())
                {
                    seen.add(normalized);
                }
            }
        }
        String singleWarehouseKey = trimToEmpty(request.getWarehouseKey());
        if (!singleWarehouseKey.isEmpty())
        {
            seen.add(singleWarehouseKey);
        }
        return new ArrayList<>(seen);
    }

    private InventoryStockSyncPolicyRequest copyRequest(InventoryStockSyncPolicyRequest source)
    {
        InventoryStockSyncPolicyRequest target = new InventoryStockSyncPolicyRequest();
        target.setScopeType(source.getScopeType());
        target.setSyncMode(source.getSyncMode());
        target.setSellerId(source.getSellerId());
        target.setWarehouseKey(source.getWarehouseKey());
        target.setWarehouseKeys(source.getWarehouseKeys());
        target.setWarehouseName(source.getWarehouseName());
        target.setSpuId(source.getSpuId());
        target.setSkuId(source.getSkuId());
        target.setStockId(source.getStockId());
        target.setConfirmed(source.getConfirmed());
        target.setRemark(source.getRemark());
        return target;
    }

    private InventoryStockSyncPolicyRequest normalizeRequest(InventoryStockSyncPolicyRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("库存同步设置不能为空");
        }
        request.setScopeType(trimUpper(request.getScopeType()));
        request.setSyncMode(trimUpper(request.getSyncMode()));
        request.setWarehouseKey(trimToEmpty(request.getWarehouseKey()));
        request.setWarehouseKeys(normalizeWarehouseKeys(request));
        request.setWarehouseName(trimToEmpty(request.getWarehouseName()));
        request.setRemark(trimToNull(request.getRemark()));

        if (!SCOPE_SELLER.equals(request.getScopeType()) && !SCOPE_WAREHOUSE.equals(request.getScopeType())
            && !SCOPE_SPU.equals(request.getScopeType()) && !SCOPE_SKU.equals(request.getScopeType())
            && !SCOPE_SKU_WAREHOUSE.equals(request.getScopeType()))
        {
            throw new ServiceException("库存同步设置范围不正确");
        }
        if (!SYNC_MODE_MANUAL.equals(request.getSyncMode())
            && !SYNC_MODE_AUTO_SOURCE_AVAILABLE.equals(request.getSyncMode()))
        {
            throw new ServiceException("库存同步方式不正确");
        }
        return request;
    }

    private List<InventorySkuWarehouseStock> selectAffectedRows(InventoryStockSyncPolicyRequest request)
    {
        InventorySkuWarehouseStock query = new InventorySkuWarehouseStock();
        if (SCOPE_SELLER.equals(request.getScopeType()))
        {
            requireSellerId(request);
            query.setSellerId(request.getSellerId());
        }
        else if (SCOPE_WAREHOUSE.equals(request.getScopeType()))
        {
            requireSellerId(request);
            if (request.getWarehouseKey().isEmpty())
            {
                throw new ServiceException("仓库设置必须选择仓库");
            }
            query.setSellerId(request.getSellerId());
            query.setWarehouseKey(request.getWarehouseKey());
        }
        else if (SCOPE_SPU.equals(request.getScopeType()))
        {
            if (request.getSpuId() == null)
            {
                throw new ServiceException("SPU设置必须指定SPU");
            }
            query.setSpuId(request.getSpuId());
        }
        else if (SCOPE_SKU.equals(request.getScopeType()))
        {
            if (request.getSkuId() == null)
            {
                throw new ServiceException("SKU设置必须指定SKU");
            }
            query.setSkuId(request.getSkuId());
        }
        else
        {
            if (request.getStockId() == null)
            {
                throw new ServiceException("明细行设置必须指定库存行");
            }
            query.setStockId(request.getStockId());
        }
        return inventoryOverviewMapper.selectWarehouseStockList(query);
    }

    private void fillRequestTargetSnapshots(InventoryStockSyncPolicyRequest request, List<InventorySkuWarehouseStock> rows)
    {
        InventorySkuWarehouseStock first = rows.get(0);
        if (request.getSellerId() == null)
        {
            request.setSellerId(first.getSellerId());
        }
        if (SCOPE_WAREHOUSE.equals(request.getScopeType()) && request.getWarehouseName().isEmpty())
        {
            request.setWarehouseName(first.getWarehouseName());
        }
        if (SCOPE_SPU.equals(request.getScopeType()) && request.getSpuId() == null)
        {
            request.setSpuId(first.getSpuId());
        }
        if (SCOPE_SKU.equals(request.getScopeType()) && request.getSkuId() == null)
        {
            request.setSkuId(first.getSkuId());
        }
        if (SCOPE_SKU_WAREHOUSE.equals(request.getScopeType()))
        {
            request.setSpuId(first.getSpuId());
            request.setSkuId(first.getSkuId());
            request.setStockId(first.getStockId());
            request.setWarehouseKey(warehouseKey(first));
            request.setWarehouseName(first.getWarehouseName());
        }
    }

    private InventoryStockSyncPolicy buildPolicy(InventoryStockSyncPolicyRequest request, Long policyId)
    {
        InventoryStockSyncPolicy policy = new InventoryStockSyncPolicy();
        policy.setPolicyId(policyId);
        policy.setPolicyKey(policyKey(request));
        policy.setSellerId(request.getSellerId());
        policy.setScopeType(request.getScopeType());
        policy.setWarehouseKey(request.getWarehouseKey());
        policy.setWarehouseName(request.getWarehouseName());
        policy.setSpuId(request.getSpuId());
        policy.setSkuId(request.getSkuId());
        policy.setStockId(request.getStockId());
        policy.setSyncMode(request.getSyncMode());
        policy.setEnabled("Y");
        policy.setRemark(request.getRemark());
        return policy;
    }

    private String policyKey(InventoryStockSyncPolicyRequest request)
    {
        if (SCOPE_SELLER.equals(request.getScopeType()))
        {
            requireSellerId(request);
            return "SELLER:" + request.getSellerId();
        }
        if (SCOPE_WAREHOUSE.equals(request.getScopeType()))
        {
            requireSellerId(request);
            if (request.getWarehouseKey().isEmpty())
            {
                throw new ServiceException("仓库设置必须选择仓库");
            }
            return "WAREHOUSE:" + request.getSellerId() + ":" + request.getWarehouseKey();
        }
        if (SCOPE_SPU.equals(request.getScopeType()))
        {
            requireSellerId(request);
            return "SPU:" + request.getSellerId() + ":" + request.getSpuId();
        }
        if (SCOPE_SKU.equals(request.getScopeType()))
        {
            requireSellerId(request);
            return "SKU:" + request.getSellerId() + ":" + request.getSkuId();
        }
        requireSellerId(request);
        return "SKU_WAREHOUSE:" + request.getSellerId() + ":" + request.getStockId();
    }

    private void requireSellerId(InventoryStockSyncPolicyRequest request)
    {
        if (request.getSellerId() == null)
        {
            throw new ServiceException("库存同步设置必须指定卖家");
        }
    }

    private boolean supportsAutoSync(InventorySkuWarehouseStock row)
    {
        return WAREHOUSE_OFFICIAL.equals(row.getWarehouseKind())
            && REF_OFFICIAL_MASTER.equals(row.getWarehouseRefType())
            && row.getSourceSnapshotTime() != null;
    }

    private String autoUnsupportedReason(InventorySkuWarehouseStock row)
    {
        if (!WAREHOUSE_OFFICIAL.equals(row.getWarehouseKind()))
        {
            return "三方仓未接入WMS来源库存，不能自动同步";
        }
        if (REF_NO_WAREHOUSE.equals(row.getWarehouseRefType()))
        {
            return "发货仓库未配置，不能自动同步WMS库存";
        }
        if (REF_SOURCE_UNBOUND.equals(row.getWarehouseRefType()))
        {
            return "来源SKU未绑定，不能自动同步WMS库存";
        }
        return "未匹配到来源仓库库存，不能自动同步WMS库存";
    }

    private void recalculate(InventorySkuWarehouseStock stock)
    {
        long available;
        if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
        {
            available = 0;
        }
        else if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
        {
            long effectiveSourceAvailable = effectiveSourceAvailable(stock);
            available = Math.min(qty(stock.getPlatformTotalQty()), effectiveSourceAvailable)
                - qty(stock.getPlatformReservedQty());
        }
        else
        {
            available = qty(stock.getPlatformTotalQty()) - qty(stock.getPlatformReservedQty());
        }
        stock.setPlatformAvailableQty(Math.max(0, available));
        stock.setEffectiveStatus(resolveStatus(stock));
        stock.setCalcTime(new Date());
    }

    private String resolveStatus(InventorySkuWarehouseStock stock)
    {
        if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
        {
            return STATUS_NO_WAREHOUSE;
        }
        if (qty(stock.getPlatformAvailableQty()) > 0)
        {
            return STATUS_IN_STOCK;
        }
        if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
        {
            if (REF_SOURCE_UNBOUND.equals(stock.getWarehouseRefType()))
            {
                return STATUS_SOURCE_UNBOUND;
            }
            if (qty(stock.getSourceAvailableQty()) <= 0 && qty(stock.getSourceInTransitQty()) > 0)
            {
                return STATUS_SOURCE_ONLY_IN_TRANSIT;
            }
            if (qty(stock.getSourceTotalQty()) <= 0 && qty(stock.getSourceAvailableQty()) <= 0
                && qty(stock.getSourceInTransitQty()) <= 0)
            {
                return STATUS_NO_SOURCE;
            }
        }
        return STATUS_OUT_OF_STOCK;
    }

    private InventorySkuWarehouseStock copyStock(InventorySkuWarehouseStock source)
    {
        InventorySkuWarehouseStock target = new InventorySkuWarehouseStock();
        target.setStockId(source.getStockId());
        target.setStockKey(source.getStockKey());
        target.setSpuId(source.getSpuId());
        target.setSkuId(source.getSkuId());
        target.setSellerId(source.getSellerId());
        target.setSellerNo(source.getSellerNo());
        target.setSellerName(source.getSellerName());
        target.setSystemSkuCode(source.getSystemSkuCode());
        target.setProductName(source.getProductName());
        target.setSkuName(source.getSkuName());
        target.setSkuImageUrl(source.getSkuImageUrl());
        target.setWarehouseKind(source.getWarehouseKind());
        target.setWarehouseRefType(source.getWarehouseRefType());
        target.setWarehouseId(source.getWarehouseId());
        target.setWarehouseCode(source.getWarehouseCode());
        target.setWarehouseName(source.getWarehouseName());
        target.setSourceScope(source.getSourceScope());
        target.setSourceMasterWarehouseName(source.getSourceMasterWarehouseName());
        target.setSourceTotalQty(qty(source.getSourceTotalQty()));
        target.setSourceAvailableQty(qty(source.getSourceAvailableQty()));
        target.setSourceInTransitQty(qty(source.getSourceInTransitQty()));
        target.setSourceSnapshotTime(source.getSourceSnapshotTime());
        target.setPlatformTotalQty(qty(source.getPlatformTotalQty()));
        target.setPlatformReservedQty(qty(source.getPlatformReservedQty()));
        target.setPlatformInTransitQty(qty(source.getPlatformInTransitQty()));
        target.setPendingAvailableInboundQty(qty(source.getPendingAvailableInboundQty()));
        target.setPendingSourceDeductionQty(qty(source.getPendingSourceDeductionQty()));
        target.setPlatformAvailableQty(qty(source.getPlatformAvailableQty()));
        target.setEffectiveStatus(source.getEffectiveStatus());
        target.setSyncMode(defaultString(source.getSyncMode(), SYNC_MODE_MANUAL));
        target.setSyncPolicyId(source.getSyncPolicyId());
        target.setSyncPolicyScope(defaultString(source.getSyncPolicyScope(), SCOPE_SYSTEM));
        target.setSyncPolicyKey(defaultString(source.getSyncPolicyKey(), ""));
        target.setSyncStatus(defaultString(source.getSyncStatus(), SYNC_STATUS_NORMAL));
        target.setLastAutoSyncTime(source.getLastAutoSyncTime());
        target.setVersion(source.getVersion());
        target.setCalcTime(source.getCalcTime());
        return target;
    }

    private long effectiveSourceAvailable(InventorySkuWarehouseStock stock)
    {
        return Math.max(0, qty(stock.getSourceAvailableQty()) - qty(stock.getPendingSourceDeductionQty()));
    }

    private String warehouseKey(InventorySkuWarehouseStock stock)
    {
        return defaultString(stock.getWarehouseKind(), "") + "|" + defaultString(stock.getWarehouseRefType(), "")
            + "|" + (stock.getWarehouseId() == null ? "" : stock.getWarehouseId())
            + "|" + defaultString(stock.getWarehouseName(), "");
    }

    private long qty(Long value)
    {
        return value == null ? 0L : value;
    }

    private String trimUpper(String value)
    {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String trimToEmpty(String value)
    {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value)
    {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultString(String value, String defaultValue)
    {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private Long safeUserId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private String currentOperator()
    {
        try
        {
            String username = SecurityUtils.getUsername();
            return username == null || username.trim().isEmpty() ? "system" : username;
        }
        catch (RuntimeException e)
        {
            return "system";
        }
    }

    private static class StockSimulation
    {
        private final boolean eligible;
        private final boolean changed;
        private final String syncMode;
        private final long platformTotalQty;
        private final long platformAvailableQty;
        private final String syncStatus;
        private final String message;

        StockSimulation(boolean eligible, boolean changed, String syncMode, long platformTotalQty,
            long platformAvailableQty, String syncStatus, String message)
        {
            this.eligible = eligible;
            this.changed = changed;
            this.syncMode = syncMode;
            this.platformTotalQty = platformTotalQty;
            this.platformAvailableQty = platformAvailableQty;
            this.syncStatus = syncStatus;
            this.message = message;
        }

        public boolean isEligible() { return eligible; }
        public boolean isChanged() { return changed; }
        public String getSyncMode() { return syncMode; }
        public long getPlatformTotalQty() { return platformTotalQty; }
        public long getPlatformAvailableQty() { return platformAvailableQty; }
        public String getSyncStatus() { return syncStatus; }
        public String getMessage() { return message; }
    }

    private static class SyncPolicyContext
    {
        private final InventoryStockSyncPolicyRequest request;
        private final InventoryStockSyncPolicy candidatePolicy;
        private final List<InventorySkuWarehouseStock> rows;
        private final List<InventoryStockSyncPolicy> policies;
        private final InventoryStockSyncPolicyPreviewResult preview;

        SyncPolicyContext(InventoryStockSyncPolicyRequest request, InventoryStockSyncPolicy candidatePolicy,
            List<InventorySkuWarehouseStock> rows, List<InventoryStockSyncPolicy> policies,
            InventoryStockSyncPolicyPreviewResult preview)
        {
            this.request = request;
            this.candidatePolicy = candidatePolicy;
            this.rows = rows;
            this.policies = policies;
            this.preview = preview;
        }

        public InventoryStockSyncPolicyRequest getRequest() { return request; }
        public InventoryStockSyncPolicy getCandidatePolicy() { return candidatePolicy; }
        public List<InventorySkuWarehouseStock> getRows() { return rows; }
        public List<InventoryStockSyncPolicy> getPolicies() { return policies; }
        public InventoryStockSyncPolicyPreviewResult getPreview() { return preview; }
    }
}
