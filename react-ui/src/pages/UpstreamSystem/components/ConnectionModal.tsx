import { Form, Input, Modal, Select } from 'antd';
import { useEffect } from 'react';
import {
  normalizeSettlementTypeValue,
  normalizeSystemKindValue,
  settlementOptions,
  systemKindOptions,
} from '@/pages/UpstreamSystem/constants';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

export type ConnectionModalMode = 'create' | 'edit' | 'credential';

interface ConnectionModalProps {
  mode: ConnectionModalMode;
  open: boolean;
  record?: API.Integration.UpstreamConnection;
  onCancel: () => void;
  onSubmit: (values: any) => Promise<boolean>;
}

export default function ConnectionModal({
  mode,
  open,
  record,
  onCancel,
  onSubmit,
}: ConnectionModalProps) {
  const [form] = Form.useForm();
  const titleMap = {
    create: '新增主仓接入',
    edit: '编辑主仓信息',
    credential: '重新授权',
  };

  useEffect(() => {
    if (!open) {
      form.resetFields();
      return;
    }
    form.setFieldsValue({
      systemKind: normalizeSystemKindValue(record?.systemKind),
      masterWarehouseName: record?.masterWarehouseName,
      settlementType: normalizeSettlementTypeValue(record?.settlementType),
      remark: record?.remark,
    });
  }, [form, open, record]);

  const showInfoFields = mode !== 'credential';
  const showCredentialFields = mode !== 'edit';

  return (
    <Modal
      title={titleMap[mode]}
      open={open}
      onCancel={onCancel}
      onOk={async () => {
        const values = await form.validateFields();
        const ok = await onSubmit(values);
        if (ok) {
          form.resetFields();
        }
      }}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          systemKind: 'lingxing-wms',
          settlementType: 'upstream-payable',
        }}
      >
        {showInfoFields ? (
          <>
            <Form.Item
              name="systemKind"
              label="上游系统类型"
              rules={[{ required: true, message: '请选择上游系统类型' }]}
            >
              <Select
                {...SEARCHABLE_SELECT_PROPS}
                disabled={mode !== 'create'}
                options={systemKindOptions}
              />
            </Form.Item>
            <Form.Item
              name="masterWarehouseName"
              label="主仓名称"
              rules={[{ required: true, message: '请输入主仓名称' }]}
            >
              <Input placeholder="例如 CA012" maxLength={200} />
            </Form.Item>
            <Form.Item
              name="settlementType"
              label="结算类型"
              rules={[{ required: true, message: '请选择结算类型' }]}
            >
              <Select {...SEARCHABLE_SELECT_PROPS} options={settlementOptions} />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea rows={3} maxLength={500} />
            </Form.Item>
          </>
        ) : null}
        {showCredentialFields ? (
          <>
            <Form.Item
              name="appKey"
              label="Key"
              rules={[{ required: true, message: '请输入Key' }]}
            >
              <Input autoComplete="off" />
            </Form.Item>
            <Form.Item
              name="appSecret"
              label="Secret"
              rules={[{ required: true, message: '请输入Secret' }]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          </>
        ) : null}
      </Form>
    </Modal>
  );
}
