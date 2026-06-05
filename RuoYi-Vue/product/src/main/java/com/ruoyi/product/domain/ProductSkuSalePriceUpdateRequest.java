package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 销售价批量更新请求。
 */
public class ProductSkuSalePriceUpdateRequest
{
    private List<Item> items;
    private String reason;

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static class Item
    {
        private Long skuId;
        private BigDecimal salePrice;

        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    }
}
