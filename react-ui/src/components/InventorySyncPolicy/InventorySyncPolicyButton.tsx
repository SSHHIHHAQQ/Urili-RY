import { Button, Descriptions, Form, Input, Modal, Radio, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  confirmInventoryOverviewSyncPolicy,
  previewInventoryOverviewSyncPolicy,
} from '@/services/inventory/overview';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import InventorySyncPolicyTargetPicker from './InventorySyncPolicyTargetPicker';

const scopeOptions = [
  { label: '卖家维度', value: 'SELLER' },
  { label: '仓库设置', value: 'WAREHOUSE' },
  { label: 'SPU设置', value: 'SPU' },
  { label: 'SKU设置', value: 'SKU' },
  { label: '明细行设置', value: 'SKU_WAREHOUSE' },
] as const;

const syncModeOptions = [
  { label: '手动设置平台库存', value: 'MANUAL' },
  { label: '自动同步WMS库存', value: 'AUTO_SOURCE_AVAILABLE' },
] as const;

const syncModeText: Record<string, string> = {
  MANUAL: '手动设置平台库存',
  AUTO_SOURCE_AVAILABLE: '自动同步WMS库存',
};

const scopeText: Record<string, string> = {
  SELLER: '卖家维度',
  WAREHOUSE: '仓库设置',
  SPU: 'SPU设置',
  SKU: 'SKU设置',
  SKU_WAREHOUSE: '明细行设置',
  SYSTEM: '系统默认',
};

function qty(value?: number | null) {
  return Number(value || 0);
}

function formatQuantity(value?: number | null) {
  return new Intl.NumberFormat('zh-CN').format(qty(value));
}

function renderMode(value?: string) {
  return <Tag color={value === 'AUTO_SOURCE_AVAILABLE' ? 'processing' : 'default'}>{syncModeText[value || 'MANUAL'] || value || '-'}</Tag>;
}

function renderDelta(value?: number) {
  const delta = qty(value);
  return (
    <Typography.Text type={delta < 0 ? 'danger' : delta > 0 ? 'success' : 'secondary'}>
      {formatQuantity(delta)}
    </Typography.Text>
  );
}

function buildInitialRequest(
  scope: API.InventoryOverview.SyncPolicyScope,
  overviewRecord?: API.InventoryOverview.OverviewItem,
  warehouseRecord?: API.InventoryOverview.WarehouseStock,
): API.InventoryOverview.SyncPolicyRequest {
  if (scope === 'SPU') {
    return {
      scopeType: 'SPU',
      syncMode: 'AUTO_SOURCE_AVAILABLE',
      sellerId: overviewRecord?.sellerId,
      spuId: overviewRecord?.spuId,
    };
  }
  if (scope === 'SKU') {
    return {
      scopeType: 'SKU',
      syncMode: 'AUTO_SOURCE_AVAILABLE',
      sellerId: overviewRecord?.sellerId,
      skuId: overviewRecord?.skuId,
    };
  }
  if (scope === 'SKU_WAREHOUSE') {
    return {
      scopeType: 'SKU_WAREHOUSE',
      syncMode: 'AUTO_SOURCE_AVAILABLE',
      sellerId: warehouseRecord?.sellerId,
      stockId: warehouseRecord?.stockId,
      skuId: warehouseRecord?.skuId,
      spuId: warehouseRecord?.spuId,
      warehouseName: warehouseRecord?.warehouseName,
    };
  }
  return { scopeType: scope, syncMode: 'AUTO_SOURCE_AVAILABLE' };
}

export function InventorySyncPolicyModal({
  open,
  onOpenChange,
  initialScope = 'SELLER',
  lockScope = false,
  overviewRecord,
  warehouseRecord,
  sellerOptions,
  warehouseOptions,
  onChanged,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialScope?: API.InventoryOverview.SyncPolicyScope;
  lockScope?: boolean;
  overviewRecord?: API.InventoryOverview.OverviewItem;
  warehouseRecord?: API.InventoryOverview.WarehouseStock;
  sellerOptions: API.InventoryOverview.SellerOption[];
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
  onChanged: () => void;
}) {
  const [form] = Form.useForm<API.InventoryOverview.SyncPolicyRequest>();
  const [preview, setPreview] = useState<API.InventoryOverview.SyncPolicyPreview>();
  const [previewing, setPreviewing] = useState(false);
  const [saving, setSaving] = useState(false);
  const scopeType = Form.useWatch('scopeType', form);

  const currentScope = (scopeType || initialScope) as API.InventoryOverview.SyncPolicyScope;
  const warehouseSelectOptions = useMemo(
    () => warehouseOptions.map((item) => ({
      label: item.label,
      value: item.value,
      warehouseName: item.warehouseName,
    })),
    [warehouseOptions],
  );

  useEffect(() => {
    if (!open) {
      setPreview(undefined);
      return;
    }
    form.setFieldsValue(buildInitialRequest(initialScope, overviewRecord, warehouseRecord));
    setPreview(undefined);
  }, [form, initialScope, open, overviewRecord, warehouseRecord]);

  const resetPreview = () => setPreview(undefined);

  const resetScopeTarget = () => {
    form.setFieldsValue({
      warehouseKey: undefined,
      warehouseKeys: undefined,
      warehouseName: undefined,
      spuId: undefined,
      skuId: undefined,
      stockId: undefined,
    });
    resetPreview();
  };

  const buildPayload = async (confirmed?: boolean) => {
    const values = await form.validateFields();
    const warehouseKeys = values.warehouseKeys?.length
      ? values.warehouseKeys
      : values.warehouseKey
        ? [values.warehouseKey]
        : undefined;
    const selectedWarehouse = warehouseKeys?.length === 1
      ? warehouseSelectOptions.find((item) => item.value === warehouseKeys[0])
      : undefined;
    return {
      ...values,
      confirmed,
      warehouseKeys,
      warehouseKey: warehouseKeys?.length === 1 ? warehouseKeys[0] : undefined,
      warehouseName: values.warehouseName || selectedWarehouse?.warehouseName,
    };
  };

  const doPreview = async () => {
    setPreviewing(true);
    try {
      const payload = await buildPayload(false);
      const resp = await previewInventoryOverviewSyncPolicy(payload);
      if (resp.code !== 200) {
        message.warning(resp.msg || '预览失败');
        return;
      }
      setPreview(resp.data);
      if (!resp.data?.allowed) {
        message.warning(resp.data?.message || '当前设置不能保存');
      }
    } finally {
      setPreviewing(false);
    }
  };

  const doConfirm = async () => {
    if (!preview?.allowed) {
      message.warning('请先完成预览');
      return;
    }
    setSaving(true);
    try {
      const payload = await buildPayload(true);
      const resp = await confirmInventoryOverviewSyncPolicy(payload);
      if (resp.code === 200) {
        message.success('库存同步方式已更新');
        onOpenChange(false);
        setPreview(undefined);
        onChanged();
      } else {
        message.warning(resp.msg || '保存失败');
      }
    } finally {
      setSaving(false);
    }
  };

  const columns: ColumnsType<API.InventoryOverview.SyncPolicyPreviewRow> = [
    {
      title: 'SKU',
      dataIndex: 'systemSkuCode',
      width: 180,
      fixed: 'left',
      render: (_, record) => (
        <>
          <Typography.Text strong>{record.systemSkuCode || '-'}</Typography.Text>
          <br />
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.skuName || record.productName }}>
            {record.skuName || record.productName || '-'}
          </Typography.Text>
        </>
      ),
    },
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 150,
      render: (value) => value || '-',
    },
    {
      title: '当前方式',
      dataIndex: 'beforeSyncMode',
      width: 150,
      render: (value) => renderMode(String(value || 'MANUAL')),
    },
    {
      title: '应用后方式',
      dataIndex: 'afterSyncMode',
      width: 160,
      render: (_, record) => (
        <Space size={4} wrap>
          {renderMode(record.afterSyncMode)}
          <Tag>{scopeText[record.afterPolicyScope || 'SYSTEM'] || record.afterPolicyScope}</Tag>
        </Space>
      ),
    },
    {
      title: '来源可用',
      dataIndex: 'sourceAvailableQty',
      width: 100,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '同步前平台总库存',
      dataIndex: 'beforePlatformTotalQty',
      width: 140,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '同步后平台总库存',
      dataIndex: 'afterPlatformTotalQty',
      width: 140,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '增减',
      dataIndex: 'platformTotalDeltaQty',
      width: 90,
      align: 'right',
      render: (value) => renderDelta(value as number),
    },
    {
      title: '结果',
      dataIndex: 'eligible',
      width: 130,
      render: (_, record) => record.eligible ? <Tag color="green">可应用</Tag> : <Tag color="orange">跳过</Tag>,
    },
    {
      title: '说明',
      dataIndex: 'message',
      width: 240,
      render: (value) => <Typography.Text type="secondary">{value || '-'}</Typography.Text>,
    },
  ];

  return (
    <Modal
      title="自动同步WMS库存设置"
      open={open}
      width={1120}
      onCancel={() => onOpenChange(false)}
      onOk={doConfirm}
      okText="确认应用"
      okButtonProps={{ disabled: !preview?.allowed }}
      confirmLoading={saving}
      destroyOnClose
    >
      <Space direction="vertical" size={14} style={{ width: '100%' }}>
        <Form
          form={form}
          layout="vertical"
          onValuesChange={resetPreview}
          initialValues={buildInitialRequest(initialScope, overviewRecord, warehouseRecord)}
        >
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Form.Item name="scopeType" label="设置范围" rules={[{ required: true, message: '请选择设置范围' }]}>
              <Radio.Group buttonStyle="solid" disabled={lockScope} onChange={resetScopeTarget}>
                {scopeOptions.map((item) => (
                  <Radio.Button key={item.value} value={item.value}>
                    {item.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
            </Form.Item>
            <Form.Item name="syncMode" label="库存同步方式" rules={[{ required: true, message: '请选择库存同步方式' }]}>
              <Radio.Group buttonStyle="solid">
                {syncModeOptions.map((item) => (
                  <Radio.Button key={item.value} value={item.value}>
                    {item.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
            </Form.Item>
            {currentScope === 'SELLER' || currentScope === 'WAREHOUSE' ? (
              <Form.Item name="sellerId" label="卖家" rules={[{ required: true, message: '请选择卖家' }]}>
                <Select
                  {...SEARCHABLE_SELECT_PROPS}
                  options={sellerOptions}
                  placeholder="请选择卖家"
                  onChange={() => {
                    form.setFieldValue('warehouseKeys', undefined);
                    resetPreview();
                  }}
                />
              </Form.Item>
            ) : null}
            {currentScope === 'WAREHOUSE' ? (
              <Form.Item name="warehouseKeys" label="仓库" rules={[{ required: true, message: '请选择仓库' }]}>
                <Select
                  {...SEARCHABLE_SELECT_PROPS}
                  mode="multiple"
                  options={warehouseSelectOptions}
                  placeholder="请选择官方仓库"
                />
              </Form.Item>
            ) : null}
            <Form.Item name="spuId" hidden rules={currentScope === 'SPU' ? [{ required: true, message: '请选择SPU' }] : []}>
              <Input />
            </Form.Item>
            <Form.Item name="skuId" hidden rules={currentScope === 'SKU' ? [{ required: true, message: '请选择SKU' }] : []}>
              <Input />
            </Form.Item>
            <Form.Item
              name="stockId"
              hidden
              rules={currentScope === 'SKU_WAREHOUSE' ? [{ required: true, message: '请选择库存明细行' }] : []}
            >
              <Input />
            </Form.Item>
            <InventorySyncPolicyTargetPicker
              form={form}
              currentScope={currentScope}
              lockScope={lockScope}
              overviewRecord={overviewRecord}
              warehouseRecord={warehouseRecord}
              onSelectionChange={resetPreview}
            />
            <Form.Item name="remark" label="备注">
              <Input.TextArea maxLength={500} rows={2} placeholder="备注（选填）" showCount />
            </Form.Item>
          </Space>
        </Form>

        <Space>
          <Button type="primary" onClick={doPreview} loading={previewing}>
            预览影响
          </Button>
          {preview ? <Typography.Text type={preview.allowed ? 'secondary' : 'danger'}>{preview.message}</Typography.Text> : null}
        </Space>

        {preview ? (
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            <Descriptions size="small" bordered column={4}>
              <Descriptions.Item label="影响明细">{formatQuantity(preview.affectedRowCount)}</Descriptions.Item>
              <Descriptions.Item label="可同步">{formatQuantity(preview.eligibleRowCount)}</Descriptions.Item>
              <Descriptions.Item label="跳过">{formatQuantity(preview.skippedRowCount)}</Descriptions.Item>
              <Descriptions.Item label="会更新">{formatQuantity(preview.changedRowCount)}</Descriptions.Item>
              <Descriptions.Item label="平台总库存" span={2}>
                {formatQuantity(preview.beforePlatformTotalQty)} → {formatQuantity(preview.afterPlatformTotalQty)}
              </Descriptions.Item>
              <Descriptions.Item label="平台可售库存" span={2}>
                {formatQuantity(preview.beforeAvailableQty)} → {formatQuantity(preview.afterAvailableQty)}
              </Descriptions.Item>
            </Descriptions>
            <Table<API.InventoryOverview.SyncPolicyPreviewRow>
              rowKey="stockId"
              size="small"
              columns={columns}
              dataSource={preview.rows || []}
              pagination={false}
              scroll={{ x: 1480, y: 320 }}
              tableLayout="fixed"
            />
          </Space>
        ) : null}
      </Space>
    </Modal>
  );
}

export default function InventorySyncPolicyButton({
  initialScope = 'SELLER',
  lockScope = false,
  overviewRecord,
  warehouseRecord,
  sellerOptions,
  warehouseOptions,
  canSync,
  onChanged,
  buttonText = '自动同步WMS库存设置',
  buttonType = 'link',
}: {
  initialScope?: API.InventoryOverview.SyncPolicyScope;
  lockScope?: boolean;
  overviewRecord?: API.InventoryOverview.OverviewItem;
  warehouseRecord?: API.InventoryOverview.WarehouseStock;
  sellerOptions: API.InventoryOverview.SellerOption[];
  warehouseOptions: API.InventoryOverview.WarehouseOption[];
  canSync: boolean;
  onChanged: () => void;
  buttonText?: string;
  buttonType?: 'link' | 'primary' | 'default';
}) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button type={buttonType} size={buttonType === 'link' ? 'small' : 'middle'} disabled={!canSync} onClick={() => setOpen(true)}>
        {buttonText}
      </Button>
      <InventorySyncPolicyModal
        open={open}
        onOpenChange={setOpen}
        initialScope={initialScope}
        lockScope={lockScope}
        overviewRecord={overviewRecord}
        warehouseRecord={warehouseRecord}
        sellerOptions={sellerOptions}
        warehouseOptions={warehouseOptions}
        onChanged={onChanged}
      />
    </>
  );
}
