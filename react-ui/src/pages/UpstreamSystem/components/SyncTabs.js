import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ProTable, } from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tabs, Tag, Typography } from 'antd';
import { deleteLogisticsChannelPairing, deleteWarehousePairing, getLogisticsChannelPairings, getLogisticsChannelSyncList, getRequestLogList, getWarehousePairings, getWarehouseSyncList, } from '@/services/integration/upstreamSystem';
import { getProTablePagination, getProTableScroll, } from '@/utils/proTableSearch';
import { resultOk, statusTag } from '../helpers';
import styles from '../style.module.css';
import SkuSyncPanel from './SkuSyncPanel';
export default function SyncTabs({ access, logActionRef, logisticsActionRef, onSkuSynced, selectedConnection, setPairingModal, skuActionRef, warehouseActionRef, }) {
    const selectedCode = selectedConnection.connectionCode;
    const warehouseColumns = [
        { title: '领星仓库代码', dataIndex: 'warehouseCode', width: 150 },
        { title: '领星仓库名称', dataIndex: 'warehouseName', width: 180 },
        { title: '国家/地区', dataIndex: 'countryCode', width: 100 },
        {
            title: '同步状态',
            dataIndex: 'status',
            width: 110,
            render: (_, record) => statusTag(record.status),
        },
        {
            title: '系统仓库代码',
            dataIndex: 'systemWarehouseCode',
            width: 150,
            search: false,
        },
        {
            title: '系统仓库名称',
            dataIndex: 'systemWarehouseName',
            width: 180,
            search: false,
        },
        {
            title: '操作',
            valueType: 'option',
            width: 160,
            render: (_, record) => record.warehousePairingId
                ? [
                    _jsx(Popconfirm, { title: "\u786E\u8BA4\u89E3\u9664\u4ED3\u5E93\u914D\u5BF9\uFF1F", onConfirm: async () => {
                            if (!record.warehousePairingId)
                                return;
                            const ok = resultOk(await deleteWarehousePairing(record.warehousePairingId), '已解除配对');
                            if (ok)
                                warehouseActionRef.current?.reload();
                        }, children: _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('integration:upstream:pair'), children: "\u89E3\u9664" }) }, "unpair"),
                ]
                : [
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('integration:upstream:pair'), onClick: () => setPairingModal({
                            open: true,
                            type: 'warehouse',
                            row: record,
                        }), children: "\u914D\u5BF9" }, "pair"),
                ],
        },
    ];
    const logisticsColumns = [
        { title: '领星渠道代码', dataIndex: 'channelCode', width: 150 },
        { title: '领星渠道名称', dataIndex: 'channelName', width: 200 },
        {
            title: '涉及仓库',
            dataIndex: 'warehouseCodes',
            width: 200,
            search: false,
        },
        {
            title: '系统渠道',
            dataIndex: 'pairings',
            search: false,
            render: (_, record) => (_jsx(Space, { wrap: true, children: record.pairings.length === 0 ? (_jsx(Typography.Text, { type: "secondary", children: "\u672A\u914D\u5BF9" })) : (record.pairings.map((pairing) => (_jsx(Popconfirm, { title: "\u786E\u8BA4\u89E3\u9664\u7269\u6D41\u6E20\u9053\u914D\u5BF9\uFF1F", onConfirm: async () => {
                        const ok = resultOk(await deleteLogisticsChannelPairing(pairing.logisticsChannelPairingId), '已解除配对');
                        if (ok)
                            logisticsActionRef.current?.reload();
                    }, children: _jsxs(Tag, { color: "blue", children: [pairing.systemChannelCode, " / ", pairing.systemChannelName] }) }, pairing.logisticsChannelPairingId)))) })),
        },
        {
            title: '操作',
            valueType: 'option',
            width: 120,
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('integration:upstream:pair'), onClick: () => setPairingModal({ open: true, type: 'logistics', row: record }), children: "\u914D\u5BF9" }, "pair"),
            ],
        },
    ];
    const logColumns = [
        { title: '时间', dataIndex: 'createTime', width: 170, search: false },
        { title: '操作', dataIndex: 'operation', width: 150 },
        {
            title: '结果',
            dataIndex: 'status',
            width: 100,
            render: (_, record) => (_jsx(Tag, { color: record.status === 'SUCCESS' ? 'green' : 'red', children: record.status })),
        },
        { title: '耗时(ms)', dataIndex: 'durationMs', width: 100, search: false },
        {
            title: '错误码',
            dataIndex: 'externalErrorCode',
            width: 130,
            search: false,
        },
        {
            title: '错误信息',
            dataIndex: 'externalErrorMessage',
            ellipsis: true,
            search: false,
        },
        {
            title: 'TraceId',
            dataIndex: 'traceId',
            width: 220,
            copyable: true,
            search: false,
        },
    ];
    return (_jsx("div", { className: `${styles.syncTabs} upstream-sync-tabs`, children: _jsx(Tabs, { items: [
                {
                    key: 'warehouse',
                    label: '领星仓库同步清单',
                    children: (_jsx("div", { className: styles.tablePane, children: _jsx(ProTable, { actionRef: warehouseActionRef, className: `${styles.fillTable} upstream-fill-table`, rowKey: (record) => `${selectedCode}:${record.warehouseCode}`, columns: warehouseColumns, search: false, options: false, toolBarRender: false, scroll: getProTableScroll(1100), params: { selectedCode }, request: async () => {
                                const requestCode = selectedCode;
                                const [syncResp, pairingResp] = await Promise.all([
                                    getWarehouseSyncList(requestCode),
                                    getWarehousePairings(requestCode),
                                ]);
                                const pairingMap = new Map((pairingResp.data || []).map((item) => [
                                    item.upstreamWarehouseCode,
                                    item,
                                ]));
                                const rows = (syncResp.data || []).map((item) => ({
                                    ...item,
                                    ...(pairingMap.get(item.warehouseCode) || {}),
                                }));
                                return { data: rows, success: syncResp.code === 200 };
                            }, pagination: getProTablePagination(10) }, `warehouse-${selectedCode}`) })),
                },
                {
                    key: 'logistics',
                    label: '领星物流渠道同步清单',
                    children: (_jsx("div", { className: styles.tablePane, children: _jsx(ProTable, { actionRef: logisticsActionRef, className: `${styles.fillTable} upstream-fill-table`, rowKey: (record) => `${selectedCode}:${record.channelCode}`, columns: logisticsColumns, search: false, options: false, toolBarRender: false, scroll: getProTableScroll(1000), params: { selectedCode }, request: async () => {
                                const requestCode = selectedCode;
                                const [syncResp, pairingResp] = await Promise.all([
                                    getLogisticsChannelSyncList(requestCode),
                                    getLogisticsChannelPairings(requestCode),
                                ]);
                                const groups = new Map();
                                (syncResp.data || []).forEach((item) => {
                                    const current = groups.get(item.channelCode);
                                    if (current) {
                                        current.warehouseCodes = Array.from(new Set(`${current.warehouseCodes},${item.warehouseCode}`.split(','))).join(',');
                                    }
                                    else {
                                        groups.set(item.channelCode, {
                                            ...item,
                                            warehouseCodes: item.warehouseCode,
                                            pairings: [],
                                        });
                                    }
                                });
                                const rows = Array.from(groups.values()).map((row) => ({
                                    ...row,
                                    pairings: (pairingResp.data || []).filter((pairing) => pairing.upstreamChannelCode === row.channelCode),
                                }));
                                return { data: rows, success: syncResp.code === 200 };
                            }, pagination: getProTablePagination(10) }, `logistics-${selectedCode}`) })),
                },
                {
                    key: 'sku',
                    label: '领星SKU同步清单',
                    children: (_jsx(SkuSyncPanel, { access: access, actionRef: skuActionRef, onSynced: onSkuSynced, selectedCode: selectedCode, setPairingModal: setPairingModal })),
                },
                {
                    key: 'logs',
                    label: '请求日志',
                    children: (_jsx(ProTable, { actionRef: logActionRef, className: `${styles.fillTable} upstream-fill-table`, rowKey: "requestLogId", columns: logColumns, params: { selectedCode }, request: async (params) => {
                            const requestCode = selectedCode;
                            const resp = await getRequestLogList(requestCode, params);
                            return {
                                data: resp.rows || [],
                                total: resp.total || 0,
                                success: resp.code === 200,
                            };
                        }, pagination: getProTablePagination(10), search: false, options: false, toolBarRender: false, scroll: getProTableScroll(1100) }, `logs-${selectedCode}`)),
                },
            ] }) }));
}
