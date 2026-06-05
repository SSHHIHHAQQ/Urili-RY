package com.ruoyi.integration.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.integration.domain.SourceProductItem;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelPairing;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelSyncItem;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuPairing;
import com.ruoyi.integration.domain.UpstreamSkuPairingAuditEvent;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.query.SourceProductQuery;

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

    int updateConnectionStatus(@Param("connectionCode") String connectionCode, @Param("status") String status,
        @Param("updateBy") String updateBy);

    int updateConnectionDisplayOrder(@Param("connectionCode") String connectionCode,
        @Param("displayOrder") Integer displayOrder, @Param("updateBy") String updateBy);

    int updateConnectionSyncSummary(@Param("connectionCode") String connectionCode);

    Integer selectMaxDisplayOrder();

    int insertRequestLog(UpstreamRequestLog log);

    List<UpstreamRequestLog> selectRequestLogList(@Param("connectionCode") String connectionCode);

    int upsertWarehouseSyncItem(UpstreamWarehouseSyncItem item);

    int markMissingWarehouses(@Param("connectionCode") String connectionCode, @Param("syncBatchId") String syncBatchId);

    List<UpstreamWarehouseSyncItem> selectWarehouseSyncList(@Param("connectionCode") String connectionCode,
        @Param("status") String status);

    UpstreamWarehouseSyncItem selectWarehouseSyncItem(@Param("connectionCode") String connectionCode,
        @Param("warehouseCode") String warehouseCode);

    List<UpstreamWarehousePairing> selectWarehousePairingList(@Param("connectionCode") String connectionCode);

    int insertWarehousePairing(UpstreamWarehousePairing pairing);

    int deleteWarehousePairing(@Param("warehousePairingId") Long warehousePairingId);

    int upsertLogisticsChannelSyncItem(UpstreamLogisticsChannelSyncItem item);

    int markMissingLogisticsChannels(@Param("connectionCode") String connectionCode, @Param("syncBatchId") String syncBatchId);

    List<UpstreamLogisticsChannelSyncItem> selectLogisticsChannelSyncList(@Param("connectionCode") String connectionCode,
        @Param("status") String status);

    UpstreamLogisticsChannelSyncItem selectLogisticsChannelSyncItem(@Param("connectionCode") String connectionCode,
        @Param("channelCode") String channelCode);

    List<UpstreamLogisticsChannelPairing> selectLogisticsChannelPairingList(@Param("connectionCode") String connectionCode);

    int insertLogisticsChannelPairing(UpstreamLogisticsChannelPairing pairing);

    int deleteLogisticsChannelPairing(@Param("logisticsChannelPairingId") Long logisticsChannelPairingId);

    int upsertSkuSyncItem(UpstreamSkuSyncItem item);

    int batchUpsertSkuSyncItems(@Param("items") List<UpstreamSkuSyncItem> items);

    int markMissingSkus(@Param("connectionCode") String connectionCode, @Param("syncBatchId") String syncBatchId);

    List<UpstreamSkuSyncItem> selectSkuSyncList(@Param("connectionCode") String connectionCode,
        @Param("status") String status, @Param("pairingStatus") String pairingStatus,
        @Param("field") String field, @Param("keyword") String keyword);

    List<SourceProductItem> selectSourceProductList(SourceProductQuery query);

    UpstreamSkuSyncItem selectSkuSyncItem(@Param("connectionCode") String connectionCode, @Param("masterSku") String masterSku);

    List<UpstreamSkuPairing> selectSkuPairingList(@Param("connectionCode") String connectionCode);

    UpstreamSkuPairing selectSkuPairingById(@Param("skuPairingId") Long skuPairingId);

    int insertSkuPairing(UpstreamSkuPairing pairing);

    int deleteSkuPairing(@Param("skuPairingId") Long skuPairingId);

    int upsertSkuSyncState(UpstreamSkuSyncState state);

    UpstreamSkuSyncState selectSkuSyncState(@Param("connectionCode") String connectionCode);

    int insertSkuPairingAuditEvent(UpstreamSkuPairingAuditEvent event);
}
