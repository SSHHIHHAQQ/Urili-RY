package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.FeeEstimateOptions;
import com.ruoyi.finance.domain.FeeEstimateResponse;
import com.ruoyi.finance.domain.FeeEstimateSkuSnapshot;
import com.ruoyi.finance.domain.query.FeeEstimateSkuQuery;
import com.ruoyi.finance.domain.request.FeeEstimateRequest;

public interface IFeeEstimateService
{
    FeeEstimateOptions selectOptions(Long schemeId);

    List<FeeEstimateSkuSnapshot> selectSkuSnapshots(FeeEstimateSkuQuery query);

    FeeEstimateResponse calculate(FeeEstimateRequest request);
}
