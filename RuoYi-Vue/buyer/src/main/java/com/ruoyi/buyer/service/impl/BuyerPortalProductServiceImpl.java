package com.ruoyi.buyer.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.pagehelper.Page;
import com.ruoyi.buyer.domain.BuyerAccount;
import com.ruoyi.buyer.domain.BuyerPortalProduct;
import com.ruoyi.buyer.domain.BuyerPortalProductSku;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.buyer.service.IBuyerPortalProductService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.service.IProductDistributionService;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Buyer terminal product browsing facade. Buyer identity is authenticated but does not define product ownership.
 */
@Service
public class BuyerPortalProductServiceImpl implements IBuyerPortalProductService
{
    private static final String TERMINAL_BUYER = "buyer";
    private static final String STATUS_ON_SALE = "ON_SALE";

    @Autowired
    private IProductDistributionService productDistributionService;

    @Autowired
    private BuyerMapper buyerMapper;

    @Override
    public List<BuyerPortalProduct> selectVisibleProductList(PortalLoginSession session, ProductSpu query)
    {
        assertBuyerSession(session);
        List<ProductSpu> products = productDistributionService.selectOnSaleProductList(buildVisibleProductQuery(query));
        List<BuyerPortalProduct> result = newPortalProductList(products);
        for (ProductSpu product : products)
        {
            result.add(toPortalProduct(product));
        }
        return result;
    }

    @Override
    public BuyerPortalProduct selectVisibleProductById(PortalLoginSession session, Long spuId)
    {
        return toPortalProduct(requireVisibleProduct(session, spuId));
    }

    @Override
    public List<BuyerPortalProductSku> selectVisibleSkuList(PortalLoginSession session, Long spuId)
    {
        requireVisibleProduct(session, spuId);
        return toPortalSkus(productDistributionService.selectOnSaleSkuList(spuId));
    }

    private ProductSpu buildVisibleProductQuery(ProductSpu query)
    {
        ProductSpu scoped = new ProductSpu();
        if (query == null)
        {
            return scoped;
        }
        scoped.setKeyword(query.getKeyword());
        scoped.setProductName(query.getProductName());
        scoped.setProductNameEn(query.getProductNameEn());
        scoped.setCategoryId(query.getCategoryId());
        return scoped;
    }

    private List<BuyerPortalProduct> newPortalProductList(List<ProductSpu> products)
    {
        if (products instanceof Page)
        {
            Page<?> source = (Page<?>) products;
            Page<BuyerPortalProduct> result = new Page<>(source.getPageNum(), source.getPageSize(),
                source.isCount());
            result.setTotal(source.getTotal());
            result.setPages(source.getPages());
            result.setStartRow(source.getStartRow());
            result.setEndRow(source.getEndRow());
            result.setReasonable(source.getReasonable());
            result.setPageSizeZero(source.getPageSizeZero());
            result.setOrderBy(source.getOrderBy());
            result.setOrderByOnly(source.isOrderByOnly());
            result.setCount(source.isCount());
            return result;
        }
        return new ArrayList<>();
    }

    private ProductSpu requireVisibleProduct(PortalLoginSession session, Long spuId)
    {
        assertBuyerSession(session);
        if (spuId == null)
        {
            throw new ServiceException("商城商品不存在");
        }
        ProductSpu product = productDistributionService.selectOnSaleProductById(spuId);
        if (product == null || !STATUS_ON_SALE.equals(product.getSpuStatus()) || visibleSkus(product.getSkus()).isEmpty())
        {
            throw new ServiceException("商城商品不存在");
        }
        return product;
    }

    private void assertBuyerSession(PortalLoginSession session)
    {
        if (session == null || session.getSubjectId() == null || session.getAccountId() == null
            || StringUtils.isBlank(session.getTokenId()) || !TERMINAL_BUYER.equals(session.getTerminal()))
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        BuyerAccount account = buyerMapper.selectBuyerAccountByIdAndBuyerId(session.getSubjectId(),
            session.getAccountId());
        if (account == null)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
    }

    private BuyerPortalProduct toPortalProduct(ProductSpu product)
    {
        List<ProductSku> visibleSkus = visibleSkus(product.getSkus());
        BuyerPortalProduct result = new BuyerPortalProduct();
        result.setSpuId(product.getSpuId());
        result.setSystemSpuCode(product.getSystemSpuCode());
        result.setCategoryId(product.getCategoryId());
        result.setCategoryCode(product.getCategoryCode());
        result.setCategoryName(product.getCategoryName());
        result.setProductName(product.getProductName());
        result.setProductNameEn(product.getProductNameEn());
        result.setSellingPoint(product.getSellingPoint());
        result.setMainImageUrl(product.getMainImageUrl());
        result.setDetailContent(product.getDetailContent());
        result.setSpuStatus(product.getSpuStatus());
        result.setSkuCount(visibleSkus.size());
        result.setSalePriceMin(minSalePrice(visibleSkus));
        result.setSalePriceMax(maxSalePrice(visibleSkus));
        result.setCurrencySummary(currencySummary(visibleSkus));
        result.setWarehouseCount(product.getWarehouseCount());
        result.setSkus(toPortalSkus(visibleSkus));
        return result;
    }

    private List<ProductSku> visibleSkus(List<ProductSku> skus)
    {
        List<ProductSku> result = new ArrayList<>();
        if (skus == null)
        {
            return result;
        }
        for (ProductSku sku : skus)
        {
            if (STATUS_ON_SALE.equals(sku.getSkuStatus()))
            {
                result.add(sku);
            }
        }
        return result;
    }

    private BigDecimal minSalePrice(List<ProductSku> skus)
    {
        BigDecimal result = null;
        for (ProductSku sku : skus)
        {
            BigDecimal salePrice = sku.getSalePrice();
            if (salePrice != null && (result == null || salePrice.compareTo(result) < 0))
            {
                result = salePrice;
            }
        }
        return result;
    }

    private BigDecimal maxSalePrice(List<ProductSku> skus)
    {
        BigDecimal result = null;
        for (ProductSku sku : skus)
        {
            BigDecimal salePrice = sku.getSalePrice();
            if (salePrice != null && (result == null || salePrice.compareTo(result) > 0))
            {
                result = salePrice;
            }
        }
        return result;
    }

    private String currencySummary(List<ProductSku> skus)
    {
        Set<String> currencies = new HashSet<>();
        for (ProductSku sku : skus)
        {
            if (sku.getCurrencyCode() != null && !sku.getCurrencyCode().isBlank())
            {
                currencies.add(sku.getCurrencyCode());
            }
        }
        if (currencies.isEmpty())
        {
            return null;
        }
        if (currencies.size() == 1)
        {
            return currencies.iterator().next();
        }
        return "MULTI";
    }

    private List<BuyerPortalProductSku> toPortalSkus(List<ProductSku> skus)
    {
        List<BuyerPortalProductSku> result = new ArrayList<>();
        if (skus == null)
        {
            return result;
        }
        for (ProductSku sku : skus)
        {
            result.add(toPortalSku(sku));
        }
        return result;
    }

    private BuyerPortalProductSku toPortalSku(ProductSku sku)
    {
        BuyerPortalProductSku result = new BuyerPortalProductSku();
        result.setSkuId(sku.getSkuId());
        result.setSpuId(sku.getSpuId());
        result.setSystemSkuCode(sku.getSystemSkuCode());
        result.setColor(sku.getColor());
        result.setSize(sku.getSize());
        result.setLengthValue(sku.getLengthValue());
        result.setWidthValue(sku.getWidthValue());
        result.setHeightValue(sku.getHeightValue());
        result.setWeight(sku.getWeight());
        result.setMaterial(sku.getMaterial());
        result.setStyle(sku.getStyle());
        result.setModel(sku.getModel());
        result.setPackageQuantity(sku.getPackageQuantity());
        result.setCapacity(sku.getCapacity());
        result.setSkuImageUrl(sku.getSkuImageUrl());
        result.setSalePrice(sku.getSalePrice());
        result.setCurrencyCode(sku.getCurrencyCode());
        result.setSkuStatus(sku.getSkuStatus());
        result.setSortOrder(sku.getSortOrder());
        result.setWarehouseCount(sku.getWarehouseCount());
        return result;
    }
}
