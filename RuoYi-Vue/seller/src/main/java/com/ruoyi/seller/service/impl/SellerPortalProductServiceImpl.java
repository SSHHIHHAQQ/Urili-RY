package com.ruoyi.seller.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.service.IProductPortalDistributionService;
import com.ruoyi.seller.domain.SellerAccount;
import com.ruoyi.seller.domain.SellerPortalProduct;
import com.ruoyi.seller.domain.SellerPortalProductSku;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.seller.service.ISellerPortalProductService;
import com.ruoyi.system.domain.PortalLoginSession;

/**
 * Seller terminal product facade. The seller scope always comes from the portal session.
 */
@Service
public class SellerPortalProductServiceImpl implements ISellerPortalProductService
{
    private static final String TERMINAL_SELLER = "seller";

    @Autowired
    private IProductPortalDistributionService productPortalDistributionService;

    @Autowired
    private SellerMapper sellerMapper;

    @Override
    public List<SellerPortalProduct> selectOwnProductList(PortalLoginSession session, ProductSpu query)
    {
        assertSellerSession(session);
        List<ProductSpu> products = productPortalDistributionService.selectSellerProductList(buildOwnProductQuery(session, query));
        List<SellerPortalProduct> result = newPortalProductList(products);
        for (ProductSpu product : products)
        {
            result.add(toPortalProduct(product, session.getSubjectId()));
        }
        return result;
    }

    @Override
    public SellerPortalProduct selectOwnProductById(PortalLoginSession session, Long spuId)
    {
        ProductSpu product = requireOwnProduct(session, spuId);
        return toPortalProduct(product, session.getSubjectId());
    }

    @Override
    public List<SellerPortalProductSku> selectOwnSkuList(PortalLoginSession session, Long spuId)
    {
        requireOwnProduct(session, spuId);
        return toPortalSkus(productPortalDistributionService.selectSellerSkuList(spuId, session.getSubjectId()));
    }

    private ProductSpu buildOwnProductQuery(PortalLoginSession session, ProductSpu query)
    {
        ProductSpu scoped = new ProductSpu();
        scoped.setSellerId(session.getSubjectId());
        if (query == null)
        {
            return scoped;
        }
        scoped.setKeyword(query.getKeyword());
        scoped.setSellerSpuCode(query.getSellerSpuCode());
        scoped.setProductName(query.getProductName());
        scoped.setProductNameEn(query.getProductNameEn());
        scoped.setCategoryId(query.getCategoryId());
        scoped.setSpuStatus(query.getSpuStatus());
        scoped.setSellerSkuCode(query.getSellerSkuCode());
        return scoped;
    }

    private List<SellerPortalProduct> newPortalProductList(List<ProductSpu> products)
    {
        if (products instanceof Page)
        {
            Page<?> source = (Page<?>) products;
            Page<SellerPortalProduct> result = new Page<>(source.getPageNum(), source.getPageSize(),
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

    private ProductSpu requireOwnProduct(PortalLoginSession session, Long spuId)
    {
        assertSellerSession(session);
        if (spuId == null)
        {
            throw new ServiceException("商城商品不存在");
        }
        ProductSpu product = productPortalDistributionService.selectSellerProductById(spuId, session.getSubjectId());
        if (product == null || !session.getSubjectId().equals(product.getSellerId()))
        {
            throw new ServiceException("商城商品不存在");
        }
        return product;
    }

    private void assertSellerSession(PortalLoginSession session)
    {
        if (session == null || session.getSubjectId() == null || session.getAccountId() == null
            || StringUtils.isBlank(session.getTokenId()) || !TERMINAL_SELLER.equals(session.getTerminal()))
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
        SellerAccount account = selectSellerSessionAccountWithoutPage(session);
        if (account == null)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
    }

    private SellerAccount selectSellerSessionAccountWithoutPage(PortalLoginSession session)
    {
        Page<?> page = PageMethod.getLocalPage();
        if (page == null)
        {
            return sellerMapper.selectSellerAccountByIdAndSellerId(session.getSubjectId(), session.getAccountId());
        }
        try
        {
            PageMethod.clearPage();
            return sellerMapper.selectSellerAccountByIdAndSellerId(session.getSubjectId(), session.getAccountId());
        }
        finally
        {
            PageMethod.setLocalPage(page);
        }
    }

    private SellerPortalProduct toPortalProduct(ProductSpu product, Long sellerId)
    {
        SellerPortalProduct result = new SellerPortalProduct();
        result.setSpuId(product.getSpuId());
        result.setSellerSpuCode(product.getSellerSpuCode());
        result.setCategoryId(product.getCategoryId());
        result.setCategoryCode(product.getCategoryCode());
        result.setCategoryName(product.getCategoryName());
        result.setProductName(product.getProductName());
        result.setProductNameEn(product.getProductNameEn());
        result.setSellingPoint(product.getSellingPoint());
        result.setMainImageUrl(product.getMainImageUrl());
        result.setDetailContent(product.getDetailContent());
        result.setSpuStatus(product.getSpuStatus());
        result.setSkuCount(product.getSkuCount());
        result.setSupplyPriceMin(product.getSupplyPriceMin());
        result.setSupplyPriceMax(product.getSupplyPriceMax());
        result.setSalePriceMin(product.getSalePriceMin());
        result.setSalePriceMax(product.getSalePriceMax());
        result.setCurrencySummary(product.getCurrencySummary());
        result.setWarehouseCount(product.getWarehouseCount());
        result.setSkus(toPortalSkus(productPortalDistributionService.selectSellerSkuList(product.getSpuId(), sellerId)));
        return result;
    }

    private List<SellerPortalProductSku> toPortalSkus(List<ProductSku> skus)
    {
        List<SellerPortalProductSku> result = new ArrayList<>();
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

    private SellerPortalProductSku toPortalSku(ProductSku sku)
    {
        SellerPortalProductSku result = new SellerPortalProductSku();
        result.setSkuId(sku.getSkuId());
        result.setSpuId(sku.getSpuId());
        result.setSellerSkuCode(sku.getSellerSkuCode());
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
        result.setSupplyPrice(sku.getSupplyPrice());
        result.setSalePrice(sku.getSalePrice());
        result.setCurrencyCode(sku.getCurrencyCode());
        result.setSkuStatus(sku.getSkuStatus());
        result.setSortOrder(sku.getSortOrder());
        result.setWarehouseCount(sku.getWarehouseCount());
        return result;
    }
}
