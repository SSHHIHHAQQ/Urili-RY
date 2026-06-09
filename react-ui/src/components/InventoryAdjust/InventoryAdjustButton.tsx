import { Button, Input, InputNumber, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  confirmInventoryOverviewBatchAdjust,
  getInventoryOverviewSpuSkuWarehouses,
  getInventoryOverviewWarehouses,
  previewInventoryOverviewBatchAdjust,
} from '@/services/inventory/overview';

type AdjustScope = 'SPU' | 'SKU' | 'WAREHOUSE';

type DraftRow = API.InventoryOverview.WarehouseStock & {
  targetPlatformTotalQty?: number;
  targetPlatformInTransitQty?: number;
  previewMessage?: string;
  previewAllowed?: boolean;
};

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
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN').format(Number(value));
}

function renderStatus(value?: string) {
  const text = inventoryStatusText[value || ''] || value || '-';
  return <Tag color={inventoryStatusColor[value || ''] || 'default'}>{text}</Tag>;
}

function renderWarehouseKind(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={warehouseKindColor[value] || 'gold'}>{warehouseKindText[value] || value}</Tag>;
}

function effectiveSourceAvailable(record: API.InventoryOverview.WarehouseStock) {
  return Math.max(0, qty(record.sourceAvailableQty) - qty(record.pendingSourceDeductionQty));
}

function maxPlatformInTransit(record: API.InventoryOverview.WarehouseStock) {
  return qty(record.sourceInTransitQty) + qty(record.pendingAvailableInboundQty);
}

function canAdjustPlatformTotal(record: API.InventoryOverview.WarehouseStock) {
  return record.warehouseRefType !== 'NO_WAREHOUSE' && record.syncMode !== 'AUTO_SOURCE_AVAILABLE';
}

function canAdjustPlatformInTransit(record: API.InventoryOverview.WarehouseStock) {
  return record.warehouseRefType !== 'NO_WAREHOUSE' && record.warehouseKind === 'official';
}

function flattenSpuWarehouseGroups(groups: API.InventoryOverview.SkuWarehouseGroup[]) {
  return groups.flatMap((group) =>
    (group.warehouses || []).map((warehouse) => ({
      ...warehouse,
      productName: warehouse.productName || group.sku?.productName,
      skuName: warehouse.skuName || group.sku?.skuName,
      systemSkuCode: warehouse.systemSkuCode || group.sku?.systemSkuCode,
    })),
  );
}

function buildInitialRows(rows: API.InventoryOverview.WarehouseStock[]): DraftRow[] {
  return rows.map((row) => ({
    ...row,
    targetPlatformTotalQty: qty(row.platformTotalQty),
    targetPlatformInTransitQty: qty(row.platformInTransitQty),
  }));
}

function buildModalTitle(scope: AdjustScope, record?: API.InventoryOverview.OverviewItem) {
  if (scope === 'SPU') {
    return `调整库存：${record?.productName || record?.systemSpuCode || 'SPU'}`;
  }
  if (scope === 'SKU') {
    return `调整库存：${record?.systemSkuCode || 'SKU'}`;
  }
  return '调整库存';
}

export function InventoryAdjustModal({
  open,
  onOpenChange,
  scope,
  overviewRecord,
  warehouseRecord,
  presetRows,
  canAdjust,
  onChanged,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  scope: AdjustScope;
  overviewRecord?: API.InventoryOverview.OverviewItem;
  warehouseRecord?: API.InventoryOverview.WarehouseStock;
  presetRows?: API.InventoryOverview.WarehouseStock[];
  canAdjust: boolean;
  onChanged: () => void;
}) {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [rows, setRows] = useState<DraftRow[]>([]);
  const [reason, setReason] = useState('');

  const summary = useMemo(() => ({
    rowCount: rows.length,
    platformTotalQty: rows.reduce((sum, row) => sum + qty(row.platformTotalQty), 0),
    platformInTransitQty: rows.reduce((sum, row) => sum + qty(row.platformInTransitQty), 0),
    platformAvailableQty: rows.reduce((sum, row) => sum + qty(row.platformAvailableQty), 0),
  }), [rows]);

  const updateRow = (stockId: number | undefined, patch: Partial<DraftRow>) => {
    setRows((prevRows) => prevRows.map((row) => (
      row.stockId === stockId ? { ...row, ...patch, previewAllowed: undefined, previewMessage: undefined } : row
    )));
  };

  const loadRows = async () => {
    setLoading(true);
    try {
      if (presetRows) {
        setRows(buildInitialRows(presetRows));
        return;
      }
      if (scope === 'WAREHOUSE' && warehouseRecord) {
        setRows(buildInitialRows([warehouseRecord]));
        return;
      }
      if (scope === 'SKU' && overviewRecord?.skuId) {
        const resp = await getInventoryOverviewWarehouses(overviewRecord.skuId);
        setRows(buildInitialRows(resp.code === 200 ? resp.data || [] : []));
        return;
      }
      if (scope === 'SPU' && overviewRecord?.spuId) {
        const resp = await getInventoryOverviewSpuSkuWarehouses(overviewRecord.spuId);
        setRows(buildInitialRows(resp.code === 200 ? flattenSpuWarehouseGroups(resp.data || []) : []));
        return;
      }
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  const closeModal = () => {
    onOpenChange(false);
    setRows([]);
    setReason('');
  };

  useEffect(() => {
    if (!open) {
      setRows([]);
      setReason('');
      return;
    }
    setReason('');
    void loadRows();
    // 只在弹窗打开或切换目标时重新取库存行，避免编辑输入时被无关渲染重置。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, scope, overviewRecord?.spuId, overviewRecord?.skuId, warehouseRecord?.stockId]);

  const buildChangedItems = () => rows
    .filter((row) =>
      qty(row.targetPlatformTotalQty) !== qty(row.platformTotalQty)
      || qty(row.targetPlatformInTransitQty) !== qty(row.platformInTransitQty),
    )
    .map((row) => ({
      stockId: row.stockId,
      targetPlatformTotalQty: qty(row.targetPlatformTotalQty),
      targetPlatformInTransitQty: qty(row.targetPlatformInTransitQty),
    }));

  const applyPreviewMessages = (previewRows?: API.InventoryOverview.BatchAdjustRowPreview[]) => {
    const previewMap = new Map((previewRows || []).map((row) => [row.stockId, row]));
    setRows((prevRows) => prevRows.map((row) => {
      const previewRow = previewMap.get(row.stockId);
      return previewRow
        ? { ...row, previewAllowed: previewRow.allowed, previewMessage: previewRow.message }
        : row;
    }));
  };

  const doConfirm = async (items: API.InventoryOverview.BatchAdjustItem[], normalizedReason: string) => {
    setSaving(true);
    try {
      const resp = await confirmInventoryOverviewBatchAdjust({
        items,
        confirmed: true,
        reason: normalizedReason,
      });
      if (resp.code === 200) {
        const reviewRequiredCount = qty(resp.data?.reviewRequiredCount);
        message.success(reviewRequiredCount > 0
          ? `库存已更新，并生成${formatQuantity(reviewRequiredCount)}条库存调整审核单`
          : '库存已更新');
        closeModal();
        onChanged();
      } else {
        message.warning(resp.msg || '库存更新失败');
      }
    } finally {
      setSaving(false);
    }
  };

  const submit = async () => {
    const normalizedReason = reason.trim();
    const items = buildChangedItems();
    if (!items.length) {
      message.warning('没有可保存的库存调整');
      return;
    }
    setSaving(true);
    try {
      if (!canAdjust) {
        message.warning('缺少库存调整权限');
        return;
      }
      const resp = await previewInventoryOverviewBatchAdjust({ items, reason: normalizedReason });
      const preview = resp.data;
      applyPreviewMessages(preview?.rows);
      if (resp.code !== 200 || !preview?.allowed) {
        message.warning(preview?.message || resp.msg || '当前调整不允许保存');
        return;
      }
      Modal.confirm({
        title: '确认库存调整',
        content: (
          <Space direction="vertical" size={4}>
            <Typography.Text>{preview.message}</Typography.Text>
            <Typography.Text type="secondary">
              平台总库存：{formatQuantity(preview.beforePlatformTotalQty)} → {formatQuantity(preview.afterPlatformTotalQty)}
            </Typography.Text>
            <Typography.Text type="secondary">
              平台在途库存：{formatQuantity(preview.beforePlatformInTransitQty)} → {formatQuantity(preview.afterPlatformInTransitQty)}
            </Typography.Text>
            <Typography.Text type="secondary">
              平台可售库存：{formatQuantity(preview.beforeAvailableQty)} → {formatQuantity(preview.afterAvailableQty)}
            </Typography.Text>
            {qty(preview.reviewRequiredCount) > 0 ? (
              <Typography.Text type="secondary">
                将生成库存调整审核单：{formatQuantity(preview.reviewRequiredCount)}条
              </Typography.Text>
            ) : null}
          </Space>
        ),
        onOk: () => doConfirm(items, normalizedReason),
      });
    } finally {
      setSaving(false);
    }
  };

  const columns: ColumnsType<DraftRow> = [
    {
      title: 'SKU',
      dataIndex: 'systemSkuCode',
      width: 180,
      fixed: 'left',
      render: (_, record) => (
        <>
          <Typography.Text strong>{record.systemSkuCode || '-'}</Typography.Text>
          {record.skuName ? (
            <>
              <br />
              <Typography.Text type="secondary" ellipsis={{ tooltip: record.skuName }}>
                {record.skuName}
              </Typography.Text>
            </>
          ) : null}
        </>
      ),
    },
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 170,
      fixed: 'left',
      render: (_, record) => (
        <>
          <Typography.Text>{record.warehouseName || '-'}</Typography.Text>
          <br />
          {renderWarehouseKind(record.warehouseKind)}
        </>
      ),
    },
    {
      title: '来源可用',
      dataIndex: 'sourceAvailableQty',
      width: 100,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceAvailableQty) : '-'),
    },
    {
      title: '来源在途',
      dataIndex: 'sourceInTransitQty',
      width: 100,
      align: 'right',
      render: (_, record) => (record.warehouseKind === 'official' ? formatQuantity(record.sourceInTransitQty) : '-'),
    },
    {
      title: '当前总库存',
      dataIndex: 'platformTotalQty',
      width: 110,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '调整后总库存',
      dataIndex: 'targetPlatformTotalQty',
      width: 150,
      align: 'right',
      render: (_, record) => (
        <InputNumber
          min={0}
          max={record.warehouseKind === 'official' ? effectiveSourceAvailable(record) : undefined}
          precision={0}
          value={record.targetPlatformTotalQty}
          disabled={!canAdjustPlatformTotal(record)}
          onChange={(value) => updateRow(record.stockId, { targetPlatformTotalQty: Number(value || 0) })}
          style={{ width: 126 }}
        />
      ),
    },
    {
      title: '总库存增减',
      dataIndex: 'platformTotalDeltaQty',
      width: 110,
      align: 'right',
      render: (_, record) => {
        const delta = qty(record.targetPlatformTotalQty) - qty(record.platformTotalQty);
        return <Typography.Text type={delta < 0 ? 'danger' : delta > 0 ? 'success' : 'secondary'}>{formatQuantity(delta)}</Typography.Text>;
      },
    },
    {
      title: '当前在途',
      dataIndex: 'platformInTransitQty',
      width: 100,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '调整后在途',
      dataIndex: 'targetPlatformInTransitQty',
      width: 150,
      align: 'right',
      render: (_, record) => (
        <InputNumber
          min={0}
          max={record.warehouseKind === 'official' ? maxPlatformInTransit(record) : 0}
          precision={0}
          value={record.targetPlatformInTransitQty}
          disabled={!canAdjustPlatformInTransit(record)}
          onChange={(value) => updateRow(record.stockId, { targetPlatformInTransitQty: Number(value || 0) })}
          style={{ width: 126 }}
        />
      ),
    },
    {
      title: '在途增减',
      dataIndex: 'platformInTransitDeltaQty',
      width: 100,
      align: 'right',
      render: (_, record) => {
        const delta = qty(record.targetPlatformInTransitQty) - qty(record.platformInTransitQty);
        return <Typography.Text type={delta < 0 ? 'danger' : delta > 0 ? 'success' : 'secondary'}>{formatQuantity(delta)}</Typography.Text>;
      },
    },
    {
      title: '平台锁定',
      dataIndex: 'platformReservedQty',
      width: 100,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '可售库存',
      dataIndex: 'platformAvailableQty',
      width: 100,
      align: 'right',
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '状态',
      dataIndex: 'effectiveStatus',
      width: 120,
      render: (value) => renderStatus(String(value || '')),
    },
    {
      title: '校验',
      dataIndex: 'previewMessage',
      width: 180,
      render: (_, record) => record.previewMessage ? (
        <Typography.Text type={record.previewAllowed === false ? 'danger' : 'secondary'}>
          {record.previewMessage}
        </Typography.Text>
      ) : '-',
    },
  ];

  return (
      <Modal
        title={buildModalTitle(scope, overviewRecord)}
        open={open}
        width={1180}
        confirmLoading={saving}
        onOk={submit}
        onCancel={closeModal}
        okText="预览并保存"
        destroyOnClose
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space size={18} wrap>
            <Typography.Text type="secondary">明细行 {formatQuantity(summary.rowCount)}</Typography.Text>
            <Typography.Text type="secondary">平台总库存 {formatQuantity(summary.platformTotalQty)}</Typography.Text>
            <Typography.Text type="secondary">平台在途库存 {formatQuantity(summary.platformInTransitQty)}</Typography.Text>
            <Typography.Text type="secondary">平台可售库存 {formatQuantity(summary.platformAvailableQty)}</Typography.Text>
          </Space>
          <Table<DraftRow>
            rowKey="stockId"
            size="small"
            loading={loading}
            columns={columns}
            dataSource={rows}
            pagination={false}
            scroll={{ x: 1680, y: 360 }}
            tableLayout="fixed"
          />
          <Input.TextArea
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            maxLength={500}
            rows={2}
            placeholder="调整原因（选填）"
            showCount
          />
        </Space>
      </Modal>
  );
}

export default function InventoryAdjustButton({
  scope,
  overviewRecord,
  warehouseRecord,
  presetRows,
  canAdjust,
  onChanged,
  buttonText = '调整库存',
}: {
  scope: AdjustScope;
  overviewRecord?: API.InventoryOverview.OverviewItem;
  warehouseRecord?: API.InventoryOverview.WarehouseStock;
  presetRows?: API.InventoryOverview.WarehouseStock[];
  canAdjust: boolean;
  onChanged: () => void;
  buttonText?: string;
}) {
  const [open, setOpen] = useState(false);
  const disabled = !canAdjust || (scope === 'WAREHOUSE' && warehouseRecord?.warehouseRefType === 'NO_WAREHOUSE');

  return (
    <>
      <Button
        type="link"
        size="small"
        disabled={disabled}
        onClick={() => setOpen(true)}
      >
        {buttonText}
      </Button>
      <InventoryAdjustModal
        open={open}
        onOpenChange={setOpen}
        scope={scope}
        overviewRecord={overviewRecord}
        warehouseRecord={warehouseRecord}
        presetRows={presetRows}
        canAdjust={canAdjust}
        onChanged={onChanged}
      />
    </>
  );
}
