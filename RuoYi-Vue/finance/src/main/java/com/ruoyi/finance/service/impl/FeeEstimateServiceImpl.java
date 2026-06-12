package com.ruoyi.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.finance.domain.FeeEstimateChannelResult;
import com.ruoyi.finance.domain.FeeEstimateExternalRequest;
import com.ruoyi.finance.domain.FeeEstimateExternalResult;
import com.ruoyi.finance.domain.FeeEstimateOptions;
import com.ruoyi.finance.domain.FeeEstimatePackageSummary;
import com.ruoyi.finance.domain.FeeEstimateQuoteSchemeOption;
import com.ruoyi.finance.domain.FeeEstimateResolveSummary;
import com.ruoyi.finance.domain.FeeEstimateResponse;
import com.ruoyi.finance.domain.FeeEstimateRouteCandidate;
import com.ruoyi.finance.domain.FeeEstimateSkuSnapshot;
import com.ruoyi.finance.domain.FeeEstimateSkuWarehouseCandidate;
import com.ruoyi.finance.domain.QuoteScheme;
import com.ruoyi.finance.domain.QuoteSchemeChannel;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.domain.QuoteSchemeScope;
import com.ruoyi.finance.domain.QuoteSchemeWarehouse;
import com.ruoyi.finance.domain.query.FeeEstimateSkuQuery;
import com.ruoyi.finance.domain.request.FeeEstimatePackageLine;
import com.ruoyi.finance.domain.request.FeeEstimateRequest;
import com.ruoyi.finance.mapper.QuoteSchemeMapper;
import com.ruoyi.finance.service.FinanceFeeEstimateExternalService;
import com.ruoyi.finance.service.FinanceFeeEstimateLogisticsLookupService;
import com.ruoyi.finance.service.FinanceFeeEstimateSkuLookupService;
import com.ruoyi.finance.service.IFeeEstimateService;
import com.ruoyi.finance.service.QuoteSchemeBuyerLookupService;
import com.ruoyi.finance.service.QuoteSchemeWarehouseLookupService;

@Service
public class FeeEstimateServiceImpl implements IFeeEstimateService
{
    private static final String MODE_SKU = "SKU";

    private static final String MODE_MANUAL = "MANUAL";

    private static final String SELECTION_MANUAL = "MANUAL";

    private static final String SELECTION_AUTO_BEST = "AUTO_BEST";

    private static final String VIEW_OPERATIONS = "OPERATIONS";

    private static final String VIEW_BUYER_SIMULATION = "BUYER_SIMULATION";

    private static final String TYPE_BILLING = "BILLING";

    private static final String SCOPE_ALL_BUYERS = "ALL_BUYERS";

    private static final String SCOPE_BUYER_LEVEL = "BUYER_LEVEL";

    private static final String SCOPE_BUYER = "BUYER";

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String WAREHOUSE_INCLUDE = "INCLUDE";

    private static final String WAREHOUSE_STATUS_NORMAL = "0";

    private static final String SOURCE_EXTERNAL_ESTIMATE = "EXTERNAL_ESTIMATE";

    private static final String SOURCE_INTERNAL_RATE = "INTERNAL_RATE";

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final BigDecimal VOLUME_WEIGHT_DIVISOR = new BigDecimal("5000");

    @Autowired
    private QuoteSchemeMapper quoteSchemeMapper;

    @Autowired
    private ObjectProvider<FinanceFeeEstimateSkuLookupService> skuLookupServiceProvider;

    @Autowired
    private ObjectProvider<FinanceFeeEstimateLogisticsLookupService> logisticsLookupServiceProvider;

    @Autowired
    private ObjectProvider<FinanceFeeEstimateExternalService> externalServiceProvider;

    @Autowired
    private ObjectProvider<QuoteSchemeBuyerLookupService> buyerLookupServiceProvider;

    @Autowired
    private ObjectProvider<QuoteSchemeWarehouseLookupService> warehouseLookupServiceProvider;

    @Override
    public FeeEstimateOptions selectOptions(Long schemeId)
    {
        List<QuoteScheme> schemes = selectEnabledQuoteSchemes();
        FeeEstimateOptions options = new FeeEstimateOptions();
        options.setQuoteSchemes(schemes.stream().map(this::toSchemeOption).collect(Collectors.toList()));
        options.setBuyers(selectBuyerOptions());
        options.setWarehouses(selectWarehouseOptions());
        options.setCustomerChannels(selectAllEnabledCustomerChannelOptions(schemes));

        QuoteScheme selectedScheme = resolveSelectedScheme(schemes, schemeId);
        if (selectedScheme != null)
        {
            FeeEstimateQuoteSchemeOption selectedOption = toSchemeOption(selectedScheme);
            List<QuoteSchemeOption> channels = selectEnabledSchemeChannelOptions(selectedScheme.getSchemeId());
            selectedOption.setChannels(channels);
            options.setSelectedScheme(selectedOption);
            options.setChannels(channels);
        }
        else
        {
            options.setChannels(Collections.emptyList());
        }
        return options;
    }

    @Override
    public List<FeeEstimateSkuSnapshot> selectSkuSnapshots(FeeEstimateSkuQuery query)
    {
        FeeEstimateSkuQuery normalizedQuery = query == null ? new FeeEstimateSkuQuery() : query;
        normalizedQuery.setSourceWarehouseCode(StringUtils.trimToNull(normalizedQuery.getSourceWarehouseCode()));
        normalizedQuery.setSkuCode(StringUtils.trimToNull(normalizedQuery.getSkuCode()));
        normalizedQuery.setProductName(StringUtils.trimToNull(normalizedQuery.getProductName()));
        return requireSkuLookupService().selectSkuSnapshots(normalizedQuery);
    }

    @Override
    public FeeEstimateResponse calculate(FeeEstimateRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("费用试算请求不能为空");
        }
        String estimateView = normalizeEstimateView(request.getEstimateView());
        String selectionMode = normalizeSelectionMode(request.getSelectionMode());
        if (VIEW_BUYER_SIMULATION.equals(estimateView) || SELECTION_AUTO_BEST.equals(selectionMode))
        {
            return calculateBuyerSimulation(request, estimateView, selectionMode);
        }

        String packageInputMode = normalizeInputMode(request.getPackageInputMode());
        String originWarehouseCode = requireCode(request.getOriginWarehouseCode(), "发货仓");
        validateOriginWarehouse(originWarehouseCode);
        requireCode(request.getDestinationCountryCode(), "到货国家/地区");
        requireCode(request.getDestinationPostalCode(), "到货邮编");

        QuoteScheme scheme = requireEnabledScheme(request.getQuoteSchemeId());
        validateSchemeWarehouseScope(scheme, originWarehouseCode);
        FeeEstimateCalculationContext packageContext = buildPackageContext(packageInputMode, request.getPackageLines());
        List<QuoteSchemeChannel> channels = selectTargetChannels(scheme.getSchemeId(), request.getChannelCodes());
        List<String> customerChannelCodes = channels.stream()
            .map(QuoteSchemeChannel::getCustomerChannelCode)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        List<FeeEstimateRouteCandidate> routeCandidates = customerChannelCodes.isEmpty()
            ? Collections.emptyList()
            : requireLogisticsLookupService().selectRouteCandidates(customerChannelCodes,
                List.of(originWarehouseCode), request.getBuyerId(), false);
        FeeEstimateSkuWarehouseCandidate originWarehouse = buildManualWarehouseCandidate(originWarehouseCode);

        FeeEstimateResponse response = new FeeEstimateResponse();
        response.setRequestNo(generateRequestNo());
        response.setEstimateView(VIEW_OPERATIONS);
        response.setSelectionMode(SELECTION_MANUAL);
        response.setOriginWarehouseCode(originWarehouseCode);
        response.setQuoteSchemeId(scheme.getSchemeId());
        response.setQuoteSchemeName(scheme.getSchemeName());
        response.setPackageInputMode(packageInputMode);
        response.setPackageSummary(packageContext.packageSummary);
        response.setSkuSnapshots(packageContext.skuSnapshots);
        List<FeeEstimateChannelResult> results = new ArrayList<>();
        List<FeeEstimateRouteCandidate> responseRoutes = new ArrayList<>();
        for (QuoteSchemeChannel channel : channels)
        {
            List<FeeEstimateRouteCandidate> matchedRoutes = routeCandidates.stream()
                .filter(route -> StringUtils.equals(route.getCustomerChannelCode(), channel.getCustomerChannelCode()))
                .filter(route -> StringUtils.isBlank(route.getWarehouseCode())
                    || StringUtils.equals(route.getWarehouseCode(), originWarehouseCode))
                .collect(Collectors.toList());
            if (matchedRoutes.isEmpty())
            {
                FeeEstimateRouteCandidate route = failedSchemeRoute(originWarehouse, scheme, channel,
                    "ROUTE_CANDIDATE_MISSING", "客户渠道没有匹配到当前仓库可用的系统渠道");
                responseRoutes.add(route);
                results.add(buildRouteFailureResult(scheme, channel, packageContext.packageSummary, route,
                    SELECTION_MANUAL));
                continue;
            }
            for (FeeEstimateRouteCandidate matchedRoute : matchedRoutes)
            {
                FeeEstimateRouteCandidate route = enrichRouteCandidate(matchedRoute, originWarehouse, scheme, channel);
                responseRoutes.add(route);
                if (Boolean.TRUE.equals(route.getExecutable()))
                {
                    FeeEstimateChannelResult result = buildEstimateResult(scheme, channel, packageContext.packageSummary,
                        request, route, SELECTION_MANUAL);
                    enrichResultWithRoute(result, SELECTION_MANUAL, route);
                    results.add(result);
                }
                else
                {
                    results.add(buildRouteFailureResult(scheme, channel, packageContext.packageSummary, route,
                        SELECTION_MANUAL));
                }
            }
        }
        response.setRouteCandidates(responseRoutes);
        response.setResults(results);
        return response;
    }

    private FeeEstimateResponse calculateBuyerSimulation(FeeEstimateRequest request, String estimateView,
        String selectionMode)
    {
        long startedAt = System.currentTimeMillis();
        String packageInputMode = normalizeInputMode(request.getPackageInputMode());
        if (!MODE_SKU.equals(packageInputMode))
        {
            throw new ServiceException("买家模拟当前只支持 SKU 录入模式");
        }
        requireCode(request.getDestinationCountryCode(), "到货国家/地区");
        requireCode(request.getDestinationPostalCode(), "到货邮编");
        boolean autoMode = SELECTION_AUTO_BEST.equals(selectionMode);
        Set<String> requestedWarehouseCodes = resolveRequestedWarehouseCodes(request, autoMode);
        for (String warehouseCode : requestedWarehouseCodes)
        {
            validateOriginWarehouse(warehouseCode);
        }
        String requestedCustomerChannelCode = resolveRequestedCustomerChannelCode(request, autoMode);

        QuoteSchemeOption buyer = requireBuyerOption(request.getBuyerId());
        FeeEstimateCalculationContext packageContext = buildPackageContext(packageInputMode, request.getPackageLines());
        List<Long> skuIds = packageContext.skuSnapshots.stream()
            .map(FeeEstimateSkuSnapshot::getSkuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        List<FeeEstimateSkuWarehouseCandidate> warehouseCandidates = resolveCommonWarehouseCandidates(
            skuIds, requestedWarehouseCodes);
        Set<String> warehouseCodes = warehouseCandidates.stream()
            .map(FeeEstimateSkuWarehouseCandidate::getWarehouseCode)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<QuoteScheme> activeBillingSchemes = selectActiveBillingSchemes(buyer, new Date());
        Map<Long, Set<String>> warehouseCodesByScheme = buildSchemeWarehouseCodeMap(activeBillingSchemes);
        Map<String, QuoteScheme> winningSchemesByWarehouse = selectWinningSchemesByWarehouse(
            warehouseCodes, activeBillingSchemes, warehouseCodesByScheme);

        List<FeeEstimateRouteCandidate> routeCandidates = new ArrayList<>();
        List<FeeEstimateChannelResult> results = new ArrayList<>();
        int schemeChannelCandidateCount = 0;
        if (winningSchemesByWarehouse.isEmpty())
        {
            for (FeeEstimateSkuWarehouseCandidate warehouse : warehouseCandidates)
            {
                routeCandidates.add(failedWarehouseRoute(warehouse, "QUOTE_SCHEME_MISSING",
                    "当前买家、仓库和时间没有匹配到可用计费报价方案"));
            }
        }
        else
        {
            Set<String> customerChannelCodes = new LinkedHashSet<>();
            Map<Long, List<QuoteSchemeChannel>> channelsByScheme = new LinkedHashMap<>();
            for (QuoteScheme scheme : new LinkedHashSet<>(winningSchemesByWarehouse.values()))
            {
                List<QuoteSchemeChannel> channels = filterSchemeChannelsBySelection(
                    selectEnabledSchemeChannels(scheme.getSchemeId()), requestedCustomerChannelCode);
                channelsByScheme.put(scheme.getSchemeId(), channels);
                schemeChannelCandidateCount += channels.size();
                channels.stream()
                    .map(QuoteSchemeChannel::getCustomerChannelCode)
                    .filter(StringUtils::isNotBlank)
                    .forEach(customerChannelCodes::add);
            }

            List<FeeEstimateRouteCandidate> logisticsCandidates = customerChannelCodes.isEmpty()
                ? Collections.emptyList()
                : requireLogisticsLookupService().selectRouteCandidates(new ArrayList<>(customerChannelCodes),
                    new ArrayList<>(winningSchemesByWarehouse.keySet()), buyer.getId(), autoMode);
            for (Map.Entry<String, QuoteScheme> entry : winningSchemesByWarehouse.entrySet())
            {
                String warehouseCode = entry.getKey();
                QuoteScheme scheme = entry.getValue();
                FeeEstimateSkuWarehouseCandidate warehouse = findWarehouseCandidate(warehouseCandidates, warehouseCode);
                List<QuoteSchemeChannel> schemeChannels = channelsByScheme.getOrDefault(scheme.getSchemeId(),
                    Collections.emptyList());
                if (schemeChannels.isEmpty())
                {
                    routeCandidates.add(failedSchemeRoute(warehouse, scheme, null, "SCHEME_CHANNEL_MISSING",
                        "报价方案未配置可用客户物流渠道"));
                    continue;
                }
                for (QuoteSchemeChannel channel : schemeChannels)
                {
                    List<FeeEstimateRouteCandidate> matchedRoutes = logisticsCandidates.stream()
                        .filter(route -> StringUtils.equals(route.getCustomerChannelCode(),
                            channel.getCustomerChannelCode()))
                        .filter(route -> StringUtils.isBlank(route.getWarehouseCode())
                            || StringUtils.equals(route.getWarehouseCode(), warehouseCode))
                        .collect(Collectors.toList());
                    if (matchedRoutes.isEmpty())
                    {
                        FeeEstimateRouteCandidate route = failedSchemeRoute(warehouse, scheme, channel,
                            "ROUTE_CANDIDATE_MISSING", "客户渠道没有匹配到当前仓库可用的系统渠道");
                        routeCandidates.add(route);
                        results.add(buildRouteFailureResult(scheme, channel, packageContext.packageSummary, route,
                            selectionMode));
                        continue;
                    }
                    for (FeeEstimateRouteCandidate matchedRoute : matchedRoutes)
                    {
                        FeeEstimateRouteCandidate route = enrichRouteCandidate(matchedRoute, warehouse, scheme, channel);
                        routeCandidates.add(route);
                        if (Boolean.TRUE.equals(route.getExecutable()))
                        {
                            FeeEstimateChannelResult result = buildEstimateResult(scheme, channel,
                                packageContext.packageSummary, request, route, selectionMode);
                            enrichResultWithRoute(result, selectionMode, route);
                            results.add(result);
                        }
                        else
                        {
                            results.add(buildRouteFailureResult(scheme, channel, packageContext.packageSummary, route,
                                selectionMode));
                        }
                    }
                }
            }
        }

        FeeEstimateResponse response = new FeeEstimateResponse();
        response.setRequestNo(generateRequestNo());
        response.setEstimateView(VIEW_BUYER_SIMULATION.equals(estimateView) ? VIEW_BUYER_SIMULATION : VIEW_OPERATIONS);
        response.setSelectionMode(selectionMode);
        response.setBuyerId(buyer.getId());
        response.setBuyerCode(buyer.getCode());
        response.setBuyerName(buyer.getName());
        response.setBuyerLevel(buyer.getKind());
        response.setOriginWarehouseCode(resolveResponseWarehouseCode(requestedWarehouseCodes, warehouseCodes));
        response.setPackageInputMode(packageInputMode);
        response.setPackageSummary(packageContext.packageSummary);
        response.setSkuSnapshots(packageContext.skuSnapshots);
        response.setRouteCandidates(routeCandidates);
        markRecommendedResult(results, selectionMode);
        response.setResults(results);
        applySingleSchemeSnapshot(response, winningSchemesByWarehouse);
        response.setResolveSummary(buildResolveSummary(warehouseCandidates.size(), winningSchemesByWarehouse,
            schemeChannelCandidateCount, routeCandidates, startedAt));
        return response;
    }

    private List<QuoteScheme> selectEnabledQuoteSchemes()
    {
        QuoteScheme query = new QuoteScheme();
        query.setStatus(STATUS_ENABLED);
        return emptyIfNull(quoteSchemeMapper.selectQuoteSchemeList(query));
    }

    private QuoteScheme resolveSelectedScheme(List<QuoteScheme> schemes, Long schemeId)
    {
        if (schemeId != null)
        {
            return schemes.stream()
                .filter(item -> Objects.equals(item.getSchemeId(), schemeId))
                .findFirst()
                .orElse(null);
        }
        return schemes.isEmpty() ? null : schemes.get(0);
    }

    private List<QuoteSchemeOption> selectWarehouseOptions()
    {
        QuoteSchemeWarehouseLookupService service = warehouseLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            return Collections.emptyList();
        }
        return emptyIfNull(service.selectWarehouseOptions(null));
    }

    private FeeEstimateSkuWarehouseCandidate buildManualWarehouseCandidate(String warehouseCode)
    {
        FeeEstimateSkuWarehouseCandidate candidate = new FeeEstimateSkuWarehouseCandidate();
        candidate.setWarehouseCode(warehouseCode);
        selectWarehouseOptions().stream()
            .filter(option -> StringUtils.equals(option.getCode(), warehouseCode)
                || StringUtils.equals(option.getValue(), warehouseCode))
            .findFirst()
            .ifPresent(option -> {
                candidate.setWarehouseName(StringUtils.defaultIfBlank(option.getName(), option.getLabel()));
                candidate.setWarehouseKind(option.getKind());
            });
        return candidate;
    }

    private List<QuoteSchemeOption> selectBuyerOptions()
    {
        QuoteSchemeBuyerLookupService service = buyerLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            return Collections.emptyList();
        }
        return emptyIfNull(service.selectBuyerOptions(null));
    }

    private QuoteSchemeOption requireBuyerOption(Long buyerId)
    {
        if (buyerId == null)
        {
            throw new ServiceException("买家模拟必须先选择买家");
        }
        QuoteSchemeBuyerLookupService service = buyerLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("买家查询服务未接入");
        }
        QuoteSchemeOption buyer = service.selectBuyerOption(buyerId);
        if (buyer == null)
        {
            throw new ServiceException("买家不存在或已停用：" + buyerId);
        }
        return buyer;
    }

    private Set<String> resolveRequestedWarehouseCodes(FeeEstimateRequest request, boolean autoMode)
    {
        Set<String> warehouseCodes = new LinkedHashSet<>();
        if (autoMode)
        {
            warehouseCodes.addAll(normalizeCodes(request.getWarehouseCodes()));
            String legacyWarehouseCode = StringUtils.trimToNull(request.getOriginWarehouseCode());
            if (legacyWarehouseCode != null)
            {
                warehouseCodes.add(legacyWarehouseCode);
            }
            return warehouseCodes;
        }
        warehouseCodes.add(requireCode(request.getOriginWarehouseCode(), "选择仓库"));
        return warehouseCodes;
    }

    private String resolveRequestedCustomerChannelCode(FeeEstimateRequest request, boolean autoMode)
    {
        if (autoMode)
        {
            return null;
        }
        String customerChannelCode = StringUtils.trimToNull(request.getCustomerChannelCode());
        if (customerChannelCode != null)
        {
            return customerChannelCode;
        }
        Set<String> channelCodes = normalizeCodes(request.getChannelCodes());
        if (channelCodes.isEmpty())
        {
            throw new ServiceException("选择客户渠道不能为空");
        }
        if (channelCodes.size() > 1)
        {
            throw new ServiceException("买家模拟手动指定一次只能选择一个客户渠道");
        }
        return channelCodes.iterator().next();
    }

    private List<QuoteScheme> selectActiveBillingSchemes(QuoteSchemeOption buyer, Date now)
    {
        return selectEnabledQuoteSchemes().stream()
            .filter(scheme -> TYPE_BILLING.equals(scheme.getSchemeType()))
            .filter(scheme -> isEffectiveNow(scheme, now))
            .filter(scheme -> matchesBuyerScope(scheme, buyer))
            .collect(Collectors.toList());
    }

    private boolean isEffectiveNow(QuoteScheme scheme, Date now)
    {
        return (scheme.getEffectiveTime() == null || !scheme.getEffectiveTime().after(now))
            && (scheme.getExpireTime() == null || !scheme.getExpireTime().before(now));
    }

    private boolean matchesBuyerScope(QuoteScheme scheme, QuoteSchemeOption buyer)
    {
        String scopeType = StringUtils.defaultIfBlank(scheme.getScopeType(), SCOPE_ALL_BUYERS);
        if (SCOPE_ALL_BUYERS.equals(scopeType))
        {
            return true;
        }
        List<QuoteSchemeScope> scopes = emptyIfNull(quoteSchemeMapper.selectQuoteSchemeScopeList(scheme.getSchemeId()));
        if (SCOPE_BUYER_LEVEL.equals(scopeType))
        {
            return scopes.stream()
                .anyMatch(scope -> StringUtils.equals(scope.getBuyerLevelCode(), buyer.getKind()));
        }
        if (SCOPE_BUYER.equals(scopeType))
        {
            return scopes.stream()
                .anyMatch(scope -> Objects.equals(scope.getBuyerId(), buyer.getId()));
        }
        return false;
    }

    private Map<Long, Set<String>> buildSchemeWarehouseCodeMap(List<QuoteScheme> schemes)
    {
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        for (QuoteScheme scheme : schemes)
        {
            if (!WAREHOUSE_INCLUDE.equals(scheme.getWarehouseScopeMode()))
            {
                result.put(scheme.getSchemeId(), Collections.emptySet());
                continue;
            }
            Set<String> warehouseCodes = emptyIfNull(quoteSchemeMapper.selectQuoteSchemeWarehouseList(scheme.getSchemeId()))
                .stream()
                .map(QuoteSchemeWarehouse::getWarehouseCode)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            result.put(scheme.getSchemeId(), warehouseCodes);
        }
        return result;
    }

    private Map<String, QuoteScheme> selectWinningSchemesByWarehouse(Set<String> warehouseCodes,
        List<QuoteScheme> activeBillingSchemes, Map<Long, Set<String>> warehouseCodesByScheme)
    {
        Map<String, QuoteScheme> result = new LinkedHashMap<>();
        for (String warehouseCode : warehouseCodes)
        {
            for (QuoteScheme scheme : activeBillingSchemes)
            {
                Set<String> schemeWarehouseCodes = warehouseCodesByScheme.getOrDefault(scheme.getSchemeId(),
                    Collections.emptySet());
                if (schemeWarehouseCodes.contains(warehouseCode))
                {
                    result.put(warehouseCode, scheme);
                    break;
                }
            }
        }
        return result;
    }

    private List<QuoteSchemeChannel> selectEnabledSchemeChannels(Long schemeId)
    {
        return emptyIfNull(quoteSchemeMapper.selectQuoteSchemeChannelList(schemeId)).stream()
            .filter(channel -> STATUS_ENABLED.equals(channel.getStatus()))
            .collect(Collectors.toList());
    }

    private List<QuoteSchemeChannel> filterSchemeChannelsBySelection(List<QuoteSchemeChannel> channels,
        String customerChannelCode)
    {
        if (StringUtils.isBlank(customerChannelCode))
        {
            return channels;
        }
        return emptyIfNull(channels).stream()
            .filter(channel -> StringUtils.equals(channel.getCustomerChannelCode(), customerChannelCode))
            .collect(Collectors.toList());
    }

    private List<FeeEstimateSkuWarehouseCandidate> resolveCommonWarehouseCandidates(List<Long> skuIds,
        Set<String> requestedWarehouseCodes)
    {
        if (skuIds == null || skuIds.isEmpty())
        {
            throw new ServiceException("自动最优必须先选择 SKU");
        }
        List<FeeEstimateSkuWarehouseCandidate> allCandidates = emptyIfNull(requireSkuLookupService()
            .selectSkuWarehouseCandidatesByIds(skuIds)).stream()
            .filter(candidate -> StringUtils.isNotBlank(candidate.getWarehouseCode()))
            .filter(this::isWarehouseCandidateEnabled)
            .collect(Collectors.toList());
        Map<Long, List<FeeEstimateSkuWarehouseCandidate>> candidatesBySkuId = allCandidates.stream()
            .collect(Collectors.groupingBy(FeeEstimateSkuWarehouseCandidate::getSkuId, LinkedHashMap::new,
                Collectors.toList()));

        Set<String> commonWarehouseCodes = null;
        for (Long skuId : skuIds)
        {
            List<FeeEstimateSkuWarehouseCandidate> skuCandidates = candidatesBySkuId.get(skuId);
            if (skuCandidates == null || skuCandidates.isEmpty())
            {
                throw new ServiceException("SKU 未绑定可用发货仓：" + skuId);
            }
            Set<String> skuWarehouseCodes = skuCandidates.stream()
                .map(FeeEstimateSkuWarehouseCandidate::getWarehouseCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (commonWarehouseCodes == null)
            {
                commonWarehouseCodes = skuWarehouseCodes;
            }
            else
            {
                commonWarehouseCodes.retainAll(skuWarehouseCodes);
            }
        }
        if (commonWarehouseCodes == null || commonWarehouseCodes.isEmpty())
        {
            throw new ServiceException("订单内 SKU 没有共同可发仓库");
        }
        if (requestedWarehouseCodes != null && !requestedWarehouseCodes.isEmpty())
        {
            Set<String> matchedWarehouseCodes = new LinkedHashSet<>(commonWarehouseCodes);
            Set<String> missingWarehouseCodes = requestedWarehouseCodes.stream()
                .filter(code -> !matchedWarehouseCodes.contains(code))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!missingWarehouseCodes.isEmpty())
            {
                throw new ServiceException("指定仓库不在订单 SKU 的共同可发仓库内："
                    + String.join(",", missingWarehouseCodes));
            }
            commonWarehouseCodes.retainAll(requestedWarehouseCodes);
        }

        Set<String> finalCommonWarehouseCodes = commonWarehouseCodes;
        List<FeeEstimateSkuWarehouseCandidate> result = allCandidates.stream()
            .filter(candidate -> finalCommonWarehouseCodes.contains(candidate.getWarehouseCode()))
            .collect(Collectors.toMap(FeeEstimateSkuWarehouseCandidate::getWarehouseCode, Function.identity(),
                (first, second) -> first, LinkedHashMap::new))
            .values()
            .stream()
            .collect(Collectors.toList());
        assertSingleCountryAndCurrency(result);
        return result;
    }

    private boolean isWarehouseCandidateEnabled(FeeEstimateSkuWarehouseCandidate candidate)
    {
        return StringUtils.isBlank(candidate.getStatus()) || WAREHOUSE_STATUS_NORMAL.equals(candidate.getStatus());
    }

    private void assertSingleCountryAndCurrency(List<FeeEstimateSkuWarehouseCandidate> warehouses)
    {
        Set<String> countries = warehouses.stream()
            .map(FeeEstimateSkuWarehouseCandidate::getCountryCode)
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (countries.size() > 1)
        {
            throw new ServiceException("订单 SKU 命中了多个国家的仓库，当前订单不允许跨国家自动最优");
        }
        Set<String> currencies = warehouses.stream()
            .map(FeeEstimateSkuWarehouseCandidate::getCurrencyCode)
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (currencies.size() > 1)
        {
            throw new ServiceException("订单 SKU 命中了多个币种的仓库，当前订单不允许跨币种自动最优");
        }
    }

    private FeeEstimateSkuWarehouseCandidate findWarehouseCandidate(List<FeeEstimateSkuWarehouseCandidate> candidates,
        String warehouseCode)
    {
        return candidates.stream()
            .filter(candidate -> StringUtils.equals(candidate.getWarehouseCode(), warehouseCode))
            .findFirst()
            .orElse(null);
    }

    private FinanceFeeEstimateLogisticsLookupService requireLogisticsLookupService()
    {
        FinanceFeeEstimateLogisticsLookupService service = logisticsLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("物流渠道候选解析服务未接入");
        }
        return service;
    }

    private List<QuoteSchemeOption> selectEnabledSchemeChannelOptions(Long schemeId)
    {
        return emptyIfNull(quoteSchemeMapper.selectQuoteSchemeChannelList(schemeId)).stream()
            .filter(channel -> STATUS_ENABLED.equals(channel.getStatus()))
            .map(this::toChannelOption)
            .collect(Collectors.toList());
    }

    private List<QuoteSchemeOption> selectAllEnabledCustomerChannelOptions(List<QuoteScheme> schemes)
    {
        Map<String, QuoteSchemeOption> optionsByCode = new LinkedHashMap<>();
        for (QuoteScheme scheme : emptyIfNull(schemes))
        {
            for (QuoteSchemeChannel channel : selectEnabledSchemeChannels(scheme.getSchemeId()))
            {
                String channelCode = StringUtils.trimToNull(channel.getCustomerChannelCode());
                if (channelCode != null && !optionsByCode.containsKey(channelCode))
                {
                    optionsByCode.put(channelCode, toChannelOption(channel));
                }
            }
        }
        return new ArrayList<>(optionsByCode.values());
    }

    private QuoteSchemeOption toChannelOption(QuoteSchemeChannel channel)
    {
        QuoteSchemeOption option = new QuoteSchemeOption();
        option.setValue(channel.getCustomerChannelCode());
        option.setCode(channel.getCustomerChannelCode());
        option.setName(channel.getCustomerChannelNameSnapshot());
        option.setLabel(channel.getCustomerChannelNameSnapshot() + " (" + channel.getCustomerChannelCode() + ")");
        option.setSearchText(String.join(" ",
            StringUtils.defaultString(channel.getCustomerChannelCode()),
            StringUtils.defaultString(channel.getCustomerChannelNameSnapshot())));
        return option;
    }

    private FeeEstimateQuoteSchemeOption toSchemeOption(QuoteScheme scheme)
    {
        FeeEstimateQuoteSchemeOption option = new FeeEstimateQuoteSchemeOption();
        option.setSchemeId(scheme.getSchemeId());
        option.setValue(scheme.getSchemeId());
        option.setSchemeCode(scheme.getSchemeCode());
        option.setSchemeName(scheme.getSchemeName());
        option.setSchemeType(scheme.getSchemeType());
        option.setFeeSourceMode(scheme.getFeeSourceMode());
        option.setCurrencyCode(scheme.getCurrencyCode());
        option.setLabel(scheme.getSchemeName() + " (" + scheme.getSchemeCode() + ")");
        return option;
    }

    private void validateOriginWarehouse(String originWarehouseCode)
    {
        QuoteSchemeWarehouseLookupService service = warehouseLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("发货仓查询服务未接入");
        }
        QuoteSchemeOption warehouse = service.selectWarehouseOption(originWarehouseCode);
        if (warehouse == null)
        {
            throw new ServiceException("发货仓不存在或已停用：" + originWarehouseCode);
        }
    }

    private QuoteScheme requireEnabledScheme(Long schemeId)
    {
        if (schemeId == null)
        {
            throw new ServiceException("报价方案不能为空");
        }
        QuoteScheme scheme = quoteSchemeMapper.selectQuoteSchemeById(schemeId);
        if (scheme == null)
        {
            throw new ServiceException("报价方案不存在");
        }
        if (!STATUS_ENABLED.equals(scheme.getStatus()))
        {
            throw new ServiceException("报价方案未启用：" + scheme.getSchemeName());
        }
        return scheme;
    }

    private void validateSchemeWarehouseScope(QuoteScheme scheme, String originWarehouseCode)
    {
        if (!WAREHOUSE_INCLUDE.equals(scheme.getWarehouseScopeMode()))
        {
            return;
        }
        boolean matched = emptyIfNull(quoteSchemeMapper.selectQuoteSchemeWarehouseList(scheme.getSchemeId())).stream()
            .map(QuoteSchemeWarehouse::getWarehouseCode)
            .anyMatch(code -> StringUtils.equals(code, originWarehouseCode));
        if (!matched)
        {
            throw new ServiceException("报价方案不适用当前发货仓：" + originWarehouseCode);
        }
    }

    private List<QuoteSchemeChannel> selectTargetChannels(Long schemeId, List<String> submittedChannelCodes)
    {
        List<QuoteSchemeChannel> enabledChannels = emptyIfNull(quoteSchemeMapper.selectQuoteSchemeChannelList(schemeId))
            .stream()
            .filter(channel -> STATUS_ENABLED.equals(channel.getStatus()))
            .collect(Collectors.toList());
        if (enabledChannels.isEmpty())
        {
            throw new ServiceException("报价方案未配置可用物流渠道");
        }
        Set<String> channelCodes = normalizeCodes(submittedChannelCodes);
        if (channelCodes.isEmpty())
        {
            return enabledChannels;
        }
        List<QuoteSchemeChannel> selectedChannels = enabledChannels.stream()
            .filter(channel -> channelCodes.contains(channel.getCustomerChannelCode()))
            .collect(Collectors.toList());
        if (selectedChannels.isEmpty())
        {
            throw new ServiceException("选择的物流渠道不在当前报价方案中");
        }
        return selectedChannels;
    }

    private FeeEstimateCalculationContext buildPackageContext(String packageInputMode,
        List<FeeEstimatePackageLine> packageLines)
    {
        if (MODE_MANUAL.equals(packageInputMode))
        {
            return buildManualPackageContext(packageLines);
        }
        return buildSkuPackageContext(packageLines);
    }

    private FeeEstimateCalculationContext buildManualPackageContext(List<FeeEstimatePackageLine> packageLines)
    {
        List<FeeEstimatePackageLine> lines = emptyIfNull(packageLines);
        if (lines.size() != 1)
        {
            throw new ServiceException("手工尺寸只支持录入一个包裹");
        }
        FeeEstimatePackageLine line = lines.get(0);
        int quantity = normalizeQuantity(line.getQuantity());
        if (quantity != 1)
        {
            throw new ServiceException("手工尺寸模式的包裹数量固定为 1");
        }
        BigDecimal edge1 = requirePositive(line.getLengthCm(), "长度");
        BigDecimal edge2 = requirePositive(line.getWidthCm(), "宽度");
        BigDecimal edge3 = requirePositive(line.getHeightCm(), "高度");
        BigDecimal weight = requirePositive(line.getWeightKg(), "重量");
        FeeEstimatePackageSummary summary = buildSummary(edge1, edge2, edge3, weight,
            "手工录入一个包裹尺寸");
        return new FeeEstimateCalculationContext(summary, Collections.emptyList());
    }

    private FeeEstimateCalculationContext buildSkuPackageContext(List<FeeEstimatePackageLine> packageLines)
    {
        Map<Long, Integer> quantities = collectSkuQuantities(packageLines);
        List<FeeEstimateSkuSnapshot> skuSnapshots = requireSkuLookupService()
            .selectSkuSnapshotsByIds(new ArrayList<>(quantities.keySet()));
        Map<Long, FeeEstimateSkuSnapshot> snapshotMap = emptyIfNull(skuSnapshots).stream()
            .filter(snapshot -> snapshot.getSkuId() != null)
            .collect(Collectors.toMap(FeeEstimateSkuSnapshot::getSkuId, Function.identity(), (a, b) -> a,
                LinkedHashMap::new));

        BigDecimal edge1 = ZERO;
        BigDecimal edge2 = ZERO;
        BigDecimal edge3 = ZERO;
        BigDecimal actualWeight = ZERO;
        List<FeeEstimateSkuSnapshot> orderedSnapshots = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet())
        {
            FeeEstimateSkuSnapshot snapshot = snapshotMap.get(entry.getKey());
            if (snapshot == null)
            {
                throw new ServiceException("SKU 不存在或已删除：" + entry.getKey());
            }
            int quantity = entry.getValue();
            assertSkuMeasurement(snapshot);
            snapshot.setQuantity(quantity);
            BigDecimal[] sides = new BigDecimal[] {
                snapshot.getMeasureLengthCm(),
                snapshot.getMeasureWidthCm(),
                snapshot.getMeasureHeightCm()
            };
            Arrays.sort(sides);
            edge1 = edge1.add(sides[0].multiply(BigDecimal.valueOf(quantity)));
            edge2 = max(edge2, sides[1]);
            edge3 = max(edge3, sides[2]);
            actualWeight = actualWeight.add(snapshot.getMeasureWeightKg().multiply(BigDecimal.valueOf(quantity)));
            orderedSnapshots.add(snapshot);
        }

        FeeEstimatePackageSummary summary = buildSummary(edge1, edge2, edge3, actualWeight,
            "SKU 合包：边1=最小边按数量相加，边2=次长边最大值，边3=最长边最大值");
        return new FeeEstimateCalculationContext(summary, orderedSnapshots);
    }

    private Map<Long, Integer> collectSkuQuantities(List<FeeEstimatePackageLine> packageLines)
    {
        List<FeeEstimatePackageLine> lines = emptyIfNull(packageLines);
        if (lines.isEmpty())
        {
            throw new ServiceException("请选择 SKU");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (FeeEstimatePackageLine line : lines)
        {
            if (line == null || line.getSkuId() == null)
            {
                throw new ServiceException("SKU 不能为空");
            }
            quantities.merge(line.getSkuId(), normalizeQuantity(line.getQuantity()), Integer::sum);
        }
        return quantities;
    }

    private void assertSkuMeasurement(FeeEstimateSkuSnapshot snapshot)
    {
        String skuCode = StringUtils.defaultIfBlank(snapshot.getSystemSkuCode(), String.valueOf(snapshot.getSkuId()));
        requirePositive(snapshot.getMeasureLengthCm(), "SKU " + skuCode + " 来源商品仓库长度");
        requirePositive(snapshot.getMeasureWidthCm(), "SKU " + skuCode + " 来源商品仓库宽度");
        requirePositive(snapshot.getMeasureHeightCm(), "SKU " + skuCode + " 来源商品仓库高度");
        requirePositive(snapshot.getMeasureWeightKg(), "SKU " + skuCode + " 来源商品仓库重量");
    }

    private FeeEstimatePackageSummary buildSummary(BigDecimal edge1, BigDecimal edge2, BigDecimal edge3,
        BigDecimal actualWeight, String mergeRule)
    {
        BigDecimal normalizedEdge1 = round(edge1);
        BigDecimal normalizedEdge2 = round(edge2);
        BigDecimal normalizedEdge3 = round(edge3);
        BigDecimal volumeWeight = normalizedEdge1.multiply(normalizedEdge2).multiply(normalizedEdge3)
            .divide(VOLUME_WEIGHT_DIVISOR, 3, RoundingMode.HALF_UP);
        BigDecimal normalizedActualWeight = round(actualWeight);
        FeeEstimatePackageSummary summary = new FeeEstimatePackageSummary();
        summary.setEdge1Cm(normalizedEdge1);
        summary.setEdge2Cm(normalizedEdge2);
        summary.setEdge3Cm(normalizedEdge3);
        summary.setActualWeightKg(normalizedActualWeight);
        summary.setVolumeWeightKg(volumeWeight);
        summary.setChargeableWeightKg(max(normalizedActualWeight, volumeWeight));
        summary.setPackageCount(1);
        summary.setSizeExpression(formatDecimal(normalizedEdge3) + " * " + formatDecimal(normalizedEdge2)
            + " * " + formatDecimal(normalizedEdge1) + " cm");
        summary.setMergeRule(mergeRule);
        return summary;
    }

    private FeeEstimateChannelResult buildUnavailableResult(QuoteScheme scheme, QuoteSchemeChannel channel,
        FeeEstimatePackageSummary packageSummary)
    {
        FeeEstimateChannelResult result = buildBaseResult(scheme, channel, packageSummary);
        if (SOURCE_INTERNAL_RATE.equals(scheme.getFeeSourceMode()))
        {
            result.setErrorCode("INTERNAL_RATE_NOT_IMPLEMENTED");
            result.setErrorMessage("系统费率试算暂未实现，未生成费用金额");
        }
        else if (SOURCE_EXTERNAL_ESTIMATE.equals(scheme.getFeeSourceMode()))
        {
            result.setErrorCode("LINGXING_ESTIMATE_FIELDS_UNCONFIRMED");
            result.setErrorMessage("领星费用试算接口字段未确认，系统已拒绝伪造费用结果");
        }
        else
        {
            result.setErrorCode("FEE_SOURCE_NOT_SUPPORTED");
            result.setErrorMessage("报价方案费用来源暂不支持：" + scheme.getFeeSourceMode());
        }
        return result;
    }

    private FeeEstimateChannelResult buildEstimateResult(QuoteScheme scheme, QuoteSchemeChannel channel,
        FeeEstimatePackageSummary packageSummary, FeeEstimateRequest request, FeeEstimateRouteCandidate route,
        String selectionMode)
    {
        if (!SOURCE_EXTERNAL_ESTIMATE.equals(scheme.getFeeSourceMode()))
        {
            return buildUnavailableResult(scheme, channel, packageSummary);
        }
        FeeEstimateChannelResult result = buildBaseResult(scheme, channel, packageSummary);
        FinanceFeeEstimateExternalService externalService = externalServiceProvider.getIfAvailable();
        if (externalService == null)
        {
            result.setErrorCode("EXTERNAL_ESTIMATE_SERVICE_MISSING");
            result.setErrorMessage("外部费用试算服务未接入");
            return result;
        }
        FeeEstimateExternalRequest externalRequest = new FeeEstimateExternalRequest();
        externalRequest.setTraceId(result.getTraceId());
        externalRequest.setSelectionMode(selectionMode);
        externalRequest.setCurrencyCode(scheme.getCurrencyCode());
        externalRequest.setOriginalRequest(request);
        externalRequest.setPackageSummary(packageSummary);
        externalRequest.setRouteCandidate(route);
        try
        {
            applyExternalResult(result, externalService.estimate(externalRequest));
        }
        catch (RuntimeException ex)
        {
            result.setSuccess(false);
            result.setErrorCode("EXTERNAL_ESTIMATE_FAILED");
            result.setErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "外部费用试算失败"));
        }
        return result;
    }

    private void applyExternalResult(FeeEstimateChannelResult result, FeeEstimateExternalResult externalResult)
    {
        if (externalResult == null)
        {
            result.setSuccess(false);
            result.setErrorCode("EXTERNAL_ESTIMATE_EMPTY");
            result.setErrorMessage("外部费用试算未返回结果");
            return;
        }
        result.setSuccess(Boolean.TRUE.equals(externalResult.getSuccess()));
        result.setTotalAmount(externalResult.getTotalAmount());
        result.setBasicFreightAmount(externalResult.getBasicFreightAmount());
        result.setSurchargeAmount(externalResult.getSurchargeAmount());
        result.setOperationFeeAmount(externalResult.getOperationFeeAmount());
        result.setPackageMaterialFeeAmount(externalResult.getPackageMaterialFeeAmount());
        if (StringUtils.isNotBlank(externalResult.getCurrencyCode()))
        {
            result.setCurrencyCode(externalResult.getCurrencyCode());
        }
        if (StringUtils.isNotBlank(externalResult.getTraceId()))
        {
            result.setTraceId(externalResult.getTraceId());
        }
        result.setErrorCode(externalResult.getErrorCode());
        result.setErrorMessage(externalResult.getErrorMessage());
    }

    private FeeEstimateChannelResult buildRouteFailureResult(QuoteScheme scheme, QuoteSchemeChannel channel,
        FeeEstimatePackageSummary packageSummary, FeeEstimateRouteCandidate route, String selectionMode)
    {
        FeeEstimateChannelResult result = buildBaseResult(scheme, channel, packageSummary);
        enrichResultWithRoute(result, selectionMode, route);
        result.setErrorCode(route.getFailureCode());
        result.setErrorMessage(route.getFailureMessage());
        return result;
    }

    private FeeEstimateChannelResult buildBaseResult(QuoteScheme scheme, QuoteSchemeChannel channel,
        FeeEstimatePackageSummary packageSummary)
    {
        FeeEstimateChannelResult result = new FeeEstimateChannelResult();
        if (channel != null)
        {
            result.setSchemeChannelId(channel.getSchemeChannelId());
            result.setChannelCode(channel.getCustomerChannelCode());
            result.setChannelName(channel.getCustomerChannelNameSnapshot());
        }
        result.setSchemeType(scheme.getSchemeType());
        result.setFeeSourceMode(scheme.getFeeSourceMode());
        result.setCurrencyCode(scheme.getCurrencyCode());
        result.setSuccess(false);
        result.setRecommended(false);
        result.setActualWeightKg(packageSummary.getActualWeightKg());
        result.setVolumeWeightKg(packageSummary.getVolumeWeightKg());
        result.setChargeableWeightKg(packageSummary.getChargeableWeightKg());
        result.setPackageCount(packageSummary.getPackageCount());
        result.setTraceId(UUID.randomUUID().toString());
        return result;
    }

    private FeeEstimateRouteCandidate enrichRouteCandidate(FeeEstimateRouteCandidate source,
        FeeEstimateSkuWarehouseCandidate warehouse, QuoteScheme scheme, QuoteSchemeChannel channel)
    {
        FeeEstimateRouteCandidate route = cloneRouteCandidate(source);
        applyWarehouseSnapshot(route, warehouse);
        route.setSchemeId(scheme.getSchemeId());
        route.setSchemeName(scheme.getSchemeName());
        route.setSchemeChannelId(channel.getSchemeChannelId());
        route.setFeeSourceMode(scheme.getFeeSourceMode());
        route.setCurrencyCode(scheme.getCurrencyCode());
        route.setCustomerChannelCode(channel.getCustomerChannelCode());
        route.setCustomerChannelName(channel.getCustomerChannelNameSnapshot());
        return route;
    }

    private FeeEstimateRouteCandidate failedWarehouseRoute(FeeEstimateSkuWarehouseCandidate warehouse,
        String failureCode, String failureMessage)
    {
        FeeEstimateRouteCandidate route = new FeeEstimateRouteCandidate();
        applyWarehouseSnapshot(route, warehouse);
        route.setExecutable(Boolean.FALSE);
        route.setFailureCode(failureCode);
        route.setFailureMessage(failureMessage);
        return route;
    }

    private FeeEstimateRouteCandidate failedSchemeRoute(FeeEstimateSkuWarehouseCandidate warehouse,
        QuoteScheme scheme, QuoteSchemeChannel channel, String failureCode, String failureMessage)
    {
        FeeEstimateRouteCandidate route = failedWarehouseRoute(warehouse, failureCode, failureMessage);
        route.setSchemeId(scheme.getSchemeId());
        route.setSchemeName(scheme.getSchemeName());
        route.setFeeSourceMode(scheme.getFeeSourceMode());
        route.setCurrencyCode(scheme.getCurrencyCode());
        if (channel != null)
        {
            route.setSchemeChannelId(channel.getSchemeChannelId());
            route.setCustomerChannelCode(channel.getCustomerChannelCode());
            route.setCustomerChannelName(channel.getCustomerChannelNameSnapshot());
        }
        return route;
    }

    private void applyWarehouseSnapshot(FeeEstimateRouteCandidate route, FeeEstimateSkuWarehouseCandidate warehouse)
    {
        if (warehouse == null)
        {
            return;
        }
        if (StringUtils.isBlank(route.getWarehouseCode()))
        {
            route.setWarehouseCode(warehouse.getWarehouseCode());
        }
        if (StringUtils.isBlank(route.getWarehouseName()))
        {
            route.setWarehouseName(warehouse.getWarehouseName());
        }
        if (StringUtils.isBlank(route.getWarehouseKind()))
        {
            route.setWarehouseKind(warehouse.getWarehouseKind());
        }
        if (StringUtils.isBlank(route.getCountryCode()))
        {
            route.setCountryCode(warehouse.getCountryCode());
        }
    }

    private FeeEstimateRouteCandidate cloneRouteCandidate(FeeEstimateRouteCandidate source)
    {
        FeeEstimateRouteCandidate route = new FeeEstimateRouteCandidate();
        route.setSchemeId(source.getSchemeId());
        route.setSchemeChannelId(source.getSchemeChannelId());
        route.setSchemeName(source.getSchemeName());
        route.setFeeSourceMode(source.getFeeSourceMode());
        route.setCurrencyCode(source.getCurrencyCode());
        route.setWarehouseCode(source.getWarehouseCode());
        route.setWarehouseName(source.getWarehouseName());
        route.setWarehouseKind(source.getWarehouseKind());
        route.setCountryCode(source.getCountryCode());
        route.setCustomerChannelCode(source.getCustomerChannelCode());
        route.setCustomerChannelName(source.getCustomerChannelName());
        route.setLabelUploadRequired(source.getLabelUploadRequired());
        route.setBuyerScopeMode(source.getBuyerScopeMode());
        route.setSystemChannelCode(source.getSystemChannelCode());
        route.setSystemChannelName(source.getSystemChannelName());
        route.setFulfillmentMode(source.getFulfillmentMode());
        route.setCarrierConnectionCode(source.getCarrierConnectionCode());
        route.setCarrierExternalChannelCode(source.getCarrierExternalChannelCode());
        route.setExecutable(source.getExecutable());
        route.setFailureCode(source.getFailureCode());
        route.setFailureMessage(source.getFailureMessage());
        return route;
    }

    private void enrichResultWithRoute(FeeEstimateChannelResult result, String selectionMode,
        FeeEstimateRouteCandidate route)
    {
        result.setSelectionMode(selectionMode);
        result.setWarehouseCode(route.getWarehouseCode());
        result.setWarehouseName(route.getWarehouseName());
        result.setSystemChannelCode(route.getSystemChannelCode());
        result.setSystemChannelName(route.getSystemChannelName());
        result.setFulfillmentMode(route.getFulfillmentMode());
    }

    private void markRecommendedResult(List<FeeEstimateChannelResult> results, String selectionMode)
    {
        if (results == null || results.isEmpty())
        {
            return;
        }
        for (FeeEstimateChannelResult result : results)
        {
            result.setRecommended(false);
        }
        if (!SELECTION_AUTO_BEST.equals(selectionMode))
        {
            return;
        }
        FeeEstimateChannelResult best = null;
        for (FeeEstimateChannelResult result : results)
        {
            if (!Boolean.TRUE.equals(result.getSuccess()) || result.getTotalAmount() == null)
            {
                continue;
            }
            if (best == null || result.getTotalAmount().compareTo(best.getTotalAmount()) < 0)
            {
                best = result;
            }
        }
        if (best != null)
        {
            best.setRecommended(true);
        }
    }

    private String resolveResponseWarehouseCode(Set<String> requestedWarehouseCodes, Set<String> warehouseCodes)
    {
        if (requestedWarehouseCodes != null && requestedWarehouseCodes.size() == 1)
        {
            return requestedWarehouseCodes.iterator().next();
        }
        return warehouseCodes.size() == 1 ? warehouseCodes.iterator().next() : null;
    }

    private void applySingleSchemeSnapshot(FeeEstimateResponse response, Map<String, QuoteScheme> schemesByWarehouse)
    {
        Set<QuoteScheme> schemes = new LinkedHashSet<>(schemesByWarehouse.values());
        if (schemes.size() == 1)
        {
            QuoteScheme scheme = schemes.iterator().next();
            response.setQuoteSchemeId(scheme.getSchemeId());
            response.setQuoteSchemeName(scheme.getSchemeName());
        }
    }

    private FeeEstimateResolveSummary buildResolveSummary(int warehouseCandidateCount,
        Map<String, QuoteScheme> winningSchemesByWarehouse, int schemeChannelCandidateCount,
        List<FeeEstimateRouteCandidate> routeCandidates, long startedAt)
    {
        FeeEstimateResolveSummary summary = new FeeEstimateResolveSummary();
        summary.setWarehouseCandidateCount(warehouseCandidateCount);
        summary.setQuoteSchemeCandidateCount(new LinkedHashSet<>(winningSchemesByWarehouse.values()).size());
        summary.setCustomerChannelCandidateCount(schemeChannelCandidateCount);
        summary.setRouteCandidateCount(routeCandidates.size());
        int executableCount = (int) routeCandidates.stream()
            .filter(route -> Boolean.TRUE.equals(route.getExecutable()))
            .count();
        summary.setExecutableRouteCount(executableCount);
        summary.setFailedCandidateCount(routeCandidates.size() - executableCount);
        summary.setResolveCostMs(System.currentTimeMillis() - startedAt);
        return summary;
    }

    private FinanceFeeEstimateSkuLookupService requireSkuLookupService()
    {
        FinanceFeeEstimateSkuLookupService service = skuLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("商品 SKU 查询服务未接入");
        }
        return service;
    }

    private String normalizeInputMode(String inputMode)
    {
        String value = StringUtils.upperCase(StringUtils.trimToEmpty(inputMode));
        if (StringUtils.isBlank(value))
        {
            return MODE_SKU;
        }
        if (!MODE_SKU.equals(value) && !MODE_MANUAL.equals(value))
        {
            throw new ServiceException("包裹信息模式不正确：" + inputMode);
        }
        return value;
    }

    private String normalizeSelectionMode(String selectionMode)
    {
        String value = StringUtils.upperCase(StringUtils.trimToEmpty(selectionMode));
        if (StringUtils.isBlank(value))
        {
            return SELECTION_MANUAL;
        }
        if (!SELECTION_MANUAL.equals(value) && !SELECTION_AUTO_BEST.equals(value))
        {
            throw new ServiceException("渠道选择模式不正确：" + selectionMode);
        }
        return value;
    }

    private String normalizeEstimateView(String estimateView)
    {
        String value = StringUtils.upperCase(StringUtils.trimToEmpty(estimateView));
        if (StringUtils.isBlank(value))
        {
            return VIEW_OPERATIONS;
        }
        if (!VIEW_OPERATIONS.equals(value) && !VIEW_BUYER_SIMULATION.equals(value))
        {
            throw new ServiceException("费用试算视图不正确：" + estimateView);
        }
        return value;
    }

    private String requireCode(String value, String fieldName)
    {
        String result = StringUtils.trimToNull(value);
        if (result == null)
        {
            throw new ServiceException(fieldName + "不能为空");
        }
        return result;
    }

    private BigDecimal requirePositive(BigDecimal value, String fieldName)
    {
        if (value == null || value.compareTo(ZERO) <= 0)
        {
            throw new ServiceException(fieldName + "必须大于 0");
        }
        return value;
    }

    private int normalizeQuantity(Integer quantity)
    {
        int result = quantity == null ? 1 : quantity;
        if (result <= 0)
        {
            throw new ServiceException("数量必须大于 0");
        }
        return result;
    }

    private Set<String> normalizeCodes(List<String> codes)
    {
        if (codes == null || codes.isEmpty())
        {
            return Collections.emptySet();
        }
        return codes.stream()
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private BigDecimal max(BigDecimal a, BigDecimal b)
    {
        if (a == null)
        {
            return b;
        }
        if (b == null)
        {
            return a;
        }
        return a.compareTo(b) >= 0 ? a : b;
    }

    private BigDecimal round(BigDecimal value)
    {
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private String formatDecimal(BigDecimal value)
    {
        return round(value).stripTrailingZeros().toPlainString();
    }

    private String generateRequestNo()
    {
        return "FE" + DateUtils.dateTimeNow("yyyyMMddHHmmss")
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private <T> List<T> emptyIfNull(List<T> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private static class FeeEstimateCalculationContext
    {
        private final FeeEstimatePackageSummary packageSummary;

        private final List<FeeEstimateSkuSnapshot> skuSnapshots;

        private FeeEstimateCalculationContext(FeeEstimatePackageSummary packageSummary,
            List<FeeEstimateSkuSnapshot> skuSnapshots)
        {
            this.packageSummary = packageSummary;
            this.skuSnapshots = skuSnapshots;
        }
    }
}
