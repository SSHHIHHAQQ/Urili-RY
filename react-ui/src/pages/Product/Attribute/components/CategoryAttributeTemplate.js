import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { HistoryOutlined, PlusOutlined } from '@ant-design/icons';
import { ModalForm, ProFormDigit, ProFormSelect, ProFormText, ProFormTextArea, ProTable, } from '@ant-design/pro-components';
import { Button, Form, Modal } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { deleteCategoryAttribute, getCategoryAttributeList, getCategoryChildren, getCategorySchema, getEnabledAttributeList, searchCategories, saveCategoryAttribute, } from '@/services/product/product';
import { message } from '@/utils/feedback';
import { getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { mergeCategoryChildren, normalizeLazyCategoryRows, toLazyCategoryTreeData, } from '../../categoryTree';
import { attributeGroupOptions, attributeTypeOptions, optionArrayToValueEnum, ruleModeOptions, statusOptions, yesNoOptions, } from '../../constants';
import ProductConfigChangeLogDrawer from '../../components/ProductConfigChangeLogDrawer';
import './CategoryAttributeTemplate.css';
import CategoryTreeFilterPanel from './CategoryTreeFilterPanel';
import { buildCategoryAttributeColumns } from './categoryAttributeColumns';
const defaultRuleValues = {
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
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
export default function CategoryAttributeTemplate({ access, }) {
    const directActionRef = useRef(null);
    const schemaActionRef = useRef(null);
    const loadedCategoryParentIds = useRef(new Set());
    const categorySearchPageRef = useRef(1);
    const categorySearchLoadedRef = useRef(0);
    const categorySearchLoadingMoreRef = useRef(false);
    const [form] = Form.useForm();
    const [categoryRows, setCategoryRows] = useState([]);
    const [categoryTreeLoading, setCategoryTreeLoading] = useState(false);
    const [categorySearchLoadingMore, setCategorySearchLoadingMore] = useState(false);
    const [categorySearchMode, setCategorySearchMode] = useState(false);
    const [categorySearchLoaded, setCategorySearchLoaded] = useState(0);
    const [categorySearchTotal, setCategorySearchTotal] = useState(0);
    const [categorySearchHasMore, setCategorySearchHasMore] = useState(false);
    const [attributes, setAttributes] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState();
    const [categoryKeyword, setCategoryKeyword] = useState('');
    const [categoryStatus, setCategoryStatus] = useState('0');
    const [categoryLevel, setCategoryLevel] = useState('ALL');
    const [leafOnly, setLeafOnly] = useState(false);
    const [expandedCategoryKeys, setExpandedCategoryKeys] = useState([]);
    const [autoExpandParent, setAutoExpandParent] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const [currentRule, setCurrentRule] = useState();
    const [operationLogOpen, setOperationLogOpen] = useState(false);
    const treeData = useMemo(() => toLazyCategoryTreeData(categoryRows, categorySearchMode, categorySearchMode), [categoryRows, categorySearchMode]);
    const categoryLevelOptions = useMemo(() => {
        return [
            { label: '全部层级', value: 'ALL' },
            ...Array.from({ length: 12 }, (_, index) => index + 1).map((level) => ({
                label: `${level}级类目`,
                value: String(level),
            })),
        ];
    }, []);
    const attributeTypeValueEnum = useMemo(() => optionArrayToValueEnum(attributeTypeOptions), []);
    const attributeGroupValueEnum = useMemo(() => optionArrayToValueEnum(attributeGroupOptions), []);
    const attributeOptions = useMemo(() => attributes.map((item) => ({
        label: `${item.attributeName} (${item.attributeCode})`,
        value: item.attributeId,
    })), [attributes]);
    const categoryEffectiveStatusParam = categoryStatus === 'ALL' ? undefined : categoryStatus;
    const loadAttributeOptions = useCallback(async (keyword = '') => {
        const resp = await getEnabledAttributeList({
            keyword,
            pageNum: 1,
            pageSize: 50,
        });
        setAttributes(resp.data || []);
    }, []);
    const loadCategoryChildren = useCallback(async (parentId = 0, force = false) => {
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
            setCategoryRows((previous) => parentId === 0 ? rows : mergeCategoryChildren(previous, parentId, rows));
            if (parentId === 0) {
                setSelectedCategoryId((current) => force ? rows[0]?.categoryId : current || rows[0]?.categoryId);
            }
        }
        finally {
            setCategoryTreeLoading(false);
        }
    }, [categoryEffectiveStatusParam]);
    const searchCategoryRows = useCallback(async (pageNum = 1, append = false) => {
        if (append && categorySearchLoadingMoreRef.current) {
            return;
        }
        if (append) {
            categorySearchLoadingMoreRef.current = true;
            setCategorySearchLoadingMore(true);
        }
        else {
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
                categoryLevel: categoryLevel === 'ALL' ? undefined : Number(categoryLevel),
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
                setSelectedCategoryId((current) => current && rows.some((item) => item.categoryId === current)
                    ? current
                    : rows[0]?.categoryId);
                setExpandedCategoryKeys([]);
                setAutoExpandParent(false);
            }
        }
        finally {
            if (append) {
                categorySearchLoadingMoreRef.current = false;
                setCategorySearchLoadingMore(false);
            }
            else {
                setCategoryTreeLoading(false);
            }
        }
    }, [
        categoryKeyword,
        categoryLevel,
        categoryEffectiveStatusParam,
        leafOnly,
    ]);
    const loadMoreCategorySearchRows = useCallback(() => {
        if (!categorySearchMode ||
            categoryTreeLoading ||
            categorySearchLoadingMoreRef.current ||
            categorySearchLoadingMore ||
            !categorySearchHasMore) {
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
            if (categoryKeyword.trim() ||
                categoryLevel !== 'ALL' ||
                leafOnly ||
                categoryStatus === '1') {
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
    const openEditRule = (record) => {
        if (record.attributeId) {
            setAttributes((previous) => previous.some((item) => item.attributeId === record.attributeId)
                ? previous
                : [
                    {
                        attributeId: record.attributeId,
                        attributeCode: record.attributeCode,
                        attributeName: record.attributeName,
                    },
                    ...previous,
                ]);
        }
        setCurrentRule(record);
        setModalOpen(true);
    };
    const saveRule = async (values) => {
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
    const removeRule = (record) => {
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
                const ok = resultOk(await deleteCategoryAttribute(categoryAttributeId), '类目属性规则已移除');
                if (ok) {
                    directActionRef.current?.reload();
                    schemaActionRef.current?.reload();
                }
            },
        });
    };
    const { directColumns, schemaColumns } = useMemo(() => buildCategoryAttributeColumns({
        access,
        attributeTypeValueEnum,
        attributeGroupValueEnum,
        onEditRule: openEditRule,
        onRemoveRule: removeRule,
    }), [access, attributeGroupValueEnum, attributeTypeValueEnum]);
    return (_jsxs(_Fragment, { children: [_jsxs("div", { className: "product-category-attribute-template", children: [_jsx("div", { className: "product-category-attribute-template__category-pane", children: _jsx(CategoryTreeFilterPanel, { treeData: treeData, selectedCategoryId: selectedCategoryId, expandedCategoryKeys: expandedCategoryKeys, autoExpandParent: autoExpandParent, loading: categoryTreeLoading, loadingMore: categorySearchLoadingMore, searchMode: categorySearchMode, searchResultLoaded: categorySearchLoaded, searchResultTotal: categorySearchTotal, searchHasMore: categorySearchHasMore, categoryKeyword: categoryKeyword, categoryStatus: categoryStatus, categoryLevel: categoryLevel, categoryLevelOptions: categoryLevelOptions, leafOnly: leafOnly, onCategoryKeywordChange: setCategoryKeyword, onCategoryStatusChange: setCategoryStatus, onCategoryLevelChange: setCategoryLevel, onLeafOnlyChange: setLeafOnly, onExpandedCategoryKeysChange: (keys, nextAutoExpandParent) => {
                                setExpandedCategoryKeys(keys);
                                setAutoExpandParent(nextAutoExpandParent);
                            }, onLoadCategoryChildren: loadCategoryChildren, onLoadMoreSearchResults: loadMoreCategorySearchRows, onSelectCategory: setSelectedCategoryId }) }), _jsxs("div", { className: "product-category-attribute-template__rules-pane", children: [_jsx("div", { className: "product-category-attribute-template__table-frame product-category-attribute-template__table-frame--direct", children: _jsx(ProTable, { className: "product-category-attribute-template__rule-table", actionRef: directActionRef, rowKey: "categoryAttributeId", headerTitle: "\u672C\u7C7B\u76EE\u89C4\u5219", columns: directColumns, search: false, pagination: false, size: "small", scroll: getProTableScroll(1250), request: async () => {
                                        if (!selectedCategoryId) {
                                            return { data: [], success: true };
                                        }
                                        const resp = await getCategoryAttributeList(selectedCategoryId);
                                        return {
                                            data: resp.data || [],
                                            success: resp.code === 200,
                                        };
                                    }, toolBarRender: () => [
                                        _jsx(Button, { icon: _jsx(HistoryOutlined, {}), onClick: () => setOperationLogOpen(true), children: "\u64CD\u4F5C\u65E5\u5FD7" }, "operationLog"),
                                        _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms('product:categoryAttribute:edit'), onClick: openCreateRule, children: "\u65B0\u589E" }, "add"),
                                    ] }) }), _jsx("div", { className: "product-category-attribute-template__table-frame product-category-attribute-template__table-frame--schema", children: _jsx(ProTable, { className: "product-category-attribute-template__rule-table", actionRef: schemaActionRef, rowKey: (record) => `${record.categoryId}-${record.attributeId}-${record.ruleMode}`, headerTitle: "\u7EE7\u627F\u9884\u89C8", columns: schemaColumns, search: false, pagination: false, size: "small", scroll: getProTableScroll(1400), request: async () => {
                                        if (!selectedCategoryId) {
                                            return { data: [], success: true };
                                        }
                                        const resp = await getCategorySchema(selectedCategoryId);
                                        return {
                                            data: resp.data || [],
                                            success: resp.code === 200,
                                        };
                                    } }) })] })] }), _jsx(ProductConfigChangeLogDrawer, { open: operationLogOpen, onOpenChange: setOperationLogOpen, bizType: "CATEGORY_ATTRIBUTE_RULE", title: "\u7C7B\u76EE\u5C5E\u6027\u6A21\u677F" }), _jsxs(ModalForm, { title: currentRule?.categoryAttributeId ? '编辑本类目规则' : '新增本类目规则', open: modalOpen, form: form, modalProps: {
                    destroyOnHidden: true,
                    onCancel: () => setModalOpen(false),
                }, onOpenChange: setModalOpen, onFinish: saveRule, children: [_jsx(ProFormSelect, { name: "attributeId", label: "\u5546\u54C1\u5C5E\u6027", options: attributeOptions, disabled: !!currentRule?.categoryAttributeId, fieldProps: {
                            ...SEARCHABLE_SELECT_PROPS,
                            filterOption: false,
                            onSearch: loadAttributeOptions,
                        }, rules: [{ required: true, message: '请选择商品属性' }] }), _jsx(ProFormSelect, { name: "ruleMode", label: "\u89C4\u5219\u6A21\u5F0F", options: ruleModeOptions, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [{ required: true, message: '请选择规则模式' }] }), _jsx(ProFormSelect, { name: "requiredFlag", label: "\u5FC5\u586B", options: yesNoOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormSelect, { name: "visibleFlag", label: "\u5C55\u793A", options: yesNoOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormSelect, { name: "editableFlag", label: "\u53EF\u7F16\u8F91", options: yesNoOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormSelect, { name: "filterableFlag", label: "\u53EF\u7B5B\u9009", options: yesNoOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormSelect, { name: "groupCode", label: "\u5C5E\u6027\u5206\u7EC4", options: attributeGroupOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormDigit, { name: "sortOrder", label: "\u6392\u5E8F", min: 0 }), _jsx(ProFormText, { name: "placeholder", label: "\u5360\u4F4D\u63D0\u793A" }), _jsx(ProFormText, { name: "helpText", label: "\u5E2E\u52A9\u6587\u6848" }), _jsx(ProFormTextArea, { name: "validationRule", label: "\u6821\u9A8C\u89C4\u5219 JSON" }), _jsx(ProFormSelect, { name: "status", label: "\u72B6\u6001", options: statusOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8" })] })] }));
}
