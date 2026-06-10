package com.ruoyi.logistics.service;

import java.util.List;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsOption;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.LogisticsSystemChannelOrderSetting;
import com.ruoyi.logistics.domain.LogisticsSystemChannelWarehouse;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelCarrierMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelOrderSettingRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelWarehouseRequest;

/**
 * 系统物流渠道服务。
 */
public interface ILogisticsSystemChannelService
{
    List<LogisticsSystemChannel> selectSystemChannelList(LogisticsSystemChannel query);

    LogisticsSystemChannel selectSystemChannelByCode(String systemChannelCode);

    int insertSystemChannel(LogisticsSystemChannelRequest request);

    int updateSystemChannel(String systemChannelCode, LogisticsSystemChannelRequest request);

    int updateSystemChannelStatus(String systemChannelCode, String status);

    List<LogisticsCarrierChannelMapping> selectCarrierMappingList(String systemChannelCode);

    int insertCarrierMapping(String systemChannelCode, LogisticsSystemChannelCarrierMappingRequest request);

    int deleteCarrierMapping(String systemChannelCode, Long mappingId);

    List<LogisticsSystemChannelWarehouse> selectWarehouseBindingList(String systemChannelCode);

    int insertWarehouseBinding(String systemChannelCode, LogisticsSystemChannelWarehouseRequest request);

    int updateWarehouseBinding(String systemChannelCode, Long bindingId, LogisticsSystemChannelWarehouseRequest request);

    int deleteWarehouseBinding(String systemChannelCode, Long bindingId);

    LogisticsSystemChannelOrderSetting selectOrderSetting(String systemChannelCode);

    int saveOrderSetting(String systemChannelCode, LogisticsSystemChannelOrderSettingRequest request);

    List<LogisticsOption> selectCarrierAccountOptions(String keyword);

    List<LogisticsOption> selectCarrierChannelOptions(Long carrierAccountId, String keyword);

    List<LogisticsOption> selectWarehouseOptions(String keyword);

}
