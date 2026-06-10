package com.ruoyi.logistics.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.LogisticsSystemChannelOrderSetting;
import com.ruoyi.logistics.domain.LogisticsSystemChannelWarehouse;

/**
 * 系统物流渠道 Mapper。
 */
public interface LogisticsSystemChannelMapper
{
    List<LogisticsSystemChannel> selectSystemChannelList(LogisticsSystemChannel query);

    LogisticsSystemChannel selectSystemChannelByCode(@Param("systemChannelCode") String systemChannelCode);

    Integer selectMaxDisplayOrder();

    int insertSystemChannel(LogisticsSystemChannel channel);

    int updateSystemChannel(LogisticsSystemChannel channel);

    int updateSystemChannelStatus(@Param("systemChannelCode") String systemChannelCode, @Param("status") String status,
        @Param("updateBy") String updateBy);

    List<LogisticsCarrierChannelMapping> selectCarrierMappingList(@Param("systemChannelCode") String systemChannelCode);

    LogisticsCarrierChannelMapping selectCarrierMappingById(@Param("systemChannelCode") String systemChannelCode,
        @Param("mappingId") Long mappingId);

    int deleteCarrierMapping(@Param("systemChannelCode") String systemChannelCode, @Param("mappingId") Long mappingId);

    List<LogisticsSystemChannelWarehouse> selectWarehouseBindingList(
        @Param("systemChannelCode") String systemChannelCode);

    LogisticsSystemChannelWarehouse selectWarehouseBindingById(@Param("systemChannelCode") String systemChannelCode,
        @Param("bindingId") Long bindingId);

    int insertWarehouseBinding(LogisticsSystemChannelWarehouse binding);

    int updateWarehouseBinding(LogisticsSystemChannelWarehouse binding);

    int deleteWarehouseBinding(@Param("systemChannelCode") String systemChannelCode, @Param("bindingId") Long bindingId);

    LogisticsSystemChannelOrderSetting selectOrderSetting(@Param("systemChannelCode") String systemChannelCode);

    int upsertOrderSetting(LogisticsSystemChannelOrderSetting setting);
}
