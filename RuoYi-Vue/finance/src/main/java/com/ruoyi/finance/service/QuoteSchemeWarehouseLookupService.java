package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.QuoteSchemeOption;

/**
 * Warehouse-owned lookup port used by finance quote schemes.
 */
public interface QuoteSchemeWarehouseLookupService
{
    QuoteSchemeOption selectWarehouseOption(String warehouseCode);

    List<QuoteSchemeOption> selectWarehouseOptions(String keyword);
}
