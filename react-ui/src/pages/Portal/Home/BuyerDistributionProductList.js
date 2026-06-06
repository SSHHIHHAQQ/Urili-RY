import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { ProTable, } from '@ant-design/pro-components';
import { Button, Descriptions, Empty, Image, Modal, Space, Table, Tag, Typography, } from 'antd';
import { useRef, useState } from 'react';
import { buildSkuDimensionText, buildSkuSpecText, formatPriceRange, getSalesStatusText, resolveResourceUrl, } from '@/pages/Product/Distribution/constants';
import { getBuyerPortalDistributionProduct, getBuyerPortalDistributionProductSkus, getBuyerPortalDistributionProducts, } from '@/services/portal/session';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTablePagination, getProTableScroll, } from '@/utils/proTableSearch';
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
const BuyerDistributionProductList = () => {
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
                getBuyerPortalDistributionProduct(record.spuId),
                getBuyerPortalDistributionProductSkus(record.spuId),
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
            width: 340,
            search: false,
            render: (_, record) => (_jsxs(Space, { size: 10, children: [record.mainImageUrl ? (_jsx(Image, { width: 48, height: 48, src: resolveResourceUrl(record.mainImageUrl), style: { objectFit: 'cover' } })) : (_jsx("div", { style: { width: 48, height: 48, background: '#f0f0f0' } })), _jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Text, { ellipsis: true, style: { maxWidth: 240 }, children: displayText(record.productName) }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: displayText(record.productNameEn) })] })] })),
        },
        {
            title: '类目',
            dataIndex: 'categoryName',
            key: 'categoryName',
            width: 180,
            search: false,
            render: (_, record) => displayText(record.categoryName),
        },
        {
            title: '销售价',
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
            search: false,
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
            title: 'SKU规格',
            key: 'spec',
            width: 240,
            render: (_, record) => buildSkuSpecText(record, skuRows) || '-',
        },
        {
            title: '尺寸重量',
            key: 'dimension',
            width: 240,
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
    return (_jsxs(_Fragment, { children: [_jsx(ProTable, { actionRef: actionRef, rowKey: "uiRowKey", headerTitle: "\u5546\u57CE\u5546\u54C1", columns: columns, search: getPersistedProTableSearch({ labelWidth: 96, defaultFormItemsNumber: 4 }, 'buyer-portal-distribution-product'), pagination: getProTablePagination({
                    defaultPageSize: 5,
                    pageSizeOptions: [5, 10, 20],
                }), scroll: getProTableScroll(980), request: async ({ current: currentPage, pageSize: currentPageSize, ...params }) => {
                    const response = await getBuyerPortalDistributionProducts({
                        keyword: params.keyword,
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
                            uiRowKey: `${row.spuId || 'product'}-${index}`,
                        })),
                        total: response.total || 0,
                        success: true,
                    };
                }, toolBarRender: () => [
                    _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => actionRef.current?.reload(), children: "\u5237\u65B0" }, "refresh"),
                ], locale: { emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE }) } }), _jsx(Modal, { title: "\u5546\u54C1\u8BE6\u60C5", open: detailOpen, onCancel: () => setDetailOpen(false), footer: null, width: 920, destroyOnHidden: true, children: _jsxs(Space, { orientation: "vertical", size: 14, style: { width: '100%' }, children: [_jsxs(Descriptions, { column: 2, size: "small", bordered: true, children: [_jsx(Descriptions.Item, { label: "\u5546\u54C1\u72B6\u6001", children: statusTag(current?.spuStatus) }), _jsx(Descriptions.Item, { label: "\u7C7B\u76EE", children: displayText(current?.categoryName) }), _jsx(Descriptions.Item, { label: "\u5546\u54C1\u540D\u79F0", children: displayText(current?.productName) }), _jsx(Descriptions.Item, { label: "\u82F1\u6587\u540D\u79F0", children: displayText(current?.productNameEn) }), _jsx(Descriptions.Item, { label: "\u4EF7\u683C", children: formatPriceRange(current?.salePriceMin, current?.salePriceMax) }), _jsx(Descriptions.Item, { label: "\u5E01\u79CD", children: displayText(current?.currencySummary) }), _jsx(Descriptions.Item, { label: "\u5356\u70B9", span: 2, children: displayText(current?.sellingPoint) })] }), _jsx(Table, { size: "small", rowKey: (record) => `${record.spuId || current?.spuId || 0}-${record.skuId || 'sku'}`, loading: detailLoading, columns: skuColumns, dataSource: skuRows, pagination: false, scroll: { x: 760 }, locale: {
                                emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE }),
                            } })] }) })] }));
};
export default BuyerDistributionProductList;
