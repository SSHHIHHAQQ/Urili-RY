package com.ruoyi.product.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商城商品图片，backed by product_image.
 */
public class ProductImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long imageId;
    private String ownerType;
    private Long ownerId;
    private Long spuId;
    private Long skuId;
    private String imageUrl;
    private String imageRole;
    private Integer sortOrder;

    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageRole() { return imageRole; }
    public void setImageRole(String imageRole) { this.imageRole = imageRole; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
