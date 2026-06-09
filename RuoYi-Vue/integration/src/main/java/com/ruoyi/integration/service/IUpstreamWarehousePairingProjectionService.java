package com.ruoyi.integration.service;

import java.util.List;
import com.ruoyi.integration.domain.UpstreamWarehousePairingSnapshot;

/**
 * Stable integration-side port for warehouse pairing projections.
 */
public interface IUpstreamWarehousePairingProjectionService
{
    List<UpstreamWarehousePairingSnapshot> selectActiveWarehousePairingSnapshotsBySystemWarehouseCodes(
        List<String> systemWarehouseCodes);
}
