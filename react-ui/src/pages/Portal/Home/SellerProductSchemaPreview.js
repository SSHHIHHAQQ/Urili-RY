import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Card, Empty, Select, Space, Table, Tag, Tooltip, } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { attributeGroupOptions, attributeTypeOptions, optionSourceOptions, ruleModeOptions, yesNoOptions, } from '@/pages/Product/constants';
import { getSellerPortalProductCategories, getSellerPortalProductSchema, } from '@/services/portal/session';
import { message } from '@/utils/feedback';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
function displayText(value) {
    return value === undefined || value === null || value === '' ? '-' : String(value);
}
function optionLabel(options, value) {
    return options.find((item) => item.value === value)?.label || displayText(value);
}
function flagTag(value) {
    const label = optionLabel(yesNoOptions, value);
    return _jsx(Tag, { color: value === 'Y' ? 'success' : undefined, children: label });
}
function optionTags(options) {
    const enabledOptions = options || [];
    if (enabledOptions.length === 0) {
        return '-';
    }
    const visibleOptions = enabledOptions.slice(0, 4);
    const hiddenCount = enabledOptions.length - visibleOptions.length;
    return (_jsxs(Space, { wrap: true, size: [4, 4], children: [visibleOptions.map((option) => (_jsx(Tag, { children: option.optionLabel || option.optionCode }, option.optionCode || option.optionLabel))), hiddenCount > 0 ? _jsxs(Tag, { children: ["+", hiddenCount] }) : null] }));
}
export const PortalProductSchemaPreview = ({ categoryPlaceholder = '商品分类', getCategories, getSchema, title, }) => {
    const [categoryLoading, setCategoryLoading] = useState(false);
    const [schemaLoading, setSchemaLoading] = useState(false);
    const [categories, setCategories] = useState([]);
    const [schemaRows, setSchemaRows] = useState([]);
    const [selectedCategoryId, setSelectedCategoryId] = useState();
    const schemaRequestSeq = useRef(0);
    const loadCategories = useCallback(async () => {
        setCategoryLoading(true);
        try {
            const response = await getCategories();
            if (response.code !== 200) {
                message.error(response.msg || '商品分类加载失败');
                setCategories([]);
                setSelectedCategoryId(undefined);
                return;
            }
            const rows = response.data || [];
            setCategories(rows);
            setSelectedCategoryId((current) => {
                if (current && rows.some((item) => item.categoryId === current)) {
                    return current;
                }
                return rows[0]?.categoryId;
            });
        }
        catch (error) {
            console.log(error);
            message.error('商品分类加载失败');
            setCategories([]);
            setSelectedCategoryId(undefined);
        }
        finally {
            setCategoryLoading(false);
        }
    }, [getCategories]);
    const loadSchema = useCallback(async (categoryId) => {
        const requestSeq = schemaRequestSeq.current + 1;
        schemaRequestSeq.current = requestSeq;
        setSchemaLoading(true);
        try {
            const response = await getSchema(categoryId);
            if (schemaRequestSeq.current !== requestSeq) {
                return;
            }
            if (response.code !== 200) {
                message.error(response.msg || '商品 Schema 加载失败');
                setSchemaRows([]);
                return;
            }
            setSchemaRows(response.data || []);
        }
        catch (error) {
            console.log(error);
            if (schemaRequestSeq.current === requestSeq) {
                message.error('商品 Schema 加载失败');
                setSchemaRows([]);
            }
        }
        finally {
            if (schemaRequestSeq.current === requestSeq) {
                setSchemaLoading(false);
            }
        }
    }, [getSchema]);
    useEffect(() => {
        loadCategories();
    }, [loadCategories]);
    useEffect(() => {
        if (!selectedCategoryId) {
            setSchemaRows([]);
            return;
        }
        loadSchema(selectedCategoryId);
    }, [loadSchema, selectedCategoryId]);
    const categoryOptions = useMemo(() => categories
        .filter((item) => item.categoryId)
        .map((item) => ({
        label: `${displayText(item.categoryName)} / ${displayText(item.categoryCode)}`,
        value: item.categoryId,
        searchText: `${item.categoryName || ''} ${item.categoryCode || ''}`,
    })), [categories]);
    const selectedCategory = useMemo(() => categories.find((item) => item.categoryId === selectedCategoryId), [categories, selectedCategoryId]);
    const columns = [
        {
            title: '属性',
            dataIndex: 'attributeName',
            key: 'attributeName',
            width: 180,
            render: (value, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx("span", { children: displayText(value) }), _jsx("span", { style: { color: '#8c8c8c', fontSize: 12 }, children: displayText(record.attributeCode) })] })),
        },
        {
            title: '类型',
            dataIndex: 'attributeType',
            key: 'attributeType',
            width: 120,
            render: (value) => optionLabel(attributeTypeOptions, value),
        },
        {
            title: '必填',
            dataIndex: 'requiredFlag',
            key: 'requiredFlag',
            width: 88,
            render: flagTag,
        },
        {
            title: '可编辑',
            dataIndex: 'editableFlag',
            key: 'editableFlag',
            width: 88,
            render: flagTag,
        },
        {
            title: '可筛选',
            dataIndex: 'filterableFlag',
            key: 'filterableFlag',
            width: 88,
            render: flagTag,
        },
        {
            title: '分组',
            dataIndex: 'groupCode',
            key: 'groupCode',
            width: 120,
            render: (value) => optionLabel(attributeGroupOptions, value),
        },
        {
            title: '规则',
            dataIndex: 'ruleMode',
            key: 'ruleMode',
            width: 120,
            render: (value) => optionLabel(ruleModeOptions, value),
        },
        {
            title: '选项来源',
            dataIndex: 'optionSource',
            key: 'optionSource',
            width: 140,
            render: (value) => optionLabel(optionSourceOptions, value),
        },
        {
            title: '选项',
            dataIndex: 'options',
            key: 'options',
            width: 220,
            render: optionTags,
        },
        {
            title: '来源类目',
            dataIndex: 'sourceCategoryName',
            key: 'sourceCategoryName',
            width: 140,
            ellipsis: true,
            render: displayText,
        },
        {
            title: '提示',
            dataIndex: 'placeholder',
            key: 'placeholder',
            width: 160,
            ellipsis: true,
            render: (value, record) => {
                const text = value || record.helpText || record.validationRule;
                return text ? (_jsx(Tooltip, { title: text, children: _jsx("span", { children: text }) })) : ('-');
            },
        },
    ];
    return (_jsx(Card, { title: title, variant: "borderless", children: _jsxs(Space, { orientation: "vertical", size: 12, style: { width: '100%' }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Select, { ...SEARCHABLE_SELECT_PROPS, loading: categoryLoading, value: selectedCategoryId, options: categoryOptions, placeholder: categoryPlaceholder, style: { minWidth: 280 }, onChange: setSelectedCategoryId }), _jsxs(Tag, { children: ["Schema v", displayText(selectedCategory?.schemaVersion)] })] }), _jsx(Table, { size: "small", rowKey: (record) => `${record.categoryId || selectedCategoryId || 0}-${record.attributeId || record.attributeCode || record.attributeName || record.sortOrder || 'schema'}`, loading: schemaLoading, columns: columns, dataSource: schemaRows, pagination: false, scroll: { x: 1450 }, locale: { emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE }) } })] }) }));
};
const SellerProductSchemaPreview = () => (_jsx(PortalProductSchemaPreview, { title: "\u5546\u54C1\u53D1\u5E03\u51C6\u5907", getCategories: getSellerPortalProductCategories, getSchema: getSellerPortalProductSchema }));
export default SellerProductSchemaPreview;
