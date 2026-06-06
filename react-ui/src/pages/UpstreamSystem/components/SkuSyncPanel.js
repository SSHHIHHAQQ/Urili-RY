import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { SearchOutlined, SyncOutlined } from '@ant-design/icons';
import { ProTable, } from '@ant-design/pro-components';
import { Button, Input, Popconfirm, Select, Typography } from 'antd';
import { useEffect, useState, } from 'react';
import { deleteSkuPairing, getSkuSyncList, getSkuSyncState, syncUpstreamSku, } from '@/services/integration/upstreamSystem';
import { message } from '@/utils/feedback';
import { getProTablePagination, getProTableScroll, } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { skuPairingStatusOptions, skuSearchFieldOptions, skuSyncItemStatusOptions, } from '../constants';
import { resultOk, statusTag } from '../helpers';
import styles from '../style.module.css';
export default function SkuSyncPanel({ access, actionRef, onSynced, selectedCode, setPairingModal, }) {
    const [syncing, setSyncing] = useState(false);
    const [syncState, setSyncState] = useState();
    const [searchField, setSearchField] = useState('all');
    const [searchInput, setSearchInput] = useState('');
    const [keyword, setKeyword] = useState('');
    const [pairingStatus, setPairingStatus] = useState('');
    const [syncStatus, setSyncStatus] = useState('');
    const loadSyncState = async () => {
        if (!selectedCode) {
            setSyncState(undefined);
            return;
        }
        const resp = await getSkuSyncState(selectedCode);
        if (resp.code === 200) {
            setSyncState(resp.data);
        }
    };
    useEffect(() => {
        loadSyncState();
    }, [selectedCode]);
    const skuColumns = [
        {
            title: '领星 masterSku',
            dataIndex: 'masterSku',
            width: 180,
            copyable: true,
        },
        {
            title: '领星产品名',
            dataIndex: 'masterProductName',
            width: 240,
            ellipsis: true,
        },
        {
            title: '同步状态',
            dataIndex: 'status',
            width: 100,
            render: (_, record) => statusTag(record.status),
        },
        {
            title: '配对状态',
            dataIndex: 'pairingStatus',
            width: 100,
            render: (_, record) => statusTag(record.pairingStatus),
        },
        { title: '系统SKU', dataIndex: 'systemSku', width: 160 },
        {
            title: '系统SKU名称',
            dataIndex: 'systemSkuName',
            width: 200,
            ellipsis: true,
        },
        {
            title: '客户名称',
            dataIndex: 'customerName',
            width: 160,
            ellipsis: true,
        },
        { title: '最近发现', dataIndex: 'lastSeenTime', width: 170 },
        {
            title: '操作',
            valueType: 'option',
            width: 140,
            render: (_, record) => record.skuPairingId
                ? [
                    _jsx(Popconfirm, { title: "\u786E\u8BA4\u89E3\u9664SKU\u914D\u5BF9\uFF1F", onConfirm: async () => {
                            if (!record.skuPairingId)
                                return;
                            const ok = resultOk(await deleteSkuPairing(record.skuPairingId), '已解除配对');
                            if (ok)
                                actionRef.current?.reload();
                        }, children: _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('integration:upstream:pair'), children: "\u89E3\u9664" }) }, "unpair"),
                ]
                : [
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('integration:upstream:pair'), onClick: () => setPairingModal({ open: true, type: 'sku', row: record }), children: "\u914D\u5BF9" }, "pair"),
                ],
        },
    ];
    const triggerSkuSync = async () => {
        setSyncing(true);
        const hide = message.loading('正在同步SKU');
        const resp = await syncUpstreamSku(selectedCode);
        hide();
        setSyncing(false);
        if (resp.code === 200) {
            message.success(`SKU同步完成：${resp.data?.skuCount || 0}`);
            await loadSyncState();
            actionRef.current?.reload();
            onSynced?.();
        }
        else {
            message.error(resp.msg);
            await loadSyncState();
        }
    };
    return (_jsxs("div", { className: styles.skuPanel, children: [_jsxs("div", { className: styles.skuToolbar, children: [_jsxs("div", { className: styles.skuState, children: [_jsx(Typography.Text, { type: "secondary", children: "\u540C\u6B65\u72B6\u6001" }), statusTag(syncState?.status || 'NEVER'), _jsxs(Typography.Text, { children: ["\u4E0A\u6B21\u6210\u529F ", syncState?.lastSuccessTime || '-'] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u4E0B\u6B21\u540C\u6B65 ", syncState?.nextSyncTime || '-'] }), syncState?.lastErrorMessage ? (_jsxs(Typography.Text, { type: "danger", children: ["\u9519\u8BEF\uFF1A", syncState.lastErrorMessage] })) : null] }), _jsx(Button, { type: "primary", icon: _jsx(SyncOutlined, {}), loading: syncing, hidden: !access.hasPerms('integration:upstream:sync'), onClick: triggerSkuSync, children: "\u540C\u6B65SKU" })] }), _jsxs("div", { className: styles.skuFilters, children: [_jsx(Select, { ...SEARCHABLE_SELECT_PROPS, style: { width: 160 }, options: skuSearchFieldOptions, value: searchField, onChange: setSearchField }), _jsx(Input.Search, { allowClear: true, enterButton: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u540C\u6B65\u6E05\u5355", style: { width: 260 }, value: searchInput, onChange: (event) => {
                            setSearchInput(event.target.value);
                            if (!event.target.value) {
                                setKeyword('');
                            }
                        }, onSearch: (value) => setKeyword(value.trim()) }), _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, style: { width: 150 }, options: skuPairingStatusOptions, value: pairingStatus, onChange: setPairingStatus }), _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, style: { width: 150 }, options: skuSyncItemStatusOptions, value: syncStatus, onChange: setSyncStatus })] }), _jsx(ProTable, { actionRef: actionRef, className: `${styles.fillTable} upstream-fill-table`, rowKey: "masterSku", columns: skuColumns, params: {
                    field: searchField,
                    keyword,
                    pairingStatus,
                    syncStatus,
                    selectedCode,
                }, request: async (params) => {
                    const resp = await getSkuSyncList(selectedCode, {
                        pageNum: params.current,
                        pageSize: params.pageSize,
                        field: searchField === 'all' ? undefined : searchField,
                        keyword,
                        pairingStatus: pairingStatus || undefined,
                        status: syncStatus || undefined,
                    });
                    return {
                        data: resp.rows || [],
                        total: resp.total || 0,
                        success: resp.code === 200,
                    };
                }, pagination: getProTablePagination(10), search: false, options: false, scroll: getProTableScroll(1400), toolBarRender: false })] }));
}
