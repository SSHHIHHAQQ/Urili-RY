import {
  PageContainer,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Space, Tag, Typography } from 'antd';
import {
  inventoryScopeSearchOptions,
  inventoryScopeText,
  pairingStatusText,
  skuPairingStatusSearchOptions,
  skuSyncItemStatusSearchOptions,
  syncItemStatusText,
  systemKindText,
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

function sourceSystemText(record: API.Integration.SourceWarehouseStockItem) {
  return record.systemKindLabel || systemKindText[record.systemKind || ''] || record.systemKind || '-';
}

function inventoryScopeLabel(value?: string) {
  return inventoryScopeText[value || ''] || value || '-';
}

export default function SourceWarehouseStockPage() {
  const columns: ProColumns<API.Integration.SourceWarehouseStockItem>[] = [
    {
      title: '来源系统编号',
      dataIndex: 'connectionCode',
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
      dataIndex: 'status',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuSyncItemStatusSearchOptions,
      },
      width: 110,
      render: (_, record) => statusTag(record.status),
    },
    {
      title: '仓库配对',
      dataIndex: 'warehousePairingStatus',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuPairingStatusSearchOptions,
      },
      width: 110,
      render: (_, record) => pairingTag(record.warehousePairingStatus),
    },
    {
      title: 'SKU配对',
      dataIndex: 'skuPairingStatus',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuPairingStatusSearchOptions,
      },
      width: 110,
      render: (_, record) => pairingTag(record.skuPairingStatus),
    },
    {
      title: '库存口径',
      dataIndex: 'inventoryScope',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: inventoryScopeSearchOptions,
      },
      width: 110,
      renderText: (value) => inventoryScopeLabel(value),
    },
    {
      title: '库存属性',
      dataIndex: 'inventoryAttribute',
      width: 120,
      renderText: (value) => displayText(value),
    },
    {
      title: '来源系统',
      key: 'sourceSystem',
      dataIndex: 'sourceSystem',
      width: 180,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Tag color="blue">{sourceSystemText(record)}</Tag>
          <Typography.Text ellipsis={{ tooltip: record.masterWarehouseName }}>
            {displayText(record.masterWarehouseName)}
          </Typography.Text>
          <Typography.Text type="secondary">{record.connectionCode}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '来源仓库',
      key: 'sourceWarehouse',
      dataIndex: 'sourceWarehouse',
      width: 210,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.upstreamWarehouseCode}>
            {displayText(record.upstreamWarehouseCode)}
          </Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.upstreamWarehouseName }}>
            {displayText(record.upstreamWarehouseName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '来源SKU',
      key: 'sourceSku',
      dataIndex: 'masterSku',
      width: 240,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.masterSku}>{displayText(record.masterSku)}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.masterProductName }}>
            {displayText(record.masterProductName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '库存数量',
      key: 'quantity',
      dataIndex: 'totalQuantity',
      width: 230,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>总库存 {displayQuantity(record.totalQuantity)}</Typography.Text>
          <Typography.Text type="secondary">
            可用 {displayQuantity(record.availableQuantity)} / 锁定 {displayQuantity(record.lockedQuantity)}
          </Typography.Text>
          <Typography.Text type="secondary">
            在途 {displayQuantity(record.inTransitQuantity)} / 箱内 {displayQuantity(record.boxedQuantity)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '批次 / 库位',
      key: 'batchLocation',
      dataIndex: 'batchNo',
      width: 170,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>批次 {displayText(record.batchNo)}</Typography.Text>
          <Typography.Text type="secondary">库位 {displayText(record.locationCode)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '系统仓库',
      key: 'systemWarehouse',
      dataIndex: 'systemWarehouseCode',
      width: 190,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>{displayText(record.systemWarehouseCode)}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.systemWarehouseName }}>
            {displayText(record.systemWarehouseName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '商城SKU',
      key: 'systemSku',
      dataIndex: 'systemSku',
      width: 220,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.systemSku}>{displayText(record.systemSku)}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.systemSkuName }}>
            {displayText(record.systemSkuName)}
          </Typography.Text>
          <Typography.Text type="secondary">{displayText(record.customerName)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '同步时间',
      key: 'syncTime',
      dataIndex: 'lastSeenTime',
      width: 190,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>{displayText(record.lastSeenTime)}</Typography.Text>
          <Typography.Text type="secondary">更新 {displayText(record.updateTime)}</Typography.Text>
        </Space>
      ),
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
        scroll={getProTableScroll(1950)}
        toolBarRender={() => []}
      />
    </PageContainer>
  );
}
