package com.ruoyi.integration.mapper;

import java.util.List;
import com.ruoyi.integration.domain.IntegrationOption;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.integration.domain.SourceOfficialWarehouseOption;
import com.ruoyi.integration.domain.SourceProductBindingSnapshot;
import com.ruoyi.integration.domain.SourceProductItem;
import com.ruoyi.integration.domain.SourceSkuPairingProjection;
import com.ruoyi.integration.domain.SourceWarehouseStockGroupItem;
import com.ruoyi.integration.domain.SourceWarehouseStockItem;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelPairing;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelSyncItem;
import com.ruoyi.integration.domain.UpstreamInventorySyncState;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuPairing;
import com.ruoyi.integration.domain.UpstreamSkuPairingAuditEvent;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSyncBatch;
import com.ruoyi.integration.domain.UpstreamSyncState;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehousePairingSnapshot;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.query.SourceProductQuery;
import com.ruoyi.integration.domain.query.SourceWarehouseStockQuery;
import com.ruoyi.inventory.domain.InventoryOfficialSourceStock;
import com.ruoyi.inventory.domain.InventorySourceSkuKey;

/**
 * 上游系统管理 Mapper。
 */
public interface UpstreamSystemMapper
{
    List<UpstreamSystemConnection> selectConnectionList(UpstreamSystemConnection query);

    UpstreamSystemConnection selectConnectionByCode(String connectionCode);

    int insertConnection(UpstreamSystemConnection connection);

    int updateConnectionInfo(UpstreamSystemConnection connection);

    int updateConnectionCredentials(UpstreamSystemConnection connection);

    int updateConnectionCredentialStatus(UpstreamSystemConnection connection);

    int updateConnectionStatus(@Param("connectionCode") String connectionCode, @Param("status") String status,
        @Param("updateBy") String updateBy);

    int updateConnectionDisplayOrder(@Param("connectionCode") String connectionCode,
        @Param("displayOrder") Integer displayOrder, @Param("updateBy") String updateBy);

    int updateConnectionSyncSummary(@Param("connectionCode") String connectionCode);

    Integer selectMaxDisplayOrder();

    int insertRequestLog(UpstreamRequestLog log);

    List<UpstreamRequestLog> selectRequestLogList(@Param("connectionCode") String connectionCode);

    int upsertSyncState(UpstreamSyncState state);

    UpstreamSyncState selectSyncState(@Param("connectionCode") String connectionCode,
        @Param("syncType") String syncType);

    List<UpstreamSyncState> selectSyncStateList(@Param("connectionCode") String connectionCode);

    int insertSyncBatch(UpstreamSyncBatch batch);

    int updateSyncBatch(UpstreamSyncBatch batch);

    int batchInsertWarehouseStage(@Param("items") List<UpstreamWarehouseSyncItem> items);

    int insertNewWarehousesFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int updateChangedWarehousesFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int touchUnchangedWarehousesFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int disableMissingWarehousesFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int cleanupWarehouseStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int upsertWarehouseSyncItem(UpstreamWarehouseSyncItem item);

    int markMissingWarehouses(@Param("connectionCode") String connectionCode, @Param("syncBatchId") String syncBatchId);

    List<UpstreamWarehouseSyncItem> selectWarehouseSyncList(@Param("connectionCode") String connectionCode,
        @Param("status") String status);

    UpstreamWarehouseSyncItem selectWarehouseSyncItem(@Param("connectionCode") String connectionCode,
        @Param("warehouseCode") String warehouseCode);

    List<UpstreamWarehousePairing> selectWarehousePairingList(@Param("connectionCode") String connectionCode);

    List<UpstreamWarehousePairingSnapshot> selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(
        @Param("systemWarehouseCodes") List<String> systemWarehouseCodes);

    int insertWarehousePairing(UpstreamWarehousePairing pairing);

    int deleteWarehousePairing(@Param("connectionCode") String connectionCode,
        @Param("warehousePairingId") Long warehousePairingId);

    int upsertLogisticsChannelSyncItem(UpstreamLogisticsChannelSyncItem item);

    int markMissingLogisticsChannels(@Param("connectionCode") String connectionCode, @Param("syncBatchId") String syncBatchId);

    int batchInsertLogisticsChannelStage(@Param("items") List<UpstreamLogisticsChannelSyncItem> items);

    int insertNewLogisticsChannelsFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int updateChangedLogisticsChannelsFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int touchUnchangedLogisticsChannelsFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int disableMissingLogisticsChannelsFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int cleanupLogisticsChannelStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    List<UpstreamLogisticsChannelSyncItem> selectLogisticsChannelSyncList(@Param("connectionCode") String connectionCode,
        @Param("status") String status);

    UpstreamLogisticsChannelSyncItem selectLogisticsChannelSyncItem(@Param("connectionCode") String connectionCode,
        @Param("warehouseCode") String warehouseCode, @Param("channelCode") String channelCode);

    List<UpstreamLogisticsChannelPairing> selectLogisticsChannelPairingList(@Param("connectionCode") String connectionCode);

    int insertLogisticsChannelPairing(UpstreamLogisticsChannelPairing pairing);

    int deleteLogisticsChannelPairing(@Param("connectionCode") String connectionCode,
        @Param("logisticsChannelPairingId") Long logisticsChannelPairingId);

    int upsertSkuSyncItem(UpstreamSkuSyncItem item);

    int batchUpsertSkuSyncItems(@Param("items") List<UpstreamSkuSyncItem> items);

    int updateSkuWmsDimensions(UpstreamSkuSyncItem item);

    int batchInsertSkuStage(@Param("items") List<UpstreamSkuSyncItem> items);

    int insertNewSkusFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int updateChangedSkusFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int touchUnchangedSkusFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int disableMissingSkusFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int cleanupSkuStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int batchInsertSkuDimensionStage(@Param("items") List<UpstreamSkuSyncItem> items);

    int insertNewSkusFromDimensionStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int updateChangedSkuDimensionsFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int touchUnchangedSkuDimensionsFromStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int cleanupSkuDimensionStage(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int markMissingSkus(@Param("connectionCode") String connectionCode, @Param("syncBatchId") String syncBatchId);

    List<UpstreamSkuSyncItem> selectSkuSyncList(@Param("connectionCode") String connectionCode,
        @Param("status") String status, @Param("pairingStatus") String pairingStatus,
        @Param("dimensionStatus") String dimensionStatus, @Param("field") String field,
        @Param("keyword") String keyword);

    List<SourceProductItem> selectSourceProductList(SourceProductQuery query);

    long countSourceProductList(SourceProductQuery query);

    SourceProductItem selectSourceProductGroupSummary(SourceProductQuery query);

    List<SourceProductItem> selectSourceProductWarehouseDetailList(SourceProductQuery query);

    int deleteAllSourceProductWarehouseDetails();

    int deleteAllSourceProductDimensionGroups();

    int deleteAllSourceProductGroups();

    int insertAllSourceProductGroups(@Param("connectionCode") String connectionCode);

    int insertAllSourceProductDimensionGroups(@Param("connectionCode") String connectionCode);

    int insertAllSourceProductWarehouseDetails(@Param("connectionCode") String connectionCode);

    int deleteSourceProductWarehouseDetailsByConnection(@Param("connectionCode") String connectionCode);

    int deleteSourceProductDimensionGroupsByConnection(@Param("connectionCode") String connectionCode);

    int deleteSourceProductGroupsByConnection(@Param("connectionCode") String connectionCode);

    int insertSourceProductGroupsByConnection(@Param("connectionCode") String connectionCode);

    int insertSourceProductDimensionGroupsByConnection(@Param("connectionCode") String connectionCode);

    int insertSourceProductWarehouseDetailsByConnection(@Param("connectionCode") String connectionCode);

    int upsertSourceWarehouseStock(SourceWarehouseStockItem item);

    int markMissingSourceWarehouseStocks(@Param("connectionCode") String connectionCode,
        @Param("syncBatchId") String syncBatchId);

    int refreshInventorySnapshotWarehousePairingByConnection(@Param("connectionCode") String connectionCode);

    int refreshInventorySnapshotSkuPairingByConnection(@Param("connectionCode") String connectionCode);

    List<SourceWarehouseStockItem> selectSourceWarehouseStockList(SourceWarehouseStockQuery query);

    long countSourceWarehouseStockGroupList(SourceWarehouseStockQuery query);

    List<SourceWarehouseStockGroupItem> selectSourceWarehouseStockGroupList(SourceWarehouseStockQuery query);

    List<SourceWarehouseStockItem> selectSourceWarehouseStockGroupDetailList(SourceWarehouseStockQuery query);

    List<IntegrationOption> selectSourceWarehouseStockMasterWarehouseOptions(SourceWarehouseStockQuery query);

    List<IntegrationOption> selectSourceWarehouseStockUpstreamWarehouseOptions(SourceWarehouseStockQuery query);

    List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroup(
        @Param("sourceDimensionGroupKey") String sourceDimensionGroupKey);

    List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroups(
        @Param("sourceDimensionGroupKeys") List<String> sourceDimensionGroupKeys);

    SourceProductBindingSnapshot selectOfficialSourceBindingSnapshot(
        @Param("sourceDimensionGroupKey") String sourceDimensionGroupKey);

    List<SourceProductBindingSnapshot> selectOfficialSourceBindingSnapshots(
        @Param("sourceDimensionGroupKeys") List<String> sourceDimensionGroupKeys);

    List<String> selectSourceConnectionCodesByDimensionGroup(
        @Param("sourceDimensionGroupKey") String sourceDimensionGroupKey);

    List<String> selectUpstreamSkuPairingConnectionCodesBySystemSkuAndMasterSku(@Param("systemSku") String systemSku,
        @Param("masterSku") String masterSku);

    int deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes(@Param("systemSku") String systemSku,
        @Param("connectionCodes") List<String> connectionCodes);

    int upsertUpstreamSkuPairingsForProjection(SourceSkuPairingProjection projection);

    int deleteAllSourceWarehouseStockFilterMetrics();

    int deleteAllSourceWarehouseStockGroups();

    int deleteAllSourceWarehouseStockDetails();

    int deleteSourceWarehouseStockFilterMetricsByConnection(@Param("connectionCode") String connectionCode);

    int deleteSourceWarehouseStockGroupsByConnection(@Param("connectionCode") String connectionCode);

    int deleteSourceWarehouseStockDetailsByConnection(@Param("connectionCode") String connectionCode);

    int insertAllSourceWarehouseStockDetails(@Param("connectionCode") String connectionCode);

    int insertAllSourceWarehouseStockGroups(@Param("connectionCode") String connectionCode);

    int insertAllSourceWarehouseStockFilterMetrics(@Param("connectionCode") String connectionCode);

    List<InventorySourceSkuKey> selectAffectedOfficialMasterSourceSkuKeysByConnection(
        @Param("connectionCode") String connectionCode);

    List<InventoryOfficialSourceStock> selectOfficialMasterSourceStocksBySourceSkuKeys(
        @Param("sourceKeys") List<InventorySourceSkuKey> sourceKeys);

    int upsertInventorySyncState(UpstreamInventorySyncState state);

    UpstreamInventorySyncState selectInventorySyncState(@Param("connectionCode") String connectionCode);

    UpstreamSkuSyncItem selectSkuSyncItem(@Param("connectionCode") String connectionCode, @Param("masterSku") String masterSku);

    List<UpstreamSkuPairing> selectSkuPairingList(@Param("connectionCode") String connectionCode);

    UpstreamSkuPairing selectSkuPairingById(@Param("connectionCode") String connectionCode,
        @Param("skuPairingId") Long skuPairingId);

    int insertSkuPairing(UpstreamSkuPairing pairing);

    int deleteSkuPairing(@Param("connectionCode") String connectionCode, @Param("skuPairingId") Long skuPairingId);

    int upsertSkuSyncState(UpstreamSkuSyncState state);

    UpstreamSkuSyncState selectSkuSyncState(@Param("connectionCode") String connectionCode);

    int insertSkuPairingAuditEvent(UpstreamSkuPairingAuditEvent event);
}
