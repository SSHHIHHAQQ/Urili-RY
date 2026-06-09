package com.ruoyi.integration.sync;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.domain.SourceWarehouseStockItem;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamSkuPairing;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.lingxing.LingxingInventoryProductPage;
import com.ruoyi.integration.lingxing.LingxingInventoryProductStock;
import com.ruoyi.integration.lingxing.LingxingOpenApiClient;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.support.UpstreamSystemConstants;

@Component
public class UpstreamInventorySyncComponent
{
    private static final int SKU_PAGE_SIZE = 100;

    private static final long INVENTORY_SYNC_OVERLAP_MS = 5 * 60 * 1000L;

    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    public int sync(LingxingOpenApiClient client, String connectionCode, String syncBatchId,
        UpstreamInventorySyncState previousState, Date startedTime)
    {
        Map<String, UpstreamWarehousePairing> warehousePairingMap = buildWarehousePairingMap(connectionCode);
        Map<String, UpstreamWarehouseSyncItem> warehouseSyncMap = buildWarehouseSyncMap(connectionCode);
        Map<String, UpstreamSkuPairing> skuPairingMap = buildSkuPairingMap(connectionCode);
        String startTime = inventorySyncStartTime(previousState);
        String endTime = StringUtils.isBlank(startTime) ? "" : formatLingxingDateTime(startedTime);
        int current = 1;
        int total = Integer.MAX_VALUE;
        int synced = 0;
        while ((current - 1) * SKU_PAGE_SIZE < total)
        {
            LingxingInventoryProductPage page = client.listInventoryProductPage(current, SKU_PAGE_SIZE,
                startTime, endTime, UUID.randomUUID().toString());
            total = page.getTotal();
            List<LingxingInventoryProductStock> records = page.getRecords();
            if (records == null || records.isEmpty())
            {
                break;
            }
            for (LingxingInventoryProductStock stock : records)
            {
                SourceWarehouseStockItem item = new SourceWarehouseStockItem();
                item.setConnectionCode(connectionCode);
                copyStockFields(item, stock);
                UpstreamWarehouseSyncItem warehouse = warehouseSyncMap.get(item.getUpstreamWarehouseCode());
                if (StringUtils.isBlank(item.getUpstreamWarehouseName()) && warehouse != null)
                {
                    item.setUpstreamWarehouseName(warehouse.getWarehouseName());
                }
                UpstreamWarehousePairing warehousePairing = warehousePairingMap.get(item.getUpstreamWarehouseCode());
                if (warehousePairing != null)
                {
                    item.setSystemWarehouseCode(warehousePairing.getSystemWarehouseCode());
                    item.setSystemWarehouseName(warehousePairing.getSystemWarehouseName());
                }
                UpstreamSkuPairing skuPairing = skuPairingMap.get(item.getMasterSku());
                if (skuPairing != null)
                {
                    item.setSystemSku(skuPairing.getSystemSku());
                    item.setSystemSkuName(skuPairing.getSystemSkuName());
                    item.setCustomerName(skuPairing.getCustomerName());
                }
                item.setStatus(UpstreamSystemConstants.STATUS_ACTIVE);
                item.setSyncBatchId(syncBatchId);
                upstreamSystemMapper.upsertSourceWarehouseStock(item);
                synced++;
            }
            current++;
        }
        return synced;
    }

    private Map<String, UpstreamWarehousePairing> buildWarehousePairingMap(String connectionCode)
    {
        Map<String, UpstreamWarehousePairing> result = new HashMap<>();
        String expectedPairingRole = pairingRoleForConnection(connectionCode);
        List<UpstreamWarehousePairing> pairings = upstreamSystemMapper.selectWarehousePairingList(connectionCode);
        for (UpstreamWarehousePairing pairing : pairings)
        {
            if (expectedPairingRole.equals(pairing.getPairingRole())
                && UpstreamSystemConstants.STATUS_ACTIVE.equals(pairing.getStatus())
                && StringUtils.isNotBlank(pairing.getUpstreamWarehouseCode()))
            {
                result.put(pairing.getUpstreamWarehouseCode(), pairing);
            }
        }
        return result;
    }

    private String pairingRoleForConnection(String connectionCode)
    {
        UpstreamSystemConnection connection = upstreamSystemMapper.selectConnectionByCode(connectionCode);
        if (connection != null
            && UpstreamSystemConstants.SETTLEMENT_TYPE_SELF_OPERATED_RECEIVABLE.equals(connection.getSettlementType()))
        {
            return UpstreamSystemConstants.PAIRING_ROLE_QUOTE;
        }
        return UpstreamSystemConstants.PAIRING_ROLE_FULFILLMENT;
    }

    private Map<String, UpstreamWarehouseSyncItem> buildWarehouseSyncMap(String connectionCode)
    {
        Map<String, UpstreamWarehouseSyncItem> result = new HashMap<>();
        List<UpstreamWarehouseSyncItem> warehouses = upstreamSystemMapper.selectWarehouseSyncList(connectionCode, null);
        for (UpstreamWarehouseSyncItem warehouse : warehouses)
        {
            if (StringUtils.isNotBlank(warehouse.getWarehouseCode()))
            {
                result.put(warehouse.getWarehouseCode(), warehouse);
            }
        }
        return result;
    }

    private Map<String, UpstreamSkuPairing> buildSkuPairingMap(String connectionCode)
    {
        Map<String, UpstreamSkuPairing> result = new HashMap<>();
        List<UpstreamSkuPairing> pairings = upstreamSystemMapper.selectSkuPairingList(connectionCode);
        for (UpstreamSkuPairing pairing : pairings)
        {
            if (StringUtils.isNotBlank(pairing.getMasterSku()))
            {
                result.put(pairing.getMasterSku(), pairing);
            }
        }
        return result;
    }

    private String inventorySyncStartTime(UpstreamInventorySyncState previousState)
    {
        if (previousState == null || previousState.getLastSuccessTime() == null)
        {
            return "";
        }
        long startAt = Math.max(0L, previousState.getLastSuccessTime().getTime() - INVENTORY_SYNC_OVERLAP_MS);
        return formatLingxingDateTime(new Date(startAt));
    }

    private String formatLingxingDateTime(Date value)
    {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    private void copyStockFields(SourceWarehouseStockItem item, LingxingInventoryProductStock stock)
    {
        item.setUpstreamWarehouseCode(StringUtils.trimToEmpty(stock.getWarehouseCode()));
        item.setUpstreamWarehouseName(StringUtils.trimToEmpty(stock.getWarehouseName()));
        item.setMasterSku(StringUtils.trimToEmpty(stock.getSku()));
        item.setMasterProductName(StringUtils.trimToEmpty(stock.getProductName()));
        item.setInventoryScope(StringUtils.defaultIfBlank(StringUtils.trimToEmpty(stock.getInventoryScope()),
            "COMPREHENSIVE"));
        item.setInventoryAttribute(StringUtils.trimToEmpty(stock.getInventoryAttribute()));
        item.setBatchNo(StringUtils.trimToEmpty(stock.getBatchNo()));
        item.setLocationCode(StringUtils.trimToEmpty(stock.getLocationCode()));
        item.setTotalQuantity(zeroIfNull(stock.getTotalQuantity()));
        item.setAvailableQuantity(zeroIfNull(stock.getAvailableQuantity()));
        item.setLockedQuantity(zeroIfNull(stock.getLockedQuantity()));
        item.setInTransitQuantity(zeroIfNull(stock.getInTransitQuantity()));
        item.setBoxedQuantity(stock.getBoxedQuantity());
        item.setUnboxedQuantity(stock.getUnboxedQuantity());
        item.setSourcePayloadJson(stock.getSourcePayloadJson());
        item.setSourcePayloadHash(stock.getSourcePayloadHash());
    }

    private Long zeroIfNull(Long value)
    {
        return value == null ? 0L : value;
    }
}
