import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownOutlined, HistoryOutlined, ImportOutlined, PlusOutlined, } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormSelect, ProFormText, ProFormTextArea, ProTable, } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Dropdown, Form, Modal } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { addCategory, deleteCategory, downloadCategoryImportTemplate, getCategoryChildren, getCategoryOptions, getCategoryPath, importCategoryData, previewCategoryImport, searchCategories, updateCategory, } from '@/services/product/product';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { getCategoryDisplayPath, toCategoryOption } from '../categoryTree';
import ProductImportModal from '../components/ProductImportModal';
import ProductConfigChangeLogDrawer from '../components/ProductConfigChangeLogDrawer';
import { statusOptions, statusValueEnum } from '../constants';
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
const defaultCategoryValues = {
    parentId: 0,
    sortOrder: 0,
    status: '0',
};
function getPlaceholderCategory(parentId) {
    return {
        categoryId: -parentId,
        parentId,
        categoryName: '加载中...',
        loadingPlaceholder: true,
    };
}
function normalizeCategoryTableRows(rows, disableExpand = false) {
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
function mergeTableCategoryChildren(rows, parentId, children) {
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
function hasCategorySearchParams(params) {
    return Boolean(params.keyword?.trim?.() ||
        params.categoryName?.trim?.() ||
        params.categoryCode?.trim?.());
}
export default function ProductCategoryPage() {
    const access = useAccess();
    const [form] = Form.useForm();
    const loadedParentIds = useRef(new Set());
    const [categoryRows, setCategoryRows] = useState([]);
    const [categoryQuery, setCategoryQuery] = useState({});
    const [categorySearchMode, setCategorySearchMode] = useState(false);
    const [tableLoading, setTableLoading] = useState(false);
    const [expandedRowKeys, setExpandedRowKeys] = useState([]);
    const [parentOptions, setParentOptions] = useState([{ label: '顶级分类', value: 0 }]);
    const [modalOpen, setModalOpen] = useState(false);
    const [importOpen, setImportOpen] = useState(false);
    const [currentCategory, setCurrentCategory] = useState();
    const [operationLogOpen, setOperationLogOpen] = useState(false);
    const loadCategoryRows = useCallback(async (params = {}) => {
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
        }
        finally {
            setTableLoading(false);
        }
    }, []);
    const loadCategoryChildren = useCallback(async (parentId) => {
        if (loadedParentIds.current.has(parentId)) {
            return;
        }
        setTableLoading(true);
        try {
            const resp = await getCategoryChildren({
                parentId,
                status: categoryQuery.status,
            });
            setCategoryRows((previous) => mergeTableCategoryChildren(previous, parentId, resp.data || []));
            loadedParentIds.current.add(parentId);
        }
        finally {
            setTableLoading(false);
        }
    }, [categoryQuery.status]);
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
    const ensureParentOption = useCallback(async (parentId) => {
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
    const openCreateModal = (parent) => {
        if (parent?.categoryId) {
            setParentOptions([
                { label: '顶级分类', value: 0 },
                toCategoryOption(parent),
            ]);
        }
        else {
            loadParentOptions();
        }
        setCurrentCategory({
            ...defaultCategoryValues,
            parentId: parent?.categoryId || 0,
        });
        setModalOpen(true);
    };
    const openEditModal = (record) => {
        ensureParentOption(record.parentId);
        setCurrentCategory(record);
        setModalOpen(true);
    };
    const saveCategory = async (values) => {
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
    const removeCategory = (record) => {
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
                const ok = resultOk(await deleteCategory(categoryId), '分类已删除');
                if (ok)
                    loadCategoryRows(categoryQuery);
            },
        });
    };
    const columns = [
        {
            title: '分类名称',
            dataIndex: 'categoryName',
            width: 220,
            render: (_, record) => record.loadingPlaceholder
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
            render: (_, record) => record.loadingPlaceholder
                ? []
                : [
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:category:edit'), onClick: () => openEditModal(record), children: "\u7F16\u8F91" }, "edit"),
                    _jsx(Dropdown, { trigger: ['click'], menu: {
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
                                if (key === 'addChild')
                                    openCreateModal(record);
                                if (key === 'delete')
                                    removeCategory(record);
                            },
                        }, children: _jsxs(Button, { type: "link", size: "small", children: ["\u66F4\u591A ", _jsx(DownOutlined, {})] }) }, "more"),
                ],
        },
    ];
    return (_jsxs(PageContainer, { title: false, children: [_jsx(ProTable, { rowKey: "categoryId", columns: columns, dataSource: categoryRows, loading: tableLoading, pagination: false, scroll: getProTableScroll(1200), options: {
                    reload: () => loadCategoryRows(categoryQuery),
                }, onSubmit: (params) => {
                    setCategoryQuery(params);
                    loadCategoryRows(params);
                }, onReset: () => {
                    setCategoryQuery({});
                    loadCategoryRows({});
                }, search: getPersistedProTableSearch({
                    labelWidth: 90,
                }, 'product-category'), expandable: {
                    expandedRowKeys,
                    rowExpandable: (record) => !categorySearchMode &&
                        !record.loadingPlaceholder &&
                        Number(record.childrenCount || 0) > 0,
                    onExpand: async (expanded, record) => {
                        if (categorySearchMode || !record.categoryId || record.loadingPlaceholder) {
                            return;
                        }
                        if (expanded) {
                            await loadCategoryChildren(record.categoryId);
                            setExpandedRowKeys((keys) => keys.includes(record.categoryId)
                                ? keys
                                : [...keys, record.categoryId]);
                            return;
                        }
                        setExpandedRowKeys((keys) => keys.filter((key) => key !== record.categoryId));
                    },
                }, toolBarRender: () => [
                    _jsx(Button, { icon: _jsx(HistoryOutlined, {}), onClick: () => setOperationLogOpen(true), children: "\u64CD\u4F5C\u65E5\u5FD7" }, "operationLog"),
                    _jsx(Button, { icon: _jsx(ImportOutlined, {}), hidden: !access.hasPerms('product:category:add'), onClick: () => setImportOpen(true), children: "\u5BFC\u5165" }, "import"),
                    _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms('product:category:add'), onClick: () => openCreateModal(), children: "\u65B0\u589E" }, "add"),
                ] }), _jsx(ProductImportModal, { title: "\u5BFC\u5165\u5546\u54C1\u5206\u7C7B", open: importOpen, onOpenChange: setImportOpen, onDownloadTemplate: downloadCategoryImportTemplate, onPreview: previewCategoryImport, onImport: importCategoryData, onSuccess: () => loadCategoryRows(categoryQuery) }), _jsx(ProductConfigChangeLogDrawer, { open: operationLogOpen, onOpenChange: setOperationLogOpen, bizType: "CATEGORY", title: "\u5546\u54C1\u5206\u7C7B\u914D\u7F6E" }), _jsxs(ModalForm, { title: currentCategory?.categoryId ? '编辑商品分类' : '新增商品分类', open: modalOpen, form: form, modalProps: { destroyOnHidden: true, onCancel: () => setModalOpen(false) }, onOpenChange: setModalOpen, onFinish: saveCategory, children: [_jsx(ProFormSelect, { name: "parentId", label: "\u4E0A\u7EA7\u5206\u7C7B", disabled: !!currentCategory?.categoryId, fieldProps: {
                            ...SEARCHABLE_SELECT_PROPS,
                            filterOption: false,
                            onSearch: loadParentOptions,
                            options: parentOptions,
                        }, rules: [{ required: true, message: '请选择上级分类' }] }), _jsx(ProFormText, { name: "categoryCode", label: "\u5206\u7C7B\u7F16\u7801", rules: [{ required: true, message: '请输入分类编码' }] }), _jsx(ProFormText, { name: "categoryName", label: "\u5206\u7C7B\u540D\u79F0", rules: [{ required: true, message: '请输入分类名称' }] }), _jsx(ProFormDigit, { name: "sortOrder", label: "\u6392\u5E8F", min: 0 }), _jsx(ProFormSelect, { name: "status", label: "\u72B6\u6001", options: statusOptions, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [{ required: true, message: '请选择状态' }] }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8" })] })] }));
}
