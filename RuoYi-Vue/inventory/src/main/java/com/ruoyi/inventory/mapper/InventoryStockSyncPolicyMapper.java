package com.ruoyi.inventory.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.inventory.domain.InventoryStockSyncPolicy;

/**
 * 库存同步策略 Mapper。
 */
public interface InventoryStockSyncPolicyMapper
{
    int upsertPolicy(InventoryStockSyncPolicy policy);

    InventoryStockSyncPolicy selectPolicyByKey(@Param("policyKey") String policyKey);

    List<InventoryStockSyncPolicy> selectEnabledPoliciesBySellerIds(@Param("sellerIds") List<Long> sellerIds);

    List<InventoryStockSyncPolicy> selectEnabledPoliciesBySpuId(@Param("spuId") Long spuId);
}
