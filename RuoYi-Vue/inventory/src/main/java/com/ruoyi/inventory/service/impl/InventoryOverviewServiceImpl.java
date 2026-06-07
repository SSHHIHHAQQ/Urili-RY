package com.ruoyi.inventory.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.inventory.domain.InventoryOverviewAdjustPreviewResult;
import com.ruoyi.inventory.domain.InventoryOverviewItem;
import com.ruoyi.inventory.domain.InventorySkuWarehouseStock;
import com.ruoyi.inventory.domain.InventoryStockLedger;
import com.ruoyi.inventory.domain.request.InventoryOverviewAdjustRequest;
import com.ruoyi.inventory.mapper.InventoryOverviewMapper;
import com.ruoyi.inventory.service.IInventoryOverviewService;

/**
 * 库存总览服务实现。
 */
@Service
public class InventoryOverviewServiceImpl implements IInventoryOverviewService
{
    private static final String FIELD_PLATFORM_TOTAL = "PLATFORM_TOTAL";
    private static final String FIELD_PLATFORM_IN_TRANSIT = "PLATFORM_IN_TRANSIT";
    private static final String WAREHOUSE_OFFICIAL = "official";
    private static final String REF_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String REF_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_IN_STOCK = "IN_STOCK";
    private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
    private static final String STATUS_NO_SOURCE = "NO_SOURCE";
    private static final String STATUS_NO_WAREHOUSE = "NO_WAREHOUSE";
    private static final String STATUS_SOURCE_UNBOUND = "SOURCE_UNBOUND";
    private static final String STATUS_SOURCE_ONLY_IN_TRANSIT = "SOURCE_ONLY_IN_TRANSIT";

    @Autowired
    private InventoryOverviewMapper inventoryOverviewMapper;

    @Override
    public List<InventoryOverviewItem> selectSpuList(InventoryOverviewItem query)
    {
        return inventoryOverviewMapper.selectSpuList(query);
    }

    @Override
    public List<InventoryOverviewItem> selectSkuList(InventoryOverviewItem query)
    {
        return inventoryOverviewMapper.selectSkuList(query);
    }

    @Override
    public List<InventorySkuWarehouseStock> selectWarehouseStockListBySkuId(Long skuId)
    {
        if (skuId == null)
        {
            throw new ServiceException("SKU ID不能为空");
        }
        return inventoryOverviewMapper.selectWarehouseStockListBySkuId(skuId);
    }

    @Override
    public InventoryOverviewAdjustPreviewResult previewAdjust(InventoryOverviewAdjustRequest request)
    {
        validateAdjustRequest(request);
        InventorySkuWarehouseStock stock = requireStock(request.getStockId());
        return buildPreview(stock, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryOverviewAdjustPreviewResult confirmAdjust(InventoryOverviewAdjustRequest request)
    {
        validateAdjustRequest(request);
        InventorySkuWarehouseStock before = requireStock(request.getStockId());
        InventoryOverviewAdjustPreviewResult preview = buildPreview(before, request);
        if (!Boolean.TRUE.equals(preview.getAllowed()))
        {
            throw new ServiceException(preview.getMessage());
        }
        if (Boolean.TRUE.equals(preview.getConfirmationRequired()) && !Boolean.TRUE.equals(request.getConfirmed()))
        {
            throw new ServiceException("请先确认库存调整风险");
        }

        InventorySkuWarehouseStock after = copyStock(before);
        if (FIELD_PLATFORM_TOTAL.equals(request.getAdjustField()))
        {
            after.setPlatformTotalQty(request.getTargetQty());
        }
        else
        {
            after.setPlatformInTransitQty(request.getTargetQty());
        }
        recalculate(after);
        after.setUpdateBy(SecurityUtils.getUsername());
        inventoryOverviewMapper.updateWarehouseStock(after);

        inventoryOverviewMapper.insertLedger(buildLedger(before, after, request, preview));
        inventoryOverviewMapper.refreshSkuReadModel(after.getSkuId());
        inventoryOverviewMapper.refreshSpuReadModel(after.getSpuId());
        return preview;
    }

    @Override
    public List<InventoryStockLedger> selectLedgerList(InventoryStockLedger query)
    {
        return inventoryOverviewMapper.selectLedgerList(query);
    }

    private void validateAdjustRequest(InventoryOverviewAdjustRequest request)
    {
        if (request == null || request.getStockId() == null)
        {
            throw new ServiceException("库存行不能为空");
        }
        if (request.getTargetQty() == null || request.getTargetQty() < 0)
        {
            throw new ServiceException("库存数量不能小于0");
        }
        String field = request.getAdjustField();
        if (!FIELD_PLATFORM_TOTAL.equals(field) && !FIELD_PLATFORM_IN_TRANSIT.equals(field))
        {
            throw new ServiceException("库存调整字段不正确");
        }
    }

    private InventorySkuWarehouseStock requireStock(Long stockId)
    {
        InventorySkuWarehouseStock stock = inventoryOverviewMapper.selectWarehouseStockById(stockId);
        if (stock == null)
        {
            throw new ServiceException("库存行不存在");
        }
        return stock;
    }

    private InventoryOverviewAdjustPreviewResult buildPreview(InventorySkuWarehouseStock stock,
        InventoryOverviewAdjustRequest request)
    {
        InventorySkuWarehouseStock simulated = copyStock(stock);
        long beforeValue;
        long afterValue = qty(request.getTargetQty());
        String message;

        if (FIELD_PLATFORM_TOTAL.equals(request.getAdjustField()))
        {
            beforeValue = qty(stock.getPlatformTotalQty());
            simulated.setPlatformTotalQty(afterValue);
            if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
            {
                return result(false, false, "商品未配置发货仓库，不能调整平台库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            if (afterValue < qty(stock.getPlatformReservedQty()))
            {
                return result(false, false, "平台总库存不能小于当前平台锁定库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            message = afterValue >= beforeValue
                ? "本次会增加平台总库存。后续再减少库存时，可能需要受近期销量保护规则限制。"
                : "本次会减少平台总库存。请确认不会影响已承诺给买家的可售库存。";
        }
        else
        {
            beforeValue = qty(stock.getPlatformInTransitQty());
            if (!WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
            {
                return result(false, false, "三方仓不支持设置平台在途库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            long maxObservableInTransit = qty(stock.getSourceInTransitQty()) + qty(stock.getPendingAvailableInboundQty());
            if (afterValue > maxObservableInTransit)
            {
                return result(false, false, "平台在途库存不能大于当前可观察的来源在途库存", beforeValue, afterValue,
                    qty(stock.getPlatformAvailableQty()), qty(stock.getPlatformAvailableQty()),
                    qty(stock.getPlatformReservedQty()));
            }
            simulated.setPlatformInTransitQty(afterValue);
            message = afterValue >= beforeValue
                ? "本次会增加平台在途库存。来源可用库存增加后，系统会自动转入平台总库存。"
                : "本次会减少平台在途库存。已进入待来源可用观察的数量不会被本次操作回滚。";
        }

        recalculate(simulated);
        return result(true, true, message, beforeValue, afterValue, qty(stock.getPlatformAvailableQty()),
            qty(simulated.getPlatformAvailableQty()), qty(stock.getPlatformReservedQty()));
    }

    private InventoryOverviewAdjustPreviewResult result(boolean allowed, boolean confirmationRequired, String message,
        long beforeValue, long afterValue, long beforeAvailableQty, long afterAvailableQty, long reservedQty)
    {
        InventoryOverviewAdjustPreviewResult result = new InventoryOverviewAdjustPreviewResult();
        result.setAllowed(allowed);
        result.setConfirmationRequired(confirmationRequired);
        result.setMessage(message);
        result.setBeforeValue(beforeValue);
        result.setAfterValue(afterValue);
        result.setBeforeAvailableQty(beforeAvailableQty);
        result.setAfterAvailableQty(afterAvailableQty);
        result.setReservedQty(reservedQty);
        return result;
    }

    private void recalculate(InventorySkuWarehouseStock stock)
    {
        long available;
        if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
        {
            available = 0;
        }
        else if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
        {
            long effectiveSourceAvailable = Math.max(0, qty(stock.getSourceAvailableQty())
                - qty(stock.getPendingSourceDeductionQty()));
            available = Math.min(qty(stock.getPlatformTotalQty()), effectiveSourceAvailable)
                - qty(stock.getPlatformReservedQty());
        }
        else
        {
            available = qty(stock.getPlatformTotalQty()) - qty(stock.getPlatformReservedQty());
        }
        stock.setPlatformAvailableQty(Math.max(0, available));
        stock.setEffectiveStatus(resolveStatus(stock));
        stock.setCalcTime(new Date());
    }

    private String resolveStatus(InventorySkuWarehouseStock stock)
    {
        if (REF_NO_WAREHOUSE.equals(stock.getWarehouseRefType()))
        {
            return STATUS_NO_WAREHOUSE;
        }
        if (qty(stock.getPlatformAvailableQty()) > 0)
        {
            return STATUS_IN_STOCK;
        }
        if (WAREHOUSE_OFFICIAL.equals(stock.getWarehouseKind()))
        {
            if (REF_SOURCE_UNBOUND.equals(stock.getWarehouseRefType()))
            {
                return STATUS_SOURCE_UNBOUND;
            }
            if (qty(stock.getSourceAvailableQty()) <= 0 && qty(stock.getSourceInTransitQty()) > 0)
            {
                return STATUS_SOURCE_ONLY_IN_TRANSIT;
            }
            if (qty(stock.getSourceTotalQty()) <= 0 && qty(stock.getSourceAvailableQty()) <= 0
                && qty(stock.getSourceInTransitQty()) <= 0)
            {
                return STATUS_NO_SOURCE;
            }
        }
        return STATUS_OUT_OF_STOCK;
    }

    private InventoryStockLedger buildLedger(InventorySkuWarehouseStock before, InventorySkuWarehouseStock after,
        InventoryOverviewAdjustRequest request, InventoryOverviewAdjustPreviewResult preview)
    {
        InventoryStockLedger ledger = new InventoryStockLedger();
        ledger.setStockId(after.getStockId());
        ledger.setStockKey(after.getStockKey());
        ledger.setSpuId(after.getSpuId());
        ledger.setSkuId(after.getSkuId());
        ledger.setSellerId(after.getSellerId());
        ledger.setWarehouseKind(after.getWarehouseKind());
        ledger.setWarehouseRefType(after.getWarehouseRefType());
        ledger.setWarehouseName(after.getWarehouseName());
        ledger.setOperationType(resolveOperationType(request, before));
        ledger.setOperationSource("ADMIN");
        ledger.setBizType("INVENTORY_OVERVIEW_ADJUST");
        ledger.setBizNo("");
        ledger.setDeltaQty(preview.getAfterValue() - preview.getBeforeValue());
        ledger.setBeforePlatformTotalQty(qty(before.getPlatformTotalQty()));
        ledger.setAfterPlatformTotalQty(qty(after.getPlatformTotalQty()));
        ledger.setBeforeAvailableQty(qty(before.getPlatformAvailableQty()));
        ledger.setAfterAvailableQty(qty(after.getPlatformAvailableQty()));
        ledger.setBeforeReservedQty(qty(before.getPlatformReservedQty()));
        ledger.setAfterReservedQty(qty(after.getPlatformReservedQty()));
        ledger.setBeforeInTransitQty(qty(before.getPlatformInTransitQty()));
        ledger.setAfterInTransitQty(qty(after.getPlatformInTransitQty()));
        ledger.setRiskConfirmed(Boolean.TRUE.equals(request.getConfirmed()) ? "Y" : "N");
        ledger.setRiskMessage(preview.getMessage());
        ledger.setReason(request.getReason());
        ledger.setOperatorId(SecurityUtils.getUserId());
        ledger.setOperatorName(SecurityUtils.getUsername());
        ledger.setOperateTime(new Date());
        return ledger;
    }

    private String resolveOperationType(InventoryOverviewAdjustRequest request, InventorySkuWarehouseStock before)
    {
        if (FIELD_PLATFORM_IN_TRANSIT.equals(request.getAdjustField()))
        {
            return "IN_TRANSIT_CONFIG";
        }
        return qty(request.getTargetQty()) >= qty(before.getPlatformTotalQty()) ? "MANUAL_INCREASE" : "MANUAL_DECREASE";
    }

    private InventorySkuWarehouseStock copyStock(InventorySkuWarehouseStock source)
    {
        InventorySkuWarehouseStock target = new InventorySkuWarehouseStock();
        target.setStockId(source.getStockId());
        target.setStockKey(source.getStockKey());
        target.setSpuId(source.getSpuId());
        target.setSkuId(source.getSkuId());
        target.setSellerId(source.getSellerId());
        target.setSystemSkuCode(source.getSystemSkuCode());
        target.setWarehouseKind(source.getWarehouseKind());
        target.setWarehouseRefType(source.getWarehouseRefType());
        target.setWarehouseId(source.getWarehouseId());
        target.setWarehouseCode(source.getWarehouseCode());
        target.setWarehouseName(source.getWarehouseName());
        target.setSourceScope(source.getSourceScope());
        target.setSourceMasterWarehouseName(source.getSourceMasterWarehouseName());
        target.setSourceTotalQty(qty(source.getSourceTotalQty()));
        target.setSourceAvailableQty(qty(source.getSourceAvailableQty()));
        target.setSourceInTransitQty(qty(source.getSourceInTransitQty()));
        target.setSourceSnapshotTime(source.getSourceSnapshotTime());
        target.setPlatformTotalQty(qty(source.getPlatformTotalQty()));
        target.setPlatformReservedQty(qty(source.getPlatformReservedQty()));
        target.setPlatformInTransitQty(qty(source.getPlatformInTransitQty()));
        target.setPendingAvailableInboundQty(qty(source.getPendingAvailableInboundQty()));
        target.setPendingSourceDeductionQty(qty(source.getPendingSourceDeductionQty()));
        target.setPlatformAvailableQty(qty(source.getPlatformAvailableQty()));
        target.setEffectiveStatus(source.getEffectiveStatus());
        target.setVersion(source.getVersion());
        target.setCalcTime(source.getCalcTime());
        return target;
    }

    private long qty(Long value)
    {
        return value == null ? 0L : value;
    }
}
