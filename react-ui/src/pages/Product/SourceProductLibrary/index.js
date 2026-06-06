import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EyeOutlined } from '@ant-design/icons';
import { PageContainer, ProTable, } from '@ant-design/pro-components';
import { Button, Space, Tag, Typography } from 'antd';
import { useRef, useState } from 'react';
import { pairingStatusText, skuPairingStatusSearchOptions, skuSyncItemStatusSearchOptions, syncItemStatusText, systemKindOptions, systemKindText, } from '@/services/integration/constants';
import { getSourceProductList } from '@/services/integration/sourceProduct';
import { getProTableColumnsState, getPersistedProTableSearch, getProTablePagination, getProTableScroll, } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { approveStatusOptions, approveStatusText, dimensionText, displayText, weightText, wmsDimensionText, wmsWeightText, } from './constants';
import SourceProductDetailDrawer from './SourceProductDetailDrawer';
import styles from './style.module.css';
const SOURCE_PRODUCT_SEARCH_FIELD_COUNT = 8;
function cleanParams(params) {
    return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''));
}
function statusTag(value) {
    const text = syncItemStatusText[value || ''] || value || '-';
    const color = value === 'ACTIVE' ? 'green' : value === 'MISSING' ? 'orange' : 'default';
    return _jsx(Tag, { color: color, children: text });
}
function pairingTag(value) {
    const text = pairingStatusText[value || ''] || value || '未配对';
    return _jsx(Tag, { color: value === 'PAIRED' ? 'blue' : 'default', children: text });
}
function approveTag(value) {
    if (!value) {
        return '-';
    }
    const color = value === '2' ? 'green' : value === '3' || value === '4' ? 'red' : 'gold';
    return _jsx(Tag, { color: color, children: approveStatusText[value] || value });
}
function sourceLabel(record) {
    return record.systemKindLabel || systemKindText[record.systemKind || ''] || record.systemKind || '-';
}
function renderProduct(record) {
    return (_jsx("div", { className: styles.productCell, children: _jsxs(Space, { className: styles.productTextStack, orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { className: styles.productText, strong: true, ellipsis: { tooltip: record.masterProductName }, children: record.masterProductName }), _jsx(Typography.Text, { className: `${styles.productText} ${styles.subText}`, ellipsis: { tooltip: record.productAliasName || record.productDescription }, children: record.productAliasName || record.productDescription || '-' })] }) }));
}
export default function SourceProductLibraryPage() {
    const actionRef = useRef(null);
    const [detailOpen, setDetailOpen] = useState(false);
    const [currentRecord, setCurrentRecord] = useState();
    const openDetail = (record) => {
        setCurrentRecord(record);
        setDetailOpen(true);
    };
    const columns = [
        {
            title: '来源系统',
            dataIndex: 'systemKind',
            hideInTable: true,
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: systemKindOptions,
            },
        },
        {
            title: '仓库名称',
            dataIndex: 'masterWarehouseName',
            hideInTable: true,
        },
        {
            title: '来源仓库',
            key: 'sourceWarehouse',
            dataIndex: 'sourceWarehouse',
            width: 170,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Tag, { color: "blue", children: sourceLabel(record) }), _jsx(Typography.Text, { children: displayText(record.masterWarehouseName) })] })),
        },
        {
            title: '来源 SKU',
            dataIndex: 'masterSku',
            width: 180,
            copyable: true,
            ellipsis: true,
        },
        {
            title: '商品名称',
            dataIndex: 'productName',
            hideInTable: true,
        },
        {
            title: '来源商品',
            key: 'masterProductName',
            dataIndex: 'masterProductName',
            width: 280,
            search: false,
            render: (_, record) => renderProduct(record),
        },
        {
            title: '识别码',
            dataIndex: 'identifyCodeKeyword',
            hideInTable: true,
        },
        {
            title: '条码 / FNSKU',
            key: 'mainCode',
            dataIndex: 'mainCode',
            width: 230,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { copyable: !!record.mainCode, children: displayText(record.mainCode) }), _jsxs(Typography.Text, { className: styles.subText, children: ["FNSKU ", displayText(record.fnsku)] })] })),
        },
        {
            title: '审核状态',
            dataIndex: 'approveStatus',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: approveStatusOptions,
            },
            width: 110,
            render: (_, record) => approveTag(record.approveStatus),
        },
        {
            title: '客户尺寸',
            key: 'customerDimension',
            dataIndex: 'length',
            width: 190,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { children: dimensionText(record) }), _jsx(Typography.Text, { className: styles.subText, children: weightText(record) })] })),
        },
        {
            title: '仓库尺寸',
            key: 'warehouseDimension',
            dataIndex: 'wmsLength',
            width: 190,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { children: wmsDimensionText(record) }), _jsx(Typography.Text, { className: styles.subText, children: wmsWeightText(record) })] })),
        },
        {
            title: '同步状态',
            dataIndex: 'status',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: skuSyncItemStatusSearchOptions,
            },
            width: 110,
            render: (_, record) => statusTag(record.status),
        },
        {
            title: '配对状态',
            dataIndex: 'pairingStatus',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: skuPairingStatusSearchOptions,
            },
            width: 110,
            render: (_, record) => pairingTag(record.pairingStatus),
        },
        {
            title: '匹配客户',
            dataIndex: 'customerName',
            width: 160,
            search: false,
            ellipsis: true,
            renderText: (value) => displayText(value),
        },
        {
            title: '商城商品',
            key: 'mallProduct',
            dataIndex: 'systemSku',
            width: 220,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { copyable: !!record.systemSku, children: displayText(record.systemSku) }), _jsx(Typography.Text, { className: styles.subText, ellipsis: { tooltip: record.systemSkuName }, children: displayText(record.systemSkuName) })] })),
        },
        {
            title: '同步时间',
            key: 'syncTime',
            dataIndex: 'lastSeenTime',
            width: 190,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { children: displayText(record.lastSeenTime) }), _jsxs(Typography.Text, { className: styles.subText, children: ["\u66F4\u65B0 ", displayText(record.updateTime)] })] })),
        },
        {
            title: '操作',
            key: 'option',
            valueType: 'option',
            width: 90,
            fixed: 'right',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openDetail(record), children: "\u67E5\u770B" }, "detail"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { title: false, children: [_jsx(ProTable, { actionRef: actionRef, className: "urili-fill-table", rowKey: (record) => `${record.connectionCode}:${record.masterSku}`, columns: columns, columnsState: getProTableColumnsState('source-product-library-columns'), search: getPersistedProTableSearch({ labelWidth: 96, fieldCount: SOURCE_PRODUCT_SEARCH_FIELD_COUNT }, 'source-product-library'), request: async (params) => {
                    const { current, pageSize, ...filters } = params;
                    const resp = await getSourceProductList(cleanParams({
                        pageNum: current,
                        pageSize,
                        ...filters,
                    }));
                    return {
                        data: resp.rows || [],
                        total: resp.total || 0,
                        success: resp.code === 200,
                    };
                }, pagination: getProTablePagination(20), options: { density: true, reload: true, setting: true }, scroll: getProTableScroll(2050), toolBarRender: () => [] }), _jsx(SourceProductDetailDrawer, { open: detailOpen, record: currentRecord, onClose: () => setDetailOpen(false) })] }));
}
