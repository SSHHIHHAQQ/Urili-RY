package com.ruoyi.logistics.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.logistics.domain.LogisticsAgg56Connection;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelCandidate;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsCarrierConnection;
import com.ruoyi.logistics.domain.LogisticsCarrierRequestLog;
import com.ruoyi.logistics.domain.LogisticsLabelOrder;
import com.ruoyi.logistics.domain.LogisticsLabelPackage;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;

/**
 * 物流商管理 Mapper。
 */
public interface LogisticsCarrierMapper
{
    List<LogisticsCarrierConnection> selectConnectionList(LogisticsCarrierConnection query);

    LogisticsCarrierConnection selectConnectionByAccountId(@Param("carrierAccountId") Long carrierAccountId);

    LogisticsCarrierConnection selectConnectionByCode(@Param("connectionCode") String connectionCode);

    LogisticsAgg56Connection selectAgg56ConnectionByAccountId(@Param("carrierAccountId") Long carrierAccountId);

    int insertConnection(LogisticsCarrierConnection connection);

    int updateConnectionInfo(LogisticsCarrierConnection connection);

    int updateConnectionStatus(@Param("carrierAccountId") Long carrierAccountId, @Param("status") String status,
        @Param("updateBy") String updateBy);

    int updateConnectionDisplayOrder(@Param("carrierAccountId") Long carrierAccountId,
        @Param("displayOrder") Integer displayOrder, @Param("updateBy") String updateBy);

    int updateConnectionAuthorizeSummary(@Param("carrierAccountId") Long carrierAccountId,
        @Param("credentialStatus") String credentialStatus, @Param("updateBy") String updateBy);

    int updateConnectionChannelSyncTime(@Param("carrierAccountId") Long carrierAccountId,
        @Param("updateBy") String updateBy);

    Integer selectMaxConnectionDisplayOrder();

    int upsertAgg56Connection(LogisticsAgg56Connection connection);

    int upsertChannelCandidate(LogisticsCarrierChannelCandidate item);

    int markMissingChannelCandidates(@Param("carrierAccountId") Long carrierAccountId,
        @Param("syncBatchId") String syncBatchId);

    List<LogisticsCarrierChannelCandidate> selectChannelCandidateList(
        @Param("carrierAccountId") Long carrierAccountId, @Param("status") String status);

    LogisticsCarrierChannelCandidate selectChannelCandidate(@Param("carrierAccountId") Long carrierAccountId,
        @Param("externalChannelCode") String externalChannelCode);

    List<LogisticsSystemChannel> selectSystemChannelList(LogisticsSystemChannel query);

    LogisticsSystemChannel selectSystemChannelByCode(@Param("systemChannelCode") String systemChannelCode);

    int insertSystemChannel(LogisticsSystemChannel channel);

    int updateSystemChannel(LogisticsSystemChannel channel);

    List<LogisticsCarrierChannelMapping> selectChannelMappingList(@Param("carrierAccountId") Long carrierAccountId);

    LogisticsCarrierChannelMapping selectActiveChannelMapping(@Param("carrierAccountId") Long carrierAccountId,
        @Param("systemChannelCode") String systemChannelCode);

    LogisticsCarrierChannelMapping selectChannelMappingById(@Param("carrierAccountId") Long carrierAccountId,
        @Param("mappingId") Long mappingId);

    int insertChannelMapping(LogisticsCarrierChannelMapping mapping);

    int deleteChannelMapping(@Param("carrierAccountId") Long carrierAccountId, @Param("mappingId") Long mappingId);

    int insertLabelOrder(LogisticsLabelOrder order);

    int updateLabelOrderFromProvider(LogisticsLabelOrder order);

    int updateLabelOrderStatus(@Param("labelOrderId") Long labelOrderId, @Param("status") String status,
        @Param("updateBy") String updateBy);

    LogisticsLabelOrder selectLabelOrderByBusinessNo(@Param("businessOrderNo") String businessOrderNo);

    LogisticsLabelOrder selectLabelOrderByProviderOrderNo(@Param("carrierAccountId") Long carrierAccountId,
        @Param("providerOrderNo") String providerOrderNo);

    List<LogisticsLabelOrder> selectLabelOrderList(LogisticsLabelOrder query);

    int deleteLabelPackages(@Param("labelOrderId") Long labelOrderId);

    int insertLabelPackage(LogisticsLabelPackage labelPackage);

    List<LogisticsLabelPackage> selectLabelPackages(@Param("labelOrderId") Long labelOrderId);

    int insertRequestLog(LogisticsCarrierRequestLog log);

    List<LogisticsCarrierRequestLog> selectRequestLogList(@Param("carrierAccountId") Long carrierAccountId);
}
