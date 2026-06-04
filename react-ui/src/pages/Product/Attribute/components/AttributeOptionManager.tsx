import { PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  type ProColumns,
  ProFormDigit,
  type ProFormInstance,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Modal } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  addAttributeOption,
  deleteAttributeOption,
  getAttributeOptionList,
  updateAttributeOption,
} from '@/services/product/product';
import { message } from '@/utils/feedback';
import {
  statusOptions,
  statusValueEnum,
  yesNoOptions,
  yesNoValueEnum,
} from '../../constants';

type AccessLike = {
  hasPerms: (permission: string) => boolean;
};

type AttributeOptionManagerProps = {
  access: AccessLike;
  attribute?: API.Product.Attribute;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const defaultOptionValues: Partial<API.Product.AttributeOption> = {
  sortOrder: 0,
  defaultFlag: 'N',
  status: '0',
};

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

export default function AttributeOptionManager({
  access,
  attribute,
  open,
  onOpenChange,
}: AttributeOptionManagerProps) {
  const actionRef = useRef<ActionType>(null);
  const formRef =
    useRef<ProFormInstance<API.Product.AttributeOption> | undefined>(
      undefined,
    );
  const [optionModalOpen, setOptionModalOpen] = useState(false);
  const [currentOption, setCurrentOption] =
    useState<API.Product.AttributeOption>();

  useEffect(() => {
    if (!optionModalOpen) {
      return;
    }
    formRef.current?.resetFields();
    formRef.current?.setFieldsValue(currentOption || defaultOptionValues);
  }, [optionModalOpen, currentOption]);

  const openCreateOption = () => {
    setCurrentOption(undefined);
    setOptionModalOpen(true);
  };

  const openEditOption = (record: API.Product.AttributeOption) => {
    setCurrentOption(record);
    setOptionModalOpen(true);
  };

  const saveOption = async (values: API.Product.AttributeOption) => {
    if (!attribute?.attributeId) {
      return false;
    }
    const resp = currentOption?.optionId
      ? await updateAttributeOption(
          attribute.attributeId,
          currentOption.optionId,
          values,
        )
      : await addAttributeOption(attribute.attributeId, values);
    if (resultOk(resp, currentOption?.optionId ? '选项已更新' : '选项已新增')) {
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const removeOption = (record: API.Product.AttributeOption) => {
    const attributeId = attribute?.attributeId;
    const optionId = record.optionId;
    if (!attributeId || !optionId) {
      return;
    }
    Modal.confirm({
      title: '删除属性选项',
      content: `确认删除 ${record.optionLabel}？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteAttributeOption(attributeId, optionId),
          '选项已删除',
        );
        if (ok) actionRef.current?.reload();
      },
    });
  };

  const columns: ProColumns<API.Product.AttributeOption>[] = [
    {
      title: '选项编码',
      dataIndex: 'optionCode',
      width: 140,
    },
    {
      title: '选项名称',
      dataIndex: 'optionLabel',
      width: 180,
    },
    {
      title: '默认',
      dataIndex: 'defaultFlag',
      valueEnum: yesNoValueEnum,
      width: 90,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: statusValueEnum,
      width: 90,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 90,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 130,
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          size="small"
          hidden={!access.hasPerms('product:attribute:edit')}
          onClick={() => openEditOption(record)}
        >
          编辑
        </Button>,
        <Button
          key="delete"
          type="link"
          size="small"
          danger
          hidden={!access.hasPerms('product:attribute:edit')}
          onClick={() => removeOption(record)}
        >
          删除
        </Button>,
      ],
    },
  ];

  return (
    <>
      <Modal
        title={`属性选项：${attribute?.attributeName || ''}`}
        open={open}
        width={760}
        footer={null}
        destroyOnClose
        onCancel={() => onOpenChange(false)}
      >
        <ProTable<API.Product.AttributeOption>
          actionRef={actionRef}
          rowKey="optionId"
          columns={columns}
          search={false}
          pagination={false}
          request={async () => {
            if (!attribute?.attributeId) {
              return { data: [], success: true };
            }
            const resp = await getAttributeOptionList(attribute.attributeId);
            return { data: resp.data || [], success: resp.code === 200 };
          }}
          toolBarRender={() => [
            <Button
              key="add"
              type="primary"
              icon={<PlusOutlined />}
              hidden={!access.hasPerms('product:attribute:edit')}
              onClick={openCreateOption}
            >
              新增
            </Button>,
          ]}
        />
      </Modal>

      <ModalForm<API.Product.AttributeOption>
        title={currentOption?.optionId ? '编辑属性选项' : '新增属性选项'}
        open={optionModalOpen}
        formRef={formRef}
        modalProps={{
          destroyOnClose: true,
          onCancel: () => setOptionModalOpen(false),
        }}
        onOpenChange={setOptionModalOpen}
        onFinish={saveOption}
      >
        <ProFormText
          name="optionCode"
          label="选项编码"
          rules={[{ required: true, message: '请输入选项编码' }]}
        />
        <ProFormText
          name="optionLabel"
          label="选项名称"
          rules={[{ required: true, message: '请输入选项名称' }]}
        />
        <ProFormSelect name="defaultFlag" label="默认" options={yesNoOptions} />
        <ProFormDigit name="sortOrder" label="排序" min={0} />
        <ProFormSelect name="status" label="状态" options={statusOptions} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </>
  );
}
