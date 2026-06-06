import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Descriptions, Drawer, Image, Table, Tag } from 'antd';
import { buildSkuDimensionText, buildSkuSpecText, getControlStatusText, getSalesStatusText, resolveResourceUrl, } from '../constants';
import DetailContentPreview from './DetailContentPreview';
import styles from '../style.module.css';
function statusText(status) {
    return getSalesStatusText(status);
}
function controlStatusText(status) {
    return getControlStatusText(status || 'NORMAL');
}
function controlStatusTag(record) {
    if (record.spuControlStatus === 'DISABLED') {
        return _jsx(Tag, { color: "error", children: "SPU\u505C\u7528" });
    }
    const status = record.controlStatus || 'NORMAL';
    return _jsx(Tag, { color: status === 'DISABLED' ? 'error' : 'success', children: controlStatusText(status) });
}
function amountText(value) {
    return value === undefined || value === null ? '--' : String(value);
}
function parseJsonArrayText(value) {
    if (!value)
        return '';
    try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed.filter(Boolean).join(' / ') : '';
    }
    catch {
        return value;
    }
}
function formatAttributeValue(item) {
    if (item.attributeType === 'BOOLEAN') {
        if (item.valueCode === 'Y')
            return '是';
        if (item.valueCode === 'N')
            return '否';
    }
    if (item.attributeType === 'MULTI_SELECT') {
        return parseJsonArrayText(item.valueJson) || '--';
    }
    return item.valueText
        || item.valueCode
        || (item.valueNumber !== undefined && item.valueNumber !== null ? String(item.valueNumber) : '')
        || item.valueDate
        || item.valueJson
        || '--';
}
export default function ProductDetailDrawer({ open, product, onClose, }) {
    const skuColumns = [
        {
            title: 'SKU图',
            dataIndex: 'skuImageUrl',
            width: 72,
            render: (url) => url ? _jsx(Image, { width: 44, height: 44, src: resolveResourceUrl(url), style: { objectFit: 'cover' } }) : '--',
        },
        { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160 },
        { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 160 },
        {
            title: 'SKU规格',
            width: 220,
            render: (_, record) => buildSkuSpecText(record, product?.skus || []) || '--',
        },
        {
            title: '尺寸重量',
            width: 220,
            render: (_, record) => buildSkuDimensionText(record) || '--',
        },
        { title: '供货价', dataIndex: 'supplyPrice', width: 100, render: (value) => amountText(value) },
        { title: '销售价', dataIndex: 'salePrice', width: 100, render: (value) => amountText(value) },
        { title: '币种', dataIndex: 'currencyCode', width: 90 },
        {
            title: '销售状态',
            dataIndex: 'skuStatus',
            width: 100,
            render: (value) => _jsx(Tag, { children: statusText(value) }),
        },
        {
            title: '管控',
            dataIndex: 'controlStatus',
            width: 100,
            render: (_, record) => controlStatusTag(record),
        },
    ];
    return (_jsx(Drawer, { title: "\u5546\u57CE\u5546\u54C1\u8BE6\u60C5", open: open, onClose: onClose, size: "large", destroyOnClose: true, children: product ? (_jsxs("div", { className: styles.detailDrawerBody, children: [_jsxs(Descriptions, { bordered: true, size: "small", column: 2, children: [_jsx(Descriptions.Item, { label: "\u7CFB\u7EDFSPU", children: product.systemSpuCode || '--' }), _jsx(Descriptions.Item, { label: "\u5BA2\u6237SPU", children: product.sellerSpuCode || '--' }), _jsx(Descriptions.Item, { label: "\u4E2D\u6587\u6807\u9898", children: product.productName || '--' }), _jsx(Descriptions.Item, { label: "\u82F1\u6587\u6807\u9898", children: product.productNameEn || '--' }), _jsx(Descriptions.Item, { label: "\u5356\u5BB6", children: product.sellerName || '--' }), _jsx(Descriptions.Item, { label: "\u5546\u54C1\u5206\u7C7B", children: product.categoryName || '--' }), _jsx(Descriptions.Item, { label: "\u9500\u552E\u72B6\u6001", children: statusText(product.spuStatus) }), _jsx(Descriptions.Item, { label: "\u7BA1\u63A7\u72B6\u6001", children: _jsx(Tag, { color: product.controlStatus === 'DISABLED' ? 'error' : 'success', children: controlStatusText(product.controlStatus) }) }), _jsx(Descriptions.Item, { label: "\u6765\u6E90", children: product.sourceType || '--' }), _jsx(Descriptions.Item, { label: "\u66F4\u65B0\u65F6\u95F4", children: product.updateTime || '--' }), _jsx(Descriptions.Item, { label: "\u5356\u70B9", span: 2, children: product.sellingPoint || '--' }), _jsx(Descriptions.Item, { label: "\u5907\u6CE8", span: 2, children: product.remark || '--' }), _jsx(Descriptions.Item, { label: "SPU\u4E3B\u56FE", span: 2, children: product.mainImageUrl ? (_jsx(Image, { width: 96, src: resolveResourceUrl(product.mainImageUrl) })) : '--' })] }), _jsxs("div", { children: [_jsx("div", { className: styles.sectionTitle, children: "\u8BE6\u60C5\u56FE\u6587" }), _jsx(DetailContentPreview, { value: product.detailContent })] }), _jsx(Table, { rowKey: "skuId", size: "small", pagination: false, columns: skuColumns, dataSource: product.skus || [], scroll: { x: 1320 } }), _jsx(Descriptions, { bordered: true, size: "small", column: 1, title: "\u7C7B\u76EE\u5C5E\u6027", children: (product.attributeValues || []).map((item) => (_jsx(Descriptions.Item, { label: item.attributeName || item.attributeCode, children: formatAttributeValue(item) }, item.valueId || item.attributeId))) })] })) : null }));
}
