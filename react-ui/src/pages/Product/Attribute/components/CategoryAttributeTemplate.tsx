import { PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  type ProColumns,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Empty, Form, Modal, Tree } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  deleteCategoryAttribute,
  getCategoryAttributeList,
  getCategoryList,
  getCategorySchema,
  getEnabledAttributeList,
  saveCategoryAttribute,
} from '@/services/product/product';
import { message } from '@/utils/feedback';
import { buildCategoryTree, toCategoryTreeData } from '../../categoryTree';
import {
  attributeGroupOptions,
  attributeTypeOptions,
  optionArrayToValueEnum,
  ruleModeOptions,
  ruleModeValueEnum,
  statusOptions,
  statusValueEnum,
  yesNoOptions,
  yesNoValueEnum,
} from '../../constants';

type AccessLike = {
  hasPerms: (permission: string) => boolean;
};

type CategoryAttributeTemplateProps = {
  access: AccessLike;
};

const defaultRuleValues: Partial<API.Product.CategoryAttribute> = {
  ruleMode: 'ADD',
  requiredFlag: 'N',
  visibleFlag: 'Y',
  editableFlag: 'Y',
  filterableFlag: 'N',
  groupCode: 'BASIC',
  sortOrder: 0,
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

export default function CategoryAttributeTemplate({
  access,
}: CategoryAttributeTemplateProps) {
  const directActionRef = useRef<ActionType>(null);
  const schemaActionRef = useRef<ActionType>(null);
  const [form] = Form.useForm<API.Product.CategoryAttribute>();
  const [categories, setCategories] = useState<API.Product.Category[]>([]);
  const [attributes, setAttributes] = useState<API.Product.Attribute[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number>();
  const [modalOpen, setModalOpen] = useState(false);
  const [currentRule, setCurrentRule] =
    useState<API.Product.CategoryAttribute>();

  const categoryTree = useMemo(() => buildCategoryTree(categories), [categories]);
  const treeData = useMemo(() => toCategoryTreeData(categoryTree), [categoryTree]);
  const attributeTypeValueEnum = useMemo(
    () => optionArrayToValueEnum(attributeTypeOptions),
    [],
  );

  const attributeOptions = useMemo(
    () =>
      attributes.map((item) => ({
        label: `${item.attributeName} (${item.attributeCode})`,
        value: item.attributeId,
      })),
    [attributes],
  );

  const loadBaseData = async () => {
    const [categoryResp, attributeResp] = await Promise.all([
      getCategoryList({ status: '0' }),
      getEnabledAttributeList(),
    ]);
    const categoryRows = categoryResp.data || [];
    setCategories(categoryRows);
    setAttributes(attributeResp.data || []);
    if (!selectedCategoryId && categoryRows[0]?.categoryId) {
      setSelectedCategoryId(categoryRows[0].categoryId);
    }
  };

  useEffect(() => {
    loadBaseData();
  }, []);

  useEffect(() => {
    directActionRef.current?.reload();
    schemaActionRef.current?.reload();
  }, [selectedCategoryId]);

  useEffect(() => {
    if (!modalOpen) {
      return;
    }
    form.resetFields();
    form.setFieldsValue({
      ...defaultRuleValues,
      categoryId: selectedCategoryId,
      ...currentRule,
    });
  }, [currentRule, form, modalOpen, selectedCategoryId]);

  const openCreateRule = () => {
    if (!selectedCategoryId) {
      message.warning('请先选择类目');
      return;
    }
    setCurrentRule(undefined);
    setModalOpen(true);
  };

  const openEditRule = (record: API.Product.CategoryAttribute) => {
    setCurrentRule(record);
    setModalOpen(true);
  };

  const saveRule = async (values: API.Product.CategoryAttribute) => {
    const payload = {
      ...values,
      categoryId: selectedCategoryId,
    };
    const ok = resultOk(await saveCategoryAttribute(payload), '类目属性规则已保存');
    if (ok) {
      directActionRef.current?.reload();
      schemaActionRef.current?.reload();
      return true;
    }
    return false;
  };

  const removeRule = (record: API.Product.CategoryAttribute) => {
    const categoryAttributeId = record.categoryAttributeId;
    if (!categoryAttributeId) {
      return;
    }
    Modal.confirm({
      title: '移除类目属性规则',
      content: `确认移除 ${record.attributeName} 的本类目规则？继承预览会重新按父级规则计算。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteCategoryAttribute(categoryAttributeId),
          '类目属性规则已移除',
        );
        if (ok) {
          directActionRef.current?.reload();
          schemaActionRef.current?.reload();
        }
      },
    });
  };

  const baseColumns: ProColumns<API.Product.CategoryAttribute>[] = [
    {
      title: '属性名称',
      dataIndex: 'attributeName',
      width: 150,
    },
    {
      title: '属性编码',
      dataIndex: 'attributeCode',
      width: 150,
    },
    {
      title: '类型',
      dataIndex: 'attributeType',
      valueEnum: attributeTypeValueEnum,
      width: 110,
    },
    {
      title: '规则',
      dataIndex: 'ruleMode',
      valueEnum: ruleModeValueEnum,
      width: 110,
    },
    {
      title: '必填',
      dataIndex: 'requiredFlag',
      valueEnum: yesNoValueEnum,
      width: 80,
    },
    {
      title: '展示',
      dataIndex: 'visibleFlag',
      valueEnum: yesNoValueEnum,
      width: 80,
    },
    {
      title: '可编辑',
      dataIndex: 'editableFlag',
      valueEnum: yesNoValueEnum,
      width: 90,
    },
    {
      title: '可筛选',
      dataIndex: 'filterableFlag',
      valueEnum: yesNoValueEnum,
      width: 90,
    },
    {
      title: '分组',
      dataIndex: 'groupCode',
      width: 110,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: statusValueEnum,
      width: 90,
    },
  ];

  const directColumns: ProColumns<API.Product.CategoryAttribute>[] = [
    ...baseColumns,
    {
      title: '操作',
      valueType: 'option',
      width: 130,
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          size="small"
          hidden={!access.hasPerms('product:categoryAttribute:edit')}
          onClick={() => openEditRule(record)}
        >
          编辑
        </Button>,
        <Button
          key="delete"
          type="link"
          size="small"
          danger
          hidden={!access.hasPerms('product:categoryAttribute:edit')}
          onClick={() => removeRule(record)}
        >
          移除
        </Button>,
      ],
    },
  ];

  const schemaColumns: ProColumns<API.Product.CategoryAttribute>[] = [
    {
      title: '来源类目',
      dataIndex: 'sourceCategoryName',
      width: 150,
    },
    ...baseColumns,
  ];

  if (!categoryTree.length) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '280px minmax(0, 1fr)',
          gap: 16,
          alignItems: 'stretch',
        }}
      >
        <div
          style={{
            minHeight: 640,
            padding: 16,
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            background: '#fff',
          }}
        >
          <Tree
            treeData={treeData}
            selectedKeys={selectedCategoryId ? [selectedCategoryId] : []}
            defaultExpandAll
            onSelect={(keys) => {
              const key = keys[0];
              if (key) setSelectedCategoryId(Number(key));
            }}
          />
        </div>
        <div style={{ minWidth: 0 }}>
          <ProTable<API.Product.CategoryAttribute>
            actionRef={directActionRef}
            rowKey="categoryAttributeId"
            headerTitle="本类目规则"
            columns={directColumns}
            search={false}
            pagination={false}
            request={async () => {
              if (!selectedCategoryId) {
                return { data: [], success: true };
              }
              const resp = await getCategoryAttributeList(selectedCategoryId);
              return { data: resp.data || [], success: resp.code === 200 };
            }}
            toolBarRender={() => [
              <Button
                key="add"
                type="primary"
                icon={<PlusOutlined />}
                hidden={!access.hasPerms('product:categoryAttribute:edit')}
                onClick={openCreateRule}
              >
                新增
              </Button>,
            ]}
          />
          <div style={{ height: 16 }} />
          <ProTable<API.Product.CategoryAttribute>
            actionRef={schemaActionRef}
            rowKey={(record) =>
              `${record.categoryId}-${record.attributeId}-${record.ruleMode}`
            }
            headerTitle="继承预览"
            columns={schemaColumns}
            search={false}
            pagination={false}
            request={async () => {
              if (!selectedCategoryId) {
                return { data: [], success: true };
              }
              const resp = await getCategorySchema(selectedCategoryId);
              return { data: resp.data || [], success: resp.code === 200 };
            }}
          />
        </div>
      </div>

      <ModalForm<API.Product.CategoryAttribute>
        title={currentRule?.categoryAttributeId ? '编辑本类目规则' : '新增本类目规则'}
        open={modalOpen}
        form={form}
        modalProps={{ destroyOnHidden: true, onCancel: () => setModalOpen(false) }}
        onOpenChange={setModalOpen}
        onFinish={saveRule}
      >
        <ProFormSelect
          name="attributeId"
          label="商品属性"
          options={attributeOptions}
          disabled={!!currentRule?.categoryAttributeId}
          rules={[{ required: true, message: '请选择商品属性' }]}
        />
        <ProFormSelect
          name="ruleMode"
          label="规则模式"
          options={ruleModeOptions}
          rules={[{ required: true, message: '请选择规则模式' }]}
        />
        <ProFormSelect
          name="requiredFlag"
          label="必填"
          options={yesNoOptions}
        />
        <ProFormSelect
          name="visibleFlag"
          label="展示"
          options={yesNoOptions}
        />
        <ProFormSelect
          name="editableFlag"
          label="可编辑"
          options={yesNoOptions}
        />
        <ProFormSelect
          name="filterableFlag"
          label="可筛选"
          options={yesNoOptions}
        />
        <ProFormSelect
          name="groupCode"
          label="属性分组"
          options={attributeGroupOptions}
        />
        <ProFormDigit name="sortOrder" label="排序" min={0} />
        <ProFormText name="placeholder" label="占位提示" />
        <ProFormText name="helpText" label="帮助文案" />
        <ProFormTextArea name="validationRule" label="校验规则 JSON" />
        <ProFormSelect name="status" label="状态" options={statusOptions} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </>
  );
}
