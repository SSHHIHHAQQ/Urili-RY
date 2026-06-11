package com.ruoyi.finance.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.finance.domain.QuoteScheme;
import com.ruoyi.finance.domain.QuoteSchemeChannel;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.domain.QuoteSchemeScope;
import com.ruoyi.finance.domain.QuoteSchemeValueFeeRule;
import com.ruoyi.finance.domain.QuoteSchemeWarehouse;
import com.ruoyi.finance.mapper.QuoteSchemeMapper;
import com.ruoyi.finance.service.IFinanceCurrencyService;
import com.ruoyi.finance.service.IQuoteSchemeService;
import com.ruoyi.finance.service.QuoteSchemeBuyerLookupService;
import com.ruoyi.finance.service.QuoteSchemeCustomerChannelLookupService;
import com.ruoyi.finance.service.QuoteSchemeSystemChannelLookupService;
import com.ruoyi.finance.service.QuoteSchemeWarehouseLookupService;
import com.ruoyi.system.service.ISysDictDataService;

@Service
public class QuoteSchemeServiceImpl implements IQuoteSchemeService
{
    private static final String TYPE_BILLING = "BILLING";

    private static final String TYPE_COST = "COST";

    private static final String SOURCE_EXTERNAL_ESTIMATE = "EXTERNAL_ESTIMATE";

    private static final String SOURCE_INTERNAL_RATE = "INTERNAL_RATE";

    private static final String SCOPE_ALL_BUYERS = "ALL_BUYERS";

    private static final String SCOPE_BUYER_LEVEL = "BUYER_LEVEL";

    private static final String SCOPE_BUYER = "BUYER";

    private static final String WAREHOUSE_ALL = "ALL_WAREHOUSES";

    private static final String WAREHOUSE_INCLUDE = "INCLUDE";

    private static final String STATUS_ENABLED = "ENABLED";

    private static final String STATUS_DISABLED = "DISABLED";

    private static final String BUYER_LEVEL_DICT = "buyer_level";

    private static final String INTERNAL_SCHEME_CODE_PREFIX = "QS";

    private static final String VALUE_FEE_TRIGGER_ORDER_CANCELLED = "ORDER_CANCELLED";

    private static final String VALUE_FEE_CALC_PERCENT = "PERCENT";

    private static final String VALUE_FEE_CALC_FIXED_AMOUNT = "FIXED_AMOUNT";

    private static final String VALUE_FEE_DIRECTION_INCREASE = "INCREASE";

    private static final String VALUE_FEE_DIRECTION_DECREASE = "DECREASE";

    @Autowired
    private QuoteSchemeMapper quoteSchemeMapper;

    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private ObjectProvider<QuoteSchemeBuyerLookupService> buyerLookupServiceProvider;

    @Autowired
    private ObjectProvider<QuoteSchemeWarehouseLookupService> warehouseLookupServiceProvider;

    @Autowired
    private ObjectProvider<QuoteSchemeCustomerChannelLookupService> customerChannelLookupServiceProvider;

    @Autowired
    private ObjectProvider<QuoteSchemeSystemChannelLookupService> systemChannelLookupServiceProvider;

    @Override
    public List<QuoteScheme> selectQuoteSchemeList(QuoteScheme query)
    {
        return quoteSchemeMapper.selectQuoteSchemeList(query);
    }

    @Override
    public QuoteScheme selectQuoteSchemeById(Long schemeId)
    {
        QuoteScheme scheme = requireScheme(schemeId);
        scheme.setScopes(quoteSchemeMapper.selectQuoteSchemeScopeList(schemeId));
        scheme.setWarehouses(quoteSchemeMapper.selectQuoteSchemeWarehouseList(schemeId));
        scheme.setChannels(quoteSchemeMapper.selectQuoteSchemeChannelList(schemeId));
        scheme.setValueFeeRules(quoteSchemeMapper.selectQuoteSchemeValueFeeRuleList(schemeId));
        return scheme;
    }

    @Override
    @Transactional
    public int insertQuoteScheme(QuoteScheme scheme)
    {
        normalizeScheme(scheme, true);
        assertEffectivePriorityNotConflicting(scheme);
        if (quoteSchemeMapper.selectQuoteSchemeByCode(scheme.getSchemeCode()) != null)
        {
            throw new ServiceException("报价方案编码已存在");
        }
        scheme.setCreateBy(currentUsername());
        int rows = quoteSchemeMapper.insertQuoteScheme(scheme);
        saveScopes(scheme.getSchemeId(), scheme);
        saveWarehouses(scheme.getSchemeId(), scheme.getWarehouseScopeMode(), scheme.getWarehouseCodes());
        return rows;
    }

    @Override
    @Transactional
    public int updateQuoteScheme(Long schemeId, QuoteScheme scheme)
    {
        QuoteScheme current = requireScheme(schemeId);
        if (StringUtils.isNotBlank(scheme.getSchemeCode())
            && !current.getSchemeCode().equalsIgnoreCase(StringUtils.trim(scheme.getSchemeCode())))
        {
            throw new ServiceException("报价方案编码新增后不能修改");
        }
        scheme.setSchemeId(schemeId);
        scheme.setSchemeCode(current.getSchemeCode());
        if (StringUtils.isBlank(scheme.getStatus()))
        {
            scheme.setStatus(current.getStatus());
        }
        normalizeScheme(scheme, false);
        assertEffectivePriorityNotConflicting(scheme);
        scheme.setUpdateBy(currentUsername());
        int rows = quoteSchemeMapper.updateQuoteScheme(scheme);
        saveScopes(schemeId, scheme);
        saveWarehouses(schemeId, scheme.getWarehouseScopeMode(), scheme.getWarehouseCodes());
        return rows;
    }

    @Override
    public int updateQuoteSchemeStatus(Long schemeId, String status)
    {
        QuoteScheme scheme = requireScheme(schemeId);
        String normalizedStatus = normalizeStatus(status);
        scheme.setStatus(normalizedStatus);
        assertEffectivePriorityNotConflicting(scheme);
        return quoteSchemeMapper.updateQuoteSchemeStatus(schemeId, normalizedStatus, currentUsername());
    }

    @Override
    public List<QuoteSchemeWarehouse> selectQuoteSchemeWarehouseList(Long schemeId)
    {
        requireScheme(schemeId);
        return quoteSchemeMapper.selectQuoteSchemeWarehouseList(schemeId);
    }

    @Override
    @Transactional
    public int saveQuoteSchemeWarehouses(Long schemeId, List<String> warehouseCodes)
    {
        requireScheme(schemeId);
        String warehouseScopeMode = emptyIfNull(warehouseCodes).isEmpty() ? WAREHOUSE_ALL : WAREHOUSE_INCLUDE;
        List<String> normalizedWarehouseCodes = normalizeWarehouseCodesForScope(warehouseScopeMode, warehouseCodes);
        quoteSchemeMapper.updateQuoteSchemeWarehouseScopeMode(schemeId, warehouseScopeMode, currentUsername());
        saveWarehouses(schemeId, warehouseScopeMode, normalizedWarehouseCodes);
        return 1;
    }

    @Override
    public List<QuoteSchemeChannel> selectQuoteSchemeChannelList(Long schemeId)
    {
        requireScheme(schemeId);
        return quoteSchemeMapper.selectQuoteSchemeChannelList(schemeId);
    }

    @Override
    @Transactional
    public int insertQuoteSchemeChannel(Long schemeId, QuoteSchemeChannel channel)
    {
        QuoteScheme scheme = requireScheme(schemeId);
        normalizeChannel(scheme, channel);
        QuoteSchemeChannel existing = quoteSchemeMapper.selectQuoteSchemeChannelByCustomerChannelCode(
            schemeId, channel.getCustomerChannelCode());
        if (existing != null)
        {
            throw new ServiceException("同一报价方案下不能重复绑定物流渠道");
        }
        if (channel.getDisplayOrder() == null)
        {
            Integer maxOrder = quoteSchemeMapper.selectMaxChannelDisplayOrder(schemeId);
            channel.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        }
        channel.setCreateBy(currentUsername());
        return quoteSchemeMapper.insertQuoteSchemeChannel(channel);
    }

    @Override
    @Transactional
    public int updateQuoteSchemeChannel(Long schemeId, Long schemeChannelId, QuoteSchemeChannel channel)
    {
        QuoteScheme scheme = requireScheme(schemeId);
        QuoteSchemeChannel current = requireSchemeChannel(schemeId, schemeChannelId);
        channel.setSchemeId(schemeId);
        channel.setSchemeChannelId(schemeChannelId);
        normalizeChannel(scheme, channel);
        QuoteSchemeChannel duplicate = quoteSchemeMapper.selectQuoteSchemeChannelByCustomerChannelCode(
            schemeId, channel.getCustomerChannelCode());
        if (duplicate != null && !Objects.equals(duplicate.getSchemeChannelId(), current.getSchemeChannelId()))
        {
            throw new ServiceException("同一报价方案下不能重复绑定物流渠道");
        }
        channel.setUpdateBy(currentUsername());
        return quoteSchemeMapper.updateQuoteSchemeChannel(channel);
    }

    @Override
    @Transactional
    public int deleteQuoteSchemeChannel(Long schemeId, Long schemeChannelId)
    {
        requireScheme(schemeId);
        requireSchemeChannel(schemeId, schemeChannelId);
        return quoteSchemeMapper.deleteQuoteSchemeChannel(schemeId, schemeChannelId);
    }

    @Override
    public List<QuoteSchemeValueFeeRule> selectQuoteSchemeValueFeeRuleList(Long schemeId)
    {
        requireScheme(schemeId);
        return quoteSchemeMapper.selectQuoteSchemeValueFeeRuleList(schemeId);
    }

    @Override
    @Transactional
    public int insertQuoteSchemeValueFeeRule(Long schemeId, QuoteSchemeValueFeeRule rule)
    {
        QuoteScheme scheme = requireScheme(schemeId);
        normalizeValueFeeRule(scheme, rule);
        QuoteSchemeValueFeeRule existing = quoteSchemeMapper.selectQuoteSchemeValueFeeRuleByChannelAndTrigger(
            schemeId, rule.getLogisticsChannelCode(), rule.getTriggerCode());
        if (existing != null)
        {
            throw new ServiceException("同一报价方案、物流渠道和触发情况不能重复配置增值费");
        }
        if (rule.getDisplayOrder() == null)
        {
            Integer maxOrder = quoteSchemeMapper.selectMaxValueFeeDisplayOrder(schemeId);
            rule.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        }
        rule.setCreateBy(currentUsername());
        return quoteSchemeMapper.insertQuoteSchemeValueFeeRule(rule);
    }

    @Override
    @Transactional
    public int updateQuoteSchemeValueFeeRule(Long schemeId, Long valueFeeRuleId, QuoteSchemeValueFeeRule rule)
    {
        QuoteScheme scheme = requireScheme(schemeId);
        QuoteSchemeValueFeeRule current = requireValueFeeRule(schemeId, valueFeeRuleId);
        rule.setSchemeId(schemeId);
        rule.setValueFeeRuleId(valueFeeRuleId);
        normalizeValueFeeRule(scheme, rule);
        QuoteSchemeValueFeeRule duplicate = quoteSchemeMapper.selectQuoteSchemeValueFeeRuleByChannelAndTrigger(
            schemeId, rule.getLogisticsChannelCode(), rule.getTriggerCode());
        if (duplicate != null && !Objects.equals(duplicate.getValueFeeRuleId(), current.getValueFeeRuleId()))
        {
            throw new ServiceException("同一报价方案、物流渠道和触发情况不能重复配置增值费");
        }
        rule.setUpdateBy(currentUsername());
        return quoteSchemeMapper.updateQuoteSchemeValueFeeRule(rule);
    }

    @Override
    @Transactional
    public int deleteQuoteSchemeValueFeeRule(Long schemeId, Long valueFeeRuleId)
    {
        requireScheme(schemeId);
        requireValueFeeRule(schemeId, valueFeeRuleId);
        return quoteSchemeMapper.deleteQuoteSchemeValueFeeRule(schemeId, valueFeeRuleId);
    }

    @Override
    public List<QuoteSchemeOption> selectBuyerOptions(String keyword)
    {
        return requireBuyerLookupService().selectBuyerOptions(keyword);
    }

    @Override
    public List<QuoteSchemeOption> selectWarehouseOptions(String keyword)
    {
        return requireWarehouseLookupService().selectWarehouseOptions(keyword);
    }

    @Override
    public List<QuoteSchemeOption> selectCustomerChannelOptions(String keyword)
    {
        return requireCustomerChannelLookupService().selectCustomerChannelOptions(keyword);
    }

    @Override
    public List<QuoteSchemeOption> selectSystemChannelOptions(String keyword)
    {
        return requireSystemChannelLookupService().selectSystemChannelOptions(keyword);
    }

    @Override
    public List<QuoteSchemeOption> selectFeePlaceholderOptions(String feeType)
    {
        return Collections.emptyList();
    }

    private void normalizeScheme(QuoteScheme scheme, boolean creating)
    {
        if (scheme == null)
        {
            throw new ServiceException("报价方案不能为空");
        }
        if (creating)
        {
            scheme.setSchemeCode(generateInternalSchemeCode(scheme.getSchemeCode()));
        }
        scheme.setSchemeName(requireText(scheme.getSchemeName(), "报价方案名称", 200));
        scheme.setSchemeType(normalizeEnum(scheme.getSchemeType(), TYPE_BILLING, TYPE_BILLING, TYPE_COST, "方案类型"));
        scheme.setFeeSourceMode(normalizeEnum(scheme.getFeeSourceMode(), SOURCE_EXTERNAL_ESTIMATE,
            SOURCE_EXTERNAL_ESTIMATE, SOURCE_INTERNAL_RATE, "费用来源模式"));
        scheme.setCurrencyCode(requireCode(scheme.getCurrencyCode(), "币种"));
        financeCurrencyService.selectCurrencyByCode(scheme.getCurrencyCode());
        scheme.setScopeType(normalizeEnum(scheme.getScopeType(), SCOPE_ALL_BUYERS,
            SCOPE_ALL_BUYERS, SCOPE_BUYER_LEVEL, SCOPE_BUYER, "适用对象类型"));
        scheme.setWarehouseScopeMode(normalizeEnum(scheme.getWarehouseScopeMode(), WAREHOUSE_ALL,
            WAREHOUSE_ALL, WAREHOUSE_INCLUDE, "仓库范围模式"));
        scheme.setWarehouseCodes(normalizeWarehouseCodesForScope(scheme.getWarehouseScopeMode(), scheme.getWarehouseCodes()));
        scheme.setStatus(normalizeStatus(scheme.getStatus()));
        if (scheme.getEffectiveTime() == null)
        {
            throw new ServiceException("生效时间不能为空");
        }
        if (scheme.getExpireTime() != null && !scheme.getExpireTime().after(scheme.getEffectiveTime()))
        {
            throw new ServiceException("失效时间必须晚于生效时间");
        }
        if (scheme.getEffectivePriority() == null)
        {
            scheme.setEffectivePriority(0);
        }
    }

    private void assertEffectivePriorityNotConflicting(QuoteScheme scheme)
    {
        if (!STATUS_ENABLED.equals(scheme.getStatus()))
        {
            return;
        }
        int conflictCount = quoteSchemeMapper.countOverlappingEnabledSchemeWithSamePriority(
            scheme.getSchemeId(), scheme.getSchemeType(), scheme.getEffectiveTime(),
            scheme.getExpireTime(), scheme.getEffectivePriority());
        if (conflictCount > 0)
        {
            throw new ServiceException("同一方案类型下，生效时间有交叉的报价方案必须使用不同的生效优先级");
        }
    }

    private void saveScopes(Long schemeId, QuoteScheme scheme)
    {
        quoteSchemeMapper.deleteQuoteSchemeScopes(schemeId);
        if (SCOPE_ALL_BUYERS.equals(scheme.getScopeType()))
        {
            return;
        }
        if (SCOPE_BUYER_LEVEL.equals(scheme.getScopeType()))
        {
            for (String buyerLevelCode : normalizeCodes(scheme.getBuyerLevelCodes(), "买家等级"))
            {
                QuoteSchemeScope scope = new QuoteSchemeScope();
                scope.setSchemeId(schemeId);
                scope.setScopeType(SCOPE_BUYER_LEVEL);
                scope.setScopeKey("LEVEL:" + buyerLevelCode);
                scope.setBuyerLevelCode(buyerLevelCode);
                scope.setBuyerLevelNameSnapshot(resolveBuyerLevelLabel(buyerLevelCode));
                scope.setCreateBy(currentUsername());
                quoteSchemeMapper.insertQuoteSchemeScope(scope);
            }
            return;
        }
        for (Long buyerId : normalizeIds(scheme.getBuyerIds(), "买家"))
        {
            QuoteSchemeOption buyer = requireBuyerLookupService().selectBuyerOption(buyerId);
            if (buyer == null)
            {
                throw new ServiceException("买家不存在或已停用：" + buyerId);
            }
            QuoteSchemeScope scope = new QuoteSchemeScope();
            scope.setSchemeId(schemeId);
            scope.setScopeType(SCOPE_BUYER);
            scope.setScopeKey("BUYER:" + buyerId);
            scope.setBuyerId(buyerId);
            scope.setBuyerCodeSnapshot(buyer.getCode());
            scope.setBuyerNameSnapshot(buyer.getName());
            scope.setBuyerShortNameSnapshot(buyer.getShortName());
            scope.setCreateBy(currentUsername());
            quoteSchemeMapper.insertQuoteSchemeScope(scope);
        }
    }

    private void saveWarehouses(Long schemeId, String warehouseScopeMode, List<String> warehouseCodes)
    {
        quoteSchemeMapper.deleteQuoteSchemeWarehouses(schemeId);
        if (WAREHOUSE_ALL.equals(warehouseScopeMode))
        {
            return;
        }
        for (String warehouseCode : normalizeWarehouseCodesForScope(warehouseScopeMode, warehouseCodes))
        {
            QuoteSchemeOption warehouse = requireWarehouseLookupService().selectWarehouseOption(warehouseCode);
            if (warehouse == null)
            {
                throw new ServiceException("仓库不存在或已停用：" + warehouseCode);
            }
            QuoteSchemeWarehouse detail = new QuoteSchemeWarehouse();
            detail.setSchemeId(schemeId);
            detail.setWarehouseCode(warehouse.getCode());
            detail.setWarehouseNameSnapshot(warehouse.getName());
            detail.setWarehouseKindSnapshot(warehouse.getKind());
            detail.setCreateBy(currentUsername());
            quoteSchemeMapper.insertQuoteSchemeWarehouse(detail);
        }
    }

    private List<String> normalizeWarehouseCodesForScope(String warehouseScopeMode, List<String> warehouseCodes)
    {
        if (WAREHOUSE_ALL.equals(warehouseScopeMode))
        {
            return Collections.emptyList();
        }
        List<String> normalizedCodes = normalizeCodes(warehouseCodes, "仓库");
        if (normalizedCodes.size() > 1)
        {
            throw new ServiceException("仓库最多只能选择一个");
        }
        return normalizedCodes;
    }

    private void normalizeChannel(QuoteScheme scheme, QuoteSchemeChannel channel)
    {
        if (channel == null)
        {
            throw new ServiceException("方案物流渠道不能为空");
        }
        channel.setSchemeId(scheme.getSchemeId());
        channel.setCustomerChannelCode(requireCode(channel.getCustomerChannelCode(), "物流渠道"));
        QuoteSchemeOption logisticsChannel = resolveLogisticsChannel(scheme, channel.getCustomerChannelCode());
        if (logisticsChannel == null)
        {
            throw new ServiceException(getLogisticsChannelLabel(scheme) + "不存在或已停用：" + channel.getCustomerChannelCode());
        }
        channel.setCustomerChannelNameSnapshot(logisticsChannel.getName());
        channel.setOperationFeeCode(trimOptional(channel.getOperationFeeCode()));
        channel.setOperationFeeNameSnapshot(trimOptional(channel.getOperationFeeNameSnapshot()));
        channel.setFreightFeeCode(trimOptional(channel.getFreightFeeCode()));
        channel.setFreightFeeNameSnapshot(trimOptional(channel.getFreightFeeNameSnapshot()));
        channel.setStatus(normalizeStatus(channel.getStatus()));
        if (channel.getDisplayOrder() != null && channel.getDisplayOrder() < 0)
        {
            throw new ServiceException("排序不能小于 0");
        }
    }

    private void normalizeValueFeeRule(QuoteScheme scheme, QuoteSchemeValueFeeRule rule)
    {
        if (rule == null)
        {
            throw new ServiceException("增值费规则不能为空");
        }
        rule.setSchemeId(scheme.getSchemeId());
        rule.setLogisticsChannelCode(requireCode(rule.getLogisticsChannelCode(), "物流渠道"));
        QuoteSchemeOption logisticsChannel = resolveLogisticsChannel(scheme, rule.getLogisticsChannelCode());
        if (logisticsChannel == null)
        {
            throw new ServiceException(getLogisticsChannelLabel(scheme) + "不存在或已停用：" + rule.getLogisticsChannelCode());
        }
        rule.setLogisticsChannelNameSnapshot(logisticsChannel.getName());
        rule.setTriggerCode(normalizeEnum(rule.getTriggerCode(), VALUE_FEE_TRIGGER_ORDER_CANCELLED,
            new String[] { VALUE_FEE_TRIGGER_ORDER_CANCELLED }, "触发情况"));
        rule.setCalculationMethod(normalizeEnum(rule.getCalculationMethod(), VALUE_FEE_CALC_PERCENT,
            VALUE_FEE_CALC_PERCENT, VALUE_FEE_CALC_FIXED_AMOUNT, "收费方式"));
        rule.setAdjustmentDirection(normalizeEnum(rule.getAdjustmentDirection(), VALUE_FEE_DIRECTION_INCREASE,
            VALUE_FEE_DIRECTION_INCREASE, VALUE_FEE_DIRECTION_DECREASE, "调整方向"));
        if (rule.getAdjustmentValue() == null)
        {
            throw new ServiceException("调整值不能为空");
        }
        if (rule.getAdjustmentValue().compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("调整值不能小于 0");
        }
        rule.setStatus(normalizeStatus(rule.getStatus()));
        if (rule.getDisplayOrder() != null && rule.getDisplayOrder() < 0)
        {
            throw new ServiceException("排序不能小于 0");
        }
    }

    private QuoteSchemeOption resolveLogisticsChannel(QuoteScheme scheme, String logisticsChannelCode)
    {
        if (TYPE_COST.equals(scheme.getSchemeType()))
        {
            return requireSystemChannelLookupService().selectSystemChannelOption(logisticsChannelCode);
        }
        return requireCustomerChannelLookupService().selectCustomerChannelOption(logisticsChannelCode);
    }

    private String getLogisticsChannelLabel(QuoteScheme scheme)
    {
        return TYPE_COST.equals(scheme.getSchemeType()) ? "系统物流渠道" : "客户物流渠道";
    }

    private QuoteScheme requireScheme(Long schemeId)
    {
        if (schemeId == null)
        {
            throw new ServiceException("报价方案 ID 不能为空");
        }
        QuoteScheme scheme = quoteSchemeMapper.selectQuoteSchemeById(schemeId);
        if (scheme == null)
        {
            throw new ServiceException("报价方案不存在");
        }
        return scheme;
    }

    private QuoteSchemeChannel requireSchemeChannel(Long schemeId, Long schemeChannelId)
    {
        if (schemeChannelId == null)
        {
            throw new ServiceException("方案客户渠道 ID 不能为空");
        }
        QuoteSchemeChannel channel = quoteSchemeMapper.selectQuoteSchemeChannelById(schemeId, schemeChannelId);
        if (channel == null)
        {
            throw new ServiceException("方案客户渠道不存在");
        }
        return channel;
    }

    private QuoteSchemeValueFeeRule requireValueFeeRule(Long schemeId, Long valueFeeRuleId)
    {
        if (valueFeeRuleId == null)
        {
            throw new ServiceException("增值费规则 ID 不能为空");
        }
        QuoteSchemeValueFeeRule rule = quoteSchemeMapper.selectQuoteSchemeValueFeeRuleById(schemeId, valueFeeRuleId);
        if (rule == null)
        {
            throw new ServiceException("增值费规则不存在");
        }
        return rule;
    }

    private String resolveBuyerLevelLabel(String buyerLevelCode)
    {
        String label = dictDataService.selectDictLabel(BUYER_LEVEL_DICT, buyerLevelCode);
        if (StringUtils.isBlank(label))
        {
            throw new ServiceException("买家等级不存在：" + buyerLevelCode);
        }
        return label;
    }

    private String generateInternalSchemeCode(String submittedCode)
    {
        if (StringUtils.isNotBlank(submittedCode))
        {
            return requireCode(submittedCode, "报价方案编码");
        }
        return INTERNAL_SCHEME_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private QuoteSchemeBuyerLookupService requireBuyerLookupService()
    {
        QuoteSchemeBuyerLookupService service = buyerLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("买家资料查询服务不可用");
        }
        return service;
    }

    private QuoteSchemeWarehouseLookupService requireWarehouseLookupService()
    {
        QuoteSchemeWarehouseLookupService service = warehouseLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("仓库资料查询服务不可用");
        }
        return service;
    }

    private QuoteSchemeCustomerChannelLookupService requireCustomerChannelLookupService()
    {
        QuoteSchemeCustomerChannelLookupService service = customerChannelLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("客户渠道查询服务不可用");
        }
        return service;
    }

    private QuoteSchemeSystemChannelLookupService requireSystemChannelLookupService()
    {
        QuoteSchemeSystemChannelLookupService service = systemChannelLookupServiceProvider.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("系统物流渠道查询服务不可用");
        }
        return service;
    }

    private String normalizeStatus(String status)
    {
        return normalizeEnum(status, STATUS_ENABLED, STATUS_ENABLED, STATUS_DISABLED, "状态");
    }

    private String normalizeEnum(String value, String defaultValue, String allowedA, String allowedB, String fieldName)
    {
        return normalizeEnum(value, defaultValue, new String[] { allowedA, allowedB }, fieldName);
    }

    private String normalizeEnum(String value, String defaultValue, String allowedA, String allowedB, String allowedC,
        String fieldName)
    {
        return normalizeEnum(value, defaultValue, new String[] { allowedA, allowedB, allowedC }, fieldName);
    }

    private String normalizeEnum(String value, String defaultValue, String[] allowedValues, String fieldName)
    {
        String normalized = StringUtils.defaultIfBlank(value, defaultValue).trim().toUpperCase(Locale.ROOT);
        for (String allowedValue : allowedValues)
        {
            if (allowedValue.equals(normalized))
            {
                return normalized;
            }
        }
        throw new ServiceException(fieldName + "不合法：" + value);
    }

    private String requireCode(String value, String fieldName)
    {
        String normalized = requireText(value, fieldName, 64).toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_\\-]+"))
        {
            throw new ServiceException(fieldName + "只能包含字母、数字、下划线或短横线");
        }
        return normalized;
    }

    private String requireText(String value, String fieldName, int maxLength)
    {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null)
        {
            throw new ServiceException(fieldName + "不能为空");
        }
        if (normalized.length() > maxLength)
        {
            throw new ServiceException(fieldName + "长度不能超过 " + maxLength);
        }
        return normalized;
    }

    private String trimOptional(String value)
    {
        return StringUtils.trimToNull(value);
    }

    private List<String> normalizeCodes(List<String> values, String fieldName)
    {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : emptyIfNull(values))
        {
            normalized.add(requireCode(value, fieldName));
        }
        if (normalized.isEmpty())
        {
            throw new ServiceException(fieldName + "不能为空");
        }
        return new ArrayList<>(normalized);
    }

    private List<Long> normalizeIds(List<Long> values, String fieldName)
    {
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long value : emptyIfNull(values))
        {
            if (value != null)
            {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty())
        {
            throw new ServiceException(fieldName + "不能为空");
        }
        return new ArrayList<>(normalized);
    }

    private <T> List<T> emptyIfNull(List<T> values)
    {
        return values == null ? Collections.emptyList() : values;
    }

    private String currentUsername()
    {
        return SecurityUtils.getUsername();
    }
}
