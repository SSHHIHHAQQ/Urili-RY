import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  PageContainer,
  type ProColumns,
  type ProFormInstance,
  ProFormDigit,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProFormTreeSelect,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Dropdown, Modal } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  addCategory,
  deleteCategory,
  getCategoryList,
  updateCategory,
} from '@/services/product/product';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { buildCategoryTree, toCategoryTreeSelectData } from '../categoryTree';
import { statusOptions, statusValueEnum, yesNoOptions, yesNoValueEnum } from '../constants';

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
  publishEnabled: 'N',
  sortOrder: 0,
  status: '0',
};

export default function ProductCategoryPage() {
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const formRef = useRef<ProFormInstance<API.Product.Category> | undefined>(
    undefined,
  );
  const [flatCategories, setFlatCategories] = useState<API.Product.Category[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [currentCategory, setCurrentCategory] = useState<API.Product.Category>();

  const categoryTree = useMemo(
    () => buildCategoryTree(flatCategories),
    [flatCategories],
  );

  const parentTreeData = useMemo(
    () => [
      {
        title: '顶级分类',
        value: 0,
        children: toCategoryTreeSelectData(categoryTree),
      },
    ],
    [categoryTree],
  );

  useEffect(() => {
    if (!modalOpen) {
      return;
    }
    formRef.current?.resetFields();
    formRef.current?.setFieldsValue(currentCategory || defaultCategoryValues);
  }, [currentCategory, modalOpen]);

  const openCreateModal = (parent?: API.Product.Category) => {
    setCurrentCategory({
      ...defaultCategoryValues,
      parentId: parent?.categoryId || 0,
    });
    setModalOpen(true);
  };

  const openEditModal = (record: API.Product.Category) => {
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
      actionRef.current?.reload();
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
        if (ok) actionRef.current?.reload();
      },
    });
  };

  const columns: ProColumns<API.Product.Category>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '分类名称',
      dataIndex: 'categoryName',
      width: 220,
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
      title: '可发布',
      dataIndex: 'publishEnabled',
      valueType: 'select',
      valueEnum: yesNoValueEnum,
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
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
      render: (_, record) => [
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
          menu={{
            items: [
              {
                key: 'addChild',
                label: '新增下级',
                disabled:
                  !access.hasPerms('product:category:add') ||
                  record.publishEnabled === 'Y',
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
        actionRef={actionRef}
        rowKey="categoryId"
        columns={columns}
        pagination={false}
        search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-category')}
        request={async (params) => {
          const resp = await getCategoryList(params);
          const rows = resp.data || [];
          setFlatCategories(rows);
          return {
            data: buildCategoryTree(rows),
            success: resp.code === 200,
          };
        }}
        toolBarRender={() => [
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

      <ModalForm<API.Product.Category>
        title={currentCategory?.categoryId ? '编辑商品分类' : '新增商品分类'}
        open={modalOpen}
        formRef={formRef}
        modalProps={{ destroyOnClose: true, onCancel: () => setModalOpen(false) }}
        onOpenChange={setModalOpen}
        onFinish={saveCategory}
      >
        <ProFormTreeSelect
          name="parentId"
          label="上级分类"
          disabled={!!currentCategory?.categoryId}
          fieldProps={{
            treeData: parentTreeData,
            treeDefaultExpandAll: true,
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
        <ProFormSelect
          name="publishEnabled"
          label="可发布"
          options={yesNoOptions}
          rules={[{ required: true, message: '请选择是否可发布' }]}
        />
        <ProFormDigit name="sortOrder" label="排序" min={0} />
        <ProFormSelect
          name="status"
          label="状态"
          options={statusOptions}
          rules={[{ required: true, message: '请选择状态' }]}
        />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </PageContainer>
  );
}
