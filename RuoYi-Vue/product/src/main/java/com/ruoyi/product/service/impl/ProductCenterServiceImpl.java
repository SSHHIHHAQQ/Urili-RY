package com.ruoyi.product.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.github.pagehelper.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductAttributeValue;
import com.ruoyi.product.domain.ProductCenterAttribute;
import com.ruoyi.product.domain.ProductCenterProduct;
import com.ruoyi.product.domain.ProductCenterQuery;
import com.ruoyi.product.domain.ProductCenterSku;
import com.ruoyi.product.domain.ProductCenterWarehouse;
import com.ruoyi.product.domain.ProductImage;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductSpuWarehouse;
import com.ruoyi.product.service.IProductCenterService;
import com.ruoyi.product.service.IProductDistributionService;

/**
 * Maps product domain data to a buyer-facing product center shape.
 */
@Service
public class ProductCenterServiceImpl implements IProductCenterService
{
    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String IMAGE_ROLE_GALLERY = "GALLERY";

    @Autowired
    private IProductDistributionService productDistributionService;

    @Override
    public List<ProductCenterProduct> selectProductList(ProductCenterQuery query)
    {
        List<ProductSpu> products = productDistributionService.selectOnSaleProductList(toProductQuery(query));
        List<ProductCenterProduct> result = newProductCenterList(products);
        for (ProductSpu product : products)
        {
            result.add(toProductCenterProduct(product, false));
        }
        return result;
    }

    @Override
    public ProductCenterProduct selectProductById(Long spuId)
    {
        return toProductCenterProduct(requireVisibleProduct(spuId), true);
    }

    @Override
    public List<ProductCenterSku> selectSkuList(Long spuId)
    {
        requireVisibleProduct(spuId);
        return toProductCenterSkus(productDistributionService.selectOnSaleSkuList(spuId));
    }

    private ProductSpu toProductQuery(ProductCenterQuery query)
    {
        ProductSpu productQuery = new ProductSpu();
        if (query == null)
        {
            return productQuery;
        }
        productQuery.setKeyword(query.getKeyword());
        productQuery.setProductName(query.getProductName());
        productQuery.setProductNameEn(query.getProductNameEn());
        productQuery.setSystemSpuCode(query.getSystemSpuCode());
        productQuery.setSystemSkuCode(query.getSystemSkuCode());
        productQuery.setCategoryId(query.getCategoryId());
        return productQuery;
    }

    private ProductSpu requireVisibleProduct(Long spuId)
    {
        if (spuId == null)
        {
            throw new ServiceException("商品不存在");
        }
        ProductSpu product = productDistributionService.selectOnSaleProductById(spuId);
        if (product == null || !STATUS_ON_SALE.equals(product.getSpuStatus()) || visibleSkus(product).isEmpty())
        {
            throw new ServiceException("商品不存在");
        }
        return product;
    }

    private List<ProductCenterProduct> newProductCenterList(List<ProductSpu> products)
    {
        if (products instanceof Page)
        {
            Page<?> source = (Page<?>) products;
            Page<ProductCenterProduct> result = new Page<>(source.getPageNum(), source.getPageSize(),
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

    private ProductCenterProduct toProductCenterProduct(ProductSpu product, boolean includeDetail)
    {
        List<ProductSku> skus = visibleSkus(product);
        ProductCenterProduct result = new ProductCenterProduct();
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
        result.setSkuCount(skus.size());
        result.setVisibleSystemSkuCodes(systemSkuCodes(skus));
        result.setSalePriceMin(product.getSalePriceMin());
        result.setSalePriceMax(product.getSalePriceMax());
        result.setCurrencySummary(product.getCurrencySummary());
        result.setWarehouseKindSummary(product.getWarehouseKindSummary());
        result.setAvailableStock(product.getAvailableStock());
        result.setWarehouseCount(product.getWarehouseCount());
        result.setInventoryStatus(product.getInventoryStatus());
        result.setStockUpdateTime(product.getStockUpdateTime());
        result.setSkus(toProductCenterSkus(skus));
        if (includeDetail)
        {
            result.setGalleryUrls(galleryUrls(product.getImages()));
            result.setWarehouses(toProductCenterWarehouses(product.getWarehouses()));
            result.setAttributes(toProductCenterAttributes(product.getAttributeValues()));
        }
        return result;
    }

    private List<ProductSku> visibleSkus(ProductSpu product)
    {
        List<ProductSku> result = new ArrayList<>();
        if (product == null || product.getSkus() == null)
        {
            return result;
        }
        for (ProductSku sku : product.getSkus())
        {
            if (STATUS_ON_SALE.equals(sku.getSkuStatus()))
            {
                result.add(sku);
            }
        }
        return result;
    }

    private List<String> systemSkuCodes(List<ProductSku> skus)
    {
        List<String> result = new ArrayList<>();
        for (ProductSku sku : skus)
        {
            if (hasText(sku.getSystemSkuCode()))
            {
                result.add(sku.getSystemSkuCode());
            }
        }
        return result;
    }

    private List<ProductCenterSku> toProductCenterSkus(List<ProductSku> skus)
    {
        List<ProductCenterSku> result = new ArrayList<>();
        if (skus == null)
        {
            return result;
        }
        for (ProductSku sku : skus)
        {
            result.add(toProductCenterSku(sku));
        }
        return result;
    }

    private ProductCenterSku toProductCenterSku(ProductSku sku)
    {
        ProductCenterSku result = new ProductCenterSku();
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
        result.setAvailableStock(sku.getAvailableStock());
        result.setWarehouseCount(sku.getWarehouseCount());
        result.setInventoryStatus(sku.getInventoryStatus());
        result.setStockUpdateTime(sku.getStockUpdateTime());
        result.setSkuStatus(sku.getSkuStatus());
        result.setSortOrder(sku.getSortOrder());
        return result;
    }

    private List<String> galleryUrls(List<ProductImage> images)
    {
        List<String> result = new ArrayList<>();
        if (images == null)
        {
            return result;
        }
        images.stream()
            .filter(image -> IMAGE_ROLE_GALLERY.equals(image.getImageRole()) && hasText(image.getImageUrl()))
            .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .forEach(image -> result.add(image.getImageUrl()));
        return result;
    }

    private List<ProductCenterWarehouse> toProductCenterWarehouses(List<ProductSpuWarehouse> warehouses)
    {
        List<ProductCenterWarehouse> result = new ArrayList<>();
        if (warehouses == null)
        {
            return result;
        }
        for (ProductSpuWarehouse warehouse : warehouses)
        {
            ProductCenterWarehouse item = new ProductCenterWarehouse();
            item.setId(warehouse.getId());
            item.setWarehouseId(warehouse.getWarehouseId());
            item.setWarehouseCode(warehouse.getWarehouseCode());
            item.setWarehouseName(warehouse.getWarehouseName());
            item.setWarehouseKind(warehouse.getWarehouseKind());
            item.setWarehouseKindLabel(warehouseKindLabel(warehouse.getWarehouseKind()));
            item.setSettlementCurrency(warehouse.getSettlementCurrency());
            item.setDeliveryText(deliveryText(warehouse.getWarehouseKind()));
            result.add(item);
        }
        return result;
    }

    private String deliveryText(String warehouseKind)
    {
        if ("official".equals(warehouseKind))
        {
            return "平台官方仓发货";
        }
        if ("third_party".equals(warehouseKind))
        {
            return "仓库发货";
        }
        return "发货仓库待确认";
    }

    private String warehouseKindLabel(String warehouseKind)
    {
        if ("official".equals(warehouseKind))
        {
            return "官方仓";
        }
        if ("third_party".equals(warehouseKind))
        {
            return "三方仓";
        }
        if ("MIXED".equals(warehouseKind))
        {
            return "混合";
        }
        return warehouseKind;
    }

    private List<ProductCenterAttribute> toProductCenterAttributes(List<ProductAttributeValue> values)
    {
        List<ProductCenterAttribute> result = new ArrayList<>();
        if (values == null)
        {
            return result;
        }
        for (ProductAttributeValue value : values)
        {
            String displayValue = attributeDisplayValue(value);
            if (!hasText(value.getAttributeName()) || !hasText(displayValue))
            {
                continue;
            }
            ProductCenterAttribute item = new ProductCenterAttribute();
            item.setLabel(value.getAttributeName());
            item.setValue(displayValue);
            result.add(item);
        }
        return result;
    }

    private String attributeDisplayValue(ProductAttributeValue value)
    {
        if (hasText(value.getValueText()))
        {
            return value.getValueText();
        }
        if (hasText(value.getValueCode()))
        {
            return value.getValueCode();
        }
        BigDecimal number = value.getValueNumber();
        if (number != null)
        {
            return number.stripTrailingZeros().toPlainString();
        }
        if (value.getValueDate() != null)
        {
            return value.getValueDate().toString();
        }
        return hasText(value.getValueJson()) ? value.getValueJson() : null;
    }

    private boolean hasText(String value)
    {
        return value != null && !value.isBlank();
    }
}
