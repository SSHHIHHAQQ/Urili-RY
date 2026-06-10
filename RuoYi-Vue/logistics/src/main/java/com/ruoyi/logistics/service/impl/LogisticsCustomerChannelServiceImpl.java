package com.ruoyi.logistics.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.logistics.domain.LogisticsCustomerChannel;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelBuyerScope;
import com.ruoyi.logistics.domain.LogisticsCustomerChannelSystemMapping;
import com.ruoyi.logistics.domain.LogisticsOption;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelBuyerScopeRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelRequest;
import com.ruoyi.logistics.domain.request.LogisticsCustomerChannelSystemMappingRequest;
import com.ruoyi.logistics.mapper.LogisticsCustomerChannelMapper;
import com.ruoyi.logistics.mapper.LogisticsSystemChannelMapper;
import com.ruoyi.logistics.service.ILogisticsCustomerChannelService;
import com.ruoyi.logistics.support.LogisticsConstants;

/**
 * 客户渠道服务实现。
 */
@Service
public class LogisticsCustomerChannelServiceImpl implements ILogisticsCustomerChannelService
{
    private static final String CHANNEL_TYPE_WAREHOUSE_LABEL = "WAREHOUSE_LABEL";
    private static final String CHANNEL_TYPE_THIRD_PARTY_LABEL = "THIRD_PARTY_LABEL";
    private static final String LABEL_UPLOAD_REQUIRED = "REQUIRED";
    private static final String LABEL_UPLOAD_NOT_REQUIRED = "NOT_REQUIRED";
    private static final String PLATFORM_LABEL_FETCH = "FETCH";
    private static final String PLATFORM_LABEL_NOT_FETCH = "NOT_FETCH";
    private static final String CUSTOMER_LABEL_SUPPORTED = "SUPPORTED";
    private static final String CUSTOMER_LABEL_UNSUPPORTED = "UNSUPPORTED";
    private static final String BUYER_SCOPE_ALL = "ALL";
    private static final String BUYER_SCOPE_INCLUDE = "INCLUDE";
    private static final String BUYER_SCOPE_EXCLUDE = "EXCLUDE";

    @Autowired
    private LogisticsCustomerChannelMapper customerChannelMapper;

    @Autowired
    private LogisticsSystemChannelMapper systemChannelMapper;

    @Autowired
    private BuyerMapper buyerMapper;

    @Override
    public List<LogisticsCustomerChannel> selectCustomerChannelList(LogisticsCustomerChannel query)
    {
        return customerChannelMapper.selectCustomerChannelList(query);
    }

    @Override
    public LogisticsCustomerChannel selectCustomerChannelByCode(String customerChannelCode)
    {
        return requireCustomerChannel(customerChannelCode);
    }

    @Override
    public int insertCustomerChannel(LogisticsCustomerChannelRequest request)
    {
        String customerChannelCode = trimRequired(request.getCustomerChannelCode(), "客户渠道代码不能为空");
        if (customerChannelMapper.selectCustomerChannelByCode(customerChannelCode) != null)
        {
            throw new ServiceException("客户渠道代码已存在");
        }
        LogisticsCustomerChannel channel = buildCustomerChannel(customerChannelCode, request, null);
        channel.setStatus(normalizeStatus(request.getStatus(), LogisticsConstants.STATUS_ENABLED));
        channel.setBuyerScopeMode(BUYER_SCOPE_ALL);
        Integer maxOrder = customerChannelMapper.selectMaxDisplayOrder();
        channel.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        String username = SecurityUtils.getUsername();
        channel.setCreateBy(username);
        channel.setUpdateBy(username);
        return customerChannelMapper.insertCustomerChannel(channel);
    }

    @Override
    @Transactional
    public int updateCustomerChannel(String customerChannelCode, LogisticsCustomerChannelRequest request)
    {
        LogisticsCustomerChannel existing = requireCustomerChannel(customerChannelCode);
        LogisticsCustomerChannel channel = buildCustomerChannel(existing.getCustomerChannelCode(), request, existing);
        channel.setStatus(normalizeStatus(request.getStatus(), existing.getStatus()));
        channel.setBuyerScopeMode(existing.getBuyerScopeMode());
        channel.setDisplayOrder(existing.getDisplayOrder());
        channel.setUpdateBy(SecurityUtils.getUsername());
        return customerChannelMapper.updateCustomerChannel(channel);
    }

    @Override
    public int updateCustomerChannelStatus(String customerChannelCode, String status)
    {
        LogisticsCustomerChannel existing = requireCustomerChannel(customerChannelCode);
        return customerChannelMapper.updateCustomerChannelStatus(existing.getCustomerChannelCode(),
            normalizeStatus(status, LogisticsConstants.STATUS_ENABLED), SecurityUtils.getUsername());
    }

    @Override
    public List<LogisticsCustomerChannelSystemMapping> selectSystemMappingList(String customerChannelCode)
    {
        LogisticsCustomerChannel channel = requireCustomerChannel(customerChannelCode);
        return customerChannelMapper.selectSystemMappingList(channel.getCustomerChannelCode());
    }

    @Override
    public int insertSystemMapping(String customerChannelCode, LogisticsCustomerChannelSystemMappingRequest request)
    {
        LogisticsCustomerChannel channel = requireCustomerChannel(customerChannelCode);
        LogisticsCustomerChannelSystemMapping mapping = buildSystemMapping(channel.getCustomerChannelCode(), null,
            request, null);
        mapping.setCreateBy(SecurityUtils.getUsername());
        Integer maxOrder = customerChannelMapper.selectMaxSystemMappingDisplayOrder(channel.getCustomerChannelCode());
        mapping.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        try
        {
            return customerChannelMapper.insertSystemMapping(mapping);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("该系统渠道已绑定当前客户渠道");
        }
    }

    @Override
    public int updateSystemMapping(String customerChannelCode, Long mappingId,
        LogisticsCustomerChannelSystemMappingRequest request)
    {
        LogisticsCustomerChannel channel = requireCustomerChannel(customerChannelCode);
        LogisticsCustomerChannelSystemMapping existing = requireSystemMapping(channel.getCustomerChannelCode(), mappingId);
        LogisticsCustomerChannelSystemMapping mapping = buildSystemMapping(channel.getCustomerChannelCode(), mappingId,
            request, existing);
        mapping.setDisplayOrder(existing.getDisplayOrder());
        mapping.setUpdateBy(SecurityUtils.getUsername());
        try
        {
            return customerChannelMapper.updateSystemMapping(mapping);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("该系统渠道已绑定当前客户渠道");
        }
    }

    @Override
    public int deleteSystemMapping(String customerChannelCode, Long mappingId)
    {
        LogisticsCustomerChannel channel = requireCustomerChannel(customerChannelCode);
        requireSystemMapping(channel.getCustomerChannelCode(), mappingId);
        return customerChannelMapper.deleteSystemMapping(channel.getCustomerChannelCode(), mappingId);
    }

    @Override
    public List<LogisticsCustomerChannelBuyerScope> selectBuyerScopeList(String customerChannelCode)
    {
        LogisticsCustomerChannel channel = requireCustomerChannel(customerChannelCode);
        return customerChannelMapper.selectBuyerScopeList(channel.getCustomerChannelCode());
    }

    @Override
    @Transactional
    public int saveBuyerScope(String customerChannelCode, LogisticsCustomerChannelBuyerScopeRequest request)
    {
        LogisticsCustomerChannel channel = requireCustomerChannel(customerChannelCode);
        String scopeMode = normalizeBuyerScopeMode(request.getBuyerScopeMode());
        customerChannelMapper.deleteBuyerScope(channel.getCustomerChannelCode());

        int affected = customerChannelMapper.updateBuyerScopeMode(channel.getCustomerChannelCode(), scopeMode,
            SecurityUtils.getUsername());
        if (BUYER_SCOPE_ALL.equals(scopeMode))
        {
            return affected;
        }

        Set<Long> buyerIds = normalizeBuyerIds(request.getBuyerIds());
        if (buyerIds.isEmpty())
        {
            throw new ServiceException("可用名单或不可用名单至少选择一个买家");
        }

        for (Long buyerId : buyerIds)
        {
            Buyer buyer = buyerMapper.selectBuyerById(buyerId);
            if (buyer == null)
            {
                throw new ServiceException("买家不存在：" + buyerId);
            }
            if (!UserConstants.NORMAL.equals(buyer.getStatus()))
            {
                throw new ServiceException("买家已停用：" + buyer.getBuyerName());
            }
            LogisticsCustomerChannelBuyerScope scope = new LogisticsCustomerChannelBuyerScope();
            scope.setCustomerChannelCode(channel.getCustomerChannelCode());
            scope.setBuyerId(buyer.getBuyerId());
            scope.setBuyerCodeSnapshot(buyer.getBuyerCode());
            scope.setBuyerNameSnapshot(buyer.getBuyerName());
            scope.setBuyerShortNameSnapshot(StringUtils.defaultString(buyer.getBuyerShortName()));
            scope.setCreateBy(SecurityUtils.getUsername());
            customerChannelMapper.insertBuyerScope(scope);
            affected++;
        }
        return affected;
    }

    @Override
    public List<LogisticsOption> selectSystemChannelOptions(String keyword)
    {
        LogisticsSystemChannel query = new LogisticsSystemChannel();
        query.setStatus(LogisticsConstants.STATUS_ENABLED);
        List<LogisticsOption> options = new ArrayList<>();
        for (LogisticsSystemChannel item : systemChannelMapper.selectSystemChannelList(query))
        {
            if (matches(keyword, item.getSystemChannelCode(), item.getSystemChannelName(),
                item.getStandardCarrierCode()))
            {
                LogisticsOption option = option(item.getSystemChannelName() + "（" + item.getSystemChannelCode() + "）",
                    item.getSystemChannelCode(), item.getStandardCarrierCode());
                option.setCode(item.getSystemChannelCode());
                option.setName(item.getSystemChannelName());
                options.add(option);
            }
        }
        return options;
    }

    @Override
    public List<LogisticsOption> selectBuyerOptions(String keyword)
    {
        Buyer query = new Buyer();
        query.setStatus(UserConstants.NORMAL);
        List<LogisticsOption> options = new ArrayList<>();
        for (Buyer item : buyerMapper.selectBuyerList(query))
        {
            if (matches(keyword, item.getBuyerCode(), item.getBuyerName(), item.getBuyerShortName()))
            {
                LogisticsOption option = option(item.getBuyerName() + "（" + item.getBuyerCode() + "）",
                    String.valueOf(item.getBuyerId()), item.getBuyerShortName());
                option.setCode(item.getBuyerCode());
                option.setName(item.getBuyerName());
                option.setShortName(item.getBuyerShortName());
                options.add(option);
            }
        }
        return options;
    }

    private LogisticsCustomerChannel buildCustomerChannel(String customerChannelCode,
        LogisticsCustomerChannelRequest request, LogisticsCustomerChannel existing)
    {
        LogisticsCustomerChannel channel = new LogisticsCustomerChannel();
        channel.setCustomerChannelCode(customerChannelCode);
        channel.setCustomerChannelName(trimRequired(request.getCustomerChannelName(), "客户渠道名称不能为空"));
        channel.setChannelType(normalizeChannelType(request.getChannelType()));
        channel.setStandardCarrierCode(trimRequired(request.getStandardCarrierCode(), "标准最终承运商不能为空"));
        channel.setSignatureServices(trimOptional(request.getSignatureServices()));
        fillLabelSettings(channel, request);
        channel.setDisplayOrder(existing == null ? null : existing.getDisplayOrder());
        channel.setRemark(trimOptional(request.getRemark()));
        return channel;
    }

    private LogisticsCustomerChannelSystemMapping buildSystemMapping(String customerChannelCode, Long mappingId,
        LogisticsCustomerChannelSystemMappingRequest request, LogisticsCustomerChannelSystemMapping existing)
    {
        String systemChannelCode = trimRequired(request.getSystemChannelCode(), "系统渠道代码不能为空");
        LogisticsSystemChannel systemChannel = systemChannelMapper.selectSystemChannelByCode(systemChannelCode);
        if (systemChannel == null)
        {
            throw new ServiceException("系统渠道不存在");
        }
        if (!LogisticsConstants.STATUS_ENABLED.equals(systemChannel.getStatus()))
        {
            throw new ServiceException("只能绑定启用状态的系统渠道");
        }

        LogisticsCustomerChannelSystemMapping mapping = new LogisticsCustomerChannelSystemMapping();
        mapping.setMappingId(mappingId);
        mapping.setCustomerChannelCode(customerChannelCode);
        mapping.setSystemChannelCode(systemChannel.getSystemChannelCode());
        mapping.setSystemChannelNameSnapshot(systemChannel.getSystemChannelName());
        mapping.setStandardCarrierCodeSnapshot(systemChannel.getStandardCarrierCode());
        mapping.setSignatureServicesSnapshot(StringUtils.defaultString(systemChannel.getSignatureServices()));
        mapping.setStatus(normalizeStatus(request.getStatus(), existing == null
            ? LogisticsConstants.STATUS_ENABLED : existing.getStatus()));
        mapping.setDisplayOrder(existing == null ? null : existing.getDisplayOrder());
        mapping.setRemark(trimOptional(request.getRemark()));
        return mapping;
    }

    private void fillLabelSettings(LogisticsCustomerChannel channel, LogisticsCustomerChannelRequest request)
    {
        if (CHANNEL_TYPE_WAREHOUSE_LABEL.equals(channel.getChannelType()))
        {
            channel.setLabelUploadRequired(LABEL_UPLOAD_NOT_REQUIRED);
            channel.setPlatformLabelFetch(PLATFORM_LABEL_NOT_FETCH);
            channel.setCustomerLabelUploadSupported(CUSTOMER_LABEL_UNSUPPORTED);
            return;
        }

        String uploadRequired = normalizeLabelUploadRequired(request.getLabelUploadRequired());
        channel.setLabelUploadRequired(uploadRequired);
        if (LABEL_UPLOAD_NOT_REQUIRED.equals(uploadRequired))
        {
            channel.setPlatformLabelFetch(PLATFORM_LABEL_NOT_FETCH);
            channel.setCustomerLabelUploadSupported(CUSTOMER_LABEL_UNSUPPORTED);
            return;
        }

        String platformFetch = normalizePlatformLabelFetch(request.getPlatformLabelFetch());
        String customerUpload = normalizeCustomerLabelUploadSupported(request.getCustomerLabelUploadSupported());
        if (PLATFORM_LABEL_NOT_FETCH.equals(platformFetch) && CUSTOMER_LABEL_UNSUPPORTED.equals(customerUpload))
        {
            throw new ServiceException("需要上传物流面单时，平台面单获取和客户上传面单至少开启一个");
        }
        channel.setPlatformLabelFetch(platformFetch);
        channel.setCustomerLabelUploadSupported(customerUpload);
    }

    private LogisticsCustomerChannel requireCustomerChannel(String customerChannelCode)
    {
        String normalizedCode = trimRequired(customerChannelCode, "客户渠道代码不能为空");
        LogisticsCustomerChannel channel = customerChannelMapper.selectCustomerChannelByCode(normalizedCode);
        if (channel == null)
        {
            throw new ServiceException("客户渠道不存在");
        }
        return channel;
    }

    private LogisticsCustomerChannelSystemMapping requireSystemMapping(String customerChannelCode, Long mappingId)
    {
        if (mappingId == null || mappingId.longValue() <= 0)
        {
            throw new ServiceException("系统渠道绑定ID不能为空");
        }
        LogisticsCustomerChannelSystemMapping mapping = customerChannelMapper.selectSystemMappingById(customerChannelCode,
            mappingId);
        if (mapping == null)
        {
            throw new ServiceException("系统渠道绑定不存在");
        }
        return mapping;
    }

    private String normalizeStatus(String value, String defaultValue)
    {
        String status = StringUtils.defaultIfBlank(trimOptional(value), defaultValue).toUpperCase(Locale.ROOT);
        if (!LogisticsConstants.STATUS_ENABLED.equals(status) && !LogisticsConstants.STATUS_DISABLED.equals(status))
        {
            throw new ServiceException("状态只能是 ENABLED 或 DISABLED");
        }
        return status;
    }

    private String normalizeChannelType(String value)
    {
        String type = trimRequired(value, "渠道类型不能为空").toUpperCase(Locale.ROOT);
        if (!CHANNEL_TYPE_WAREHOUSE_LABEL.equals(type) && !CHANNEL_TYPE_THIRD_PARTY_LABEL.equals(type))
        {
            throw new ServiceException("渠道类型只能是 WAREHOUSE_LABEL 或 THIRD_PARTY_LABEL");
        }
        return type;
    }

    private String normalizeLabelUploadRequired(String value)
    {
        String status = StringUtils.defaultIfBlank(trimOptional(value), LABEL_UPLOAD_NOT_REQUIRED).toUpperCase(Locale.ROOT);
        if (!LABEL_UPLOAD_REQUIRED.equals(status) && !LABEL_UPLOAD_NOT_REQUIRED.equals(status))
        {
            throw new ServiceException("上传物流面单只能是 REQUIRED 或 NOT_REQUIRED");
        }
        return status;
    }

    private String normalizePlatformLabelFetch(String value)
    {
        String status = StringUtils.defaultIfBlank(trimOptional(value), PLATFORM_LABEL_NOT_FETCH).toUpperCase(Locale.ROOT);
        if (!PLATFORM_LABEL_FETCH.equals(status) && !PLATFORM_LABEL_NOT_FETCH.equals(status))
        {
            throw new ServiceException("平台面单获取只能是 FETCH 或 NOT_FETCH");
        }
        return status;
    }

    private String normalizeCustomerLabelUploadSupported(String value)
    {
        String status = StringUtils.defaultIfBlank(trimOptional(value), CUSTOMER_LABEL_UNSUPPORTED).toUpperCase(Locale.ROOT);
        if (!CUSTOMER_LABEL_SUPPORTED.equals(status) && !CUSTOMER_LABEL_UNSUPPORTED.equals(status))
        {
            throw new ServiceException("客户上传面单支持只能是 SUPPORTED 或 UNSUPPORTED");
        }
        return status;
    }

    private String normalizeBuyerScopeMode(String value)
    {
        String mode = StringUtils.defaultIfBlank(trimOptional(value), BUYER_SCOPE_ALL).toUpperCase(Locale.ROOT);
        if (!BUYER_SCOPE_ALL.equals(mode) && !BUYER_SCOPE_INCLUDE.equals(mode) && !BUYER_SCOPE_EXCLUDE.equals(mode))
        {
            throw new ServiceException("买家范围模式只能是 ALL、INCLUDE 或 EXCLUDE");
        }
        return mode;
    }

    private Set<Long> normalizeBuyerIds(List<Long> buyerIds)
    {
        Set<Long> normalized = new LinkedHashSet<>();
        if (buyerIds == null)
        {
            return normalized;
        }
        for (Long buyerId : buyerIds)
        {
            if (buyerId != null && buyerId.longValue() > 0)
            {
                normalized.add(buyerId);
            }
        }
        return normalized;
    }

    private boolean matches(String keyword, String... values)
    {
        String normalizedKeyword = StringUtils.trimToEmpty(keyword).toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(normalizedKeyword))
        {
            return true;
        }
        for (String value : values)
        {
            if (StringUtils.contains(StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT), normalizedKeyword))
            {
                return true;
            }
        }
        return false;
    }

    private LogisticsOption option(String label, String value, String extra)
    {
        LogisticsOption option = new LogisticsOption();
        option.setLabel(label);
        option.setValue(value);
        option.setExtra(extra);
        option.setSearchText(String.join(" ", StringUtils.defaultString(label), StringUtils.defaultString(value),
            StringUtils.defaultString(extra)));
        return option;
    }

    private String trimRequired(String value, String message)
    {
        String trimmed = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(trimmed))
        {
            throw new ServiceException(message);
        }
        return trimmed;
    }

    private String trimOptional(String value)
    {
        return StringUtils.trimToEmpty(value);
    }
}
