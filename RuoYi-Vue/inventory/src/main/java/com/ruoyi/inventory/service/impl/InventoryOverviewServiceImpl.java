package com.ruoyi.inventory.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewDecision;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewRequest;
import com.ruoyi.inventory.domain.InventoryOverviewAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryOverviewBatchAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryOverviewBatchAdjustRowPreview;
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventoryOverviewSellerOption;
import com.ruoyi.inventory.domain.InventoryOverviewSkuWarehouseGroup;
import com.ruoyi.inventory.domain.InventoryOverviewWarehouseOption;
import com.ruoyi.inventory.domain.InventoryOfficialSourceStock;
import com.ruoyi.inventory.domain.InventoryProductSkuSnapshot;
import com.ruoyi.inventory.domain.InventoryProductSourceBindingSnapshot;
import com.ruoyi.inventory.domain.InventoryProductWarehouseSnapshot;
import com.ruoyi.inventory.domain.InventorySourceSkuKey;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;
import com.ruoyi.inventory.domain.request.InventoryOverviewBatchAdjustItem;
import com.ruoyi.inventory.domain.request.InventoryOverviewBatchAdjustRequest;
import com.ruoyi.inventory.mapper.InventoryOverviewMapper;
import com.ruoyi.inventory.service.IInventoryAdjustmentReviewService;
import com.ruoyi.inventory.service.IInventoryOverviewService;
import com.ruoyi.inventory.service.IInventoryStockSyncPolicyService;
import com.ruoyi.inventory.service.InventoryProductLookupService;
import com.ruoyi.inventory.service.InventorySourceWarehouseStockLookupService;

/**
 * 库存总览服务实现。
 */
@Service
public class InventoryOverviewServiceImpl implements IInventoryOverviewService
{
    private static final String FIELD_PLATFORM_TOTAL = "PLATFORM_TOTAL";
    private static final String FIELD_PLATFORM_IN_TRANSIT = "PLATFORM_IN_TRANSIT";
    private static final String SYNC_MODE_MANUAL = "MANUAL";
    private static final String SYNC_MODE_AUTO_SOURCE_AVAILABLE = "AUTO_SOURCE_AVAILABLE";
    private static final String WAREHOUSE_OFFICIAL = "official";
    private static final String REF_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String REF_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_IN_STOCK = "IN_STOCK";
    private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String STATUS_NO_SOURCE = "NO_SOURCE";
    private static final String STATUS_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String STATUS_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_SOURCE_ONLY_IN_TRANSIT = "SOURCE_ONLY_IN_TRANSIT";

    @Autowired
    private InventoryOverviewMapper inventoryOverviewMapper;

    @Autowired
    private IInventoryAdjustmentReviewService inventoryAdjustmentReviewService;

    @Autowired
    private IInventoryStockSyncPolicyService inventoryStockSyncPolicyService;

    @Autowired
    private ObjectProvider<InventoryProductLookupService> productLookupService;

    @Autowired
    private ObjectProvider<InventorySourceWarehouseStockLookupService> sourceWarehouseStockLookupService;

    @Override
    public List<InventoryOverviewItem> selectSpuList(InventoryOverviewItem query)
    {
        return inventoryOverviewMapper.selectSpuList(query);
    }

    @Override
    public List<InventoryOverviewItem> selectSkuList(InventoryOverviewItem query)
    {
        return inventoryOverviewMapper.selectSkuList(query);
    }

    @Override
    public List<InventorySkuWarehouseStock> selectWarehouseStockList(InventorySkuWarehouseStock query)
    {
        return inventoryOverviewMapper.selectWarehouseStockList(query);
    }

    @Override
    public List<InventoryOverviewWarehouseOption> selectWarehouseOptions()
    {
        return inventoryOverviewMapper.selectWarehouseOptions();
    }

    @Override
    public List<InventoryOverviewWarehouseOption> selectOfficialWarehouseOptions()
    {
        return inventoryOverviewMapper.selectOfficialWarehouseOptions();
    }

    @Override
    public List<InventoryOverviewSellerOption> selectSellerOptions()
    {
        return inventoryOverviewMapper.selectSellerOptions();
    }

    @Override
    public List<InventorySkuWarehouseStock> selectWarehouseStockListBySkuId(Long skuId)
    {
        if (skuId == null)
        {
            throw new ServiceException("SKU ID不能为空");
        }
        return inventoryOverviewMapper.selectWarehouseStockListBySkuId(skuId);
    }

    @Override
    public List<InventoryOverviewSkuWarehouseGroup> selectSkuWarehouseGroupsBySpuId(Long spuId)
    {
        if (spuId == null)
        {
            throw new ServiceException("SPU ID不能为空");
        }

        InventoryOverviewItem query = new InventoryOverviewItem();
        query.setSpuId(spuId);
        List<InventoryOverviewItem> skuRows = inventoryOverviewMapper.selectSkuList(query);
        Map<Long, InventoryOverviewSkuWarehouseGroup> groupMap = new LinkedHashMap<>();
        for (InventoryOverviewItem skuRow : skuRows)
        {
            InventoryOverviewSkuWarehouseGroup group = new InventoryOverviewSkuWarehouseGroup();
            group.setSku(skuRow);
            groupMap.put(skuRow.getSkuId(), group);
        }

        List<InventorySkuWarehouseStock> stockRows = inventoryOverviewMapper.selectWarehouseStockListBySpuId(spuId);
        for (InventorySkuWarehouseStock stockRow : stockRows)
        {
            InventoryOverviewSkuWarehouseGroup group = groupMap.get(stockRow.getSkuId());
            if (group != null)
            {
                group.getWarehouses().add(stockRow);
            }
        }
        return new ArrayList<>(groupMap.values());
    }

    @Override
    public InventoryOverviewAdjustPreviewResult previewAdjust(InventoryOverviewAdjustRequest request)
    {
        validateAdjustRequest(request);
        InventorySkuWarehouseStock stock = requireStock(request.getStockId());
        InventoryOverviewAdjustPreviewResult preview = buildPreview(stock, request);
        applyReviewDecision(preview, inventoryAdjustmentReviewService.evaluateAdjustment(stock, request));
        return preview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOverviewAdjustPreviewResult confirmAdjust(InventoryOverviewAdjustRequest request)
    {
        validateAdjustRequest(request);
        InventorySkuWarehouseStock before = requireStock(request.getStockId());
        InventoryOverviewAdjustPreviewResult preview = buildPreview(before, request);
        if (!Boolean.TRUE.equals(preview.getAllowed()))
        {
            throw new ServiceException(preview.getMessage());
        }
        if (Boolean.TRUE.equals(preview.getConfirmationRequired()) && !Boolean.TRUE.equals(request.getConfirmed()))
        {
            throw new ServiceException("请先确认库存调整风险");
        }
        InventoryAdjustmentReviewDecision decision = inventoryAdjustmentReviewService.evaluateAdjustment(before, request);
        if (Boolean.TRUE.equals(decision.getReviewRequired()))
        {
            InventoryAdjustmentReviewRequest review = inventoryAdjustmentReviewService.submitAdjustmentReview(before, request, decision);
            applyReviewDecision(preview, decision);
            preview.setReviewId(review.getReviewId());
            preview.setReviewNo(review.getReviewNo());
            preview.setPlannedEffectiveTime(review.getPlannedEffectiveTime());
            preview.setMessage("已生成库存调整审核单：" + review.getReviewNo());
            return preview;
        }

        InventorySkuWarehouseStock after = copyStock(before);
        if (FIELD_PLATFORM_TOTAL.equals(request.getAdjustField()))
        {
            after.setPlatformTotalQty(request.getTargetQty());
        }
        else
        {
            after.setPlatformInTransitQty(request.getTargetQty());
        }
        recalculate(after);
        after.setUpdateBy(SecurityUtils.getUsername());
        int updatedRows = inventoryOverviewMapper.updateWarehouseStock(after);
        if (updatedRows != 1)
        {
            throw new ServiceException("库存数据已变更，请刷新后重试");
        }

        inventoryOverviewMapper.insertLedger(buildLedger(before, after, request, preview));
        refreshSkuReadModel(after.getSkuId());
        refreshSpuReadModel(after.getSpuId());
        return preview;
    }

    @Override
    public InventoryOverviewBatchAdjustPreviewResult previewBatchAdjust(InventoryOverviewBatchAdjustRequest request)
    {
        return buildBatchAdjust(request).getPreview();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOverviewBatchAdjustPreviewResult confirmBatchAdjust(InventoryOverviewBatchAdjustRequest request)
    {
        BatchAdjustBuild build = buildBatchAdjust(request);
        InventoryOverviewBatchAdjustPreviewResult preview = build.getPreview();
        if (!Boolean.TRUE.equals(preview.getAllowed()))
        {
            throw new ServiceException(preview.getMessage());
        }
        if (Boolean.TRUE.equals(preview.getConfirmationRequired()) && !Boolean.TRUE.equals(request.getConfirmed()))
        {
            throw new ServiceException("请先确认库存调整风险");
        }

        Set<Long> skuIds = new LinkedHashSet<>();
        Set<Long> spuIds = new LinkedHashSet<>();
        for (BatchAdjustContext context : build.getContexts())
        {
            if (context.isReviewRequired())
            {
                inventoryAdjustmentReviewService.submitAdjustmentReview(context.getBefore(),
                    buildAdjustRequest(context.getBefore().getStockId(), FIELD_PLATFORM_TOTAL,
                        context.getAfter().getPlatformTotalQty(), request), context.getReviewDecision());
                continue;
            }
            InventorySkuWarehouseStock after = context.getAfter();
            after.setUpdateBy(SecurityUtils.getUsername());
            int updatedRows = inventoryOverviewMapper.updateWarehouseStock(after);
            if (updatedRows != 1)
            {
                throw new ServiceException("库存数据已变更，请刷新后重试");
            }
            if (context.isPlatformTotalChanged())
            {
                inventoryOverviewMapper.insertLedger(buildLedger(context.getBefore(), after,
                    buildAdjustRequest(after.getStockId(), FIELD_PLATFORM_TOTAL, after.getPlatformTotalQty(), request),
                    buildFieldPreview(context, FIELD_PLATFORM_TOTAL)));
            }
            if (context.isPlatformInTransitChanged())
            {
                inventoryOverviewMapper.insertLedger(buildLedger(context.getBefore(), after,
                    buildAdjustRequest(after.getStockId(), FIELD_PLATFORM_IN_TRANSIT, after.getPlatformInTransitQty(), request),
                    buildFieldPreview(context, FIELD_PLATFORM_IN_TRANSIT)));
            }
            skuIds.add(after.getSkuId());
            spuIds.add(after.getSpuId());
        }

        for (Long affectedSkuId : skuIds)
        {
            refreshSkuReadModel(affectedSkuId);
        }
        for (Long affectedSpuId : spuIds)
        {
            refreshSpuReadModel(affectedSpuId);
        }
        if (qty(preview.getReviewRequiredCount()) > 0)
        {
            long directCount = qty(preview.getChangedRowCount()) - qty(preview.getReviewRequiredCount());
            preview.setMessage("已直接更新" + directCount + "条库存明细，生成"
                + preview.getReviewRequiredCount() + "条库存调整审核单");
        }
        return preview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshProductInventoryOverview(Long spuId)
    {
        if (spuId == null)
        {
            throw new ServiceException("SPU ID不能为空");
        }
        String operator = currentOperator();
        InventoryProductLookupService productLookup = requireProductLookupService();
        List<InventoryProductSkuSnapshot> skuSnapshots = emptyIfNull(productLookup.selectSkuSnapshotsBySpuId(spuId));
        List<InventoryProductSourceBindingSnapshot> sourceBindings =
            emptyIfNull(productLookup.selectSourceBindingSnapshotsBySpuId(spuId));
        List<InventoryProductWarehouseSnapshot> warehouseSnapshots =
            emptyIfNull(productLookup.selectWarehouseSnapshotsBySpuId(spuId));
        List<InventorySourceSkuKey> sourceKeys = productLookup.selectSourceSkuKeysBySpuId(spuId);
        List<InventoryOfficialSourceStock> sourceStocks = sourceKeys == null || sourceKeys.isEmpty()
                ? new ArrayList<>() : requireSourceWarehouseStockLookupService()
                        .selectOfficialMasterStocksBySourceSkuKeys(sourceKeys);
        inventoryOverviewMapper.deleteObsoleteSkuWarehouseStocksBySpuId(spuId, skuSnapshots, sourceBindings,
            warehouseSnapshots, sourceStocks);
        inventoryOverviewMapper.upsertOfficialMasterSkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,
            sourceBindings, sourceStocks);
        inventoryOverviewMapper.upsertSourceUnboundSkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,
            sourceBindings, warehouseSnapshots);
        inventoryOverviewMapper.upsertUnmatchedOfficialSkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,
            sourceBindings, sourceStocks);
        inventoryOverviewMapper.upsertThirdPartySkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,
            warehouseSnapshots);
        inventoryOverviewMapper.upsertNoWarehouseSkuWarehouseStocksBySpuId(spuId, operator, skuSnapshots,
            sourceBindings, warehouseSnapshots);
        inventoryStockSyncPolicyService.applyAutoSyncForSpu(spuId, operator);

        for (InventoryProductSkuSnapshot skuSnapshot : skuSnapshots)
        {
            if (skuSnapshot != null && skuSnapshot.getSkuId() != null)
            {
                inventoryOverviewMapper.refreshSkuReadModel(skuSnapshot.getSkuId(), skuSnapshots);
            }
        }
        inventoryOverviewMapper.refreshSpuReadModel(spuId, skuSnapshots);
    }

    @Override
    public List<Long> selectSourceInventoryOverviewSpuIdsByConnection(String connectionCode)
    {
        if (connectionCode == null || connectionCode.trim().isEmpty())
        {
            return new ArrayList<>();
        }
        List<InventorySourceSkuKey> sourceKeys =
            requireSourceWarehouseStockLookupService()
                    .selectAffectedOfficialMasterSkuKeysByConnection(connectionCode.trim());
        if (sourceKeys == null || sourceKeys.isEmpty())
        {
            return new ArrayList<>();
        }
        return requireProductLookupService().selectSpuIdsBySourceSkuKeys(sourceKeys);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshSourceInventoryOverviewByConnection(String connectionCode)
    {
        refreshSourceInventoryOverviewByConnection(connectionCode, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshSourceInventoryOverviewByConnection(String connectionCode, List<Long> preRebuildSpuIds)
    {
        if (connectionCode == null || connectionCode.trim().isEmpty())
        {
            return;
        }
        Set<Long> spuIds = new LinkedHashSet<>();
        if (preRebuildSpuIds != null)
        {
            for (Long spuId : preRebuildSpuIds)
            {
                if (spuId != null)
                {
                    spuIds.add(spuId);
                }
            }
        }
        for (Long spuId : selectSourceInventoryOverviewSpuIdsByConnection(connectionCode))
        {
            if (spuId != null)
            {
                spuIds.add(spuId);
            }
        }
        for (Long spuId : spuIds)
        {
            refreshProductInventoryOverview(spuId);
        }
    }

    @Override
    public List<InventoryStockLedger> selectLedgerList(InventoryStockLedger query)
    {
        return inventoryOverviewMapper.selectLedgerList(query);
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

    private InventorySourceWarehouseStockLookupService requireSourceWarehouseStockLookupService()
    {
        InventorySourceWarehouseStockLookupService lookupService = sourceWarehouseStockLookupService.getIfAvailable();
        if (lookupService == null)
        {
            throw new ServiceException("来源仓库存读模型查询服务不可用");
        }
        return lookupService;
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

    private <T> List<T> emptyIfNull(List<T> rows)
    {
        return rows == null ? new ArrayList<>() : rows;
    }

    private void validateAdjustRequest(InventoryOverviewAdjustRequest request)
    {
        if (request == null || request.getStockId() == null)
        {
            throw new ServiceException("库存行不能为空");
        }
        if (request.getTargetQty() == null || request.getTargetQty() < 0)
        {
            throw new ServiceException("库存数量不能小于0");
        }
        String field = request.getAdjustField();
        if (!FIELD_PLATFORM_TOTAL.equals(field) && !FIELD_PLATFORM_IN_TRANSIT.equals(field))
        {
            throw new ServiceException("库存调整字段不正确");
        }
        request.setReason(trimReason(request.getReason()));
    }

    private void validateBatchAdjustRequest(InventoryOverviewBatchAdjustRequest request)
    {
        if (request == null || request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("库存调整明细不能为空");
        }
        request.setReason(trimReason(request.getReason()));
        Set<Long> stockIds = new LinkedHashSet<>();
        for (InventoryOverviewBatchAdjustItem item : request.getItems())
        {
            if (item == null || item.getStockId() == null)
            {
                throw new ServiceException("库存行不能为空");
            }
            if (!stockIds.add(item.getStockId()))
            {
                throw new ServiceException("同一库存行不能重复提交");
            }
            if (item.getTargetPlatformTotalQty() == null || item.getTargetPlatformTotalQty() < 0)
            {
                throw new ServiceException("调整后平台总库存不能小于0");
            }
            if (item.getTargetPlatformInTransitQty() == null || item.getTargetPlatformInTransitQty() < 0)
            {
                throw new ServiceException("调整后平台在途库存不能小于0");
            }
        }
    }

    private String trimReason(String reason)
    {
        if (reason == null)
        {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private InventorySkuWarehouseStock requireStock(Long stockId)
    {
        InventorySkuWarehouseStock stock = inventoryOverviewMapper.selectWarehouseStockById(stockId);
        if (stock == null)
        {
            throw new ServiceException("库存行不存在");
        }
        return stock;
    }

    private BatchAdjustBuild buildBatchAdjust(InventoryOverviewBatchAdjustRequest request)
    {
        validateBatchAdjustRequest(request);
        InventoryOverviewBatchAdjustPreviewResult preview = new InventoryOverviewBatchAdjustPreviewResult();
        List<BatchAdjustContext> contexts = new ArrayList<>();
        boolean allowed = true;
        String firstRejectedMessage = null;
        long reviewRequiredCount = 0;
        long beforePlatformTotalQty = 0;
        long afterPlatformTotalQty = 0;
        long beforePlatformInTransitQty = 0;
        long afterPlatformInTransitQty = 0;
        long beforeAvailableQty = 0;
        long afterAvailableQty = 0;

        for (InventoryOverviewBatchAdjustItem item : request.getItems())
        {
            InventorySkuWarehouseStock before = requireStock(item.getStockId());
            BatchAdjustContext context = buildBatchAdjustContext(before, item);
            if (context == null)
            {
                continue;
            }
            InventoryOverviewBatchAdjustRowPreview rowPreview = context.getRowPreview();
            preview.getRows().add(rowPreview);
            if (!Boolean.TRUE.equals(rowPreview.getAllowed()))
            {
                allowed = false;
                if (firstRejectedMessage == null)
                {
                    firstRejectedMessage = rowPreview.getMessage();
                }
            }
            else
            {
                contexts.add(context);
                if (context.isReviewRequired())
                {
                    reviewRequiredCount++;
                }
            }
            beforePlatformTotalQty += qty(rowPreview.getBeforePlatformTotalQty());
            afterPlatformTotalQty += qty(rowPreview.getAfterPlatformTotalQty());
            beforePlatformInTransitQty += qty(rowPreview.getBeforePlatformInTransitQty());
            afterPlatformInTransitQty += qty(rowPreview.getAfterPlatformInTransitQty());
            beforeAvailableQty += qty(rowPreview.getBeforeAvailableQty());
            afterAvailableQty += qty(rowPreview.getAfterAvailableQty());
        }

        if (preview.getRows().isEmpty())
        {
            preview.setAllowed(false);
            preview.setConfirmationRequired(false);
            preview.setMessage("没有可保存的库存调整");
        }
        else
        {
            preview.setAllowed(allowed);
            preview.setConfirmationRequired(allowed);
            preview.setMessage(allowed ? buildBatchPreviewMessage(contexts.size(), reviewRequiredCount)
                : "存在不允许保存的库存调整：" + firstRejectedMessage);
        }
        preview.setChangedRowCount((long) contexts.size());
        preview.setReviewRequiredCount(reviewRequiredCount);
        preview.setBeforePlatformTotalQty(beforePlatformTotalQty);
        preview.setAfterPlatformTotalQty(afterPlatformTotalQty);
        preview.setBeforePlatformInTransitQty(beforePlatformInTransitQty);
        preview.setAfterPlatformInTransitQty(afterPlatformInTransitQty);
        preview.setBeforeAvailableQty(beforeAvailableQty);
        preview.setAfterAvailableQty(afterAvailableQty);
        return new BatchAdjustBuild(preview, contexts);
    }

    private BatchAdjustContext buildBatchAdjustContext(InventorySkuWarehouseStock before,
        InventoryOverviewBatchAdjustItem item)
    {
        long beforePlatformTotalQty = qty(before.getPlatformTotalQty());
        long beforePlatformInTransitQty = qty(before.getPlatformInTransitQty());
        long targetPlatformTotalQty = qty(item.getTargetPlatformTotalQty());
        long targetPlatformInTransitQty = qty(item.getTargetPlatformInTransitQty());
        boolean platformTotalChanged = beforePlatformTotalQty != targetPlatformTotalQty;
        boolean platformInTransitChanged = beforePlatformInTransitQty != targetPlatformInTransitQty;
        if (!platformTotalChanged && !platformInTransitChanged)
        {
            return null;
        }

        InventoryOverviewBatchAdjustRowPreview rowPreview = new InventoryOverviewBatchAdjustRowPreview();
        rowPreview.setStockId(before.getStockId());
        rowPreview.setSystemSkuCode(before.getSystemSkuCode());
        rowPreview.setWarehouseName(before.getWarehouseName());
        rowPreview.setBeforePlatformTotalQty(beforePlatformTotalQty);
        rowPreview.setAfterPlatformTotalQty(targetPlatformTotalQty);
        rowPreview.setPlatformTotalDeltaQty(targetPlatformTotalQty - beforePlatformTotalQty);
        rowPreview.setBeforePlatformInTransitQty(beforePlatformInTransitQty);
        rowPreview.setAfterPlatformInTransitQty(targetPlatformInTransitQty);
        rowPreview.setPlatformInTransitDeltaQty(targetPlatformInTransitQty - beforePlatformInTransitQty);
        rowPreview.setBeforeAvailableQty(qty(before.getPlatformAvailableQty()));

        String validationMessage = validateBatchAdjustTargets(before, targetPlatformTotalQty, targetPlatformInTransitQty,
            platformTotalChanged, platformInTransitChanged);
        if (validationMessage != null)
        {
            rowPreview.setAllowed(false);
            rowPreview.setMessage(validationMessage);
            rowPreview.setAfterAvailableQty(qty(before.getPlatformAvailableQty()));
            return new BatchAdjustContext(before, before, rowPreview, platformTotalChanged, platformInTransitChanged,
                null);
        }

        InventorySkuWarehouseStock after = copyStock(before);
        after.setPlatformTotalQty(targetPlatformTotalQty);
        after.setPlatformInTransitQty(targetPlatformInTransitQty);
        recalculate(after);
        InventoryAdjustmentReviewDecision reviewDecision = null;
        if (platformTotalChanged)
        {
            reviewDecision = inventoryAdjustmentReviewService.evaluateAdjustment(before,
                buildAdjustRequest(before.getStockId(), FIELD_PLATFORM_TOTAL, targetPlatformTotalQty, null));
            if (Boolean.TRUE.equals(reviewDecision.getReviewRequired()) && platformInTransitChanged)
            {
                rowPreview.setAllowed(false);
                rowPreview.setMessage("平台总库存进入审核时，不能同时调整平台在途库存");
                rowPreview.setAfterAvailableQty(qty(before.getPlatformAvailableQty()));
                return new BatchAdjustContext(before, before, rowPreview, platformTotalChanged, platformInTransitChanged,
                    reviewDecision);
            }
            if (Boolean.TRUE.equals(reviewDecision.getReviewRequired()))
            {
                rowPreview.setReviewRequired(true);
                rowPreview.setRequestedAdjustQty(reviewDecision.getRequestedAdjustQty());
                rowPreview.setImmediateReturnableQty(reviewDecision.getImmediateReturnableQty());
            }
        }
        rowPreview.setAllowed(true);
        rowPreview.setMessage(Boolean.TRUE.equals(rowPreview.getReviewRequired()) ? reviewDecision.getMessage()
            : buildBatchAdjustRowMessage(before, targetPlatformTotalQty, targetPlatformInTransitQty,
                platformTotalChanged, platformInTransitChanged));
        rowPreview.setAfterAvailableQty(qty(after.getPlatformAvailableQty()));
        return new BatchAdjustContext(before, after, rowPreview, platformTotalChanged, platformInTransitChanged,
            reviewDecision);
    }

    private String validateBatchAdjustTargets(InventorySkuWarehouseStock stock, long targetPlatformTotalQty,
        long targetPlatformInTransitQty, boolean platformTotalChanged, boolean platformInTransitChanged)
    {
        if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
        {
            return "商品未配置发货仓库，不能调整平台库存";
        }
        if (platformTotalChanged)
        {
            if (SYNC_MODE_AUTO_SOURCE_AVAILABLE.equals(stock.getSyncMode()))
            {
                return "当前库存行已开启自动同步WMS库存，请先切换为手动设置平台库存";
            }
            if (targetPlatformTotalQty < qty(stock.getPlatformReservedQty()))
            {
                return "平台总库存不能小于当前平台锁定库存";
            }
            if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
            {
                long maxPlatformTotalQty = effectiveSourceAvailable(stock);
                if (targetPlatformTotalQty > maxPlatformTotalQty)
                {
                    return "平台总库存不能大于当前来源可用库存扣减后的数量，当前最大可调整为"
                        + maxPlatformTotalQty;
                }
            }
        }
        if (platformInTransitChanged)
        {
            if (!WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
            {
                return "三方仓不支持设置平台在途库存";
            }
            long maxObservableInTransit = qty(stock.getSourceInTransitQty()) + qty(stock.getPendingAvailableInboundQty());
            if (targetPlatformInTransitQty > maxObservableInTransit)
            {
                return "平台在途库存不能大于当前可观察的来源在途库存";
            }
        }
        return null;
    }

    private String buildBatchAdjustRowMessage(InventorySkuWarehouseStock stock, long targetPlatformTotalQty,
        long targetPlatformInTransitQty, boolean platformTotalChanged, boolean platformInTransitChanged)
    {
        List<String> messages = new ArrayList<>();
        if (platformTotalChanged)
        {
            messages.add(targetPlatformTotalQty >= qty(stock.getPlatformTotalQty())
                ? "增加平台总库存"
                : "减少平台总库存");
        }
        if (platformInTransitChanged)
        {
            messages.add(targetPlatformInTransitQty >= qty(stock.getPlatformInTransitQty())
                ? "增加平台在途库存"
                : "减少平台在途库存");
        }
        return String.join("；", messages);
    }

    private String buildBatchPreviewMessage(long contextCount, long reviewRequiredCount)
    {
        if (reviewRequiredCount <= 0)
        {
            return "本次将调整" + contextCount + "条库存明细，请确认后保存";
        }
        long directCount = contextCount - reviewRequiredCount;
        return "本次将直接调整" + directCount + "条库存明细，并生成"
            + reviewRequiredCount + "条库存调整审核单，请确认后保存";
    }

    private void applyReviewDecision(InventoryOverviewAdjustPreviewResult preview,
        InventoryAdjustmentReviewDecision decision)
    {
        if (preview == null || decision == null)
        {
            return;
        }
        preview.setReviewRequired(Boolean.TRUE.equals(decision.getReviewRequired()));
        preview.setRequestedAdjustQty(decision.getRequestedAdjustQty());
        preview.setProtectedRetainedQty(decision.getProtectedRetainedQty());
        preview.setMinRetainedQty(decision.getMinRetainedQty());
        preview.setImmediateReturnableQty(decision.getImmediateReturnableQty());
        if (Boolean.TRUE.equals(decision.getReviewRequired()))
        {
            preview.setConfirmationRequired(true);
            preview.setMessage(decision.getMessage());
        }
    }

    private InventoryOverviewAdjustPreviewResult buildFieldPreview(BatchAdjustContext context, String field)
    {
        InventoryOverviewAdjustPreviewResult preview = new InventoryOverviewAdjustPreviewResult();
        preview.setAllowed(true);
        preview.setConfirmationRequired(true);
        preview.setMessage(context.getRowPreview().getMessage());
        if (FIELD_PLATFORM_TOTAL.equals(field))
        {
            preview.setBeforeValue(qty(context.getBefore().getPlatformTotalQty()));
            preview.setAfterValue(qty(context.getAfter().getPlatformTotalQty()));
        }
        else
        {
            preview.setBeforeValue(qty(context.getBefore().getPlatformInTransitQty()));
            preview.setAfterValue(qty(context.getAfter().getPlatformInTransitQty()));
        }
        preview.setBeforeAvailableQty(qty(context.getBefore().getPlatformAvailableQty()));
        preview.setAfterAvailableQty(qty(context.getAfter().getPlatformAvailableQty()));
        preview.setReservedQty(qty(context.getBefore().getPlatformReservedQty()));
        return preview;
    }

    private InventoryOverviewAdjustRequest buildAdjustRequest(Long stockId, String field, Long targetQty,
        InventoryOverviewBatchAdjustRequest batchRequest)
    {
        InventoryOverviewAdjustRequest request = new InventoryOverviewAdjustRequest();
        request.setStockId(stockId);
        request.setAdjustField(field);
        request.setTargetQty(targetQty);
        if (batchRequest != null)
        {
            request.setConfirmed(batchRequest.getConfirmed());
            request.setReason(batchRequest.getReason());
        }
        return request;
    }

    private InventoryOverviewAdjustPreviewResult buildPreview(InventorySkuWarehouseStock stock,
        InventoryOverviewAdjustRequest request)
    {
        InventorySkuWarehouseStock simulated = copyStock(stock);
        long beforeValue;
        long afterValue = qty(request.getTargetQty());
        String message;

        if (FIELD_PLATFORM_TOTAL.equals(request.getAdjustField()))
        {
            beforeValue = qty(stock.getPlatformTotalQty());
            simulated.setPlatformTotalQty(afterValue);
            if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
            {
                return result(false, false, "商品未配置发货仓库，不能调整平台库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            if (SYNC_MODE_AUTO_SOURCE_AVAILABLE.equals(stock.getSyncMode()))
            {
                return result(false, false, "当前库存行已开启自动同步WMS库存，请先切换为手动设置平台库存",
                    beforeValue, afterValue, qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            if (afterValue < qty(stock.getPlatformReservedQty()))
            {
                return result(false, false, "平台总库存不能小于当前平台锁定库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
            {
                long maxPlatformTotalQty = effectiveSourceAvailable(stock);
                if (afterValue > maxPlatformTotalQty)
                {
                    return result(false, false, "平台总库存不能大于当前来源可用库存扣减后的数量，当前最大可调整为"
                        + maxPlatformTotalQty, beforeValue, afterValue, qty(stock.getPlatformAvailableQty()),
                        qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformReservedQty()));
                }
            }
            message = afterValue >= beforeValue
                ? "本次会增加平台总库存。后续再减少库存时，可能需要受近期销量保护规则限制。"
                : "本次会减少平台总库存。请确认不会影响已承诺给买家的可售库存。";
        }
        else
        {
            beforeValue = qty(stock.getPlatformInTransitQty());
            if (!WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
            {
                return result(false, false, "三方仓不支持设置平台在途库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            long maxObservableInTransit = qty(stock.getSourceInTransitQty()) + qty(stock.getPendingAvailableInboundQty());
            if (afterValue > maxObservableInTransit)
            {
                return result(false, false, "平台在途库存不能大于当前可观察的来源在途库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            simulated.setPlatformInTransitQty(afterValue);
            message = afterValue >= beforeValue
                ? "本次会增加平台在途库存。来源可用库存增加后，系统会自动转入平台总库存。"
                : "本次会减少平台在途库存。已进入待来源可用观察的数量不会被本次操作回滚。";
        }

        recalculate(simulated);
        return result(true, true, message, beforeValue, afterValue, qty(stock.getPlatformAvailableQty()),
            qty(simulated.getPlatformAvailableQty()), qty(stock.getPlatformReservedQty()));
    }

    private InventoryOverviewAdjustPreviewResult result(boolean allowed, boolean confirmationRequired, String message,
        long beforeValue, long afterValue, long beforeAvailableQty, long afterAvailableQty, long reservedQty)
    {
        InventoryOverviewAdjustPreviewResult result = new InventoryOverviewAdjustPreviewResult();
        result.setAllowed(allowed);
        result.setConfirmationRequired(confirmationRequired);
        result.setMessage(message);
        result.setBeforeValue(beforeValue);
        result.setAfterValue(afterValue);
        result.setBeforeAvailableQty(beforeAvailableQty);
        result.setAfterAvailableQty(afterAvailableQty);
        result.setReservedQty(reservedQty);
        return result;
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

    private InventoryStockLedger buildLedger(InventorySkuWarehouseStock before, InventorySkuWarehouseStock after,
        InventoryOverviewAdjustRequest request, InventoryOverviewAdjustPreviewResult preview)
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
        ledger.setOperationType(resolveOperationType(request, before));
        ledger.setOperationSource("ADMIN");
        ledger.setBizType("INVENTORY_OVERVIEW_ADJUST");
        ledger.setBizNo("");
        ledger.setDeltaQty(preview.getAfterValue() - preview.getBeforeValue());
        ledger.setBeforePlatformTotalQty(qty(before.getPlatformTotalQty()));
        ledger.setAfterPlatformTotalQty(qty(after.getPlatformTotalQty()));
        ledger.setBeforeAvailableQty(qty(before.getPlatformAvailableQty()));
        ledger.setAfterAvailableQty(qty(after.getPlatformAvailableQty()));
        ledger.setBeforeReservedQty(qty(before.getPlatformReservedQty()));
        ledger.setAfterReservedQty(qty(after.getPlatformReservedQty()));
        ledger.setBeforeInTransitQty(qty(before.getPlatformInTransitQty()));
        ledger.setAfterInTransitQty(qty(after.getPlatformInTransitQty()));
        ledger.setRiskConfirmed(Boolean.TRUE.equals(request.getConfirmed()) ? "Y" : "N");
        ledger.setRiskMessage(preview.getMessage());
        ledger.setReason(request.getReason());
        ledger.setOperatorId(SecurityUtils.getUserId());
        ledger.setOperatorName(SecurityUtils.getUsername());
        ledger.setOperateTime(new Date());
        return ledger;
    }

    private String resolveOperationType(InventoryOverviewAdjustRequest request, InventorySkuWarehouseStock before)
    {
        if (FIELD_PLATFORM_IN_TRANSIT.equals(request.getAdjustField()))
        {
            return "IN_TRANSIT_CONFIG";
        }
        return qty(request.getTargetQty()) >= qty(before.getPlatformTotalQty()) ? "MANUAL_INCREASE" : "MANUAL_DECREASE";
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
        target.setSyncMode(source.getSyncMode() == null ? SYNC_MODE_MANUAL : source.getSyncMode());
        target.setSyncPolicyId(source.getSyncPolicyId());
        target.setSyncPolicyScope(source.getSyncPolicyScope());
        target.setSyncPolicyKey(source.getSyncPolicyKey());
        target.setSyncStatus(source.getSyncStatus());
        target.setLastAutoSyncTime(source.getLastAutoSyncTime());
        target.setVersion(source.getVersion());
        target.setCalcTime(source.getCalcTime());
        return target;
    }

    private long effectiveSourceAvailable(InventorySkuWarehouseStock stock)
    {
        return Math.max(0, qty(stock.getSourceAvailableQty()) - qty(stock.getPendingSourceDeductionQty()));
    }

    private long qty(Long value)
    {
        return value == null ? 0L : value;
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

    private static class BatchAdjustBuild
    {
        private final InventoryOverviewBatchAdjustPreviewResult preview;
        private final List<BatchAdjustContext> contexts;

        BatchAdjustBuild(InventoryOverviewBatchAdjustPreviewResult preview, List<BatchAdjustContext> contexts)
        {
            this.preview = preview;
            this.contexts = contexts;
        }

        public InventoryOverviewBatchAdjustPreviewResult getPreview()
        {
            return preview;
        }

        public List<BatchAdjustContext> getContexts()
        {
            return contexts;
        }
    }

    private static class BatchAdjustContext
    {
        private final InventorySkuWarehouseStock before;
        private final InventorySkuWarehouseStock after;
        private final InventoryOverviewBatchAdjustRowPreview rowPreview;
        private final boolean platformTotalChanged;
        private final boolean platformInTransitChanged;
        private final InventoryAdjustmentReviewDecision reviewDecision;

        BatchAdjustContext(InventorySkuWarehouseStock before, InventorySkuWarehouseStock after,
            InventoryOverviewBatchAdjustRowPreview rowPreview, boolean platformTotalChanged,
            boolean platformInTransitChanged, InventoryAdjustmentReviewDecision reviewDecision)
        {
            this.before = before;
            this.after = after;
            this.rowPreview = rowPreview;
            this.platformTotalChanged = platformTotalChanged;
            this.platformInTransitChanged = platformInTransitChanged;
            this.reviewDecision = reviewDecision;
        }

        public InventorySkuWarehouseStock getBefore()
        {
            return before;
        }

        public InventorySkuWarehouseStock getAfter()
        {
            return after;
        }

        public InventoryOverviewBatchAdjustRowPreview getRowPreview()
        {
            return rowPreview;
        }

        public boolean isPlatformTotalChanged()
        {
            return platformTotalChanged;
        }

        public boolean isPlatformInTransitChanged()
        {
            return platformInTransitChanged;
        }

        public InventoryAdjustmentReviewDecision getReviewDecision()
        {
            return reviewDecision;
        }

        public boolean isReviewRequired()
        {
            return reviewDecision != null && Boolean.TRUE.equals(reviewDecision.getReviewRequired());
        }
    }
}
