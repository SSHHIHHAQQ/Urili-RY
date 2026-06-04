import { Form, Input, Modal } from 'antd';
import { useEffect } from 'react';

interface PairingModalProps {
  open: boolean;
  title: string;
  codeLabel: string;
  nameLabel: string;
  codeName: string;
  nameName: string;
  upstreamLabel: string;
  upstreamValue?: string;
  onCancel: () => void;
  onSubmit: (values: any) => Promise<boolean>;
}

export default function PairingModal({
  open,
  title,
  codeLabel,
  nameLabel,
  codeName,
  nameName,
  upstreamLabel,
  upstreamValue,
  onCancel,
  onSubmit,
}: PairingModalProps) {
  const [form] = Form.useForm();

  useEffect(() => {
    if (!open) {
      form.resetFields();
    }
  }, [form, open]);

  return (
    <Modal
      title={title}
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
      <Form form={form} layout="vertical">
        <Form.Item label={upstreamLabel}>
          <Input value={upstreamValue} disabled />
        </Form.Item>
        <Form.Item name={codeName} label={codeLabel} rules={[{ required: true, message: `请输入${codeLabel}` }]}>
          <Input maxLength={128} />
        </Form.Item>
        <Form.Item name={nameName} label={nameLabel} rules={[{ required: true, message: `请输入${nameLabel}` }]}>
          <Input maxLength={255} />
        </Form.Item>
        <Form.Item name="remark" label="备注">
          <Input.TextArea rows={3} maxLength={500} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
