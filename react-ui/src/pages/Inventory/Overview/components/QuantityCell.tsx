import { Button, Input, InputNumber, Modal, Space, Typography, message } from 'antd';
import { useState } from 'react';
import {
  confirmInventoryOverviewAdjust,
  previewInventoryOverviewAdjust,
} from '@/services/inventory/overview';
import { formatQuantity } from '../helpers';

export type AdjustField = 'PLATFORM_TOTAL' | 'PLATFORM_IN_TRANSIT';

export default function QuantityCell({
  record,
  field,
  value,
  disabled,
  onChanged,
}: {
  record: API.InventoryOverview.WarehouseStock;
  field: AdjustField;
  value?: number;
  disabled?: boolean;
  onChanged: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const [nextValue, setNextValue] = useState<number>(value || 0);
  const [reason, setReason] = useState('');
  const [saving, setSaving] = useState(false);

  const close = () => {
    setEditing(false);
    setNextValue(value || 0);
    setReason('');
  };

  const doConfirm = async (confirmed: boolean) => {
    setSaving(true);
    try {
      const resp = await confirmInventoryOverviewAdjust({
        stockId: record.stockId,
        adjustField: field,
        targetQty: nextValue,
        confirmed,
        reason: reason.trim(),
      });
      if (resp.code === 200) {
        message.success(resp.data?.reviewRequired
          ? `已生成库存调整审核单${resp.data.reviewNo ? `：${resp.data.reviewNo}` : ''}`
          : '库存已更新');
        setEditing(false);
        onChanged();
      } else {
        message.warning(resp.msg || '库存更新失败');
      }
    } finally {
      setSaving(false);
    }
  };

  const submit = async () => {
    if (!record.stockId) {
      return;
    }
    const normalizedReason = reason.trim();
    setSaving(true);
    try {
      const resp = await previewInventoryOverviewAdjust({
        stockId: record.stockId,
        adjustField: field,
        targetQty: nextValue,
        reason: normalizedReason,
      });
      const preview = resp.data;
      if (resp.code !== 200 || !preview?.allowed) {
        message.warning(preview?.message || resp.msg || '当前调整不允许保存');
        return;
      }
      if (preview.confirmationRequired) {
        Modal.confirm({
          title: '确认库存调整',
          content: (
            <Space direction="vertical" size={4}>
              <Typography.Text>{preview.message}</Typography.Text>
              <Typography.Text type="secondary">
                调整值：{formatQuantity(preview.beforeValue)} → {formatQuantity(preview.afterValue)}
              </Typography.Text>
              <Typography.Text type="secondary">
                平台可售：{formatQuantity(preview.beforeAvailableQty)} → {formatQuantity(preview.afterAvailableQty)}
              </Typography.Text>
              {preview.reviewRequired ? (
                <>
                  <Typography.Text type="secondary">
                    申请退回：{formatQuantity(preview.requestedAdjustQty)}，可立即退回：{formatQuantity(preview.immediateReturnableQty)}
                  </Typography.Text>
                  <Typography.Text type="secondary">
                    保护保留库存：{formatQuantity(preview.protectedRetainedQty)}
                  </Typography.Text>
                </>
              ) : null}
            </Space>
          ),
          onOk: () => doConfirm(true),
        });
      } else {
        await doConfirm(false);
      }
    } finally {
      setSaving(false);
    }
  };

  if (!editing) {
    return (
      <Typography.Text
        onDoubleClick={() => {
          if (!disabled) {
            setNextValue(value || 0);
            setEditing(true);
          }
        }}
        style={{ cursor: disabled ? 'default' : 'text' }}
      >
        {formatQuantity(value)}
      </Typography.Text>
    );
  }

  return (
    <Space direction="vertical" size={4}>
      <InputNumber
        min={0}
        precision={0}
        value={nextValue}
        onChange={(val) => setNextValue(Number(val || 0))}
        style={{ width: 118 }}
      />
      <Input.TextArea
        value={reason}
        onChange={(event) => setReason(event.target.value)}
        maxLength={500}
        rows={2}
        placeholder="调整原因（选填）"
        style={{ width: 180 }}
      />
      <Space size={4}>
        <Button size="small" type="primary" loading={saving} onClick={submit}>
          确认
        </Button>
        <Button size="small" onClick={close}>
          取消
        </Button>
      </Space>
    </Space>
  );
}
