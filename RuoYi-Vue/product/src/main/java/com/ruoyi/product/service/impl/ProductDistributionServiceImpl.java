package com.ruoyi.product.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.finance.domain.FinanceCurrency;
import com.ruoyi.finance.service.IFinanceCurrencyService;
import com.ruoyi.integration.domain.SourceOfficialWarehouseOption;
import com.ruoyi.integration.domain.SourceProductBindingSnapshot;
import com.ruoyi.integration.domain.SourceSkuPairingProjection;
import com.ruoyi.integration.service.ISourceReadModelRefreshService;
import com.ruoyi.integration.service.ISourceSkuPairingProjectionService;
import com.ruoyi.inventory.service.IInventoryOverviewService;
import com.ruoyi.product.domain.ProductAttributeValue;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;
import com.ruoyi.product.domain.ProductDistributionOperationLog;
import com.ruoyi.product.domain.ProductImage;
import com.ruoyi.product.domain.ProductReviewRequest;
import com.ruoyi.product.domain.ProductSellerSnapshot;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSourceBinding;
import com.ruoyi.product.domain.ProductSkuSalePriceUpdateRequest;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductSpuWarehouse;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.product.mapper.ProductDistributionOperationLogMapper;
import com.ruoyi.product.mapper.ProductReviewMapper;
import com.ruoyi.product.service.IProductConfigService;
import com.ruoyi.product.service.IProductDistributionService;
import com.ruoyi.product.service.ProductSellerLookupService;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.service.IWarehouseService;

/**
 * 商城商品 SPU/SKU 服务实现。
 */
@Service
public class ProductDistributionServiceImpl implements IProductDistributionService
{
    private static final Logger log = LoggerFactory.getLogger(ProductDistributionServiceImpl.class);

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
    private static final String CONTROL_NORMAL = "NORMAL";
    private static final String CONTROL_DISABLED = "DISABLED";
    private static final String WAREHOUSE_OFFICIAL = "official";
    private static final String WAREHOUSE_THIRD_PARTY = "third_party";
    private static final String SOURCE_SCOPE_OFFICIAL_MASTER = "OFFICIAL_MASTER";
    private static final String SOURCE_REF_TYPE_SOURCE_SKU_GROUP = "SOURCE_SKU_GROUP";
    private static final String BINDING_ACTIVE = "ACTIVE";
    private static final String BINDING_LOCKED = "LOCKED";
    private static final String BINDING_UNLOCKED = "UNLOCKED";
    private static final String OP_SALES_STATUS_CHANGE = "SALES_STATUS_CHANGE";
    private static final String OP_CONTROL_DISABLE = "CONTROL_DISABLE";
    private static final String OP_CONTROL_RECOVER = "CONTROL_RECOVER";
    private static final String OP_SALE_PRICE_ADJUST = "SALE_PRICE_ADJUST";
    private static final String OP_SOURCE_BIND = "SOURCE_BIND";
    private static final String OP_SOURCE_LOCK = "SOURCE_LOCK";
    private static final String OP_SOURCE_REBIND = "SOURCE_REBIND";
    private static final String OP_SOURCE_RELEASE = "SOURCE_RELEASE";
    private static final String OP_SOURCE_PAGE = "PAGE";

    private static final Set<String> PRODUCT_STATUSES = Set.of(STATUS_DRAFT, STATUS_READY, STATUS_ON_SALE,
        STATUS_OFF_SALE);
    private static final Set<String> CONTROL_STATUSES = Set.of(CONTROL_NORMAL, CONTROL_DISABLED);

    @Autowired
    private ProductDistributionMapper productDistributionMapper;

    @Autowired
    private ProductDistributionOperationLogMapper operationLogMapper;

    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Autowired
    private IProductConfigService productConfigService;

    @Autowired
    private ObjectProvider<ProductSellerLookupService> productSellerLookupService;

    @Autowired
    private IFinanceCurrencyService financeCurrencyService;

    @Autowired
    private IWarehouseService warehouseService;

    @Autowired
    private ObjectProvider<ISourceReadModelRefreshService> sourceReadModelRefreshService;

    @Autowired
    private ObjectProvider<ISourceSkuPairingProjectionService> sourceSkuPairingProjectionService;

    @Autowired
    private ObjectProvider<IInventoryOverviewService> inventoryOverviewService;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private TaskExecutor readModelRefreshExecutor;

    @Override
    public List<ProductSpu> selectProductList(ProductSpu query)
    {
        List<ProductSpu> products = productDistributionMapper.selectProductList(query);
        fillLatestReviewSummaries(products);
        for (ProductSpu product : products)
        {
            product.setSkus(productDistributionMapper.selectSkuListBySpuId(product.getSpuId()));
        }
        return products;
    }

    @Override
    public List<ProductSpu> selectOnSaleProductList(ProductSpu query)
    {
        List<ProductSpu> products = productDistributionMapper.selectOnSaleProductList(query);
        for (ProductSpu product : products)
        {
            product.setSkus(productDistributionMapper.selectOnSaleSkuListBySpuId(product.getSpuId()));
        }
        return products;
    }

    @Override
    public ProductSpu selectProductById(Long spuId)
    {
        ProductSpu product = requireProduct(spuId);
        fillLatestReviewSummary(product);
        product.setSkus(productDistributionMapper.selectSkuListBySpuId(spuId));
        fillWarehouseBindings(product);
        product.setAttributeValues(productDistributionMapper.selectAttributeValuesBySpuId(spuId));
        product.setImages(productDistributionMapper.selectImagesBySpuId(spuId));
        return product;
    }

    @Override
    public ProductSpu selectProductById(Long spuId, Long sellerId)
    {
        if (sellerId == null)
        {
            throw new ServiceException("卖家主体不能为空");
        }
        ProductSpu product = requireProduct(spuId, sellerId);
        fillLatestReviewSummary(product);
        product.setSkus(productDistributionMapper.selectSkuListBySpuIdAndSellerId(spuId, sellerId));
        fillWarehouseBindings(product);
        product.setAttributeValues(productDistributionMapper.selectAttributeValuesBySpuId(spuId));
        product.setImages(productDistributionMapper.selectImagesBySpuId(spuId));
        return product;
    }

    private void fillLatestReviewSummaries(List<ProductSpu> products)
    {
        if (products == null || products.isEmpty())
        {
            return;
        }
        List<Long> spuIds = products.stream().map(ProductSpu::getSpuId).filter(Objects::nonNull).distinct()
            .collect(Collectors.toList());
        if (spuIds.isEmpty())
        {
            return;
        }
        Map<Long, ProductReviewRequest> reviewMap = productReviewMapper.selectLatestReviewsBySpuIds(spuIds).stream()
            .collect(Collectors.toMap(ProductReviewRequest::getSpuId, Function.identity(), (a, b) -> a));
        for (ProductSpu product : products)
        {
            applyLatestReviewSummary(product, reviewMap.get(product.getSpuId()));
        }
    }

    private void fillLatestReviewSummary(ProductSpu product)
    {
        if (product == null || product.getSpuId() == null)
        {
            return;
        }
        List<ProductReviewRequest> reviews = productReviewMapper.selectLatestReviewsBySpuIds(List.of(product.getSpuId()));
        applyLatestReviewSummary(product, reviews.isEmpty() ? null : reviews.get(0));
    }

    private void applyLatestReviewSummary(ProductSpu product, ProductReviewRequest review)
    {
        if (product == null || review == null)
        {
            return;
        }
        product.setLatestReviewId(review.getReviewId());
        product.setLatestReviewNo(review.getReviewNo());
        product.setLatestReviewStatus(review.getReviewStatus());
        product.setLatestReviewFeedback(StringUtils.defaultString(review.getReviewReason()));
        product.setLatestReviewTime(review.getReviewTime());
    }

    @Override
    public ProductSpu selectOnSaleProductById(Long spuId)
    {
        ProductSpu product = requireOnSaleProduct(spuId);
        product.setSkus(productDistributionMapper.selectOnSaleSkuListBySpuId(spuId));
        fillWarehouseBindings(product);
        product.setAttributeValues(productDistributionMapper.selectAttributeValuesBySpuId(spuId));
        product.setImages(productDistributionMapper.selectImagesBySpuId(spuId));
        return product;
    }

    @Override
    @Transactional
    public int insertProduct(ProductSpu product)
    {
        OfficialSourceSaveContext sourceContext = buildOfficialSourceSaveContext(product);
        normalizeSpuForSave(product, null, sourceContext);
        product.setSystemSpuCode(generateSpuCode());
        String operator = currentUsername();
        product.setCreateBy(operator);
        product.setUpdateBy(operator);
        long saveStartedAt = System.currentTimeMillis();
        int rows = productDistributionMapper.insertSpu(product);
        long spuSavedAt = System.currentTimeMillis();
        saveWarehouses(product);
        long warehousesSavedAt = System.currentTimeMillis();
        saveSkus(product, List.of(), sourceContext);
        long skusSavedAt = System.currentTimeMillis();
        saveAttributeValues(product);
        long attributesSavedAt = System.currentTimeMillis();
        saveImages(product);
        long imagesSavedAt = System.currentTimeMillis();
        refreshInventoryOverviewAfterCommit(product.getSpuId());
        logCoreSaveCost("insert", product.getSpuId(), saveStartedAt, spuSavedAt, warehousesSavedAt, skusSavedAt,
            attributesSavedAt, imagesSavedAt);
        return rows;
    }

    @Override
    @Transactional
    public int updateProduct(ProductSpu product)
    {
        ProductSpu current = requireProduct(product.getSpuId());
        ensureNoPendingProductReview(current.getSpuId());
        OfficialSourceSaveContext sourceContext = buildOfficialSourceSaveContext(product);
        normalizeSpuForSave(product, current, sourceContext);
        product.setSystemSpuCode(current.getSystemSpuCode());
        product.setSourceType(current.getSourceType());
        product.setSourceRefType(current.getSourceRefType());
        product.setSourceRefId(current.getSourceRefId());
        product.setUpdateBy(currentUsername());
        long saveStartedAt = System.currentTimeMillis();
        int rows = productDistributionMapper.updateSpu(product);
        long spuSavedAt = System.currentTimeMillis();
        saveWarehouses(product);
        long warehousesSavedAt = System.currentTimeMillis();
        saveSkus(product, productDistributionMapper.selectSkuListBySpuId(product.getSpuId()), sourceContext);
        long skusSavedAt = System.currentTimeMillis();
        saveAttributeValues(product);
        long attributesSavedAt = System.currentTimeMillis();
        saveImages(product);
        long imagesSavedAt = System.currentTimeMillis();
        refreshInventoryOverviewAfterCommit(product.getSpuId());
        logCoreSaveCost("update", product.getSpuId(), saveStartedAt, spuSavedAt, warehousesSavedAt, skusSavedAt,
            attributesSavedAt, imagesSavedAt);
        return rows;
    }

    @Override
    public ProductSpu prepareReviewedProductUpdate(ProductSpu product)
    {
        ProductSpu current = requireProduct(product.getSpuId());
        if (STATUS_DRAFT.equals(current.getSpuStatus()))
        {
            throw new ServiceException("草稿商品请直接保存，不需要提交编辑审核");
        }
        ensureControlNormal(current.getControlStatus(), "停用商品不能提交编辑审核");
        normalizeReviewedProductForSave(product, current, buildOfficialSourceSaveContext(product));
        return product;
    }

    @Override
    @Transactional
    public int applyReviewedProductUpdate(ProductSpu product)
    {
        ProductSpu current = requireProduct(product.getSpuId());
        if (STATUS_DRAFT.equals(current.getSpuStatus()))
        {
            throw new ServiceException("草稿商品不能按编辑审核生效");
        }
        OfficialSourceSaveContext sourceContext = buildOfficialSourceSaveContext(product);
        normalizeReviewedProductForSave(product, current, sourceContext);
        product.setUpdateBy(currentUsername());
        long saveStartedAt = System.currentTimeMillis();
        int rows = productDistributionMapper.updateSpu(product);
        long spuSavedAt = System.currentTimeMillis();
        saveWarehouses(product);
        long warehousesSavedAt = System.currentTimeMillis();
        saveSkus(product, productDistributionMapper.selectSkuListBySpuId(product.getSpuId()), sourceContext);
        long skusSavedAt = System.currentTimeMillis();
        saveAttributeValues(product);
        long attributesSavedAt = System.currentTimeMillis();
        saveImages(product);
        long imagesSavedAt = System.currentTimeMillis();
        refreshInventoryOverviewAfterCommit(product.getSpuId());
        logCoreSaveCost("review-apply", product.getSpuId(), saveStartedAt, spuSavedAt, warehousesSavedAt,
            skusSavedAt, attributesSavedAt, imagesSavedAt);
        return rows;
    }

    @Override
    @Transactional
    public int updateSpuStatus(Long spuId, String status, String reason)
    {
        String batchNo = generateBatchNo();
        return updateSpuStatusInternal(spuId, status, batchNo, true, reason);
    }

    private int updateSpuStatusInternal(Long spuId, String status, String batchNo, boolean syncSkuStatus, String reason)
    {
        ProductSpu product = requireProduct(spuId);
        ensureNoPendingProductReview(product.getSpuId());
        String targetStatus = normalizeStatus(status);
        String normalizedReason = normalizeSalesStatusReason(targetStatus, reason);
        validateSpuTransition(product.getSpuStatus(), targetStatus);
        ensureControlNormal(product.getControlStatus(), "停用商品不能调整销售状态");
        if (STATUS_ON_SALE.equals(targetStatus))
        {
            validateSpuOnSale(product);
        }
        int rows = productDistributionMapper.updateSpuStatus(spuId, targetStatus, currentUsername());
        recordSpuStatusLog(batchNo, product, targetStatus, normalizedReason);
        if (syncSkuStatus)
        {
            rows += syncSkuStatusBySpuFlow(product, targetStatus, batchNo, normalizedReason);
        }
        return rows;
    }

    private int syncSkuStatusBySpuFlow(ProductSpu product, String targetStatus, String batchNo, String reason)
    {
        int rows = 0;
        List<ProductSku> skus = productDistributionMapper.selectSkuListBySpuId(product.getSpuId());
        for (ProductSku sku : skus)
        {
            if (!CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(sku.getControlStatus(), CONTROL_NORMAL)))
            {
                continue;
            }
            String skuTargetStatus = null;
            if (STATUS_READY.equals(targetStatus) && STATUS_DRAFT.equals(sku.getSkuStatus()))
            {
                skuTargetStatus = STATUS_READY;
            }
            else if (STATUS_ON_SALE.equals(targetStatus)
                && (STATUS_READY.equals(sku.getSkuStatus()) || STATUS_OFF_SALE.equals(sku.getSkuStatus())))
            {
                validateSkuOnSale(sku);
                skuTargetStatus = STATUS_ON_SALE;
            }
            else if (STATUS_OFF_SALE.equals(targetStatus) && STATUS_ON_SALE.equals(sku.getSkuStatus()))
            {
                skuTargetStatus = STATUS_OFF_SALE;
            }
            if (skuTargetStatus == null)
            {
                continue;
            }
            validateSkuTransition(sku.getSkuStatus(), skuTargetStatus);
            rows += productDistributionMapper.updateSkuStatus(sku.getSkuId(), skuTargetStatus, currentUsername());
            recordSkuStatusLog(batchNo, product, sku, skuTargetStatus, reason);
            lockSkuSourceBindingIfNeeded(product, sku, skuTargetStatus, batchNo);
        }
        return rows;
    }

    @Override
    @Transactional
    public int updateSkuStatus(Long spuId, Long skuId, String status, String reason)
    {
        String batchNo = generateBatchNo();
        return updateSkuStatusInternal(spuId, skuId, status, batchNo, reason);
    }

    private int updateSkuStatusInternal(Long spuId, Long skuId, String status, String batchNo, String reason)
    {
        ProductSpu product = requireProduct(spuId);
        ensureNoPendingProductReview(product.getSpuId());
        ProductSku sku = productDistributionMapper.selectSkuById(skuId);
        if (sku == null || !spuId.equals(sku.getSpuId()))
        {
            throw new ServiceException("商品 SKU 不存在");
        }
        String targetStatus = normalizeStatus(status);
        String normalizedReason = normalizeSalesStatusReason(targetStatus, reason);
        validateSkuTransition(sku.getSkuStatus(), targetStatus);
        ensureControlNormal(product.getControlStatus(), "SPU 已停用，不能调整 SKU 销售状态");
        ensureControlNormal(sku.getControlStatus(), "停用 SKU 不能调整销售状态");
        if (STATUS_ON_SALE.equals(targetStatus))
        {
            validateSkuOnSale(sku);
            if (!STATUS_ON_SALE.equals(product.getSpuStatus()) && !STATUS_READY.equals(product.getSpuStatus()))
            {
                throw new ServiceException("SPU 当前状态不允许上架 SKU");
            }
        }
        int rows = productDistributionMapper.updateSkuStatus(skuId, targetStatus, currentUsername());
        recordSkuStatusLog(batchNo, product, sku, targetStatus, normalizedReason);
        lockSkuSourceBindingIfNeeded(product, sku, targetStatus, batchNo);
        return rows;
    }

    @Override
    @Transactional
    public int batchUpdateSpuStatus(List<Long> spuIds, String status, boolean syncSkuStatus, String reason)
    {
        List<Long> ids = requireIds(spuIds, "请选择商品");
        String batchNo = generateBatchNo();
        int rows = 0;
        for (Long spuId : ids)
        {
            rows += updateSpuStatusInternal(spuId, status, batchNo, syncSkuStatus, reason);
        }
        return rows;
    }

    @Override
    @Transactional
    public int batchUpdateSkuStatus(List<Long> skuIds, String status, String reason)
    {
        List<Long> ids = requireIds(skuIds, "请选择 SKU");
        String batchNo = generateBatchNo();
        int rows = 0;
        for (Long skuId : ids)
        {
            ProductSku sku = productDistributionMapper.selectSkuById(skuId);
            if (sku == null)
            {
                throw new ServiceException("商品 SKU 不存在：" + skuId);
            }
            rows += updateSkuStatusInternal(sku.getSpuId(), skuId, status, batchNo, reason);
        }
        return rows;
    }

    @Override
    @Transactional
    public int batchUpdateSpuControlStatus(List<Long> spuIds, String controlStatus, String reason)
    {
        List<Long> ids = requireIds(spuIds, "请选择商品");
        String targetStatus = normalizeControlStatus(controlStatus);
        String normalizedReason = normalizeControlReason(targetStatus, reason);
        String batchNo = generateBatchNo();
        int rows = 0;
        for (Long spuId : ids)
        {
            ProductSpu product = requireProduct(spuId);
            validateControlTransition(product.getControlStatus(), targetStatus, "商品");
            rows += productDistributionMapper.updateSpuControlStatus(spuId, targetStatus, normalizedReason,
                currentUsername());
            recordSpuControlLog(batchNo, product, targetStatus, normalizedReason);
        }
        return rows;
    }

    @Override
    @Transactional
    public int batchUpdateSkuControlStatus(List<Long> skuIds, String controlStatus, String reason)
    {
        List<Long> ids = requireIds(skuIds, "请选择 SKU");
        String targetStatus = normalizeControlStatus(controlStatus);
        String normalizedReason = normalizeControlReason(targetStatus, reason);
        String batchNo = generateBatchNo();
        int rows = 0;
        for (Long skuId : ids)
        {
            ProductSku sku = productDistributionMapper.selectSkuById(skuId);
            if (sku == null)
            {
                throw new ServiceException("商品 SKU 不存在：" + skuId);
            }
            ProductSpu product = requireProduct(sku.getSpuId());
            validateControlTransition(sku.getControlStatus(), targetStatus, "SKU");
            rows += productDistributionMapper.updateSkuControlStatus(skuId, targetStatus, normalizedReason,
                currentUsername());
            recordSkuControlLog(batchNo, product, sku, targetStatus, normalizedReason);
        }
        return rows;
    }

    @Override
    @Transactional
    public int batchUpdateSkuSalePrice(ProductSkuSalePriceUpdateRequest request)
    {
        if (request == null || request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("请选择需要调价的 SKU");
        }
        String batchNo = generateBatchNo();
        int rows = 0;
        for (ProductSkuSalePriceUpdateRequest.Item item : request.getItems())
        {
            if (item == null || item.getSkuId() == null)
            {
                throw new ServiceException("SKU 调价参数不完整");
            }
            ProductSku sku = productDistributionMapper.selectSkuById(item.getSkuId());
            if (sku == null)
            {
                throw new ServiceException("商品 SKU 不存在：" + item.getSkuId());
            }
            BigDecimal salePrice = item.getSalePrice();
            validateMoney(salePrice, "SKU 销售价不能为空且不能小于 0");
            ProductSpu product = requireProduct(sku.getSpuId());
            ensureNoPendingProductReview(product.getSpuId());
            ensureControlNormal(product.getControlStatus(), "SPU 已停用，不能调整 SKU 销售价");
            ensureControlNormal(sku.getControlStatus(), "停用 SKU 不能调整销售价");
            rows += productDistributionMapper.updateSkuSalePrice(item.getSkuId(), salePrice, currentUsername());
            recordSkuPriceLog(batchNo, product, sku, salePrice, trimToEmpty(request.getReason()));
        }
        return rows;
    }

    @Override
    public List<ProductDistributionOperationLog> selectOperationLogList(ProductDistributionOperationLog query)
    {
        return operationLogMapper.selectOperationLogList(query);
    }

    @Override
    public List<ProductSku> selectSkuList(Long spuId)
    {
        requireProduct(spuId);
        return productDistributionMapper.selectSkuListBySpuId(spuId);
    }

    @Override
    public List<ProductSku> selectSkuList(Long spuId, Long sellerId)
    {
        if (sellerId == null)
        {
            throw new ServiceException("卖家主体不能为空");
        }
        requireProduct(spuId, sellerId);
        return productDistributionMapper.selectSkuListBySpuIdAndSellerId(spuId, sellerId);
    }

    @Override
    public List<ProductSku> selectSkuPageList(ProductSku query)
    {
        return productDistributionMapper.selectSkuPageList(query);
    }

    @Override
    public List<ProductSku> selectOnSaleSkuList(Long spuId)
    {
        requireOnSaleProduct(spuId);
        return productDistributionMapper.selectOnSaleSkuListBySpuId(spuId);
    }

    private void normalizeSpuForSave(ProductSpu product, ProductSpu current, OfficialSourceSaveContext sourceContext)
    {
        product.setProductName(requireTrim(product.getProductName(), "商品名称不能为空"));
        product.setProductNameEn(requireTrim(product.getProductNameEn(), "商品英文标题不能为空"));
        product.setSellerSpuCode(trimToEmpty(product.getSellerSpuCode()));
        product.setSellingPoint(trimToEmpty(product.getSellingPoint()));
        product.setMainImageUrl(trimToEmpty(product.getMainImageUrl()));
        product.setDetailContent(StringUtils.defaultString(product.getDetailContent()));
        product.setRemark(StringUtils.defaultString(product.getRemark()));
        product.setSpuStatus(normalizeStatus(StringUtils.defaultIfBlank(product.getSpuStatus(), STATUS_DRAFT)));
        if (current == null && !STATUS_DRAFT.equals(product.getSpuStatus()))
        {
            throw new ServiceException("新增商品只能保存为草稿，请提交商品审核后进入待上架");
        }
        product.setControlStatus(current == null ? CONTROL_NORMAL
            : normalizeControlStatus(StringUtils.defaultIfBlank(current.getControlStatus(), CONTROL_NORMAL)));
        product.setControlReason(current == null ? "" : trimToEmpty(current.getControlReason()));
        if (current != null)
        {
            validateSpuTransition(current.getSpuStatus(), product.getSpuStatus());
            if (!StringUtils.equals(current.getControlStatus(), product.getControlStatus()))
            {
                throw new ServiceException("商品管控状态请在列表页停用或恢复");
            }
        }
        product.setSourceType(current == null ? SOURCE_ADMIN_MANUAL : current.getSourceType());
        product.setSourceRefType(current == null ? "" : trimToEmpty(current.getSourceRefType()));
        product.setSourceRefId(current == null ? "" : trimToEmpty(current.getSourceRefId()));
        fillSellerSnapshot(product);
        fillCategorySnapshot(product);
        normalizeWarehouseBindings(product, current, sourceContext);
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

    private void normalizeReviewedProductForSave(ProductSpu product, ProductSpu current,
        OfficialSourceSaveContext sourceContext)
    {
        product.setSpuId(current.getSpuId());
        product.setSpuStatus(current.getSpuStatus());
        normalizeSpuForSave(product, current, sourceContext);
        product.setSystemSpuCode(current.getSystemSpuCode());
        product.setSourceType(current.getSourceType());
        product.setSourceRefType(current.getSourceRefType());
        product.setSourceRefId(current.getSourceRefId());
        List<ProductSku> currentSkus = productDistributionMapper.selectSkuListBySpuId(current.getSpuId());
        ensureReviewedSkuDeletionNotSubmitted(product, currentSkus);
        normalizeSubmittedSkus(product, currentSkus, sourceContext);
    }

    private void ensureReviewedSkuDeletionNotSubmitted(ProductSpu product, List<ProductSku> currentSkus)
    {
        Set<Long> submittedSkuIds = new HashSet<>();
        if (product.getSkus() != null)
        {
            for (ProductSku sku : product.getSkus())
            {
                if (sku != null && sku.getSkuId() != null)
                {
                    submittedSkuIds.add(sku.getSkuId());
                }
            }
        }
        for (ProductSku currentSku : currentSkus)
        {
            if (currentSku.getSkuId() != null && !submittedSkuIds.contains(currentSku.getSkuId()))
            {
                throw new ServiceException("已审核商品暂不支持删除 SKU，请先保留原 SKU 再提交审核");
            }
        }
    }

    private void normalizeSubmittedSkus(ProductSpu product, List<ProductSku> currentSkus,
        OfficialSourceSaveContext sourceContext)
    {
        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty())
        {
            throw new ServiceException("至少需要维护一个 SKU");
        }
        Map<Long, ProductSku> currentSkuMap = currentSkus.stream()
            .filter(sku -> sku.getSkuId() != null)
            .collect(Collectors.toMap(ProductSku::getSkuId, Function.identity()));
        int index = 0;
        for (ProductSku sku : skus)
        {
            normalizeSku(product, sku, currentSkuMap.get(sku.getSkuId()), index++, sourceContext);
        }
    }

    private void fillSellerSnapshot(ProductSpu product)
    {
        if (product.getSellerId() == null)
        {
            throw new ServiceException("请选择卖家");
        }
        ProductSellerLookupService lookupService = productSellerLookupService.getIfAvailable();
        if (lookupService == null)
        {
            throw new ServiceException("Product seller lookup service is not enabled");
        }
        ProductSellerSnapshot seller = lookupService.selectSellerSnapshot(product.getSellerId());
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

    private void normalizeWarehouseBindings(ProductSpu product, ProductSpu current,
        OfficialSourceSaveContext sourceContext)
    {
        String requestedKind = trimToEmpty(product.getWarehouseKind());
        if (WAREHOUSE_OFFICIAL.equals(requestedKind))
        {
            normalizeOfficialWarehouseBindings(product, current, sourceContext);
            return;
        }
        normalizeManualWarehouseBindings(product, current);
    }

    private void normalizeManualWarehouseBindings(ProductSpu product, ProductSpu current)
    {
        List<Long> warehouseIds = requireWarehouseIds(product);
        List<ProductSpuWarehouse> warehouses = new ArrayList<>();
        String selectedCurrency = null;
        String selectedKind = null;
        for (Long warehouseId : warehouseIds)
        {
            Warehouse warehouse = warehouseService.selectWarehouseById(warehouseId);
            if (!STATUS_NORMAL.equals(warehouse.getStatus()))
            {
                throw new ServiceException("发货仓库不是正常状态：" + warehouseLabel(warehouse));
            }
            String currency = requireTrim(warehouse.getSettlementCurrency(),
                "发货仓库未维护币种：" + warehouseLabel(warehouse)).toUpperCase();
            validateCurrency(currency);
            String kind = requireTrim(warehouse.getWarehouseKind(),
                "发货仓库未维护类型：" + warehouseLabel(warehouse));
            if (!WAREHOUSE_OFFICIAL.equals(kind) && !WAREHOUSE_THIRD_PARTY.equals(kind))
            {
                throw new ServiceException("发货仓库类型不支持：" + warehouseLabel(warehouse));
            }
            if (selectedCurrency != null && !selectedCurrency.equals(currency))
            {
                throw new ServiceException("发货仓库必须选择相同币种");
            }
            if (selectedKind != null && !selectedKind.equals(kind))
            {
                throw new ServiceException("官方仓和三方仓不能混在一起选择");
            }
            if (WAREHOUSE_THIRD_PARTY.equals(kind) && !product.getSellerId().equals(warehouse.getSellerId()))
            {
                throw new ServiceException("第三方发货仓库不属于当前卖家：" + warehouseLabel(warehouse));
            }
            selectedCurrency = currency;
            selectedKind = kind;

            ProductSpuWarehouse binding = new ProductSpuWarehouse();
            binding.setWarehouseId(warehouse.getWarehouseId());
            binding.setWarehouseCode(requireTrim(warehouse.getWarehouseCode(), "发货仓库编码不能为空"));
            binding.setWarehouseName(requireTrim(warehouse.getWarehouseName(), "发货仓库名称不能为空"));
            binding.setWarehouseKind(kind);
            binding.setSettlementCurrency(currency);
            binding.setSellerId(product.getSellerId());
            warehouses.add(binding);
        }
        validateWarehouseKindChange(current, selectedKind);
        product.setWarehouses(warehouses);
        product.setWarehouseIds(warehouses.stream().map(ProductSpuWarehouse::getWarehouseId).collect(Collectors.toList()));
    }

    private void normalizeOfficialWarehouseBindings(ProductSpu product, ProductSpu current,
        OfficialSourceSaveContext sourceContext)
    {
        Map<Long, ProductSpuWarehouse> warehouseMap = new HashMap<>();
        String selectedCurrency = null;
        if (product.getSkus() == null || product.getSkus().isEmpty())
        {
            throw new ServiceException("官方仓商品至少需要维护一个来源 SKU");
        }
        Set<String> dimensionKeys = new LinkedHashSet<>();
        for (ProductSku sku : product.getSkus())
        {
            String sourceDimensionGroupKey = requireTrim(sku.getSourceDimensionGroupKey(),
                "官方仓 SKU 必须从来源商品库选择来源 SKU");
            dimensionKeys.add(sourceDimensionGroupKey);
            ProductSkuSourceBinding snapshot = requireSourceSnapshot(sourceDimensionGroupKey, sourceContext);
            List<ProductSpuWarehouse> warehouses =
                selectOfficialWarehousesBySourceDimensionGroup(sourceDimensionGroupKey, sourceContext);
            if (warehouses == null || warehouses.isEmpty())
            {
                throw new ServiceException("来源 SKU 未配对到正常官方履约仓：" + snapshot.getMasterSku());
            }
            for (ProductSpuWarehouse warehouse : warehouses)
            {
                String currency = requireTrim(warehouse.getSettlementCurrency(),
                    "官方履约仓未维护币种：" + warehouse.getWarehouseName()).toUpperCase();
                validateCurrency(currency);
                if (selectedCurrency != null && !selectedCurrency.equals(currency))
                {
                    throw new ServiceException("官方来源 SKU 派生的发货仓库必须使用相同币种");
                }
                selectedCurrency = currency;
                warehouse.setWarehouseKind(WAREHOUSE_OFFICIAL);
                warehouse.setSettlementCurrency(currency);
                warehouse.setSellerId(product.getSellerId());
                warehouseMap.putIfAbsent(warehouse.getWarehouseId(), warehouse);
            }
        }
        if (dimensionKeys.isEmpty())
        {
            throw new ServiceException("官方仓商品至少需要选择一个来源 SKU");
        }
        validateWarehouseKindChange(current, WAREHOUSE_OFFICIAL);
        List<ProductSpuWarehouse> warehouses = new ArrayList<>(warehouseMap.values());
        product.setWarehouses(warehouses);
        product.setWarehouseIds(warehouses.stream().map(ProductSpuWarehouse::getWarehouseId).collect(Collectors.toList()));
    }

    private void validateWarehouseKindChange(ProductSpu current, String selectedKind)
    {
        if (current == null || STATUS_DRAFT.equals(current.getSpuStatus()))
        {
            return;
        }
        String currentKind = resolveWarehouseKind(productDistributionMapper.selectWarehousesBySpuId(current.getSpuId()));
        if (StringUtils.isBlank(currentKind))
        {
            return;
        }
        if (!StringUtils.equals(currentKind, selectedKind))
        {
            throw new ServiceException("仅草稿商品允许修改仓库类型");
        }
    }

    private String resolveWarehouseKind(List<ProductSpuWarehouse> warehouses)
    {
        String resolvedKind = null;
        if (warehouses == null)
        {
            return null;
        }
        for (ProductSpuWarehouse warehouse : warehouses)
        {
            if (warehouse == null || StringUtils.isBlank(warehouse.getWarehouseKind()))
            {
                continue;
            }
            String kind = warehouse.getWarehouseKind();
            if (resolvedKind != null && !resolvedKind.equals(kind))
            {
                return "MIXED";
            }
            resolvedKind = kind;
        }
        return resolvedKind;
    }

    private List<Long> requireWarehouseIds(ProductSpu product)
    {
        Set<Long> ids = new LinkedHashSet<>();
        if (product.getWarehouseIds() != null)
        {
            for (Long warehouseId : product.getWarehouseIds())
            {
                if (warehouseId == null)
                {
                    throw new ServiceException("发货仓库参数不完整");
                }
                ids.add(warehouseId);
            }
        }
        if (ids.isEmpty() && product.getWarehouses() != null)
        {
            for (ProductSpuWarehouse warehouse : product.getWarehouses())
            {
                if (warehouse == null || warehouse.getWarehouseId() == null)
                {
                    throw new ServiceException("发货仓库参数不完整");
                }
                ids.add(warehouse.getWarehouseId());
            }
        }
        if (ids.isEmpty())
        {
            throw new ServiceException("请选择发货仓库");
        }
        return new ArrayList<>(ids);
    }

    private void saveWarehouses(ProductSpu product)
    {
        List<ProductSpuWarehouse> warehouses = product.getWarehouses();
        if (warehouses == null || warehouses.isEmpty())
        {
            throw new ServiceException("请选择发货仓库");
        }
        productDistributionMapper.deleteWarehousesBySpuId(product.getSpuId());
        for (ProductSpuWarehouse warehouse : warehouses)
        {
            warehouse.setSpuId(product.getSpuId());
            warehouse.setSellerId(product.getSellerId());
            warehouse.setCreateBy(currentUsername());
            productDistributionMapper.insertSpuWarehouse(warehouse);
        }
    }

    private void fillWarehouseBindings(ProductSpu product)
    {
        List<ProductSpuWarehouse> warehouses = productDistributionMapper.selectWarehousesBySpuId(product.getSpuId());
        product.setWarehouses(warehouses);
        product.setWarehouseIds(warehouses.stream().map(ProductSpuWarehouse::getWarehouseId).collect(Collectors.toList()));
    }

    private String resolveWarehouseCurrency(ProductSpu product)
    {
        List<ProductSpuWarehouse> warehouses = product.getWarehouses();
        if (warehouses == null || warehouses.isEmpty())
        {
            throw new ServiceException("请选择发货仓库以确定 SKU 币种");
        }
        return requireTrim(warehouses.get(0).getSettlementCurrency(), "发货仓库币种不能为空").toUpperCase();
    }

    private String warehouseLabel(Warehouse warehouse)
    {
        return StringUtils.defaultIfBlank(warehouse.getWarehouseName(),
            StringUtils.defaultIfBlank(warehouse.getWarehouseCode(), String.valueOf(warehouse.getWarehouseId())));
    }

    private void saveSkus(ProductSpu product, List<ProductSku> currentSkus, OfficialSourceSaveContext sourceContext)
    {
        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty())
        {
            throw new ServiceException("至少需要维护一个 SKU");
        }
        boolean officialWarehouseProduct = isOfficialWarehouseProduct(product);
        Map<Long, ProductSku> currentSkuMap = currentSkus.stream()
            .filter(sku -> sku.getSkuId() != null)
            .collect(Collectors.toMap(ProductSku::getSkuId, Function.identity()));
        Map<Long, ProductSkuSourceBinding> activeBindingBySkuId = officialWarehouseProduct
            ? selectActiveSourceBindingsBySkuIds(currentSkuMap.keySet()) : new HashMap<>();
        Map<String, ProductSkuSourceBinding> activeBindingBySourceSkuGroupKey = officialWarehouseProduct
            ? selectActiveSourceBindingsBySourceSkuGroupKeys(collectSourceSkuGroupKeys(skus, sourceContext))
            : new HashMap<>();
        Set<Long> submittedSkuIds = new HashSet<>();
        int index = 0;
        for (ProductSku sku : skus)
        {
            normalizeSku(product, sku, currentSkuMap.get(sku.getSkuId()), index++, sourceContext);
            if (sku.getSkuId() == null)
            {
                sku.setSystemSkuCode(generateSkuCode());
                String operator = currentUsername();
                sku.setCreateBy(operator);
                sku.setUpdateBy(operator);
                productDistributionMapper.insertSku(sku);
            }
            else
            {
                submittedSkuIds.add(sku.getSkuId());
                sku.setUpdateBy(currentUsername());
                productDistributionMapper.updateSku(sku);
            }
            saveSkuSourceBinding(product, sku, currentSkuMap.get(sku.getSkuId()), officialWarehouseProduct,
                sourceContext, activeBindingBySkuId, activeBindingBySourceSkuGroupKey);
        }
        ensureOfficialSourceBindingsSaved(product, skus);
        for (ProductSku currentSku : currentSkus)
        {
            if (!submittedSkuIds.contains(currentSku.getSkuId()))
            {
                if (BINDING_LOCKED.equals(currentSku.getLockStatus()))
                {
                    throw new ServiceException("已锁定来源 SKU 绑定的 SKU 不能通过商品编辑删除");
                }
                handleRemovedSkuSourceBinding(product, currentSku);
                productDistributionMapper.deleteSkuById(product.getSpuId(), currentSku.getSkuId(), currentUsername());
            }
        }
    }

    private Map<Long, ProductSkuSourceBinding> selectActiveSourceBindingsBySkuIds(Set<Long> skuIds)
    {
        Map<Long, ProductSkuSourceBinding> result = new HashMap<>();
        if (skuIds == null || skuIds.isEmpty())
        {
            return result;
        }
        List<Long> ids = skuIds.stream().filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty())
        {
            return result;
        }
        List<ProductSkuSourceBinding> bindings = productDistributionMapper.selectActiveSourceBindingsBySkuIds(ids);
        if (bindings == null)
        {
            return result;
        }
        for (ProductSkuSourceBinding binding : bindings)
        {
            if (binding != null && binding.getSkuId() != null)
            {
                result.putIfAbsent(binding.getSkuId(), binding);
            }
        }
        return result;
    }

    private Map<String, ProductSkuSourceBinding> selectActiveSourceBindingsBySourceSkuGroupKeys(
        Set<String> sourceSkuGroupKeys)
    {
        Map<String, ProductSkuSourceBinding> result = new HashMap<>();
        if (sourceSkuGroupKeys == null || sourceSkuGroupKeys.isEmpty())
        {
            return result;
        }
        List<String> keys = sourceSkuGroupKeys.stream().filter(StringUtils::isNotBlank).distinct()
            .collect(Collectors.toList());
        if (keys.isEmpty())
        {
            return result;
        }
        List<ProductSkuSourceBinding> bindings =
            productDistributionMapper.selectActiveSourceBindingsBySourceSkuGroupKeys(keys);
        if (bindings == null)
        {
            return result;
        }
        for (ProductSkuSourceBinding binding : bindings)
        {
            if (binding != null && StringUtils.isNotBlank(binding.getSourceSkuGroupKey()))
            {
                result.putIfAbsent(binding.getSourceSkuGroupKey(), binding);
            }
        }
        return result;
    }

    private Set<String> collectSourceSkuGroupKeys(List<ProductSku> skus, OfficialSourceSaveContext sourceContext)
    {
        Set<String> sourceSkuGroupKeys = new LinkedHashSet<>();
        if (skus == null)
        {
            return sourceSkuGroupKeys;
        }
        for (ProductSku sku : skus)
        {
            if (sku == null || StringUtils.isBlank(sku.getSourceDimensionGroupKey()))
            {
                continue;
            }
            ProductSkuSourceBinding snapshot = requireSourceSnapshot(sku.getSourceDimensionGroupKey(), sourceContext);
            if (StringUtils.isNotBlank(snapshot.getSourceSkuGroupKey()))
            {
                sourceSkuGroupKeys.add(snapshot.getSourceSkuGroupKey());
            }
        }
        return sourceSkuGroupKeys;
    }

    private void normalizeSku(ProductSpu product, ProductSku sku, ProductSku currentSku, int index,
        OfficialSourceSaveContext sourceContext)
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
        if (isOfficialWarehouseProduct(product))
        {
            applySourceSnapshotToSku(sku, requireSourceSnapshot(sku.getSourceDimensionGroupKey(), sourceContext));
        }
        sku.setCurrencyCode(resolveWarehouseCurrency(product));
        sku.setSkuStatus(normalizeSkuSaveStatus(product, sku, currentSku));
        sku.setControlStatus(currentSku == null ? CONTROL_NORMAL
            : normalizeControlStatus(StringUtils.defaultIfBlank(currentSku.getControlStatus(), CONTROL_NORMAL)));
        sku.setControlReason(currentSku == null ? "" : trimToEmpty(currentSku.getControlReason()));
        sku.setSortOrder(sku.getSortOrder() == null ? index : sku.getSortOrder());
        sku.setRemark(StringUtils.defaultString(sku.getRemark()));
        validateMoney(sku.getSupplyPrice(), "SKU 供货价不能为空且不能小于 0");
        if (currentSku != null)
        {
            sku.setSalePrice(currentSku.getSalePrice());
        }
        else if (sku.getSalePrice() != null)
        {
            validateMoney(sku.getSalePrice(), "SKU 销售价不能小于 0");
        }
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
        if (currentSku == null && !STATUS_DRAFT.equals(requested))
        {
            throw new ServiceException("新增 SKU 只能保存为草稿，请提交商品审核后进入待上架");
        }
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

    private boolean isOfficialWarehouseProduct(ProductSpu product)
    {
        return WAREHOUSE_OFFICIAL.equals(resolveWarehouseKind(product.getWarehouses()))
            || WAREHOUSE_OFFICIAL.equals(trimToEmpty(product.getWarehouseKind()));
    }

    private OfficialSourceSaveContext buildOfficialSourceSaveContext(ProductSpu product)
    {
        if (!isOfficialWarehouseProduct(product))
        {
            return OfficialSourceSaveContext.empty();
        }
        Set<String> dimensionKeys = collectSubmittedSourceDimensionKeys(product);
        if (dimensionKeys.isEmpty())
        {
            return OfficialSourceSaveContext.empty();
        }
        List<String> keyList = new ArrayList<>(dimensionKeys);
        Map<String, ProductSkuSourceBinding> snapshotMap = new HashMap<>();
        List<SourceProductBindingSnapshot> snapshots =
            sourceSkuPairingProjectionService().selectOfficialSourceBindingSnapshots(keyList);
        if (snapshots != null)
        {
            for (SourceProductBindingSnapshot source : snapshots)
            {
                ProductSkuSourceBinding snapshot = toProductSkuSourceBinding(source);
                if (snapshot != null && StringUtils.isNotBlank(snapshot.getSourceDimensionGroupKey()))
                {
                    snapshotMap.put(snapshot.getSourceDimensionGroupKey(), snapshot);
                }
            }
        }
        Map<String, List<ProductSpuWarehouse>> warehouseMap = new HashMap<>();
        List<SourceOfficialWarehouseOption> options =
            sourceSkuPairingProjectionService().selectOfficialWarehousesBySourceDimensionGroups(keyList);
        if (options != null)
        {
            for (SourceOfficialWarehouseOption option : options)
            {
                String key = trimToEmpty(option.getSourceDimensionGroupKey());
                if (StringUtils.isBlank(key))
                {
                    continue;
                }
                warehouseMap.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(toProductSpuWarehouse(option));
            }
        }
        return new OfficialSourceSaveContext(true, snapshotMap, warehouseMap);
    }

    private Set<String> collectSubmittedSourceDimensionKeys(ProductSpu product)
    {
        Set<String> dimensionKeys = new LinkedHashSet<>();
        if (product == null || product.getSkus() == null)
        {
            return dimensionKeys;
        }
        for (ProductSku sku : product.getSkus())
        {
            if (sku == null)
            {
                continue;
            }
            String key = trimToEmpty(sku.getSourceDimensionGroupKey());
            if (StringUtils.isNotBlank(key))
            {
                dimensionKeys.add(key);
            }
        }
        return dimensionKeys;
    }

    private ProductSkuSourceBinding requireSourceSnapshot(String sourceDimensionGroupKey)
    {
        return requireSourceSnapshot(sourceDimensionGroupKey, OfficialSourceSaveContext.empty());
    }

    private ProductSkuSourceBinding requireSourceSnapshot(String sourceDimensionGroupKey,
        OfficialSourceSaveContext sourceContext)
    {
        String dimensionGroupKey = requireTrim(sourceDimensionGroupKey, "官方仓 SKU 必须选择来源 SKU");
        ProductSkuSourceBinding snapshot = null;
        if (sourceContext != null && sourceContext.isBatchLoaded())
        {
            snapshot = sourceContext.snapshot(dimensionGroupKey);
        }
        else
        {
            snapshot = toProductSkuSourceBinding(
                sourceSkuPairingProjectionService().selectOfficialSourceBindingSnapshot(dimensionGroupKey));
        }
        if (snapshot == null)
        {
            throw new ServiceException("来源 SKU 不存在或不是可用状态");
        }
        if (!SOURCE_SCOPE_OFFICIAL_MASTER.equals(snapshot.getSourceScope()))
        {
            throw new ServiceException("当前商品只允许绑定官方来源 SKU");
        }
        if (StringUtils.isBlank(snapshot.getSourceSkuGroupKey()))
        {
            throw new ServiceException("来源 SKU 缺少稳定分组键");
        }
        if (snapshot.getMeasureLengthCm() == null || snapshot.getMeasureWidthCm() == null
            || snapshot.getMeasureHeightCm() == null || snapshot.getMeasureWeightKg() == null)
        {
            throw new ServiceException("来源 SKU 缺少完整尺寸重量：" + snapshot.getMasterSku());
        }
        return snapshot;
    }

    private ProductSkuSourceBinding toProductSkuSourceBinding(SourceProductBindingSnapshot source)
    {
        if (source == null)
        {
            return null;
        }
        ProductSkuSourceBinding snapshot = new ProductSkuSourceBinding();
        snapshot.setSourceScope(source.getSourceScope());
        snapshot.setSourceSkuGroupKey(source.getSourceSkuGroupKey());
        snapshot.setSourceDimensionGroupKey(source.getSourceDimensionGroupKey());
        snapshot.setMasterSku(source.getMasterSku());
        snapshot.setMasterProductNameSnapshot(source.getMasterProductNameSnapshot());
        snapshot.setSourcePayloadHash(source.getSourcePayloadHash());
        snapshot.setWmsPayloadHash(source.getWmsPayloadHash());
        snapshot.setMeasureLengthCm(source.getMeasureLengthCm());
        snapshot.setMeasureWidthCm(source.getMeasureWidthCm());
        snapshot.setMeasureHeightCm(source.getMeasureHeightCm());
        snapshot.setMeasureWeightKg(source.getMeasureWeightKg());
        snapshot.setMeasureSource(source.getMeasureSource());
        snapshot.setCurrencyCode(source.getCurrencyCode());
        snapshot.setSourceWarehouseNames(source.getSourceWarehouseNames());
        snapshot.setSourceWarehouseCount(source.getSourceWarehouseCount());
        return snapshot;
    }

    private void applySourceSnapshotToSku(ProductSku sku, ProductSkuSourceBinding snapshot)
    {
        sku.setSourceScope(snapshot.getSourceScope());
        sku.setSourceSkuGroupKey(snapshot.getSourceSkuGroupKey());
        sku.setSourceDimensionGroupKey(snapshot.getSourceDimensionGroupKey());
        sku.setMasterSku(snapshot.getMasterSku());
        sku.setMasterProductNameSnapshot(snapshot.getMasterProductNameSnapshot());
        sku.setSourcePayloadHash(snapshot.getSourcePayloadHash());
        sku.setWmsPayloadHash(snapshot.getWmsPayloadHash());
        sku.setMeasureLengthCm(snapshot.getMeasureLengthCm());
        sku.setMeasureWidthCm(snapshot.getMeasureWidthCm());
        sku.setMeasureHeightCm(snapshot.getMeasureHeightCm());
        sku.setMeasureWeightKg(snapshot.getMeasureWeightKg());
        sku.setMeasureSource(snapshot.getMeasureSource());
        sku.setSourceWarehouseNames(snapshot.getSourceWarehouseNames());
        sku.setSourceWarehouseCount(snapshot.getSourceWarehouseCount());
        sku.setLengthValue(formatDimensionText(snapshot.getMeasureLengthCm()));
        sku.setWidthValue(formatDimensionText(snapshot.getMeasureWidthCm()));
        sku.setHeightValue(formatDimensionText(snapshot.getMeasureHeightCm()));
        sku.setWeight(formatWeightText(snapshot.getMeasureWeightKg()));
    }

    private void saveSkuSourceBinding(ProductSpu product, ProductSku sku, ProductSku currentSku,
        boolean officialWarehouseProduct, OfficialSourceSaveContext sourceContext,
        Map<Long, ProductSkuSourceBinding> activeBindingBySkuId,
        Map<String, ProductSkuSourceBinding> activeBindingBySourceSkuGroupKey)
    {
        if (!officialWarehouseProduct)
        {
            releaseUnlockedSkuSourceBinding(product, sku, "商品切换为非官方仓");
            return;
        }
        ProductSkuSourceBinding snapshot = requireSourceSnapshot(sku.getSourceDimensionGroupKey(), sourceContext);
        ProductSkuSourceBinding currentBinding =
            activeBindingBySkuId.get(sku.getSkuId());
        ProductSkuSourceBinding occupiedBinding =
            activeBindingBySourceSkuGroupKey.get(snapshot.getSourceSkuGroupKey());
        if (occupiedBinding != null && !sku.getSkuId().equals(occupiedBinding.getSkuId()))
        {
            throw new ServiceException("该官方来源 SKU 已绑定其他商城 SKU：" + snapshot.getMasterSku());
        }
        if (currentBinding != null && BINDING_LOCKED.equals(currentBinding.getLockStatus())
            && !StringUtils.equals(currentBinding.getSourceDimensionGroupKey(), snapshot.getSourceDimensionGroupKey()))
        {
            throw new ServiceException("来源 SKU 绑定已锁定，仅管理端换绑功能可以调整");
        }
        ProductSkuSourceBinding binding = buildSourceBinding(product, sku, snapshot, currentBinding);
        boolean lockedBefore = currentBinding != null && BINDING_LOCKED.equals(currentBinding.getLockStatus());
        if (currentBinding == null)
        {
            binding.setCreateBy(currentUsername());
            requireAffectedRows(productDistributionMapper.insertSourceBinding(binding), "官方仓 SKU 来源绑定保存失败");
            recordSkuSourceLog(generateBatchNo(), OP_SOURCE_BIND, product, sku, "-", binding.getMasterSku(), "");
        }
        else if (StringUtils.equals(currentBinding.getSourceDimensionGroupKey(), binding.getSourceDimensionGroupKey()))
        {
            binding.setBindingId(currentBinding.getBindingId());
            requireAffectedRows(productDistributionMapper.updateActiveSourceBinding(binding), "官方仓 SKU 来源绑定更新失败");
        }
        else
        {
            productDistributionMapper.markSourceBindingReplaced(currentBinding.getBindingId(), "草稿商品换绑来源 SKU",
                currentUsername());
            binding.setCreateBy(currentUsername());
            requireAffectedRows(productDistributionMapper.insertSourceBinding(binding), "官方仓 SKU 来源绑定保存失败");
            recordSkuSourceLog(generateBatchNo(), OP_SOURCE_REBIND, product, sku, currentBinding.getMasterSku(),
                binding.getMasterSku(), "草稿商品换绑来源 SKU");
        }
        boolean sourcePairingProjectionChanged = isSourcePairingProjectionChanged(currentBinding, binding);
        if (isSourcePairingProjectionScopeChanged(currentBinding, binding))
        {
            removeSourcePairingProjection(currentBinding);
        }
        if (sourcePairingProjectionChanged)
        {
            syncSourcePairingProjection(binding);
        }
        if (!lockedBefore && BINDING_LOCKED.equals(binding.getLockStatus()))
        {
            recordSkuSourceLog(generateBatchNo(), OP_SOURCE_LOCK, product, sku, binding.getMasterSku(),
                binding.getMasterSku(), "商品提交后锁定来源 SKU 绑定");
        }
    }

    private void ensureOfficialSourceBindingsSaved(ProductSpu product, List<ProductSku> skus)
    {
        if (!isOfficialWarehouseProduct(product))
        {
            return;
        }
        Set<Long> skuIds = skus.stream()
            .map(ProductSku::getSkuId)
            .filter(id -> id != null)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ProductSkuSourceBinding> savedBindingBySkuId = selectActiveSourceBindingsBySkuIds(skuIds);
        for (ProductSku sku : skus)
        {
            ProductSkuSourceBinding savedBinding = savedBindingBySkuId.get(sku.getSkuId());
            if (savedBinding == null
                || !StringUtils.equals(savedBinding.getSourceDimensionGroupKey(), sku.getSourceDimensionGroupKey()))
            {
                throw new ServiceException("官方仓 SKU 来源绑定保存失败，请重新配对来源 SKU");
            }
        }
    }

    private void requireAffectedRows(int rows, String message)
    {
        if (rows <= 0)
        {
            throw new ServiceException(message);
        }
    }

    private ProductSkuSourceBinding buildSourceBinding(ProductSpu product, ProductSku sku,
        ProductSkuSourceBinding snapshot, ProductSkuSourceBinding currentBinding)
    {
        ProductSkuSourceBinding binding = new ProductSkuSourceBinding();
        binding.setSpuId(product.getSpuId());
        binding.setSkuId(sku.getSkuId());
        binding.setSellerId(product.getSellerId());
        binding.setSystemSkuCode(requireTrim(sku.getSystemSkuCode(), "系统 SKU 编码不能为空"));
        binding.setSourceScope(SOURCE_SCOPE_OFFICIAL_MASTER);
        binding.setSourceSkuGroupKey(snapshot.getSourceSkuGroupKey());
        binding.setSourceDimensionGroupKey(snapshot.getSourceDimensionGroupKey());
        binding.setMasterSku(snapshot.getMasterSku());
        binding.setMasterProductNameSnapshot(snapshot.getMasterProductNameSnapshot());
        binding.setSystemSkuNameSnapshot(buildSystemSkuNameSnapshot(product, sku));
        binding.setSellerNameSnapshot(trimToEmpty(product.getSellerName()));
        binding.setSourcePayloadHash(trimToEmpty(snapshot.getSourcePayloadHash()));
        binding.setWmsPayloadHash(trimToEmpty(snapshot.getWmsPayloadHash()));
        binding.setMeasureLengthCm(snapshot.getMeasureLengthCm());
        binding.setMeasureWidthCm(snapshot.getMeasureWidthCm());
        binding.setMeasureHeightCm(snapshot.getMeasureHeightCm());
        binding.setMeasureWeightKg(snapshot.getMeasureWeightKg());
        binding.setMeasureSource(StringUtils.defaultIfBlank(snapshot.getMeasureSource(), "PRODUCT"));
        binding.setCurrencyCode(resolveWarehouseCurrency(product));
        binding.setSourceWarehouseNames(trimToEmpty(snapshot.getSourceWarehouseNames()));
        binding.setSourceWarehouseCount(snapshot.getSourceWarehouseCount() == null ? 0 : snapshot.getSourceWarehouseCount());
        binding.setBindingStatus(BINDING_ACTIVE);
        boolean shouldLock = shouldLockSourceBinding(product, sku);
        if (currentBinding != null && BINDING_LOCKED.equals(currentBinding.getLockStatus()))
        {
            binding.setLockStatus(BINDING_LOCKED);
            binding.setLockedTime(currentBinding.getLockedTime());
            binding.setLockedBy(currentBinding.getLockedBy());
        }
        else
        {
            binding.setLockStatus(shouldLock ? BINDING_LOCKED : BINDING_UNLOCKED);
            binding.setLockedTime(shouldLock ? DateUtils.getNowDate() : null);
            binding.setLockedBy(shouldLock ? currentUsername() : "");
        }
        binding.setReleaseReason("");
        binding.setReplaceReason("");
        binding.setActiveSkuKey(sku.getSkuId());
        binding.setActiveSourceKey(snapshot.getSourceSkuGroupKey());
        binding.setUpdateBy(currentUsername());
        binding.setRemark("");
        return binding;
    }

    private boolean shouldLockSourceBinding(ProductSpu product, ProductSku sku)
    {
        return !STATUS_DRAFT.equals(product.getSpuStatus()) || !STATUS_DRAFT.equals(sku.getSkuStatus());
    }

    private void handleRemovedSkuSourceBinding(ProductSpu product, ProductSku sku)
    {
        releaseUnlockedSkuSourceBinding(product, sku, "商品编辑移除SKU");
    }

    private void releaseUnlockedSkuSourceBinding(ProductSpu product, ProductSku sku, String reason)
    {
        if (sku == null || sku.getSkuId() == null)
        {
            return;
        }
        ProductSkuSourceBinding binding = productDistributionMapper.selectActiveSourceBindingBySkuId(sku.getSkuId());
        if (binding == null)
        {
            return;
        }
        if (BINDING_LOCKED.equals(binding.getLockStatus()))
        {
            return;
        }
        productDistributionMapper.releaseActiveSourceBindingBySkuId(sku.getSkuId(), reason, currentUsername());
        removeSourcePairingProjection(binding);
        recordSkuSourceLog(generateBatchNo(), OP_SOURCE_RELEASE, product, sku, binding.getMasterSku(), "-",
            reason);
    }

    private boolean isSourcePairingProjectionScopeChanged(ProductSkuSourceBinding currentBinding,
        ProductSkuSourceBinding binding)
    {
        if (currentBinding == null || binding == null)
        {
            return false;
        }
        return !StringUtils.equals(currentBinding.getSystemSkuCode(), binding.getSystemSkuCode())
            || !StringUtils.equals(currentBinding.getSourceSkuGroupKey(), binding.getSourceSkuGroupKey())
            || !StringUtils.equals(currentBinding.getSourceDimensionGroupKey(), binding.getSourceDimensionGroupKey());
    }

    private boolean isSourcePairingProjectionChanged(ProductSkuSourceBinding currentBinding,
        ProductSkuSourceBinding binding)
    {
        if (binding == null)
        {
            return false;
        }
        if (currentBinding == null)
        {
            return true;
        }
        return !StringUtils.equals(currentBinding.getSystemSkuCode(), binding.getSystemSkuCode())
            || !StringUtils.equals(currentBinding.getSourceSkuGroupKey(), binding.getSourceSkuGroupKey())
            || !StringUtils.equals(currentBinding.getSourceDimensionGroupKey(), binding.getSourceDimensionGroupKey())
            || !StringUtils.equals(currentBinding.getBindingStatus(), binding.getBindingStatus())
            || !StringUtils.equals(currentBinding.getLockStatus(), binding.getLockStatus());
    }

    private void removeSourcePairingProjection(ProductSkuSourceBinding binding)
    {
        if (binding == null)
        {
            return;
        }
        Set<String> affectedConnectionCodes = deleteUpstreamSkuPairings(binding);
        refreshSourcePairingProjectionReadModels(affectedConnectionCodes);
    }

    private void syncSourcePairingProjection(ProductSkuSourceBinding binding)
    {
        Set<String> affectedConnectionCodes = deleteUpstreamSkuPairings(binding);
        sourceSkuPairingProjectionService().upsertPairingsForProjection(toProjection(binding));
        addConnectionCodes(affectedConnectionCodes,
            selectSourceConnectionCodesByDimensionGroup(binding.getSourceDimensionGroupKey()));
        refreshSourcePairingProjectionReadModels(affectedConnectionCodes);
    }

    private Set<String> deleteUpstreamSkuPairings(ProductSkuSourceBinding binding)
    {
        Set<String> connectionCodes = new LinkedHashSet<>();
        if (binding == null || StringUtils.isBlank(binding.getSystemSkuCode()))
        {
            return connectionCodes;
        }
        addConnectionCodes(connectionCodes, selectConnectionCodesForPairingProjectionCleanup(binding));
        if (connectionCodes.isEmpty())
        {
            return connectionCodes;
        }
        sourceSkuPairingProjectionService().deletePairingsBySystemSkuAndConnectionCodes(binding.getSystemSkuCode(),
            new ArrayList<>(connectionCodes));
        return connectionCodes;
    }

    private List<String> selectConnectionCodesForPairingProjectionCleanup(ProductSkuSourceBinding binding)
    {
        List<String> connectionCodes = List.of();
        if (StringUtils.isNotBlank(binding.getSourceDimensionGroupKey()))
        {
            connectionCodes = selectSourceConnectionCodesByDimensionGroup(binding.getSourceDimensionGroupKey());
        }
        if ((connectionCodes == null || connectionCodes.isEmpty()) && StringUtils.isNotBlank(binding.getMasterSku()))
        {
            connectionCodes = sourceSkuPairingProjectionService().selectPairingConnectionCodesBySystemSkuAndMasterSku(
                binding.getSystemSkuCode(), binding.getMasterSku());
        }
        return connectionCodes;
    }

    private List<String> selectSourceConnectionCodesByDimensionGroup(String sourceDimensionGroupKey)
    {
        if (StringUtils.isBlank(sourceDimensionGroupKey))
        {
            return List.of();
        }
        List<String> connectionCodes =
            sourceSkuPairingProjectionService().selectSourceConnectionCodesByDimensionGroup(sourceDimensionGroupKey);
        return connectionCodes == null ? List.of() : connectionCodes;
    }

    private List<ProductSpuWarehouse> selectOfficialWarehousesBySourceDimensionGroup(String sourceDimensionGroupKey)
    {
        return selectOfficialWarehousesBySourceDimensionGroup(sourceDimensionGroupKey, OfficialSourceSaveContext.empty());
    }

    private List<ProductSpuWarehouse> selectOfficialWarehousesBySourceDimensionGroup(String sourceDimensionGroupKey,
        OfficialSourceSaveContext sourceContext)
    {
        String dimensionGroupKey = requireTrim(sourceDimensionGroupKey, "官方仓 SKU 必须选择来源 SKU");
        if (sourceContext != null && sourceContext.isBatchLoaded())
        {
            return sourceContext.officialWarehouses(dimensionGroupKey);
        }
        List<SourceOfficialWarehouseOption> options =
            sourceSkuPairingProjectionService().selectOfficialWarehousesBySourceDimensionGroup(dimensionGroupKey);
        if (options == null || options.isEmpty())
        {
            return List.of();
        }
        List<ProductSpuWarehouse> warehouses = new ArrayList<>();
        for (SourceOfficialWarehouseOption option : options)
        {
            warehouses.add(toProductSpuWarehouse(option));
        }
        return warehouses;
    }

    private ProductSpuWarehouse toProductSpuWarehouse(SourceOfficialWarehouseOption option)
    {
        ProductSpuWarehouse warehouse = new ProductSpuWarehouse();
        warehouse.setWarehouseId(option.getWarehouseId());
        warehouse.setWarehouseCode(option.getWarehouseCode());
        warehouse.setWarehouseName(option.getWarehouseName());
        warehouse.setWarehouseKind(option.getWarehouseKind());
        warehouse.setSettlementCurrency(option.getSettlementCurrency());
        return warehouse;
    }

    private SourceSkuPairingProjection toProjection(ProductSkuSourceBinding binding)
    {
        SourceSkuPairingProjection projection = new SourceSkuPairingProjection();
        projection.setSourceDimensionGroupKey(binding.getSourceDimensionGroupKey());
        projection.setSystemSkuCode(binding.getSystemSkuCode());
        projection.setSystemSkuName(binding.getSystemSkuNameSnapshot());
        projection.setSellerName(binding.getSellerNameSnapshot());
        projection.setUpdateBy(binding.getUpdateBy());
        return projection;
    }

    private ISourceSkuPairingProjectionService sourceSkuPairingProjectionService()
    {
        ISourceSkuPairingProjectionService service = sourceSkuPairingProjectionService.getIfAvailable();
        if (service == null)
        {
            throw new ServiceException("来源SKU配对投影服务不可用");
        }
        return service;
    }

    private void addConnectionCodes(Set<String> target, List<String> connectionCodes)
    {
        if (connectionCodes == null)
        {
            return;
        }
        for (String connectionCode : connectionCodes)
        {
            if (StringUtils.isNotBlank(connectionCode))
            {
                target.add(connectionCode);
            }
        }
    }

    private void refreshSourcePairingProjectionReadModels(Set<String> connectionCodes)
    {
        if (connectionCodes == null || connectionCodes.isEmpty())
        {
            return;
        }
        List<String> refreshConnectionCodes = new ArrayList<>(connectionCodes);
        runAfterCommitAsync("source-pairing-read-model-refresh", () -> {
            ISourceReadModelRefreshService readModelRefreshService = sourceReadModelRefreshService.getIfAvailable();
            if (readModelRefreshService == null)
            {
                return;
            }
            for (String connectionCode : refreshConnectionCodes)
            {
                readModelRefreshService.refreshOfficialMasterSkuPairingByConnection(connectionCode);
            }
        });
    }

    private void refreshInventoryOverviewAfterCommit(Long spuId)
    {
        if (spuId == null)
        {
            return;
        }
        runAfterCommitAsync("product-inventory-overview-refresh", () -> refreshInventoryOverview(spuId));
    }

    private void refreshInventoryOverview(Long spuId)
    {
        if (spuId == null || inventoryOverviewService == null)
        {
            return;
        }
        IInventoryOverviewService overviewService = inventoryOverviewService.getIfAvailable();
        if (overviewService != null)
        {
            overviewService.refreshProductInventoryOverview(spuId);
        }
    }

    private void runAfterCommitAsync(String taskName, Runnable task)
    {
        Runnable asyncTask = () -> readModelRefreshExecutor.execute(() -> runLoggedReadModelTask(taskName, task));
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    asyncTask.run();
                }
            });
        }
        else
        {
            asyncTask.run();
        }
    }

    private void runLoggedReadModelTask(String taskName, Runnable task)
    {
        long startedAt = System.currentTimeMillis();
        try
        {
            task.run();
            log.info("商城商品后台读模型刷新完成 task={} cost={}ms", taskName, System.currentTimeMillis() - startedAt);
        }
        catch (Exception e)
        {
            log.error("商城商品后台读模型刷新失败 task={} cost={}ms", taskName, System.currentTimeMillis() - startedAt, e);
        }
    }

    private void logCoreSaveCost(String action, Long spuId, long saveStartedAt, long spuSavedAt,
        long warehousesSavedAt, long skusSavedAt, long attributesSavedAt, long imagesSavedAt)
    {
        long now = System.currentTimeMillis();
        log.info("商城商品核心保存完成 action={} spuId={} total={}ms spu={}ms warehouses={}ms skusAndSource={}ms attributes={}ms images={}ms asyncRegister={}ms",
            action, spuId, now - saveStartedAt, spuSavedAt - saveStartedAt, warehousesSavedAt - spuSavedAt,
            skusSavedAt - warehousesSavedAt, attributesSavedAt - skusSavedAt, imagesSavedAt - attributesSavedAt,
            now - imagesSavedAt);
    }

    private void lockSkuSourceBindingIfNeeded(ProductSpu product, ProductSku sku, String targetStatus, String batchNo)
    {
        if (STATUS_DRAFT.equals(targetStatus))
        {
            return;
        }
        int rows = productDistributionMapper.lockActiveSourceBindingBySkuId(sku.getSkuId(), currentUsername());
        if (rows > 0)
        {
            ProductSkuSourceBinding binding = productDistributionMapper.selectActiveSourceBindingBySkuId(sku.getSkuId());
            String masterSku = binding == null ? "-" : binding.getMasterSku();
            recordSkuSourceLog(batchNo, OP_SOURCE_LOCK, product, sku, masterSku, masterSku, "商品状态流转后锁定来源 SKU 绑定");
        }
    }

    private String buildSystemSkuNameSnapshot(ProductSpu product, ProductSku sku)
    {
        String spec = buildSkuSpecSummary(sku);
        if (StringUtils.isBlank(spec))
        {
            return product.getProductName();
        }
        return product.getProductName() + " / " + spec;
    }

    private String buildSkuSpecSummary(ProductSku sku)
    {
        List<String> parts = new ArrayList<>();
        addSpecPart(parts, "颜色", sku.getColor());
        addSpecPart(parts, "尺寸", sku.getSize());
        addSpecPart(parts, "材质", sku.getMaterial());
        addSpecPart(parts, "风格", sku.getStyle());
        addSpecPart(parts, "型号", sku.getModel());
        addSpecPart(parts, "商品数量", sku.getPackageQuantity());
        addSpecPart(parts, "容量", sku.getCapacity());
        return String.join(" / ", parts);
    }

    private void addSpecPart(List<String> parts, String label, String value)
    {
        if (StringUtils.isNotBlank(value))
        {
            parts.add(label + "：" + value);
        }
    }

    private String formatDimensionText(BigDecimal value)
    {
        return formatMeasureText(value, "cm");
    }

    private String formatWeightText(BigDecimal value)
    {
        return formatMeasureText(value, "kg");
    }

    private String formatMeasureText(BigDecimal value, String unit)
    {
        if (value == null)
        {
            return "";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + unit;
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

    private ProductSpu requireProduct(Long spuId, Long sellerId)
    {
        ProductSpu product = productDistributionMapper.selectProductByIdAndSellerId(spuId, sellerId);
        if (product == null)
        {
            throw new ServiceException("商城商品不存在");
        }
        return product;
    }

    private ProductSpu requireOnSaleProduct(Long spuId)
    {
        ProductSpu product = productDistributionMapper.selectOnSaleProductById(spuId);
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
        if (product.getWarehouses() == null || product.getWarehouses().isEmpty())
        {
            throw new ServiceException("商品上架前必须绑定发货仓库");
        }
    }

    private void validateSpuOnSale(ProductSpu product)
    {
        ProductSpu detail = selectProductById(product.getSpuId());
        validateSpuBasics(detail);
        ensureControlNormal(detail.getControlStatus(), "停用商品不允许上架");
        List<ProductSku> skus = detail.getSkus();
        if (skus == null || skus.isEmpty())
        {
            throw new ServiceException("商品上架前至少需要一个 SKU");
        }
        boolean hasValidSku = false;
        for (ProductSku sku : skus)
        {
            if ((STATUS_READY.equals(sku.getSkuStatus()) || STATUS_ON_SALE.equals(sku.getSkuStatus())
                || STATUS_OFF_SALE.equals(sku.getSkuStatus()))
                && CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(sku.getControlStatus(), CONTROL_NORMAL)))
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
        if (WAREHOUSE_OFFICIAL.equals(sku.getWarehouseKindSummary()) && StringUtils.isBlank(sku.getSourceSkuGroupKey()))
        {
            throw new ServiceException("官方仓 SKU 上架前必须绑定来源 SKU");
        }
        ensureControlNormal(sku.getControlStatus(), "停用 SKU 不允许上架");
    }

    private void validateSpuTransition(String currentStatus, String targetStatus)
    {
        if (StringUtils.equals(currentStatus, targetStatus))
        {
            return;
        }
        if (STATUS_DRAFT.equals(currentStatus))
        {
            throw new ServiceException("草稿商品请提交商品审核，审核通过后进入待上架");
        }
        if (STATUS_READY.equals(currentStatus) && !STATUS_ON_SALE.equals(targetStatus))
        {
            throw new ServiceException("待上架商品只能切换为已上架");
        }
        if (STATUS_ON_SALE.equals(currentStatus) && !STATUS_OFF_SALE.equals(targetStatus))
        {
            throw new ServiceException("已上架商品只能切换为已下架");
        }
        if (STATUS_OFF_SALE.equals(currentStatus) && !STATUS_ON_SALE.equals(targetStatus))
        {
            throw new ServiceException("已下架商品只能切换为已上架");
        }
    }

    private void validateSkuTransition(String currentStatus, String targetStatus)
    {
        if (StringUtils.equals(currentStatus, targetStatus))
        {
            return;
        }
        if (STATUS_DRAFT.equals(currentStatus))
        {
            throw new ServiceException("草稿 SKU 请提交商品审核，审核通过后进入待上架");
        }
        if (STATUS_READY.equals(currentStatus) && !STATUS_ON_SALE.equals(targetStatus))
        {
            throw new ServiceException("待上架 SKU 只能切换为已上架");
        }
        if (STATUS_ON_SALE.equals(currentStatus) && !STATUS_OFF_SALE.equals(targetStatus))
        {
            throw new ServiceException("已上架 SKU 只能切换为已下架");
        }
        if (STATUS_OFF_SALE.equals(currentStatus) && !STATUS_ON_SALE.equals(targetStatus))
        {
            throw new ServiceException("已下架 SKU 只能切换为已上架");
        }
    }

    private void ensureNoPendingProductReview(Long spuId)
    {
        if (productReviewMapper.countPendingReviewByKey("SPU:" + spuId) > 0)
        {
            throw new ServiceException("当前商品存在待审核单，审核完成后再修改商品");
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

    private String normalizeControlStatus(String status)
    {
        String value = requireTrim(status, "商品管控状态不能为空").toUpperCase();
        if (!CONTROL_STATUSES.contains(value))
        {
            throw new ServiceException("商品管控状态不正确：" + value);
        }
        return value;
    }

    private String normalizeControlReason(String controlStatus, String reason)
    {
        String normalizedReason = trimToEmpty(reason);
        if (CONTROL_DISABLED.equals(controlStatus) && StringUtils.isBlank(normalizedReason))
        {
            throw new ServiceException("停用原因不能为空");
        }
        return CONTROL_NORMAL.equals(controlStatus) ? "" : normalizedReason;
    }

    private String normalizeSalesStatusReason(String targetStatus, String reason)
    {
        String normalizedReason = trimToEmpty(reason);
        if ((STATUS_ON_SALE.equals(targetStatus) || STATUS_OFF_SALE.equals(targetStatus))
            && StringUtils.isBlank(normalizedReason))
        {
            throw new ServiceException("销售状态调整原因不能为空");
        }
        return normalizedReason;
    }

    private void ensureControlNormal(String controlStatus, String message)
    {
        if (!CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(controlStatus, CONTROL_NORMAL)))
        {
            throw new ServiceException(message);
        }
    }

    private void validateControlTransition(String currentStatus, String targetStatus, String ownerName)
    {
        String current = normalizeControlStatus(StringUtils.defaultIfBlank(currentStatus, CONTROL_NORMAL));
        if (StringUtils.equals(current, targetStatus))
        {
            throw new ServiceException(ownerName + "管控状态未变化");
        }
    }

    private List<Long> requireIds(List<Long> ids, String message)
    {
        if (ids == null || ids.isEmpty())
        {
            throw new ServiceException(message);
        }
        List<Long> result = ids.stream()
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
        if (result.isEmpty())
        {
            throw new ServiceException(message);
        }
        return result;
    }

    private String generateBatchNo()
    {
        return "PLOG" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + UUID.randomUUID().toString().replace("-", "");
    }

    private void recordSpuStatusLog(String batchNo, ProductSpu product, String targetStatus, String reason)
    {
        ProductDistributionOperationLog log = baseOperationLog(batchNo, OP_SALES_STATUS_CHANGE, OWNER_TYPE_SPU,
            product, null);
        log.setBeforeSalesStatus(product.getSpuStatus());
        log.setAfterSalesStatus(targetStatus);
        log.setReason(reason);
        log.setChangeSummary("SPU销售状态：" + displayChange(product.getSpuStatus(), targetStatus));
        log.setDiffJson(simpleDiff("spuStatus", product.getSpuStatus(), targetStatus));
        operationLogMapper.insertOperationLog(log);
    }

    private void recordSkuStatusLog(String batchNo, ProductSpu product, ProductSku sku, String targetStatus,
        String reason)
    {
        ProductDistributionOperationLog log = baseOperationLog(batchNo, OP_SALES_STATUS_CHANGE, OWNER_TYPE_SKU,
            product, sku);
        log.setBeforeSalesStatus(sku.getSkuStatus());
        log.setAfterSalesStatus(targetStatus);
        log.setReason(reason);
        log.setChangeSummary("SKU销售状态：" + displayChange(sku.getSkuStatus(), targetStatus));
        log.setDiffJson(simpleDiff("skuStatus", sku.getSkuStatus(), targetStatus));
        operationLogMapper.insertOperationLog(log);
    }

    private void recordSpuControlLog(String batchNo, ProductSpu product, String targetStatus, String reason)
    {
        ProductDistributionOperationLog log = baseOperationLog(batchNo, controlOperationType(targetStatus),
            OWNER_TYPE_SPU, product, null);
        log.setBeforeControlStatus(StringUtils.defaultIfBlank(product.getControlStatus(), CONTROL_NORMAL));
        log.setAfterControlStatus(targetStatus);
        log.setReason(reason);
        log.setChangeSummary("SPU管控状态：" + displayChange(log.getBeforeControlStatus(), targetStatus));
        log.setDiffJson(simpleDiff("controlStatus", log.getBeforeControlStatus(), targetStatus));
        operationLogMapper.insertOperationLog(log);
    }

    private void recordSkuControlLog(String batchNo, ProductSpu product, ProductSku sku, String targetStatus,
        String reason)
    {
        ProductDistributionOperationLog log = baseOperationLog(batchNo, controlOperationType(targetStatus),
            OWNER_TYPE_SKU, product, sku);
        log.setBeforeControlStatus(StringUtils.defaultIfBlank(sku.getControlStatus(), CONTROL_NORMAL));
        log.setAfterControlStatus(targetStatus);
        log.setReason(reason);
        log.setChangeSummary("SKU管控状态：" + displayChange(log.getBeforeControlStatus(), targetStatus));
        log.setDiffJson(simpleDiff("controlStatus", log.getBeforeControlStatus(), targetStatus));
        operationLogMapper.insertOperationLog(log);
    }

    private void recordSkuPriceLog(String batchNo, ProductSpu product, ProductSku sku, BigDecimal targetPrice,
        String reason)
    {
        ProductDistributionOperationLog log = baseOperationLog(batchNo, OP_SALE_PRICE_ADJUST, OWNER_TYPE_SKU,
            product, sku);
        log.setBeforeSalePrice(sku.getSalePrice());
        log.setAfterSalePrice(targetPrice);
        log.setCurrencyCode(sku.getCurrencyCode());
        log.setReason(reason);
        log.setChangeSummary("SKU销售价：" + displayChange(String.valueOf(sku.getSalePrice()), String.valueOf(targetPrice)));
        log.setDiffJson(simpleDiff("salePrice", String.valueOf(sku.getSalePrice()), String.valueOf(targetPrice)));
        operationLogMapper.insertOperationLog(log);
    }

    private void recordSkuSourceLog(String batchNo, String operationType, ProductSpu product, ProductSku sku,
        String beforeSource, String afterSource, String reason)
    {
        ProductDistributionOperationLog log = baseOperationLog(batchNo, operationType, OWNER_TYPE_SKU, product, sku);
        log.setReason(trimToEmpty(reason));
        log.setChangeSummary("SKU来源绑定：" + displayChange(beforeSource, afterSource));
        log.setDiffJson(simpleDiff("sourceSku", beforeSource, afterSource));
        operationLogMapper.insertOperationLog(log);
    }

    private ProductDistributionOperationLog baseOperationLog(String batchNo, String operationType, String ownerType,
        ProductSpu product, ProductSku sku)
    {
        ProductDistributionOperationLog log = new ProductDistributionOperationLog();
        log.setBatchNo(batchNo);
        log.setOperationType(operationType);
        log.setOwnerType(ownerType);
        log.setSpuId(product.getSpuId());
        log.setSkuId(sku == null ? null : sku.getSkuId());
        log.setSystemSpuCode(product.getSystemSpuCode());
        log.setSystemSkuCode(sku == null ? null : sku.getSystemSkuCode());
        log.setSellerId(product.getSellerId());
        log.setSellerName(product.getSellerName());
        log.setOperatorName(currentUsername());
        log.setOperationSource(OP_SOURCE_PAGE);
        return log;
    }

    private String controlOperationType(String targetStatus)
    {
        return CONTROL_DISABLED.equals(targetStatus) ? OP_CONTROL_DISABLE : OP_CONTROL_RECOVER;
    }

    private String displayChange(String before, String after)
    {
        return StringUtils.defaultString(before, "-") + " -> " + StringUtils.defaultString(after, "-");
    }

    private String simpleDiff(String field, String before, String after)
    {
        return "{\"field\":\"" + escapeJson(field) + "\",\"before\":\"" + escapeJson(before)
            + "\",\"after\":\"" + escapeJson(after) + "\"}";
    }

    private String escapeJson(String value)
    {
        return StringUtils.defaultString(value).replace("\\", "\\\\").replace("\"", "\\\"");
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

    private static class OfficialSourceSaveContext
    {
        private static final OfficialSourceSaveContext EMPTY =
            new OfficialSourceSaveContext(false, Map.of(), Map.of());

        private final boolean batchLoaded;
        private final Map<String, ProductSkuSourceBinding> snapshotMap;
        private final Map<String, List<ProductSpuWarehouse>> warehouseMap;

        private OfficialSourceSaveContext(boolean batchLoaded, Map<String, ProductSkuSourceBinding> snapshotMap,
            Map<String, List<ProductSpuWarehouse>> warehouseMap)
        {
            this.batchLoaded = batchLoaded;
            this.snapshotMap = snapshotMap == null ? Map.of() : snapshotMap;
            this.warehouseMap = warehouseMap == null ? Map.of() : warehouseMap;
        }

        private static OfficialSourceSaveContext empty()
        {
            return EMPTY;
        }

        private boolean isBatchLoaded()
        {
            return batchLoaded;
        }

        private ProductSkuSourceBinding snapshot(String sourceDimensionGroupKey)
        {
            return snapshotMap.get(sourceDimensionGroupKey);
        }

        private List<ProductSpuWarehouse> officialWarehouses(String sourceDimensionGroupKey)
        {
            List<ProductSpuWarehouse> warehouses = warehouseMap.get(sourceDimensionGroupKey);
            return warehouses == null ? List.of() : warehouses;
        }
    }
}
