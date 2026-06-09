package com.ruoyi.integration.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.integration.domain.SourceOfficialWarehouseOption;
import com.ruoyi.integration.domain.SourceProductBindingSnapshot;
import com.ruoyi.integration.domain.SourceSkuPairingProjection;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.service.ISourceSkuPairingProjectionService;

/**
 * Integration-owned implementation for source SKU pairing projections.
 */
@Service
public class SourceSkuPairingProjectionServiceImpl implements ISourceSkuPairingProjectionService
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Override
    public List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroup(
        String sourceDimensionGroupKey)
    {
        if (StringUtils.isBlank(sourceDimensionGroupKey))
        {
            return List.of();
        }
        List<SourceOfficialWarehouseOption> result =
            upstreamSystemMapper.selectOfficialWarehousesBySourceDimensionGroup(sourceDimensionGroupKey);
        return result == null ? List.of() : result;
    }

    @Override
    public List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroups(
        List<String> sourceDimensionGroupKeys)
    {
        if (sourceDimensionGroupKeys == null || sourceDimensionGroupKeys.isEmpty())
        {
            return List.of();
        }
        List<String> keys = sourceDimensionGroupKeys.stream().filter(StringUtils::isNotBlank).distinct()
            .collect(Collectors.toList());
        if (keys.isEmpty())
        {
            return List.of();
        }
        List<SourceOfficialWarehouseOption> result =
            upstreamSystemMapper.selectOfficialWarehousesBySourceDimensionGroups(keys);
        return result == null ? List.of() : result;
    }

    @Override
    public SourceProductBindingSnapshot selectOfficialSourceBindingSnapshot(String sourceDimensionGroupKey)
    {
        if (StringUtils.isBlank(sourceDimensionGroupKey))
        {
            return null;
        }
        return upstreamSystemMapper.selectOfficialSourceBindingSnapshot(sourceDimensionGroupKey);
    }

    @Override
    public List<SourceProductBindingSnapshot> selectOfficialSourceBindingSnapshots(List<String> sourceDimensionGroupKeys)
    {
        if (sourceDimensionGroupKeys == null || sourceDimensionGroupKeys.isEmpty())
        {
            return List.of();
        }
        List<String> keys = sourceDimensionGroupKeys.stream().filter(StringUtils::isNotBlank).distinct()
            .collect(Collectors.toList());
        if (keys.isEmpty())
        {
            return List.of();
        }
        List<SourceProductBindingSnapshot> result = upstreamSystemMapper.selectOfficialSourceBindingSnapshots(keys);
        return result == null ? List.of() : result;
    }

    @Override
    public List<String> selectSourceConnectionCodesByDimensionGroup(String sourceDimensionGroupKey)
    {
        if (StringUtils.isBlank(sourceDimensionGroupKey))
        {
            return List.of();
        }
        List<String> result = upstreamSystemMapper.selectSourceConnectionCodesByDimensionGroup(sourceDimensionGroupKey);
        return result == null ? List.of() : result;
    }

    @Override
    public List<String> selectPairingConnectionCodesBySystemSkuAndMasterSku(String systemSku, String masterSku)
    {
        if (StringUtils.isBlank(systemSku) || StringUtils.isBlank(masterSku))
        {
            return List.of();
        }
        List<String> result =
            upstreamSystemMapper.selectUpstreamSkuPairingConnectionCodesBySystemSkuAndMasterSku(systemSku, masterSku);
        return result == null ? List.of() : result;
    }

    @Override
    public int deletePairingsBySystemSkuAndConnectionCodes(String systemSku, List<String> connectionCodes)
    {
        if (StringUtils.isBlank(systemSku) || connectionCodes == null || connectionCodes.isEmpty())
        {
            return 0;
        }
        return upstreamSystemMapper.deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes(systemSku, connectionCodes);
    }

    @Override
    public int upsertPairingsForProjection(SourceSkuPairingProjection projection)
    {
        if (projection == null || StringUtils.isBlank(projection.getSourceDimensionGroupKey())
            || StringUtils.isBlank(projection.getSystemSkuCode()))
        {
            return 0;
        }
        return upstreamSystemMapper.upsertUpstreamSkuPairingsForProjection(projection);
    }
}
