import { ImportOutlined, PlusOutlined } from '@ant-design/icons';
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
import { Button, Form, Switch } from 'antd';
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
  updateAttributeStatus,
} from '@/services/product/product';
import { getDictTypeOptionSelect } from '@/services/system/dict';
import { message, modal } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  attributeTypeOptions,
  isNumberAttributeType,
  isOptionAttributeType,
  optionArrayToValueEnum,
  optionSourceOptions,
  selectAttributeOptionSourceOptions,
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
  unit: '',
  valuePrecision: 0,
  status: '0',
};

function normalizeAttributeValues(
  values?: Partial<API.Product.Attribute>,
): Partial<API.Product.Attribute> {
  const next = { ...defaultAttributeValues, ...values };
  if (!isOptionAttributeType(next.attributeType)) {
    next.optionSource = 'NONE';
    next.dictType = '';
  } else if (
    next.optionSource !== 'ATTRIBUTE_OPTION' &&
    next.optionSource !== 'SYS_DICT'
  ) {
    next.optionSource = 'ATTRIBUTE_OPTION';
  }
  if (next.optionSource !== 'SYS_DICT') {
    next.dictType = '';
  }
  if (!isNumberAttributeType(next.attributeType)) {
    next.unit = '';
    next.valuePrecision = 0;
  } else {
    next.unit = next.unit || '';
    next.valuePrecision = next.valuePrecision ?? 0;
  }
  return next;
}

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
  const [statusUpdatingId, setStatusUpdatingId] = useState<number>();
  const [currentAttribute, setCurrentAttribute] = useState<API.Product.Attribute>();
  const [optionAttribute, setOptionAttribute] = useState<API.Product.Attribute>();
  const [dictTypeOptions, setDictTypeOptions] = useState<
    { label: string; value: string }[]
  >([]);

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
    attributeForm.setFieldsValue(normalizeAttributeValues(currentAttribute));
  }, [attributeForm, attributeModalOpen, currentAttribute]);

  useEffect(() => {
    getDictTypeOptionSelect()
      .then((resp: any) => {
        if (resp.code !== 200) {
          return;
        }
        const options = (resp.data || [])
          .filter((item: API.System.DictType) => item.status !== '1')
          .map((item: API.System.DictType) => ({
            label: `${item.dictName}（${item.dictType}）`,
            value: item.dictType,
          }));
        setDictTypeOptions(options);
      })
      .catch(() => {
        message.error('字典类型加载失败');
      });
  }, []);

  const openCreateAttribute = () => {
    setCurrentAttribute(undefined);
    setAttributeModalOpen(true);
  };

  const openEditAttribute = (record: API.Product.Attribute) => {
    setCurrentAttribute(record);
    setAttributeModalOpen(true);
  };

  const saveAttribute = async (values: API.Product.Attribute) => {
    const payload = normalizeAttributeValues(values) as API.Product.Attribute;
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
    modal.confirm({
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

  const changeAttributeStatus = (
    record: API.Product.Attribute,
    targetStatus: string,
  ) => {
    const attributeId = record.attributeId;
    if (!attributeId || record.status === targetStatus) {
      return;
    }
    const actionText = targetStatus === '0' ? '启用' : '停用';
    modal.confirm({
      title: `${actionText}商品属性`,
      content:
        targetStatus === '0'
          ? `确认启用 ${record.attributeName}？启用后可继续被类目属性模板选择。`
          : `确认停用 ${record.attributeName}？停用后卖家上传商品时不会再使用该属性。`,
      okText: `确认${actionText}`,
      cancelText: '取消',
      okButtonProps: {
        danger: targetStatus === '1',
      },
      onOk: async () => {
        setStatusUpdatingId(attributeId);
        try {
          const ok = resultOk(
            await updateAttributeStatus(attributeId, targetStatus),
            `属性已${actionText}`,
          );
          if (ok) {
            actionRef.current?.reload();
          }
        } finally {
          setStatusUpdatingId(undefined);
        }
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
      render: (_, record) =>
        access.hasPerms('product:attribute:edit') ? (
          <Switch
            checked={record.status === '0'}
            checkedChildren="启用"
            unCheckedChildren="停用"
            loading={statusUpdatingId === record.attributeId}
            onChange={(checked) =>
              changeAttributeStatus(record, checked ? '0' : '1')
            }
          />
        ) : record.status === '0' ? (
          '正常'
        ) : (
          '停用'
        ),
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
        <Button
          key="delete"
          type="link"
          size="small"
          danger
          hidden={!access.hasPerms('product:attribute:remove')}
          onClick={() => removeAttribute(record)}
        >
          删除
        </Button>,
      ],
    },
  ];

  return (
    <>
      <ProTable<API.Product.Attribute>
        actionRef={actionRef}
        rowKey="attributeId"
        columns={columns}
        scroll={getProTableScroll(1450)}
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
        onValuesChange={(changedValues, allValues) => {
          if ('attributeType' in changedValues) {
            const resetValues: Partial<API.Product.Attribute> = {};
            if (!isOptionAttributeType(changedValues.attributeType)) {
              resetValues.optionSource = 'NONE';
              resetValues.dictType = '';
            } else if (
              allValues.optionSource !== 'ATTRIBUTE_OPTION' &&
              allValues.optionSource !== 'SYS_DICT'
            ) {
              resetValues.optionSource = 'ATTRIBUTE_OPTION';
              resetValues.dictType = '';
            }
            if (!isNumberAttributeType(changedValues.attributeType)) {
              resetValues.unit = '';
              resetValues.valuePrecision = 0;
            }
            if (Object.keys(resetValues).length > 0) {
              attributeForm.setFieldsValue(resetValues);
            }
          }
          if (
            'optionSource' in changedValues &&
            changedValues.optionSource !== 'SYS_DICT'
          ) {
            attributeForm.setFieldsValue({ dictType: '' });
          }
        }}
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
        <ProFormDependency name={['attributeType', 'optionSource']}>
          {({ attributeType, optionSource }) =>
            isOptionAttributeType(attributeType) ? (
              <>
                <ProFormSelect
                  name="optionSource"
                  label="选项来源"
                  options={selectAttributeOptionSourceOptions}
                  fieldProps={SEARCHABLE_SELECT_PROPS}
                  rules={[{ required: true, message: '请选择选项来源' }]}
                />
                {optionSource === 'SYS_DICT' ? (
                  <ProFormSelect
                    name="dictType"
                    label="字典类型"
                    options={dictTypeOptions}
                    fieldProps={SEARCHABLE_SELECT_PROPS}
                    rules={[{ required: true, message: '请选择若依字典类型' }]}
                  />
                ) : null}
              </>
            ) : null
          }
        </ProFormDependency>
        <ProFormDependency name={['attributeType']}>
          {({ attributeType }) =>
            isNumberAttributeType(attributeType) ? (
              <>
                <ProFormText name="unit" label="单位" />
                <ProFormDigit
                  name="valuePrecision"
                  label="小数位数"
                  min={0}
                  max={8}
                />
              </>
            ) : null
          }
        </ProFormDependency>
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
