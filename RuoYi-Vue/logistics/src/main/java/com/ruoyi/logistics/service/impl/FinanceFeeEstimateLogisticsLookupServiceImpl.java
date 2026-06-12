package com.ruoyi.logistics.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.finance.domain.FeeEstimateRouteCandidate;
import com.ruoyi.finance.service.FinanceFeeEstimateLogisticsLookupService;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsCustomerChannel;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelBuyerScope;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelSystemMapping;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.LogisticsSystemChannelWarehouse;
import com.ruoyi.logistics.mapper.LogisticsCustomerChannelMapper;
import com.ruoyi.logistics.mapper.LogisticsSystemChannelMapper;
import com.ruoyi.logistics.support.LogisticsConstants;

/**
 * Logistics implementation of the finance fee estimate route lookup port.
 */
@Service
public class FinanceFeeEstimateLogisticsLookupServiceImpl implements FinanceFeeEstimateLogisticsLookupService
{
    private static final String LABEL_REQUIRED = "REQUIRED";

    private static final String BUYER_SCOPE_ALL = "ALL";

    private static final String BUYER_SCOPE_INCLUDE = "INCLUDE";

    private static final String BUYER_SCOPE_EXCLUDE = "EXCLUDE";

    private static final String FULFILLMENT_CARRIER_LABELING = "CARRIER_LABELING";

    @Autowired
    private LogisticsCustomerChannelMapper customerChannelMapper;

    @Autowired
    private LogisticsSystemChannelMapper systemChannelMapper;

    @Override
    public List<FeeEstimateRouteCandidate> selectRouteCandidates(List<String> customerChannelCodes,
        List<String> warehouseCodes, Long buyerId, boolean autoMode)
    {
        Set<String> normalizedChannelCodes = normalizeCodeSet(customerChannelCodes);
        Set<String> normalizedWarehouseCodes = normalizeCodeSet(warehouseCodes);
        if (normalizedChannelCodes.isEmpty() || normalizedWarehouseCodes.isEmpty())
        {
            return List.of();
        }
        List<FeeEstimateRouteCandidate> candidates = new ArrayList<>();
        for (String customerChannelCode : normalizedChannelCodes)
        {
            resolveCustomerChannel(candidates, customerChannelCode, normalizedWarehouseCodes, buyerId, autoMode);
        }
        return candidates;
    }

    private void resolveCustomerChannel(List<FeeEstimateRouteCandidate> candidates, String customerChannelCode,
        Set<String> warehouseCodes, Long buyerId, boolean autoMode)
    {
        LogisticsCustomerChannel customerChannel = customerChannelMapper.selectCustomerChannelByCode(customerChannelCode);
        if (customerChannel == null || !LogisticsConstants.STATUS_ENABLED.equals(customerChannel.getStatus()))
        {
            candidates.add(failed(customerChannelCode, null, null, "CUSTOMER_CHANNEL_DISABLED",
                "客户渠道不存在或已停用"));
            return;
        }
        if (autoMode && LABEL_REQUIRED.equals(customerChannel.getLabelUploadRequired()))
        {
            candidates.add(failed(customerChannelCode, customerChannel.getCustomerChannelName(), null,
                "CUSTOMER_LABEL_REQUIRED", "自动最优不使用客户上传面单渠道"));
            return;
        }
        if (!buyerVisible(customerChannel, buyerId))
        {
            candidates.add(failed(customerChannelCode, customerChannel.getCustomerChannelName(), null,
                "CUSTOMER_CHANNEL_BUYER_BLOCKED", "客户渠道未对当前买家开放"));
            return;
        }

        List<LogisticsCustomerChannelSystemMapping> mappings = customerChannelMapper
            .selectSystemMappingList(customerChannelCode).stream()
            .filter(mapping -> LogisticsConstants.STATUS_ENABLED.equals(mapping.getStatus()))
            .collect(Collectors.toList());
        if (mappings.isEmpty())
        {
            candidates.add(failed(customerChannelCode, customerChannel.getCustomerChannelName(), null,
                "SYSTEM_MAPPING_MISSING", "客户渠道未映射系统渠道"));
            return;
        }
        for (LogisticsCustomerChannelSystemMapping mapping : mappings)
        {
            resolveSystemChannel(candidates, customerChannel, mapping, warehouseCodes);
        }
    }

    private void resolveSystemChannel(List<FeeEstimateRouteCandidate> candidates,
        LogisticsCustomerChannel customerChannel, LogisticsCustomerChannelSystemMapping mapping,
        Set<String> warehouseCodes)
    {
        LogisticsSystemChannel systemChannel = systemChannelMapper.selectSystemChannelByCode(mapping.getSystemChannelCode());
        if (systemChannel == null || !LogisticsConstants.STATUS_ENABLED.equals(systemChannel.getStatus()))
        {
            candidates.add(failed(customerChannel.getCustomerChannelCode(), customerChannel.getCustomerChannelName(),
                mapping.getSystemChannelCode(), "SYSTEM_CHANNEL_DISABLED", "系统渠道不存在或已停用"));
            return;
        }

        List<LogisticsSystemChannelWarehouse> matchedBindings = systemChannelMapper
            .selectWarehouseBindingList(systemChannel.getSystemChannelCode()).stream()
            .filter(binding -> LogisticsConstants.STATUS_ENABLED.equals(binding.getStatus()))
            .filter(binding -> warehouseCodes.contains(StringUtils.trimToEmpty(binding.getWarehouseCode())))
            .collect(Collectors.toList());
        if (matchedBindings.isEmpty())
        {
            candidates.add(failed(customerChannel.getCustomerChannelCode(), customerChannel.getCustomerChannelName(),
                systemChannel.getSystemChannelCode(), "SYSTEM_CHANNEL_WAREHOUSE_MISSING",
                "系统渠道未绑定当前候选仓库"));
            return;
        }

        List<LogisticsCarrierChannelMapping> carrierMappings = systemChannelMapper
            .selectCarrierMappingList(systemChannel.getSystemChannelCode()).stream()
            .filter(mappingRow -> LogisticsConstants.STATUS_ENABLED.equals(mappingRow.getStatus()))
            .collect(Collectors.toList());
        boolean carrierRequired = FULFILLMENT_CARRIER_LABELING.equals(systemChannel.getFulfillmentMode());
        for (LogisticsSystemChannelWarehouse binding : matchedBindings)
        {
            if (carrierRequired && carrierMappings.isEmpty())
            {
                FeeEstimateRouteCandidate failed = baseCandidate(customerChannel, systemChannel, binding);
                failed.setExecutable(Boolean.FALSE);
                failed.setFailureCode("CARRIER_MAPPING_MISSING");
                failed.setFailureMessage("物流商打单渠道未配置物流商渠道映射");
                candidates.add(failed);
                continue;
            }
            if (carrierRequired)
            {
                for (LogisticsCarrierChannelMapping carrierMapping : carrierMappings)
                {
                    FeeEstimateRouteCandidate candidate = baseCandidate(customerChannel, systemChannel, binding);
                    candidate.setCarrierConnectionCode(carrierMapping.getConnectionCode());
                    candidate.setCarrierExternalChannelCode(carrierMapping.getExternalChannelCode());
                    candidate.setExecutable(Boolean.TRUE);
                    candidates.add(candidate);
                }
            }
            else
            {
                FeeEstimateRouteCandidate candidate = baseCandidate(customerChannel, systemChannel, binding);
                candidate.setExecutable(Boolean.TRUE);
                candidates.add(candidate);
            }
        }
    }

    private boolean buyerVisible(LogisticsCustomerChannel channel, Long buyerId)
    {
        String scopeMode = StringUtils.defaultIfBlank(channel.getBuyerScopeMode(), BUYER_SCOPE_ALL);
        if (BUYER_SCOPE_ALL.equals(scopeMode))
        {
            return true;
        }
        List<LogisticsCustomerChannelBuyerScope> scopes = customerChannelMapper
            .selectBuyerScopeList(channel.getCustomerChannelCode());
        boolean containsBuyer = buyerId != null && scopes.stream()
            .anyMatch(scope -> Objects.equals(scope.getBuyerId(), buyerId));
        if (BUYER_SCOPE_INCLUDE.equals(scopeMode))
        {
            return containsBuyer;
        }
        if (BUYER_SCOPE_EXCLUDE.equals(scopeMode))
        {
            return !containsBuyer;
        }
        return false;
    }

    private FeeEstimateRouteCandidate baseCandidate(LogisticsCustomerChannel customerChannel,
        LogisticsSystemChannel systemChannel, LogisticsSystemChannelWarehouse binding)
    {
        FeeEstimateRouteCandidate candidate = new FeeEstimateRouteCandidate();
        candidate.setWarehouseCode(binding.getWarehouseCode());
        candidate.setWarehouseName(binding.getWarehouseName());
        candidate.setWarehouseKind(binding.getWarehouseKind());
        candidate.setCustomerChannelCode(customerChannel.getCustomerChannelCode());
        candidate.setCustomerChannelName(customerChannel.getCustomerChannelName());
        candidate.setLabelUploadRequired(customerChannel.getLabelUploadRequired());
        candidate.setBuyerScopeMode(customerChannel.getBuyerScopeMode());
        candidate.setSystemChannelCode(systemChannel.getSystemChannelCode());
        candidate.setSystemChannelName(systemChannel.getSystemChannelName());
        candidate.setFulfillmentMode(systemChannel.getFulfillmentMode());
        return candidate;
    }

    private FeeEstimateRouteCandidate failed(String customerChannelCode, String customerChannelName,
        String systemChannelCode, String failureCode, String failureMessage)
    {
        FeeEstimateRouteCandidate candidate = new FeeEstimateRouteCandidate();
        candidate.setCustomerChannelCode(customerChannelCode);
        candidate.setCustomerChannelName(customerChannelName);
        candidate.setSystemChannelCode(systemChannelCode);
        candidate.setExecutable(Boolean.FALSE);
        candidate.setFailureCode(failureCode);
        candidate.setFailureMessage(failureMessage);
        return candidate;
    }

    private Set<String> normalizeCodeSet(List<String> codes)
    {
        if (codes == null || codes.isEmpty())
        {
            return Set.of();
        }
        return codes.stream()
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    }
}
