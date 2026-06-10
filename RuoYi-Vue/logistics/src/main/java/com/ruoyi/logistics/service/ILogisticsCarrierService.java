package com.ruoyi.logistics.service;

import java.util.List;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelCandidate;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsCarrierConnection;
import com.ruoyi.logistics.domain.LogisticsCarrierRequestLog;
import com.ruoyi.logistics.domain.LogisticsLabelOrder;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.request.Agg56CredentialRequest;
import com.ruoyi.logistics.domain.request.LogisticsChannelMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsConnectionRequest;
import com.ruoyi.logistics.domain.request.LogisticsCreateLabelRequest;
import com.ruoyi.logistics.domain.request.LogisticsLabelActionRequest;
import com.ruoyi.logistics.domain.request.LogisticsQuoteRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelRequest;
import com.ruoyi.logistics.domain.response.LogisticsQuoteResponse;

/**
 * 物流商管理服务。
 */
public interface ILogisticsCarrierService
{
    List<LogisticsCarrierConnection> selectConnectionList(LogisticsCarrierConnection query);

    LogisticsCarrierConnection selectConnectionByAccountId(Long carrierAccountId);

    int insertConnection(LogisticsConnectionRequest request);

    int updateConnectionInfo(Long carrierAccountId, LogisticsConnectionRequest request);

    int updateAgg56Credentials(Long carrierAccountId, Agg56CredentialRequest request);

    int updateConnectionStatus(Long carrierAccountId, String status);

    int updateConnectionOrder(List<Long> carrierAccountIds);

    int authorize(Long carrierAccountId);

    int syncChannels(Long carrierAccountId);

    List<LogisticsCarrierChannelCandidate> selectChannelCandidateList(Long carrierAccountId, String status);

    List<LogisticsSystemChannel> selectSystemChannelList(LogisticsSystemChannel query);

    int insertSystemChannel(LogisticsSystemChannelRequest request);

    int updateSystemChannel(String systemChannelCode, LogisticsSystemChannelRequest request, String status);

    List<LogisticsCarrierChannelMapping> selectChannelMappingList(Long carrierAccountId);

    int insertChannelMapping(Long carrierAccountId, LogisticsChannelMappingRequest request);

    int deleteChannelMapping(Long carrierAccountId, Long mappingId);

    LogisticsQuoteResponse quote(LogisticsQuoteRequest request);

    LogisticsLabelOrder createLabel(LogisticsCreateLabelRequest request);

    LogisticsLabelOrder getLabel(LogisticsLabelActionRequest request);

    LogisticsLabelOrder cancelLabel(LogisticsLabelActionRequest request);

    List<LogisticsLabelOrder> selectLabelOrderList(LogisticsLabelOrder query);

    List<LogisticsCarrierRequestLog> selectRequestLogList(Long carrierAccountId);
}
