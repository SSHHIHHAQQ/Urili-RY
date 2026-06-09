package com.ruoyi.product.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.product.service.IProductPortalDistributionService;

/**
 * Mapper-backed read-only distribution queries for terminal portals.
 */
@Service
public class ProductPortalDistributionServiceImpl implements IProductPortalDistributionService
{
    @Autowired
    private ProductDistributionMapper productDistributionMapper;

    @Override
    public List<ProductSpu> selectSellerProductList(ProductSpu query)
    {
        List<ProductSpu> products = productDistributionMapper.selectProductList(query);
        for (ProductSpu product : products)
        {
            Long sellerId = query != null && query.getSellerId() != null ? query.getSellerId() : product.getSellerId();
            product.setSkus(productDistributionMapper.selectSkuListBySpuIdAndSellerId(product.getSpuId(), sellerId));
        }
        return products;
    }

    @Override
    public ProductSpu selectSellerProductById(Long spuId, Long sellerId)
    {
        if (sellerId == null)
        {
            throw new ServiceException("卖家主体不能为空");
        }
        ProductSpu product = productDistributionMapper.selectProductByIdAndSellerId(spuId, sellerId);
        if (product == null)
        {
            throw new ServiceException("商城商品不存在");
        }
        product.setSkus(productDistributionMapper.selectSkuListBySpuIdAndSellerId(spuId, sellerId));
        return product;
    }

    @Override
    public List<ProductSku> selectSellerSkuList(Long spuId, Long sellerId)
    {
        selectSellerProductById(spuId, sellerId);
        return productDistributionMapper.selectSkuListBySpuIdAndSellerId(spuId, sellerId);
    }

    @Override
    public List<ProductSpu> selectBuyerVisibleProductList(ProductSpu query)
    {
        List<ProductSpu> products = productDistributionMapper.selectOnSaleProductList(query);
        for (ProductSpu product : products)
        {
            product.setSkus(productDistributionMapper.selectOnSaleSkuListBySpuId(product.getSpuId()));
        }
        return products;
    }

    @Override
    public ProductSpu selectBuyerVisibleProductById(Long spuId)
    {
        ProductSpu product = productDistributionMapper.selectOnSaleProductById(spuId);
        if (product == null)
        {
            throw new ServiceException("商城商品不存在");
        }
        product.setSkus(productDistributionMapper.selectOnSaleSkuListBySpuId(spuId));
        return product;
    }

    @Override
    public List<ProductSku> selectBuyerVisibleSkuList(Long spuId)
    {
        selectBuyerVisibleProductById(spuId);
        return productDistributionMapper.selectOnSaleSkuListBySpuId(spuId);
    }
}
