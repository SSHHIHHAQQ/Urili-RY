package com.ruoyi.inventory.service;

import java.util.List;
import com.ruoyi.inventory.domain.InventoryOfficialSourceStock;
import com.ruoyi.inventory.domain.InventorySourceSkuKey;

/**
 * 来源仓库存读模型查询端口。
 */
public interface InventorySourceWarehouseStockLookupService
{
    List<InventorySourceSkuKey> selectAffectedOfficialMasterSkuKeysByConnection(String connectionCode);

    List<InventoryOfficialSourceStock> selectOfficialMasterStocksBySourceSkuKeys(List<InventorySourceSkuKey> sourceKeys);
}
