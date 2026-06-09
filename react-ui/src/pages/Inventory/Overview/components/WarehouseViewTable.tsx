import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Space, Typography } from 'antd';
import type { ReactNode, RefObject } from 'react';
import { getInventoryOverviewWarehouseList } from '@/services/inventory/overview';
import InventorySyncPolicyButton from '@/components/InventorySyncPolicy/InventorySyncPolicyButton';
import {
  getPersistedProTableSearch,
  getProTableColumnsState,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  buildDateRangeSearchColumns,
  buildInventoryOverviewListParams,
  buildQuantityRangeSearchColumns,
  formatDateTime,
  formatQuantity,
  renderSyncMode,
  inventoryStatusValueEnum,
  pairingStatusValueEnum,
  renderStatus,
  renderWarehouseKind,
  SEARCH_FIELD_COUNT,
  TABLE_SCROLL_X,
  syncModeValueEnum,
  warehouseStockKindValueEnum,
} from '../helpers';
import InventoryAdjustButton from './InventoryAdjustButton';
import QuantityCell from './QuantityCell';

export default function WarehouseViewTable({
  actionRef,
  canQuery,
  canAdjust,
  canSync,
  toolbar,
  sellerOptions,
  sellerOptionsLoading,
  warehouseOptions,
  warehouseOptionsLoading,
  syncWarehouseOptions,
}: {
  actionRef: RefObject<ActionType | null>;
  canQuery: boolean;
  canAdjust: boolean;
  canSync: boolean;
  toolbar: ReactNode | ReactNode[];
  sellerOptions: API.InventoryOverview.SellerOption[];
  sellerOptionsLoading: boolean;
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
  warehouseOptionsLoading: boolean;
  syncWarehouseOptions: API.InventoryOverview.WarehouseOption[];
}) {
  const reloadTable = () => {
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.InventoryOverview.WarehouseStock>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '商品名 / SPU / SKU / 仓库' },
    },
    {
      title: '库存状态',
      dataIndex: 'effectiveStatus',
      valueType: 'select',
      valueEnum: inventoryStatusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      hideInTable: true,
    },
    {
      title: '仓库类型',
      dataIndex: 'warehouseKind',
      valueType: 'select',
      valueEnum: warehouseStockKindValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      hideInTable: true,
    },
    {
      title: '卖家',
      dataIndex: 'sellerId',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        allowClear: true,
        loading: sellerOptionsLoading,
        options: sellerOptions,
        placeholder: '请选择',
      },
      hideInTable: true,
    },
    {
      title: '同步方式',
      dataIndex: 'syncMode',
      valueType: 'select',
      valueEnum: syncModeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      hideInTable: true,
    },
    {
      title: '配对状态',
      dataIndex: 'pairingStatus',
      valueType: 'select',
      valueEnum: pairingStatusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      hideInTable: true,
    },
    {
      title: '仓库',
      dataIndex: 'warehouseKey',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        allowClear: true,
        loading: warehouseOptionsLoading,
        options: warehouseOptions,
        placeholder: '请选择',
      },
      hideInTable: true,
    },
    ...buildQuantityRangeSearchColumns<API.InventoryOverview.WarehouseStock>(),
    ...buildDateRangeSearchColumns<API.InventoryOverview.WarehouseStock>(),
    {
      title: 'SKU信息',
      dataIndex: 'systemSkuCode',
      width: 280,
      fixed: 'left',
      search: false,
      render: (_, record) => (
        <>
          <Typography.Text strong>{record.systemSkuCode || '-'}</Typography.Text>
          <br />
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.productName }}>
            {record.productName || '-'}
          </Typography.Text>
          {record.skuName ? (
            <>
              <br />
              <Typography.Text type="secondary">{record.skuName}</Typography.Text>
            </>
          ) : null}
        </>
      ),
    },
    {
      title: '卖家',
      dataIndex: 'sellerName',
      width: 170,
      search: false,
      render: (_, record) => (
        <>
          <Typography.Text ellipsis={{ tooltip: record.sellerName }}>{record.sellerName || '-'}</Typography.Text>
          {record.sellerNo ? (
            <>
              <br />
              <Typography.Text type="secondary">{record.sellerNo}</Typography.Text>
            </>
          ) : null}
        </>
      ),
    },
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 180,
      search: false,
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
      search: false,
      render: (value) => renderWarehouseKind(String(value || '')),
    },
    {
      title: '平台总库存',
      dataIndex: 'platformTotalQty',
      width: 130,
      search: false,
      align: 'right',
      render: (_, record) => (
        <QuantityCell
          record={record}
          field="PLATFORM_TOTAL"
          value={record.platformTotalQty}
          disabled={!canAdjust || record.warehouseRefType === 'NO_WAREHOUSE' || record.syncMode === 'AUTO_SOURCE_AVAILABLE'}
          onChanged={reloadTable}
        />
      ),
    },
    {
      title: '平台可售库存',
      dataIndex: 'platformAvailableQty',
      width: 120,
      search: false,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台锁定库存',
      dataIndex: 'platformReservedQty',
      width: 120,
      search: false,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台在途库存',
      dataIndex: 'platformInTransitQty',
      width: 130,
      search: false,
      align: 'right',
      render: (_, record) => (
        <QuantityCell
          record={record}
          field="PLATFORM_IN_TRANSIT"
          value={record.platformInTransitQty}
          disabled={!canAdjust || record.warehouseKind !== 'official'}
          onChanged={reloadTable}
        />
      ),
    },
    {
      title: '来源总库存',
      dataIndex: 'sourceTotalQty',
      width: 110,
      search: false,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceTotalQty) : '-'),
    },
    {
      title: '来源可用库存',
      dataIndex: 'sourceAvailableQty',
      width: 120,
      search: false,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceAvailableQty) : '-'),
    },
    {
      title: '来源在途库存',
      dataIndex: 'sourceInTransitQty',
      width: 120,
      search: false,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceInTransitQty) : '-'),
    },
    {
      title: '同步方式',
      dataIndex: 'syncMode',
      width: 170,
      search: false,
      render: (_, record) => renderSyncMode(record.syncMode, record.syncPolicyScope, record.syncStatus),
    },
    {
      title: '状态',
      dataIndex: 'effectiveStatus',
      width: 120,
      search: false,
      render: (value) => renderStatus(String(value || '')),
    },
    {
      title: '同步时间',
      dataIndex: 'sourceSnapshotTime',
      width: 170,
      search: false,
      render: (value) => formatDateTime(value as string),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 170,
      search: false,
      render: (value) => formatDateTime(value as string),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 170,
      render: (_, record) => (
        <Space size={4}>
          <InventoryAdjustButton
            scope="WAREHOUSE"
            warehouseRecord={record}
            canAdjust={canAdjust}
            onChanged={reloadTable}
          />
          <InventorySyncPolicyButton
            initialScope="SKU_WAREHOUSE"
            lockScope
            warehouseRecord={record}
            sellerOptions={sellerOptions}
            warehouseOptions={syncWarehouseOptions}
            canSync={canSync}
            onChanged={reloadTable}
            buttonText="同步方式"
          />
        </Space>
      ),
    },
  ];

  return (
    <ProTable<API.InventoryOverview.WarehouseStock>
      key="warehouse"
      actionRef={actionRef}
      rowKey="stockId"
      columns={columns}
      scroll={getProTableScroll(TABLE_SCROLL_X + 740)}
      tableLayout="fixed"
      search={getPersistedProTableSearch({ fieldCount: SEARCH_FIELD_COUNT }, 'inventory-overview-warehouse')}
      columnsState={getProTableColumnsState('inventory-overview-warehouse-columns')}
      pagination={getProTablePagination()}
      toolBarRender={() => Array.isArray(toolbar) ? toolbar : [toolbar]}
      request={async ({ current, pageSize, ...params }) => {
        if (!canQuery) {
          return { data: [], total: 0, success: true };
        }
        const resp = await getInventoryOverviewWarehouseList(
          buildInventoryOverviewListParams(params, current, pageSize),
        );
        return {
          data: resp.rows || [],
          total: resp.total || 0,
          success: resp.code === 200,
        };
      }}
    />
  );
}
