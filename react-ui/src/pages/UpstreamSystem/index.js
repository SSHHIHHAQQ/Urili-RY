import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Empty } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { addLogisticsChannelPairing, addSkuPairing, addUpstreamConnection, addWarehousePairing, authorizeUpstreamConnection, getUpstreamConnectionList, syncUpstreamConnection, updateUpstreamConnection, updateUpstreamConnectionOrder, updateUpstreamCredentials, updateUpstreamStatus, } from '@/services/integration/upstreamSystem';
import { message } from '@/utils/feedback';
import ConnectionModal from './components/ConnectionModal';
import ConnectionSidebar from './components/ConnectionSidebar';
import ConnectionSummary from './components/ConnectionSummary';
import PairingModal from './components/PairingModal';
import SyncTabs from './components/SyncTabs';
import { resultOk } from './helpers';
import './style.css';
import styles from './style.module.css';
const connectionPageSize = 200;
export default function UpstreamSystemPage() {
    const access = useAccess();
    const warehouseActionRef = useRef(null);
    const logisticsActionRef = useRef(null);
    const skuActionRef = useRef(null);
    const logActionRef = useRef(null);
    const [connections, setConnections] = useState([]);
    const [loadingConnections, setLoadingConnections] = useState(false);
    const [selectedConnection, setSelectedConnection] = useState();
    const [connectionModal, setConnectionModal] = useState({ open: false, mode: 'create' });
    const [pairingModal, setPairingModal] = useState({
        open: false,
    });
    const fetchConnections = useCallback(async (preferredCode) => {
        setLoadingConnections(true);
        try {
            const resp = await getUpstreamConnectionList({
                pageNum: 1,
                pageSize: connectionPageSize,
            });
            if (resp.code !== 200) {
                message.error(resp.msg || '主仓接入加载失败');
                return [];
            }
            const rows = resp.rows || [];
            setConnections(rows);
            setSelectedConnection((current) => {
                const nextCode = preferredCode || current?.connectionCode;
                return rows.find((item) => item.connectionCode === nextCode) || rows[0];
            });
            return rows;
        }
        finally {
            setLoadingConnections(false);
        }
    }, []);
    useEffect(() => {
        fetchConnections();
    }, [fetchConnections]);
    const selectedCode = selectedConnection?.connectionCode || '';
    const reloadTabs = () => {
        warehouseActionRef.current?.reload();
        logisticsActionRef.current?.reload();
        skuActionRef.current?.reload();
        logActionRef.current?.reload();
    };
    const reloadCurrent = async (preferredCode) => {
        const targetCode = preferredCode || selectedCode;
        await fetchConnections(targetCode);
        if (!targetCode || targetCode === selectedCode) {
            reloadTabs();
        }
    };
    const handleAuthorize = async (record) => {
        const hide = message.loading('正在校验授权');
        const ok = resultOk(await authorizeUpstreamConnection(record.connectionCode), '授权校验通过');
        hide();
        if (ok) {
            await reloadCurrent(record.connectionCode);
        }
    };
    const handleSync = async (record) => {
        const hide = message.loading('正在同步');
        const resp = await syncUpstreamConnection(record.connectionCode);
        hide();
        if (resp.code === 200) {
            message.success(`同步完成：仓库 ${resp.data?.warehouseCount || 0}，渠道 ${resp.data?.logisticsChannelCount || 0}，SKU ${resp.data?.skuCount || 0}`);
            await reloadCurrent(record.connectionCode);
        }
        else {
            message.error(resp.msg);
        }
    };
    const handleToggleStatus = async (record) => {
        const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
        const ok = resultOk(await updateUpstreamStatus(record.connectionCode, nextStatus), '状态已更新');
        if (ok) {
            await reloadCurrent(record.connectionCode);
        }
    };
    const saveOrder = async (connectionCodes) => {
        const ok = resultOk(await updateUpstreamConnectionOrder(connectionCodes), '排序已保存');
        if (ok) {
            await fetchConnections(selectedCode);
        }
        return ok;
    };
    return (_jsxs(PageContainer, { children: [_jsxs("div", { className: styles.workspace, children: [_jsx(ConnectionSidebar, { access: access, connections: connections, loading: loadingConnections, onCreate: () => setConnectionModal({ open: true, mode: 'create' }), onSaveOrder: saveOrder, onSelect: setSelectedConnection, selectedCode: selectedCode }), selectedConnection ? (_jsxs("div", { className: styles.detailPane, children: [_jsx(ConnectionSummary, { access: access, connection: selectedConnection, onAuthorize: () => handleAuthorize(selectedConnection), onCredential: () => setConnectionModal({
                                    open: true,
                                    mode: 'credential',
                                    record: selectedConnection,
                                }), onEdit: () => setConnectionModal({
                                    open: true,
                                    mode: 'edit',
                                    record: selectedConnection,
                                }), onSync: () => handleSync(selectedConnection), onToggleStatus: () => handleToggleStatus(selectedConnection) }), _jsx(SyncTabs, { access: access, logActionRef: logActionRef, logisticsActionRef: logisticsActionRef, onSkuSynced: () => fetchConnections(selectedCode), selectedConnection: selectedConnection, setPairingModal: setPairingModal, skuActionRef: skuActionRef, warehouseActionRef: warehouseActionRef }, selectedCode)] })) : (_jsx("div", { className: styles.emptyPane, children: _jsx(Empty, { description: "\u6682\u65E0\u4E3B\u4ED3\u63A5\u5165" }) }))] }), _jsx(ConnectionModal, { mode: connectionModal.mode, open: connectionModal.open, record: connectionModal.record, onCancel: () => setConnectionModal({ open: false, mode: 'create' }), onSubmit: async (values) => {
                    const modalRecord = connectionModal.record;
                    if (connectionModal.mode !== 'create' &&
                        !modalRecord?.connectionCode) {
                        return false;
                    }
                    const hide = message.loading('正在保存');
                    let resp;
                    if (connectionModal.mode === 'create') {
                        resp = await addUpstreamConnection(values);
                    }
                    else if (connectionModal.mode === 'edit') {
                        if (!modalRecord)
                            return false;
                        resp = await updateUpstreamConnection(modalRecord.connectionCode, {
                            masterWarehouseName: values.masterWarehouseName,
                            settlementType: values.settlementType,
                            remark: values.remark,
                        });
                    }
                    else {
                        if (!modalRecord)
                            return false;
                        resp = await updateUpstreamCredentials(modalRecord.connectionCode, values);
                    }
                    hide();
                    const ok = resultOk(resp, '保存成功');
                    if (ok) {
                        setConnectionModal({ open: false, mode: 'create' });
                        await fetchConnections(modalRecord?.connectionCode || values.connectionCode);
                    }
                    return ok;
                } }), _jsx(PairingModal, { open: pairingModal.open, title: pairingModal.open && pairingModal.type === 'warehouse'
                    ? '仓库配对'
                    : pairingModal.open && pairingModal.type === 'logistics'
                        ? '物流渠道配对'
                        : 'SKU配对', upstreamLabel: pairingModal.open && pairingModal.type === 'warehouse'
                    ? '领星仓库'
                    : pairingModal.open && pairingModal.type === 'logistics'
                        ? '领星渠道'
                        : '领星masterSku', upstreamValue: pairingModal.open && pairingModal.type === 'warehouse'
                    ? `${pairingModal.row.warehouseCode} / ${pairingModal.row.warehouseName}`
                    : pairingModal.open && pairingModal.type === 'logistics'
                        ? `${pairingModal.row.channelCode} / ${pairingModal.row.channelName}`
                        : pairingModal.open
                            ? `${pairingModal.row.masterSku} / ${pairingModal.row.masterProductName}`
                            : '', codeLabel: pairingModal.open && pairingModal.type === 'sku'
                    ? '系统SKU'
                    : pairingModal.open && pairingModal.type === 'warehouse'
                        ? '系统仓库代码'
                        : '系统渠道代码', nameLabel: pairingModal.open && pairingModal.type === 'sku'
                    ? '系统SKU名称'
                    : pairingModal.open && pairingModal.type === 'warehouse'
                        ? '系统仓库名称'
                        : '系统渠道名称', codeName: pairingModal.open && pairingModal.type === 'sku'
                    ? 'systemSku'
                    : pairingModal.open && pairingModal.type === 'warehouse'
                        ? 'systemWarehouseCode'
                        : 'systemChannelCode', nameName: pairingModal.open && pairingModal.type === 'sku'
                    ? 'systemSkuName'
                    : pairingModal.open && pairingModal.type === 'warehouse'
                        ? 'systemWarehouseName'
                        : 'systemChannelName', showCustomerName: pairingModal.open && pairingModal.type === 'sku', onCancel: () => setPairingModal({ open: false }), onSubmit: async (values) => {
                    if (!pairingModal.open)
                        return false;
                    const hide = message.loading('正在配对');
                    const resp = pairingModal.type === 'warehouse'
                        ? await addWarehousePairing(selectedCode, {
                            ...values,
                            upstreamWarehouseCode: pairingModal.row.warehouseCode,
                        })
                        : pairingModal.type === 'logistics'
                            ? await addLogisticsChannelPairing(selectedCode, {
                                ...values,
                                upstreamChannelCode: pairingModal.row.channelCode,
                            })
                            : await addSkuPairing(selectedCode, {
                                ...values,
                                masterSku: pairingModal.row.masterSku,
                            });
                    hide();
                    const ok = resultOk(resp, '配对成功');
                    if (ok) {
                        setPairingModal({ open: false });
                        warehouseActionRef.current?.reload();
                        logisticsActionRef.current?.reload();
                        skuActionRef.current?.reload();
                    }
                    return ok;
                } })] }));
}
