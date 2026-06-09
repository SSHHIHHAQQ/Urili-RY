package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.alibaba.fastjson2.JSON;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.domain.ProductDistributionOperationLog;
import com.ruoyi.product.domain.ProductReviewItem;
import com.ruoyi.product.domain.ProductReviewOperationLog;
import com.ruoyi.product.domain.ProductReviewRequest;
import com.ruoyi.product.domain.ProductReviewSnapshot;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSkuSupplyPriceUpdateRequest;
import com.ruoyi.product.domain.ProductSpu;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.product.mapper.ProductDistributionOperationLogMapper;
import com.ruoyi.product.mapper.ProductReviewMapper;
import com.ruoyi.product.service.IProductDistributionService;

public class ProductReviewServiceImplTest
{
    @After
    public void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void submitNewProductReviewRejectsDuplicatePendingReviewBeforeInsert() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        reviewMapper.pendingReviewCount = 1;
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = draftProduct();
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                new RecordingProductDistributionMapper().proxy(), new RecordingOperationLogMapper().proxy());

        try
        {
            service.submitNewProductReview(10L);
        }
        catch (ServiceException e)
        {
            assertEquals("当前商品已存在待审核单", e.getMessage());
            assertEquals("SPU:10", reviewMapper.pendingKey);
            assertEquals(0, reviewMapper.insertReviewCalls);
            assertTrue(reviewMapper.insertedItems.isEmpty());
            assertTrue(reviewMapper.insertedSnapshots.isEmpty());
            return;
        }
        fail("Expected duplicate pending review to be rejected");
    }

    @Test
    public void submitNewProductReviewCreatesStableAuditAnchorsItemsSnapshotsAndLog() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = draftProduct();
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                new RecordingProductDistributionMapper().proxy(), new RecordingOperationLogMapper().proxy());

        int rows = service.submitNewProductReview(10L);

        assertEquals(1, rows);
        assertEquals(Long.valueOf(10L), distributionService.selectedSpuId);
        assertNotNull(reviewMapper.insertedReview);
        assertEquals("PENDING", reviewMapper.insertedReview.getReviewStatus());
        assertEquals("ADMIN", reviewMapper.insertedReview.getSubmitTerminal());
        assertEquals(Long.valueOf(100L), reviewMapper.insertedReview.getSubmitSubjectId());
        assertEquals(Long.valueOf(100L), reviewMapper.insertedReview.getSubmitAccountId());
        assertEquals("admin", reviewMapper.insertedReview.getSubmitUserName());
        assertEquals(Integer.valueOf(2), reviewMapper.insertedReview.getItemCount());
        assertEquals(Integer.valueOf(1), reviewMapper.insertedReview.getSkuCount());
        assertEquals("official", reviewMapper.insertedReview.getWarehouseSummary());
        assertEquals(new BigDecimal("10.00"), reviewMapper.insertedReview.getPriceAfterMin());
        assertEquals(new BigDecimal("10.00"), reviewMapper.insertedReview.getPriceAfterMax());
        assertEquals(2, reviewMapper.insertedItems.size());
        assertEquals(2, reviewMapper.insertedSnapshots.size());
        assertEquals(1, reviewMapper.insertedOperationLogs.size());
        ProductReviewOperationLog log = reviewMapper.insertedOperationLogs.get(0);
        assertEquals("SUBMIT", log.getOperationType());
        assertEquals("PENDING", log.getAfterStatus());
        assertEquals("ADMIN", log.getOperatorTerminal());
        assertEquals(Long.valueOf(100L), log.getOperatorId());
        assertEquals("admin", log.getOperatorName());
    }

    @Test
    public void approveReviewMovesDraftProductToReadyAndWritesReviewerAndLogs() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        reviewMapper.reviewById = pendingNewProductReview();
        reviewMapper.reviewItems = List.of(skuReviewItem());
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = draftProduct();
        RecordingProductDistributionMapper distributionMapper = new RecordingProductDistributionMapper();
        distributionMapper.skuByIdResult = draftSku();
        RecordingOperationLogMapper operationLogMapper = new RecordingOperationLogMapper();
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                distributionMapper.proxy(), operationLogMapper.proxy());

        int rows = service.approveReview(900L, "审核通过");

        assertEquals(1, rows);
        assertEquals(Long.valueOf(10L), distributionMapper.updateSpuStatusId);
        assertEquals("READY", distributionMapper.updateSpuStatusValue);
        assertEquals("admin", distributionMapper.updateSpuStatusBy);
        assertEquals(Long.valueOf(100L), distributionMapper.updateSkuStatusId);
        assertEquals("READY", distributionMapper.updateSkuStatusValue);
        assertEquals("admin", distributionMapper.updateSkuStatusBy);
        assertSame(reviewMapper.reviewById, reviewMapper.updatedReview);
        assertEquals("APPROVED", reviewMapper.updatedReview.getReviewStatus());
        assertEquals(Long.valueOf(100L), reviewMapper.updatedReview.getReviewerId());
        assertEquals("admin", reviewMapper.updatedReview.getReviewerName());
        assertEquals("审核通过", reviewMapper.updatedReview.getReviewReason());
        assertEquals(Long.valueOf(900L), reviewMapper.updatedItemsReviewId);
        assertEquals("APPROVED", reviewMapper.updatedItemsStatus);
        assertEquals(1, reviewMapper.insertedOperationLogs.size());
        assertEquals("APPROVE", reviewMapper.insertedOperationLogs.get(0).getOperationType());
        assertEquals("PENDING", reviewMapper.insertedOperationLogs.get(0).getBeforeStatus());
        assertEquals("APPROVED", reviewMapper.insertedOperationLogs.get(0).getAfterStatus());
        assertEquals(2, operationLogMapper.insertedLogs.size());
    }

    @Test
    public void submitProductEditReviewStoresBeforeAndAfterSnapshotsWithoutSavingLiveProduct() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = readyProduct("正式商品");
        distributionService.preparedProduct = readyProduct("修改后商品");
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                new RecordingProductDistributionMapper().proxy(), new RecordingOperationLogMapper().proxy());

        int rows = service.submitProductEditReview(readyProduct("修改后商品"));

        assertEquals(1, rows);
        assertEquals(Long.valueOf(10L), distributionService.selectedSpuId);
        assertNotNull(distributionService.prepareReviewedProductUpdateArg);
        assertEquals("EDIT_PRODUCT_INFO", reviewMapper.insertedReview.getReviewType());
        assertEquals("PENDING", reviewMapper.insertedReview.getReviewStatus());
        assertEquals("正式商品", reviewMapper.insertedReview.getProductNameBefore());
        assertEquals("修改后商品", reviewMapper.insertedReview.getProductNameAfter());
        assertEquals(2, reviewMapper.insertedItems.size());
        assertEquals(4, reviewMapper.insertedSnapshots.size());
        assertEquals(1, reviewMapper.insertedOperationLogs.size());
        assertEquals("SUBMIT", reviewMapper.insertedOperationLogs.get(0).getOperationType());
    }

    @Test
    public void submitProductEditReviewClassifiesSupplyPriceOnlyChangeAsPriceReview() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = readyProduct("正式商品");
        ProductSpu supplyPriceChanged = readyProduct("正式商品");
        supplyPriceChanged.getSkus().get(0).setSupplyPrice(new BigDecimal("19.99"));
        distributionService.preparedProduct = supplyPriceChanged;
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                new RecordingProductDistributionMapper().proxy(), new RecordingOperationLogMapper().proxy());

        int rows = service.submitProductEditReview(supplyPriceChanged);

        assertEquals(1, rows);
        assertEquals("EDIT_PRICE", reviewMapper.insertedReview.getReviewType());
        assertEquals("SKU供货价调整：1 个SKU", reviewMapper.insertedReview.getDiffSummary());
        assertEquals(new BigDecimal("10.00"), reviewMapper.insertedReview.getPriceBeforeMin());
        assertEquals(new BigDecimal("19.99"), reviewMapper.insertedReview.getPriceAfterMin());
    }

    @Test
    public void approveProductEditReviewAppliesAfterSnapshotAndKeepsLiveStatus() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        reviewMapper.reviewById = pendingEditProductReview();
        ProductReviewSnapshot afterSnapshot = new ProductReviewSnapshot();
        afterSnapshot.setSnapshotRole("AFTER");
        afterSnapshot.setPayloadType("SPU");
        afterSnapshot.setPayloadJson(JSON.toJSONString(readyProduct("审核通过后的商品")));
        reviewMapper.reviewSnapshots = List.of(afterSnapshot);
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = readyProduct("正式商品");
        RecordingOperationLogMapper operationLogMapper = new RecordingOperationLogMapper();
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                new RecordingProductDistributionMapper().proxy(), operationLogMapper.proxy());

        int rows = service.approveReview(900L, "通过");

        assertEquals(1, rows);
        assertNotNull(distributionService.applyReviewedProductUpdateArg);
        assertEquals("审核通过后的商品", distributionService.applyReviewedProductUpdateArg.getProductName());
        assertEquals("APPROVED", reviewMapper.updatedReview.getReviewStatus());
        assertEquals(1, operationLogMapper.insertedLogs.size());
        assertEquals("REVIEW_APPROVE", operationLogMapper.insertedLogs.get(0).getOperationType());
    }

    @Test
    public void approvePriceReviewUpdatesSkuSupplyPriceAfterApproval() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        reviewMapper.reviewById = pendingPriceReview();
        ProductReviewItem item = skuReviewItem();
        reviewMapper.reviewItems = List.of(item);
        ProductSku beforeSku = draftSku();
        beforeSku.setSkuStatus("READY");
        ProductSku afterSku = draftSku();
        afterSku.setSkuStatus("READY");
        afterSku.setSupplyPrice(new BigDecimal("19.99"));
        ProductReviewSnapshot beforeSnapshot = skuSnapshot(item, "BEFORE", beforeSku);
        ProductReviewSnapshot afterSnapshot = new ProductReviewSnapshot();
        afterSnapshot.setItemId(item.getItemId());
        afterSnapshot.setSnapshotRole("AFTER");
        afterSnapshot.setPayloadType("SKU");
        afterSnapshot.setPayloadJson(JSON.toJSONString(afterSku));
        reviewMapper.reviewSnapshots = List.of(beforeSnapshot, afterSnapshot);
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = readyProduct("正式商品");
        RecordingProductDistributionMapper distributionMapper = new RecordingProductDistributionMapper();
        distributionMapper.skuByIdResult = beforeSku;
        RecordingOperationLogMapper operationLogMapper = new RecordingOperationLogMapper();
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                distributionMapper.proxy(), operationLogMapper.proxy());

        int rows = service.approveReview(900L, "通过");

        assertEquals(1, rows);
        assertEquals(Long.valueOf(100L), distributionMapper.updateSupplyPriceSkuId);
        assertEquals(new BigDecimal("19.99"), distributionMapper.updateSupplyPriceValue);
        assertEquals("admin", distributionMapper.updateSupplyPriceBy);
        assertEquals(null, distributionMapper.updateSalePriceSkuId);
        assertEquals(1, operationLogMapper.insertedLogs.size());
    }

    @Test
    public void approvePriceReviewRejectsWhenLiveSkuChangedAfterSubmit() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        reviewMapper.reviewById = pendingPriceReview();
        ProductReviewItem item = skuReviewItem();
        reviewMapper.reviewItems = List.of(item);
        ProductSku beforeSku = draftSku();
        beforeSku.setSkuStatus("READY");
        ProductSku afterSku = draftSku();
        afterSku.setSkuStatus("READY");
        afterSku.setSupplyPrice(new BigDecimal("19.99"));
        reviewMapper.reviewSnapshots = List.of(skuSnapshot(item, "BEFORE", beforeSku),
                skuSnapshot(item, "AFTER", afterSku));
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = readyProduct("正式商品");
        RecordingProductDistributionMapper distributionMapper = new RecordingProductDistributionMapper();
        ProductSku changedSku = draftSku();
        changedSku.setSkuStatus("READY");
        changedSku.setSupplyPrice(new BigDecimal("18.88"));
        distributionMapper.skuByIdResult = changedSku;
        RecordingOperationLogMapper operationLogMapper = new RecordingOperationLogMapper();
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                distributionMapper.proxy(), operationLogMapper.proxy());

        try
        {
            service.approveReview(900L, "通过");
        }
        catch (ServiceException e)
        {
            assertEquals("SKU 正式数据已变化，请重新提交审核", e.getMessage());
            assertEquals(null, distributionMapper.updateSupplyPriceSkuId);
            assertTrue(operationLogMapper.insertedLogs.isEmpty());
            return;
        }
        fail("Expected stale SKU price review to be rejected");
    }

    @Test
    public void submitPriceReviewCreatesPendingEditPriceReview() throws Exception
    {
        authenticateAdmin(100L, "admin");
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        RecordingProductDistributionService distributionService = new RecordingProductDistributionService();
        distributionService.productByIdResult = readyProduct("正式商品");
        RecordingProductDistributionMapper distributionMapper = new RecordingProductDistributionMapper();
        ProductSku sku = draftSku();
        sku.setSkuStatus("READY");
        distributionMapper.skuByIdResult = sku;
        ProductReviewServiceImpl service = service(reviewMapper.proxy(), distributionService.proxy(),
                distributionMapper.proxy(), new RecordingOperationLogMapper().proxy());
        ProductSkuSupplyPriceUpdateRequest request = new ProductSkuSupplyPriceUpdateRequest();
        ProductSkuSupplyPriceUpdateRequest.Item item = new ProductSkuSupplyPriceUpdateRequest.Item();
        item.setSkuId(100L);
        item.setSupplyPrice(new BigDecimal("19.99"));
        request.setItems(List.of(item));

        int rows = service.submitSkuSupplyPriceReview(request);

        assertEquals(1, rows);
        assertEquals("EDIT_PRICE", reviewMapper.insertedReview.getReviewType());
        assertEquals("PENDING", reviewMapper.insertedReview.getReviewStatus());
        assertEquals(1, reviewMapper.insertedItems.size());
        assertEquals(2, reviewMapper.insertedSnapshots.size());
    }

    @Test
    public void selectReviewByIdLoadsItemsAndSnapshotsWithoutOperationLogs() throws Exception
    {
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        ProductReviewRequest review = pendingNewProductReview();
        ProductReviewItem item = skuReviewItem();
        ProductReviewSnapshot snapshot = new ProductReviewSnapshot();
        reviewMapper.reviewById = review;
        reviewMapper.reviewItems = List.of(item);
        reviewMapper.reviewSnapshots = List.of(snapshot);
        ProductReviewServiceImpl service = service(reviewMapper.proxy(),
                new RecordingProductDistributionService().proxy(), new RecordingProductDistributionMapper().proxy(),
                new RecordingOperationLogMapper().proxy());

        ProductReviewRequest result = service.selectReviewById(900L);

        assertSame(review, result);
        assertSame(reviewMapper.reviewItems, result.getItems());
        assertSame(reviewMapper.reviewSnapshots, result.getSnapshots());
        assertEquals(0, reviewMapper.selectOperationLogsCalls);
    }

    @Test
    public void selectReviewByIdRecomputesSupplyPriceRangeForEveryReviewTypeFromSnapshots() throws Exception
    {
        assertReviewSupplyPriceRange("NEW_PRODUCT", new BigDecimal("10.00"), new BigDecimal("20.00"), null, null);
        assertReviewSupplyPriceRange("ADD_SKU", new BigDecimal("10.00"), new BigDecimal("10.00"), null, null);
        assertReviewSupplyPriceRange("EDIT_PRODUCT_INFO", new BigDecimal("10.00"), new BigDecimal("20.00"),
                new BigDecimal("8.00"), new BigDecimal("18.00"));
        assertReviewSupplyPriceRange("EDIT_SKU_INFO", new BigDecimal("10.00"), new BigDecimal("20.00"),
                new BigDecimal("8.00"), new BigDecimal("18.00"));
        assertReviewSupplyPriceRange("EDIT_PRICE", new BigDecimal("10.00"), new BigDecimal("20.00"),
                new BigDecimal("8.00"), new BigDecimal("18.00"));
    }

    @Test
    public void selectReviewListAlsoRecomputesSupplyPriceRangeFromSnapshots() throws Exception
    {
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        ProductReviewRequest legacyReview = legacyReviewWithSalePriceRange("NEW_PRODUCT");
        reviewMapper.reviewList = List.of(legacyReview);
        reviewMapper.reviewItems = List.of(reviewItem(901L, "CREATE"));
        ProductSku afterSku = reviewSku(100L, "10.00", "99.00");
        reviewMapper.reviewSnapshots = List.of(skuSnapshot(reviewMapper.reviewItems.get(0), "AFTER", afterSku));
        ProductReviewServiceImpl service = service(reviewMapper.proxy(),
                new RecordingProductDistributionService().proxy(), new RecordingProductDistributionMapper().proxy(),
                new RecordingOperationLogMapper().proxy());

        List<ProductReviewRequest> result = service.selectReviewList(new ProductReviewRequest());

        assertEquals(1, result.size());
        assertSame(legacyReview, result.get(0));
        assertEquals(new BigDecimal("10.00"), result.get(0).getPriceAfterMin());
        assertEquals(new BigDecimal("10.00"), result.get(0).getPriceAfterMax());
    }

    @Test
    public void selectLatestRejectedReusableSubmissionRestoresAfterProductAndSkuSnapshots() throws Exception
    {
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        ProductReviewRequest review = pendingEditProductReview();
        review.setReviewStatus("REJECTED");
        review.setReviewReason("标题不清晰");
        reviewMapper.latestRejectedReusableReview = review;
        ProductSpu product = readyProduct("驳回后的商品");
        ProductReviewSnapshot productSnapshot = new ProductReviewSnapshot();
        productSnapshot.setSnapshotRole("AFTER");
        productSnapshot.setPayloadType("SPU");
        productSnapshot.setPayloadJson(JSON.toJSONString(product));
        ProductSku sku = draftSku();
        sku.setSellerSkuCode("REJECTED-SKU");
        ProductReviewSnapshot skuSnapshot = new ProductReviewSnapshot();
        skuSnapshot.setSnapshotRole("AFTER");
        skuSnapshot.setPayloadType("SKU");
        skuSnapshot.setPayloadJson(JSON.toJSONString(sku));
        reviewMapper.reviewSnapshots = List.of(productSnapshot, skuSnapshot);
        ProductReviewServiceImpl service = service(reviewMapper.proxy(),
                new RecordingProductDistributionService().proxy(), new RecordingProductDistributionMapper().proxy(),
                new RecordingOperationLogMapper().proxy());

        ProductSpu result = service.selectLatestRejectedReusableSubmission(10L);

        assertNotNull(result);
        assertEquals("驳回后的商品", result.getProductName());
        assertEquals(Long.valueOf(900L), result.getLatestReviewId());
        assertEquals("REJECTED", result.getLatestReviewStatus());
        assertEquals("标题不清晰", result.getLatestReviewFeedback());
        assertEquals(1, result.getSkus().size());
        assertEquals(Long.valueOf(10L), result.getSkus().get(0).getSpuId());
        assertEquals("REJECTED-SKU", result.getSkus().get(0).getSellerSkuCode());
        assertEquals(Long.valueOf(10L), reviewMapper.latestRejectedReusableSpuId);
    }

    private void assertReviewSupplyPriceRange(String reviewType, BigDecimal expectedAfterMin,
            BigDecimal expectedAfterMax, BigDecimal expectedBeforeMin, BigDecimal expectedBeforeMax) throws Exception
    {
        RecordingProductReviewMapper reviewMapper = new RecordingProductReviewMapper();
        ProductReviewRequest legacyReview = legacyReviewWithSalePriceRange(reviewType);
        reviewMapper.reviewById = legacyReview;
        ProductReviewItem existingItem = reviewItem(901L, "UPDATE");
        ProductReviewItem createdItem = reviewItem(902L, "CREATE");
        reviewMapper.reviewItems = List.of(existingItem, createdItem);
        ProductSku beforeExistingSku = reviewSku(100L, "8.00", "88.00");
        ProductSku beforeCreatedSku = reviewSku(101L, "18.00", "188.00");
        ProductSku afterExistingSku = reviewSku(100L, "20.00", "199.00");
        ProductSku afterCreatedSku = reviewSku(101L, "10.00", "99.00");
        reviewMapper.reviewSnapshots = List.of(
                skuSnapshot(existingItem, "BEFORE", beforeExistingSku),
                skuSnapshot(createdItem, "BEFORE", beforeCreatedSku),
                skuSnapshot(existingItem, "AFTER", afterExistingSku),
                skuSnapshot(createdItem, "AFTER", afterCreatedSku));
        ProductReviewServiceImpl service = service(reviewMapper.proxy(),
                new RecordingProductDistributionService().proxy(), new RecordingProductDistributionMapper().proxy(),
                new RecordingOperationLogMapper().proxy());

        ProductReviewRequest result = service.selectReviewById(900L);

        assertEquals(expectedAfterMin, result.getPriceAfterMin());
        assertEquals(expectedAfterMax, result.getPriceAfterMax());
        assertEquals(expectedBeforeMin, result.getPriceBeforeMin());
        assertEquals(expectedBeforeMax, result.getPriceBeforeMax());
    }

    private ProductReviewServiceImpl service(ProductReviewMapper reviewMapper,
            IProductDistributionService distributionService, ProductDistributionMapper distributionMapper,
            ProductDistributionOperationLogMapper operationLogMapper) throws Exception
    {
        ProductReviewServiceImpl service = new ProductReviewServiceImpl();
        setField(service, "productReviewMapper", reviewMapper);
        setField(service, "productDistributionService", distributionService);
        setField(service, "productDistributionMapper", distributionMapper);
        setField(service, "operationLogMapper", operationLogMapper);
        return service;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void authenticateAdmin(Long userId, String username)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        LoginUser loginUser = new LoginUser(userId, 103L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private ProductSpu draftProduct()
    {
        ProductSpu product = new ProductSpu();
        product.setSpuId(10L);
        product.setSystemSpuCode("SPU-10");
        product.setSellerId(20L);
        product.setSellerName("Seller A");
        product.setCategoryId(30L);
        product.setCategoryName("Category A");
        product.setProductName("Draft product");
        product.setProductNameEn("Draft product");
        product.setMainImageUrl("https://example.invalid/main.jpg");
        product.setSpuStatus("DRAFT");
        product.setControlStatus("NORMAL");
        product.setSalePriceMin(new BigDecimal("12.34"));
        product.setSalePriceMax(new BigDecimal("56.78"));
        product.setSupplyPriceMin(new BigDecimal("10.00"));
        product.setSupplyPriceMax(new BigDecimal("10.00"));
        product.setCurrencySummary("USD");
        product.setWarehouseKindSummary("official");
        product.setSkus(List.of(draftSku()));
        return product;
    }

    private ProductSpu readyProduct(String productName)
    {
        ProductSpu product = draftProduct();
        product.setProductName(productName);
        product.setSpuStatus("READY");
        product.getSkus().get(0).setSkuStatus("READY");
        return product;
    }

    private ProductSku draftSku()
    {
        ProductSku sku = new ProductSku();
        sku.setSkuId(100L);
        sku.setSpuId(10L);
        sku.setSellerId(20L);
        sku.setSystemSkuCode("SKU-100");
        sku.setSellerSkuCode("SELLER-SKU-100");
        sku.setSkuStatus("DRAFT");
        sku.setSupplyPrice(new BigDecimal("10.00"));
        sku.setSalePrice(new BigDecimal("12.34"));
        sku.setCurrencyCode("USD");
        sku.setSkuImageUrl("https://example.invalid/sku.jpg");
        return sku;
    }

    private ProductReviewRequest pendingNewProductReview()
    {
        ProductReviewRequest review = new ProductReviewRequest();
        review.setReviewId(900L);
        review.setReviewType("NEW_PRODUCT");
        review.setReviewStatus("PENDING");
        review.setSpuId(10L);
        review.setSellerId(20L);
        return review;
    }

    private ProductReviewRequest pendingEditProductReview()
    {
        ProductReviewRequest review = pendingNewProductReview();
        review.setReviewType("EDIT_PRODUCT_INFO");
        review.setReviewNo("PRV-EDIT-1");
        return review;
    }

    private ProductReviewRequest pendingPriceReview()
    {
        ProductReviewRequest review = pendingNewProductReview();
        review.setReviewType("EDIT_PRICE");
        review.setReviewNo("PRV-PRICE-1");
        return review;
    }

    private ProductReviewItem skuReviewItem()
    {
        ProductReviewItem item = new ProductReviewItem();
        item.setItemId(901L);
        item.setReviewId(900L);
        item.setItemType("SKU");
        item.setSkuId(100L);
        item.setSpuId(10L);
        item.setItemStatus("PENDING");
        return item;
    }

    private ProductReviewItem reviewItem(Long itemId, String changeType)
    {
        ProductReviewItem item = skuReviewItem();
        item.setItemId(itemId);
        item.setSkuId(itemId == 901L ? 100L : 101L);
        item.setChangeType(changeType);
        return item;
    }

    private ProductReviewRequest legacyReviewWithSalePriceRange(String reviewType)
    {
        ProductReviewRequest review = pendingNewProductReview();
        review.setReviewType(reviewType);
        review.setPriceBeforeMin(new BigDecimal("88.00"));
        review.setPriceBeforeMax(new BigDecimal("188.00"));
        review.setPriceAfterMin(new BigDecimal("99.00"));
        review.setPriceAfterMax(new BigDecimal("199.00"));
        return review;
    }

    private ProductSku reviewSku(Long skuId, String supplyPrice, String salePrice)
    {
        ProductSku sku = draftSku();
        sku.setSkuId(skuId);
        sku.setSupplyPrice(new BigDecimal(supplyPrice));
        sku.setSalePrice(new BigDecimal(salePrice));
        return sku;
    }

    private ProductReviewSnapshot skuSnapshot(ProductReviewItem item, String role, ProductSku sku)
    {
        String payload = JSON.toJSONString(sku);
        ProductReviewSnapshot snapshot = new ProductReviewSnapshot();
        snapshot.setItemId(item.getItemId());
        snapshot.setSnapshotRole(role);
        snapshot.setPayloadType("SKU");
        snapshot.setPayloadJson(payload);
        snapshot.setPayloadHash(sha256(payload));
        return snapshot;
    }

    private String sha256(String value)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes)
            {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static class RecordingProductReviewMapper implements InvocationHandler
    {
        private int pendingReviewCount;
        private String pendingKey;
        private int insertReviewCalls;
        private ProductReviewRequest insertedReview;
        private List<ProductReviewRequest> reviewList = new ArrayList<>();
        private ProductReviewRequest reviewById;
        private ProductReviewRequest latestRejectedReusableReview;
        private Long latestRejectedReusableSpuId;
        private ProductReviewRequest updatedReview;
        private Long updatedItemsReviewId;
        private String updatedItemsStatus;
        private List<ProductReviewItem> reviewItems = new ArrayList<>();
        private List<ProductReviewSnapshot> reviewSnapshots = new ArrayList<>();
        private final List<ProductReviewItem> insertedItems = new ArrayList<>();
        private final List<ProductReviewSnapshot> insertedSnapshots = new ArrayList<>();
        private final List<ProductReviewOperationLog> insertedOperationLogs = new ArrayList<>();
        private int selectOperationLogsCalls;

        private ProductReviewMapper proxy()
        {
            return (ProductReviewMapper) Proxy.newProxyInstance(ProductReviewMapper.class.getClassLoader(),
                    new Class<?>[] { ProductReviewMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            switch (method.getName())
            {
                case "selectReviewList":
                    return reviewList;
                case "selectReviewById":
                    return reviewById;
                case "selectLatestRejectedReusableReviewBySpuId":
                    latestRejectedReusableSpuId = (Long) args[0];
                    return latestRejectedReusableReview;
                case "countPendingReviewByKey":
                    pendingKey = (String) args[0];
                    return pendingReviewCount;
                case "insertReview":
                    insertReviewCalls++;
                    insertedReview = (ProductReviewRequest) args[0];
                    insertedReview.setReviewId(900L);
                    return 1;
                case "updateReviewStatus":
                    updatedReview = (ProductReviewRequest) args[0];
                    return 1;
                case "insertReviewItem":
                    ProductReviewItem item = (ProductReviewItem) args[0];
                    item.setItemId(1000L + insertedItems.size());
                    insertedItems.add(item);
                    return 1;
                case "selectReviewItems":
                    return reviewItems;
                case "updateReviewItemsStatus":
                    updatedItemsReviewId = (Long) args[0];
                    updatedItemsStatus = (String) args[1];
                    return 1;
                case "insertReviewSnapshot":
                    insertedSnapshots.add((ProductReviewSnapshot) args[0]);
                    return 1;
                case "selectReviewSnapshots":
                    return reviewSnapshots;
                case "insertReviewOperationLog":
                    insertedOperationLogs.add((ProductReviewOperationLog) args[0]);
                    return 1;
                case "selectReviewOperationLogs":
                    selectOperationLogsCalls++;
                    return new ArrayList<ProductReviewOperationLog>();
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }
    }

    private static class RecordingProductDistributionService implements InvocationHandler
    {
        private ProductSpu productByIdResult;
        private ProductSpu preparedProduct;
        private Long selectedSpuId;
        private ProductSpu prepareReviewedProductUpdateArg;
        private ProductSpu applyReviewedProductUpdateArg;

        private IProductDistributionService proxy()
        {
            return (IProductDistributionService) Proxy.newProxyInstance(
                    IProductDistributionService.class.getClassLoader(),
                    new Class<?>[] { IProductDistributionService.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if ("selectProductById".equals(method.getName()) && args.length == 1)
            {
                selectedSpuId = (Long) args[0];
                return productByIdResult;
            }
            if ("prepareReviewedProductUpdate".equals(method.getName()))
            {
                prepareReviewedProductUpdateArg = (ProductSpu) args[0];
                return preparedProduct == null ? args[0] : preparedProduct;
            }
            if ("applyReviewedProductUpdate".equals(method.getName()))
            {
                applyReviewedProductUpdateArg = (ProductSpu) args[0];
                return 1;
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    private static class RecordingProductDistributionMapper implements InvocationHandler
    {
        private ProductSku skuByIdResult;
        private Long updateSpuStatusId;
        private String updateSpuStatusValue;
        private String updateSpuStatusBy;
        private Long updateSkuStatusId;
        private String updateSkuStatusValue;
        private String updateSkuStatusBy;
        private Long updateSalePriceSkuId;
        private BigDecimal updateSalePriceValue;
        private String updateSalePriceBy;
        private Long updateSupplyPriceSkuId;
        private BigDecimal updateSupplyPriceValue;
        private String updateSupplyPriceBy;

        private ProductDistributionMapper proxy()
        {
            return (ProductDistributionMapper) Proxy.newProxyInstance(ProductDistributionMapper.class.getClassLoader(),
                    new Class<?>[] { ProductDistributionMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            switch (method.getName())
            {
                case "updateSpuStatus":
                    updateSpuStatusId = (Long) args[0];
                    updateSpuStatusValue = (String) args[1];
                    updateSpuStatusBy = (String) args[2];
                    return 1;
                case "selectSkuById":
                    return skuByIdResult;
                case "updateSkuStatus":
                    updateSkuStatusId = (Long) args[0];
                    updateSkuStatusValue = (String) args[1];
                    updateSkuStatusBy = (String) args[2];
                    return 1;
                case "updateSkuSalePrice":
                    updateSalePriceSkuId = (Long) args[0];
                    updateSalePriceValue = (BigDecimal) args[1];
                    updateSalePriceBy = (String) args[2];
                    return 1;
                case "updateSkuSupplyPrice":
                    updateSupplyPriceSkuId = (Long) args[0];
                    updateSupplyPriceValue = (BigDecimal) args[1];
                    updateSupplyPriceBy = (String) args[2];
                    return 1;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }
    }

    private static class RecordingOperationLogMapper implements InvocationHandler
    {
        private final List<ProductDistributionOperationLog> insertedLogs = new ArrayList<>();

        private ProductDistributionOperationLogMapper proxy()
        {
            return (ProductDistributionOperationLogMapper) Proxy.newProxyInstance(
                    ProductDistributionOperationLogMapper.class.getClassLoader(),
                    new Class<?>[] { ProductDistributionOperationLogMapper.class }, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if ("insertOperationLog".equals(method.getName()))
            {
                insertedLogs.add((ProductDistributionOperationLog) args[0]);
                return 1;
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }
}
