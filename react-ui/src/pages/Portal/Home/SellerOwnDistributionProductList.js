import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { ProTable, } from '@ant-design/pro-components';
import { Button, Descriptions, Empty, Image, Modal, Space, Table, Tag, Typography, } from 'antd';
import { useRef, useState } from 'react';
import { buildSkuDimensionText, buildSkuSpecText, formatPriceRange, getSalesStatusText, resolveResourceUrl, } from '@/pages/Product/Distribution/constants';
import { getSellerPortalDistributionProduct, getSellerPortalDistributionProductSkus, getSellerPortalDistributionProducts, } from '@/services/portal/session';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTablePagination, getProTableScroll, } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
function displayText(value) {
    return value === undefined || value === null || value === ''
        ? '-'
        : String(value);
}
function statusTag(status) {
    const label = getSalesStatusText(status);
    const color = status === 'ON_SALE'
        ? 'success'
        : status === 'DISABLED'
            ? 'error'
            : undefined;
    return _jsx(Tag, { color: color, children: label });
}
const PRODUCT_STATUS_VALUE_ENUM = {
    DRAFT: { text: getSalesStatusText('DRAFT') },
    READY: { text: getSalesStatusText('READY') },
    ON_SALE: { text: getSalesStatusText('ON_SALE') },
    OFF_SALE: { text: getSalesStatusText('OFF_SALE') },
    DISABLED: { text: getSalesStatusText('DISABLED') },
};
const PRODUCT_STATUS_OPTIONS = Object.entries(PRODUCT_STATUS_VALUE_ENUM).map(([value, item]) => ({ value, label: item.text }));
const SellerOwnDistributionProductList = () => {
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailOpen, setDetailOpen] = useState(false);
    const [current, setCurrent] = useState();
    const [skuRows, setSkuRows] = useState([]);
    const actionRef = useRef(undefined);
    const openDetail = async (record) => {
        if (!record.spuId) {
            return;
        }
        setDetailOpen(true);
        setDetailLoading(true);
        setCurrent(record);
        setSkuRows([]);
        try {
            const [detailResponse, skuResponse] = await Promise.all([
                getSellerPortalDistributionProduct(record.spuId),
                getSellerPortalDistributionProductSkus(record.spuId),
            ]);
            if (detailResponse.code !== 200) {
                message.error(detailResponse.msg || '商品详情加载失败');
                return;
            }
            if (skuResponse.code !== 200) {
                message.error(skuResponse.msg || '商品 SKU 加载失败');
                return;
            }
            setCurrent(detailResponse.data);
            setSkuRows(skuResponse.data || []);
        }
        catch (error) {
            console.log(error);
            message.error('商品详情加载失败');
        }
        finally {
            setDetailLoading(false);
        }
    };
    const columns = [
        {
            title: '关键词',
            dataIndex: 'keyword',
            hideInTable: true,
        },
        {
            title: '商品',
            dataIndex: 'productName',
            key: 'productName',
            width: 320,
            search: false,
            render: (_, record) => (_jsxs(Space, { size: 10, children: [record.mainImageUrl ? (_jsx(Image, { width: 48, height: 48, src: resolveResourceUrl(record.mainImageUrl), style: { objectFit: 'cover' } })) : (_jsx("div", { style: { width: 48, height: 48, background: '#f0f0f0' } })), _jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { ellipsis: true, style: { maxWidth: 220 }, children: displayText(record.productName) }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: displayText(record.productNameEn) })] })] })),
        },
        {
            title: '客户SPU',
            dataIndex: 'sellerSpuCode',
            key: 'sellerSpuCode',
            width: 140,
            render: (_, record) => displayText(record.sellerSpuCode),
        },
        {
            title: '客户SKU',
            dataIndex: 'sellerSkuCode',
            hideInTable: true,
        },
        {
            title: '类目',
            dataIndex: 'categoryName',
            key: 'categoryName',
            width: 160,
            search: false,
            render: (_, record) => displayText(record.categoryName),
        },
        {
            title: '价格',
            key: 'price',
            width: 160,
            search: false,
            render: (_, record) => (_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx("span", { children: formatPriceRange(record.salePriceMin, record.salePriceMax) }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: displayText(record.currencySummary) })] })),
        },
        {
            title: 'SKU',
            dataIndex: 'skuCount',
            key: 'skuCount',
            width: 80,
            search: false,
            render: (_, record) => displayText(record.skuCount),
        },
        {
            title: '状态',
            dataIndex: 'spuStatus',
            key: 'spuStatus',
            width: 96,
            valueType: 'select',
            valueEnum: PRODUCT_STATUS_VALUE_ENUM,
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: PRODUCT_STATUS_OPTIONS,
            },
            render: (_, record) => statusTag(record.spuStatus),
        },
        {
            title: '操作',
            valueType: 'option',
            width: 88,
            render: (_, record) => (_jsx(Button, { type: "link", size: "small", onClick: () => openDetail(record), children: "\u8BE6\u60C5" })),
        },
    ];
    const skuColumns = [
        {
            title: '客户SKU',
            dataIndex: 'sellerSkuCode',
            key: 'sellerSkuCode',
            width: 150,
            render: displayText,
        },
        {
            title: 'SKU规格',
            key: 'spec',
            width: 220,
            render: (_, record) => buildSkuSpecText(record, skuRows) || '-',
        },
        {
            title: '尺寸重量',
            key: 'dimension',
            width: 220,
            render: (_, record) => buildSkuDimensionText(record) || '-',
        },
        {
            title: '销售价',
            dataIndex: 'salePrice',
            key: 'salePrice',
            width: 100,
            render: displayText,
        },
        {
            title: '币种',
            dataIndex: 'currencyCode',
            key: 'currencyCode',
            width: 80,
            render: displayText,
        },
        {
            title: '状态',
            dataIndex: 'skuStatus',
            key: 'skuStatus',
            width: 96,
            render: statusTag,
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(ProTable, { actionRef: actionRef, rowKey: "uiRowKey", headerTitle: "\u6211\u7684\u5546\u57CE\u5546\u54C1", columns: columns, search: getPersistedProTableSearch({ labelWidth: 96, defaultFormItemsNumber: 4 }, 'seller-portal-distribution-product'), pagination: getProTablePagination({
                    defaultPageSize: 5,
                    pageSizeOptions: [5, 10, 20],
                }), scroll: getProTableScroll(1040), request: async ({ current: currentPage, pageSize: currentPageSize, ...params }) => {
                    const response = await getSellerPortalDistributionProducts({
                        keyword: params.keyword,
                        sellerSpuCode: params.sellerSpuCode,
                        sellerSkuCode: params.sellerSkuCode,
                        spuStatus: params.spuStatus,
                        pageNum: currentPage,
                        pageSize: currentPageSize,
                    });
                    if (response.code !== 200) {
                        message.error(response.msg || '商品加载失败');
                        return { data: [], total: 0, success: false };
                    }
                    return {
                        data: (response.rows || []).map((row, index) => ({
                            ...row,
                            uiRowKey: `${row.spuId || 'product'}-${row.sellerSpuCode || ''}-${index}`,
                        })),
                        total: response.total || 0,
                        success: true,
                    };
                }, toolBarRender: () => [
                    _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => actionRef.current?.reload(), children: "\u5237\u65B0" }, "refresh"),
                ], locale: { emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE }) } }), _jsx(Modal, { title: "\u5546\u54C1\u8BE6\u60C5", open: detailOpen, onCancel: () => setDetailOpen(false), footer: null, width: 920, destroyOnHidden: true, children: _jsxs(Space, { orientation: "vertical", size: 14, style: { width: '100%' }, children: [_jsxs(Descriptions, { column: 2, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u5BA2\u6237SPU", children: displayText(current?.sellerSpuCode) }), _jsx(Descriptions.Item, { label: "\u5546\u54C1\u72B6\u6001", children: statusTag(current?.spuStatus) }), _jsx(Descriptions.Item, { label: "\u5546\u54C1\u540D\u79F0", children: displayText(current?.productName) }), _jsx(Descriptions.Item, { label: "\u82F1\u6587\u540D\u79F0", children: displayText(current?.productNameEn) }), _jsx(Descriptions.Item, { label: "\u7C7B\u76EE", children: displayText(current?.categoryName) }), _jsx(Descriptions.Item, { label: "\u4EF7\u683C", children: formatPriceRange(current?.salePriceMin, current?.salePriceMax) }), _jsx(Descriptions.Item, { label: "\u5356\u70B9", span: 2, children: displayText(current?.sellingPoint) })] }), _jsx(Table, { size: "small", rowKey: (record) => `${record.spuId || current?.spuId || 0}-${record.skuId || record.sellerSkuCode || 'sku'}`, loading: detailLoading, columns: skuColumns, dataSource: skuRows, pagination: false, scroll: { x: 860 }, locale: {
                                emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE }),
                            } })] }) })] }));
};
export default SellerOwnDistributionProductList;
