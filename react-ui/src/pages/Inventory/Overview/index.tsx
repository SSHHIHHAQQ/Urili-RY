import {
  PageContainer,
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Empty, Radio, Space, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  getInventoryOverviewSkuList,
  getInventoryOverviewSpuList,
  getInventoryOverviewOfficialWarehouseOptions,
  getInventoryOverviewSellerOptions,
  getInventoryOverviewWarehouseOptions,
} from '@/services/inventory/overview';
import {
  getPersistedProTableSearch,
  getProTableColumnsState,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import InventoryAdjustButton from '@/components/InventoryAdjust/InventoryAdjustButton';
import InventorySyncPolicyButton from '@/components/InventorySyncPolicy/InventorySyncPolicyButton';
import SpuSkuWarehouseTable from './components/SpuSkuWarehouseTable';
import SkuWarehouseTable from './components/SkuWarehouseTable';
import WarehouseViewTable from './components/WarehouseViewTable';
import {
  buildDateRangeSearchColumns,
  buildInventoryOverviewListParams,
  buildQuantityRangeSearchColumns,
  formatDateTime,
  formatQuantity,
  inventoryStatusValueEnum,
  pairingStatusValueEnum,
  renderSyncMode,
  renderStatus,
  renderWarehouseKind,
  SEARCH_FIELD_COUNT,
  TABLE_SCROLL_X,
  type ViewMode,
  viewModeOptions,
  syncModeValueEnum,
  warehouseKindValueEnum,
} from './helpers';
import styles from './style.module.css';

export default function InventoryOverviewPage() {
  const access = useAccess();
  const canListInventoryOverview = access.hasPerms('inventory:overview:list');
  const canQueryInventoryOverview = access.hasPerms('inventory:overview:query');
  const canAdjustInventoryOverview = access.hasPerms('inventory:overview:adjust') && canQueryInventoryOverview;
  const canSyncInventoryOverview = access.hasPerms('inventory:overview:syncPolicy') && canQueryInventoryOverview;
  const [viewMode, setViewMode] = useState<ViewMode>('SPU');
  const [warehouseOptions, setWarehouseOptions] = useState<API.InventoryOverview.WarehouseOption[]>([]);
  const [warehouseOptionsLoading, setWarehouseOptionsLoading] = useState(false);
  const [officialWarehouseOptions, setOfficialWarehouseOptions] = useState<API.InventoryOverview.WarehouseOption[]>([]);
  const [sellerOptions, setSellerOptions] = useState<API.InventoryOverview.SellerOption[]>([]);
  const [sellerOptionsLoading, setSellerOptionsLoading] = useState(false);
  const visibleViewModeOptions = viewModeOptions.filter((item) =>
    item.value === 'WAREHOUSE' ? canQueryInventoryOverview : canListInventoryOverview,
  );
  const effectiveViewMode = visibleViewModeOptions.some((item) => item.value === viewMode)
    ? viewMode
    : visibleViewModeOptions[0]?.value;
  const spuActionRef = useRef<ActionType>(null);
  const skuActionRef = useRef<ActionType>(null);
  const warehouseActionRef = useRef<ActionType>(null);

  useEffect(() => {
    if (!canQueryInventoryOverview) {
      setWarehouseOptions([]);
      return;
    }
    let ignore = false;
    setWarehouseOptionsLoading(true);
    getInventoryOverviewWarehouseOptions()
      .then((resp) => {
        if (!ignore) {
          setWarehouseOptions(resp.data || []);
        }
      })
      .catch(() => {
        if (!ignore) {
          setWarehouseOptions([]);
        }
      })
      .finally(() => {
        if (!ignore) {
          setWarehouseOptionsLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, [canQueryInventoryOverview]);

  useEffect(() => {
    if (!canQueryInventoryOverview) {
      setSellerOptions([]);
      return;
    }
    let ignore = false;
    setSellerOptionsLoading(true);
    getInventoryOverviewSellerOptions()
      .then((resp) => {
        if (!ignore) {
          setSellerOptions(resp.data || []);
        }
      })
      .catch(() => {
        if (!ignore) {
          setSellerOptions([]);
        }
      })
      .finally(() => {
        if (!ignore) {
          setSellerOptionsLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, [canQueryInventoryOverview]);

  useEffect(() => {
    if (!canQueryInventoryOverview) {
      setOfficialWarehouseOptions([]);
      return;
    }
    let ignore = false;
    getInventoryOverviewOfficialWarehouseOptions()
      .then((resp) => {
        if (!ignore) {
          setOfficialWarehouseOptions(resp.data || []);
        }
      })
      .catch(() => {
        if (!ignore) {
          setOfficialWarehouseOptions([]);
        }
      });
    return () => {
      ignore = true;
    };
  }, [canQueryInventoryOverview]);

  const reloadCurrent = () => {
    if (effectiveViewMode === 'SPU') {
      spuActionRef.current?.reload();
    } else if (effectiveViewMode === 'SKU') {
      skuActionRef.current?.reload();
    } else {
      warehouseActionRef.current?.reload();
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
      dataIndex: 'syncModeSummary',
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
      title: '仓库类型',
      dataIndex: 'warehouseKindSummary',
      valueType: 'select',
      valueEnum: warehouseKindValueEnum,
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
    ...buildQuantityRangeSearchColumns<API.InventoryOverview.OverviewItem>(),
    ...buildDateRangeSearchColumns<API.InventoryOverview.OverviewItem>(),
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
      title: '同步方式',
      dataIndex: 'syncModeSummary',
      width: 170,
      search: false,
      render: (_, record) => renderSyncMode(record.syncModeSummary, record.syncPolicyScopeSummary),
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
    ...metricColumns,
    {
      title: '操作',
      valueType: 'option',
      width: 170,
      render: (_, record) => (
        <Space size={4}>
          <InventoryAdjustButton
            scope="SPU"
            overviewRecord={record}
            canAdjust={canAdjustInventoryOverview}
            onChanged={reloadCurrent}
          />
          <InventorySyncPolicyButton
            initialScope="SPU"
            lockScope
            overviewRecord={record}
            sellerOptions={sellerOptions}
            warehouseOptions={officialWarehouseOptions}
            canSync={canSyncInventoryOverview}
            onChanged={reloadCurrent}
            buttonText="同步方式"
          />
        </Space>
      ),
    },
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
    ...metricColumns,
    {
      title: '操作',
      valueType: 'option',
      width: 170,
      render: (_, record) => (
        <Space size={4}>
          <InventoryAdjustButton
            scope="SKU"
            overviewRecord={record}
            canAdjust={canAdjustInventoryOverview}
            onChanged={reloadCurrent}
          />
          <InventorySyncPolicyButton
            initialScope="SKU"
            lockScope
            overviewRecord={record}
            sellerOptions={sellerOptions}
            warehouseOptions={officialWarehouseOptions}
            canSync={canSyncInventoryOverview}
            onChanged={reloadCurrent}
            buttonText="同步方式"
          />
        </Space>
      ),
    },
  ];

  const viewModeSwitch = (
    <Radio.Group
      key="view-mode"
      buttonStyle="solid"
      value={effectiveViewMode}
      onChange={(event) => setViewMode(event.target.value as ViewMode)}
    >
      {visibleViewModeOptions.map((item) => (
        <Radio.Button key={item.value} value={item.value}>
          {item.label}
        </Radio.Button>
      ))}
    </Radio.Group>
  );

  const syncPolicyToolbarButton = (
    <InventorySyncPolicyButton
      key="sync-policy"
      initialScope="SELLER"
      sellerOptions={sellerOptions}
      warehouseOptions={officialWarehouseOptions}
      canSync={canSyncInventoryOverview}
      onChanged={reloadCurrent}
      buttonType="primary"
    />
  );

  return (
    <PageContainer title={false}>
      {effectiveViewMode === 'SPU' ? (
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
          toolBarRender={() => [viewModeSwitch, syncPolicyToolbarButton]}
          expandable={canQueryInventoryOverview
            ? {
                expandedRowClassName: () => styles.overviewExpandedRow,
                expandedRowRender: (record) => (
                  <SpuSkuWarehouseTable
                    spuId={record.spuId}
                    canAdjust={canAdjustInventoryOverview}
                    canSync={canSyncInventoryOverview}
                    sellerOptions={sellerOptions}
                    warehouseOptions={officialWarehouseOptions}
                    onChanged={reloadCurrent}
                  />
                ),
              }
            : undefined}
          request={async ({ current, pageSize, ...params }) => {
            if (!canListInventoryOverview) {
              return { data: [], total: 0, success: true };
            }
            const resp = await getInventoryOverviewSpuList(
              buildInventoryOverviewListParams(params, current, pageSize),
            );
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
        />
      ) : effectiveViewMode === 'SKU' ? (
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
          toolBarRender={() => [viewModeSwitch, syncPolicyToolbarButton]}
          expandable={canQueryInventoryOverview
            ? {
                expandedRowClassName: () => styles.overviewExpandedRow,
                expandedRowRender: (record) => (
                  <SkuWarehouseTable
                    skuId={record.skuId}
                    canAdjust={canAdjustInventoryOverview}
                    canSync={canSyncInventoryOverview}
                    sellerOptions={sellerOptions}
                    warehouseOptions={officialWarehouseOptions}
                    onChanged={reloadCurrent}
                  />
                ),
              }
            : undefined}
          request={async ({ current, pageSize, ...params }) => {
            if (!canListInventoryOverview) {
              return { data: [], total: 0, success: true };
            }
            const resp = await getInventoryOverviewSkuList(
              buildInventoryOverviewListParams(params, current, pageSize),
            );
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
        />
      ) : effectiveViewMode === 'WAREHOUSE' ? (
        <WarehouseViewTable
          actionRef={warehouseActionRef}
          canQuery={canQueryInventoryOverview}
          canAdjust={canAdjustInventoryOverview}
          canSync={canSyncInventoryOverview}
          toolbar={[viewModeSwitch, syncPolicyToolbarButton]}
          sellerOptions={sellerOptions}
          sellerOptionsLoading={sellerOptionsLoading}
          warehouseOptions={warehouseOptions}
          warehouseOptionsLoading={warehouseOptionsLoading}
          syncWarehouseOptions={officialWarehouseOptions}
        />
      ) : (
        <Empty description="暂无库存总览权限" />
      )}
    </PageContainer>
  );
}
