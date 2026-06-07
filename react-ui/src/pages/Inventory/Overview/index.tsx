import {
  PageContainer,
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Radio, Typography } from 'antd';
import { useRef, useState } from 'react';
import {
  getInventoryOverviewSkuList,
  getInventoryOverviewSpuList,
} from '@/services/inventory/overview';
import {
  getPersistedProTableSearch,
  getProTableColumnsState,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import SkuWarehouseTable from './components/SkuWarehouseTable';
import {
  formatDateTime,
  formatQuantity,
  inventoryStatusValueEnum,
  renderStatus,
  renderWarehouseKind,
  SEARCH_FIELD_COUNT,
  TABLE_SCROLL_X,
  type ViewMode,
  viewModeOptions,
  warehouseKindValueEnum,
} from './helpers';
import styles from './style.module.css';

function buildListParams(params: Record<string, any>, current?: number, pageSize?: number) {
  return Object.fromEntries(
    Object.entries({
      ...params,
      pageNum: current,
      pageSize,
    }).filter(([, value]) => value !== undefined && value !== null && String(value) !== ''),
  );
}

export default function InventoryOverviewPage() {
  const access = useAccess();
  const canQueryInventoryOverview = access.hasPerms('inventory:overview:query');
  const [viewMode, setViewMode] = useState<ViewMode>('SPU');
  const spuActionRef = useRef<ActionType>(null);
  const skuActionRef = useRef<ActionType>(null);

  const reloadCurrent = () => {
    if (viewMode === 'SPU') {
      spuActionRef.current?.reload();
    } else {
      skuActionRef.current?.reload();
    }
  };

  const commonColumns: ProColumns<API.InventoryOverview.OverviewItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '商品名 / SPU / SKU' },
    },
    {
      title: '库存状态',
      dataIndex: 'inventoryStatus',
      valueType: 'select',
      valueEnum: inventoryStatusValueEnum,
      hideInTable: true,
    },
    {
      title: '仓库类型',
      dataIndex: 'warehouseKindSummary',
      valueType: 'select',
      valueEnum: warehouseKindValueEnum,
      hideInTable: true,
    },
  ];

  const metricColumns: ProColumns<API.InventoryOverview.OverviewItem>[] = [
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      width: 90,
      search: false,
      align: 'right',
    },
    {
      title: '仓库类型',
      dataIndex: 'warehouseKindSummary',
      width: 110,
      search: false,
      render: (_, record) => renderWarehouseKind(record.warehouseKindSummary),
    },
    {
      title: '平台总库存',
      dataIndex: 'platformTotalQty',
      width: 120,
      search: false,
      align: 'right',
      render: (value) => formatQuantity(value as number),
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
      width: 120,
      search: false,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '来源可用库存',
      dataIndex: 'sourceAvailableQty',
      width: 120,
      search: false,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '状态',
      dataIndex: 'inventoryStatus',
      width: 120,
      search: false,
      render: (value) => renderStatus(String(value || '')),
    },
    {
      title: '更新时间',
      dataIndex: 'latestStockUpdateTime',
      width: 170,
      search: false,
      render: (value) => formatDateTime(value as string),
    },
  ];

  const spuColumns: ProColumns<API.InventoryOverview.OverviewItem>[] = [
    ...commonColumns,
    {
      title: '商品信息',
      dataIndex: 'productName',
      width: 280,
      fixed: 'left',
      search: false,
      render: (_, record) => (
        <>
          <Typography.Text strong ellipsis={{ tooltip: record.productName }}>
            {record.productName || '-'}
          </Typography.Text>
          <br />
          <Typography.Text type="secondary">{record.systemSpuCode || '-'}</Typography.Text>
        </>
      ),
    },
    {
      title: 'SKU数',
      dataIndex: 'skuCount',
      width: 90,
      search: false,
      align: 'right',
    },
    ...metricColumns,
  ];

  const skuColumns: ProColumns<API.InventoryOverview.OverviewItem>[] = [
    ...commonColumns,
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
    ...metricColumns,
  ];

  const viewModeSwitch = (
    <Radio.Group
      key="view-mode"
      buttonStyle="solid"
      value={viewMode}
      onChange={(event) => setViewMode(event.target.value as ViewMode)}
    >
      {viewModeOptions.map((item) => (
        <Radio.Button key={item.value} value={item.value}>
          {item.label}
        </Radio.Button>
      ))}
    </Radio.Group>
  );

  return (
    <PageContainer title={false}>
      {viewMode === 'SPU' ? (
        <ProTable<API.InventoryOverview.OverviewItem>
          key="spu"
          actionRef={spuActionRef}
          rowKey="spuId"
          columns={spuColumns}
          scroll={getProTableScroll(TABLE_SCROLL_X)}
          tableLayout="fixed"
          search={getPersistedProTableSearch({ fieldCount: SEARCH_FIELD_COUNT }, 'inventory-overview-spu')}
          columnsState={getProTableColumnsState('inventory-overview-spu-columns')}
          pagination={getProTablePagination()}
          toolBarRender={() => [viewModeSwitch]}
        expandable={{
          expandedRowClassName: () => styles.overviewExpandedRow,
          expandedRowRender: (record) => (
            <div className={styles.overviewNestedTable}>
              <ProTable<API.InventoryOverview.OverviewItem>
                rowKey="skuId"
                size="small"
                columns={skuColumns.filter((item) => !item.hideInTable)}
                search={false}
                options={false}
                pagination={false}
                tableLayout="fixed"
                scroll={{ x: TABLE_SCROLL_X }}
                request={async () => {
                  const resp = await getInventoryOverviewSkuList({ spuId: record.spuId });
                  return {
                    data: resp.rows || [],
                    total: resp.total || 0,
                    success: resp.code === 200,
                  };
                }}
              />
            </div>
          ),
        }}
          request={async ({ current, pageSize, ...params }) => {
            const resp = await getInventoryOverviewSpuList(buildListParams(params, current, pageSize));
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
        />
      ) : (
        <ProTable<API.InventoryOverview.OverviewItem>
          key="sku"
          actionRef={skuActionRef}
          rowKey="skuId"
          columns={skuColumns}
          scroll={getProTableScroll(TABLE_SCROLL_X)}
          tableLayout="fixed"
          search={getPersistedProTableSearch({ fieldCount: SEARCH_FIELD_COUNT }, 'inventory-overview-sku')}
          columnsState={getProTableColumnsState('inventory-overview-sku-columns')}
          pagination={getProTablePagination()}
          toolBarRender={() => [viewModeSwitch]}
          expandable={canQueryInventoryOverview
            ? {
                expandedRowClassName: () => styles.overviewExpandedRow,
                expandedRowRender: (record) => (
                  <SkuWarehouseTable
                    skuId={record.skuId}
                    canAdjust={access.hasPerms('inventory:overview:adjust')}
                    onChanged={reloadCurrent}
                  />
                ),
              }
            : undefined}
          request={async ({ current, pageSize, ...params }) => {
            const resp = await getInventoryOverviewSkuList(buildListParams(params, current, pageSize));
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
        />
      )}
    </PageContainer>
  );
}
