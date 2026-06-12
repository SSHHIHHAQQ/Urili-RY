package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.FeeEstimateRouteCandidate;

/**
 * Logistics-owned route lookup port used by finance fee estimates.
 */
public interface FinanceFeeEstimateLogisticsLookupService
{
    List<FeeEstimateRouteCandidate> selectRouteCandidates(List<String> customerChannelCodes,
        List<String> warehouseCodes, Long buyerId, boolean autoMode);
}
