package com.ruoyi.logistics.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.service.QuoteSchemeCustomerChannelLookupService;
import com.ruoyi.logistics.domain.LogisticsCustomerChannel;
import com.ruoyi.logistics.mapper.LogisticsCustomerChannelMapper;
import com.ruoyi.logistics.support.LogisticsConstants;

/**
 * Logistics implementation of the finance quote scheme lookup port.
 */
@Service
public class QuoteSchemeCustomerChannelLookupServiceImpl implements QuoteSchemeCustomerChannelLookupService
{
    @Autowired
    private LogisticsCustomerChannelMapper customerChannelMapper;

    @Override
    public QuoteSchemeOption selectCustomerChannelOption(String customerChannelCode)
    {
        String normalizedCode = StringUtils.trimToNull(customerChannelCode);
        if (normalizedCode == null)
        {
            return null;
        }
        LogisticsCustomerChannel channel = customerChannelMapper.selectCustomerChannelByCode(normalizedCode);
        if (channel == null || !LogisticsConstants.STATUS_ENABLED.equals(channel.getStatus()))
        {
            return null;
        }
        return toOption(channel);
    }

    @Override
    public List<QuoteSchemeOption> selectCustomerChannelOptions(String keyword)
    {
        LogisticsCustomerChannel query = new LogisticsCustomerChannel();
        query.setStatus(LogisticsConstants.STATUS_ENABLED);
        String normalizedKeyword = StringUtils.trimToNull(keyword);
        return customerChannelMapper.selectCustomerChannelList(query).stream()
            .filter(item -> matchesKeyword(item, normalizedKeyword))
            .map(this::toOption)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean matchesKeyword(LogisticsCustomerChannel channel, String keyword)
    {
        if (keyword == null)
        {
            return true;
        }
        return StringUtils.containsIgnoreCase(channel.getCustomerChannelCode(), keyword)
            || StringUtils.containsIgnoreCase(channel.getCustomerChannelName(), keyword)
            || StringUtils.containsIgnoreCase(channel.getChannelType(), keyword)
            || StringUtils.containsIgnoreCase(channel.getStandardCarrierCode(), keyword);
    }

    private QuoteSchemeOption toOption(LogisticsCustomerChannel channel)
    {
        QuoteSchemeOption option = new QuoteSchemeOption();
        option.setValue(channel.getCustomerChannelCode());
        option.setCode(channel.getCustomerChannelCode());
        option.setName(channel.getCustomerChannelName());
        option.setKind(channel.getChannelType());
        option.setLabel(channel.getCustomerChannelName() + " (" + channel.getCustomerChannelCode() + ")");
        option.setSearchText(String.join(" ",
            StringUtils.defaultString(channel.getCustomerChannelCode()),
            StringUtils.defaultString(channel.getCustomerChannelName()),
            StringUtils.defaultString(channel.getChannelType()),
            StringUtils.defaultString(channel.getStandardCarrierCode())));
        return option;
    }
}
