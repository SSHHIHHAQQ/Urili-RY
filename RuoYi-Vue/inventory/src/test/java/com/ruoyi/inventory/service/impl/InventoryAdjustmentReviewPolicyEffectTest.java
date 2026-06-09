package com.ruoyi.inventory.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewDecision;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewOperationLog;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicy;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewPolicyBinding;
import com.ruoyi.inventory.domain.InventoryAdjustmentReviewRequest;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.request.InventoryAdjustmentReviewActionRequest;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;
import com.ruoyi.inventory.mapper.InventoryAdjustmentReviewMapper;

/**
 * 库存调整审核策略配置效果测试。
 */
public class InventoryAdjustmentReviewPolicyEffectTest
{
    private FakeReviewMapper reviewMapper;
    private InventoryAdjustmentReviewServiceImpl service;

    @Before
    public void setUp() throws Exception
    {
        reviewMapper = new FakeReviewMapper();
        reviewMapper.salesQtyByWindow.put(7, 210L);
        reviewMapper.salesQtyByWindow.put(30, 300L);
        service = new InventoryAdjustmentReviewServiceImpl();
        setField(service, "reviewMapper", reviewMapper);
    }

    @Test
    public void reviewModeAndDirectionScopeMatrixMustAffectReviewDecision()
    {
        String[] modes = {"DISABLED", "CONDITIONAL", "ALWAYS"};
        String[] scopes = {"DECREASE", "INCREASE", "BOTH"};
        String[] directions = {"DECREASE", "INCREASE"};

        for (String mode : modes)
        {
            for (String scope : scopes)
            {
                for (String direction : directions)
                {
                    reviewMapper.reset(policy(1L, mode, scope));
                    boolean actual = evaluate(direction).getReviewRequired();
                    boolean scopeMatches = "BOTH".equals(scope) || direction.equals(scope);
                    boolean expected = "ALWAYS".equals(mode) && scopeMatches;
                    expected = expected || ("CONDITIONAL".equals(mode)
                        && "DECREASE".equals(direction) && scopeMatches);
                    assertEquals("mode=" + mode + ", scope=" + scope + ", direction=" + direction,
                        expected, actual);
                }
            }
        }
    }

    @Test
    public void fieldScopeMustLimitPolicyToSupportedInventoryField()
    {
        InventoryAdjustmentReviewPolicy all = policy(1L, "CONDITIONAL", "DECREASE");
        all.setFieldScope("ALL");
        reviewMapper.reset(all);
        assertTrue(evaluate("DECREASE").getReviewRequired());

        InventoryAdjustmentReviewPolicy unsupported = policy(2L, "CONDITIONAL", "DECREASE");
        unsupported.setFieldScope("PLATFORM_IN_TRANSIT");
        reviewMapper.reset(unsupported);
        assertFalse(evaluate("DECREASE").getReviewRequired());
    }

    @Test
    public void salesWindowAndReserveDaysMustAffectConditionalThreshold()
    {
        InventoryAdjustmentReviewPolicy sevenDay = policy(1L, "CONDITIONAL", "DECREASE");
        sevenDay.setSalesWindowDays("[7]");
        sevenDay.setReserveDays(7);
        reviewMapper.reset(sevenDay);
        InventoryAdjustmentReviewDecision sevenDayDecision = evaluate("DECREASE");
        assertTrue(sevenDayDecision.getReviewRequired());
        assertEquals(new BigDecimal("30.0000"), sevenDayDecision.getThresholdDailyAvg());
        assertEquals(Long.valueOf(210L), sevenDayDecision.getProtectedRetainedQty());
        assertEquals(Long.valueOf(790L), sevenDayDecision.getImmediateReturnableQty());

        InventoryAdjustmentReviewPolicy thirtyDay = policy(2L, "CONDITIONAL", "DECREASE");
        thirtyDay.setSalesWindowDays("[30]");
        thirtyDay.setReserveDays(7);
        reviewMapper.reset(thirtyDay);
        InventoryAdjustmentReviewDecision thirtyDayDecision = evaluate("DECREASE");
        assertFalse(thirtyDayDecision.getReviewRequired());
        assertEquals(new BigDecimal("10.0000"), thirtyDayDecision.getThresholdDailyAvg());
        assertEquals(Long.valueOf(70L), thirtyDayDecision.getProtectedRetainedQty());
        assertEquals(Long.valueOf(930L), thirtyDayDecision.getImmediateReturnableQty());

        InventoryAdjustmentReviewPolicy oneDayReserve = policy(3L, "CONDITIONAL", "DECREASE");
        oneDayReserve.setSalesWindowDays("[7]");
        oneDayReserve.setReserveDays(1);
        reviewMapper.reset(oneDayReserve);
        assertFalse(evaluate("DECREASE").getReviewRequired());

        InventoryAdjustmentReviewPolicy thirtyDayReserve = policy(4L, "CONDITIONAL", "DECREASE");
        thirtyDayReserve.setSalesWindowDays("[7]");
        thirtyDayReserve.setReserveDays(30);
        reviewMapper.reset(thirtyDayReserve);
        InventoryAdjustmentReviewDecision reserveDecision = evaluate("DECREASE");
        assertTrue(reserveDecision.getReviewRequired());
        assertEquals(Long.valueOf(900L), reserveDecision.getProtectedRetainedQty());
        assertEquals(Long.valueOf(100L), reserveDecision.getImmediateReturnableQty());
    }

    @Test
    public void reservedStockAndLowImpactThresholdsMustSuppressConditionalReview()
    {
        InventoryAdjustmentReviewPolicy reservedPolicy = policy(1L, "CONDITIONAL", "DECREASE");
        reservedPolicy.setSalesWindowDays("[30]");
        reservedPolicy.setReserveDays(7);
        reviewMapper.reset(reservedPolicy);
        InventorySkuWarehouseStock reservedStock = stock();
        reservedStock.setPlatformReservedQty(900L);
        InventoryAdjustmentReviewDecision reservedDecision = service.evaluateAdjustment(reservedStock,
            request("DECREASE"));
        assertTrue(reservedDecision.getReviewRequired());
        assertEquals(Long.valueOf(900L), reservedDecision.getMinRetainedQty());
        assertEquals(Long.valueOf(100L), reservedDecision.getImmediateReturnableQty());

        InventoryAdjustmentReviewPolicy minQtyPolicy = policy(2L, "CONDITIONAL", "DECREASE");
        minQtyPolicy.setMinReturnQtyToReview(900L);
        reviewMapper.reset(minQtyPolicy);
        assertFalse(evaluate("DECREASE").getReviewRequired());
        minQtyPolicy.setMinReturnQtyToReview(850L);
        assertTrue(evaluate("DECREASE").getReviewRequired());

        InventoryAdjustmentReviewPolicy minRatioPolicy = policy(3L, "CONDITIONAL", "DECREASE");
        minRatioPolicy.setMinReturnRatioToReview(new BigDecimal("0.9000"));
        reviewMapper.reset(minRatioPolicy);
        assertFalse(evaluate("DECREASE").getReviewRequired());
        minRatioPolicy.setMinReturnRatioToReview(new BigDecimal("0.8500"));
        assertTrue(evaluate("DECREASE").getReviewRequired());
    }

    @Test
    public void sellerBindingsMustOverrideGlobalAndDisabledBindingsMustFallBack()
    {
        InventoryAdjustmentReviewPolicy globalConditional = policy(1L, "CONDITIONAL", "DECREASE");
        InventoryAdjustmentReviewPolicy sellerDisabledReview = policy(2L, "DISABLED", "BOTH");
        reviewMapper.reset(globalConditional, sellerDisabledReview);
        reviewMapper.bind(global(1L, 100));
        reviewMapper.bind(seller(2L, 100L, 1));
        assertFalse(evaluate("DECREASE").getReviewRequired());

        reviewMapper.bindings.clear();
        InventoryAdjustmentReviewPolicy sellerAlways = policy(3L, "ALWAYS", "BOTH");
        reviewMapper.reset(globalConditional, sellerAlways);
        reviewMapper.bind(global(1L, 100));
        InventoryAdjustmentReviewPolicyBinding disabledSellerBinding = seller(3L, 100L, 1);
        disabledSellerBinding.setStatus("DISABLED");
        reviewMapper.bind(disabledSellerBinding);
        assertTrue(evaluate("DECREASE").getReviewRequired());

        reviewMapper.bindings.clear();
        sellerAlways.setPolicyStatus("DISABLED");
        reviewMapper.reset(globalConditional, sellerAlways);
        reviewMapper.bind(global(1L, 100));
        reviewMapper.bind(seller(3L, 100L, 1));
        assertTrue(evaluate("DECREASE").getReviewRequired());

        reviewMapper.bindings.clear();
        reviewMapper.reset(sellerAlways);
        reviewMapper.bind(seller(3L, 100L, 1));
        assertFalse("无启用匹配策略时不能再回退成默认强制审核", evaluate("DECREASE").getReviewRequired());
    }

    @Test
    public void cooldownHoursMustSetReviewPlannedEffectiveTime()
    {
        InventoryAdjustmentReviewPolicy policy = policy(1L, "ALWAYS", "DECREASE");
        policy.setCooldownHours(3);
        reviewMapper.reset(policy);
        InventoryAdjustmentReviewDecision decision = evaluate("DECREASE");
        Date before = new Date(System.currentTimeMillis() + 2L * 60 * 60 * 1000);
        InventoryAdjustmentReviewRequest review = service.submitAdjustmentReview(stock(), request("DECREASE"), decision);
        Date after = new Date(System.currentTimeMillis() + 4L * 60 * 60 * 1000);

        assertNotNull(review.getReviewId());
        assertTrue(review.getPlannedEffectiveTime().after(before));
        assertTrue(review.getPlannedEffectiveTime().before(after));
        assertEquals(Long.valueOf(850L), review.getRequestedAdjustQty());
        assertEquals(1, reviewMapper.logs.size());
    }

    @Test
    public void manualAndAutoEffectFlagsMustControlProcessingPaths() throws Exception
    {
        InventoryAdjustmentReviewPolicy manualBlocked = policy(1L, "ALWAYS", "DECREASE");
        manualBlocked.setManualEffectAllowed("N");
        reviewMapper.reset(manualBlocked);
        InventoryAdjustmentReviewRequest review = waitingReview(1L);
        review.setPolicyId(1L);
        review.setPlannedEffectiveTime(new Date(System.currentTimeMillis() + 60_000L));
        reviewMapper.reviews.put(1L, review);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.effectNow(1L, new InventoryAdjustmentReviewActionRequest()));
        assertTrue(ex.getMessage().contains("不允许人工提前生效"));

        String xml = Files.readString(Path.of("src/main/resources/mapper/inventory/InventoryAdjustmentReviewMapper.xml"),
            StandardCharsets.UTF_8);
        assertTrue(xml.contains("coalesce(p.auto_effect_enabled, 'Y') = 'Y'"));
        assertTrue(xml.contains("r.planned_effective_time &lt;= #{now}"));
    }

    @Test
    public void savePolicyAndBindingMustDefaultAndRejectInvalidConfig()
    {
        InventoryAdjustmentReviewPolicy defaultedPolicy = new InventoryAdjustmentReviewPolicy();
        defaultedPolicy.setPolicyName("默认值策略");
        service.savePolicy(defaultedPolicy);
        assertEquals("ENABLED", defaultedPolicy.getPolicyStatus());
        assertEquals("CONDITIONAL", defaultedPolicy.getReviewMode());
        assertEquals("DECREASE", defaultedPolicy.getDirectionScope());
        assertEquals("PLATFORM_TOTAL", defaultedPolicy.getFieldScope());
        assertEquals("[7,30]", defaultedPolicy.getSalesWindowDays());
        assertEquals(Integer.valueOf(7), defaultedPolicy.getReserveDays());
        assertEquals(Integer.valueOf(168), defaultedPolicy.getCooldownHours());
        assertEquals("Y", defaultedPolicy.getAutoEffectEnabled());
        assertEquals("Y", defaultedPolicy.getManualEffectAllowed());

        assertInvalidPolicy("reviewMode", "UNKNOWN");
        assertInvalidPolicy("directionScope", "UNKNOWN");
        assertInvalidPolicy("fieldScope", "UNKNOWN");
        assertInvalidPolicy("salesWindowDays", "[]");
        assertInvalidPolicy("salesAggregateMode", "SUM");
        assertInvalidPolicy("policyStatus", "UNKNOWN");
        assertInvalidPolicy("autoEffectEnabled", "UNKNOWN");
        assertInvalidPolicy("manualEffectAllowed", "UNKNOWN");

        InventoryAdjustmentReviewPolicy negativeReserve = basePolicy();
        negativeReserve.setReserveDays(-1);
        assertThrows(ServiceException.class, () -> service.savePolicy(negativeReserve));
        InventoryAdjustmentReviewPolicy negativeCooldown = basePolicy();
        negativeCooldown.setCooldownHours(-1);
        assertThrows(ServiceException.class, () -> service.savePolicy(negativeCooldown));
        InventoryAdjustmentReviewPolicy negativeMinQty = basePolicy();
        negativeMinQty.setMinReturnQtyToReview(-1L);
        assertThrows(ServiceException.class, () -> service.savePolicy(negativeMinQty));
        InventoryAdjustmentReviewPolicy invalidRatio = basePolicy();
        invalidRatio.setMinReturnRatioToReview(new BigDecimal("1.1000"));
        assertThrows(ServiceException.class, () -> service.savePolicy(invalidRatio));

        InventoryAdjustmentReviewPolicyBinding globalBinding = new InventoryAdjustmentReviewPolicyBinding();
        globalBinding.setPolicyId(1L);
        globalBinding.setBindingType("GLOBAL");
        service.savePolicyBinding(globalBinding);
        assertEquals(Long.valueOf(0L), globalBinding.getBindingIdValue());
        assertEquals(Integer.valueOf(100), globalBinding.getPriority());
        assertEquals("ENABLED", globalBinding.getStatus());

        InventoryAdjustmentReviewPolicyBinding sellerBinding = new InventoryAdjustmentReviewPolicyBinding();
        sellerBinding.setPolicyId(1L);
        sellerBinding.setBindingType("SELLER");
        assertThrows(ServiceException.class, () -> service.savePolicyBinding(sellerBinding));
        sellerBinding.setBindingIdValue(0L);
        assertThrows(ServiceException.class, () -> service.savePolicyBinding(sellerBinding));
        sellerBinding.setBindingIdValue(100L);
        sellerBinding.setPriority(0);
        assertThrows(ServiceException.class, () -> service.savePolicyBinding(sellerBinding));
        sellerBinding.setPriority(1);
        sellerBinding.setStatus("UNKNOWN");
        assertThrows(ServiceException.class, () -> service.savePolicyBinding(sellerBinding));
    }

    private InventoryAdjustmentReviewDecision evaluate(String direction)
    {
        return service.evaluateAdjustment(stock(), request(direction));
    }

    private InventorySkuWarehouseStock stock()
    {
        InventorySkuWarehouseStock stock = new InventorySkuWarehouseStock();
        stock.setStockId(1L);
        stock.setStockKey("SKU-1|WH-1");
        stock.setSpuId(10L);
        stock.setSkuId(20L);
        stock.setSellerId(100L);
        stock.setSystemSkuCode("SYS-SKU-1");
        stock.setProductName("商品");
        stock.setSkuName("规格");
        stock.setWarehouseKind("third_party");
        stock.setWarehouseRefType("THIRD_PARTY");
        stock.setWarehouseName("三方仓");
        stock.setPlatformTotalQty(1000L);
        stock.setPlatformReservedQty(0L);
        stock.setPlatformAvailableQty(1000L);
        stock.setVersion(0);
        return stock;
    }

    private InventoryOverviewAdjustRequest request(String direction)
    {
        InventoryOverviewAdjustRequest request = new InventoryOverviewAdjustRequest();
        request.setStockId(1L);
        request.setAdjustField("PLATFORM_TOTAL");
        request.setTargetQty("INCREASE".equals(direction) ? 1100L : 150L);
        request.setConfirmed(true);
        request.setReason("测试");
        return request;
    }

    private InventoryAdjustmentReviewPolicy basePolicy()
    {
        return policy(1L, "CONDITIONAL", "DECREASE");
    }

    private InventoryAdjustmentReviewPolicy policy(Long policyId, String mode, String directionScope)
    {
        InventoryAdjustmentReviewPolicy policy = new InventoryAdjustmentReviewPolicy();
        policy.setPolicyId(policyId);
        policy.setPolicyName("策略" + policyId);
        policy.setPolicyStatus("ENABLED");
        policy.setReviewMode(mode);
        policy.setDirectionScope(directionScope);
        policy.setFieldScope("PLATFORM_TOTAL");
        policy.setSalesWindowDays("[7,30]");
        policy.setSalesAggregateMode("MAX_DAILY_AVG");
        policy.setReserveDays(7);
        policy.setCooldownHours(168);
        policy.setAutoEffectEnabled("Y");
        policy.setManualEffectAllowed("Y");
        return policy;
    }

    private InventoryAdjustmentReviewPolicyBinding global(Long policyId, int priority)
    {
        InventoryAdjustmentReviewPolicyBinding binding = new InventoryAdjustmentReviewPolicyBinding();
        binding.setBindingId((long) reviewMapper.bindings.size() + 1);
        binding.setPolicyId(policyId);
        binding.setBindingType("GLOBAL");
        binding.setBindingIdValue(0L);
        binding.setPriority(priority);
        binding.setStatus("ENABLED");
        return binding;
    }

    private InventoryAdjustmentReviewPolicyBinding seller(Long policyId, Long sellerId, int priority)
    {
        InventoryAdjustmentReviewPolicyBinding binding = new InventoryAdjustmentReviewPolicyBinding();
        binding.setBindingId((long) reviewMapper.bindings.size() + 1);
        binding.setPolicyId(policyId);
        binding.setBindingType("SELLER");
        binding.setBindingIdValue(sellerId);
        binding.setPriority(priority);
        binding.setStatus("ENABLED");
        return binding;
    }

    private InventoryAdjustmentReviewRequest waitingReview(Long reviewId)
    {
        InventoryAdjustmentReviewRequest review = new InventoryAdjustmentReviewRequest();
        review.setReviewId(reviewId);
        review.setReviewNo("IAR-TEST");
        review.setReviewStatus("WAITING");
        review.setStockId(1L);
        review.setRequestedAdjustQty(100L);
        review.setAdjustDirection("DECREASE");
        review.setVersion(0);
        return review;
    }

    private void assertInvalidPolicy(String fieldName, Object value)
    {
        InventoryAdjustmentReviewPolicy policy = basePolicy();
        switch (fieldName)
        {
            case "reviewMode" -> policy.setReviewMode(String.valueOf(value));
            case "directionScope" -> policy.setDirectionScope(String.valueOf(value));
            case "fieldScope" -> policy.setFieldScope(String.valueOf(value));
            case "salesWindowDays" -> policy.setSalesWindowDays(String.valueOf(value));
            case "salesAggregateMode" -> policy.setSalesAggregateMode(String.valueOf(value));
            case "policyStatus" -> policy.setPolicyStatus(String.valueOf(value));
            case "autoEffectEnabled" -> policy.setAutoEffectEnabled(String.valueOf(value));
            case "manualEffectAllowed" -> policy.setManualEffectAllowed(String.valueOf(value));
            default -> throw new IllegalArgumentException(fieldName);
        }
        assertThrows(ServiceException.class, () -> service.savePolicy(policy));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class FakeReviewMapper implements InventoryAdjustmentReviewMapper
    {
        private final Map<Long, InventoryAdjustmentReviewPolicy> policies = new HashMap<>();
        private final List<InventoryAdjustmentReviewPolicyBinding> bindings = new ArrayList<>();
        private final Map<Integer, Long> salesQtyByWindow = new HashMap<>();
        private final Map<Long, InventoryAdjustmentReviewRequest> reviews = new HashMap<>();
        private final List<InventoryAdjustmentReviewOperationLog> logs = new ArrayList<>();
        private long nextReviewId = 1L;

        private void reset(InventoryAdjustmentReviewPolicy... rows)
        {
            policies.clear();
            bindings.clear();
            for (InventoryAdjustmentReviewPolicy row : rows)
            {
                policies.put(row.getPolicyId(), row);
                bindings.add(defaultGlobalBinding(row.getPolicyId()));
            }
        }

        private InventoryAdjustmentReviewPolicyBinding defaultGlobalBinding(Long policyId)
        {
            InventoryAdjustmentReviewPolicyBinding binding = new InventoryAdjustmentReviewPolicyBinding();
            binding.setBindingId((long) bindings.size() + 1);
            binding.setPolicyId(policyId);
            binding.setBindingType("GLOBAL");
            binding.setBindingIdValue(0L);
            binding.setPriority(100);
            binding.setStatus("ENABLED");
            return binding;
        }

        private void bind(InventoryAdjustmentReviewPolicyBinding binding)
        {
            bindings.add(binding);
        }

        @Override
        public InventoryAdjustmentReviewPolicy selectMatchedPolicy(Long sellerId)
        {
            return bindings.stream()
                .filter(binding -> "ENABLED".equals(binding.getStatus()))
                .filter(binding -> "GLOBAL".equals(binding.getBindingType())
                    || ("SELLER".equals(binding.getBindingType()) && binding.getBindingIdValue().equals(sellerId)))
                .map(binding -> new PolicyBindingMatch(policies.get(binding.getPolicyId()), binding))
                .filter(match -> match.policy != null && "ENABLED".equals(match.policy.getPolicyStatus()))
                .sorted(Comparator
                    .comparingInt((PolicyBindingMatch match) -> "SELLER".equals(match.binding.getBindingType()) ? 1 : 2)
                    .thenComparingInt(match -> match.binding.getPriority())
                    .thenComparingLong(match -> match.binding.getBindingId()))
                .map(match -> match.policy)
                .findFirst()
                .orElse(null);
        }

        @Override
        public Long sumSalesQty(Long skuId, Integer days)
        {
            return salesQtyByWindow.getOrDefault(days, 0L);
        }

        @Override
        public int insertReview(InventoryAdjustmentReviewRequest review)
        {
            review.setReviewId(nextReviewId++);
            reviews.put(review.getReviewId(), review);
            return 1;
        }

        @Override
        public int insertReviewLog(InventoryAdjustmentReviewOperationLog log)
        {
            logs.add(log);
            return 1;
        }

        @Override
        public InventoryAdjustmentReviewRequest selectReviewById(Long reviewId)
        {
            return reviews.get(reviewId);
        }

        @Override
        public InventoryAdjustmentReviewPolicy selectPolicyById(Long policyId)
        {
            return policies.get(policyId);
        }

        @Override
        public int insertPolicy(InventoryAdjustmentReviewPolicy policy)
        {
            policy.setPolicyId(policy.getPolicyId() == null ? (long) policies.size() + 1 : policy.getPolicyId());
            policies.put(policy.getPolicyId(), policy);
            return 1;
        }

        @Override
        public int updatePolicy(InventoryAdjustmentReviewPolicy policy)
        {
            policies.put(policy.getPolicyId(), policy);
            return 1;
        }

        @Override
        public int insertPolicyBinding(InventoryAdjustmentReviewPolicyBinding binding)
        {
            binding.setBindingId(binding.getBindingId() == null ? (long) bindings.size() + 1 : binding.getBindingId());
            bindings.add(binding);
            return 1;
        }

        @Override
        public int updatePolicyBinding(InventoryAdjustmentReviewPolicyBinding binding)
        {
            bindings.removeIf(row -> row.getBindingId().equals(binding.getBindingId()));
            bindings.add(binding);
            return 1;
        }

        @Override
        public List<InventoryAdjustmentReviewRequest> selectReviewList(InventoryAdjustmentReviewRequest query)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateReviewStatus(InventoryAdjustmentReviewRequest review)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateReviewPlannedEffectiveTime(InventoryAdjustmentReviewRequest review)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<InventoryAdjustmentReviewOperationLog> selectReviewLogs(Long reviewId)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<InventoryAdjustmentReviewRequest> selectDueWaitingReviews(Date now, Integer limit)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<InventoryAdjustmentReviewPolicy> selectPolicyList(InventoryAdjustmentReviewPolicy query)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<InventoryAdjustmentReviewPolicyBinding> selectPolicyBindingList(InventoryAdjustmentReviewPolicyBinding query)
        {
            throw new UnsupportedOperationException();
        }

        private record PolicyBindingMatch(InventoryAdjustmentReviewPolicy policy,
                                          InventoryAdjustmentReviewPolicyBinding binding)
        {
        }
    }
}
