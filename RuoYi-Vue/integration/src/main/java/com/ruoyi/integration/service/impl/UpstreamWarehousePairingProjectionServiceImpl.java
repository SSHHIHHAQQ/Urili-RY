package com.ruoyi.integration.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.integration.domain.UpstreamWarehousePairingSnapshot;
import com.ruoyi.integration.mapper.UpstreamSystemMapper;
import com.ruoyi.integration.service.IUpstreamWarehousePairingProjectionService;

/**
 * Integration-owned implementation for warehouse pairing projections.
 */
@Service
public class UpstreamWarehousePairingProjectionServiceImpl implements IUpstreamWarehousePairingProjectionService
{
    @Autowired
    private UpstreamSystemMapper upstreamSystemMapper;

    @Override
    public List<UpstreamWarehousePairingSnapshot> selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(
        List<String> systemWarehouseCodes)
    {
        if (systemWarehouseCodes == null || systemWarehouseCodes.isEmpty())
        {
            return List.of();
        }
        List<String> codes = systemWarehouseCodes.stream().filter(StringUtils::isNotBlank).distinct()
            .collect(Collectors.toList());
        if (codes.isEmpty())
        {
            return List.of();
        }
        List<UpstreamWarehousePairingSnapshot> result =
            upstreamSystemMapper.selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(codes);
        return result == null ? List.of() : result;
    }
}
