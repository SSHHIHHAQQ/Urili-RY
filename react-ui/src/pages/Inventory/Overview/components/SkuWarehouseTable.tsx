import { Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useState } from 'react';
import { getInventoryOverviewWarehouses } from '@/services/inventory/overview';
import {
  formatDateTime,
  formatQuantity,
  renderStatus,
  renderWarehouseKind,
} from '../helpers';
import styles from '../style.module.css';
import QuantityCell from './QuantityCell';

const WAREHOUSE_TABLE_SCROLL_X = 1420;

export default function SkuWarehouseTable({
  skuId,
  canAdjust,
  onChanged,
}: {
  skuId?: number;
  canAdjust: boolean;
  onChanged: () => void;
}) {
  const [rows, setRows] = useState<API.InventoryOverview.WarehouseStock[]>([]);
  const [loading, setLoading] = useState(false);

  const loadRows = useCallback(async () => {
    if (!skuId) {
      setRows([]);
      return;
    }
    setLoading(true);
    try {
      const resp = await getInventoryOverviewWarehouses(skuId);
      setRows(resp.code === 200 ? resp.data || [] : []);
    } finally {
      setLoading(false);
    }
  }, [skuId]);

  const handleChanged = () => {
    void loadRows();
    onChanged();
  };

  useEffect(() => {
    void loadRows();
  }, [loadRows]);

  const columns: ColumnsType<API.InventoryOverview.WarehouseStock> = [
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 180,
      render: (_, record) => (
        <>
          <Typography.Text>{record.warehouseName || '-'}</Typography.Text>
          {record.warehouseCode ? (
            <>
              <br />
              <Typography.Text type="secondary">{record.warehouseCode}</Typography.Text>
            </>
          ) : null}
        </>
      ),
    },
    {
      title: '类型',
      dataIndex: 'warehouseKind',
      width: 90,
      render: (value) => renderWarehouseKind(String(value || '')),
    },
    {
      title: '来源总库存',
      dataIndex: 'sourceTotalQty',
      width: 110,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceTotalQty) : '-'),
    },
    {
      title: '来源可用库存',
      dataIndex: 'sourceAvailableQty',
      width: 120,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceAvailableQty) : '-'),
    },
    {
      title: '来源在途库存',
      dataIndex: 'sourceInTransitQty',
      width: 120,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceInTransitQty) : '-'),
    },
    {
      title: '平台总库存',
      dataIndex: 'platformTotalQty',
      width: 130,
      align: 'right',
      render: (_, record) => (
        <QuantityCell
          record={record}
          field="PLATFORM_TOTAL"
          value={record.platformTotalQty}
          disabled={!canAdjust || record.warehouseRefType === 'NO_WAREHOUSE'}
          onChanged={handleChanged}
        />
      ),
    },
    {
      title: '平台可售库存',
      dataIndex: 'platformAvailableQty',
      width: 120,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台锁定库存',
      dataIndex: 'platformReservedQty',
      width: 120,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台在途库存',
      dataIndex: 'platformInTransitQty',
      width: 130,
      align: 'right',
      render: (_, record) => (
        <QuantityCell
          record={record}
          field="PLATFORM_IN_TRANSIT"
          value={record.platformInTransitQty}
          disabled={!canAdjust || record.warehouseKind !== 'official'}
          onChanged={handleChanged}
        />
      ),
    },
    {
      title: '状态',
      dataIndex: 'effectiveStatus',
      width: 120,
      render: (value) => renderStatus(String(value || '')),
    },
    {
      title: '同步时间',
      dataIndex: 'sourceSnapshotTime',
      width: 170,
      render: (value) => formatDateTime(value as string),
    },
  ];

  return (
    <div className={styles.overviewNestedTable}>
      <Table<API.InventoryOverview.WarehouseStock>
        rowKey="stockId"
        size="small"
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={false}
        scroll={{ x: WAREHOUSE_TABLE_SCROLL_X }}
        tableLayout="fixed"
      />
    </div>
  );
}
