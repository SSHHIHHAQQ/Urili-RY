package com.ruoyi.product.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.finance.domain.FinanceCurrency;
import com.ruoyi.finance.service.IFinanceCurrencyService;
import com.ruoyi.product.domain.ProductAttributeValue;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.domain.ProductImage;
import com.ruoyi.product.domain.ProductSellerSnapshot;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.product.service.IProductConfigService;
import com.ruoyi.product.service.IProductDistributionService;
import com.ruoyi.product.service.ProductSellerLookupService;

/**
 * 商城商品 SPU/SKU 服务实现。
 */
@Service
public class ProductDistributionServiceImpl implements IProductDistributionService
{
    private static final String STATUS_NORMAL = "0";
    private static final String YES = "Y";
    private static final String OWNER_TYPE_SPU = "SPU";
    private static final String OWNER_TYPE_SKU = "SKU";
    private static final String IMAGE_MAIN = "MAIN";
    private static final String IMAGE_GALLERY = "GALLERY";
    private static final String IMAGE_SKU_MAIN = "SKU_MAIN";
    private static final String IMAGE_DETAIL = "DETAIL";
    private static final String SOURCE_ADMIN_MANUAL = "ADMIN_MANUAL";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_ON_SALE = "ON_SALE";
    private static final String STATUS_OFF_SALE = "OFF_SALE";
    private static final String STATUS_DISABLED = "DISABLED";

    private static final Set<String> PRODUCT_STATUSES = Set.of(STATUS_DRAFT, STATUS_READY, STATUS_ON_SALE,
        STATUS_OFF_SALE, STATUS_DISABLED);

    @Autowired
    private ProductDistributionMapper productDistributionMapper;

    @Autowired
    private IProductConfigService productConfigService;

    @Autowired
    private ProductSellerLookupService productSellerLookupService;

    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @Override
    public List<ProductSpu> selectProductList(ProductSpu query)
    {
        List<ProductSpu> products = productDistributionMapper.selectProductList(query);
        for (ProductSpu product : products)
        {
            product.setSkus(productDistributionMapper.selectSkuListBySpuId(product.getSpuId()));
        }
        return products;
    }

    @Override
    public ProductSpu selectProductById(Long spuId)
    {
        ProductSpu product = requireProduct(spuId);
        product.setSkus(productDistributionMapper.selectSkuListBySpuId(spuId));
        product.setAttributeValues(productDistributionMapper.selectAttributeValuesBySpuId(spuId));
        product.setImages(productDistributionMapper.selectImagesBySpuId(spuId));
        return product;
    }

    @Override
    @Transactional
    public int insertProduct(ProductSpu product)
    {
        normalizeSpuForSave(product, null);
        product.setSystemSpuCode(generateSpuCode());
        product.setCreateBy(currentUsername());
        int rows = productDistributionMapper.insertSpu(product);
        saveSkus(product, List.of());
        saveAttributeValues(product);
        saveImages(product);
        return rows;
    }

    @Override
    @Transactional
    public int updateProduct(ProductSpu product)
    {
        ProductSpu current = requireProduct(product.getSpuId());
        normalizeSpuForSave(product, current);
        product.setSystemSpuCode(current.getSystemSpuCode());
        product.setSourceType(current.getSourceType());
        product.setSourceRefType(current.getSourceRefType());
        product.setSourceRefId(current.getSourceRefId());
        product.setUpdateBy(currentUsername());
        int rows = productDistributionMapper.updateSpu(product);
        saveSkus(product, productDistributionMapper.selectSkuListBySpuId(product.getSpuId()));
        saveAttributeValues(product);
        saveImages(product);
        return rows;
    }

    @Override
    @Transactional
    public int updateSpuStatus(Long spuId, String status)
    {
        ProductSpu product = requireProduct(spuId);
        String targetStatus = normalizeStatus(status);
        validateSpuTransition(product.getSpuStatus(), targetStatus);
        if (STATUS_ON_SALE.equals(targetStatus))
        {
            validateSpuOnSale(product);
        }
        return productDistributionMapper.updateSpuStatus(spuId, targetStatus, currentUsername());
    }

    @Override
    @Transactional
    public int updateSkuStatus(Long spuId, Long skuId, String status)
    {
        ProductSpu product = requireProduct(spuId);
        ProductSku sku = productDistributionMapper.selectSkuById(skuId);
        if (sku == null || !spuId.equals(sku.getSpuId()))
        {
            throw new ServiceException("商品 SKU 不存在");
        }
        String targetStatus = normalizeStatus(status);
        validateSkuTransition(sku.getSkuStatus(), targetStatus);
        if (STATUS_ON_SALE.equals(targetStatus))
        {
            validateSkuOnSale(sku);
            if (!STATUS_ON_SALE.equals(product.getSpuStatus()) && !STATUS_READY.equals(product.getSpuStatus()))
            {
                throw new ServiceException("SPU 当前状态不允许上架 SKU");
            }
        }
        return productDistributionMapper.updateSkuStatus(skuId, targetStatus, currentUsername());
    }

    @Override
    public List<ProductSku> selectSkuList(Long spuId)
    {
        requireProduct(spuId);
        return productDistributionMapper.selectSkuListBySpuId(spuId);
    }

    private void normalizeSpuForSave(ProductSpu product, ProductSpu current)
    {
        product.setProductName(requireTrim(product.getProductName(), "商品名称不能为空"));
        product.setProductNameEn(requireTrim(product.getProductNameEn(), "商品英文标题不能为空"));
        product.setSellerSpuCode(trimToEmpty(product.getSellerSpuCode()));
        product.setSellingPoint(trimToEmpty(product.getSellingPoint()));
        product.setMainImageUrl(trimToEmpty(product.getMainImageUrl()));
        product.setDetailContent(StringUtils.defaultString(product.getDetailContent()));
        product.setRemark(StringUtils.defaultString(product.getRemark()));
        product.setSpuStatus(normalizeStatus(StringUtils.defaultIfBlank(product.getSpuStatus(), STATUS_DRAFT)));
        if (current != null)
        {
            validateSpuTransition(current.getSpuStatus(), product.getSpuStatus());
        }
        product.setSourceType(current == null ? SOURCE_ADMIN_MANUAL : current.getSourceType());
        product.setSourceRefType(current == null ? "" : trimToEmpty(current.getSourceRefType()));
        product.setSourceRefId(current == null ? "" : trimToEmpty(current.getSourceRefId()));
        fillSellerSnapshot(product);
        fillCategorySnapshot(product);
        if (StringUtils.isNotBlank(product.getSellerSpuCode())
            && productDistributionMapper.countSellerSpuCode(product.getSellerId(), product.getSellerSpuCode(),
                product.getSpuId()) > 0)
        {
            throw new ServiceException("同一卖家下客户SPU已存在");
        }
        if (STATUS_ON_SALE.equals(product.getSpuStatus()))
        {
            validateSpuBasics(product);
        }
    }

    private void fillSellerSnapshot(ProductSpu product)
    {
        if (product.getSellerId() == null)
        {
            throw new ServiceException("请选择卖家");
        }
        ProductSellerSnapshot seller = productSellerLookupService.selectSellerSnapshot(product.getSellerId());
        if (seller == null)
        {
            throw new ServiceException("卖家不存在");
        }
        product.setSellerNo(trimToEmpty(seller.getSellerNo()));
        product.setSellerName(requireTrim(seller.getSellerName(), "卖家名称不能为空"));
    }

    private void fillCategorySnapshot(ProductSpu product)
    {
        if (product.getCategoryId() == null)
        {
            throw new ServiceException("请选择商品分类");
        }
        ProductCategory category = productConfigService.selectCategoryById(product.getCategoryId());
        if (!STATUS_NORMAL.equals(category.getStatus()))
        {
            throw new ServiceException("商品分类已停用");
        }
        if (!YES.equals(category.getPublishEnabled()))
        {
            throw new ServiceException("商品只能选择末级可发布分类");
        }
        product.setCategoryCode(category.getCategoryCode());
        product.setCategoryName(category.getCategoryName());
    }

    private void saveSkus(ProductSpu product, List<ProductSku> currentSkus)
    {
        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty())
        {
            throw new ServiceException("至少需要维护一个 SKU");
        }
        Map<Long, ProductSku> currentSkuMap = currentSkus.stream()
            .filter(sku -> sku.getSkuId() != null)
            .collect(Collectors.toMap(ProductSku::getSkuId, Function.identity()));
        Set<Long> submittedSkuIds = new HashSet<>();
        int index = 0;
        for (ProductSku sku : skus)
        {
            normalizeSku(product, sku, currentSkuMap.get(sku.getSkuId()), index++);
            if (sku.getSkuId() == null)
            {
                sku.setSystemSkuCode(generateSkuCode());
                sku.setCreateBy(currentUsername());
                productDistributionMapper.insertSku(sku);
            }
            else
            {
                submittedSkuIds.add(sku.getSkuId());
                sku.setUpdateBy(currentUsername());
                productDistributionMapper.updateSku(sku);
            }
        }
        for (ProductSku currentSku : currentSkus)
        {
            if (!submittedSkuIds.contains(currentSku.getSkuId()))
            {
                productDistributionMapper.updateSkuStatus(currentSku.getSkuId(), STATUS_DISABLED, currentUsername());
            }
        }
    }

    private void normalizeSku(ProductSpu product, ProductSku sku, ProductSku currentSku, int index)
    {
        if (sku.getSkuId() != null && currentSku == null)
        {
            throw new ServiceException("商品 SKU 不存在或不属于当前 SPU");
        }
        sku.setSpuId(product.getSpuId());
        sku.setSellerId(product.getSellerId());
        sku.setSellerSkuCode(trimToEmpty(sku.getSellerSkuCode()));
        sku.setColor(trimToEmpty(sku.getColor()));
        sku.setSize(trimToEmpty(sku.getSize()));
        sku.setLengthValue(trimToEmpty(sku.getLengthValue()));
        sku.setWidthValue(trimToEmpty(sku.getWidthValue()));
        sku.setHeightValue(trimToEmpty(sku.getHeightValue()));
        sku.setWeight(trimToEmpty(sku.getWeight()));
        sku.setMaterial(trimToEmpty(sku.getMaterial()));
        sku.setStyle(trimToEmpty(sku.getStyle()));
        sku.setModel(trimToEmpty(sku.getModel()));
        sku.setPackageQuantity(trimToEmpty(sku.getPackageQuantity()));
        sku.setCapacity(trimToEmpty(sku.getCapacity()));
        sku.setSkuImageUrl(trimToEmpty(sku.getSkuImageUrl()));
        sku.setCurrencyCode(requireTrim(sku.getCurrencyCode(), "SKU 币种不能为空").toUpperCase());
        sku.setSkuStatus(normalizeSkuSaveStatus(product, sku, currentSku));
        sku.setSortOrder(sku.getSortOrder() == null ? index : sku.getSortOrder());
        sku.setRemark(StringUtils.defaultString(sku.getRemark()));
        validateMoney(sku.getSupplyPrice(), "SKU 供货价不能为空且不能小于 0");
        validateMoney(sku.getSalePrice(), "SKU 销售价不能为空且不能小于 0");
        validateCurrency(sku.getCurrencyCode());
        if (StringUtils.isNotBlank(sku.getSellerSkuCode())
            && productDistributionMapper.countSellerSkuCode(product.getSellerId(), sku.getSellerSkuCode(),
                sku.getSkuId()) > 0)
        {
            throw new ServiceException("同一卖家下客户SKU已存在：" + sku.getSellerSkuCode());
        }
        if (STATUS_ON_SALE.equals(sku.getSkuStatus()))
        {
            validateSkuOnSale(sku);
        }
    }

    private String normalizeSkuSaveStatus(ProductSpu product, ProductSku sku, ProductSku currentSku)
    {
        String requested = normalizeStatus(StringUtils.defaultIfBlank(sku.getSkuStatus(), STATUS_DRAFT));
        if (currentSku == null && STATUS_ON_SALE.equals(product.getSpuStatus()) && STATUS_ON_SALE.equals(requested))
        {
            return STATUS_DRAFT;
        }
        if (currentSku != null)
        {
            validateSkuTransition(currentSku.getSkuStatus(), requested);
        }
        return requested;
    }

    private void saveAttributeValues(ProductSpu product)
    {
        productDistributionMapper.deleteAttributeValuesBySpuId(product.getSpuId());
        List<ProductCategoryAttribute> schema = productConfigService.previewCategorySchema(product.getCategoryId());
        Map<Long, ProductCategoryAttribute> schemaMap = schema.stream()
            .collect(Collectors.toMap(ProductCategoryAttribute::getAttributeId, Function.identity(), (a, b) -> a));
        Map<Long, ProductAttributeValue> inputMap = new HashMap<>();
        if (product.getAttributeValues() != null)
        {
            for (ProductAttributeValue value : product.getAttributeValues())
            {
                if (value.getAttributeId() != null)
                {
                    inputMap.put(value.getAttributeId(), value);
                }
            }
        }
        for (ProductCategoryAttribute rule : schema)
        {
            ProductAttributeValue value = inputMap.get(rule.getAttributeId());
            if ("Y".equals(rule.getRequiredFlag()) && !hasAttributeValue(value))
            {
                throw new ServiceException("类目属性必填：" + rule.getAttributeName());
            }
        }
        for (ProductAttributeValue value : inputMap.values())
        {
            ProductCategoryAttribute rule = schemaMap.get(value.getAttributeId());
            if (rule == null)
            {
                throw new ServiceException("商品属性不属于当前类目模板：" + value.getAttributeId());
            }
            normalizeAttributeValue(product, value, rule);
            productDistributionMapper.insertAttributeValue(value);
        }
    }

    private void normalizeAttributeValue(ProductSpu product, ProductAttributeValue value, ProductCategoryAttribute rule)
    {
        value.setOwnerType(OWNER_TYPE_SPU);
        value.setOwnerId(product.getSpuId());
        value.setSpuId(product.getSpuId());
        value.setCategoryId(product.getCategoryId());
        value.setCategorySchemaVersion(productConfigService.selectCategoryById(product.getCategoryId()).getSchemaVersion());
        value.setAttributeId(rule.getAttributeId());
        value.setAttributeCode(rule.getAttributeCode());
        value.setAttributeName(rule.getAttributeName());
        value.setAttributeType(rule.getAttributeType());
        value.setValueCode(trimToEmpty(value.getValueCode()));
        value.setValueText(trimToNull(value.getValueText()));
        value.setValueJson(trimToNull(value.getValueJson()));
        value.setCreateBy(currentUsername());
        value.setUpdateBy(currentUsername());
    }

    private void saveImages(ProductSpu product)
    {
        productDistributionMapper.deleteImagesBySpuId(product.getSpuId());
        List<ProductImage> images = new ArrayList<>();
        if (StringUtils.isNotBlank(product.getMainImageUrl()))
        {
            ProductImage main = new ProductImage();
            main.setOwnerType(OWNER_TYPE_SPU);
            main.setOwnerId(product.getSpuId());
            main.setSpuId(product.getSpuId());
            main.setImageRole(IMAGE_MAIN);
            main.setImageUrl(product.getMainImageUrl());
            main.setSortOrder(0);
            images.add(main);
        }
        if (product.getImages() != null)
        {
            int galleryCount = 0;
            int detailCount = 0;
            for (ProductImage image : product.getImages())
            {
                if (IMAGE_GALLERY.equals(image.getImageRole()) && StringUtils.isNotBlank(image.getImageUrl()))
                {
                    if (++galleryCount > 9)
                    {
                        throw new ServiceException("SPU 轮播图最多上传 9 张");
                    }
                    image.setOwnerType(OWNER_TYPE_SPU);
                    image.setOwnerId(product.getSpuId());
                    image.setSpuId(product.getSpuId());
                    image.setSkuId(null);
                    images.add(image);
                }
                if (IMAGE_DETAIL.equals(image.getImageRole()) && StringUtils.isNotBlank(image.getImageUrl()))
                {
                    if (++detailCount > 20)
                    {
                        throw new ServiceException("商品详情图片最多上传 20 张");
                    }
                    image.setOwnerType(OWNER_TYPE_SPU);
                    image.setOwnerId(product.getSpuId());
                    image.setSpuId(product.getSpuId());
                    image.setSkuId(null);
                    images.add(image);
                }
            }
        }
        for (ProductSku sku : productDistributionMapper.selectSkuListBySpuId(product.getSpuId()))
        {
            if (StringUtils.isNotBlank(sku.getSkuImageUrl()))
            {
                ProductImage image = new ProductImage();
                image.setOwnerType(OWNER_TYPE_SKU);
                image.setOwnerId(sku.getSkuId());
                image.setSpuId(product.getSpuId());
                image.setSkuId(sku.getSkuId());
                image.setImageRole(IMAGE_SKU_MAIN);
                image.setImageUrl(sku.getSkuImageUrl());
                image.setSortOrder(sku.getSortOrder());
                images.add(image);
            }
        }
        for (ProductImage image : images)
        {
            image.setImageUrl(requireTrim(image.getImageUrl(), "商品图片不能为空"));
            image.setImageRole(requireTrim(image.getImageRole(), "商品图片角色不能为空"));
            image.setSortOrder(image.getSortOrder() == null ? 0 : image.getSortOrder());
            image.setCreateBy(currentUsername());
            productDistributionMapper.insertImage(image);
        }
    }

    private ProductSpu requireProduct(Long spuId)
    {
        ProductSpu product = productDistributionMapper.selectProductById(spuId);
        if (product == null)
        {
            throw new ServiceException("商城商品不存在");
        }
        return product;
    }

    private void validateSpuBasics(ProductSpu product)
    {
        if (product.getSellerId() == null)
        {
            throw new ServiceException("商品上架前必须绑定卖家");
        }
        if (product.getCategoryId() == null)
        {
            throw new ServiceException("商品上架前必须选择商品分类");
        }
        if (StringUtils.isBlank(product.getMainImageUrl()))
        {
            throw new ServiceException("商品上架前必须上传 SPU 主图");
        }
    }

    private void validateSpuOnSale(ProductSpu product)
    {
        ProductSpu detail = selectProductById(product.getSpuId());
        validateSpuBasics(detail);
        List<ProductSku> skus = detail.getSkus();
        if (skus == null || skus.isEmpty())
        {
            throw new ServiceException("商品上架前至少需要一个 SKU");
        }
        boolean hasValidSku = false;
        for (ProductSku sku : skus)
        {
            if (STATUS_READY.equals(sku.getSkuStatus()) || STATUS_ON_SALE.equals(sku.getSkuStatus()))
            {
                validateSkuOnSale(sku);
                hasValidSku = true;
            }
        }
        if (!hasValidSku)
        {
            throw new ServiceException("商品上架前至少需要一个可上架 SKU");
        }
    }

    private void validateSkuOnSale(ProductSku sku)
    {
        if (StringUtils.isBlank(sku.getSystemSkuCode()) && sku.getSkuId() != null)
        {
            ProductSku current = productDistributionMapper.selectSkuById(sku.getSkuId());
            sku.setSystemSkuCode(current == null ? sku.getSystemSkuCode() : current.getSystemSkuCode());
        }
        if (StringUtils.isBlank(sku.getSystemSkuCode()))
        {
            throw new ServiceException("SKU 上架前必须存在系统 SKU");
        }
        validateMoney(sku.getSupplyPrice(), "SKU 上架前必须维护供货价");
        validateMoney(sku.getSalePrice(), "SKU 上架前必须维护销售价");
        validateCurrency(sku.getCurrencyCode());
        if (STATUS_DISABLED.equals(sku.getSkuStatus()))
        {
            throw new ServiceException("停用 SKU 不允许上架");
        }
    }

    private void validateSpuTransition(String currentStatus, String targetStatus)
    {
        if (StringUtils.equals(currentStatus, targetStatus))
        {
            return;
        }
        if (STATUS_DISABLED.equals(currentStatus) && !STATUS_DRAFT.equals(targetStatus))
        {
            throw new ServiceException("停用商品只能先恢复为草稿");
        }
        if (STATUS_ON_SALE.equals(targetStatus) && !STATUS_READY.equals(currentStatus) && !STATUS_OFF_SALE.equals(currentStatus))
        {
            throw new ServiceException("商品只能从待上架或已下架切换为已上架");
        }
    }

    private void validateSkuTransition(String currentStatus, String targetStatus)
    {
        if (StringUtils.equals(currentStatus, targetStatus))
        {
            return;
        }
        if (STATUS_DISABLED.equals(currentStatus) && !STATUS_DRAFT.equals(targetStatus))
        {
            throw new ServiceException("停用 SKU 只能先恢复为草稿");
        }
        if (STATUS_ON_SALE.equals(targetStatus) && !STATUS_READY.equals(currentStatus) && !STATUS_OFF_SALE.equals(currentStatus))
        {
            throw new ServiceException("SKU 只能从待上架或已下架切换为已上架");
        }
    }

    private String generateSpuCode()
    {
        return generateCode("SPU", productDistributionMapper::countSystemSpuCode);
    }

    private String generateSkuCode()
    {
        return generateCode("SKU", productDistributionMapper::countSystemSkuCode);
    }

    private String generateCode(String prefix, Function<String, Integer> existsCounter)
    {
        String dayPrefix = prefix + DateUtils.dateTimeNow("yyyyMMdd");
        for (int i = 1; i <= 9999; i++)
        {
            String code = dayPrefix + String.format("%04d", i);
            if (existsCounter.apply(code) == 0)
            {
                return code;
            }
        }
        throw new ServiceException(prefix + "编码当天序号已用尽");
    }

    private String normalizeStatus(String status)
    {
        String value = requireTrim(status, "商品状态不能为空").toUpperCase();
        if (!PRODUCT_STATUSES.contains(value))
        {
            throw new ServiceException("商品状态不正确：" + value);
        }
        return value;
    }

    private void validateMoney(BigDecimal value, String message)
    {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException(message);
        }
    }

    private void validateCurrency(String currencyCode)
    {
        FinanceCurrency currency = financeCurrencyService.selectCurrencyByCode(currencyCode);
        if (currency == null)
        {
            throw new ServiceException("币种不存在：" + currencyCode);
        }
        if (!STATUS_NORMAL.equals(currency.getStatus()))
        {
            throw new ServiceException("币种未启用：" + currencyCode);
        }
    }

    private boolean hasAttributeValue(ProductAttributeValue value)
    {
        if (value == null)
        {
            return false;
        }
        return StringUtils.isNotBlank(value.getValueCode())
            || StringUtils.isNotBlank(value.getValueText())
            || value.getValueNumber() != null
            || value.getValueDate() != null
            || StringUtils.isNotBlank(value.getValueJson());
    }

    private String requireTrim(String value, String message)
    {
        String result = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(result))
        {
            throw new ServiceException(message);
        }
        return result;
    }

    private String trimToEmpty(String value)
    {
        return StringUtils.trimToEmpty(value);
    }

    private String trimToNull(String value)
    {
        return StringUtils.trimToNull(value);
    }

    private String currentUsername()
    {
        return SecurityUtils.getUsername();
    }
}
