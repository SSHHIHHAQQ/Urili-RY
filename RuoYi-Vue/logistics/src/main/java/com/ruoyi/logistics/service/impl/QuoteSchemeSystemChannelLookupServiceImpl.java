package com.ruoyi.logistics.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.service.QuoteSchemeSystemChannelLookupService;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.mapper.LogisticsSystemChannelMapper;
import com.ruoyi.logistics.support.LogisticsConstants;

/**
 * Logistics implementation of the finance quote scheme system channel lookup port.
 */
@Service
public class QuoteSchemeSystemChannelLookupServiceImpl implements QuoteSchemeSystemChannelLookupService
{
    @Autowired
    private LogisticsSystemChannelMapper systemChannelMapper;

    @Override
    public QuoteSchemeOption selectSystemChannelOption(String systemChannelCode)
    {
        String normalizedCode = StringUtils.trimToNull(systemChannelCode);
        if (normalizedCode == null)
        {
            return null;
        }
        LogisticsSystemChannel channel = systemChannelMapper.selectSystemChannelByCode(normalizedCode);
        if (channel == null || !LogisticsConstants.STATUS_ENABLED.equals(channel.getStatus()))
        {
            return null;
        }
        return toOption(channel);
    }

    @Override
    public List<QuoteSchemeOption> selectSystemChannelOptions(String keyword)
    {
        LogisticsSystemChannel query = new LogisticsSystemChannel();
        query.setStatus(LogisticsConstants.STATUS_ENABLED);
        String normalizedKeyword = StringUtils.trimToNull(keyword);
        return systemChannelMapper.selectSystemChannelList(query).stream()
            .filter(item -> matchesKeyword(item, normalizedKeyword))
            .map(this::toOption)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean matchesKeyword(LogisticsSystemChannel channel, String keyword)
    {
        if (keyword == null)
        {
            return true;
        }
        return StringUtils.containsIgnoreCase(channel.getSystemChannelCode(), keyword)
            || StringUtils.containsIgnoreCase(channel.getSystemChannelName(), keyword)
            || StringUtils.containsIgnoreCase(channel.getFulfillmentMode(), keyword)
            || StringUtils.containsIgnoreCase(channel.getStandardCarrierCode(), keyword);
    }

    private QuoteSchemeOption toOption(LogisticsSystemChannel channel)
    {
        QuoteSchemeOption option = new QuoteSchemeOption();
        option.setValue(channel.getSystemChannelCode());
        option.setCode(channel.getSystemChannelCode());
        option.setName(channel.getSystemChannelName());
        option.setKind(channel.getFulfillmentMode());
        option.setLabel(channel.getSystemChannelName() + " (" + channel.getSystemChannelCode() + ")");
        option.setSearchText(String.join(" ",
            StringUtils.defaultString(channel.getSystemChannelCode()),
            StringUtils.defaultString(channel.getSystemChannelName()),
            StringUtils.defaultString(channel.getFulfillmentMode()),
            StringUtils.defaultString(channel.getStandardCarrierCode())));
        return option;
    }
}
