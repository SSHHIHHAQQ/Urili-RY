package com.ruoyi.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.product.domain.ProductReviewItem;
import com.ruoyi.product.domain.ProductReviewOperationLog;
import com.ruoyi.product.domain.ProductReviewRequest;
import com.ruoyi.product.domain.ProductReviewSnapshot;

/**
 * 商品审核 Mapper。
 */
public interface ProductReviewMapper
{
    List<ProductReviewRequest> selectReviewList(ProductReviewRequest query);

    ProductReviewRequest selectReviewById(@Param("reviewId") Long reviewId);

    List<ProductReviewRequest> selectLatestReviewsBySpuIds(@Param("spuIds") List<Long> spuIds);

    ProductReviewRequest selectLatestRejectedReusableReviewBySpuId(@Param("spuId") Long spuId);

    int countPendingReviewByKey(@Param("activePendingKey") String activePendingKey);

    int insertReview(ProductReviewRequest review);

    int updateReviewStatus(ProductReviewRequest review);

    int insertReviewItem(ProductReviewItem item);

    List<ProductReviewItem> selectReviewItems(@Param("reviewId") Long reviewId);

    int updateReviewItemsStatus(@Param("reviewId") Long reviewId, @Param("itemStatus") String itemStatus);

    int insertReviewSnapshot(ProductReviewSnapshot snapshot);

    List<ProductReviewSnapshot> selectReviewSnapshots(@Param("reviewId") Long reviewId);

    int insertReviewOperationLog(ProductReviewOperationLog log);

    List<ProductReviewOperationLog> selectReviewOperationLogs(@Param("reviewId") Long reviewId);
}
