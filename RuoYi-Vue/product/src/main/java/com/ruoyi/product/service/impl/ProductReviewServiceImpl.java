package com.ruoyi.product.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.product.domain.ProductDistributionOperationLog;
import com.ruoyi.product.domain.ProductReviewItem;
import com.ruoyi.product.domain.ProductReviewOperationLog;
import com.ruoyi.product.domain.ProductReviewRequest;
import com.ruoyi.product.domain.ProductReviewSnapshot;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSalePriceUpdateRequest;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.domain.ProductSpuWarehouse;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.product.mapper.ProductDistributionOperationLogMapper;
import com.ruoyi.product.mapper.ProductReviewMapper;
import com.ruoyi.product.service.IProductDistributionService;
import com.ruoyi.product.service.IProductReviewService;

/**
 * 商品审核服务实现。
 */
@Service
public class ProductReviewServiceImpl implements IProductReviewService
{
    private static final String REVIEW_TYPE_NEW_PRODUCT = "NEW_PRODUCT";
    private static final String REVIEW_TYPE_ADD_SKU = "ADD_SKU";
    private static final String REVIEW_TYPE_EDIT_PRODUCT_INFO = "EDIT_PRODUCT_INFO";
    private static final String REVIEW_TYPE_EDIT_SKU_INFO = "EDIT_SKU_INFO";
    private static final String REVIEW_TYPE_EDIT_PRICE = "EDIT_PRICE";
    private static final String REVIEW_STATUS_PENDING = "PENDING";
    private static final String REVIEW_STATUS_APPROVED = "APPROVED";
    private static final String REVIEW_STATUS_REJECTED = "REJECTED";
    private static final String TERMINAL_ADMIN = "ADMIN";
    private static final String ITEM_TYPE_SPU = "SPU";
    private static final String ITEM_TYPE_SKU = "SKU";
    private static final String CHANGE_CREATE = "CREATE";
    private static final String CHANGE_UPDATE = "UPDATE";
    private static final String ITEM_STATUS_PENDING = "PENDING";
    private static final String SNAPSHOT_BEFORE = "BEFORE";
    private static final String SNAPSHOT_AFTER = "AFTER";
    private static final String PAYLOAD_SPU = "SPU";
    private static final String PAYLOAD_SKU = "SKU";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_READY = "READY";
    private static final String CONTROL_NORMAL = "NORMAL";
    private static final String OP_SUBMIT = "SUBMIT";
    private static final String OP_APPROVE = "APPROVE";
    private static final String OP_REJECT = "REJECT";
    private static final String DIST_OP_REVIEW_APPROVE = "REVIEW_APPROVE";
    private static final String DIST_OP_SOURCE_REVIEW = "REVIEW";

    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Autowired
    private IProductDistributionService productDistributionService;

    @Autowired
    private ProductDistributionMapper productDistributionMapper;

    @Autowired
    private ProductDistributionOperationLogMapper operationLogMapper;

    @Override
    public List<ProductReviewRequest> selectReviewList(ProductReviewRequest query)
    {
        return productReviewMapper.selectReviewList(query);
    }

    @Override
    public ProductReviewRequest selectReviewById(Long reviewId)
    {
        ProductReviewRequest review = requireReview(reviewId);
        review.setItems(productReviewMapper.selectReviewItems(reviewId));
        review.setSnapshots(productReviewMapper.selectReviewSnapshots(reviewId));
        return review;
    }

    @Override
    public ProductSpu selectLatestRejectedReusableSubmission(Long spuId)
    {
        if (spuId == null)
        {
            throw new ServiceException("商品不存在");
        }
        ProductReviewRequest review = productReviewMapper.selectLatestRejectedReusableReviewBySpuId(spuId);
        if (review == null)
        {
            return null;
        }
        List<ProductReviewSnapshot> snapshots = productReviewMapper.selectReviewSnapshots(review.getReviewId());
        ProductReviewSnapshot productSnapshot = findProductSnapshot(snapshots, SNAPSHOT_AFTER);
        if (productSnapshot == null)
        {
            return null;
        }
        ProductSpu product = parseProductSnapshot(productSnapshot.getPayloadJson());
        product.setSpuId(spuId);
        List<ProductSku> skus = snapshots == null ? List.of() : snapshots.stream()
            .filter(snapshot -> SNAPSHOT_AFTER.equals(snapshot.getSnapshotRole()) && PAYLOAD_SKU.equals(snapshot.getPayloadType()))
            .map(snapshot -> parseSkuSnapshot(snapshot.getPayloadJson()))
            .peek(sku -> sku.setSpuId(spuId))
            .collect(Collectors.toList());
        if (!skus.isEmpty())
        {
            product.setSkus(skus);
        }
        product.setLatestReviewId(review.getReviewId());
        product.setLatestReviewNo(review.getReviewNo());
        product.setLatestReviewStatus(review.getReviewStatus());
        product.setLatestReviewFeedback(StringUtils.defaultString(review.getReviewReason()));
        product.setLatestReviewTime(review.getReviewTime());
        return product;
    }

    @Override
    @Transactional
    public int submitNewProductReview(Long spuId)
    {
        ProductSpu product = productDistributionService.selectProductById(spuId);
        if (!STATUS_DRAFT.equals(product.getSpuStatus()))
        {
            throw new ServiceException("只有草稿商品可以提交商品审核");
        }
        if (!CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(product.getControlStatus(), CONTROL_NORMAL)))
        {
            throw new ServiceException("停用商品不能提交审核");
        }
        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty())
        {
            throw new ServiceException("商品审核前至少需要一个 SKU");
        }
        String activePendingKey = activePendingKey(product.getSpuId());
        if (productReviewMapper.countPendingReviewByKey(activePendingKey) > 0)
        {
            throw new ServiceException("当前商品已存在待审核单");
        }

        String operator = currentUsername();
        ProductReviewRequest review = buildNewProductReview(product, skus, operator, activePendingKey);
        productReviewMapper.insertReview(review);
        insertReviewItemsAndSnapshots(review, product, skus);
        insertReviewLog(review, OP_SUBMIT, "", REVIEW_STATUS_PENDING, operator, "");
        return 1;
    }

    @Override
    @Transactional
    public int submitProductEditReview(ProductSpu submittedProduct)
    {
        if (submittedProduct == null || submittedProduct.getSpuId() == null)
        {
            throw new ServiceException("商品不存在");
        }
        ProductSpu before = productDistributionService.selectProductById(submittedProduct.getSpuId());
        if (STATUS_DRAFT.equals(before.getSpuStatus()))
        {
            throw new ServiceException("草稿商品请直接保存，不需要提交编辑审核");
        }
        if (!CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(before.getControlStatus(), CONTROL_NORMAL)))
        {
            throw new ServiceException("停用商品不能提交编辑审核");
        }
        String activePendingKey = activePendingKey(before.getSpuId());
        if (productReviewMapper.countPendingReviewByKey(activePendingKey) > 0)
        {
            throw new ServiceException("当前商品已存在待审核单");
        }

        ProductSpu after = productDistributionService.prepareReviewedProductUpdate(submittedProduct);
        String operator = currentUsername();
        ProductReviewRequest review = buildProductEditReview(before, after, operator, activePendingKey);
        productReviewMapper.insertReview(review);
        insertEditReviewItemsAndSnapshots(review, before, after);
        insertReviewLog(review, OP_SUBMIT, before.getSpuStatus(), REVIEW_STATUS_PENDING, operator, "");
        return 1;
    }

    @Override
    @Transactional
    public int submitSkuSalePriceReview(ProductSkuSalePriceUpdateRequest request)
    {
        if (request == null || request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("请选择需要调价的 SKU");
        }
        Map<Long, List<ProductSkuSalePriceUpdateRequest.Item>> itemsBySpu = new LinkedHashMap<>();
        Map<Long, ProductSku> skuMap = new HashMap<>();
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
            validateReviewPrice(item.getSalePrice());
            ProductSpu product = productDistributionService.selectProductById(sku.getSpuId());
            if (STATUS_DRAFT.equals(product.getSpuStatus()))
            {
                throw new ServiceException("草稿商品请在商品编辑页直接维护销售价");
            }
            if (!CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(product.getControlStatus(), CONTROL_NORMAL))
                || !CONTROL_NORMAL.equals(StringUtils.defaultIfBlank(sku.getControlStatus(), CONTROL_NORMAL)))
            {
                throw new ServiceException("停用商品或 SKU 不能提交调价审核");
            }
            itemsBySpu.computeIfAbsent(product.getSpuId(), key -> new ArrayList<>()).add(item);
            skuMap.put(sku.getSkuId(), sku);
        }

        String operator = currentUsername();
        int rows = 0;
        for (Map.Entry<Long, List<ProductSkuSalePriceUpdateRequest.Item>> entry : itemsBySpu.entrySet())
        {
            ProductSpu product = productDistributionService.selectProductById(entry.getKey());
            String activePendingKey = activePendingKey(product.getSpuId());
            if (productReviewMapper.countPendingReviewByKey(activePendingKey) > 0)
            {
                throw new ServiceException("当前商品已存在待审核单：" + product.getSystemSpuCode());
            }
            ProductReviewRequest review = buildSkuSalePriceReview(product, entry.getValue(), skuMap, operator,
                activePendingKey, request.getReason());
            productReviewMapper.insertReview(review);
            insertSkuSalePriceReviewItemsAndSnapshots(review, entry.getValue(), skuMap);
            insertReviewLog(review, OP_SUBMIT, product.getSpuStatus(), REVIEW_STATUS_PENDING, operator,
                StringUtils.defaultString(request.getReason()));
            rows++;
        }
        return rows;
    }

    @Override
    @Transactional
    public int approveReview(Long reviewId, String reason)
    {
        ProductReviewRequest review = requirePendingReview(reviewId);
        String operator = currentUsername();
        if (REVIEW_TYPE_NEW_PRODUCT.equals(review.getReviewType()))
        {
            approveNewProduct(review, operator);
        }
        else if (REVIEW_TYPE_ADD_SKU.equals(review.getReviewType()))
        {
            if (hasAfterProductSnapshot(review.getReviewId()))
            {
                approveProductEdit(review, operator);
            }
            else
            {
                approveAddSku(review, operator);
            }
        }
        else if (REVIEW_TYPE_EDIT_PRODUCT_INFO.equals(review.getReviewType())
            || REVIEW_TYPE_EDIT_SKU_INFO.equals(review.getReviewType()))
        {
            approveProductEdit(review, operator);
        }
        else if (REVIEW_TYPE_EDIT_PRICE.equals(review.getReviewType()))
        {
            approveSkuSalePriceEdit(review, operator);
        }
        else
        {
            throw new ServiceException("当前审核类型的生效逻辑尚未实现：" + review.getReviewType());
        }
        completeReview(review, REVIEW_STATUS_APPROVED, reason, operator);
        insertReviewLog(review, OP_APPROVE, REVIEW_STATUS_PENDING, REVIEW_STATUS_APPROVED, operator, reason);
        return 1;
    }

    @Override
    @Transactional
    public int rejectReview(Long reviewId, String reason)
    {
        ProductReviewRequest review = requirePendingReview(reviewId);
        String normalizedReason = requireTrim(reason, "请填写驳回原因");
        String operator = currentUsername();
        completeReview(review, REVIEW_STATUS_REJECTED, normalizedReason, operator);
        insertReviewLog(review, OP_REJECT, REVIEW_STATUS_PENDING, REVIEW_STATUS_REJECTED, operator, normalizedReason);
        return 1;
    }

    @Override
    public List<ProductReviewOperationLog> selectReviewOperationLogs(Long reviewId)
    {
        requireReview(reviewId);
        return productReviewMapper.selectReviewOperationLogs(reviewId);
    }

    private ProductReviewRequest buildNewProductReview(ProductSpu product, List<ProductSku> skus, String operator,
        String activePendingKey)
    {
        ProductReviewRequest review = new ProductReviewRequest();
        review.setReviewNo(generateReviewNo());
        review.setReviewType(REVIEW_TYPE_NEW_PRODUCT);
        review.setReviewStatus(REVIEW_STATUS_PENDING);
        review.setSpuId(product.getSpuId());
        review.setSystemSpuCode(StringUtils.defaultString(product.getSystemSpuCode()));
        review.setSellerId(product.getSellerId());
        review.setSellerName(StringUtils.defaultString(product.getSellerName()));
        review.setCategoryId(product.getCategoryId());
        review.setCategoryName(StringUtils.defaultString(product.getCategoryName()));
        review.setProductNameBefore("");
        review.setProductNameAfter(StringUtils.defaultString(product.getProductName()));
        review.setMainImageUrlBefore("");
        review.setMainImageUrlAfter(StringUtils.defaultString(product.getMainImageUrl()));
        review.setSubmitTerminal(TERMINAL_ADMIN);
        Long currentAdminUserId = currentUserId();
        // 管理端没有独立主体表，审核审计用当前 sys_user 作为主体和账号锚点。
        review.setSubmitSubjectId(currentAdminUserId);
        review.setSubmitAccountId(currentAdminUserId);
        review.setSubmitUserName(operator);
        review.setSubmitTime(DateUtils.getNowDate());
        review.setRiskLevel(resolveRiskLevel(product, skus));
        review.setRiskSummary(resolveRiskSummary(product, skus));
        review.setSkuCount(skus.size());
        review.setItemCount(skus.size() + 1);
        review.setPriceAfterMin(product.getSalePriceMin());
        review.setPriceAfterMax(product.getSalePriceMax());
        review.setCurrencySummary(StringUtils.defaultString(product.getCurrencySummary()));
        review.setWarehouseSummary(StringUtils.defaultString(product.getWarehouseKindSummary()));
        review.setDiffSummary("新增商品：" + StringUtils.defaultString(product.getProductName()));
        review.setActivePendingKey(activePendingKey);
        review.setCreateBy(operator);
        review.setUpdateBy(operator);
        return review;
    }

    private ProductReviewRequest buildProductEditReview(ProductSpu before, ProductSpu after, String operator,
        String activePendingKey)
    {
        ProductReviewRequest review = new ProductReviewRequest();
        review.setReviewNo(generateReviewNo());
        review.setReviewType(resolveEditReviewType(before, after));
        review.setReviewStatus(REVIEW_STATUS_PENDING);
        review.setSpuId(before.getSpuId());
        review.setSystemSpuCode(StringUtils.defaultString(before.getSystemSpuCode()));
        review.setSellerId(before.getSellerId());
        review.setSellerName(StringUtils.defaultString(before.getSellerName()));
        review.setCategoryId(after.getCategoryId());
        review.setCategoryName(StringUtils.defaultString(after.getCategoryName()));
        review.setProductNameBefore(StringUtils.defaultString(before.getProductName()));
        review.setProductNameAfter(StringUtils.defaultString(after.getProductName()));
        review.setMainImageUrlBefore(StringUtils.defaultString(before.getMainImageUrl()));
        review.setMainImageUrlAfter(StringUtils.defaultString(after.getMainImageUrl()));
        review.setSubmitTerminal(TERMINAL_ADMIN);
        Long currentAdminUserId = currentUserId();
        review.setSubmitSubjectId(currentAdminUserId);
        review.setSubmitAccountId(currentAdminUserId);
        review.setSubmitUserName(operator);
        review.setSubmitTime(DateUtils.getNowDate());
        review.setRiskLevel(resolveRiskLevel(after, safeSkus(after)));
        review.setRiskSummary(resolveRiskSummary(after, safeSkus(after)));
        review.setSkuCount(safeSkus(after).size());
        review.setItemCount(safeSkus(after).size() + 1);
        review.setPriceBeforeMin(minSalePrice(before));
        review.setPriceBeforeMax(maxSalePrice(before));
        review.setPriceAfterMin(minSalePrice(after));
        review.setPriceAfterMax(maxSalePrice(after));
        review.setCurrencySummary(resolveCurrencySummary(after));
        review.setWarehouseSummary(resolveWarehouseSummary(after));
        review.setDiffSummary(buildEditDiffSummary(before, after));
        review.setActivePendingKey(activePendingKey);
        review.setCreateBy(operator);
        review.setUpdateBy(operator);
        return review;
    }

    private ProductReviewRequest buildSkuSalePriceReview(ProductSpu product,
        List<ProductSkuSalePriceUpdateRequest.Item> items, Map<Long, ProductSku> skuMap, String operator,
        String activePendingKey, String reason)
    {
        ProductReviewRequest review = new ProductReviewRequest();
        review.setReviewNo(generateReviewNo());
        review.setReviewType(REVIEW_TYPE_EDIT_PRICE);
        review.setReviewStatus(REVIEW_STATUS_PENDING);
        review.setSpuId(product.getSpuId());
        review.setSystemSpuCode(StringUtils.defaultString(product.getSystemSpuCode()));
        review.setSellerId(product.getSellerId());
        review.setSellerName(StringUtils.defaultString(product.getSellerName()));
        review.setCategoryId(product.getCategoryId());
        review.setCategoryName(StringUtils.defaultString(product.getCategoryName()));
        review.setProductNameBefore(StringUtils.defaultString(product.getProductName()));
        review.setProductNameAfter(StringUtils.defaultString(product.getProductName()));
        review.setMainImageUrlBefore(StringUtils.defaultString(product.getMainImageUrl()));
        review.setMainImageUrlAfter(StringUtils.defaultString(product.getMainImageUrl()));
        review.setSubmitTerminal(TERMINAL_ADMIN);
        Long currentAdminUserId = currentUserId();
        review.setSubmitSubjectId(currentAdminUserId);
        review.setSubmitAccountId(currentAdminUserId);
        review.setSubmitUserName(operator);
        review.setSubmitTime(DateUtils.getNowDate());
        review.setRiskLevel("LOW");
        review.setRiskSummary("");
        review.setSkuCount(items.size());
        review.setItemCount(items.size());
        review.setPriceBeforeMin(items.stream().map(item -> skuMap.get(item.getSkuId()).getSalePrice())
            .filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null));
        review.setPriceBeforeMax(items.stream().map(item -> skuMap.get(item.getSkuId()).getSalePrice())
            .filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
        review.setPriceAfterMin(items.stream().map(ProductSkuSalePriceUpdateRequest.Item::getSalePrice)
            .filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null));
        review.setPriceAfterMax(items.stream().map(ProductSkuSalePriceUpdateRequest.Item::getSalePrice)
            .filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
        review.setCurrencySummary(resolveCurrencySummary(product));
        review.setWarehouseSummary(resolveWarehouseSummary(product));
        review.setDiffSummary("SKU销售价调整：" + items.size() + " 个SKU");
        review.setActivePendingKey(activePendingKey);
        review.setCreateBy(operator);
        review.setUpdateBy(operator);
        review.setRemark(StringUtils.defaultString(reason));
        return review;
    }

    private void insertReviewItemsAndSnapshots(ProductReviewRequest review, ProductSpu product, List<ProductSku> skus)
    {
        ProductReviewItem spuItem = newReviewItem(review, ITEM_TYPE_SPU, CHANGE_CREATE, product.getSpuId(), null,
            "", "", 0, "新增SPU");
        productReviewMapper.insertReviewItem(spuItem);
        insertSnapshot(review.getReviewId(), spuItem.getItemId(), SNAPSHOT_AFTER, PAYLOAD_SPU, snapshotProductFull(product));

        int index = 1;
        for (ProductSku sku : skus)
        {
            ProductReviewItem skuItem = newReviewItem(review, ITEM_TYPE_SKU, CHANGE_CREATE, product.getSpuId(),
                sku.getSkuId(), StringUtils.defaultString(sku.getSystemSkuCode()),
                StringUtils.defaultString(sku.getSellerSkuCode()), index++, "新增SKU");
            productReviewMapper.insertReviewItem(skuItem);
            insertSnapshot(review.getReviewId(), skuItem.getItemId(), SNAPSHOT_AFTER, PAYLOAD_SKU, snapshotSkuFull(sku));
        }
    }

    private void insertEditReviewItemsAndSnapshots(ProductReviewRequest review, ProductSpu before, ProductSpu after)
    {
        String beforeProductJson = snapshotProductFull(before);
        String afterProductJson = snapshotProductFull(after);
        ProductReviewItem spuItem = newReviewItem(review, ITEM_TYPE_SPU, CHANGE_UPDATE, before.getSpuId(), null,
            "", "", 0, "商品资料变更");
        spuItem.setBeforeHash(sha256(beforeProductJson));
        spuItem.setAfterHash(sha256(afterProductJson));
        productReviewMapper.insertReviewItem(spuItem);
        insertSnapshot(review.getReviewId(), spuItem.getItemId(), SNAPSHOT_BEFORE, PAYLOAD_SPU, beforeProductJson);
        insertSnapshot(review.getReviewId(), spuItem.getItemId(), SNAPSHOT_AFTER, PAYLOAD_SPU, afterProductJson);

        Map<Long, ProductSku> beforeSkuMap = safeSkus(before).stream()
            .filter(sku -> sku.getSkuId() != null)
            .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku, (a, b) -> a));
        int index = 1;
        for (ProductSku afterSku : safeSkus(after))
        {
            ProductSku beforeSku = afterSku.getSkuId() == null ? null : beforeSkuMap.get(afterSku.getSkuId());
            String beforeSkuJson = beforeSku == null ? "" : snapshotSkuFull(beforeSku);
            String afterSkuJson = snapshotSkuFull(afterSku);
            ProductReviewItem skuItem = newReviewItem(review, ITEM_TYPE_SKU,
                beforeSku == null ? CHANGE_CREATE : CHANGE_UPDATE, before.getSpuId(), afterSku.getSkuId(),
                StringUtils.defaultString(afterSku.getSystemSkuCode()),
                StringUtils.defaultString(afterSku.getSellerSkuCode()), index++,
                beforeSku == null ? "新增SKU" : "SKU资料变更");
            skuItem.setBeforeHash(sha256(beforeSkuJson));
            skuItem.setAfterHash(sha256(afterSkuJson));
            productReviewMapper.insertReviewItem(skuItem);
            if (beforeSku != null)
            {
                insertSnapshot(review.getReviewId(), skuItem.getItemId(), SNAPSHOT_BEFORE, PAYLOAD_SKU, beforeSkuJson);
            }
            insertSnapshot(review.getReviewId(), skuItem.getItemId(), SNAPSHOT_AFTER, PAYLOAD_SKU, afterSkuJson);
        }
    }

    private void insertSkuSalePriceReviewItemsAndSnapshots(ProductReviewRequest review,
        List<ProductSkuSalePriceUpdateRequest.Item> items, Map<Long, ProductSku> skuMap)
    {
        int index = 0;
        for (ProductSkuSalePriceUpdateRequest.Item priceItem : items)
        {
            ProductSku beforeSku = skuMap.get(priceItem.getSkuId());
            ProductSku afterSku = copySkuForPriceReview(beforeSku, priceItem.getSalePrice());
            String beforeSkuJson = snapshotSkuFull(beforeSku);
            String afterSkuJson = snapshotSkuFull(afterSku);
            ProductReviewItem skuItem = newReviewItem(review, ITEM_TYPE_SKU, CHANGE_UPDATE, review.getSpuId(),
                beforeSku.getSkuId(), StringUtils.defaultString(beforeSku.getSystemSkuCode()),
                StringUtils.defaultString(beforeSku.getSellerSkuCode()), index++,
                "销售价：" + beforeSku.getSalePrice() + " -> " + priceItem.getSalePrice());
            skuItem.setBeforeHash(sha256(beforeSkuJson));
            skuItem.setAfterHash(sha256(afterSkuJson));
            productReviewMapper.insertReviewItem(skuItem);
            insertSnapshot(review.getReviewId(), skuItem.getItemId(), SNAPSHOT_BEFORE, PAYLOAD_SKU, beforeSkuJson);
            insertSnapshot(review.getReviewId(), skuItem.getItemId(), SNAPSHOT_AFTER, PAYLOAD_SKU, afterSkuJson);
        }
    }

    private ProductSku copySkuForPriceReview(ProductSku beforeSku, BigDecimal salePrice)
    {
        ProductSku afterSku = JSON.parseObject(JSON.toJSONString(beforeSku), ProductSku.class);
        afterSku.setSalePrice(salePrice);
        return afterSku;
    }

    private ProductReviewItem newReviewItem(ProductReviewRequest review, String itemType, String changeType, Long spuId,
        Long skuId, String systemSkuCode, String sellerSkuCode, int sortOrder, String diffSummary)
    {
        ProductReviewItem item = new ProductReviewItem();
        item.setReviewId(review.getReviewId());
        item.setItemType(itemType);
        item.setChangeType(changeType);
        item.setSpuId(spuId);
        item.setSkuId(skuId);
        item.setSystemSkuCode(systemSkuCode);
        item.setSellerSkuCode(sellerSkuCode);
        item.setItemStatus(ITEM_STATUS_PENDING);
        item.setBeforeHash("");
        item.setAfterHash("");
        item.setDiffSummary(diffSummary);
        item.setRiskSummary("");
        item.setSortOrder(sortOrder);
        return item;
    }

    private void insertSnapshot(Long reviewId, Long itemId, String snapshotRole, String payloadType, String payloadJson)
    {
        ProductReviewSnapshot snapshot = new ProductReviewSnapshot();
        snapshot.setReviewId(reviewId);
        snapshot.setItemId(itemId);
        snapshot.setSnapshotRole(snapshotRole);
        snapshot.setPayloadType(payloadType);
        snapshot.setPayloadJson(payloadJson);
        snapshot.setPayloadHash(sha256(payloadJson));
        productReviewMapper.insertReviewSnapshot(snapshot);
    }

    private void approveNewProduct(ProductReviewRequest review, String operator)
    {
        ProductSpu product = productDistributionService.selectProductById(review.getSpuId());
        if (!STATUS_DRAFT.equals(product.getSpuStatus()))
        {
            throw new ServiceException("商品当前状态不是草稿，不能按新增商品审核通过");
        }
        productDistributionMapper.updateSpuStatus(review.getSpuId(), STATUS_READY, operator);
        recordDistributionReviewStatusLog(product, null, STATUS_READY, "商品审核通过");
        approveDraftSkuItems(review, product, operator);
    }

    private void approveAddSku(ProductReviewRequest review, String operator)
    {
        ProductSpu product = productDistributionService.selectProductById(review.getSpuId());
        approveDraftSkuItems(review, product, operator);
    }

    private void approveProductEdit(ProductReviewRequest review, String operator)
    {
        ProductSpu current = productDistributionService.selectProductById(review.getSpuId());
        List<ProductReviewSnapshot> snapshots = productReviewMapper.selectReviewSnapshots(review.getReviewId());
        ProductReviewSnapshot beforeSnapshot = findProductSnapshot(snapshots, SNAPSHOT_BEFORE);
        ProductReviewSnapshot afterSnapshot = findProductSnapshot(snapshots, SNAPSHOT_AFTER);
        if (afterSnapshot == null)
        {
            throw new ServiceException("审核单缺少商品编辑生效快照");
        }
        if (beforeSnapshot != null)
        {
            String currentHash = sha256(snapshotProductFull(current));
            if (!StringUtils.equals(currentHash, beforeSnapshot.getPayloadHash()))
            {
                throw new ServiceException("商品正式数据已变化，请重新提交审核");
            }
        }
        ProductSpu after = parseProductSnapshot(afterSnapshot.getPayloadJson());
        productDistributionService.applyReviewedProductUpdate(after);
        recordDistributionReviewEditLog(current, review, operator);
    }

    private void approveSkuSalePriceEdit(ProductReviewRequest review, String operator)
    {
        ProductSpu product = productDistributionService.selectProductById(review.getSpuId());
        List<ProductReviewItem> items = productReviewMapper.selectReviewItems(review.getReviewId());
        List<ProductReviewSnapshot> snapshots = productReviewMapper.selectReviewSnapshots(review.getReviewId());
        Map<Long, ProductReviewSnapshot> beforeSnapshotsByItemId = snapshots.stream()
            .filter(snapshot -> SNAPSHOT_BEFORE.equals(snapshot.getSnapshotRole()) && PAYLOAD_SKU.equals(snapshot.getPayloadType()))
            .collect(Collectors.toMap(ProductReviewSnapshot::getItemId, snapshot -> snapshot, (a, b) -> a));
        Map<Long, ProductReviewSnapshot> afterSnapshotsByItemId = snapshots.stream()
            .filter(snapshot -> SNAPSHOT_AFTER.equals(snapshot.getSnapshotRole()) && PAYLOAD_SKU.equals(snapshot.getPayloadType()))
            .collect(Collectors.toMap(ProductReviewSnapshot::getItemId, snapshot -> snapshot, (a, b) -> a));
        for (ProductReviewItem item : items)
        {
            if (!ITEM_TYPE_SKU.equals(item.getItemType()))
            {
                continue;
            }
            ProductReviewSnapshot afterSnapshot = afterSnapshotsByItemId.get(item.getItemId());
            if (afterSnapshot == null)
            {
                throw new ServiceException("调价审核缺少 SKU 快照");
            }
            ProductReviewSnapshot beforeSnapshot = beforeSnapshotsByItemId.get(item.getItemId());
            if (beforeSnapshot == null)
            {
                throw new ServiceException("调价审核缺少 SKU 生效前快照");
            }
            ProductSku afterSku = JSON.parseObject(afterSnapshot.getPayloadJson(), ProductSku.class);
            validateReviewPrice(afterSku.getSalePrice());
            ProductSku currentSku = productDistributionMapper.selectSkuById(afterSku.getSkuId());
            if (currentSku == null || !product.getSpuId().equals(currentSku.getSpuId()))
            {
                throw new ServiceException("调价 SKU 不存在：" + afterSku.getSkuId());
            }
            String currentHash = sha256(snapshotSkuFull(currentSku));
            if (!StringUtils.equals(currentHash, beforeSnapshot.getPayloadHash()))
            {
                throw new ServiceException("SKU 正式数据已变化，请重新提交审核");
            }
            productDistributionMapper.updateSkuSalePrice(afterSku.getSkuId(), afterSku.getSalePrice(), operator);
            recordDistributionReviewPriceLog(product, currentSku, afterSku.getSalePrice(), review, operator);
        }
    }

    private void approveDraftSkuItems(ProductReviewRequest review, ProductSpu product, String operator)
    {
        List<ProductReviewItem> items = productReviewMapper.selectReviewItems(review.getReviewId());
        for (ProductReviewItem item : items)
        {
            if (!ITEM_TYPE_SKU.equals(item.getItemType()) || item.getSkuId() == null)
            {
                continue;
            }
            ProductSku sku = productDistributionMapper.selectSkuById(item.getSkuId());
            if (sku == null || !product.getSpuId().equals(sku.getSpuId()))
            {
                throw new ServiceException("审核 SKU 不存在：" + item.getSkuId());
            }
            if (STATUS_DRAFT.equals(sku.getSkuStatus()))
            {
                productDistributionMapper.updateSkuStatus(sku.getSkuId(), STATUS_READY, operator);
                recordDistributionReviewStatusLog(product, sku, STATUS_READY, "商品审核通过");
            }
        }
    }

    private void completeReview(ProductReviewRequest review, String status, String reason, String operator)
    {
        review.setReviewStatus(status);
        review.setReviewerId(currentUserId());
        review.setReviewerName(operator);
        review.setReviewTime(new Date());
        review.setReviewReason(StringUtils.defaultString(reason));
        review.setUpdateBy(operator);
        productReviewMapper.updateReviewStatus(review);
        productReviewMapper.updateReviewItemsStatus(review.getReviewId(), status);
    }

    private void insertReviewLog(ProductReviewRequest review, String operationType, String beforeStatus,
        String afterStatus, String operator, String reason)
    {
        ProductReviewOperationLog log = new ProductReviewOperationLog();
        log.setReviewId(review.getReviewId());
        log.setSpuId(review.getSpuId());
        log.setOperationType(operationType);
        log.setBeforeStatus(StringUtils.defaultString(beforeStatus));
        log.setAfterStatus(afterStatus);
        log.setOperatorTerminal(TERMINAL_ADMIN);
        log.setOperatorId(currentUserId());
        log.setOperatorName(operator);
        log.setReason(StringUtils.defaultString(reason));
        productReviewMapper.insertReviewOperationLog(log);
    }

    private void recordDistributionReviewStatusLog(ProductSpu product, ProductSku sku, String targetStatus,
        String reason)
    {
        ProductDistributionOperationLog log = new ProductDistributionOperationLog();
        log.setBatchNo(generateBatchNo());
        log.setOperationType(DIST_OP_REVIEW_APPROVE);
        log.setOwnerType(sku == null ? ITEM_TYPE_SPU : ITEM_TYPE_SKU);
        log.setSpuId(product.getSpuId());
        log.setSkuId(sku == null ? null : sku.getSkuId());
        log.setSystemSpuCode(product.getSystemSpuCode());
        log.setSystemSkuCode(sku == null ? null : sku.getSystemSkuCode());
        log.setSellerId(product.getSellerId());
        log.setSellerName(product.getSellerName());
        log.setBeforeSalesStatus(sku == null ? product.getSpuStatus() : sku.getSkuStatus());
        log.setAfterSalesStatus(targetStatus);
        log.setReason(reason);
        log.setChangeSummary((sku == null ? "SPU" : "SKU") + "审核通过进入待上架");
        log.setDiffJson(simpleDiff(sku == null ? "spuStatus" : "skuStatus",
            sku == null ? product.getSpuStatus() : sku.getSkuStatus(), targetStatus));
        log.setOperatorName(currentUsername());
        log.setOperationSource(DIST_OP_SOURCE_REVIEW);
        operationLogMapper.insertOperationLog(log);
    }

    private void recordDistributionReviewEditLog(ProductSpu product, ProductReviewRequest review, String operator)
    {
        ProductDistributionOperationLog log = new ProductDistributionOperationLog();
        log.setBatchNo(generateBatchNo());
        log.setOperationType(DIST_OP_REVIEW_APPROVE);
        log.setOwnerType(ITEM_TYPE_SPU);
        log.setSpuId(product.getSpuId());
        log.setSystemSpuCode(product.getSystemSpuCode());
        log.setSellerId(product.getSellerId());
        log.setSellerName(product.getSellerName());
        log.setBeforeSalesStatus(product.getSpuStatus());
        log.setAfterSalesStatus(product.getSpuStatus());
        log.setReason("商品编辑审核通过");
        log.setChangeSummary("商品编辑审核通过生效：" + StringUtils.defaultString(review.getReviewNo()));
        log.setDiffJson("{\"reviewId\":" + review.getReviewId() + ",\"reviewType\":\""
            + escapeJson(review.getReviewType()) + "\",\"summary\":\"" + escapeJson(review.getDiffSummary()) + "\"}");
        log.setOperatorName(operator);
        log.setOperationSource(DIST_OP_SOURCE_REVIEW);
        operationLogMapper.insertOperationLog(log);
    }

    private void recordDistributionReviewPriceLog(ProductSpu product, ProductSku sku, BigDecimal targetPrice,
        ProductReviewRequest review, String operator)
    {
        ProductDistributionOperationLog log = new ProductDistributionOperationLog();
        log.setBatchNo(generateBatchNo());
        log.setOperationType(DIST_OP_REVIEW_APPROVE);
        log.setOwnerType(ITEM_TYPE_SKU);
        log.setSpuId(product.getSpuId());
        log.setSkuId(sku.getSkuId());
        log.setSystemSpuCode(product.getSystemSpuCode());
        log.setSystemSkuCode(sku.getSystemSkuCode());
        log.setSellerId(product.getSellerId());
        log.setSellerName(product.getSellerName());
        log.setBeforeSalePrice(sku.getSalePrice());
        log.setAfterSalePrice(targetPrice);
        log.setBeforeSalesStatus(sku.getSkuStatus());
        log.setAfterSalesStatus(sku.getSkuStatus());
        log.setReason("调价审核通过");
        log.setChangeSummary("SKU销售价审核通过：" + sku.getSalePrice() + " -> " + targetPrice);
        log.setDiffJson("{\"reviewId\":" + review.getReviewId() + ",\"reviewType\":\""
            + escapeJson(review.getReviewType()) + "\",\"before\":\"" + sku.getSalePrice()
            + "\",\"after\":\"" + targetPrice + "\"}");
        log.setOperatorName(operator);
        log.setOperationSource(DIST_OP_SOURCE_REVIEW);
        operationLogMapper.insertOperationLog(log);
    }

    private ProductReviewRequest requireReview(Long reviewId)
    {
        if (reviewId == null)
        {
            throw new ServiceException("审核单不存在");
        }
        ProductReviewRequest review = productReviewMapper.selectReviewById(reviewId);
        if (review == null)
        {
            throw new ServiceException("审核单不存在");
        }
        return review;
    }

    private ProductReviewRequest requirePendingReview(Long reviewId)
    {
        ProductReviewRequest review = requireReview(reviewId);
        if (!REVIEW_STATUS_PENDING.equals(review.getReviewStatus()))
        {
            throw new ServiceException("只有待审核单可以处理");
        }
        return review;
    }

    private String resolveRiskLevel(ProductSpu product, List<ProductSku> skus)
    {
        return StringUtils.isBlank(product.getMainImageUrl()) || skus.isEmpty() ? "MEDIUM" : "LOW";
    }

    private String resolveRiskSummary(ProductSpu product, List<ProductSku> skus)
    {
        StringBuilder summary = new StringBuilder();
        if (StringUtils.isBlank(product.getMainImageUrl()))
        {
            summary.append("缺少主图 ");
        }
        if (skus.isEmpty())
        {
            summary.append("缺少SKU ");
        }
        return StringUtils.trimToEmpty(summary.toString());
    }

    private String resolveEditReviewType(ProductSpu before, ProductSpu after)
    {
        boolean hasNewSku = safeSkus(after).stream().anyMatch(sku -> sku.getSkuId() == null);
        if (hasNewSku)
        {
            return REVIEW_TYPE_ADD_SKU;
        }
        boolean productChanged = !StringUtils.equals(productReviewPayload(before), productReviewPayload(after));
        boolean skuChanged = hasSkuReviewPayloadChanged(before, after);
        if (skuChanged && !productChanged)
        {
            return REVIEW_TYPE_EDIT_SKU_INFO;
        }
        return REVIEW_TYPE_EDIT_PRODUCT_INFO;
    }

    private boolean hasSkuReviewPayloadChanged(ProductSpu before, ProductSpu after)
    {
        Map<Long, ProductSku> beforeSkuMap = safeSkus(before).stream()
            .filter(sku -> sku.getSkuId() != null)
            .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku, (a, b) -> a));
        int submittedExistingSkuCount = 0;
        for (ProductSku afterSku : safeSkus(after))
        {
            if (afterSku.getSkuId() == null)
            {
                return true;
            }
            submittedExistingSkuCount++;
            ProductSku beforeSku = beforeSkuMap.get(afterSku.getSkuId());
            if (beforeSku == null || !StringUtils.equals(skuReviewPayload(beforeSku), skuReviewPayload(afterSku)))
            {
                return true;
            }
        }
        return submittedExistingSkuCount != beforeSkuMap.size();
    }

    private String productReviewPayload(ProductSpu product)
    {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sellerSpuCode", product.getSellerSpuCode());
        payload.put("sellerId", product.getSellerId());
        payload.put("categoryId", product.getCategoryId());
        payload.put("categoryName", product.getCategoryName());
        payload.put("productName", product.getProductName());
        payload.put("productNameEn", product.getProductNameEn());
        payload.put("mainImageUrl", product.getMainImageUrl());
        payload.put("detailContent", product.getDetailContent());
        payload.put("warehouseKind", product.getWarehouseKind());
        payload.put("warehouseIds", product.getWarehouseIds());
        payload.put("warehouses", warehouseReviewPayload(product));
        payload.put("attributeValues", product.getAttributeValues());
        payload.put("images", product.getImages());
        return JSON.toJSONString(payload);
    }

    private String skuReviewPayload(ProductSku sku)
    {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sellerSkuCode", sku.getSellerSkuCode());
        payload.put("color", sku.getColor());
        payload.put("size", sku.getSize());
        payload.put("lengthValue", sku.getLengthValue());
        payload.put("widthValue", sku.getWidthValue());
        payload.put("heightValue", sku.getHeightValue());
        payload.put("weight", sku.getWeight());
        payload.put("material", sku.getMaterial());
        payload.put("style", sku.getStyle());
        payload.put("model", sku.getModel());
        payload.put("packageQuantity", sku.getPackageQuantity());
        payload.put("capacity", sku.getCapacity());
        payload.put("skuImageUrl", sku.getSkuImageUrl());
        payload.put("supplyPrice", sku.getSupplyPrice());
        payload.put("salePrice", sku.getSalePrice());
        payload.put("currencyCode", sku.getCurrencyCode());
        payload.put("skuStatus", sku.getSkuStatus());
        payload.put("sourceDimensionGroupKey", sku.getSourceDimensionGroupKey());
        payload.put("sourceSkuGroupKey", sku.getSourceSkuGroupKey());
        payload.put("masterSku", sku.getMasterSku());
        payload.put("measureLengthCm", sku.getMeasureLengthCm());
        payload.put("measureWidthCm", sku.getMeasureWidthCm());
        payload.put("measureHeightCm", sku.getMeasureHeightCm());
        payload.put("measureWeightKg", sku.getMeasureWeightKg());
        return JSON.toJSONString(payload);
    }

    private List<Map<String, Object>> warehouseReviewPayload(ProductSpu product)
    {
        if (product == null || product.getWarehouses() == null)
        {
            return List.of();
        }
        return product.getWarehouses().stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ProductSpuWarehouse::getWarehouseId, Comparator.nullsLast(Long::compareTo)))
            .map(warehouse -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("warehouseId", warehouse.getWarehouseId());
                item.put("warehouseCode", warehouse.getWarehouseCode());
                item.put("warehouseName", warehouse.getWarehouseName());
                item.put("warehouseKind", warehouse.getWarehouseKind());
                item.put("settlementCurrency", warehouse.getSettlementCurrency());
                item.put("sellerId", warehouse.getSellerId());
                return item;
            })
            .collect(Collectors.toList());
    }

    private String buildEditDiffSummary(ProductSpu before, ProductSpu after)
    {
        List<String> parts = new ArrayList<>();
        if (!StringUtils.equals(before.getProductName(), after.getProductName()))
        {
            parts.add("商品标题");
        }
        if (!Objects.equals(before.getCategoryId(), after.getCategoryId()))
        {
            parts.add("商品分类");
        }
        if (!StringUtils.equals(before.getMainImageUrl(), after.getMainImageUrl()))
        {
            parts.add("主图");
        }
        if (!StringUtils.equals(productReviewPayload(before), productReviewPayload(after)))
        {
            parts.add("SPU资料");
        }
        if (hasSkuReviewPayloadChanged(before, after))
        {
            parts.add("SKU资料");
        }
        if (parts.isEmpty())
        {
            return "商品资料提交审核";
        }
        return "变更：" + StringUtils.join(parts, "、");
    }

    private List<ProductSku> safeSkus(ProductSpu product)
    {
        return product == null || product.getSkus() == null ? List.of() : product.getSkus();
    }

    private BigDecimal minSalePrice(ProductSpu product)
    {
        return safeSkus(product).stream().map(ProductSku::getSalePrice).filter(Objects::nonNull)
            .min(BigDecimal::compareTo).orElse(product == null ? null : product.getSalePriceMin());
    }

    private BigDecimal maxSalePrice(ProductSpu product)
    {
        return safeSkus(product).stream().map(ProductSku::getSalePrice).filter(Objects::nonNull)
            .max(BigDecimal::compareTo).orElse(product == null ? null : product.getSalePriceMax());
    }

    private String resolveCurrencySummary(ProductSpu product)
    {
        String currencies = safeSkus(product).stream().map(ProductSku::getCurrencyCode)
            .filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("/"));
        return StringUtils.defaultIfBlank(currencies, product == null ? "" : StringUtils.defaultString(product.getCurrencySummary()));
    }

    private String resolveWarehouseSummary(ProductSpu product)
    {
        if (product != null && product.getWarehouses() != null && !product.getWarehouses().isEmpty())
        {
            return product.getWarehouses().stream()
                .map(warehouse -> StringUtils.defaultIfBlank(warehouse.getWarehouseName(), warehouse.getWarehouseCode()))
                .filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("/"));
        }
        return product == null ? "" : StringUtils.defaultString(product.getWarehouseKindSummary());
    }

    private String snapshotProduct(ProductSpu product)
    {
        return "{"
            + jsonPair("spuId", product.getSpuId()) + ","
            + jsonPair("systemSpuCode", product.getSystemSpuCode()) + ","
            + jsonPair("productName", product.getProductName()) + ","
            + jsonPair("productNameEn", product.getProductNameEn()) + ","
            + jsonPair("sellerId", product.getSellerId()) + ","
            + jsonPair("sellerName", product.getSellerName()) + ","
            + jsonPair("categoryId", product.getCategoryId()) + ","
            + jsonPair("categoryName", product.getCategoryName()) + ","
            + jsonPair("mainImageUrl", product.getMainImageUrl()) + ","
            + jsonPair("spuStatus", product.getSpuStatus())
            + "}";
    }

    private String snapshotSku(ProductSku sku)
    {
        return "{"
            + jsonPair("skuId", sku.getSkuId()) + ","
            + jsonPair("systemSkuCode", sku.getSystemSkuCode()) + ","
            + jsonPair("sellerSkuCode", sku.getSellerSkuCode()) + ","
            + jsonPair("skuStatus", sku.getSkuStatus()) + ","
            + jsonPair("supplyPrice", sku.getSupplyPrice()) + ","
            + jsonPair("salePrice", sku.getSalePrice()) + ","
            + jsonPair("currencyCode", sku.getCurrencyCode()) + ","
            + jsonPair("skuImageUrl", sku.getSkuImageUrl())
            + "}";
    }

    private String snapshotProductFull(ProductSpu product)
    {
        return JSON.toJSONString(product);
    }

    private String snapshotSkuFull(ProductSku sku)
    {
        return JSON.toJSONString(sku);
    }

    private ProductSpu parseProductSnapshot(String payloadJson)
    {
        try
        {
            return JSON.parseObject(requireTrim(payloadJson, "审核快照为空"), ProductSpu.class);
        }
        catch (RuntimeException e)
        {
            throw new ServiceException("审核SKU快照解析失败");
        }
    }

    private ProductSku parseSkuSnapshot(String payloadJson)
    {
        try
        {
            return JSON.parseObject(requireTrim(payloadJson, "审核快照为空"), ProductSku.class);
        }
        catch (RuntimeException e)
        {
            throw new ServiceException("审核快照解析失败");
        }
    }

    private ProductReviewSnapshot findProductSnapshot(List<ProductReviewSnapshot> snapshots, String role)
    {
        if (snapshots == null)
        {
            return null;
        }
        for (ProductReviewSnapshot snapshot : snapshots)
        {
            if (SNAPSHOT_AFTER.equals(role) || SNAPSHOT_BEFORE.equals(role))
            {
                if (role.equals(snapshot.getSnapshotRole()) && PAYLOAD_SPU.equals(snapshot.getPayloadType()))
                {
                    return snapshot;
                }
            }
        }
        return null;
    }

    private boolean hasAfterProductSnapshot(Long reviewId)
    {
        return findProductSnapshot(productReviewMapper.selectReviewSnapshots(reviewId), SNAPSHOT_AFTER) != null;
    }

    private String jsonPair(String field, Object value)
    {
        if (value == null)
        {
            return "\"" + field + "\":null";
        }
        if (value instanceof Number)
        {
            return "\"" + field + "\":" + value;
        }
        return "\"" + field + "\":\"" + escapeJson(String.valueOf(value)) + "\"";
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

    private String sha256(String value)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes)
            {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ServiceException("审核快照签名失败");
        }
    }

    private String activePendingKey(Long spuId)
    {
        return "SPU:" + spuId;
    }

    private String generateReviewNo()
    {
        return "PRV" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateBatchNo()
    {
        return "PLOG" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + UUID.randomUUID().toString().replace("-", "");
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

    private void validateReviewPrice(BigDecimal price)
    {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("SKU 销售价不能为空且不能小于 0");
        }
    }

    private String currentUsername()
    {
        return SecurityUtils.getUsername();
    }

    private Long currentUserId()
    {
        return SecurityUtils.getUserId();
    }
}
