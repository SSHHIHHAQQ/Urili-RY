import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ReloadOutlined, SettingOutlined, SyncOutlined } from '@ant-design/icons';
import { ModalForm, ProFormSelect, ProFormText, ProFormTextArea, ProFormTimePicker, ProTable, } from '@ant-design/pro-components';
import { Button, Descriptions, Space, Tag, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { getSyncConfig, getSyncLogList, saveSyncConfig, syncRates, testSyncConfig, } from '@/services/finance/currency';
import { statusValueEnum, syncStatusValueEnum, } from '../constants';
const showApiApplicationName = 'fenxiao';
const showApiApplicationId = '2080411';
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
function normalizeSyncValues(values) {
    const rateAnchorTime = values.rateAnchorTime;
    return {
        ...values,
        rateAnchorTime: rateAnchorTime && typeof rateAnchorTime.format === 'function'
            ? rateAnchorTime.format('HH:mm:ss')
            : rateAnchorTime,
    };
}
function buildSyncInitialValues(syncConfig) {
    return {
        baseCurrencyCode: 'CNY',
        providerCode: 'SHOWAPI_BANK_RATE',
        providerName: 'ShowAPI银行汇率查询',
        showApiApplicationName,
        showApiApplicationId,
        rateAnchorTime: '09:30:00',
        status: '0',
        ...syncConfig,
        credential: undefined,
    };
}
function statusText(value) {
    if (!value)
        return '-';
    return statusValueEnum[value]?.text || value;
}
function syncStatusText(value) {
    if (!value)
        return '-';
    return syncStatusValueEnum[value]?.text || value;
}
function syncStatusTag(value) {
    if (!value)
        return '-';
    const color = value === 'SUCCESS' ? 'success' : value === 'FAILED' ? 'error' : 'warning';
    return _jsx(Tag, { color: color, children: syncStatusText(value) });
}
function statusTag(value) {
    if (!value)
        return '-';
    return _jsx(Tag, { color: value === '0' ? 'success' : 'default', children: statusText(value) });
}
export default function SyncSettingsPanel({ access, onSynced, }) {
    const syncFormRef = useRef(undefined);
    const syncLogActionRef = useRef(undefined);
    const [syncConfig, setSyncConfig] = useState();
    const [syncModalOpen, setSyncModalOpen] = useState(false);
    const [testing, setTesting] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const reloadSyncConfig = async () => {
        const resp = await getSyncConfig();
        if (resp.code === 200) {
            setSyncConfig({ ...resp.data, credential: undefined });
        }
    };
    useEffect(() => {
        reloadSyncConfig();
    }, []);
    const handleSave = async (values) => {
        const ok = resultOk(await saveSyncConfig(normalizeSyncValues(values)), '同步设置已保存');
        if (ok) {
            await reloadSyncConfig();
        }
        return ok;
    };
    const handleTestConnection = async () => {
        setTesting(true);
        try {
            const values = syncFormRef.current?.getFieldsValue() || {};
            const resp = await testSyncConfig(normalizeSyncValues(values));
            if (resp.code === 200) {
                message.success(`测试成功，返回币种 ${resp.data.currencyCount}`);
                syncLogActionRef.current?.reload();
                await reloadSyncConfig();
            }
            else {
                message.error(resp.msg);
            }
        }
        finally {
            setTesting(false);
        }
    };
    const handleSyncNow = async () => {
        setSyncing(true);
        try {
            const values = await syncFormRef.current?.validateFields();
            const saveOk = resultOk(await saveSyncConfig(normalizeSyncValues(values || {})), '同步设置已保存');
            if (!saveOk)
                return;
            const resp = await syncRates();
            if (resp.code === 200) {
                message.success(`同步完成，更新币种 ${resp.data.updatedCount}`);
                onSynced?.();
                syncLogActionRef.current?.reload();
                await reloadSyncConfig();
                setSyncModalOpen(false);
            }
            else {
                message.error(resp.msg);
            }
        }
        finally {
            setSyncing(false);
        }
    };
    const syncLogColumns = [
        { title: '请求时间', dataIndex: 'requestTime', width: 170, search: false },
        {
            title: '状态',
            dataIndex: 'status',
            valueEnum: syncStatusValueEnum,
            width: 110,
        },
        { title: '服务商', dataIndex: 'providerCode', width: 140 },
        { title: '返回币种数', dataIndex: 'currencyCount', width: 110, search: false },
        { title: '更新币种数', dataIndex: 'updatedCount', width: 110, search: false },
        { title: '耗时(ms)', dataIndex: 'costMs', width: 100, search: false },
        { title: '错误码', dataIndex: 'errorCode', width: 130, search: false },
        { title: '错误信息', dataIndex: 'errorMessage', ellipsis: true, search: false },
        { title: 'TraceId', dataIndex: 'traceId', width: 220, copyable: true },
    ];
    return (_jsxs(_Fragment, { children: [_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsxs("div", { style: {
                            background: '#fff',
                            border: '1px solid #f0f0f0',
                            borderRadius: 6,
                            padding: 16,
                        }, children: [_jsxs(Space, { align: "start", style: {
                                    width: '100%',
                                    justifyContent: 'space-between',
                                    marginBottom: 12,
                                }, children: [_jsxs(Space, { direction: "vertical", size: 2, children: [_jsx(Typography.Text, { strong: true, children: syncConfig?.providerName || 'ShowAPI银行汇率查询' }), _jsx(Typography.Text, { type: "secondary", children: "\u5B98\u65B9\u6C47\u7387\u53D6\u73B0\u6C47\u5356\u51FA\u4EF7\uFF0C\u57FA\u51C6\u5E01\u79CD CNY" })] }), _jsx(Button, { icon: _jsx(SettingOutlined, {}), hidden: !access.hasPerms('finance:currency:syncConfig'), onClick: () => setSyncModalOpen(true), children: "\u540C\u6B65\u8BBE\u7F6E" })] }), _jsxs(Descriptions, { size: "small", column: { xs: 1, sm: 2, md: 3, xl: 4 }, children: [_jsx(Descriptions.Item, { label: "\u542F\u7528\u72B6\u6001", children: statusTag(syncConfig?.status) }), _jsx(Descriptions.Item, { label: "\u6C47\u7387\u57FA\u51C6\u65F6\u95F4", children: syncConfig?.rateAnchorTime || '09:30:00' }), _jsx(Descriptions.Item, { label: "\u63A5\u5165\u5BC6\u94A5", children: syncConfig?.credentialMasked || '-' }), _jsx(Descriptions.Item, { label: "\u6700\u8FD1\u540C\u6B65", children: syncConfig?.lastSyncTime || '-' }), _jsx(Descriptions.Item, { label: "\u6700\u8FD1\u72B6\u6001", children: syncStatusTag(syncConfig?.lastSyncStatus) }), _jsx(Descriptions.Item, { label: "\u5E94\u7528\u540D\u79F0", children: showApiApplicationName }), _jsx(Descriptions.Item, { label: "\u5E94\u7528 ID", children: showApiApplicationId }), _jsx(Descriptions.Item, { label: "\u57FA\u51C6\u5E01\u79CD", children: "\u4EBA\u6C11\u5E01 (CNY)" })] })] }), _jsx(ProTable, { actionRef: syncLogActionRef, rowKey: "syncLogId", columns: syncLogColumns, scroll: getProTableScroll(1200), search: getPersistedProTableSearch({ labelWidth: 100 }, 'finance-currency-sync-log'), request: async (params) => {
                            const resp = await getSyncLogList(params);
                            return {
                                data: resp.rows || [],
                                success: resp.code === 200,
                                total: resp.total || 0,
                            };
                        } })] }), _jsxs(ModalForm, { formRef: syncFormRef, title: "\u540C\u6B65\u8BBE\u7F6E", open: syncModalOpen, width: 760, grid: true, modalProps: {
                    destroyOnHidden: true,
                    onCancel: () => setSyncModalOpen(false),
                }, initialValues: buildSyncInitialValues(syncConfig), onOpenChange: setSyncModalOpen, onFinish: handleSave, submitter: {
                    searchConfig: {
                        submitText: '保存设置',
                    },
                    render: (_, dom) => [
                        _jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: testing, hidden: !access.hasPerms('finance:currency:sync'), onClick: handleTestConnection, children: "\u6D4B\u8BD5\u8FDE\u63A5" }, "test"),
                        _jsx(Button, { icon: _jsx(SyncOutlined, {}), loading: syncing, hidden: !access.hasPerms('finance:currency:sync'), onClick: handleSyncNow, children: "\u7ACB\u5373\u540C\u6B65" }, "sync"),
                        ...dom,
                    ],
                }, children: [_jsx(ProFormText, { name: "providerName", label: "\u5B98\u65B9\u6C47\u7387\u670D\u52A1", colProps: { xs: 24, md: 12 }, readonly: true }), _jsx(ProFormSelect, { name: "baseCurrencyCode", label: "\u57FA\u51C6\u5E01\u79CD", colProps: { xs: 24, md: 12 }, options: [{ label: '人民币 (CNY)', value: 'CNY' }], readonly: true, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormText, { name: "showApiApplicationName", label: "\u5E94\u7528\u540D\u79F0", colProps: { xs: 24, md: 12 }, readonly: true }), _jsx(ProFormText, { name: "showApiApplicationId", label: "\u5E94\u7528 ID", colProps: { xs: 24, md: 12 }, readonly: true }), _jsx(ProFormText.Password, { name: "credential", label: "\u63A5\u5165\u5BC6\u94A5", colProps: { xs: 24 }, placeholder: syncConfig?.credentialMasked || '保存后只展示脱敏值', fieldProps: { autoComplete: 'new-password' } }), _jsx(ProFormTimePicker, { name: "rateAnchorTime", label: "\u6C47\u7387\u57FA\u51C6\u65F6\u95F4", colProps: { xs: 24, md: 12 }, fieldProps: { format: 'HH:mm:ss' }, rules: [{ required: true }] }), _jsx(ProFormSelect, { name: "status", label: "\u542F\u7528\u72B6\u6001", colProps: { xs: 24, md: 12 }, valueEnum: statusValueEnum, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8", colProps: { xs: 24 } })] }, syncConfig?.syncConfigId || 'new-sync-config')] }));
}
