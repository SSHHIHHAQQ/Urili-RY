package com.ruoyi.warehouse.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.service.QuoteSchemeWarehouseLookupService;
import com.ruoyi.warehouse.domain.Warehouse;
import com.ruoyi.warehouse.mapper.WarehouseMapper;

/**
 * Warehouse implementation of the finance quote scheme lookup port.
 */
@Service
public class QuoteSchemeWarehouseLookupServiceImpl implements QuoteSchemeWarehouseLookupService
{
    private static final String STATUS_NORMAL = "0";

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Override
    public QuoteSchemeOption selectWarehouseOption(String warehouseCode)
    {
        String normalizedCode = StringUtils.trimToNull(warehouseCode);
        if (normalizedCode == null)
        {
            return null;
        }
        Warehouse warehouse = warehouseMapper.selectWarehouseByCode(normalizedCode);
        if (warehouse == null || !STATUS_NORMAL.equals(warehouse.getStatus()))
        {
            return null;
        }
        return toOption(warehouse);
    }

    @Override
    public List<QuoteSchemeOption> selectWarehouseOptions(String keyword)
    {
        Warehouse query = new Warehouse();
        query.setStatus(STATUS_NORMAL);
        String normalizedKeyword = StringUtils.trimToNull(keyword);
        return warehouseMapper.selectWarehouseList(query).stream()
            .filter(item -> matchesKeyword(item, normalizedKeyword))
            .map(this::toOption)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean matchesKeyword(Warehouse warehouse, String keyword)
    {
        if (keyword == null)
        {
            return true;
        }
        return StringUtils.containsIgnoreCase(warehouse.getWarehouseCode(), keyword)
            || StringUtils.containsIgnoreCase(warehouse.getWarehouseName(), keyword)
            || StringUtils.containsIgnoreCase(warehouse.getWarehouseKind(), keyword);
    }

    private QuoteSchemeOption toOption(Warehouse warehouse)
    {
        QuoteSchemeOption option = new QuoteSchemeOption();
        option.setValue(warehouse.getWarehouseCode());
        option.setCode(warehouse.getWarehouseCode());
        option.setName(warehouse.getWarehouseName());
        option.setKind(warehouse.getWarehouseKind());
        option.setLabel(warehouse.getWarehouseName() + " (" + warehouse.getWarehouseCode() + ")");
        option.setSearchText(String.join(" ",
            StringUtils.defaultString(warehouse.getWarehouseCode()),
            StringUtils.defaultString(warehouse.getWarehouseName()),
            StringUtils.defaultString(warehouse.getWarehouseKind())));
        return option;
    }
}
