import { Empty, Spin } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { getInventoryOverviewSpuSkuWarehouses } from '@/services/inventory/overview';
import styles from '../style.module.css';
import {
  WarehouseStockTable,
  type WarehouseStockTableRow,
} from './SkuWarehouseTable';

function flattenSkuWarehouseGroups(groups: API.InventoryOverview.SkuWarehouseGroup[]) {
  const rows: WarehouseStockTableRow[] = [];

  groups.forEach((group, groupIndex) => {
    const sku = group.sku || {};
    const warehouses = group.warehouses || [];

    warehouses.forEach((warehouse, warehouseIndex) => {
      rows.push({
        ...warehouse,
        productName: warehouse.productName || sku.productName,
        skuName: warehouse.skuName || sku.skuName,
        systemSkuCode: warehouse.systemSkuCode || sku.systemSkuCode,
        skuGroupStart: groupIndex > 0 && warehouseIndex === 0,
        skuGroupFirst: warehouseIndex === 0,
        skuOverview: sku,
        skuGroupRows: warehouses,
      });
    });
  });

  return rows;
}

export default function SpuSkuWarehouseTable({
  spuId,
  canAdjust,
  canSync,
  sellerOptions,
  warehouseOptions,
  onChanged,
}: {
  spuId?: number;
  canAdjust: boolean;
  canSync: boolean;
  sellerOptions: API.InventoryOverview.SellerOption[];
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
  onChanged: () => void;
}) {
  const [groups, setGroups] = useState<API.InventoryOverview.SkuWarehouseGroup[]>([]);
  const [loading, setLoading] = useState(false);

  const loadGroups = useCallback(async () => {
    if (!spuId) {
      setGroups([]);
      return;
    }
    setLoading(true);
    try {
      const resp = await getInventoryOverviewSpuSkuWarehouses(spuId);
      setGroups(resp.code === 200 ? resp.data || [] : []);
    } finally {
      setLoading(false);
    }
  }, [spuId]);

  const handleChanged = () => {
    void loadGroups();
    onChanged();
  };

  useEffect(() => {
    void loadGroups();
  }, [loadGroups]);

  const rows = useMemo(() => flattenSkuWarehouseGroups(groups), [groups]);

  return (
    <div className={styles.overviewNestedTable}>
      <Spin spinning={loading}>
        {rows.length ? (
          <WarehouseStockTable
            rows={rows}
            canAdjust={canAdjust}
            canSync={canSync}
            sellerOptions={sellerOptions}
            warehouseOptions={warehouseOptions}
            onChanged={handleChanged}
            showSkuColumn
            showSkuAdjust
            rowClassName={(record) => (record.skuGroupStart ? styles.spuSkuWarehouseGroupStart : '')}
          />
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无SKU库存" />
        )}
      </Spin>
    </div>
  );
}
