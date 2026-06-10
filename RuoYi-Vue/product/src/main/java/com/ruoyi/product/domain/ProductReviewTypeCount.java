package com.ruoyi.product.domain;

/**
 * 商品审核类型数量。
 */
public class ProductReviewTypeCount
{
    private String reviewType;

    private Long total;

    public String getReviewType()
    {
        return reviewType;
    }

    public void setReviewType(String reviewType)
    {
        this.reviewType = reviewType;
    }

    public Long getTotal()
    {
        return total;
    }

    public void setTotal(Long total)
    {
        this.total = total;
    }
}
