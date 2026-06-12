package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.FeeEstimateSkuSnapshot;
import com.ruoyi.finance.domain.FeeEstimateSkuWarehouseCandidate;
import com.ruoyi.finance.domain.query.FeeEstimateSkuQuery;

/**
 * Product-owned SKU lookup port used by finance fee estimates.
 */
public interface FinanceFeeEstimateSkuLookupService
{
    List<FeeEstimateSkuSnapshot> selectSkuSnapshots(FeeEstimateSkuQuery query);

    List<FeeEstimateSkuSnapshot> selectSkuSnapshotsByIds(List<Long> skuIds);

    List<FeeEstimateSkuWarehouseCandidate> selectSkuWarehouseCandidatesByIds(List<Long> skuIds);
}
