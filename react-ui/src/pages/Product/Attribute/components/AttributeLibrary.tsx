import { DownOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  type ProColumns,
  ProFormDependency,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Dropdown, Form, Modal } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  addAttribute,
  deleteAttribute,
  downloadAttributeImportTemplate,
  downloadAttributeOptionImportTemplate,
  getAttributeList,
  importAttributeData,
  importAttributeOptionData,
  previewAttributeImport,
  previewAttributeOptionImport,
  updateAttribute,
} from '@/services/product/product';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  attributeTypeOptions,
  optionArrayToValueEnum,
  optionSourceOptions,
  statusOptions,
  statusValueEnum,
} from '../../constants';
import ProductImportModal from '../../components/ProductImportModal';
import AttributeOptionManager from './AttributeOptionManager';

type AccessLike = {
  hasPerms: (permission: string) => boolean;
};

type AttributeLibraryProps = {
  access: AccessLike;
};

const defaultAttributeValues: Partial<API.Product.Attribute> = {
  attributeType: 'TEXT',
  optionSource: 'NONE',
  valuePrecision: 0,
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

export default function AttributeLibrary({ access }: AttributeLibraryProps) {
  const actionRef = useRef<ActionType>(null);
  const [attributeForm] = Form.useForm<API.Product.Attribute>();
  const [attributeModalOpen, setAttributeModalOpen] = useState(false);
  const [attributeImportOpen, setAttributeImportOpen] = useState(false);
  const [optionImportOpen, setOptionImportOpen] = useState(false);
  const [optionListOpen, setOptionListOpen] = useState(false);
  const [currentAttribute, setCurrentAttribute] = useState<API.Product.Attribute>();
  const [optionAttribute, setOptionAttribute] = useState<API.Product.Attribute>();

  const attributeTypeValueEnum = useMemo(
    () => optionArrayToValueEnum(attributeTypeOptions),
    [],
  );
  const optionSourceValueEnum = useMemo(
    () => optionArrayToValueEnum(optionSourceOptions),
    [],
  );

  useEffect(() => {
    if (!attributeModalOpen) {
      return;
    }
    attributeForm.resetFields();
    attributeForm.setFieldsValue(currentAttribute || defaultAttributeValues);
  }, [attributeForm, attributeModalOpen, currentAttribute]);

  const openCreateAttribute = () => {
    setCurrentAttribute(undefined);
    setAttributeModalOpen(true);
  };

  const openEditAttribute = (record: API.Product.Attribute) => {
    setCurrentAttribute(record);
    setAttributeModalOpen(true);
  };

  const saveAttribute = async (values: API.Product.Attribute) => {
    const payload = { ...values };
    const resp = currentAttribute?.attributeId
      ? await updateAttribute(currentAttribute.attributeId, payload)
      : await addAttribute(payload);
    if (
      resultOk(resp, currentAttribute?.attributeId ? '属性已更新' : '属性已新增')
    ) {
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const removeAttribute = (record: API.Product.Attribute) => {
    const attributeId = record.attributeId;
    if (!attributeId) {
      return;
    }
    Modal.confirm({
      title: '删除商品属性',
      content: `确认删除 ${record.attributeName}？已被类目引用的属性会被后端拒绝删除。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteAttribute(attributeId),
          '属性已删除',
        );
        if (ok) actionRef.current?.reload();
      },
    });
  };

  const openOptions = (record: API.Product.Attribute) => {
    setOptionAttribute(record);
    setOptionListOpen(true);
  };

  const columns: ProColumns<API.Product.Attribute>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '属性编码',
      dataIndex: 'attributeCode',
      width: 160,
    },
    {
      title: '属性名称',
      dataIndex: 'attributeName',
      width: 160,
    },
    {
      title: '属性类型',
      dataIndex: 'attributeType',
      valueType: 'select',
      valueEnum: attributeTypeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 130,
    },
    {
      title: '选项来源',
      dataIndex: 'optionSource',
      valueType: 'select',
      valueEnum: optionSourceValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 150,
    },
    {
      title: '字典类型',
      dataIndex: 'dictType',
      width: 150,
      search: false,
    },
    {
      title: '单位',
      dataIndex: 'unit',
      width: 100,
      search: false,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 100,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 170,
      search: false,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 190,
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          size="small"
          hidden={!access.hasPerms('product:attribute:edit')}
          onClick={() => openEditAttribute(record)}
        >
          编辑
        </Button>,
        <Button
          key="options"
          type="link"
          size="small"
          hidden={
            !access.hasPerms('product:attribute:query') ||
            record.optionSource !== 'ATTRIBUTE_OPTION'
          }
          onClick={() => openOptions(record)}
        >
          选项
        </Button>,
        <Dropdown
          key="more"
          menu={{
            items: [
              {
                key: 'delete',
                label: '删除',
                danger: true,
                disabled: !access.hasPerms('product:attribute:remove'),
              },
            ],
            onClick: ({ key }) => {
              if (key === 'delete') removeAttribute(record);
            },
          }}
        >
          <Button type="link" size="small">
            更多 <DownOutlined />
          </Button>
        </Dropdown>,
      ],
    },
  ];

  return (
    <>
      <ProTable<API.Product.Attribute>
        actionRef={actionRef}
        rowKey="attributeId"
        columns={columns}
        search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-attribute')}
        request={async (params) => {
          const resp = await getAttributeList(params);
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        toolBarRender={() => [
          <Button
            key="importAttribute"
            icon={<ImportOutlined />}
            hidden={!access.hasPerms('product:attribute:add')}
            onClick={() => setAttributeImportOpen(true)}
          >
            导入属性
          </Button>,
          <Button
            key="importOption"
            icon={<ImportOutlined />}
            hidden={!access.hasPerms('product:attribute:edit')}
            onClick={() => setOptionImportOpen(true)}
          >
            导入选项
          </Button>,
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            hidden={!access.hasPerms('product:attribute:add')}
            onClick={openCreateAttribute}
          >
            新增
          </Button>,
        ]}
      />

      <ProductImportModal
        title="导入商品属性"
        open={attributeImportOpen}
        onOpenChange={setAttributeImportOpen}
        onDownloadTemplate={downloadAttributeImportTemplate}
        onPreview={previewAttributeImport}
        onImport={importAttributeData}
        onSuccess={() => actionRef.current?.reload()}
      />

      <ProductImportModal
        title="导入商品属性选项"
        open={optionImportOpen}
        onOpenChange={setOptionImportOpen}
        onDownloadTemplate={downloadAttributeOptionImportTemplate}
        onPreview={previewAttributeOptionImport}
        onImport={importAttributeOptionData}
        onSuccess={() => actionRef.current?.reload()}
      />

      <ModalForm<API.Product.Attribute>
        title={currentAttribute?.attributeId ? '编辑商品属性' : '新增商品属性'}
        open={attributeModalOpen}
        form={attributeForm}
        modalProps={{
          destroyOnHidden: true,
          onCancel: () => setAttributeModalOpen(false),
        }}
        onOpenChange={setAttributeModalOpen}
        onFinish={saveAttribute}
      >
        <ProFormText
          name="attributeCode"
          label="属性编码"
          rules={[{ required: true, message: '请输入属性编码' }]}
        />
        <ProFormText
          name="attributeName"
          label="属性名称"
          rules={[{ required: true, message: '请输入属性名称' }]}
        />
        <ProFormSelect
          name="attributeType"
          label="属性类型"
          options={attributeTypeOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择属性类型' }]}
        />
        <ProFormSelect
          name="optionSource"
          label="选项来源"
          options={optionSourceOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择选项来源' }]}
        />
        <ProFormDependency name={['optionSource']}>
          {({ optionSource }) =>
            optionSource === 'SYS_DICT' ? (
              <ProFormText
                name="dictType"
                label="字典类型"
                rules={[{ required: true, message: '请输入若依字典类型' }]}
              />
            ) : null
          }
        </ProFormDependency>
        <ProFormText name="unit" label="单位" />
        <ProFormDigit name="valuePrecision" label="数值精度" min={0} max={8} />
        <ProFormSelect
          name="status"
          label="状态"
          options={statusOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择状态' }]}
        />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <AttributeOptionManager
        access={access}
        attribute={optionAttribute}
        open={optionListOpen}
        onOpenChange={setOptionListOpen}
      />
    </>
  );
}
