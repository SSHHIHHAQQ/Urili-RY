package com.ruoyi.integration.service;

import java.util.List;
import com.ruoyi.integration.domain.SourceOfficialWarehouseOption;
import com.ruoyi.integration.domain.SourceProductBindingSnapshot;
import com.ruoyi.integration.domain.SourceSkuPairingProjection;

/**
 * Stable integration-side port for source SKU pairing projections.
 */
public interface ISourceSkuPairingProjectionService
{
    List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroup(String sourceDimensionGroupKey);

    List<SourceOfficialWarehouseOption> selectOfficialWarehousesBySourceDimensionGroups(
        List<String> sourceDimensionGroupKeys);

    SourceProductBindingSnapshot selectOfficialSourceBindingSnapshot(String sourceDimensionGroupKey);

    List<SourceProductBindingSnapshot> selectOfficialSourceBindingSnapshots(List<String> sourceDimensionGroupKeys);

    List<String> selectSourceConnectionCodesByDimensionGroup(String sourceDimensionGroupKey);

    List<String> selectPairingConnectionCodesBySystemSkuAndMasterSku(String systemSku, String masterSku);

    int deletePairingsBySystemSkuAndConnectionCodes(String systemSku, List<String> connectionCodes);

    int upsertPairingsForProjection(SourceSkuPairingProjection projection);
}
