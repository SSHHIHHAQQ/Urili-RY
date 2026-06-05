import { HistoryOutlined, PlusOutlined } from '@ant-design/icons';
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
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  deleteCategoryAttribute,
  getCategoryAttributeList,
  getCategoryChildren,
  getCategorySchema,
  getEnabledAttributeList,
  searchCategories,
  saveCategoryAttribute,
} from '@/services/product/product';
import { message } from '@/utils/feedback';
import { getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  mergeCategoryChildren,
  normalizeLazyCategoryRows,
  toLazyCategoryTreeData,
} from '../../categoryTree';
import {
  attributeGroupOptions,
  attributeTypeOptions,
  optionArrayToValueEnum,
  ruleModeOptions,
  statusOptions,
  yesNoOptions,
} from '../../constants';
import ProductConfigChangeLogDrawer from '../../components/ProductConfigChangeLogDrawer';
import './CategoryAttributeTemplate.css';
import CategoryTreeFilterPanel from './CategoryTreeFilterPanel';
import { buildCategoryAttributeColumns } from './categoryAttributeColumns';

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

const CATEGORY_SEARCH_PAGE_SIZE = 200;

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
  const loadedCategoryParentIds = useRef<Set<number>>(new Set());
  const categorySearchPageRef = useRef(1);
  const categorySearchLoadedRef = useRef(0);
  const categorySearchLoadingMoreRef = useRef(false);
  const [form] = Form.useForm<API.Product.CategoryAttribute>();
  const [categoryRows, setCategoryRows] = useState<API.Product.Category[]>([]);
  const [categoryTreeLoading, setCategoryTreeLoading] = useState(false);
  const [categorySearchLoadingMore, setCategorySearchLoadingMore] =
    useState(false);
  const [categorySearchMode, setCategorySearchMode] = useState(false);
  const [categorySearchLoaded, setCategorySearchLoaded] = useState(0);
  const [categorySearchTotal, setCategorySearchTotal] = useState(0);
  const [categorySearchHasMore, setCategorySearchHasMore] = useState(false);
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
  const [operationLogOpen, setOperationLogOpen] = useState(false);

  const treeData = useMemo(
    () =>
      toLazyCategoryTreeData(
        categoryRows,
        categorySearchMode,
        categorySearchMode,
      ),
    [categoryRows, categorySearchMode],
  );
  const categoryLevelOptions = useMemo(() => {
    return [
      { label: '全部层级', value: 'ALL' },
      ...Array.from({ length: 12 }, (_, index) => index + 1).map((level) => ({
        label: `${level}级类目`,
        value: String(level),
      })),
    ];
  }, []);
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

  const categoryEffectiveStatusParam =
    categoryStatus === 'ALL' ? undefined : categoryStatus;

  const loadAttributeOptions = useCallback(async (keyword = '') => {
    const resp = await getEnabledAttributeList({
      keyword,
      pageNum: 1,
      pageSize: 50,
    });
    setAttributes(resp.data || []);
  }, []);

  const loadCategoryChildren = useCallback(
    async (parentId = 0, force = false) => {
      if (!force && loadedCategoryParentIds.current.has(parentId)) {
        return;
      }
      setCategoryTreeLoading(true);
      try {
        const resp = await getCategoryChildren({
          parentId,
          effectiveStatus: categoryEffectiveStatusParam,
        });
        const rows = normalizeLazyCategoryRows(resp.data || []);
        loadedCategoryParentIds.current.add(parentId);
        setCategoryRows((previous) =>
          parentId === 0 ? rows : mergeCategoryChildren(previous, parentId, rows),
        );
        if (parentId === 0) {
          setSelectedCategoryId((current) =>
            force ? rows[0]?.categoryId : current || rows[0]?.categoryId,
          );
        }
      } finally {
        setCategoryTreeLoading(false);
      }
    },
    [categoryEffectiveStatusParam],
  );

  const searchCategoryRows = useCallback(
    async (pageNum = 1, append = false) => {
      if (append && categorySearchLoadingMoreRef.current) {
        return;
      }
      if (append) {
        categorySearchLoadingMoreRef.current = true;
        setCategorySearchLoadingMore(true);
      } else {
        categorySearchPageRef.current = 1;
        categorySearchLoadedRef.current = 0;
        setCategorySearchLoaded(0);
        setCategorySearchTotal(0);
        setCategorySearchHasMore(false);
        setCategoryTreeLoading(true);
      }
      try {
        const resp = await searchCategories({
          keyword: categoryKeyword || undefined,
          effectiveStatus: categoryEffectiveStatusParam,
          categoryLevel:
            categoryLevel === 'ALL' ? undefined : Number(categoryLevel),
          leafOnly: leafOnly || undefined,
          pageNum,
          pageSize: CATEGORY_SEARCH_PAGE_SIZE,
        });
        const rows = normalizeLazyCategoryRows(resp.rows || []);
        const total = Number(resp.total || 0);
        const nextLoaded = append
          ? categorySearchLoadedRef.current + rows.length
          : rows.length;

        categorySearchPageRef.current = pageNum;
        categorySearchLoadedRef.current = nextLoaded;
        setCategoryRows((previous) => (append ? [...previous, ...rows] : rows));
        setCategorySearchLoaded(nextLoaded);
        setCategorySearchTotal(total);
        setCategorySearchHasMore(nextLoaded < total);
        if (!append) {
          setSelectedCategoryId((current) =>
            current && rows.some((item) => item.categoryId === current)
              ? current
              : rows[0]?.categoryId,
          );
          setExpandedCategoryKeys([]);
          setAutoExpandParent(false);
        }
      } finally {
        if (append) {
          categorySearchLoadingMoreRef.current = false;
          setCategorySearchLoadingMore(false);
        } else {
          setCategoryTreeLoading(false);
        }
      }
    },
    [
      categoryKeyword,
      categoryLevel,
      categoryEffectiveStatusParam,
      leafOnly,
    ],
  );

  const loadMoreCategorySearchRows = useCallback(() => {
    if (
      !categorySearchMode ||
      categoryTreeLoading ||
      categorySearchLoadingMoreRef.current ||
      categorySearchLoadingMore ||
      !categorySearchHasMore
    ) {
      return;
    }
    searchCategoryRows(categorySearchPageRef.current + 1, true);
  }, [
    categorySearchHasMore,
    categorySearchLoadingMore,
    categorySearchMode,
    categoryTreeLoading,
    searchCategoryRows,
  ]);

  useEffect(() => {
    loadAttributeOptions();
  }, [loadAttributeOptions]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadedCategoryParentIds.current.clear();
      setExpandedCategoryKeys([]);
      setAutoExpandParent(false);
      if (
        categoryKeyword.trim() ||
        categoryLevel !== 'ALL' ||
        leafOnly ||
        categoryStatus === '1'
      ) {
        setCategorySearchMode(true);
        searchCategoryRows();
        return;
      }
      setCategorySearchMode(false);
      categorySearchPageRef.current = 1;
      categorySearchLoadedRef.current = 0;
      categorySearchLoadingMoreRef.current = false;
      setCategorySearchLoadingMore(false);
      setCategorySearchLoaded(0);
      setCategorySearchTotal(0);
      setCategorySearchHasMore(false);
      loadCategoryChildren(0, true);
    }, 250);
    return () => window.clearTimeout(timer);
  }, [
    categoryKeyword,
    categoryStatus,
    categoryLevel,
    leafOnly,
    loadCategoryChildren,
    searchCategoryRows,
  ]);

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
    if (record.attributeId) {
      setAttributes((previous) =>
        previous.some((item) => item.attributeId === record.attributeId)
          ? previous
          : [
              {
                attributeId: record.attributeId,
                attributeCode: record.attributeCode,
                attributeName: record.attributeName,
              },
              ...previous,
            ],
      );
    }
    setCurrentRule(record);
    setModalOpen(true);
  };

  const saveRule = async (values: API.Product.CategoryAttribute) => {
    const payload = {
      ...values,
      categoryId: selectedCategoryId,
    };
    const ok = resultOk(
      await saveCategoryAttribute(payload),
      '类目属性规则已保存',
    );
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
        onEditRule: openEditRule,
        onRemoveRule: removeRule,
      }),
    [access, attributeGroupValueEnum, attributeTypeValueEnum],
  );

  return (
    <>
      <div className="product-category-attribute-template">
        <div className="product-category-attribute-template__category-pane">
          <CategoryTreeFilterPanel
            treeData={treeData}
            selectedCategoryId={selectedCategoryId}
            expandedCategoryKeys={expandedCategoryKeys}
            autoExpandParent={autoExpandParent}
            loading={categoryTreeLoading}
            loadingMore={categorySearchLoadingMore}
            searchMode={categorySearchMode}
            searchResultLoaded={categorySearchLoaded}
            searchResultTotal={categorySearchTotal}
            searchHasMore={categorySearchHasMore}
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
            onLoadCategoryChildren={loadCategoryChildren}
            onLoadMoreSearchResults={loadMoreCategorySearchRows}
            onSelectCategory={setSelectedCategoryId}
          />
        </div>
        <div className="product-category-attribute-template__rules-pane">
          <div className="product-category-attribute-template__table-frame product-category-attribute-template__table-frame--direct">
            <ProTable<API.Product.CategoryAttribute>
              className="product-category-attribute-template__rule-table"
              actionRef={directActionRef}
              rowKey="categoryAttributeId"
              headerTitle="本类目规则"
              columns={directColumns}
              search={false}
              pagination={false}
              size="small"
              scroll={getProTableScroll(1250)}
              request={async () => {
                if (!selectedCategoryId) {
                  return { data: [], success: true };
                }
                const resp = await getCategoryAttributeList(selectedCategoryId);
                return {
                  data: resp.data || [],
                  success: resp.code === 200,
                };
              }}
              toolBarRender={() => [
                <Button
                  key="operationLog"
                  icon={<HistoryOutlined />}
                  onClick={() => setOperationLogOpen(true)}
                >
                  操作日志
                </Button>,
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
          </div>
          <div className="product-category-attribute-template__table-frame product-category-attribute-template__table-frame--schema">
            <ProTable<API.Product.CategoryAttribute>
              className="product-category-attribute-template__rule-table"
              actionRef={schemaActionRef}
              rowKey={(record) =>
                `${record.categoryId}-${record.attributeId}-${record.ruleMode}`
              }
              headerTitle="继承预览"
              columns={schemaColumns}
              search={false}
              pagination={false}
              size="small"
              scroll={getProTableScroll(1400)}
              request={async () => {
                if (!selectedCategoryId) {
                  return { data: [], success: true };
                }
                const resp = await getCategorySchema(selectedCategoryId);
                return {
                  data: resp.data || [],
                  success: resp.code === 200,
                };
              }}
            />
          </div>
        </div>
      </div>

      <ProductConfigChangeLogDrawer
        open={operationLogOpen}
        onOpenChange={setOperationLogOpen}
        bizType="CATEGORY_ATTRIBUTE_RULE"
        title="类目属性模板"
      />

      <ModalForm<API.Product.CategoryAttribute>
        title={
          currentRule?.categoryAttributeId ? '编辑本类目规则' : '新增本类目规则'
        }
        open={modalOpen}
        form={form}
        modalProps={{
          destroyOnHidden: true,
          onCancel: () => setModalOpen(false),
        }}
        onOpenChange={setModalOpen}
        onFinish={saveRule}
      >
        <ProFormSelect
          name="attributeId"
          label="商品属性"
          options={attributeOptions}
          disabled={!!currentRule?.categoryAttributeId}
          fieldProps={{
            ...SEARCHABLE_SELECT_PROPS,
            filterOption: false,
            onSearch: loadAttributeOptions,
          }}
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
        <ProFormSelect
          name="status"
          label="状态"
          options={statusOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </>
  );
}
