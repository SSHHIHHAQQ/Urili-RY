package com.ruoyi.product.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 供货价批量更新审核请求。
 */
public class ProductSkuSupplyPriceUpdateRequest
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
        private BigDecimal supplyPrice;

        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
        public BigDecimal getSupplyPrice() { return supplyPrice; }
        public void setSupplyPrice(BigDecimal supplyPrice) { this.supplyPrice = supplyPrice; }
    }
}
