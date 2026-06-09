package com.ruoyi.integration.service;

import com.ruoyi.integration.domain.WarehouseFact;

/**
 * Warehouse fact lookup port used by integration pairing flows.
 */
public interface IWarehouseFactLookupService
{
    WarehouseFact selectNormalOfficialWarehouseByCode(String warehouseCode);
}
