import {
  PageContainer,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Tag, Typography } from 'antd';
import { useState } from 'react';
import {
  inventoryScopeText,
  pairingStatusText,
  skuPairingStatusSearchOptions,
  skuSyncItemStatusSearchOptions,
  syncItemStatusText,
} from '@/services/integration/constants';
import { getSourceWarehouseStockList } from '@/services/integration/sourceWarehouseStock';
import {
  getProTableColumnsState,
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

const SOURCE_WAREHOUSE_STOCK_SEARCH_FIELD_COUNT = 7;

const inventoryScopeTabs = [
  { key: 'COMPREHENSIVE', label: inventoryScopeText.COMPREHENSIVE || '综合库存' },
  { key: 'PRODUCT', label: inventoryScopeText.PRODUCT || '产品库存' },
  { key: 'RETURN', label: inventoryScopeText.RETURN || '退货库存' },
  { key: 'BOX', label: inventoryScopeText.BOX || '箱库存' },
];

const inventoryAttributeOptions = [
  { label: '全部库存属性', value: '' },
  { label: '正品', value: '0' },
  { label: '次品', value: '1' },
];

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

function displayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function displayQuantity(value?: number | null) {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN').format(Number(value));
}

function statusTag(value?: string) {
  const text = syncItemStatusText[value || ''] || value || '-';
  const color = value === 'ACTIVE' ? 'green' : value === 'MISSING' ? 'orange' : 'default';
  return <Tag color={color}>{text}</Tag>;
}

function pairingTag(value?: string) {
  const text = pairingStatusText[value || ''] || value || '未配对';
  return <Tag color={value === 'PAIRED' ? 'blue' : 'default'}>{text}</Tag>;
}

function inventoryAttributeText(value?: string | number | null) {
  const normalized = value === undefined || value === null ? '' : String(value);
  if (normalized === '0') {
    return '正品';
  }
  if (normalized === '1') {
    return '次品';
  }
  return displayText(value);
}

export default function SourceWarehouseStockPage() {
  const [inventoryScope, setInventoryScope] = useState('COMPREHENSIVE');

  const columns: ProColumns<API.Integration.SourceWarehouseStockItem>[] = [
    {
      title: '来源主仓',
      dataIndex: 'masterWarehouseKeyword',
      hideInTable: true,
    },
    {
      title: '来源仓库',
      dataIndex: 'warehouseKeyword',
      hideInTable: true,
    },
    {
      title: 'SKU / 商品',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '同步状态',
      key: 'statusSearch',
      dataIndex: 'status',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuSyncItemStatusSearchOptions,
      },
      hideInTable: true,
    },
    {
      title: '仓库配对',
      key: 'warehousePairingStatusSearch',
      dataIndex: 'warehousePairingStatus',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuPairingStatusSearchOptions,
      },
      hideInTable: true,
    },
    {
      title: 'SKU配对',
      key: 'skuPairingStatusSearch',
      dataIndex: 'skuPairingStatus',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuPairingStatusSearchOptions,
      },
      hideInTable: true,
    },
    {
      title: '库存属性',
      key: 'inventoryAttributeSearch',
      dataIndex: 'inventoryAttribute',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: inventoryAttributeOptions,
      },
      hideInTable: true,
    },
    {
      title: '来源主仓',
      dataIndex: 'masterWarehouseName',
      width: 110,
      search: false,
      renderText: (value) => displayText(value),
    },
    {
      title: '来源仓库',
      key: 'sourceWarehouse',
      dataIndex: 'upstreamWarehouseCode',
      width: 190,
      search: false,
      render: (_, record) => (
        <>
          <Typography.Text copyable={!!record.upstreamWarehouseCode}>
            {displayText(record.upstreamWarehouseCode)}
          </Typography.Text>
          <br />
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.upstreamWarehouseName }}>
            {displayText(record.upstreamWarehouseName)}
          </Typography.Text>
        </>
      ),
    },
    {
      title: '来源SKU',
      dataIndex: 'masterSku',
      width: 150,
      search: false,
      render: (_, record) => (
        <Typography.Text copyable={!!record.masterSku}>{displayText(record.masterSku)}</Typography.Text>
      ),
    },
    {
      title: '商品名称',
      dataIndex: 'masterProductName',
      width: 220,
      search: false,
      ellipsis: true,
      renderText: (value) => displayText(value),
    },
    {
      title: '库存属性',
      key: 'inventoryAttributeDisplay',
      dataIndex: 'inventoryAttribute',
      width: 90,
      search: false,
      renderText: (value) => inventoryAttributeText(value),
    },
    {
      title: '总库存',
      dataIndex: 'totalQuantity',
      width: 100,
      align: 'right',
      search: false,
      renderText: (value) => displayQuantity(value),
    },
    {
      title: '可用库存',
      dataIndex: 'availableQuantity',
      width: 100,
      align: 'right',
      search: false,
      renderText: (value) => displayQuantity(value),
    },
    {
      title: '锁定库存',
      dataIndex: 'lockedQuantity',
      width: 100,
      align: 'right',
      search: false,
      renderText: (value) => displayQuantity(value),
    },
    {
      title: '在途库存',
      dataIndex: 'inTransitQuantity',
      width: 100,
      align: 'right',
      search: false,
      renderText: (value) => displayQuantity(value),
    },
    {
      title: '箱内库存',
      dataIndex: 'boxedQuantity',
      width: 100,
      align: 'right',
      search: false,
      renderText: (value) => displayQuantity(value),
    },
    {
      title: '批次',
      dataIndex: 'batchNo',
      width: 120,
      search: false,
      renderText: (value) => displayText(value),
    },
    {
      title: '库位',
      dataIndex: 'locationCode',
      width: 120,
      search: false,
      renderText: (value) => displayText(value),
    },
    {
      title: '系统仓库',
      key: 'systemWarehouse',
      dataIndex: 'systemWarehouseCode',
      width: 180,
      search: false,
      render: (_, record) => (
        <>
          <Typography.Text>{displayText(record.systemWarehouseCode)}</Typography.Text>
          <br />
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.systemWarehouseName }}>
            {displayText(record.systemWarehouseName)}
          </Typography.Text>
        </>
      ),
    },
    {
      title: '商城SKU',
      dataIndex: 'systemSku',
      width: 150,
      search: false,
      render: (_, record) => (
        <Typography.Text copyable={!!record.systemSku}>{displayText(record.systemSku)}</Typography.Text>
      ),
    },
    {
      title: '客户',
      dataIndex: 'customerName',
      width: 150,
      search: false,
      ellipsis: true,
      renderText: (value) => displayText(value),
    },
    {
      title: '仓库配对',
      key: 'warehousePairingStatusDisplay',
      dataIndex: 'warehousePairingStatus',
      width: 100,
      search: false,
      render: (_, record) => pairingTag(record.warehousePairingStatus),
    },
    {
      title: 'SKU配对',
      key: 'skuPairingStatusDisplay',
      dataIndex: 'skuPairingStatus',
      width: 100,
      search: false,
      render: (_, record) => pairingTag(record.skuPairingStatus),
    },
    {
      title: '同步状态',
      key: 'statusDisplay',
      dataIndex: 'status',
      width: 100,
      search: false,
      render: (_, record) => statusTag(record.status),
    },
    {
      title: '同步时间',
      dataIndex: 'lastSeenTime',
      width: 170,
      search: false,
      renderText: (value) => displayText(value),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 170,
      search: false,
      renderText: (value) => displayText(value),
    },
  ];

  return (
    <PageContainer title={false}>
      <ProTable<API.Integration.SourceWarehouseStockItem>
        className="urili-fill-table"
        rowKey={(record) =>
          record.inventorySnapshotId ||
          [
            record.connectionCode,
            record.upstreamWarehouseCode,
            record.masterSku,
            record.inventoryScope,
            record.inventoryAttribute,
            record.batchNo,
            record.locationCode,
          ].join(':')
        }
        columns={columns}
        columnsState={getProTableColumnsState('source-warehouse-stock-columns')}
        params={{ inventoryScope }}
        search={getPersistedProTableSearch(
          { labelWidth: 96, fieldCount: SOURCE_WAREHOUSE_STOCK_SEARCH_FIELD_COUNT },
          'source-warehouse-stock',
        )}
        request={async (params) => {
          const { current, pageSize, ...filters } = params;
          const resp = await getSourceWarehouseStockList(
            cleanParams({
              pageNum: current,
              pageSize,
              ...filters,
            }),
          );
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        pagination={getProTablePagination(20)}
        options={{ density: true, reload: true, setting: true }}
        scroll={getProTableScroll(2550)}
        toolbar={{
          menu: {
            type: 'tab',
            activeKey: inventoryScope,
            items: inventoryScopeTabs,
            onChange: (key) => setInventoryScope(String(key)),
          },
        }}
        toolBarRender={() => []}
      />
    </PageContainer>
  );
}
