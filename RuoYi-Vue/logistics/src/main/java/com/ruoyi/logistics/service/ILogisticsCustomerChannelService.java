package com.ruoyi.logistics.service;

import java.util.List;
import com.ruoyi.logistics.domain.LogisticsCustomerChannel;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelBuyerScope;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelQuoteMapping;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelSystemMapping;
import com.ruoyi.logistics.domain.LogisticsOption;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelBuyerScopeRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelQuoteMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelSystemMappingRequest;

/**
 * 客户渠道服务。
 */
public interface ILogisticsCustomerChannelService
{
    List<LogisticsCustomerChannel> selectCustomerChannelList(LogisticsCustomerChannel query);

    LogisticsCustomerChannel selectCustomerChannelByCode(String customerChannelCode);

    int insertCustomerChannel(LogisticsCustomerChannelRequest request);

    int updateCustomerChannel(String customerChannelCode, LogisticsCustomerChannelRequest request);

    int updateCustomerChannelStatus(String customerChannelCode, String status);

    List<LogisticsCustomerChannelSystemMapping> selectSystemMappingList(String customerChannelCode);

    int insertSystemMapping(String customerChannelCode, LogisticsCustomerChannelSystemMappingRequest request);

    int updateSystemMapping(String customerChannelCode, Long mappingId,
        LogisticsCustomerChannelSystemMappingRequest request);

    int deleteSystemMapping(String customerChannelCode, Long mappingId);

    List<LogisticsCustomerChannelQuoteMapping> selectQuoteMappingList(String customerChannelCode);

    int insertQuoteMapping(String customerChannelCode, LogisticsCustomerChannelQuoteMappingRequest request);

    int deleteQuoteMapping(String customerChannelCode, Long mappingId);

    List<LogisticsCustomerChannelBuyerScope> selectBuyerScopeList(String customerChannelCode);

    int saveBuyerScope(String customerChannelCode, LogisticsCustomerChannelBuyerScopeRequest request);

    List<LogisticsOption> selectSystemChannelOptions(String keyword);

    List<LogisticsOption> selectBuyerOptions(String keyword);
}
