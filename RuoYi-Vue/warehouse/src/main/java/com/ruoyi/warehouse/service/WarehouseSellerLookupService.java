package com.ruoyi.warehouse.service;

import java.util.Collection;
import java.util.List;
import com.ruoyi.warehouse.domain.WarehouseSellerProfile;

/**
 * 仓库模块读取卖家资料的扩展点，由 seller 模块提供实现。
 */
public interface WarehouseSellerLookupService
{
    boolean isNormalSeller(Long sellerId);

    WarehouseSellerProfile selectSellerProfile(Long sellerId);

    List<WarehouseSellerProfile> selectSellerProfilesByIds(Collection<Long> sellerIds);

    List<WarehouseSellerProfile> selectSellerProfilesByKeyword(String keyword);

    List<WarehouseSellerProfile> selectNormalSellerOptions(String keyword);
}
