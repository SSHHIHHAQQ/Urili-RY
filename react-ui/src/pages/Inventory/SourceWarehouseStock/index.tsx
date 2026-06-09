import {
  PageContainer,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import {
  inventoryAttributeSearchOptions,
  inventoryAttributeText,
  inventoryScopeText,
  pairingStatusText,
  skuPairingStatusSearchOptions,
  skuSyncItemStatusSearchOptions,
  syncItemStatusText,
} from '@/services/integration/constants';
import {
  getSourceWarehouseStockGroupDetail,
  getSourceWarehouseStockGroupList,
  getSourceWarehouseStockMasterWarehouseOptions,
  getSourceWarehouseStockSourceWarehouseOptions,
} from '@/services/integration/sourceWarehouseStock';
import {
  getProTableColumnsState,
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

const SOURCE_WAREHOUSE_STOCK_SEARCH_FIELD_COUNT = 11;
const DETAIL_TABLE_SCROLL_X = 2150;

const inventoryScopeTabs = [
  { key: 'COMPREHENSIVE', label: inventoryScopeText.COMPREHENSIVE || '综合库存' },
  { key: 'PRODUCT', label: inventoryScopeText.PRODUCT || '产品库存' },
  { key: 'RETURN', label: inventoryScopeText.RETURN || '退货库存' },
  { key: 'BOX', label: inventoryScopeText.BOX || '箱库存' },
];

const quantityRangeFields = [
  {
    key: 'totalQuantityRange',
    label: '总库存数',
    minParam: 'totalQuantityMin',
    maxParam: 'totalQuantityMax',
  },
  {
    key: 'availableQuantityRange',
    label: '可用库存数',
    minParam: 'availableQuantityMin',
    maxParam: 'availableQuantityMax',
  },
  {
    key: 'lockedQuantityRange',
    label: '锁定库存数',
    minParam: 'lockedQuantityMin',
    maxParam: 'lockedQuantityMax',
  },
  {
    key: 'inTransitQuantityRange',
    label: '在途库存数',
    minParam: 'inTransitQuantityMin',
    maxParam: 'inTransitQuantityMax',
  },
] as const;

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

function normalizeQuantityRangeParamValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function buildSourceWarehouseStockListParams(params: Record<string, any>, current?: number, pageSize?: number) {
  const next = { ...params };
  const quantityParams: Record<string, number> = {};

  quantityRangeFields.forEach((field) => {
    const rangeValue = next[field.key];
    delete next[field.key];
    const [minValue, maxValue] = Array.isArray(rangeValue) ? rangeValue : [];
    const normalizedMinValue = normalizeQuantityRangeParamValue(minValue);
    const normalizedMaxValue = normalizeQuantityRangeParamValue(maxValue);
    if (normalizedMinValue !== undefined) {
      quantityParams[field.minParam] = normalizedMinValue;
    }
    if (normalizedMaxValue !== undefined) {
      quantityParams[field.maxParam] = normalizedMaxValue;
    }
  });

  return cleanParams({
    pageNum: current,
    pageSize,
    ...next,
    ...quantityParams,
  });
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
  const color = value === 'ACTIVE' ? 'green' : value === 'MISSING' ? 'orange' : value === 'MIXED' ? 'gold' : 'default';
  return <Tag color={color}>{text}</Tag>;
}

function pairingTag(value?: string) {
  const text = pairingStatusText[value || ''] || value || '未配对';
  const color = value === 'PAIRED' ? 'blue' : value === 'PARTIAL' ? 'gold' : 'default';
  return <Tag color={color}>{text}</Tag>;
}

function inventoryAttributeDisplay(value?: string | number | null) {
  const normalized = value === undefined || value === null ? '' : String(value);
  return inventoryAttributeText[normalized] || displayText(value);
}

const detailColumns: ColumnsType<API.Integration.SourceWarehouseStockItem> = [
  {
    title: '来源主仓',
    dataIndex: 'masterWarehouseName',
    width: 110,
    render: (value) => displayText(value),
  },
  {
    title: '来源仓库',
    dataIndex: 'upstreamWarehouseCode',
    width: 190,
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
    title: '库存属性',
    dataIndex: 'inventoryAttribute',
    width: 90,
    render: (value) => inventoryAttributeDisplay(value),
  },
  {
    title: '总库存',
    dataIndex: 'totalQuantity',
    width: 100,
    align: 'right',
    render: (value) => displayQuantity(value as number),
  },
  {
    title: '可用库存',
    dataIndex: 'availableQuantity',
    width: 100,
    align: 'right',
    render: (value) => displayQuantity(value as number),
  },
  {
    title: '锁定库存',
    dataIndex: 'lockedQuantity',
    width: 100,
    align: 'right',
    render: (value) => displayQuantity(value as number),
  },
  {
    title: '在途库存',
    dataIndex: 'inTransitQuantity',
    width: 100,
    align: 'right',
    render: (value) => displayQuantity(value as number),
  },
  {
    title: '箱内库存',
    dataIndex: 'boxedQuantity',
    width: 100,
    align: 'right',
    render: (value) => displayQuantity(value as number),
  },
  {
    title: '批次',
    dataIndex: 'batchNo',
    width: 120,
    render: (value) => displayText(value),
  },
  {
    title: '库位',
    dataIndex: 'locationCode',
    width: 120,
    render: (value) => displayText(value),
  },
  {
    title: '系统仓库',
    dataIndex: 'systemWarehouseCode',
    width: 180,
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
    render: (_, record) => (
      <Typography.Text copyable={!!record.systemSku}>{displayText(record.systemSku)}</Typography.Text>
    ),
  },
  {
    title: '客户',
    dataIndex: 'customerName',
    width: 150,
    ellipsis: true,
    render: (value) => displayText(value),
  },
  {
    title: '仓库配对',
    dataIndex: 'warehousePairingStatus',
    width: 100,
    render: (value) => pairingTag(String(value || '')),
  },
  {
    title: 'SKU配对',
    dataIndex: 'skuPairingStatus',
    width: 100,
    render: (value) => pairingTag(String(value || '')),
  },
  {
    title: '同步状态',
    dataIndex: 'status',
    width: 100,
    render: (value) => statusTag(String(value || '')),
  },
  {
    title: '同步时间',
    dataIndex: 'lastSeenTime',
    width: 170,
    render: (value) => displayText(value),
  },
];

function SourceWarehouseStockDetailTable({
  sourceStockGroupKey,
  inventoryScope,
  canListSourceWarehouseStock,
}: {
  sourceStockGroupKey: string;
  inventoryScope: string;
  canListSourceWarehouseStock: boolean;
}) {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<API.Integration.SourceWarehouseStockItem[]>([]);

  useEffect(() => {
    if (!canListSourceWarehouseStock) {
      setRows([]);
      setLoading(false);
      return undefined;
    }
    let alive = true;
    setLoading(true);
    getSourceWarehouseStockGroupDetail(
      cleanParams({
        sourceStockGroupKey,
        inventoryScope,
      }),
    )
      .then((resp) => {
        if (alive) {
          setRows(resp.data || []);
        }
      })
      .catch(() => {
        if (alive) {
          setRows([]);
        }
      })
      .finally(() => {
        if (alive) {
          setLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [sourceStockGroupKey, inventoryScope, canListSourceWarehouseStock]);

  return (
    <Table<API.Integration.SourceWarehouseStockItem>
      size="small"
      loading={loading}
      rowKey={(record) => String(record.inventorySnapshotId)}
      columns={detailColumns}
      dataSource={rows}
      pagination={false}
      scroll={{ x: DETAIL_TABLE_SCROLL_X }}
      tableLayout="fixed"
    />
  );
}

export default function SourceWarehouseStockPage() {
  const access = useAccess();
  const canListSourceWarehouseStock = access.hasPerms('inventory:sourceWarehouse:list');
  const [inventoryScope, setInventoryScope] = useState('COMPREHENSIVE');
  const [filterOptionsLoading, setFilterOptionsLoading] = useState(false);
  const [masterWarehouseOptions, setMasterWarehouseOptions] = useState<API.Integration.SourceWarehouseStockOption[]>(
    [],
  );
  const [sourceWarehouseOptions, setSourceWarehouseOptions] = useState<API.Integration.SourceWarehouseStockOption[]>(
    [],
  );

  useEffect(() => {
    if (!canListSourceWarehouseStock) {
      setMasterWarehouseOptions([]);
      setSourceWarehouseOptions([]);
      setFilterOptionsLoading(false);
      return undefined;
    }
    let alive = true;
    setFilterOptionsLoading(true);
    Promise.all([
      getSourceWarehouseStockMasterWarehouseOptions({ inventoryScope }),
      getSourceWarehouseStockSourceWarehouseOptions({ inventoryScope }),
    ])
      .then(([masterWarehouseResp, sourceWarehouseResp]) => {
        if (alive) {
          setMasterWarehouseOptions(masterWarehouseResp.data || []);
          setSourceWarehouseOptions(sourceWarehouseResp.data || []);
        }
      })
      .catch(() => {
        if (alive) {
          setMasterWarehouseOptions([]);
          setSourceWarehouseOptions([]);
        }
      })
      .finally(() => {
        if (alive) {
          setFilterOptionsLoading(false);
        }
      });
    return () => {
      alive = false;
    };
  }, [inventoryScope, canListSourceWarehouseStock]);

  const columns: ProColumns<API.Integration.SourceWarehouseStockGroupItem>[] = [
    {
      title: '来源主仓',
      dataIndex: 'masterWarehouseName',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        allowClear: true,
        loading: filterOptionsLoading,
        options: masterWarehouseOptions,
        placeholder: '请选择',
      },
      hideInTable: true,
    },
    {
      title: '来源仓库',
      dataIndex: 'upstreamWarehouseCode',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        allowClear: true,
        loading: filterOptionsLoading,
        options: sourceWarehouseOptions,
        placeholder: '请选择',
      },
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
        options: inventoryAttributeSearchOptions,
      },
      hideInTable: true,
    },
    ...quantityRangeFields.map(
      (field) => ({
        title: field.label,
        dataIndex: field.key,
        colSize: 2,
        valueType: 'formSet' as any,
        hideInTable: true,
        fieldProps: {
          type: 'group',
          space: { block: true, style: { width: '100%' } },
        },
        columns: [
          {
            valueType: 'digit',
            fieldProps: {
              min: 0,
              precision: 0,
              placeholder: '最小',
              style: { width: '50%' },
            },
          },
          {
            valueType: 'digit',
            fieldProps: {
              min: 0,
              precision: 0,
              placeholder: '最大',
              style: { width: '50%' },
            },
          },
        ] as any,
      }) as ProColumns<API.Integration.SourceWarehouseStockGroupItem>,
    ),
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
      title: '来源主仓',
      dataIndex: 'masterWarehouseNames',
      width: 160,
      search: false,
      ellipsis: true,
      renderText: (value) => displayText(value),
    },
    {
      title: '来源仓库数',
      dataIndex: 'upstreamWarehouseCount',
      width: 110,
      align: 'right',
      search: false,
      renderText: (value) => displayQuantity(value),
    },
    {
      title: '库存属性',
      dataIndex: 'inventoryAttributeLabels',
      width: 110,
      search: false,
      renderText: (value) => displayText(value),
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
      title: '系统仓库',
      dataIndex: 'systemWarehouseNames',
      width: 180,
      search: false,
      ellipsis: true,
      renderText: (value) => displayText(value),
    },
    {
      title: '商城SKU',
      dataIndex: 'systemSkus',
      width: 160,
      search: false,
      ellipsis: true,
      renderText: (value) => displayText(value),
    },
    {
      title: '客户',
      dataIndex: 'customerNames',
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
      title: '更新时间',
      dataIndex: 'latestUpdateTime',
      width: 170,
      search: false,
      renderText: (value) => displayText(value),
    },
  ];

  return (
    <PageContainer title={false}>
      <ProTable<API.Integration.SourceWarehouseStockGroupItem>
        className="urili-fill-table"
        rowKey="sourceStockGroupKey"
        columns={columns}
        columnsState={getProTableColumnsState('source-warehouse-stock-columns')}
        params={{ inventoryScope }}
        search={getPersistedProTableSearch(
          { labelWidth: 96, fieldCount: SOURCE_WAREHOUSE_STOCK_SEARCH_FIELD_COUNT },
          'source-warehouse-stock',
        )}
        request={async (params) => {
          if (!canListSourceWarehouseStock) {
            return { data: [], total: 0, success: true };
          }
          const { current, pageSize, ...filters } = params;
          const resp = await getSourceWarehouseStockGroupList(
            buildSourceWarehouseStockListParams(filters, current, pageSize),
          );
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        pagination={getProTablePagination(20)}
        options={{ density: true, reload: true, setting: true }}
        scroll={getProTableScroll(2300)}
        expandable={{
          expandedRowRender: (record) => (
            <SourceWarehouseStockDetailTable
              sourceStockGroupKey={record.sourceStockGroupKey}
              inventoryScope={inventoryScope}
              canListSourceWarehouseStock={canListSourceWarehouseStock}
            />
          ),
          rowExpandable: (record) => !!record.sourceStockGroupKey,
        }}
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
