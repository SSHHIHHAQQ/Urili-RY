package com.ruoyi.seller.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.product.domain.ProductSellerSnapshot;
import com.ruoyi.product.service.ProductSellerLookupService;
import com.ruoyi.seller.domain.Seller;
import com.ruoyi.seller.service.ISellerService;

/**
 * 商品模块读取卖家快照的 seller 端实现。
 */
@Service
public class ProductSellerLookupServiceImpl implements ProductSellerLookupService
{
    @Autowired
    private ISellerService sellerService;

    @Override
    public ProductSellerSnapshot selectSellerSnapshot(Long sellerId)
    {
        Seller seller = sellerService.selectSellerById(sellerId);
        return new ProductSellerSnapshot(seller.getSellerId(), seller.getSellerNo(), seller.getCompanyName());
    }
}
