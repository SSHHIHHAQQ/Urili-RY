package com.ruoyi.product.service;

import java.util.List;
import com.ruoyi.product.domain.ProductReviewOperationLog;
import com.ruoyi.product.domain.ProductReviewRequest;
import com.ruoyi.product.domain.ProductSkuSalePriceUpdateRequest;
import com.ruoyi.product.domain.ProductSpu;

/**
 * 商品审核服务。
 */
public interface IProductReviewService
{
    List<ProductReviewRequest> selectReviewList(ProductReviewRequest query);

    ProductReviewRequest selectReviewById(Long reviewId);

    ProductSpu selectLatestRejectedReusableSubmission(Long spuId);

    int submitNewProductReview(Long spuId);

    int submitProductEditReview(ProductSpu product);

    int submitSkuSalePriceReview(ProductSkuSalePriceUpdateRequest request);

    int approveReview(Long reviewId, String reason);

    int rejectReview(Long reviewId, String reason);

    List<ProductReviewOperationLog> selectReviewOperationLogs(Long reviewId);
}
