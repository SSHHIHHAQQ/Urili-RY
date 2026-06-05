package com.ruoyi.product.service;

import com.ruoyi.product.domain.ProductSellerSnapshot;

/**
 * 商品模块读取卖家快照的扩展点，由 seller 模块提供实现。
 */
public interface ProductSellerLookupService
{
    ProductSellerSnapshot selectSellerSnapshot(Long sellerId);
}
