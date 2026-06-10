import { type ProColumns, ProTable } from '@ant-design/pro-components';
import { Button, Form, Space, Tag, Typography } from 'antd';
import type { FormInstance } from 'antd';
import { useEffect, useState } from 'react';
import {
  getInventoryOverviewSkuList,
  getInventoryOverviewSpuList,
  getInventoryOverviewWarehouseList,
} from '@/services/inventory/overview';

type SyncPolicyForm = API.InventoryOverview.SyncPolicyRequest;

const warehouseKindText: Record<string, string> = {
  official: '官方仓',
  third_party: '三方仓',
  unconfigured: '未配置',
  MIXED: '混合',
};

const warehouseKindColor: Record<string, string> = {
  official: 'blue',
  third_party: 'purple',
  unconfigured: 'red',
  MIXED: 'gold',
};

const inventoryStatusText: Record<string, string> = {
  IN_STOCK: '有货',
  OUT_OF_STOCK: '缺货',
  NO_WAREHOUSE: '仓库未配置',
  SOURCE_UNBOUND: '来源SKU未绑定',
  NO_SOURCE: '无来源库存',
  SOURCE_ONLY_IN_TRANSIT: '仅来源在途',
  DISABLED: '停用',
};

const inventoryStatusColor: Record<string, string> = {
  IN_STOCK: 'green',
  OUT_OF_STOCK: 'default',
  NO_WAREHOUSE: 'red',
  SOURCE_UNBOUND: 'orange',
  NO_SOURCE: 'orange',
  SOURCE_ONLY_IN_TRANSIT: 'blue',
  DISABLED: 'red',
};

function qty(value?: number | null) {
  return Number(value || 0);
}

function formatQuantity(value?: number | null) {
  return new Intl.NumberFormat('zh-CN').format(qty(value));
}

function renderWarehouseKind(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={warehouseKindColor[value] || 'default'}>{warehouseKindText[value] || value}</Tag>;
}

function renderStatus(value?: string) {
  const text = inventoryStatusText[value || ''] || value || '-';
  return <Tag color={inventoryStatusColor[value || ''] || 'default'}>{text}</Tag>;
}

function renderProductInfo(record: API.InventoryOverview.OverviewItem) {
  return (
    <>
      <Typography.Text strong ellipsis={{ tooltip: record.productName }}>
        {record.productName || '-'}
      </Typography.Text>
      <br />
      <Typography.Text type="secondary">{record.systemSpuCode || '-'}</Typography.Text>
    </>
  );
}

function renderSkuInfo(record: API.InventoryOverview.OverviewItem | API.InventoryOverview.WarehouseStock) {
  return (
    <>
      <Typography.Text strong>{record.systemSkuCode || '-'}</Typography.Text>
      <br />
      <Typography.Text type="secondary" ellipsis={{ tooltip: record.productName }}>
        {record.productName || '-'}
      </Typography.Text>
      {record.skuName ? (
        <>
          <br />
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.skuName }}>
            {record.skuName}
          </Typography.Text>
        </>
      ) : null}
    </>
  );
}

function renderSeller(record: API.InventoryOverview.OverviewItem | API.InventoryOverview.WarehouseStock) {
  return (
    <>
      <Typography.Text ellipsis={{ tooltip: record.sellerName }}>{record.sellerName || '-'}</Typography.Text>
      {record.sellerNo ? (
        <>
          <br />
          <Typography.Text type="secondary">{record.sellerNo}</Typography.Text>
        </>
      ) : null}
    </>
  );
}

function renderWarehouse(record: API.InventoryOverview.WarehouseStock) {
  return (
    <>
      <Typography.Text>{record.warehouseName || '-'}</Typography.Text>
      {record.warehouseCode ? (
        <>
          <br />
          <Typography.Text type="secondary">{record.warehouseCode}</Typography.Text>
        </>
      ) : null}
    </>
  );
}

function buildListParams(params: Record<string, any>) {
  return {
    ...params,
    pageNum: params.current,
    pageSize: params.pageSize,
  };
}

function SelectedTargetBoard({
  title,
  children,
  locked,
  onClear,
}: {
  title: string;
  children: React.ReactNode;
  locked: boolean;
  onClear: () => void;
}) {
  return (
    <div style={{ border: '1px solid #f0f0f0', borderRadius: 6, padding: 12, background: '#fafafa' }}>
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Typography.Text strong>{title}</Typography.Text>
          {!locked ? (
            <Button type="link" size="small" onClick={onClear}>
              清空
            </Button>
          ) : null}
        </Space>
        {children}
      </Space>
    </div>
  );
}

export default function InventorySyncPolicyTargetPicker({
  form,
  currentScope,
  lockScope,
  overviewRecord,
  warehouseRecord,
  onSelectionChange,
}: {
  form: FormInstance<SyncPolicyForm>;
  currentScope: API.InventoryOverview.SyncPolicyScope;
  lockScope: boolean;
  overviewRecord?: API.InventoryOverview.OverviewItem;
  warehouseRecord?: API.InventoryOverview.WarehouseStock;
  onSelectionChange: () => void;
}) {
  const selectedSpuId = Form.useWatch('spuId', form);
  const selectedSkuId = Form.useWatch('skuId', form);
  const selectedStockId = Form.useWatch('stockId', form);
  const [selectedSpu, setSelectedSpu] = useState<API.InventoryOverview.OverviewItem | undefined>();
  const [selectedSku, setSelectedSku] = useState<API.InventoryOverview.OverviewItem | undefined>();
  const [selectedStock, setSelectedStock] = useState<API.InventoryOverview.WarehouseStock | undefined>();

  useEffect(() => {
    if (currentScope === 'SPU' && overviewRecord?.spuId) {
      setSelectedSpu(overviewRecord);
    } else {
      setSelectedSpu(undefined);
    }
    if (currentScope === 'SKU' && overviewRecord?.skuId) {
      setSelectedSku(overviewRecord);
    } else {
      setSelectedSku(undefined);
    }
    if (currentScope === 'SKU_WAREHOUSE' && warehouseRecord?.stockId) {
      setSelectedStock(warehouseRecord);
    } else {
      setSelectedStock(undefined);
    }
  }, [currentScope, overviewRecord, warehouseRecord]);

  const clearTarget = () => {
    form.setFieldsValue({
      spuId: undefined,
      skuId: undefined,
      stockId: undefined,
      warehouseName: undefined,
    });
    setSelectedSpu(undefined);
    setSelectedSku(undefined);
    setSelectedStock(undefined);
    onSelectionChange();
  };

  const spuColumns: ProColumns<API.InventoryOverview.OverviewItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '商品名 / SPU' },
    },
    {
      title: '商品信息',
      dataIndex: 'productName',
      width: 240,
      search: false,
      render: (_, record) => renderProductInfo(record),
    },
    {
      title: '卖家',
      dataIndex: 'sellerName',
      width: 160,
      search: false,
      render: (_, record) => renderSeller(record),
    },
    {
      title: 'SKU数',
      dataIndex: 'skuCount',
      width: 80,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      width: 80,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '来源可用',
      dataIndex: 'sourceAvailableQty',
      width: 100,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台总库存',
      dataIndex: 'platformTotalQty',
      width: 110,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '状态',
      dataIndex: 'inventoryStatus',
      width: 120,
      search: false,
      render: (value) => renderStatus(String(value || '')),
    },
  ];

  const skuColumns: ProColumns<API.InventoryOverview.OverviewItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '商品名 / SKU' },
    },
    {
      title: 'SKU信息',
      dataIndex: 'systemSkuCode',
      width: 260,
      search: false,
      render: (_, record) => renderSkuInfo(record),
    },
    {
      title: '卖家',
      dataIndex: 'sellerName',
      width: 160,
      search: false,
      render: (_, record) => renderSeller(record),
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      width: 80,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '来源可用',
      dataIndex: 'sourceAvailableQty',
      width: 100,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台总库存',
      dataIndex: 'platformTotalQty',
      width: 110,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台可售',
      dataIndex: 'platformAvailableQty',
      width: 100,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '状态',
      dataIndex: 'inventoryStatus',
      width: 120,
      search: false,
      render: (value) => renderStatus(String(value || '')),
    },
  ];

  const stockColumns: ProColumns<API.InventoryOverview.WarehouseStock>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '商品名 / SKU / 仓库' },
    },
    {
      title: 'SKU信息',
      dataIndex: 'systemSkuCode',
      width: 240,
      search: false,
      render: (_, record) => renderSkuInfo(record),
    },
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 160,
      search: false,
      render: (_, record) => renderWarehouse(record),
    },
    {
      title: '类型',
      dataIndex: 'warehouseKind',
      width: 90,
      search: false,
      render: (value) => renderWarehouseKind(String(value || '')),
    },
    {
      title: '卖家',
      dataIndex: 'sellerName',
      width: 150,
      search: false,
      render: (_, record) => renderSeller(record),
    },
    {
      title: '来源可用',
      dataIndex: 'sourceAvailableQty',
      width: 100,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台总库存',
      dataIndex: 'platformTotalQty',
      width: 110,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '平台可售',
      dataIndex: 'platformAvailableQty',
      width: 100,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '状态',
      dataIndex: 'effectiveStatus',
      width: 120,
      search: false,
      render: (value) => renderStatus(String(value || '')),
    },
  ];

  if (!['SPU', 'SKU', 'SKU_WAREHOUSE'].includes(currentScope)) {
    return null;
  }

  if (currentScope === 'SPU') {
    return (
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        <SelectedTargetBoard title="已选择SPU" locked={lockScope} onClear={clearTarget}>
          {selectedSpu || selectedSpuId ? (
            <Typography.Text>
              {selectedSpu?.productName || '-'} / {selectedSpu?.systemSpuCode || selectedSpuId}
            </Typography.Text>
          ) : (
            <Typography.Text type="secondary">未选择</Typography.Text>
          )}
        </SelectedTargetBoard>
        {!lockScope ? (
          <ProTable<API.InventoryOverview.OverviewItem>
            rowKey={(record) => record.spuId || record.stockKey || ''}
            columns={spuColumns}
            size="small"
            options={false}
            search={{ labelWidth: 70, span: 8 }}
            pagination={{ pageSize: 5, showSizeChanger: false }}
            tableAlertRender={false}
            tableAlertOptionRender={false}
            rowSelection={{
              type: 'radio',
              preserveSelectedRowKeys: true,
              selectedRowKeys: selectedSpuId ? [selectedSpuId] : [],
              onSelect: (record) => {
                form.setFieldsValue({ spuId: record.spuId, sellerId: record.sellerId });
                setSelectedSpu(record);
                onSelectionChange();
              },
            }}
            request={async (params) => {
              const resp = await getInventoryOverviewSpuList(buildListParams(params));
              return { data: resp.rows || [], total: resp.total || 0, success: resp.code === 200 };
            }}
            scroll={{ x: 1020, y: 260 }}
          />
        ) : null}
      </Space>
    );
  }

  if (currentScope === 'SKU') {
    return (
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        <SelectedTargetBoard title="已选择SKU" locked={lockScope} onClear={clearTarget}>
          {selectedSku || selectedSkuId ? (
            <Typography.Text>
              {selectedSku?.systemSkuCode || selectedSkuId} / {selectedSku?.productName || '-'}
            </Typography.Text>
          ) : (
            <Typography.Text type="secondary">未选择</Typography.Text>
          )}
        </SelectedTargetBoard>
        {!lockScope ? (
          <ProTable<API.InventoryOverview.OverviewItem>
            rowKey={(record) => record.skuId || record.stockKey || ''}
            columns={skuColumns}
            size="small"
            options={false}
            search={{ labelWidth: 70, span: 8 }}
            pagination={{ pageSize: 5, showSizeChanger: false }}
            tableAlertRender={false}
            tableAlertOptionRender={false}
            rowSelection={{
              type: 'radio',
              preserveSelectedRowKeys: true,
              selectedRowKeys: selectedSkuId ? [selectedSkuId] : [],
              onSelect: (record) => {
                form.setFieldsValue({ sellerId: record.sellerId, spuId: record.spuId, skuId: record.skuId });
                setSelectedSku(record);
                onSelectionChange();
              },
            }}
            request={async (params) => {
              const resp = await getInventoryOverviewSkuList(buildListParams(params));
              return { data: resp.rows || [], total: resp.total || 0, success: resp.code === 200 };
            }}
            scroll={{ x: 1050, y: 260 }}
          />
        ) : null}
      </Space>
    );
  }

  return (
    <Space direction="vertical" size={10} style={{ width: '100%' }}>
      <SelectedTargetBoard title="已选择明细行" locked={lockScope} onClear={clearTarget}>
        {selectedStock || selectedStockId ? (
          <Typography.Text>
            {selectedStock?.systemSkuCode || selectedStockId} / {selectedStock?.warehouseName || '-'}
          </Typography.Text>
        ) : (
          <Typography.Text type="secondary">未选择</Typography.Text>
        )}
      </SelectedTargetBoard>
      {!lockScope ? (
        <ProTable<API.InventoryOverview.WarehouseStock>
          rowKey={(record) => record.stockId || record.stockKey || ''}
          columns={stockColumns}
          size="small"
          options={false}
          search={{ labelWidth: 70, span: 8 }}
          pagination={{ pageSize: 5, showSizeChanger: false }}
          tableAlertRender={false}
          tableAlertOptionRender={false}
          rowSelection={{
            type: 'radio',
            preserveSelectedRowKeys: true,
            selectedRowKeys: selectedStockId ? [selectedStockId] : [],
            onSelect: (record) => {
              form.setFieldsValue({
                sellerId: record.sellerId,
                spuId: record.spuId,
                skuId: record.skuId,
                stockId: record.stockId,
                warehouseName: record.warehouseName,
              });
              setSelectedStock(record);
              onSelectionChange();
            },
          }}
          request={async (params) => {
            const resp = await getInventoryOverviewWarehouseList(buildListParams(params));
            return { data: resp.rows || [], total: resp.total || 0, success: resp.code === 200 };
          }}
          scroll={{ x: 1170, y: 260 }}
        />
      ) : null}
    </Space>
  );
}
