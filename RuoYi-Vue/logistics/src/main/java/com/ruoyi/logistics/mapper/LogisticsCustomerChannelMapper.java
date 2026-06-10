package com.ruoyi.logistics.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.logistics.domain.LogisticsCustomerChannel;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelBuyerScope;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelSystemMapping;

/**
 * 客户渠道 Mapper。
 */
public interface LogisticsCustomerChannelMapper
{
    List<LogisticsCustomerChannel> selectCustomerChannelList(LogisticsCustomerChannel query);

    LogisticsCustomerChannel selectCustomerChannelByCode(@Param("customerChannelCode") String customerChannelCode);

    Integer selectMaxDisplayOrder();

    Integer selectMaxSystemMappingDisplayOrder(@Param("customerChannelCode") String customerChannelCode);

    int insertCustomerChannel(LogisticsCustomerChannel channel);

    int updateCustomerChannel(LogisticsCustomerChannel channel);

    int updateCustomerChannelStatus(@Param("customerChannelCode") String customerChannelCode,
        @Param("status") String status, @Param("updateBy") String updateBy);

    int updateBuyerScopeMode(@Param("customerChannelCode") String customerChannelCode,
        @Param("buyerScopeMode") String buyerScopeMode, @Param("updateBy") String updateBy);

    List<LogisticsCustomerChannelSystemMapping> selectSystemMappingList(
        @Param("customerChannelCode") String customerChannelCode);

    LogisticsCustomerChannelSystemMapping selectSystemMappingById(
        @Param("customerChannelCode") String customerChannelCode, @Param("mappingId") Long mappingId);

    int insertSystemMapping(LogisticsCustomerChannelSystemMapping mapping);

    int updateSystemMapping(LogisticsCustomerChannelSystemMapping mapping);

    int deleteSystemMapping(@Param("customerChannelCode") String customerChannelCode,
        @Param("mappingId") Long mappingId);

    List<LogisticsCustomerChannelBuyerScope> selectBuyerScopeList(
        @Param("customerChannelCode") String customerChannelCode);

    int deleteBuyerScope(@Param("customerChannelCode") String customerChannelCode);

    int insertBuyerScope(LogisticsCustomerChannelBuyerScope scope);
}
