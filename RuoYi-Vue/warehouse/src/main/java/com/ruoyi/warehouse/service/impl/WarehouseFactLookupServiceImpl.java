package com.ruoyi.warehouse.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.integration.domain.WarehouseFact;
import com.ruoyi.integration.service.IWarehouseFactLookupService;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.mapper.WarehouseMapper;

/**
 * Warehouse fact lookup implementation for integration module pairing validation.
 */
@Service
public class WarehouseFactLookupServiceImpl implements IWarehouseFactLookupService
{
    private static final String KIND_OFFICIAL = "official";

    private static final String STATUS_NORMAL = "0";

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public WarehouseFact selectNormalOfficialWarehouseByCode(String warehouseCode)
    {
        String normalizedCode = StringUtils.trimToNull(warehouseCode);
        if (normalizedCode == null)
        {
            return null;
        }
        Warehouse warehouse = warehouseMapper.selectWarehouseByCode(normalizedCode);
        if (warehouse == null || !KIND_OFFICIAL.equals(warehouse.getWarehouseKind())
            || !STATUS_NORMAL.equals(warehouse.getStatus()))
        {
            return null;
        }
        WarehouseFact fact = new WarehouseFact();
        fact.setWarehouseCode(warehouse.getWarehouseCode());
        fact.setWarehouseName(warehouse.getWarehouseName());
        return fact;
    }
}
