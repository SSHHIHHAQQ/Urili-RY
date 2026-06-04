package com.ruoyi.integration.service;

import java.util.List;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelPairing;
import com.ruoyi.integration.domain.UpstreamLogisticsChannelSyncItem;
import com.ruoyi.integration.domain.UpstreamRequestLog;
import com.ruoyi.integration.domain.UpstreamSkuPairing;
import com.ruoyi.integration.domain.UpstreamSkuSyncItem;
import com.ruoyi.integration.domain.UpstreamSkuSyncState;
import com.ruoyi.integration.domain.UpstreamSystemConnection;
import com.ruoyi.integration.domain.UpstreamWarehousePairing;
import com.ruoyi.integration.domain.UpstreamWarehouseSyncItem;
import com.ruoyi.integration.domain.request.LogisticsChannelPairingRequest;
import com.ruoyi.integration.domain.request.SkuPairingRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionInfoRequest;
import com.ruoyi.integration.domain.request.UpstreamConnectionRequest;
import com.ruoyi.integration.domain.request.UpstreamCredentialRequest;
import com.ruoyi.integration.domain.request.WarehousePairingRequest;
import com.ruoyi.integration.domain.response.UpstreamSyncResult;

/**
 * 上游系统管理服务。
 */
public interface IUpstreamSystemService
{
    List<UpstreamSystemConnection> selectConnectionList(UpstreamSystemConnection query);

    UpstreamSystemConnection selectConnectionByCode(String connectionCode);

    int insertConnection(UpstreamConnectionRequest request);

    int updateConnectionInfo(String connectionCode, UpstreamConnectionInfoRequest request);

    int updateConnectionCredentials(String connectionCode, UpstreamCredentialRequest request);

    int updateConnectionStatus(String connectionCode, String status);

    int updateConnectionOrder(List<String> connectionCodes);

    int authorize(String connectionCode);

    UpstreamSyncResult syncAll(String connectionCode);

    UpstreamSyncResult syncSkusOnly(String connectionCode);

    List<UpstreamWarehouseSyncItem> selectWarehouseSyncList(String connectionCode, String status);

    List<UpstreamWarehousePairing> selectWarehousePairingList(String connectionCode);

    int insertWarehousePairing(String connectionCode, WarehousePairingRequest request);

    int deleteWarehousePairing(Long warehousePairingId);

    List<UpstreamLogisticsChannelSyncItem> selectLogisticsChannelSyncList(String connectionCode, String status);

    List<UpstreamLogisticsChannelPairing> selectLogisticsChannelPairingList(String connectionCode);

    int insertLogisticsChannelPairing(String connectionCode, LogisticsChannelPairingRequest request);

    int deleteLogisticsChannelPairing(Long logisticsChannelPairingId);

    List<UpstreamSkuSyncItem> selectSkuSyncList(String connectionCode, String status, String pairingStatus,
        String field, String keyword);

    List<UpstreamSkuPairing> selectSkuPairingList(String connectionCode);

    int insertSkuPairing(String connectionCode, SkuPairingRequest request);

    int deleteSkuPairing(Long skuPairingId);

    UpstreamSkuSyncState selectSkuSyncState(String connectionCode);

    List<UpstreamRequestLog> selectRequestLogList(String connectionCode);
}
