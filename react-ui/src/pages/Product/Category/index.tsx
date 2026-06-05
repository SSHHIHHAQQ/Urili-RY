import {
  DownOutlined,
  HistoryOutlined,
  ImportOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  type ProColumns,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Dropdown, Form, Modal } from 'antd';
import type { Key } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  addCategory,
  deleteCategory,
  downloadCategoryImportTemplate,
  getCategoryChildren,
  getCategoryOptions,
  getCategoryPath,
  importCategoryData,
  previewCategoryImport,
  searchCategories,
  updateCategory,
} from '@/services/product/product';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { getCategoryDisplayPath, toCategoryOption } from '../categoryTree';
import ProductImportModal from '../components/ProductImportModal';
import ProductConfigChangeLogDrawer from '../components/ProductConfigChangeLogDrawer';
import { statusOptions, statusValueEnum } from '../constants';

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

const defaultCategoryValues: Partial<API.Product.Category> = {
  parentId: 0,
  sortOrder: 0,
  status: '0',
};

type CategoryQueryParams = Record<string, any>;

function getPlaceholderCategory(parentId: number): API.Product.Category {
  return {
    categoryId: -parentId,
    parentId,
    categoryName: '加载中...',
    loadingPlaceholder: true,
  };
}

function normalizeCategoryTableRows(
  rows: API.Product.Category[],
  disableExpand = false,
): API.Product.Category[] {
  return rows.map((item) => {
    const hasChildren = Number(item.childrenCount || 0) > 0;
    const children = disableExpand
      ? undefined
      : item.children?.length
      ? normalizeCategoryTableRows(item.children)
      : hasChildren && item.categoryId
        ? [getPlaceholderCategory(item.categoryId)]
        : undefined;
    return {
      ...item,
      children,
    };
  });
}

function mergeTableCategoryChildren(
  rows: API.Product.Category[],
  parentId: number,
  children: API.Product.Category[],
): API.Product.Category[] {
  return rows.map((item) => {
    if (item.categoryId === parentId) {
      return {
        ...item,
        children: normalizeCategoryTableRows(children),
      };
    }
    if (item.children?.length) {
      return {
        ...item,
        children: mergeTableCategoryChildren(item.children, parentId, children),
      };
    }
    return item;
  });
}

function hasCategorySearchParams(params: CategoryQueryParams) {
  return Boolean(
    params.keyword?.trim?.() ||
      params.categoryName?.trim?.() ||
      params.categoryCode?.trim?.(),
  );
}

export default function ProductCategoryPage() {
  const access = useAccess();
  const [form] = Form.useForm<API.Product.Category>();
  const loadedParentIds = useRef<Set<number>>(new Set());
  const [categoryRows, setCategoryRows] = useState<API.Product.Category[]>([]);
  const [categoryQuery, setCategoryQuery] = useState<CategoryQueryParams>({});
  const [categorySearchMode, setCategorySearchMode] = useState(false);
  const [tableLoading, setTableLoading] = useState(false);
  const [expandedRowKeys, setExpandedRowKeys] = useState<Key[]>([]);
  const [parentOptions, setParentOptions] = useState<
    { label: string; value: number }[]
  >([{ label: '顶级分类', value: 0 }]);
  const [modalOpen, setModalOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [currentCategory, setCurrentCategory] = useState<API.Product.Category>();
  const [operationLogOpen, setOperationLogOpen] = useState(false);

  const loadCategoryRows = useCallback(async (params: CategoryQueryParams = {}) => {
    setTableLoading(true);
    try {
      const searchMode = hasCategorySearchParams(params);
      setCategorySearchMode(searchMode);
      if (searchMode) {
        const resp = await searchCategories({
          ...params,
          pageNum: 1,
          pageSize: 200,
        });
        setCategoryRows(normalizeCategoryTableRows(resp.rows || [], true));
        setExpandedRowKeys([]);
        loadedParentIds.current.clear();
        return;
      }
      const resp = await getCategoryChildren({
        parentId: 0,
        status: params.status,
      });
      setCategoryRows(normalizeCategoryTableRows(resp.data || []));
      setExpandedRowKeys([]);
      loadedParentIds.current.clear();
      loadedParentIds.current.add(0);
    } finally {
      setTableLoading(false);
    }
  }, []);

  const loadCategoryChildren = useCallback(
    async (parentId: number) => {
      if (loadedParentIds.current.has(parentId)) {
        return;
      }
      setTableLoading(true);
      try {
        const resp = await getCategoryChildren({
          parentId,
          status: categoryQuery.status,
        });
        setCategoryRows((previous) =>
          mergeTableCategoryChildren(previous, parentId, resp.data || []),
        );
        loadedParentIds.current.add(parentId);
      } finally {
        setTableLoading(false);
      }
    },
    [categoryQuery.status],
  );

  const loadParentOptions = useCallback(async (keyword = '') => {
    const resp = await getCategoryOptions({
      keyword,
      status: '0',
      pageNum: 1,
      pageSize: 50,
    });
    setParentOptions([
      { label: '顶级分类', value: 0 },
      ...(resp.data || [])
        .filter((item) => Boolean(item.categoryId))
        .map(toCategoryOption),
    ]);
  }, []);

  const ensureParentOption = useCallback(async (parentId?: number) => {
    if (!parentId) {
      setParentOptions([{ label: '顶级分类', value: 0 }]);
      return;
    }
    const resp = await getCategoryPath(parentId);
    const pathRows = resp.data || [];
    const parent = pathRows[pathRows.length - 1];
    setParentOptions([
      { label: '顶级分类', value: 0 },
      parent ? toCategoryOption(parent) : { label: String(parentId), value: parentId },
    ]);
  }, []);

  useEffect(() => {
    loadCategoryRows({});
  }, [loadCategoryRows]);

  useEffect(() => {
    if (!modalOpen) {
      return;
    }
    form.resetFields();
    form.setFieldsValue(currentCategory || defaultCategoryValues);
  }, [currentCategory, form, modalOpen]);

  const openCreateModal = (parent?: API.Product.Category) => {
    if (parent?.categoryId) {
      setParentOptions([
        { label: '顶级分类', value: 0 },
        toCategoryOption(parent),
      ]);
    } else {
      loadParentOptions();
    }
    setCurrentCategory({
      ...defaultCategoryValues,
      parentId: parent?.categoryId || 0,
    });
    setModalOpen(true);
  };

  const openEditModal = (record: API.Product.Category) => {
    ensureParentOption(record.parentId);
    setCurrentCategory(record);
    setModalOpen(true);
  };

  const saveCategory = async (values: API.Product.Category) => {
    const payload = {
      ...values,
      parentId: values.parentId || 0,
    };
    const resp = currentCategory?.categoryId
      ? await updateCategory(currentCategory.categoryId, payload)
      : await addCategory(payload);
    if (resultOk(resp, currentCategory?.categoryId ? '分类已更新' : '分类已新增')) {
      loadCategoryRows(categoryQuery);
      return true;
    }
    return false;
  };

  const removeCategory = (record: API.Product.Category) => {
    const categoryId = record.categoryId;
    if (!categoryId) {
      return;
    }
    Modal.confirm({
      title: '删除商品分类',
      content: `确认删除 ${record.categoryName}？存在下级分类或属性配置时后端会拒绝删除。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteCategory(categoryId),
          '分类已删除',
        );
        if (ok) loadCategoryRows(categoryQuery);
      },
    });
  };

  const columns: ProColumns<API.Product.Category>[] = [
    {
      title: '分类名称',
      dataIndex: 'categoryName',
      width: 220,
      render: (_, record) =>
        record.loadingPlaceholder
          ? '加载中...'
          : categorySearchMode
            ? getCategoryDisplayPath(record)
            : record.categoryName,
    },
    {
      title: '分类编码',
      dataIndex: 'categoryCode',
      width: 180,
    },
    {
      title: '层级',
      dataIndex: 'categoryLevel',
      width: 90,
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
      title: '子类目数',
      dataIndex: 'childrenCount',
      width: 100,
      search: false,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 90,
      search: false,
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
      width: 180,
      render: (_, record) =>
        record.loadingPlaceholder
          ? []
          : [
              <Button
                key="edit"
                type="link"
                size="small"
                hidden={!access.hasPerms('product:category:edit')}
                onClick={() => openEditModal(record)}
              >
                编辑
              </Button>,
              <Dropdown
                key="more"
                trigger={['click']}
                menu={{
                  items: [
                    {
                      key: 'addChild',
                      label: '新增下级',
                      disabled: !access.hasPerms('product:category:add'),
                    },
                    {
                      key: 'delete',
                      label: '删除',
                      danger: true,
                      disabled: !access.hasPerms('product:category:remove'),
                    },
                  ],
                  onClick: ({ key }) => {
                    if (key === 'addChild') openCreateModal(record);
                    if (key === 'delete') removeCategory(record);
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
    <PageContainer title={false}>
      <ProTable<API.Product.Category>
        rowKey="categoryId"
        columns={columns}
        dataSource={categoryRows}
        loading={tableLoading}
        pagination={false}
        scroll={getProTableScroll(1200)}
        options={{
          reload: () => loadCategoryRows(categoryQuery),
        }}
        onSubmit={(params) => {
          setCategoryQuery(params);
          loadCategoryRows(params);
        }}
        onReset={() => {
          setCategoryQuery({});
          loadCategoryRows({});
        }}
        search={getPersistedProTableSearch(
          {
            labelWidth: 90,
          },
          'product-category',
        )}
        expandable={{
          expandedRowKeys,
          rowExpandable: (record) =>
            !categorySearchMode &&
            !record.loadingPlaceholder &&
            Number(record.childrenCount || 0) > 0,
          onExpand: async (expanded, record) => {
            if (categorySearchMode || !record.categoryId || record.loadingPlaceholder) {
              return;
            }
            if (expanded) {
              await loadCategoryChildren(record.categoryId);
              setExpandedRowKeys((keys) =>
                keys.includes(record.categoryId as Key)
                  ? keys
                  : [...keys, record.categoryId as Key],
              );
              return;
            }
            setExpandedRowKeys((keys) =>
              keys.filter((key) => key !== record.categoryId),
            );
          },
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
            key="import"
            icon={<ImportOutlined />}
            hidden={!access.hasPerms('product:category:add')}
            onClick={() => setImportOpen(true)}
          >
            导入
          </Button>,
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            hidden={!access.hasPerms('product:category:add')}
            onClick={() => openCreateModal()}
          >
            新增
          </Button>,
        ]}
      />

      <ProductImportModal
        title="导入商品分类"
        open={importOpen}
        onOpenChange={setImportOpen}
        onDownloadTemplate={downloadCategoryImportTemplate}
        onPreview={previewCategoryImport}
        onImport={importCategoryData}
        onSuccess={() => loadCategoryRows(categoryQuery)}
      />

      <ProductConfigChangeLogDrawer
        open={operationLogOpen}
        onOpenChange={setOperationLogOpen}
        bizType="CATEGORY"
        title="商品分类配置"
      />

      <ModalForm<API.Product.Category>
        title={currentCategory?.categoryId ? '编辑商品分类' : '新增商品分类'}
        open={modalOpen}
        form={form}
        modalProps={{ destroyOnHidden: true, onCancel: () => setModalOpen(false) }}
        onOpenChange={setModalOpen}
        onFinish={saveCategory}
      >
        <ProFormSelect
          name="parentId"
          label="上级分类"
          disabled={!!currentCategory?.categoryId}
          fieldProps={{
            ...SEARCHABLE_SELECT_PROPS,
            filterOption: false,
            onSearch: loadParentOptions,
            options: parentOptions,
          }}
          rules={[{ required: true, message: '请选择上级分类' }]}
        />
        <ProFormText
          name="categoryCode"
          label="分类编码"
          rules={[{ required: true, message: '请输入分类编码' }]}
        />
        <ProFormText
          name="categoryName"
          label="分类名称"
          rules={[{ required: true, message: '请输入分类名称' }]}
        />
        <ProFormDigit name="sortOrder" label="排序" min={0} />
        <ProFormSelect
          name="status"
          label="状态"
          options={statusOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择状态' }]}
        />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </PageContainer>
  );
}
