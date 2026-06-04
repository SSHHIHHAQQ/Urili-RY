import { PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Form, Modal } from 'antd';
import type { Key } from 'react';
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
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { buildCategoryTree, toCategoryTreeData } from '../../categoryTree';
import {
  attributeGroupOptions,
  attributeTypeOptions,
  optionArrayToValueEnum,
  ruleModeOptions,
  statusOptions,
  yesNoOptions,
} from '../../constants';
import CategoryTreeFilterPanel from './CategoryTreeFilterPanel';
import { buildCategoryAttributeColumns } from './categoryAttributeColumns';
import {
  collectCategoryKeys,
  collectCategoryOptions,
  filterCategoryAttributeRows,
  filterCategoryTree,
  findCategoryInTree,
} from './categoryAttributeFilterUtils';

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
  const [categoryKeyword, setCategoryKeyword] = useState('');
  const [categoryStatus, setCategoryStatus] = useState('0');
  const [categoryLevel, setCategoryLevel] = useState('ALL');
  const [leafOnly, setLeafOnly] = useState(false);
  const [expandedCategoryKeys, setExpandedCategoryKeys] = useState<Key[]>([]);
  const [autoExpandParent, setAutoExpandParent] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [currentRule, setCurrentRule] =
    useState<API.Product.CategoryAttribute>();

  const categoryTree = useMemo(() => buildCategoryTree(categories), [categories]);
  const filteredCategoryTree = useMemo(
    () =>
      filterCategoryTree(categoryTree, {
        keyword: categoryKeyword,
        status: categoryStatus,
        level: categoryLevel,
        leafOnly,
      }),
    [categoryKeyword, categoryLevel, categoryStatus, categoryTree, leafOnly],
  );
  const treeData = useMemo(
    () => toCategoryTreeData(filteredCategoryTree),
    [filteredCategoryTree],
  );
  const visibleCategoryKeys = useMemo(
    () => collectCategoryKeys(filteredCategoryTree),
    [filteredCategoryTree],
  );
  const sourceCategoryOptions = useMemo(
    () => collectCategoryOptions(categoryTree),
    [categoryTree],
  );
  const categoryLevelOptions = useMemo(() => {
    const levels = Array.from(
      new Set(categories.map((item) => item.categoryLevel).filter(Boolean)),
    ).sort((a, b) => Number(a) - Number(b));
    return [
      { label: '全部层级', value: 'ALL' },
      ...levels.map((level) => ({ label: `${level}级类目`, value: String(level) })),
    ];
  }, [categories]);
  const attributeTypeValueEnum = useMemo(
    () => optionArrayToValueEnum(attributeTypeOptions),
    [],
  );
  const attributeGroupValueEnum = useMemo(
    () => optionArrayToValueEnum(attributeGroupOptions),
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
      getCategoryList(),
      getEnabledAttributeList(),
    ]);
    const categoryRows = categoryResp.data || [];
    setCategories(categoryRows);
    setAttributes(attributeResp.data || []);
    const firstNormalCategory = categoryRows.find((item) => item.status === '0');
    const firstCategoryId = firstNormalCategory?.categoryId || categoryRows[0]?.categoryId;
    if (!selectedCategoryId && firstCategoryId) {
      setSelectedCategoryId(firstCategoryId);
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
    setExpandedCategoryKeys(visibleCategoryKeys);
    setAutoExpandParent(true);
  }, [categoryKeyword, categoryLevel, categoryStatus, leafOnly, visibleCategoryKeys]);

  useEffect(() => {
    if (selectedCategoryId && !findCategoryInTree(filteredCategoryTree, selectedCategoryId)) {
      setSelectedCategoryId(undefined);
    }
  }, [filteredCategoryTree, selectedCategoryId]);

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

  const { directColumns, schemaColumns } = useMemo(
    () =>
      buildCategoryAttributeColumns({
        access,
        attributeTypeValueEnum,
        attributeGroupValueEnum,
        sourceCategoryOptions,
        onEditRule: openEditRule,
        onRemoveRule: removeRule,
      }),
    [
      access,
      attributeGroupValueEnum,
      attributeTypeValueEnum,
      sourceCategoryOptions,
    ],
  );

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
            height: 'calc(100vh - 260px)',
            minHeight: 520,
            padding: 16,
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            background: '#fff',
            overflow: 'hidden',
          }}
        >
          <CategoryTreeFilterPanel
            treeData={treeData}
            selectedCategoryId={selectedCategoryId}
            expandedCategoryKeys={expandedCategoryKeys}
            autoExpandParent={autoExpandParent}
            visibleCategoryKeys={visibleCategoryKeys}
            categoryKeyword={categoryKeyword}
            categoryStatus={categoryStatus}
            categoryLevel={categoryLevel}
            categoryLevelOptions={categoryLevelOptions}
            leafOnly={leafOnly}
            onCategoryKeywordChange={setCategoryKeyword}
            onCategoryStatusChange={setCategoryStatus}
            onCategoryLevelChange={setCategoryLevel}
            onLeafOnlyChange={setLeafOnly}
            onExpandedCategoryKeysChange={(keys, nextAutoExpandParent) => {
              setExpandedCategoryKeys(keys);
              setAutoExpandParent(nextAutoExpandParent);
            }}
            onSelectCategory={setSelectedCategoryId}
          />
        </div>
        <div style={{ minWidth: 0 }}>
          <ProTable<API.Product.CategoryAttribute>
            actionRef={directActionRef}
            rowKey="categoryAttributeId"
            headerTitle="本类目规则"
            columns={directColumns}
            search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-category-attribute-direct')}
            pagination={false}
            request={async (params) => {
              if (!selectedCategoryId) {
                return { data: [], success: true };
              }
              const resp = await getCategoryAttributeList(selectedCategoryId);
              return {
                data: filterCategoryAttributeRows(resp.data || [], params, selectedCategoryId),
                success: resp.code === 200,
              };
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
            search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-category-attribute-schema')}
            pagination={false}
            request={async (params) => {
              if (!selectedCategoryId) {
                return { data: [], success: true };
              }
              const resp = await getCategorySchema(selectedCategoryId);
              return {
                data: filterCategoryAttributeRows(resp.data || [], params, selectedCategoryId),
                success: resp.code === 200,
              };
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
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择商品属性' }]}
        />
        <ProFormSelect
          name="ruleMode"
          label="规则模式"
          options={ruleModeOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择规则模式' }]}
        />
        <ProFormSelect
          name="requiredFlag"
          label="必填"
          options={yesNoOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormSelect
          name="visibleFlag"
          label="展示"
          options={yesNoOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormSelect
          name="editableFlag"
          label="可编辑"
          options={yesNoOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormSelect
          name="filterableFlag"
          label="可筛选"
          options={yesNoOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormSelect
          name="groupCode"
          label="属性分组"
          options={attributeGroupOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormDigit name="sortOrder" label="排序" min={0} />
        <ProFormText name="placeholder" label="占位提示" />
        <ProFormText name="helpText" label="帮助文案" />
        <ProFormTextArea name="validationRule" label="校验规则 JSON" />
        <ProFormSelect name="status" label="状态" options={statusOptions} fieldProps={SEARCHABLE_SELECT_PROPS} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </>
  );
}
