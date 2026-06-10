package com.ruoyi.integration.sync;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.common.utils.file.ImageResourceUtils;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.lingxing.LingxingProductSku;

final class LingxingSkuSyncItemMapper
{
    private LingxingSkuSyncItemMapper()
    {
    }

    static void copyFields(UpstreamSkuSyncItem item, LingxingProductSku sku)
    {
        item.setProductAliasName(sku.getProductAliasName());
        item.setApproveStatus(sku.getApproveStatus());
        item.setProductType(sku.getProductType());
        item.setProductDescription(sku.getProductDescription());
        item.setImageUrl(ImageResourceUtils.normalizeExternalImageResourceOrEmpty(sku.getImageUrl()));
        item.setMainCode(sku.getMainCode());
        item.setOtherCode(sku.getOtherCode());
        item.setFnsku(sku.getFnsku());
        item.setCountryOfOriginName(sku.getCountryOfOriginName());
        item.setCurrencyCode(sku.getCurrencyCode());
        item.setCustomhouseCode(sku.getCustomhouseCode());
        item.setDangerousCargo(sku.getDangerousCargo());
        item.setDeclareNameCn(sku.getDeclareNameCn());
        item.setDeclareNameEn(sku.getDeclareNameEn());
        item.setDeclarePrice(sku.getDeclarePrice());
        item.setHeight(sku.getHeight());
        item.setHeightBs(sku.getHeightBs());
        item.setLength(sku.getLength());
        item.setLengthBs(sku.getLengthBs());
        item.setWeight(sku.getWeight());
        item.setWeightBs(sku.getWeightBs());
        item.setWidth(sku.getWidth());
        item.setWidthBs(sku.getWidthBs());
        item.setWmsHeight(sku.getWmsHeight());
        item.setWmsHeightBs(sku.getWmsHeightBs());
        item.setWmsLength(sku.getWmsLength());
        item.setWmsLengthBs(sku.getWmsLengthBs());
        item.setWmsWeight(sku.getWmsWeight());
        item.setWmsWeightBs(sku.getWmsWeightBs());
        item.setWmsWidth(sku.getWmsWidth());
        item.setWmsWidthBs(sku.getWmsWidthBs());
        item.setCat1Name(sku.getCat1Name());
        item.setCat2Name(sku.getCat2Name());
        item.setCat3Name(sku.getCat3Name());
        item.setPlatformSkuInfoJson(sku.getPlatformSkuInfoJson());
        item.setBrazilTaxInfoJson(sku.getBrazilTaxInfoJson());
        item.setSourcePayloadJson(sku.getSourcePayloadJson());
        item.setSourcePayloadHash(sku.getSourcePayloadHash());
    }

    static String buildSearchText(LingxingProductSku sku)
    {
        List<String> parts = new ArrayList<>();
        addSearchPart(parts, sku.getSku());
        addSearchPart(parts, sku.getProductName());
        addSearchPart(parts, sku.getProductAliasName());
        addSearchPart(parts, sku.getMainCode());
        addSearchPart(parts, sku.getOtherCode());
        addSearchPart(parts, sku.getFnsku());
        addSearchPart(parts, sku.getDeclareNameCn());
        addSearchPart(parts, sku.getDeclareNameEn());
        addSearchPart(parts, sku.getCustomhouseCode());
        addSearchPart(parts, sku.getCountryOfOriginName());
        addSearchPart(parts, sku.getCat1Name());
        addSearchPart(parts, sku.getCat2Name());
        addSearchPart(parts, sku.getCat3Name());
        return String.join(" ", parts);
    }

    static boolean hasAnyWmsDimension(LingxingProductSku sku)
    {
        return sku.getWmsHeight() != null || sku.getWmsLength() != null || sku.getWmsWidth() != null
            || sku.getWmsWeight() != null || sku.getWmsHeightBs() != null || sku.getWmsLengthBs() != null
            || sku.getWmsWidthBs() != null || sku.getWmsWeightBs() != null;
    }

    private static void addSearchPart(List<String> parts, String value)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isNotBlank(trimmed))
        {
            parts.add(trimmed);
        }
    }
}
