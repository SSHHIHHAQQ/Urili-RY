import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownOutlined, HistoryOutlined, PlusOutlined } from '@ant-design/icons';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { history, useAccess } from '@umijs/max';
import { Alert, Button, Dropdown, Image, Input, InputNumber, Modal, Radio, Select, Space, Table, Tabs, Tag, Typography, } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getAdminSellerList } from '@/services/seller/seller';
import { getCategoryList } from '@/services/product/product';
import { batchUpdateDistributionControlStatus, batchUpdateDistributionSkuSalePrices, batchUpdateDistributionStatus, getDistributionProduct, getDistributionProductList, getDistributionSkuList, } from '@/services/product/distributionProduct';
import { message, modal } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import { buildCategoryTree, toCategoryTreeSelectData } from '../categoryTree';
import { buildSkuDimensionText, buildSkuSpecText, formatPriceRange, getControlStatusText, getSalesStatusText, resolveResourceUrl, salesStatusTabOptions, salesStatusValueEnum, sourceTypeValueEnum, warehouseKindText, } from './constants';
import ProductDetailDrawer from './components/ProductDetailDrawer';
import ProductDistributionOperationLogDrawer from './components/ProductDistributionOperationLogDrawer';
import styles from './style.module.css';
const viewModeOptions = [
    { label: 'SPU视图', value: 'SPU' },
    { label: 'SKU视图', value: 'SKU' },
];
const salesStatusColor = {
    DRAFT: 'default',
    READY: 'warning',
    ON_SALE: 'success',
    OFF_SALE: 'processing',
};
const statusFlowMap = {
    DRAFT: { targetStatus: 'READY', label: '提交待上架', batchLabel: '提交待上架' },
    READY: { targetStatus: 'ON_SALE', label: '上架', batchLabel: '批量上架' },
    ON_SALE: { targetStatus: 'OFF_SALE', label: '下架', batchLabel: '批量下架' },
    OFF_SALE: { targetStatus: 'ON_SALE', label: '上架', batchLabel: '批量上架' },
};
const priceModeOptions = [
    { label: '按供货价加价', value: 'SUPPLY_MARKUP' },
    { label: '按当前售价调整', value: 'CURRENT_ADJUST' },
    { label: '统一设置售价', value: 'FIXED' },
];
const tailRuleOptions = [
    { label: '不处理尾数', value: 'NONE' },
    { label: '尾数 .99', value: 'TAIL_99' },
    { label: '尾数 .90', value: 'TAIL_90' },
    { label: '尾数 .09', value: 'TAIL_09' },
    { label: '取整数', value: 'INTEGER' },
];
const TABLE_SELECTION_COLUMN_WIDTH = 48;
const SPU_TABLE_SCROLL_X = 2580;
const SKU_TABLE_SCROLL_X = 3000;
const SKU_DETAIL_TABLE_SCROLL_X = 1680;
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
function compactIds(rows, ownerType) {
    const field = ownerType === 'SPU' ? 'spuId' : 'skuId';
    return rows
        .map((row) => row[field])
        .filter((value) => typeof value === 'number' && Number.isFinite(value));
}
function renderSalesStatusTag(status) {
    return _jsx(Tag, { color: salesStatusColor[status || ''], children: getSalesStatusText(status) });
}
function renderSpuControlStatusTag(record) {
    const status = record.controlStatus || 'NORMAL';
    return _jsx(Tag, { color: status === 'DISABLED' ? 'error' : 'success', children: getControlStatusText(status) });
}
function renderSkuControlStatusTag(record) {
    if (record.spuControlStatus === 'DISABLED') {
        return _jsx(Tag, { color: "error", children: "SPU\u505C\u7528" });
    }
    return _jsx(Tag, { color: record.controlStatus === 'DISABLED' ? 'error' : 'success', children: getControlStatusText(record.controlStatus || 'NORMAL') });
}
function renderWarehouseKindTag(kind) {
    if (!kind)
        return '--';
    if (kind === 'MIXED') {
        return _jsx(Tag, { color: "error", children: warehouseKindText[kind] });
    }
    const color = kind === 'official' ? 'blue' : 'purple';
    return _jsx(Tag, { color: color, children: warehouseKindText[kind] || kind });
}
function formatAmount(value) {
    if (value === undefined || value === null)
        return '--';
    return String(value);
}
function applyTailRule(value, rule) {
    if (!Number.isFinite(value) || value < 0)
        return undefined;
    if (rule === 'INTEGER')
        return Number(Math.round(value).toFixed(2));
    if (rule === 'TAIL_99')
        return Number((Math.floor(value) + 0.99).toFixed(2));
    if (rule === 'TAIL_90')
        return Number((Math.floor(value) + 0.9).toFixed(2));
    if (rule === 'TAIL_09')
        return Number((Math.floor(value) + 0.09).toFixed(2));
    return Number(value.toFixed(2));
}
export default function ProductDistributionPage() {
    const access = useAccess();
    const canViewDistributionDetail = access.hasPerms('product:distribution:query');
    const actionRef = useRef(null);
    const [detailOpen, setDetailOpen] = useState(false);
    const [current, setCurrent] = useState();
    const [sellerOptions, setSellerOptions] = useState([]);
    const [categories, setCategories] = useState([]);
    const [statusTab, setStatusTab] = useState('READY');
    const [viewMode, setViewMode] = useState('SPU');
    const [selectedSpuRows, setSelectedSpuRows] = useState([]);
    const [selectedSkuRows, setSelectedSkuRows] = useState([]);
    const [controlReason, setControlReason] = useState('');
    const [controlModal, setControlModal] = useState({
        open: false,
        ownerType: 'SPU',
        ids: [],
        targetStatus: 'DISABLED',
    });
    const [priceRows, setPriceRows] = useState([]);
    const [priceModalOpen, setPriceModalOpen] = useState(false);
    const [priceMode, setPriceMode] = useState('SUPPLY_MARKUP');
    const [priceAdjustDirection, setPriceAdjustDirection] = useState('UP');
    const [priceNumberType, setPriceNumberType] = useState('PERCENT');
    const [priceAmount, setPriceAmount] = useState();
    const [tailRule, setTailRule] = useState('NONE');
    const [priceReason, setPriceReason] = useState('');
    const [operationLogOpen, setOperationLogOpen] = useState(false);
    const categoryTreeData = useMemo(() => toCategoryTreeSelectData(buildCategoryTree(categories)), [categories]);
    const selectedRows = viewMode === 'SPU' ? selectedSpuRows : selectedSkuRows;
    const selectedIds = compactIds(selectedRows, viewMode);
    const currentFlow = statusFlowMap[statusTab];
    const priceCurrencyText = useMemo(() => {
        const currencies = Array.from(new Set(priceRows.map((row) => row.currencyCode).filter(Boolean)));
        if (currencies.length === 0)
            return '金额';
        return currencies.length === 1 ? currencies[0] : '多币种';
    }, [priceRows]);
    useEffect(() => {
        getAdminSellerList({ pageNum: 1, pageSize: 100, status: '0' }).then((resp) => {
            setSellerOptions((resp.rows || []).map((seller) => ({
                label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}（${seller.sellerNo || '-'}）`,
                value: seller.sellerId,
            })));
        });
        getCategoryList({ status: '0' }).then((resp) => setCategories(resp.data || []));
    }, []);
    useEffect(() => {
        setSelectedSpuRows([]);
        setSelectedSkuRows([]);
    }, [statusTab, viewMode]);
    const reload = () => actionRef.current?.reload();
    const statusTabItems = useMemo(() => salesStatusTabOptions.map((item) => ({ key: item.value, label: item.label })), []);
    const renderInventoryNumber = (value) => (value === undefined || value === null ? '--' : value);
    const renderInventoryStatus = (record) => {
        if (record.inventoryStatus) {
            return _jsx(Tag, { children: record.inventoryStatus });
        }
        if (record.availableStock == null && record.warehouseCount == null) {
            return '--';
        }
        return record.availableStock && record.availableStock > 0
            ? _jsx(Tag, { color: "success", children: "\u6709\u5E93\u5B58" })
            : _jsx(Tag, { children: "\u65E0\u5E93\u5B58" });
    };
    const openDetail = async (record) => {
        if (!canViewDistributionDetail || record.spuId == null) {
            return;
        }
        const resp = await getDistributionProduct(record.spuId);
        setCurrent(resp.data);
        setDetailOpen(true);
    };
    const openEdit = async (record) => {
        history.push(`/product/distribution/edit/${record.spuId}`);
    };
    const openSkuEdit = (record) => {
        if (record.spuId == null) {
            return;
        }
        history.push(`/product/distribution/edit/${record.spuId}?skuId=${record.skuId || ''}`);
    };
    const executeSalesStatus = (ownerType, ids, targetStatus, label) => {
        if (ids.length === 0) {
            message.warning('请先选择商品');
            return;
        }
        const shouldAskSkuSync = ownerType === 'SPU' && ['ON_SALE', 'OFF_SALE'].includes(targetStatus);
        let syncSkuStatus = true;
        const skuSyncActionText = targetStatus === 'OFF_SALE' ? '下架' : '上架';
        modal.confirm({
            title: label,
            content: shouldAskSkuSync ? (_jsxs(Space, { orientation: "vertical", size: 12, children: [_jsx("div", { children: `确认对 ${ids.length} 个 SPU 执行“${label}”？` }), _jsx(Alert, { type: "info", showIcon: true, title: "SKU\u540C\u6B65\u5904\u7406", description: `可以同时${skuSyncActionText}符合当前状态流转条件的 SKU，也可以只调整 SPU 状态。` }), _jsx(Radio.Group, { defaultValue: "WITH_SKU", onChange: (event) => {
                            syncSkuStatus = event.target.value === 'WITH_SKU';
                        }, children: _jsxs(Space, { orientation: "vertical", children: [_jsx(Radio, { value: "WITH_SKU", children: `同时${skuSyncActionText}可处理 SKU` }), _jsx(Radio, { value: "SPU_ONLY", children: "\u4EC5\u8C03\u6574 SPU" })] }) })] })) : `确认对 ${ids.length} 个${ownerType}执行“${label}”？`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const ok = resultOk(await batchUpdateDistributionStatus(ownerType, ids, targetStatus, shouldAskSkuSync ? syncSkuStatus : undefined), '状态已更新');
                if (ok)
                    reload();
            },
        });
    };
    const openControlModal = (ownerType, ids, targetStatus) => {
        if (ids.length === 0) {
            message.warning('请先选择商品');
            return;
        }
        setControlReason('');
        setControlModal({ open: true, ownerType, ids, targetStatus });
    };
    const submitControlStatus = async () => {
        if (controlModal.targetStatus === 'DISABLED' && !controlReason.trim()) {
            message.warning('请输入停用原因');
            return;
        }
        const ok = resultOk(await batchUpdateDistributionControlStatus(controlModal.ownerType, controlModal.ids, controlModal.targetStatus, controlReason.trim()), controlModal.targetStatus === 'DISABLED' ? '商品已停用' : '商品已恢复');
        if (ok) {
            setControlModal({ ...controlModal, open: false });
            reload();
        }
    };
    const buildStatusQuery = (activeStatusTab) => {
        if (!activeStatusTab || activeStatusTab === 'ALL')
            return {};
        if (activeStatusTab === 'DISABLED')
            return { controlStatus: 'DISABLED' };
        return { spuStatus: activeStatusTab, controlStatus: 'NORMAL' };
    };
    const buildSkuStatusQuery = (activeStatusTab) => {
        if (!activeStatusTab || activeStatusTab === 'ALL')
            return {};
        if (activeStatusTab === 'DISABLED')
            return { controlStatus: 'DISABLED' };
        return { skuStatus: activeStatusTab, controlStatus: 'NORMAL' };
    };
    const calculatePreviewPrice = (record) => {
        if (priceAmount === undefined || priceAmount === null)
            return undefined;
        if (priceMode === 'FIXED')
            return applyTailRule(priceAmount, tailRule);
        const baseValue = priceMode === 'SUPPLY_MARKUP' ? record.supplyPrice : record.salePrice;
        if (baseValue === undefined || baseValue === null)
            return undefined;
        const base = Number(baseValue);
        if (!Number.isFinite(base))
            return undefined;
        const sign = priceMode === 'CURRENT_ADJUST' && priceAdjustDirection === 'DOWN' ? -1 : 1;
        const next = priceNumberType === 'PERCENT'
            ? base * (1 + sign * priceAmount / 100)
            : base + sign * priceAmount;
        return applyTailRule(next, tailRule);
    };
    const pricePreviewRows = useMemo(() => priceRows.map((row) => {
        const nextSalePrice = calculatePreviewPrice(row);
        let priceError;
        if (nextSalePrice === undefined) {
            priceError = priceMode === 'CURRENT_ADJUST' && (row.salePrice === undefined || row.salePrice === null)
                ? '当前售价为空'
                : '待输入';
        }
        else if (nextSalePrice < 0) {
            priceError = '价格不能小于0';
        }
        return { ...row, nextSalePrice, priceError };
    }), [priceRows, priceMode, priceAdjustDirection, priceNumberType, priceAmount, tailRule]);
    const openPriceModal = (rows) => {
        if (rows.length === 0) {
            message.warning('请先选择 SKU');
            return;
        }
        setPriceRows(rows);
        setPriceReason('');
        setPriceModalOpen(true);
    };
    const submitSalePrice = async () => {
        if (priceAmount === undefined || priceAmount === null) {
            message.warning('请输入调价值');
            return;
        }
        const invalid = pricePreviewRows.find((row) => row.nextSalePrice === undefined || row.nextSalePrice < 0);
        if (invalid) {
            message.warning('存在无法计算的新售价，请检查调价方式和选中的 SKU');
            return;
        }
        const belowSupply = pricePreviewRows.some((row) => row.nextSalePrice !== undefined
            && row.supplyPrice !== undefined
            && row.supplyPrice !== null
            && row.nextSalePrice < Number(row.supplyPrice));
        const items = pricePreviewRows
            .filter((row) => row.skuId && row.nextSalePrice !== undefined)
            .map((row) => ({ skuId: row.skuId, salePrice: row.nextSalePrice }));
        const run = async () => {
            const ok = resultOk(await batchUpdateDistributionSkuSalePrices(items, priceReason.trim()), '销售价已更新');
            if (ok) {
                setPriceModalOpen(false);
                setPriceRows([]);
                reload();
            }
        };
        if (belowSupply) {
            modal.confirm({
                title: '存在低于供货价的售价',
                content: '低于供货价可能导致亏损或审核风险，确认继续保存本次调价？',
                okText: '确认保存',
                cancelText: '返回修改',
                okButtonProps: { danger: true },
                onOk: run,
            });
            return;
        }
        await run();
    };
    const skuColumns = (siblingRows = []) => [
        {
            title: 'SKU图',
            dataIndex: 'skuImageUrl',
            width: 72,
            render: (url) => url ? _jsx(Image, { width: 40, height: 40, src: resolveResourceUrl(url), style: { objectFit: 'cover' } }) : '--',
        },
        { title: '系统SKU', dataIndex: 'systemSkuCode', width: 150 },
        { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 140 },
        {
            title: 'SKU规格',
            width: 220,
            render: (_, record) => buildSkuSpecText(record, siblingRows) || '--',
        },
        {
            title: '尺寸重量',
            width: 220,
            render: (_, record) => buildSkuDimensionText(record) || '--',
        },
        { title: '供货价', dataIndex: 'supplyPrice', width: 90, render: (value) => formatAmount(value) },
        { title: '销售价', dataIndex: 'salePrice', width: 90, render: (value) => formatAmount(value) },
        { title: '币种', dataIndex: 'currencyCode', width: 80 },
        {
            title: '可售库存',
            dataIndex: 'availableStock',
            width: 90,
            render: (_, record) => renderInventoryNumber(record.availableStock),
        },
        {
            title: '仓库数',
            dataIndex: 'warehouseCount',
            width: 80,
            render: (_, record) => renderInventoryNumber(record.warehouseCount),
        },
        {
            title: '库存状态',
            width: 90,
            render: (_, record) => renderInventoryStatus(record),
        },
        {
            title: '销售状态',
            dataIndex: 'skuStatus',
            width: 100,
            render: (value) => renderSalesStatusTag(String(value || '')),
        },
        {
            title: '管控',
            dataIndex: 'controlStatus',
            width: 90,
            render: (_, record) => renderSkuControlStatusTag(record),
        },
        {
            title: '操作',
            width: 130,
            render: (_, record) => {
                const flow = statusFlowMap[record.skuStatus || ''];
                const parentDisabled = record.spuControlStatus === 'DISABLED';
                const skuDisabled = record.controlStatus === 'DISABLED';
                const items = [
                    ...(access.hasPerms('product:distribution:price')
                        ? [{ key: 'price', label: '调价' }]
                        : []),
                    ...(flow && !parentDisabled && !skuDisabled
                        ? [{ key: `status:${flow.targetStatus}`, label: flow.label }]
                        : []),
                    ...(!parentDisabled && !skuDisabled
                        ? [{ key: 'disable', label: '停用', danger: true }]
                        : []),
                    ...(skuDisabled
                        ? [{ key: 'recover', label: '恢复' }]
                        : []),
                    ...(parentDisabled && !skuDisabled
                        ? [{ key: 'parent-disabled', label: 'SPU已停用', disabled: true }]
                        : []),
                ];
                return (_jsx(Dropdown, { menu: {
                        items,
                        onClick: ({ key }) => {
                            if (key === 'disable') {
                                openControlModal('SKU', compactIds([record], 'SKU'), 'DISABLED');
                            }
                            else if (key === 'recover') {
                                openControlModal('SKU', compactIds([record], 'SKU'), 'NORMAL');
                            }
                            else if (String(key).startsWith('status:')) {
                                executeSalesStatus('SKU', compactIds([record], 'SKU'), String(key).slice(7), flow?.label || '调整状态');
                            }
                        },
                    }, children: _jsxs(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:distribution:status'), children: ["\u72B6\u6001 ", _jsx(DownOutlined, {})] }) }));
            },
        },
    ];
    const columns = [
        Table.SELECTION_COLUMN,
        Table.EXPAND_COLUMN,
        {
            title: '商品图',
            dataIndex: 'mainImageUrl',
            search: false,
            width: 72,
            render: (_, record) => record.mainImageUrl ? (_jsx(Image, { width: 44, height: 44, src: resolveResourceUrl(record.mainImageUrl), style: { objectFit: 'cover' } })) : '--',
        },
        { title: '系统SPU', dataIndex: 'systemSpuCode', width: 160 },
        { title: '客户SPU', dataIndex: 'sellerSpuCode', width: 150 },
        {
            title: '商品标题',
            dataIndex: 'productName',
            width: 260,
            render: (_, record) => (_jsxs("div", { children: [_jsx("div", { children: record.productName || '--' }), _jsx("div", { className: styles.mutedText, children: record.productNameEn || '--' })] })),
        },
        {
            title: '卖家',
            dataIndex: 'sellerId',
            valueType: 'select',
            fieldProps: { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions },
            render: (_, record) => record.sellerName || '--',
            width: 180,
        },
        {
            title: '类目',
            dataIndex: 'categoryId',
            valueType: 'treeSelect',
            fieldProps: { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: categoryTreeData, treeDefaultExpandAll: true },
            render: (_, record) => record.categoryName || '--',
            width: 160,
        },
        { title: 'SKU数', dataIndex: 'skuCount', search: false, width: 80 },
        {
            title: '供货价区间',
            search: false,
            width: 130,
            render: (_, record) => formatPriceRange(record.supplyPriceMin, record.supplyPriceMax),
        },
        {
            title: '销售价区间',
            search: false,
            width: 130,
            render: (_, record) => formatPriceRange(record.salePriceMin, record.salePriceMax),
        },
        { title: '币种', dataIndex: 'currencySummary', search: false, width: 90 },
        {
            title: '仓库类型',
            dataIndex: 'warehouseKindSummary',
            search: false,
            width: 100,
            render: (_, record) => renderWarehouseKindTag(record.warehouseKindSummary),
        },
        {
            title: '总可售库存',
            dataIndex: 'availableStock',
            search: false,
            width: 100,
            render: (_, record) => renderInventoryNumber(record.availableStock),
        },
        {
            title: '仓库数',
            dataIndex: 'warehouseCount',
            search: false,
            width: 80,
            render: (_, record) => renderInventoryNumber(record.warehouseCount),
        },
        {
            title: '库存状态',
            search: false,
            width: 90,
            render: (_, record) => renderInventoryStatus(record),
        },
        {
            title: '销售状态',
            dataIndex: 'spuStatus',
            search: false,
            valueEnum: salesStatusValueEnum,
            width: 100,
            render: (_, record) => renderSalesStatusTag(record.spuStatus),
        },
        {
            title: '管控',
            dataIndex: 'controlStatus',
            search: false,
            width: 90,
            render: (_, record) => renderSpuControlStatusTag(record),
        },
        {
            title: '来源',
            dataIndex: 'sourceType',
            valueEnum: sourceTypeValueEnum,
            search: false,
            width: 140,
        },
        { title: '更新时间', dataIndex: 'updateTime', search: false, width: 170 },
        {
            title: '操作',
            valueType: 'option',
            width: 190,
            fixed: 'right',
            render: (_, record) => {
                const flow = statusFlowMap[record.spuStatus || ''];
                const disabled = record.controlStatus === 'DISABLED';
                const items = [
                    ...(flow && !disabled ? [{ key: `status:${flow.targetStatus}`, label: flow.label }] : []),
                    ...(!disabled ? [{ key: 'disable', label: '停用', danger: true }] : []),
                    ...(disabled ? [{ key: 'recover', label: '恢复' }] : []),
                ];
                return [
                    _jsx(Button, { type: "link", size: "small", hidden: !canViewDistributionDetail, onClick: () => openDetail(record), children: "\u67E5\u770B" }, "view"),
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:distribution:edit'), onClick: () => openEdit(record), children: "\u7F16\u8F91" }, "edit"),
                    _jsx(Dropdown, { menu: {
                            items,
                            onClick: ({ key }) => {
                                if (key === 'disable') {
                                    openControlModal('SPU', compactIds([record], 'SPU'), 'DISABLED');
                                }
                                else if (key === 'recover') {
                                    openControlModal('SPU', compactIds([record], 'SPU'), 'NORMAL');
                                }
                                else if (String(key).startsWith('status:')) {
                                    executeSalesStatus('SPU', compactIds([record], 'SPU'), String(key).slice(7), flow?.label || '调整状态');
                                }
                            },
                        }, children: _jsxs(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:distribution:status'), children: ["\u66F4\u591A ", _jsx(DownOutlined, {})] }) }, "more"),
                ];
            },
        },
    ];
    const skuListColumns = [
        {
            title: 'SKU图',
            dataIndex: 'skuImageUrl',
            search: false,
            width: 72,
            render: (_, record) => record.skuImageUrl ? (_jsx(Image, { width: 44, height: 44, src: resolveResourceUrl(record.skuImageUrl), style: { objectFit: 'cover' } })) : '--',
        },
        { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160 },
        { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 150 },
        {
            title: 'SKU规格',
            search: false,
            width: 220,
            render: (_, record) => buildSkuSpecText(record) || '--',
        },
        {
            title: '商品标题',
            dataIndex: 'productName',
            width: 260,
            render: (_, record) => (_jsxs("div", { children: [_jsx("div", { children: record.productName || '--' }), _jsx("div", { className: styles.mutedText, children: record.productNameEn || '--' })] })),
        },
        { title: '系统SPU', dataIndex: 'systemSpuCode', width: 160 },
        { title: '客户SPU', dataIndex: 'sellerSpuCode', width: 150 },
        {
            title: '卖家',
            dataIndex: 'sellerId',
            valueType: 'select',
            fieldProps: { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions },
            render: (_, record) => record.sellerName || '--',
            width: 180,
        },
        {
            title: '类目',
            dataIndex: 'categoryId',
            valueType: 'treeSelect',
            fieldProps: { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: categoryTreeData, treeDefaultExpandAll: true },
            render: (_, record) => record.categoryName || '--',
            width: 160,
        },
        {
            title: '尺寸重量',
            search: false,
            width: 220,
            render: (_, record) => buildSkuDimensionText(record) || '--',
        },
        { title: '供货价', dataIndex: 'supplyPrice', search: false, width: 90, render: (value) => formatAmount(value) },
        { title: '销售价', dataIndex: 'salePrice', search: false, width: 90, render: (value) => formatAmount(value) },
        { title: '币种', dataIndex: 'currencyCode', search: false, width: 80 },
        {
            title: '仓库类型',
            dataIndex: 'warehouseKindSummary',
            search: false,
            width: 100,
            render: (_, record) => renderWarehouseKindTag(record.warehouseKindSummary),
        },
        {
            title: '可售库存',
            dataIndex: 'availableStock',
            search: false,
            width: 90,
            render: (_, record) => renderInventoryNumber(record.availableStock),
        },
        {
            title: '仓库数',
            dataIndex: 'warehouseCount',
            search: false,
            width: 80,
            render: (_, record) => renderInventoryNumber(record.warehouseCount),
        },
        {
            title: '库存状态',
            search: false,
            width: 90,
            render: (_, record) => renderInventoryStatus(record),
        },
        {
            title: '销售状态',
            dataIndex: 'skuStatus',
            search: false,
            valueEnum: salesStatusValueEnum,
            width: 100,
            render: (_, record) => renderSalesStatusTag(record.skuStatus),
        },
        {
            title: '管控',
            dataIndex: 'controlStatus',
            search: false,
            width: 90,
            render: (_, record) => renderSkuControlStatusTag(record),
        },
        { title: '更新时间', dataIndex: 'updateTime', search: false, width: 170 },
        {
            title: '操作',
            valueType: 'option',
            width: 210,
            fixed: 'right',
            render: (_, record) => {
                const flow = statusFlowMap[record.skuStatus || ''];
                const parentDisabled = record.spuControlStatus === 'DISABLED';
                const skuDisabled = record.controlStatus === 'DISABLED';
                const items = [
                    ...(access.hasPerms('product:distribution:price')
                        ? [{ key: 'price', label: '调价' }]
                        : []),
                    ...(flow && !parentDisabled && !skuDisabled
                        ? [{ key: `status:${flow.targetStatus}`, label: flow.label }]
                        : []),
                    ...(!parentDisabled && !skuDisabled
                        ? [{ key: 'disable', label: '停用', danger: true }]
                        : []),
                    ...(skuDisabled ? [{ key: 'recover', label: '恢复' }] : []),
                    ...(parentDisabled && !skuDisabled
                        ? [{ key: 'parent-disabled', label: 'SPU已停用', disabled: true }]
                        : []),
                ];
                return [
                    _jsx(Button, { type: "link", size: "small", hidden: !canViewDistributionDetail, onClick: () => openDetail({ spuId: record.spuId }), children: "\u67E5\u770BSPU" }, "view"),
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:distribution:edit'), onClick: () => openSkuEdit(record), children: "\u7F16\u8F91\u5546\u54C1" }, "edit"),
                    _jsx(Dropdown, { menu: {
                            items,
                            onClick: ({ key }) => {
                                if (key === 'price') {
                                    openPriceModal([record]);
                                }
                                else if (key === 'disable') {
                                    openControlModal('SKU', compactIds([record], 'SKU'), 'DISABLED');
                                }
                                else if (key === 'recover') {
                                    openControlModal('SKU', compactIds([record], 'SKU'), 'NORMAL');
                                }
                                else if (String(key).startsWith('status:')) {
                                    executeSalesStatus('SKU', compactIds([record], 'SKU'), String(key).slice(7), flow?.label || '调整状态');
                                }
                            },
                        }, children: _jsxs(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:distribution:status'), children: ["\u66F4\u591A ", _jsx(DownOutlined, {})] }) }, "status"),
                ];
            },
        },
    ];
    const viewModeSwitch = (_jsx(Radio.Group, { buttonStyle: "solid", value: viewMode, onChange: (event) => setViewMode(event.target.value), children: viewModeOptions.map((option) => (_jsx(Radio.Button, { value: option.value, children: option.label }, option.value))) }, "view-mode"));
    const headerTitle = (_jsx(Tabs, { activeKey: statusTab, items: statusTabItems, tabBarStyle: { marginBottom: 0 }, onChange: (key) => setStatusTab(key) }));
    const renderBatchActions = () => {
        const actions = [viewModeSwitch];
        if (statusTab === 'DISABLED') {
            actions.push(_jsx(Button, { disabled: !selectedIds.length, hidden: !access.hasPerms('product:distribution:status'), onClick: () => openControlModal(viewMode, selectedIds, 'NORMAL'), children: "\u6062\u590D" }, "recover"));
        }
        else if (currentFlow) {
            actions.push(_jsx(Button, { disabled: !selectedIds.length, hidden: !access.hasPerms('product:distribution:status'), onClick: () => executeSalesStatus(viewMode, selectedIds, currentFlow.targetStatus, currentFlow.batchLabel), children: currentFlow.batchLabel }, "flow"));
            actions.push(_jsx(Button, { danger: true, disabled: !selectedIds.length, hidden: !access.hasPerms('product:distribution:status'), onClick: () => openControlModal(viewMode, selectedIds, 'DISABLED'), children: "\u6279\u91CF\u505C\u7528" }, "disable"));
        }
        if (viewMode === 'SKU') {
            actions.push(_jsx(Button, { disabled: !selectedSkuRows.length, hidden: !access.hasPerms('product:distribution:price'), onClick: () => openPriceModal(selectedSkuRows), children: "\u8C03\u6574\u552E\u4EF7" }, "price"));
        }
        actions.push(_jsx(Button, { icon: _jsx(HistoryOutlined, {}), hidden: !access.hasPerms('product:distribution:log'), onClick: () => setOperationLogOpen(true), children: "\u64CD\u4F5C\u65E5\u5FD7" }, "log"));
        actions.push(_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms('product:distribution:add'), onClick: () => history.push('/product/distribution/create'), children: "\u65B0\u589E\u5546\u54C1" }, "add"));
        return actions;
    };
    return (_jsxs(PageContainer, { title: false, children: [viewMode === 'SPU' ? (_jsx(ProTable, { actionRef: actionRef, rowKey: "spuId", columns: columns, scroll: getProTableScroll(SPU_TABLE_SCROLL_X), tableLayout: "fixed", search: getPersistedProTableSearch({ labelWidth: 90 }, 'product-distribution-spu'), params: { statusTab }, headerTitle: headerTitle, rowSelection: {
                    columnWidth: TABLE_SELECTION_COLUMN_WIDTH,
                    selectedRowKeys: selectedSpuRows.map((row) => row.spuId).filter(Boolean),
                    onChange: (_, rows) => setSelectedSpuRows(rows),
                }, expandable: {
                    expandedRowRender: (record) => (_jsx(Table, { rowKey: "skuId", size: "small", columns: skuColumns(record.skus || []), dataSource: record.skus || [], pagination: false, scroll: { x: SKU_DETAIL_TABLE_SCROLL_X }, tableLayout: "fixed" })),
                }, request: async ({ current, pageSize, ...params }) => {
                    const { statusTab: activeStatusTab, ...queryParams } = params;
                    delete queryParams.spuStatus;
                    delete queryParams.controlStatus;
                    const resp = await getDistributionProductList({
                        ...queryParams,
                        ...buildStatusQuery(activeStatusTab),
                        pageNum: current,
                        pageSize,
                    });
                    return {
                        data: resp.rows || [],
                        total: resp.total || 0,
                        success: resp.code === 200,
                    };
                }, toolBarRender: renderBatchActions }, "spu-view")) : (_jsx(ProTable, { actionRef: actionRef, rowKey: "skuId", columns: skuListColumns, scroll: getProTableScroll(SKU_TABLE_SCROLL_X), tableLayout: "fixed", search: getPersistedProTableSearch({ labelWidth: 90 }, 'product-distribution-sku'), params: { statusTab }, headerTitle: headerTitle, rowSelection: {
                    columnWidth: TABLE_SELECTION_COLUMN_WIDTH,
                    selectedRowKeys: selectedSkuRows.map((row) => row.skuId).filter(Boolean),
                    onChange: (_, rows) => setSelectedSkuRows(rows),
                }, request: async ({ current, pageSize, ...params }) => {
                    const { statusTab: activeStatusTab, ...queryParams } = params;
                    delete queryParams.skuStatus;
                    delete queryParams.controlStatus;
                    const resp = await getDistributionSkuList({
                        ...queryParams,
                        ...buildSkuStatusQuery(activeStatusTab),
                        pageNum: current,
                        pageSize,
                    });
                    return {
                        data: resp.rows || [],
                        total: resp.total || 0,
                        success: resp.code === 200,
                    };
                }, toolBarRender: renderBatchActions }, "sku-view")), _jsx(ProductDetailDrawer, { open: detailOpen, product: current, onClose: () => setDetailOpen(false) }), _jsx(ProductDistributionOperationLogDrawer, { open: operationLogOpen, onOpenChange: setOperationLogOpen }), _jsx(Modal, { title: controlModal.targetStatus === 'DISABLED' ? '停用商品' : '恢复商品', open: controlModal.open, okText: controlModal.targetStatus === 'DISABLED' ? '确认停用' : '确认恢复', cancelText: "\u53D6\u6D88", okButtonProps: { danger: controlModal.targetStatus === 'DISABLED' }, onOk: submitControlStatus, onCancel: () => setControlModal({ ...controlModal, open: false }), children: controlModal.targetStatus === 'DISABLED' ? (_jsx(Input.TextArea, { rows: 4, value: controlReason, maxLength: 500, showCount: true, placeholder: "\u8BF7\u8F93\u5165\u505C\u7528\u539F\u56E0", onChange: (event) => setControlReason(event.target.value) })) : (_jsx(Alert, { type: "info", showIcon: true, title: `确认恢复 ${controlModal.ids.length} 个${controlModal.ownerType}？恢复后仍保持原销售状态。` })) }), _jsx(Modal, { title: "\u8C03\u6574\u552E\u4EF7", open: priceModalOpen, width: 1040, okText: "\u4FDD\u5B58\u8C03\u4EF7", cancelText: "\u53D6\u6D88", onOk: submitSalePrice, onCancel: () => setPriceModalOpen(false), children: _jsxs(Space, { orientation: "vertical", size: 14, style: { width: '100%' }, children: [_jsx(Radio.Group, { buttonStyle: "solid", value: priceMode, options: priceModeOptions, onChange: (event) => setPriceMode(event.target.value) }), _jsxs(Space, { wrap: true, children: [priceMode === 'CURRENT_ADJUST' ? (_jsx(Select, { value: priceAdjustDirection, style: { width: 96 }, options: [
                                        { label: '上调', value: 'UP' },
                                        { label: '下调', value: 'DOWN' },
                                    ], onChange: (value) => setPriceAdjustDirection(value) })) : null, priceMode !== 'FIXED' ? (_jsx(Select, { value: priceNumberType, style: { width: 110 }, options: [
                                        { label: '百分比', value: 'PERCENT' },
                                        { label: '金额', value: 'AMOUNT' },
                                    ], onChange: (value) => setPriceNumberType(value) })) : null, _jsx(InputNumber, { min: 0, precision: 4, value: priceAmount, style: { width: 180 }, suffix: priceMode === 'FIXED' || priceNumberType === 'AMOUNT' ? priceCurrencyText : '%', placeholder: priceMode === 'FIXED' ? '输入统一售价' : '输入调价值', onChange: (value) => setPriceAmount(value === null ? undefined : Number(value)) }), _jsx(Select, { value: tailRule, style: { width: 140 }, options: tailRuleOptions, onChange: (value) => setTailRule(value) }), _jsx(Input, { value: priceReason, style: { width: 260 }, placeholder: "\u8C03\u4EF7\u539F\u56E0\uFF08\u53EF\u9009\uFF09", onChange: (event) => setPriceReason(event.target.value) })] }), priceCurrencyText === '多币种' ? (_jsx(Alert, { type: "warning", showIcon: true, title: "\u5F53\u524D\u9009\u62E9\u5305\u542B\u591A\u5E01\u79CD SKU\uFF0C\u8BF7\u786E\u8BA4\u8C03\u4EF7\u89C4\u5219\u5BF9\u6240\u6709\u5E01\u79CD\u90FD\u9002\u7528\u3002" })) : null, _jsx(Table, { rowKey: "skuId", size: "small", pagination: false, dataSource: pricePreviewRows, scroll: { x: 980, y: 320 }, columns: [
                                { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160 },
                                { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 140 },
                                { title: '供货价', dataIndex: 'supplyPrice', width: 100, render: (value) => formatAmount(value) },
                                { title: '原销售价', dataIndex: 'salePrice', width: 100, render: (value) => formatAmount(value) },
                                {
                                    title: '新销售价',
                                    dataIndex: 'nextSalePrice',
                                    width: 120,
                                    render: (_, record) => record.priceError ? (_jsx(Typography.Text, { type: "secondary", children: record.priceError })) : (_jsx(Typography.Text, { type: record.nextSalePrice !== undefined
                                            && record.supplyPrice !== undefined
                                            && record.nextSalePrice < Number(record.supplyPrice)
                                            ? 'warning'
                                            : undefined, children: formatAmount(record.nextSalePrice) })),
                                },
                                { title: '币种', dataIndex: 'currencyCode', width: 90 },
                                {
                                    title: '规格',
                                    width: 220,
                                    render: (_, record) => buildSkuSpecText(record) || '--',
                                },
                            ] })] }) })] }));
}
