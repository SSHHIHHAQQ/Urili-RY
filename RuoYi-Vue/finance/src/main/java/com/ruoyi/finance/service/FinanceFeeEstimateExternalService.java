package com.ruoyi.finance.service;

import com.ruoyi.finance.domain.FeeEstimateExternalRequest;
import com.ruoyi.finance.domain.FeeEstimateExternalResult;

/**
 * External-system adapter port used by finance fee estimates.
 */
public interface FinanceFeeEstimateExternalService
{
    FeeEstimateExternalResult estimate(FeeEstimateExternalRequest request);
}
