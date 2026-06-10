import { Button, Dropdown, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DownOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useState } from 'react';
import { getInventoryOverviewWarehouses } from '@/services/inventory/overview';
import InventorySyncPolicyButton, { InventorySyncPolicyModal } from '@/components/InventorySyncPolicy/InventorySyncPolicyButton';
import { InventoryAdjustModal } from '@/components/InventoryAdjust/InventoryAdjustButton';
import {
  formatDateTime,
  formatQuantity,
  renderSyncMode,
  renderStatus,
  renderWarehouseKind,
} from '../helpers';
import styles from '../style.module.css';
import InventoryAdjustButton from './InventoryAdjustButton';
import QuantityCell from './QuantityCell';

const WAREHOUSE_TABLE_SCROLL_X = 1810;
const SKU_COLUMN_WIDTH = 230;
const OPERATION_COLUMN_WIDTH = 220;

export type WarehouseStockTableRow = API.InventoryOverview.WarehouseStock & {
  skuGroupStart?: boolean;
  skuGroupFirst?: boolean;
  skuOverview?: API.InventoryOverview.OverviewItem;
  skuGroupRows?: API.InventoryOverview.WarehouseStock[];
};

function WarehouseOperationCell({
  record,
  showSkuAdjust,
  canAdjust,
  canSync,
  sellerOptions,
  warehouseOptions,
  onChanged,
}: {
  record: WarehouseStockTableRow;
  showSkuAdjust: boolean;
  canAdjust: boolean;
  canSync: boolean;
  sellerOptions: API.InventoryOverview.SellerOption[];
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
  onChanged: () => void;
}) {
  const [skuAdjustOpen, setSkuAdjustOpen] = useState(false);
  const [skuSyncOpen, setSkuSyncOpen] = useState(false);
  const hasSkuActions = showSkuAdjust && record.skuGroupFirst;
  const moreItems = hasSkuActions
    ? [
        { key: 'adjustSku', label: '调整SKU', disabled: !canAdjust },
        { key: 'syncSku', label: '同步SKU', disabled: !canSync },
      ]
    : [];

  return (
    <>
      <Space size={8} className={styles.overviewWarehouseActions}>
        <InventoryAdjustButton
          scope="WAREHOUSE"
          warehouseRecord={record}
          canAdjust={canAdjust}
          onChanged={onChanged}
        />
        <InventorySyncPolicyButton
          initialScope="SKU_WAREHOUSE"
          lockScope
          warehouseRecord={record}
          sellerOptions={sellerOptions}
          warehouseOptions={warehouseOptions}
          canSync={canSync}
          onChanged={onChanged}
          buttonText="同步方式"
        />
        {moreItems.length ? (
          <Dropdown
            menu={{
              items: moreItems,
              onClick: ({ key }) => {
                if (key === 'adjustSku') {
                  setSkuAdjustOpen(true);
                } else if (key === 'syncSku') {
                  setSkuSyncOpen(true);
                }
              },
            }}
            trigger={['click']}
          >
            <Button type="link" size="small">
              更多 <DownOutlined />
            </Button>
          </Dropdown>
        ) : null}
      </Space>
      {hasSkuActions ? (
        <>
          <InventoryAdjustModal
            open={skuAdjustOpen}
            onOpenChange={setSkuAdjustOpen}
            scope="SKU"
            overviewRecord={record.skuOverview}
            presetRows={record.skuGroupRows || []}
            canAdjust={canAdjust}
            onChanged={onChanged}
          />
          <InventorySyncPolicyModal
            open={skuSyncOpen}
            onOpenChange={setSkuSyncOpen}
            initialScope="SKU"
            lockScope
            overviewRecord={record.skuOverview}
            sellerOptions={sellerOptions}
            warehouseOptions={warehouseOptions}
            onChanged={onChanged}
          />
        </>
      ) : null}
    </>
  );
}

export function WarehouseStockTable({
  rows,
  loading,
  canAdjust,
  canSync,
  sellerOptions,
  warehouseOptions,
  onChanged,
  showSkuColumn = false,
  showSkuAdjust = false,
  rowClassName,
}: {
  rows: WarehouseStockTableRow[];
  loading?: boolean;
  canAdjust: boolean;
  canSync: boolean;
  sellerOptions: API.InventoryOverview.SellerOption[];
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
  onChanged: () => void;
  showSkuColumn?: boolean;
  showSkuAdjust?: boolean;
  rowClassName?: (record: WarehouseStockTableRow, index: number) => string;
}) {
  const columns: ColumnsType<WarehouseStockTableRow> = [
    ...(showSkuColumn
      ? [
          {
            title: 'SKU信息',
            dataIndex: 'systemSkuCode',
            width: SKU_COLUMN_WIDTH,
            render: (_: unknown, record: WarehouseStockTableRow) => (
              <Space direction="vertical" size={1} className={styles.compactSkuInfo}>
                <Typography.Text strong>{record.systemSkuCode || '-'}</Typography.Text>
                <Typography.Text type="secondary" ellipsis={{ tooltip: record.productName }}>
                  {record.productName || '-'}
                </Typography.Text>
                {record.skuName ? (
                  <Typography.Text type="secondary" ellipsis={{ tooltip: record.skuName }}>
                    {record.skuName}
                  </Typography.Text>
                ) : null}
              </Space>
            ),
          },
        ]
      : []),
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
          disabled={!canAdjust || record.warehouseRefType === 'NO_WAREHOUSE' || record.syncMode === 'AUTO_SOURCE_AVAILABLE'}
          onChanged={onChanged}
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
          onChanged={onChanged}
        />
      ),
    },
    {
      title: '同步方式',
      dataIndex: 'syncMode',
      width: 170,
      render: (_, record) => renderSyncMode(record.syncMode, record.syncPolicyScope, record.syncStatus),
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
    {
      title: '操作',
      dataIndex: 'operation',
      width: showSkuAdjust ? OPERATION_COLUMN_WIDTH : 170,
      render: (_, record) => (
        <WarehouseOperationCell
          record={record}
          showSkuAdjust={showSkuAdjust}
          canAdjust={canAdjust}
          canSync={canSync}
          sellerOptions={sellerOptions}
          warehouseOptions={warehouseOptions}
          onChanged={onChanged}
        />
      ),
    },
  ];

  return (
    <div className={styles.overviewWarehouseTable}>
      <Table<WarehouseStockTableRow>
        rowKey="stockId"
        size="small"
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={false}
        rowClassName={rowClassName}
        scroll={{ x: showSkuColumn ? WAREHOUSE_TABLE_SCROLL_X + SKU_COLUMN_WIDTH : WAREHOUSE_TABLE_SCROLL_X }}
        tableLayout="fixed"
      />
    </div>
  );
}

export default function SkuWarehouseTable({
  skuId,
  canAdjust,
  canSync,
  sellerOptions,
  warehouseOptions,
  onChanged,
}: {
  skuId?: number;
  canAdjust: boolean;
  canSync: boolean;
  sellerOptions: API.InventoryOverview.SellerOption[];
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
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

  return (
    <div className={styles.overviewNestedTable}>
      <WarehouseStockTable
        rows={rows}
        loading={loading}
        canAdjust={canAdjust}
        canSync={canSync}
        sellerOptions={sellerOptions}
        warehouseOptions={warehouseOptions}
        onChanged={handleChanged}
      />
    </div>
  );
}
