package com.ruoyi.logistics.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelCandidate;
import com.ruoyi.logistics.domain.LogisticsCarrierChannelMapping;
import com.ruoyi.logistics.domain.LogisticsCarrierConnection;
import com.ruoyi.logistics.domain.LogisticsOption;
import com.ruoyi.logistics.domain.LogisticsSystemChannel;
import com.ruoyi.logistics.domain.LogisticsSystemChannelOrderSetting;
import com.ruoyi.logistics.domain.LogisticsSystemChannelWarehouse;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelCarrierMappingRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelOrderSettingRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelRequest;
import com.ruoyi.logistics.domain.request.LogisticsSystemChannelWarehouseRequest;
import com.ruoyi.logistics.mapper.LogisticsCarrierMapper;
import com.ruoyi.logistics.mapper.LogisticsSystemChannelMapper;
import com.ruoyi.logistics.service.ILogisticsSystemChannelService;
import com.ruoyi.logistics.support.LogisticsConstants;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.service.IWarehouseService;

/**
 * 系统物流渠道服务实现。
 */
@Service
public class LogisticsSystemChannelServiceImpl implements ILogisticsSystemChannelService
{
    private static final String SHIPPER_ADDRESS_WAREHOUSE = "WAREHOUSE";
    private static final String SHIPPER_ADDRESS_EXTERNAL_CODE = "EXTERNAL_CODE";
    private static final String SHIPPER_ADDRESS_OVERRIDE = "OVERRIDE";
    private static final String VALIDATION_STRICT = "STRICT";
    private static final String VALIDATION_WARNING = "WARNING";
    private static final String FULFILLMENT_MODE_CARRIER_LABELING = "CARRIER_LABELING";
    private static final String FULFILLMENT_MODE_DIRECT_WAREHOUSE = "DIRECT_FULFILLMENT_WAREHOUSE";

    @Autowired
    private LogisticsSystemChannelMapper systemChannelMapper;

    @Autowired
    private LogisticsCarrierMapper carrierMapper;

    @Autowired
    private IWarehouseService warehouseService;

    @Override
    public List<LogisticsSystemChannel> selectSystemChannelList(LogisticsSystemChannel query)
    {
        return systemChannelMapper.selectSystemChannelList(query);
    }

    @Override
    public LogisticsSystemChannel selectSystemChannelByCode(String systemChannelCode)
    {
        return requireSystemChannel(systemChannelCode);
    }

    @Override
    public int insertSystemChannel(LogisticsSystemChannelRequest request)
    {
        String systemChannelCode = trimRequired(request.getSystemChannelCode(), "系统渠道代码不能为空");
        if (systemChannelMapper.selectSystemChannelByCode(systemChannelCode) != null)
        {
            throw new ServiceException("系统渠道代码已存在");
        }
        LogisticsSystemChannel channel = buildSystemChannel(systemChannelCode, request);
        channel.setStatus(normalizeStatus(request.getStatus(), LogisticsConstants.STATUS_ENABLED));
        Integer maxOrder = systemChannelMapper.selectMaxDisplayOrder();
        channel.setDisplayOrder(maxOrder == null ? 1 : maxOrder + 1);
        String username = SecurityUtils.getUsername();
        channel.setCreateBy(username);
        channel.setUpdateBy(username);
        return systemChannelMapper.insertSystemChannel(channel);
    }

    @Override
    @Transactional
    public int updateSystemChannel(String systemChannelCode, LogisticsSystemChannelRequest request)
    {
        LogisticsSystemChannel existing = requireSystemChannel(systemChannelCode);
        LogisticsSystemChannel channel = buildSystemChannel(existing.getSystemChannelCode(), request);
        channel.setStatus(normalizeStatus(request.getStatus(), existing.getStatus()));
        channel.setUpdateBy(SecurityUtils.getUsername());
        return systemChannelMapper.updateSystemChannel(channel);
    }

    @Override
    public int updateSystemChannelStatus(String systemChannelCode, String status)
    {
        LogisticsSystemChannel existing = requireSystemChannel(systemChannelCode);
        return systemChannelMapper.updateSystemChannelStatus(existing.getSystemChannelCode(),
            normalizeStatus(status, LogisticsConstants.STATUS_ENABLED), SecurityUtils.getUsername());
    }

    @Override
    public List<LogisticsCarrierChannelMapping> selectCarrierMappingList(String systemChannelCode)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        return systemChannelMapper.selectCarrierMappingList(channel.getSystemChannelCode());
    }

    @Override
    @Transactional
    public int insertCarrierMapping(String systemChannelCode, LogisticsSystemChannelCarrierMappingRequest request)
    {
        LogisticsSystemChannel systemChannel = requireSystemChannel(systemChannelCode);
        if (FULFILLMENT_MODE_DIRECT_WAREHOUSE.equals(systemChannel.getFulfillmentMode()))
        {
            throw new ServiceException("直推履约仓渠道不需要维护物流商映射");
        }
        LogisticsCarrierConnection connection = requireCarrierConnection(request.getCarrierAccountId());
        String externalChannelCode = trimRequired(request.getExternalChannelCode(), "物流商渠道代码不能为空");
        LogisticsCarrierChannelCandidate candidate = carrierMapper.selectChannelCandidate(
            connection.getCarrierAccountId(), externalChannelCode);
        if (candidate == null)
        {
            throw new ServiceException("物流商渠道不存在，请先在物流商管理中同步渠道");
        }
        if (!LogisticsConstants.ITEM_ACTIVE.equals(candidate.getStatus()))
        {
            throw new ServiceException("物流商渠道不是可映射状态");
        }

        LogisticsCarrierChannelMapping mapping = new LogisticsCarrierChannelMapping();
        mapping.setCarrierAccountId(connection.getCarrierAccountId());
        mapping.setConnectionCode(connection.getConnectionCode());
        mapping.setExternalChannelCode(externalChannelCode);
        mapping.setExternalChannelNameSnapshot(candidate.getExternalChannelName());
        mapping.setSystemChannelCode(systemChannel.getSystemChannelCode());
        mapping.setSystemChannelNameSnapshot(systemChannel.getSystemChannelName());
        mapping.setStandardCarrierCode(trimRequired(request.getStandardCarrierCode(), "标准最终承运商不能为空"));
        mapping.setStatus(LogisticsConstants.STATUS_ENABLED);
        mapping.setCreateBy(SecurityUtils.getUsername());
        mapping.setRemark(trimOptional(request.getRemark()));
        try
        {
            return carrierMapper.insertChannelMapping(mapping);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("该物流商渠道已绑定系统渠道");
        }
    }

    @Override
    public int deleteCarrierMapping(String systemChannelCode, Long mappingId)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        requireMappingId(mappingId);
        if (systemChannelMapper.selectCarrierMappingById(channel.getSystemChannelCode(), mappingId) == null)
        {
            throw new ServiceException("物流商渠道映射不存在");
        }
        return systemChannelMapper.deleteCarrierMapping(channel.getSystemChannelCode(), mappingId);
    }

    @Override
    public List<LogisticsSystemChannelWarehouse> selectWarehouseBindingList(String systemChannelCode)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        return systemChannelMapper.selectWarehouseBindingList(channel.getSystemChannelCode());
    }

    @Override
    public int insertWarehouseBinding(String systemChannelCode, LogisticsSystemChannelWarehouseRequest request)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        LogisticsSystemChannelWarehouse binding = buildWarehouseBinding(channel.getSystemChannelCode(), null, request);
        binding.setCreateBy(SecurityUtils.getUsername());
        try
        {
            return systemChannelMapper.insertWarehouseBinding(binding);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("该仓库已绑定当前系统渠道");
        }
    }

    @Override
    public int updateWarehouseBinding(String systemChannelCode, Long bindingId,
        LogisticsSystemChannelWarehouseRequest request)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        requireBinding(channel.getSystemChannelCode(), bindingId);
        LogisticsSystemChannelWarehouse binding = buildWarehouseBinding(channel.getSystemChannelCode(), bindingId, request);
        binding.setUpdateBy(SecurityUtils.getUsername());
        try
        {
            return systemChannelMapper.updateWarehouseBinding(binding);
        }
        catch (DuplicateKeyException ex)
        {
            throw new ServiceException("该仓库已绑定当前系统渠道");
        }
    }

    @Override
    public int deleteWarehouseBinding(String systemChannelCode, Long bindingId)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        requireBinding(channel.getSystemChannelCode(), bindingId);
        return systemChannelMapper.deleteWarehouseBinding(channel.getSystemChannelCode(), bindingId);
    }

    @Override
    public LogisticsSystemChannelOrderSetting selectOrderSetting(String systemChannelCode)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        return systemChannelMapper.selectOrderSetting(channel.getSystemChannelCode());
    }

    @Override
    public int saveOrderSetting(String systemChannelCode, LogisticsSystemChannelOrderSettingRequest request)
    {
        LogisticsSystemChannel channel = requireSystemChannel(systemChannelCode);
        validateWeightRange(request.getMinWeight(), request.getMaxWeight());
        LogisticsSystemChannelOrderSetting setting = new LogisticsSystemChannelOrderSetting();
        setting.setSystemChannelCode(channel.getSystemChannelCode());
        setting.setDestinationCountries(trimOptional(request.getDestinationCountries()));
        setting.setMinWeight(request.getMinWeight());
        setting.setMaxWeight(request.getMaxWeight());
        setting.setMaxLength(request.getMaxLength());
        setting.setMaxWidth(request.getMaxWidth());
        setting.setMaxHeight(request.getMaxHeight());
        setting.setMaxGirth(request.getMaxGirth());
        setting.setSignatureService(trimOptional(request.getSignatureService()));
        setting.setValidationMode(normalizeValidationMode(request.getValidationMode()));
        setting.setCreateBy(SecurityUtils.getUsername());
        setting.setUpdateBy(SecurityUtils.getUsername());
        setting.setRemark(trimOptional(request.getRemark()));
        return systemChannelMapper.upsertOrderSetting(setting);
    }

    @Override
    public List<LogisticsOption> selectCarrierAccountOptions(String keyword)
    {
        LogisticsCarrierConnection query = new LogisticsCarrierConnection();
        query.setStatus(LogisticsConstants.STATUS_ENABLED);
        List<LogisticsOption> options = new ArrayList<>();
        for (LogisticsCarrierConnection item : carrierMapper.selectConnectionList(query))
        {
            if (matches(keyword, item.getCarrierName(), item.getProviderKind(), String.valueOf(item.getCarrierAccountId())))
            {
                options.add(option(item.getCarrierName() + "（" + item.getProviderKind() + "）",
                    String.valueOf(item.getCarrierAccountId()), item.getProviderKind()));
            }
        }
        return options;
    }

    @Override
    public List<LogisticsOption> selectCarrierChannelOptions(Long carrierAccountId, String keyword)
    {
        requireCarrierConnection(carrierAccountId);
        List<LogisticsOption> options = new ArrayList<>();
        for (LogisticsCarrierChannelCandidate item : carrierMapper.selectChannelCandidateList(carrierAccountId,
            LogisticsConstants.ITEM_ACTIVE))
        {
            if (matches(keyword, item.getExternalChannelName(), item.getExternalChannelCode()))
            {
                options.add(option(item.getExternalChannelName() + "（" + item.getExternalChannelCode() + "）",
                    item.getExternalChannelCode(), item.getRawFinalCarrierText()));
            }
        }
        return options;
    }

    @Override
    public List<LogisticsOption> selectWarehouseOptions(String keyword)
    {
        Warehouse query = new Warehouse();
        query.setStatus(UserConstants.NORMAL);
        List<Warehouse> warehouses = new ArrayList<>();
        warehouses.addAll(warehouseService.selectOfficialWarehouseList(query));
        warehouses.addAll(warehouseService.selectThirdPartyWarehouseList(query));

        List<LogisticsOption> options = new ArrayList<>();
        for (Warehouse item : warehouses)
        {
            if (matches(keyword, item.getWarehouseCode(), item.getWarehouseName(), item.getWarehouseKind()))
            {
                options.add(option(item.getWarehouseName() + "（" + item.getWarehouseCode() + "）",
                    String.valueOf(item.getWarehouseId()), item.getWarehouseKind()));
            }
        }
        return options;
    }

    private LogisticsSystemChannel buildSystemChannel(String systemChannelCode, LogisticsSystemChannelRequest request)
    {
        LogisticsSystemChannel channel = new LogisticsSystemChannel();
        channel.setSystemChannelCode(systemChannelCode);
        channel.setSystemChannelName(trimRequired(request.getSystemChannelName(), "系统渠道名称不能为空"));
        channel.setFulfillmentMode(normalizeFulfillmentMode(request.getFulfillmentMode()));
        channel.setStandardCarrierCode(trimRequired(request.getStandardCarrierCode(), "标准最终承运商不能为空"));
        channel.setSignatureServices(trimOptional(request.getSignatureServices()));
        channel.setRemark(trimOptional(request.getRemark()));
        return channel;
    }

    private LogisticsSystemChannelWarehouse buildWarehouseBinding(String systemChannelCode, Long bindingId,
        LogisticsSystemChannelWarehouseRequest request)
    {
        Warehouse warehouse = warehouseService.selectWarehouseById(request.getWarehouseId());
        if (!UserConstants.NORMAL.equals(warehouse.getStatus()))
        {
            throw new ServiceException("仓库已停用：" + warehouse.getWarehouseName());
        }
        String mode = normalizeShipperAddressMode(request.getShipperAddressMode());
        LogisticsSystemChannelWarehouse binding = new LogisticsSystemChannelWarehouse();
        binding.setBindingId(bindingId);
        binding.setSystemChannelCode(systemChannelCode);
        binding.setWarehouseId(warehouse.getWarehouseId());
        binding.setWarehouseCode(warehouse.getWarehouseCode());
        binding.setWarehouseName(warehouse.getWarehouseName());
        binding.setWarehouseKind(warehouse.getWarehouseKind());
        binding.setStatus(normalizeStatus(request.getStatus(), LogisticsConstants.STATUS_ENABLED));
        binding.setShipperAddressMode(mode);
        fillShipperAddress(binding, mode, request);
        binding.setRemark(trimOptional(request.getRemark()));
        return binding;
    }

    private void fillShipperAddress(LogisticsSystemChannelWarehouse binding, String mode,
        LogisticsSystemChannelWarehouseRequest request)
    {
        String externalShipperCode = trimOptional(request.getExternalShipperCode());
        binding.setExternalShipperCode(externalShipperCode);
        binding.setShipperCompanyName(trimOptional(request.getShipperCompanyName()));
        binding.setShipperContactName(trimOptional(request.getShipperContactName()));
        binding.setShipperContactPhone(trimOptional(request.getShipperContactPhone()));
        binding.setShipperContactEmail(trimOptional(request.getShipperContactEmail()));
        binding.setShipperCountryCode(trimOptional(request.getShipperCountryCode()));
        binding.setShipperStateProvince(trimOptional(request.getShipperStateProvince()));
        binding.setShipperCity(trimOptional(request.getShipperCity()));
        binding.setShipperPostalCode(trimOptional(request.getShipperPostalCode()));
        binding.setShipperAddressLine1(trimOptional(request.getShipperAddressLine1()));
        binding.setShipperAddressLine2(trimOptional(request.getShipperAddressLine2()));
    }

    private LogisticsSystemChannel requireSystemChannel(String systemChannelCode)
    {
        String normalizedCode = trimRequired(systemChannelCode, "系统渠道代码不能为空");
        LogisticsSystemChannel channel = systemChannelMapper.selectSystemChannelByCode(normalizedCode);
        if (channel == null)
        {
            throw new ServiceException("系统渠道不存在");
        }
        return channel;
    }

    private LogisticsCarrierConnection requireCarrierConnection(Long carrierAccountId)
    {
        if (carrierAccountId == null || carrierAccountId.longValue() <= 0)
        {
            throw new ServiceException("物流商账号不能为空");
        }
        LogisticsCarrierConnection connection = carrierMapper.selectConnectionByAccountId(carrierAccountId);
        if (connection == null)
        {
            throw new ServiceException("物流商账号不存在");
        }
        if (!LogisticsConstants.STATUS_ENABLED.equals(connection.getStatus()))
        {
            throw new ServiceException("物流商账号已停用");
        }
        return connection;
    }

    private LogisticsSystemChannelWarehouse requireBinding(String systemChannelCode, Long bindingId)
    {
        if (bindingId == null || bindingId.longValue() <= 0)
        {
            throw new ServiceException("仓库绑定ID不能为空");
        }
        LogisticsSystemChannelWarehouse binding = systemChannelMapper.selectWarehouseBindingById(systemChannelCode, bindingId);
        if (binding == null)
        {
            throw new ServiceException("仓库绑定不存在");
        }
        return binding;
    }

    private Long requireMappingId(Long mappingId)
    {
        if (mappingId == null || mappingId.longValue() <= 0)
        {
            throw new ServiceException("映射ID不能为空");
        }
        return mappingId;
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

    private String normalizeFulfillmentMode(String value)
    {
        String mode = StringUtils.defaultIfBlank(trimOptional(value), FULFILLMENT_MODE_CARRIER_LABELING)
            .toUpperCase(Locale.ROOT);
        if (!FULFILLMENT_MODE_CARRIER_LABELING.equals(mode) && !FULFILLMENT_MODE_DIRECT_WAREHOUSE.equals(mode))
        {
            throw new ServiceException("渠道履约模式只能是 CARRIER_LABELING 或 DIRECT_FULFILLMENT_WAREHOUSE");
        }
        return mode;
    }

    private String normalizeShipperAddressMode(String value)
    {
        String mode = StringUtils.defaultIfBlank(trimOptional(value), SHIPPER_ADDRESS_WAREHOUSE).toUpperCase(Locale.ROOT);
        if (!SHIPPER_ADDRESS_WAREHOUSE.equals(mode) && !SHIPPER_ADDRESS_EXTERNAL_CODE.equals(mode)
            && !SHIPPER_ADDRESS_OVERRIDE.equals(mode))
        {
            throw new ServiceException("发货地址模式只能是 WAREHOUSE、EXTERNAL_CODE 或 OVERRIDE");
        }
        return mode;
    }

    private String normalizeValidationMode(String value)
    {
        String mode = StringUtils.defaultIfBlank(trimOptional(value), VALIDATION_STRICT).toUpperCase(Locale.ROOT);
        if (!VALIDATION_STRICT.equals(mode) && !VALIDATION_WARNING.equals(mode))
        {
            throw new ServiceException("下单校验模式只能是 STRICT 或 WARNING");
        }
        return mode;
    }

    private void validateWeightRange(BigDecimal minWeight, BigDecimal maxWeight)
    {
        if (minWeight != null && maxWeight != null && minWeight.compareTo(maxWeight) > 0)
        {
            throw new ServiceException("最小重量不能大于最大重量");
        }
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
