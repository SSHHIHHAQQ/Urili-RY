import { settlementOptions } from '@/pages/UpstreamSystem/constants';
import { Form, Input, Modal, Select } from 'antd';
import { useEffect } from 'react';

export type ConnectionModalMode = 'create' | 'edit' | 'credential';

interface ConnectionModalProps {
  mode: ConnectionModalMode;
  open: boolean;
  record?: API.Integration.UpstreamConnection;
  onCancel: () => void;
  onSubmit: (values: any) => Promise<boolean>;
}

export default function ConnectionModal({ mode, open, record, onCancel, onSubmit }: ConnectionModalProps) {
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
      masterWarehouseName: record?.masterWarehouseName,
      settlementType: record?.settlementType || 'UPSTREAM_PAYABLE',
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
      <Form form={form} layout="vertical" initialValues={{ settlementType: 'UPSTREAM_PAYABLE' }}>
        {showInfoFields ? (
          <>
            <Form.Item name="masterWarehouseName" label="主仓名称" rules={[{ required: true, message: '请输入主仓名称' }]}>
              <Input placeholder="例如 CA012" maxLength={200} />
            </Form.Item>
            <Form.Item name="settlementType" label="结算类型" rules={[{ required: true, message: '请选择结算类型' }]}>
              <Select options={settlementOptions} />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea rows={3} maxLength={500} />
            </Form.Item>
          </>
        ) : null}
        {showCredentialFields ? (
          <>
            <Form.Item name="appKey" label="Key" rules={[{ required: true, message: '请输入Key' }]}>
              <Input autoComplete="off" />
            </Form.Item>
            <Form.Item name="appSecret" label="Secret" rules={[{ required: true, message: '请输入Secret' }]}>
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          </>
        ) : null}
      </Form>
    </Modal>
  );
}
