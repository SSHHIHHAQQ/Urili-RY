package com.ruoyi.product.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.finance.domain.FeeEstimateSkuSnapshot;
import com.ruoyi.finance.domain.FeeEstimateSkuWarehouseCandidate;
import com.ruoyi.finance.domain.query.FeeEstimateSkuQuery;
import com.ruoyi.finance.service.FinanceFeeEstimateSkuLookupService;
import com.ruoyi.product.domain.ProductSku;
import com.ruoyi.product.domain.ProductSpuWarehouse;
import com.ruoyi.product.mapper.ProductDistributionMapper;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.service.IWarehouseService;

/**
 * Product implementation of the finance fee estimate SKU lookup port.
 */
@Service
public class ProductFeeEstimateSkuLookupServiceImpl implements FinanceFeeEstimateSkuLookupService
{
    @Autowired
    private ProductDistributionMapper productDistributionMapper;

    @Autowired
    private IWarehouseService warehouseService;

    @Override
    public List<FeeEstimateSkuSnapshot> selectSkuSnapshots(FeeEstimateSkuQuery query)
    {
        FeeEstimateSkuQuery filters = query == null ? new FeeEstimateSkuQuery() : query;
        ProductSku skuQuery = new ProductSku();
        skuQuery.setSourceWarehouseCode(StringUtils.trimToNull(filters.getSourceWarehouseCode()));
        skuQuery.setSkuCode(StringUtils.trimToNull(filters.getSkuCode()));
        skuQuery.setProductName(StringUtils.trimToNull(filters.getProductName()));
        return productDistributionMapper.selectSkuPageList(skuQuery).stream()
            .map(this::toSnapshot)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<FeeEstimateSkuSnapshot> selectSkuSnapshotsByIds(List<Long> skuIds)
    {
        if (skuIds == null || skuIds.isEmpty())
        {
            return List.of();
        }
        return skuIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(productDistributionMapper::selectSkuById)
            .map(this::toSnapshot)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<FeeEstimateSkuWarehouseCandidate> selectSkuWarehouseCandidatesByIds(List<Long> skuIds)
    {
        if (skuIds == null || skuIds.isEmpty())
        {
            return List.of();
        }
        return skuIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(productDistributionMapper::selectSkuById)
            .filter(Objects::nonNull)
            .flatMap(sku -> productDistributionMapper.selectWarehousesBySpuId(sku.getSpuId()).stream()
                .map(warehouse -> toWarehouseCandidate(sku, warehouse)))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private FeeEstimateSkuSnapshot toSnapshot(ProductSku sku)
    {
        if (sku == null)
        {
            return null;
        }
        FeeEstimateSkuSnapshot snapshot = new FeeEstimateSkuSnapshot();
        snapshot.setSkuId(sku.getSkuId());
        snapshot.setSystemSkuCode(sku.getSystemSkuCode());
        snapshot.setSellerSkuCode(sku.getSellerSkuCode());
        snapshot.setProductName(sku.getProductName());
        snapshot.setProductNameEn(sku.getProductNameEn());
        snapshot.setMasterSku(sku.getMasterSku());
        snapshot.setMeasureLengthCm(sku.getMeasureLengthCm());
        snapshot.setMeasureWidthCm(sku.getMeasureWidthCm());
        snapshot.setMeasureHeightCm(sku.getMeasureHeightCm());
        snapshot.setMeasureWeightKg(sku.getMeasureWeightKg());
        snapshot.setMeasureSource(sku.getMeasureSource());
        snapshot.setSourceWarehouseNames(sku.getSourceWarehouseNames());
        snapshot.setSourceWarehouseCount(sku.getSourceWarehouseCount());
        snapshot.setAvailableStock(sku.getAvailableStock());
        List<ProductSpuWarehouse> warehouses = productDistributionMapper.selectWarehousesBySpuId(sku.getSpuId());
        List<String> warehouseCodes = warehouses.stream()
            .map(ProductSpuWarehouse::getWarehouseCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        snapshot.setSourceWarehouseCodes(warehouseCodes);
        if (snapshot.getSourceWarehouseCount() == null)
        {
            snapshot.setSourceWarehouseCount(warehouseCodes.size());
        }
        if (StringUtils.isBlank(snapshot.getSourceWarehouseNames()))
        {
            snapshot.setSourceWarehouseNames(warehouses.stream()
                .map(warehouse -> StringUtils.defaultIfBlank(warehouse.getWarehouseName(), warehouse.getWarehouseCode()))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(", ")));
        }
        snapshot.setLabel(buildLabel(sku));
        snapshot.setSearchText(String.join(" ",
            StringUtils.defaultString(sku.getSystemSkuCode()),
            StringUtils.defaultString(sku.getSellerSkuCode()),
            StringUtils.defaultString(sku.getMasterSku()),
            StringUtils.defaultString(sku.getProductName()),
            StringUtils.defaultString(sku.getProductNameEn())));
        return snapshot;
    }

    private String buildLabel(ProductSku sku)
    {
        String code = StringUtils.defaultIfBlank(sku.getSystemSkuCode(),
            StringUtils.defaultIfBlank(sku.getSellerSkuCode(), String.valueOf(sku.getSkuId())));
        String name = StringUtils.defaultIfBlank(sku.getProductName(), sku.getProductNameEn());
        return StringUtils.isBlank(name) ? code : code + " - " + name;
    }

    private FeeEstimateSkuWarehouseCandidate toWarehouseCandidate(ProductSku sku, ProductSpuWarehouse spuWarehouse)
    {
        if (sku == null || spuWarehouse == null || StringUtils.isBlank(spuWarehouse.getWarehouseCode()))
        {
            return null;
        }
        Warehouse warehouse = spuWarehouse.getWarehouseId() == null
            ? null
            : warehouseService.selectWarehouseById(spuWarehouse.getWarehouseId());
        FeeEstimateSkuWarehouseCandidate candidate = new FeeEstimateSkuWarehouseCandidate();
        candidate.setSkuId(sku.getSkuId());
        candidate.setSpuId(sku.getSpuId());
        candidate.setWarehouseId(spuWarehouse.getWarehouseId());
        candidate.setWarehouseCode(spuWarehouse.getWarehouseCode());
        candidate.setWarehouseName(spuWarehouse.getWarehouseName());
        candidate.setWarehouseKind(spuWarehouse.getWarehouseKind());
        candidate.setCurrencyCode(spuWarehouse.getSettlementCurrency());
        if (warehouse != null)
        {
            candidate.setCountryCode(warehouse.getCountryCode());
            candidate.setStatus(warehouse.getStatus());
        }
        return candidate;
    }
}
