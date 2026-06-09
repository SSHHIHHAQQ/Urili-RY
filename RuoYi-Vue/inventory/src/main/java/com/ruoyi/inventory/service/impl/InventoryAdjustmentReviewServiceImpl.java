package com.ruoyi.inventory.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewDecision;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewOperationLog;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicy;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicyBinding;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewRequest;
import com.ruoyi.inventory.domain.InventoryOverviewAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryProductSkuSnapshot;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.request.InventoryAdjustmentReviewActionRequest;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;
import com.ruoyi.inventory.mapper.InventoryAdjustmentReviewMapper;
import com.ruoyi.inventory.mapper.InventoryOverviewMapper;
import com.ruoyi.inventory.service.IInventoryAdjustmentReviewService;
import com.ruoyi.inventory.service.InventoryProductLookupService;

/**
 * 库存调整审核服务实现。
 */
@Service
public class InventoryAdjustmentReviewServiceImpl implements IInventoryAdjustmentReviewService
{
    private static final Logger log = LoggerFactory.getLogger(InventoryAdjustmentReviewServiceImpl.class);

    private static final String FIELD_PLATFORM_TOTAL = "PLATFORM_TOTAL";
    private static final String MODE_DISABLED = "DISABLED";
    private static final String MODE_ALWAYS = "ALWAYS";
    private static final String MODE_CONDITIONAL = "CONDITIONAL";
    private static final String DIRECTION_DECREASE = "DECREASE";
    private static final String DIRECTION_INCREASE = "INCREASE";
    private static final String DIRECTION_BOTH = "BOTH";
    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String WAREHOUSE_OFFICIAL = "official";
    private static final String REF_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String STATUS_IN_STOCK = "IN_STOCK";
    private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String STATUS_NO_SOURCE = "NO_SOURCE";
    private static final String STATUS_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String STATUS_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_SOURCE_ONLY_IN_TRANSIT = "SOURCE_ONLY_IN_TRANSIT";
    private static final String REF_SOURCE_UNBOUND = "SOURCE_UNBOUND";

    @Autowired
    private InventoryAdjustmentReviewMapper reviewMapper;

    @Autowired
    private InventoryOverviewMapper inventoryOverviewMapper;

    @Autowired
    private ObjectProvider<InventoryProductLookupService> productLookupService;

    @Override
    public InventoryAdjustmentReviewDecision evaluateAdjustment(InventorySkuWarehouseStock stock,
        InventoryOverviewAdjustRequest request)
    {
        InventoryAdjustmentReviewDecision decision = new InventoryAdjustmentReviewDecision();
        decision.setReviewRequired(false);
        if (stock == null || request == null || !FIELD_PLATFORM_TOTAL.equals(request.getAdjustField()))
        {
            return decision;
        }

        long beforeQty = qty(stock.getPlatformTotalQty());
        long targetQty = qty(request.getTargetQty());
        String direction = targetQty < beforeQty ? DIRECTION_DECREASE : targetQty > beforeQty ? DIRECTION_INCREASE : "";
        if (direction.isEmpty())
        {
            return decision;
        }
        long requestedQty = Math.abs(beforeQty - targetQty);
        decision.setRequestedAdjustQty(requestedQty);
        decision.setRequestExpectedAfterPlatformTotalQty(targetQty);

        InventoryAdjustmentReviewPolicy policy = matchedPolicy(stock.getSellerId());
        decision.setPolicy(policy);
        if (MODE_DISABLED.equals(policy.getReviewMode()) || !matchesDirection(policy, direction)
            || !matchesField(policy))
        {
            return decision;
        }

        fillSalesDecision(decision, stock, policy);
        if (MODE_ALWAYS.equals(policy.getReviewMode()))
        {
            decision.setReviewRequired(true);
            decision.setMessage("命中强制审核策略，已生成库存调整审核单");
            return decision;
        }

        if (!DIRECTION_DECREASE.equals(direction))
        {
            return decision;
        }
        if (isBelowLowImpactThreshold(requestedQty, beforeQty, policy))
        {
            return decision;
        }
        if (requestedQty > qty(decision.getImmediateReturnableQty()))
        {
            decision.setReviewRequired(true);
            decision.setMessage("申请退回" + requestedQty + "，超过当前可立即退回"
                + qty(decision.getImmediateReturnableQty()) + "，已进入库存调整审核保护期");
        }
        return decision;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustmentReviewRequest submitAdjustmentReview(InventorySkuWarehouseStock stock,
        InventoryOverviewAdjustRequest request, InventoryAdjustmentReviewDecision decision)
    {
        if (stock == null || request == null || decision == null || !Boolean.TRUE.equals(decision.getReviewRequired()))
        {
            throw new ServiceException("库存调整审核判定不能为空");
        }
        String direction = qty(request.getTargetQty()) < qty(stock.getPlatformTotalQty()) ? DIRECTION_DECREASE
            : DIRECTION_INCREASE;
        InventoryAdjustmentReviewPolicy policy = decision.getPolicy();
        Date now = new Date();
        InventoryAdjustmentReviewRequest review = new InventoryAdjustmentReviewRequest();
        review.setReviewNo(buildReviewNo());
        review.setReviewStatus(STATUS_WAITING);
        review.setPolicyId(policy.getPolicyId());
        review.setPolicySnapshotJson(policySnapshot(policy));
        review.setStockId(stock.getStockId());
        review.setStockKey(stock.getStockKey());
        review.setSpuId(stock.getSpuId());
        review.setSkuId(stock.getSkuId());
        review.setSellerId(stock.getSellerId());
        review.setSystemSkuCode(stock.getSystemSkuCode());
        review.setProductName(stock.getProductName());
        review.setSkuName(stock.getSkuName());
        review.setWarehouseKind(stock.getWarehouseKind());
        review.setWarehouseRefType(stock.getWarehouseRefType());
        review.setWarehouseName(stock.getWarehouseName());
        review.setAdjustField(request.getAdjustField());
        review.setAdjustDirection(direction);
        review.setRequestBeforePlatformTotalQty(qty(stock.getPlatformTotalQty()));
        review.setRequestedAdjustQty(decision.getRequestedAdjustQty());
        review.setRequestExpectedAfterPlatformTotalQty(decision.getRequestExpectedAfterPlatformTotalQty());
        review.setPlatformReservedQtySnapshot(qty(stock.getPlatformReservedQty()));
        review.setSales7dQty(qty(decision.getSales7dQty()));
        review.setSales7dDailyAvg(decimal(decision.getSales7dDailyAvg()));
        review.setSales30dQty(qty(decision.getSales30dQty()));
        review.setSales30dDailyAvg(decimal(decision.getSales30dDailyAvg()));
        review.setThresholdDailyAvg(decimal(decision.getThresholdDailyAvg()));
        review.setThresholdReserveDays(decision.getThresholdReserveDays());
        review.setProtectedRetainedQty(qty(decision.getProtectedRetainedQty()));
        review.setMinRetainedQty(qty(decision.getMinRetainedQty()));
        review.setImmediateReturnableQty(qty(decision.getImmediateReturnableQty()));
        review.setTriggerReason(decision.getMessage());
        review.setSubmitTerminal("ADMIN");
        review.setSubmitUserId(currentUserId());
        review.setSubmitUserName(currentOperator());
        review.setSubmitReason(request.getReason());
        review.setSubmitTime(now);
        review.setPlannedEffectiveTime(addHours(now, defaultInt(policy.getCooldownHours(), 168)));
        review.setCreateBy(currentOperator());
        reviewMapper.insertReview(review);
        insertLog(review, "SUBMIT", "", STATUS_WAITING, request.getReason(), decision.getMessage());
        return review;
    }

    @Override
    public List<InventoryAdjustmentReviewRequest> selectReviewList(InventoryAdjustmentReviewRequest query)
    {
        return reviewMapper.selectReviewList(query);
    }

    @Override
    public InventoryAdjustmentReviewRequest selectReviewById(Long reviewId)
    {
        return requireReview(reviewId);
    }

    @Override
    public List<InventoryAdjustmentReviewOperationLog> selectReviewLogs(Long reviewId)
    {
        return reviewMapper.selectReviewLogs(reviewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustmentReviewRequest effectNow(Long reviewId, InventoryAdjustmentReviewActionRequest request)
    {
        InventoryAdjustmentReviewRequest review = requireWaitingReview(reviewId);
        assertManualEffectAllowed(review);
        InventorySkuWarehouseStock before = requireStock(review.getStockId());
        InventorySkuWarehouseStock after = copyStock(before);
        long requestedQty = qty(review.getRequestedAdjustQty());
        long actualQty;
        if (DIRECTION_DECREASE.equals(review.getAdjustDirection()))
        {
            actualQty = Math.min(requestedQty, Math.max(0, qty(before.getPlatformTotalQty())
                - qty(before.getPlatformReservedQty())));
            after.setPlatformTotalQty(qty(before.getPlatformTotalQty()) - actualQty);
        }
        else
        {
            long maxIncrease = resolveMaxIncrease(before);
            actualQty = Math.min(requestedQty, maxIncrease);
            after.setPlatformTotalQty(qty(before.getPlatformTotalQty()) + actualQty);
        }
        recalculate(after);
        after.setUpdateBy(currentOperator());
        int updatedRows = inventoryOverviewMapper.updateWarehouseStock(after);
        if (updatedRows != 1)
        {
            throw new ServiceException("库存数据已变更，请刷新后重试");
        }

        InventoryOverviewAdjustRequest ledgerRequest = new InventoryOverviewAdjustRequest();
        ledgerRequest.setStockId(after.getStockId());
        ledgerRequest.setAdjustField(FIELD_PLATFORM_TOTAL);
        ledgerRequest.setTargetQty(after.getPlatformTotalQty());
        ledgerRequest.setConfirmed(true);
        ledgerRequest.setReason(trimReason(request == null ? null : request.getReason()));
        inventoryOverviewMapper.insertLedger(buildLedger(before, after, ledgerRequest, review));
        refreshSkuReadModel(after.getSkuId());
        refreshSpuReadModel(after.getSpuId());

        String beforeStatus = review.getReviewStatus();
        review.setReviewStatus(STATUS_EFFECTIVE);
        review.setEffectiveTime(new Date());
        review.setEffectiveOperatorId(currentUserId());
        review.setEffectiveOperatorName(currentOperator());
        review.setEffectiveBeforePlatformTotalQty(qty(before.getPlatformTotalQty()));
        review.setActualEffectQty(actualQty);
        review.setUnfulfilledQty(Math.max(0, requestedQty - actualQty));
        review.setEffectiveAfterPlatformTotalQty(qty(after.getPlatformTotalQty()));
        review.setReviewReason(trimReason(request == null ? null : request.getReason()));
        review.setUpdateBy(currentOperator());
        int rows = reviewMapper.updateReviewStatus(review);
        if (rows != 1)
        {
            throw new ServiceException("审核单状态已变化，请刷新后重试");
        }
        insertLog(review, "EFFECT_NOW", beforeStatus, STATUS_EFFECTIVE, review.getReviewReason(),
            "实际生效数量：" + actualQty + "，未满足数量：" + review.getUnfulfilledQty());
        return requireReview(reviewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustmentReviewRequest reject(Long reviewId, InventoryAdjustmentReviewActionRequest request)
    {
        InventoryAdjustmentReviewRequest review = requireWaitingReview(reviewId);
        String beforeStatus = review.getReviewStatus();
        review.setReviewStatus(STATUS_REJECTED);
        review.setReviewReason(trimReason(request == null ? null : request.getReason()));
        review.setEffectiveTime(new Date());
        review.setEffectiveOperatorId(currentUserId());
        review.setEffectiveOperatorName(currentOperator());
        review.setActualEffectQty(0L);
        review.setUnfulfilledQty(qty(review.getRequestedAdjustQty()));
        review.setUpdateBy(currentOperator());
        int rows = reviewMapper.updateReviewStatus(review);
        if (rows != 1)
        {
            throw new ServiceException("审核单状态已变化，请刷新后重试");
        }
        insertLog(review, "REJECT", beforeStatus, STATUS_REJECTED, review.getReviewReason(), "审核单已驳回");
        return requireReview(reviewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryAdjustmentReviewRequest changeEffectiveTime(Long reviewId, InventoryAdjustmentReviewActionRequest request)
    {
        if (request == null || request.getPlannedEffectiveTime() == null)
        {
            throw new ServiceException("计划生效时间不能为空");
        }
        InventoryAdjustmentReviewRequest review = requireWaitingReview(reviewId);
        Date beforeTime = review.getPlannedEffectiveTime();
        review.setPlannedEffectiveTime(request.getPlannedEffectiveTime());
        review.setReviewReason(trimReason(request.getReason()));
        review.setUpdateBy(currentOperator());
        int rows = reviewMapper.updateReviewPlannedEffectiveTime(review);
        if (rows != 1)
        {
            throw new ServiceException("审核单状态已变化，请刷新后重试");
        }
        insertLog(review, "CHANGE_EFFECTIVE_TIME", STATUS_WAITING, STATUS_WAITING, review.getReviewReason(),
            "计划生效时间：" + beforeTime + " -> " + request.getPlannedEffectiveTime());
        return requireReview(reviewId);
    }

    @Override
    public int effectDueReviews()
    {
        List<InventoryAdjustmentReviewRequest> dueReviews = reviewMapper.selectDueWaitingReviews(new Date(), 100);
        if (dueReviews.isEmpty())
        {
            return 0;
        }
        IInventoryAdjustmentReviewService proxy = SpringUtils.getBean(IInventoryAdjustmentReviewService.class);
        int affected = 0;
        for (InventoryAdjustmentReviewRequest dueReview : dueReviews)
        {
            InventoryAdjustmentReviewActionRequest request = new InventoryAdjustmentReviewActionRequest();
            request.setReason("计划生效时间到期自动生效");
            try
            {
                proxy.effectNow(dueReview.getReviewId(), request);
                affected++;
            }
            catch (RuntimeException ex)
            {
                log.warn("库存调整审核单到期自动生效失败，reviewId={}, reviewNo={}",
                    dueReview.getReviewId(), dueReview.getReviewNo(), ex);
            }
        }
        return affected;
    }

    @Override
    public List<InventoryAdjustmentReviewPolicy> selectPolicyList(InventoryAdjustmentReviewPolicy query)
    {
        return reviewMapper.selectPolicyList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int savePolicy(InventoryAdjustmentReviewPolicy policy)
    {
        validatePolicy(policy);
        if (policy.getPolicyId() == null)
        {
            policy.setCreateBy(currentOperator());
            return reviewMapper.insertPolicy(policy);
        }
        policy.setUpdateBy(currentOperator());
        return reviewMapper.updatePolicy(policy);
    }

    @Override
    public List<InventoryAdjustmentReviewPolicyBinding> selectPolicyBindingList(InventoryAdjustmentReviewPolicyBinding query)
    {
        return reviewMapper.selectPolicyBindingList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int savePolicyBinding(InventoryAdjustmentReviewPolicyBinding binding)
    {
        validateBinding(binding);
        if (binding.getBindingId() == null)
        {
            binding.setCreateBy(currentOperator());
            return reviewMapper.insertPolicyBinding(binding);
        }
        binding.setUpdateBy(currentOperator());
        return reviewMapper.updatePolicyBinding(binding);
    }

    private InventoryAdjustmentReviewPolicy matchedPolicy(Long sellerId)
    {
        InventoryAdjustmentReviewPolicy policy = reviewMapper.selectMatchedPolicy(sellerId);
        if (policy != null)
        {
            return policy;
        }
        InventoryAdjustmentReviewPolicy fallback = new InventoryAdjustmentReviewPolicy();
        fallback.setPolicyId(0L);
        fallback.setPolicyName("默认库存退回保护策略");
        fallback.setPolicyStatus("ENABLED");
        fallback.setReviewMode(MODE_CONDITIONAL);
        fallback.setDirectionScope(DIRECTION_DECREASE);
        fallback.setFieldScope(FIELD_PLATFORM_TOTAL);
        fallback.setSalesWindowDays("[7,30]");
        fallback.setSalesAggregateMode("MAX_DAILY_AVG");
        fallback.setReserveDays(7);
        fallback.setCooldownHours(168);
        fallback.setAutoEffectEnabled("Y");
        fallback.setManualEffectAllowed("Y");
        return fallback;
    }

    private void fillSalesDecision(InventoryAdjustmentReviewDecision decision, InventorySkuWarehouseStock stock,
        InventoryAdjustmentReviewPolicy policy)
    {
        long sales7d = salesQty(stock.getSkuId(), 7);
        long sales30d = salesQty(stock.getSkuId(), 30);
        BigDecimal avg7d = dailyAvg(sales7d, 7);
        BigDecimal avg30d = dailyAvg(sales30d, 30);
        BigDecimal thresholdDailyAvg = BigDecimal.ZERO;
        for (Integer windowDays : parseWindows(policy.getSalesWindowDays()))
        {
            BigDecimal avg = windowDays == 30 ? avg30d : windowDays == 7 ? avg7d : dailyAvg(salesQty(stock.getSkuId(), windowDays), windowDays);
            if (avg.compareTo(thresholdDailyAvg) > 0)
            {
                thresholdDailyAvg = avg;
            }
        }
        int reserveDays = defaultInt(policy.getReserveDays(), 7);
        long protectedRetainedQty = thresholdDailyAvg.multiply(BigDecimal.valueOf(reserveDays))
            .setScale(0, RoundingMode.CEILING).longValue();
        long minRetainedQty = Math.max(protectedRetainedQty, qty(stock.getPlatformReservedQty()));
        long immediateReturnableQty = Math.max(0, qty(stock.getPlatformTotalQty()) - minRetainedQty);
        decision.setSales7dQty(sales7d);
        decision.setSales7dDailyAvg(avg7d);
        decision.setSales30dQty(sales30d);
        decision.setSales30dDailyAvg(avg30d);
        decision.setThresholdDailyAvg(thresholdDailyAvg);
        decision.setThresholdReserveDays(reserveDays);
        decision.setProtectedRetainedQty(protectedRetainedQty);
        decision.setMinRetainedQty(minRetainedQty);
        decision.setImmediateReturnableQty(immediateReturnableQty);
    }

    private boolean matchesDirection(InventoryAdjustmentReviewPolicy policy, String direction)
    {
        String scope = policy.getDirectionScope();
        return DIRECTION_BOTH.equals(scope) || direction.equals(scope);
    }

    private boolean matchesField(InventoryAdjustmentReviewPolicy policy)
    {
        String scope = policy.getFieldScope();
        return FIELD_PLATFORM_TOTAL.equals(scope) || "ALL".equals(scope);
    }

    private boolean isBelowLowImpactThreshold(long requestedQty, long beforeQty, InventoryAdjustmentReviewPolicy policy)
    {
        if (policy.getMinReturnQtyToReview() != null && requestedQty < policy.getMinReturnQtyToReview())
        {
            return true;
        }
        if (policy.getMinReturnRatioToReview() != null && beforeQty > 0)
        {
            BigDecimal ratio = BigDecimal.valueOf(requestedQty).divide(BigDecimal.valueOf(beforeQty), 4, RoundingMode.HALF_UP);
            return ratio.compareTo(policy.getMinReturnRatioToReview()) < 0;
        }
        return false;
    }

    private InventoryAdjustmentReviewRequest requireReview(Long reviewId)
    {
        if (reviewId == null)
        {
            throw new ServiceException("审核单ID不能为空");
        }
        InventoryAdjustmentReviewRequest review = reviewMapper.selectReviewById(reviewId);
        if (review == null)
        {
            throw new ServiceException("库存调整审核单不存在");
        }
        return review;
    }

    private InventoryAdjustmentReviewRequest requireWaitingReview(Long reviewId)
    {
        InventoryAdjustmentReviewRequest review = requireReview(reviewId);
        if (!STATUS_WAITING.equals(review.getReviewStatus()))
        {
            throw new ServiceException("只有等待中的审核单可以处理");
        }
        return review;
    }

    private void assertManualEffectAllowed(InventoryAdjustmentReviewRequest review)
    {
        if (review.getPlannedEffectiveTime() == null || !review.getPlannedEffectiveTime().after(new Date()))
        {
            return;
        }
        InventoryAdjustmentReviewPolicy policy = review.getPolicyId() == null
            ? null : reviewMapper.selectPolicyById(review.getPolicyId());
        if (policy != null && "N".equals(policy.getManualEffectAllowed()))
        {
            throw new ServiceException("当前策略不允许人工提前生效");
        }
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

    private InventoryStockLedger buildLedger(InventorySkuWarehouseStock before, InventorySkuWarehouseStock after,
        InventoryOverviewAdjustRequest request, InventoryAdjustmentReviewRequest review)
    {
        InventoryStockLedger ledger = new InventoryStockLedger();
        ledger.setStockId(after.getStockId());
        ledger.setStockKey(after.getStockKey());
        ledger.setSpuId(after.getSpuId());
        ledger.setSkuId(after.getSkuId());
        ledger.setSellerId(after.getSellerId());
        ledger.setWarehouseKind(after.getWarehouseKind());
        ledger.setWarehouseRefType(after.getWarehouseRefType());
        ledger.setWarehouseName(after.getWarehouseName());
        ledger.setOperationType(DIRECTION_DECREASE.equals(review.getAdjustDirection()) ? "MANUAL_DECREASE" : "MANUAL_INCREASE");
        ledger.setOperationSource("ADMIN");
        ledger.setBizType("INVENTORY_ADJUSTMENT_REVIEW");
        ledger.setBizNo(review.getReviewNo());
        ledger.setDeltaQty(qty(after.getPlatformTotalQty()) - qty(before.getPlatformTotalQty()));
        ledger.setBeforePlatformTotalQty(qty(before.getPlatformTotalQty()));
        ledger.setAfterPlatformTotalQty(qty(after.getPlatformTotalQty()));
        ledger.setBeforeAvailableQty(qty(before.getPlatformAvailableQty()));
        ledger.setAfterAvailableQty(qty(after.getPlatformAvailableQty()));
        ledger.setBeforeReservedQty(qty(before.getPlatformReservedQty()));
        ledger.setAfterReservedQty(qty(after.getPlatformReservedQty()));
        ledger.setBeforeInTransitQty(qty(before.getPlatformInTransitQty()));
        ledger.setAfterInTransitQty(qty(after.getPlatformInTransitQty()));
        ledger.setRiskConfirmed("Y");
        ledger.setRiskMessage(review.getTriggerReason());
        ledger.setReason(request.getReason());
        ledger.setOperatorId(currentUserId());
        ledger.setOperatorName(currentOperator());
        ledger.setOperateTime(new Date());
        return ledger;
    }

    private void insertLog(InventoryAdjustmentReviewRequest review, String operationType, String beforeStatus,
        String afterStatus, String reason, String summary)
    {
        InventoryAdjustmentReviewOperationLog log = new InventoryAdjustmentReviewOperationLog();
        log.setReviewId(review.getReviewId());
        log.setReviewNo(review.getReviewNo());
        log.setOperationType(operationType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setOperationReason(reason);
        log.setOperatorId(currentUserId());
        log.setOperatorName(currentOperator());
        log.setOperateTime(new Date());
        log.setChangeSummary(summary);
        reviewMapper.insertReviewLog(log);
    }

    private void validatePolicy(InventoryAdjustmentReviewPolicy policy)
    {
        if (policy == null || isBlank(policy.getPolicyName()))
        {
            throw new ServiceException("策略名称不能为空");
        }
        policy.setPolicyStatus(defaultString(policy.getPolicyStatus(), "ENABLED"));
        policy.setReviewMode(defaultString(policy.getReviewMode(), MODE_CONDITIONAL));
        policy.setDirectionScope(defaultString(policy.getDirectionScope(), DIRECTION_DECREASE));
        policy.setFieldScope(defaultString(policy.getFieldScope(), FIELD_PLATFORM_TOTAL));
        policy.setSalesWindowDays(defaultString(policy.getSalesWindowDays(), "[7,30]"));
        policy.setSalesAggregateMode(defaultString(policy.getSalesAggregateMode(), "MAX_DAILY_AVG"));
        policy.setReserveDays(defaultInt(policy.getReserveDays(), 7));
        policy.setCooldownHours(defaultInt(policy.getCooldownHours(), 168));
        policy.setAutoEffectEnabled(defaultString(policy.getAutoEffectEnabled(), "Y"));
        policy.setManualEffectAllowed(defaultString(policy.getManualEffectAllowed(), "Y"));
    }

    private void validateBinding(InventoryAdjustmentReviewPolicyBinding binding)
    {
        if (binding == null || binding.getPolicyId() == null)
        {
            throw new ServiceException("策略ID不能为空");
        }
        binding.setBindingType(defaultString(binding.getBindingType(), "SELLER"));
        if (!"GLOBAL".equals(binding.getBindingType()) && binding.getBindingIdValue() == null)
        {
            throw new ServiceException("绑定对象不能为空");
        }
        if ("GLOBAL".equals(binding.getBindingType()))
        {
            binding.setBindingIdValue(0L);
        }
        binding.setPriority(defaultInt(binding.getPriority(), 100));
        binding.setStatus(defaultString(binding.getStatus(), "ENABLED"));
    }

    private List<Integer> parseWindows(String value)
    {
        List<Integer> result = new ArrayList<>();
        if (value != null)
        {
            String[] parts = value.replace("[", "").replace("]", "").replace("\"", "").split(",");
            for (String part : parts)
            {
                try
                {
                    int days = Integer.parseInt(part.trim());
                    if (days > 0)
                    {
                        result.add(days);
                    }
                }
                catch (NumberFormatException ignored)
                {
                    // Ignore invalid window entries and fall back below.
                }
            }
        }
        if (result.isEmpty())
        {
            result.add(7);
            result.add(30);
        }
        return result;
    }

    private long salesQty(Long skuId, int days)
    {
        Long value = reviewMapper.sumSalesQty(skuId, days);
        return qty(value);
    }

    private BigDecimal dailyAvg(long salesQty, int days)
    {
        if (days <= 0)
        {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(salesQty).divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
    }

    private String buildReviewNo()
    {
        return "IAR" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
            + IdUtils.fastSimpleUUID().substring(0, 8).toUpperCase();
    }

    private String policySnapshot(InventoryAdjustmentReviewPolicy policy)
    {
        if (policy == null)
        {
            return "{}";
        }
        return "{"
            + "\"policyId\":" + policy.getPolicyId()
            + ",\"policyName\":\"" + escape(policy.getPolicyName()) + "\""
            + ",\"reviewMode\":\"" + escape(policy.getReviewMode()) + "\""
            + ",\"directionScope\":\"" + escape(policy.getDirectionScope()) + "\""
            + ",\"fieldScope\":\"" + escape(policy.getFieldScope()) + "\""
            + ",\"salesWindowDays\":\"" + escape(policy.getSalesWindowDays()) + "\""
            + ",\"reserveDays\":" + policy.getReserveDays()
            + ",\"cooldownHours\":" + policy.getCooldownHours()
            + "}";
    }

    private String escape(String value)
    {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Date addHours(Date date, int hours)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    private long resolveMaxIncrease(InventorySkuWarehouseStock stock)
    {
        if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
        {
            return Math.max(0, effectiveSourceAvailable(stock) - qty(stock.getPlatformTotalQty()));
        }
        return Long.MAX_VALUE / 4;
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
            available = Math.min(qty(stock.getPlatformTotalQty()), effectiveSourceAvailable(stock))
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

    private BigDecimal decimal(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultInt(Integer value, int defaultValue)
    {
        return value == null ? defaultValue : value;
    }

    private String defaultString(String value, String defaultValue)
    {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private String trimReason(String value)
    {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    private Long currentUserId()
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
            return isBlank(username) ? "system" : username;
        }
        catch (RuntimeException e)
        {
            return "system";
        }
    }
}
