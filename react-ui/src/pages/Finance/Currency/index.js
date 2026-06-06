import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import { DrawerForm, ModalForm, PageContainer, ProFormDependency, ProFormDigit, ProFormSelect, ProFormText, ProFormTextArea, ProTable, } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Dropdown, Modal, Tabs } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { getDictSelectOption } from '@/services/system/dict';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { addCurrency, deleteCurrency, getCurrencyList, getRateHistoryList, updateCurrency, updateCurrencyStatus, } from '@/services/finance/currency';
import { adjustmentModeOptions, roundingModeOptions, statusValueEnum, yesNoValueEnum, } from './constants';
import SyncSettingsPanel from './components/SyncSettingsPanel';
const defaultCurrencyValues = {
    amountPrecision: 2,
    baseCurrencyCode: 'CNY',
    isDefault: 'N',
    ratePrecision: 8,
    roundingMode: 'HALF_UP',
    adjustmentMode: 'NONE',
    status: '0',
};
const baseCurrencyOptions = [{ label: '人民币 (CNY)', value: 'CNY' }];
const adjustedModeValues = ['PERCENT_UP', 'PERCENT_DOWN', 'FIXED_DELTA'];
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
function normalizeRatePrecision(precision) {
    if (precision === undefined || precision === null) {
        return 8;
    }
    return Math.min(Math.max(Number(precision), 0), 10);
}
function formatRate(value, precision) {
    if (value === undefined || value === null) {
        return '-';
    }
    return Number(value).toFixed(normalizeRatePrecision(precision));
}
export default function FinanceCurrencyPage() {
    const access = useAccess();
    const actionRef = useRef(null);
    const historyActionRef = useRef(null);
    const currencyFormRef = useRef(undefined);
    const [currencyOptions, setCurrencyOptions] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [currentCurrency, setCurrentCurrency] = useState();
    const [historyCurrency, setHistoryCurrency] = useState();
    useEffect(() => {
        getDictSelectOption('currency_code').then(setCurrencyOptions);
    }, []);
    const openCreateModal = () => {
        setCurrentCurrency(undefined);
        setModalOpen(true);
    };
    const openEditModal = (record) => {
        setCurrentCurrency(record);
        setModalOpen(true);
    };
    const saveCurrency = async (values) => {
        const payload = { ...values, baseCurrencyCode: 'CNY' };
        const resp = currentCurrency
            ? await updateCurrency(currentCurrency.currencyCode, payload)
            : await addCurrency(payload);
        if (resultOk(resp, currentCurrency ? '币种已更新' : '币种已新增')) {
            actionRef.current?.reload();
            return true;
        }
        return false;
    };
    const toggleStatus = async (record) => {
        const nextStatus = record.status === '0' ? '1' : '0';
        const ok = resultOk(await updateCurrencyStatus(record.currencyCode, nextStatus), nextStatus === '0' ? '币种已启用' : '币种已停用');
        if (ok)
            actionRef.current?.reload();
    };
    const removeCurrency = (record) => {
        Modal.confirm({
            title: '删除币种',
            content: `确认删除 ${record.currencyCode}？已有汇率历史的币种会被后端拒绝删除。`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const ok = resultOk(await deleteCurrency(record.currencyCode), '币种已删除');
                if (ok)
                    actionRef.current?.reload();
            },
        });
    };
    const setDefaultCurrency = async (record) => {
        const ok = resultOk(await updateCurrency(record.currencyCode, { ...record, isDefault: 'Y' }), '默认币种已更新');
        if (ok)
            actionRef.current?.reload();
    };
    useEffect(() => {
        if (!modalOpen) {
            return;
        }
        currencyFormRef.current?.resetFields();
        currencyFormRef.current?.setFieldsValue(currentCurrency || defaultCurrencyValues);
    }, [currentCurrency, modalOpen]);
    const currencyColumns = [
        {
            title: '币种代码',
            dataIndex: 'currencyCode',
            width: 110,
        },
        {
            title: '币种名称',
            dataIndex: 'currencyName',
            width: 180,
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 100,
        },
        {
            title: '默认',
            dataIndex: 'isDefault',
            valueType: 'select',
            valueEnum: yesNoValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
        },
        {
            title: '基准币种',
            dataIndex: 'baseCurrencyCode',
            width: 110,
            search: false,
        },
        {
            title: '官方汇率',
            dataIndex: 'officialRate',
            width: 130,
            search: false,
            render: (_, record) => formatRate(record.officialRate, record.ratePrecision),
        },
        {
            title: '生效汇率',
            dataIndex: 'effectiveRate',
            width: 130,
            search: false,
            render: (_, record) => formatRate(record.effectiveRate, record.ratePrecision),
        },
        {
            title: '调整方式',
            dataIndex: 'adjustmentMode',
            width: 110,
            search: false,
            renderText: (value) => adjustmentModeOptions.find((item) => item.value === value)?.label ||
                value,
        },
        {
            title: '生效汇率时间',
            dataIndex: 'effectiveRateTime',
            width: 170,
            search: false,
        },
        {
            title: '生效汇率时间',
            dataIndex: 'effectiveRateTimeRange',
            colSize: 2,
            valueType: 'dateRange',
            hideInTable: true,
            search: {
                transform: (value) => ({
                    effectiveBeginTime: value?.[0],
                    effectiveEndTime: value?.[1],
                }),
            },
        },
        {
            title: '操作',
            valueType: 'option',
            width: 190,
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('finance:currency:edit'), onClick: () => openEditModal(record), children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('finance:currency:edit'), onClick: () => toggleStatus(record), children: record.status === '0' ? '停用' : '启用' }, "status"),
                _jsx(Dropdown, { menu: {
                        items: [
                            {
                                key: 'history',
                                label: '汇率历史',
                            },
                            {
                                key: 'default',
                                label: '设为默认',
                                disabled: record.isDefault === 'Y' ||
                                    !access.hasPerms('finance:currency:edit'),
                            },
                            {
                                key: 'delete',
                                danger: true,
                                label: '删除',
                                disabled: !access.hasPerms('finance:currency:remove'),
                            },
                        ],
                        onClick: ({ key }) => {
                            if (key === 'history')
                                setHistoryCurrency(record);
                            if (key === 'default')
                                setDefaultCurrency(record);
                            if (key === 'delete')
                                removeCurrency(record);
                        },
                    }, trigger: ['click'], children: _jsxs(Button, { type: "link", size: "small", children: ["\u66F4\u591A ", _jsx(DownOutlined, { style: { fontSize: 10 } })] }) }, "more"),
            ],
        },
    ];
    const historyColumns = [
        { title: '时间', dataIndex: 'createTime', width: 170 },
        { title: '来源', dataIndex: 'sourceType', width: 100 },
        { title: '基准币种', dataIndex: 'baseCurrencyCode', width: 110 },
        {
            title: '官方汇率',
            dataIndex: 'officialRate',
            width: 130,
            render: (_, record) => formatRate(record.officialRate, historyCurrency?.ratePrecision),
        },
        {
            title: '生效汇率',
            dataIndex: 'effectiveRate',
            width: 130,
            render: (_, record) => formatRate(record.effectiveRate, historyCurrency?.ratePrecision),
        },
        { title: '调整方式', dataIndex: 'adjustmentMode', width: 120 },
        { title: '原因', dataIndex: 'changeReason', ellipsis: true },
    ];
    const currencyTable = (_jsx(ProTable, { actionRef: actionRef, rowKey: "currencyCode", columns: currencyColumns, scroll: getProTableScroll(1500), search: getPersistedProTableSearch({ labelWidth: 110 }, 'finance-currency'), request: async (params) => {
            const resp = await getCurrencyList(params);
            return {
                data: resp.rows || [],
                success: resp.code === 200,
                total: resp.total || 0,
            };
        }, toolBarRender: () => [
            _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms('finance:currency:add'), onClick: openCreateModal, children: "\u65B0\u589E\u5E01\u79CD" }, "add"),
        ] }));
    return (_jsxs(PageContainer, { children: [_jsx(Tabs, { items: [
                    { key: 'currency', label: '币种列表', children: currencyTable },
                    {
                        key: 'sync',
                        label: '同步设置',
                        children: (_jsx(SyncSettingsPanel, { access: access, onSynced: () => actionRef.current?.reload() })),
                    },
                ] }), _jsxs(ModalForm, { formRef: currencyFormRef, title: currentCurrency ? '编辑币种' : '新增币种', open: modalOpen, modalProps: { destroyOnHidden: true, onCancel: () => setModalOpen(false) }, initialValues: currentCurrency || defaultCurrencyValues, onOpenChange: setModalOpen, onFinish: saveCurrency, children: [_jsx(ProFormSelect, { name: "currencyCode", label: "\u5E01\u79CD\u4EE3\u7801", options: currencyOptions, disabled: !!currentCurrency, rules: [{ required: true }], fieldProps: {
                            ...SEARCHABLE_SELECT_PROPS,
                            onChange: (_, option) => {
                                if (!currentCurrency && option?.label) {
                                    currencyFormRef.current?.setFieldValue('currencyName', option.label);
                                }
                            },
                        } }), _jsx(ProFormText, { name: "currencyName", label: "\u5E01\u79CD\u540D\u79F0", rules: [{ required: true }] }), _jsx(ProFormText, { name: "currencySymbol", label: "\u5E01\u79CD\u7B26\u53F7" }), _jsx(ProFormSelect, { name: "baseCurrencyCode", label: "\u57FA\u51C6\u5E01\u79CD", options: baseCurrencyOptions, readonly: true, rules: [{ required: true }], fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormDigit, { name: "officialRate", label: "\u5B98\u65B9\u6C47\u7387", min: 0 }), _jsx(ProFormSelect, { name: "adjustmentMode", label: "\u8C03\u6574\u65B9\u5F0F", options: adjustmentModeOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormDependency, { name: ['adjustmentMode'], children: ({ adjustmentMode }) => {
                            const mode = adjustmentMode || 'NONE';
                            const manualMode = mode === 'MANUAL';
                            const adjustedMode = adjustedModeValues.includes(mode);
                            return (_jsxs(_Fragment, { children: [_jsx(ProFormDigit, { name: "effectiveRate", label: "\u751F\u6548\u6C47\u7387", min: 0, disabled: !manualMode, tooltip: "\u4EBA\u5DE5\u7EF4\u62A4\u6A21\u5F0F\u4E0B\u586B\u5199\uFF1B\u5176\u4ED6\u8C03\u6574\u65B9\u5F0F\u7531\u540E\u7AEF\u6309\u5B98\u65B9\u6C47\u7387\u548C\u8C03\u6574\u503C\u8BA1\u7B97" }), _jsx(ProFormDigit, { name: "adjustmentValue", label: "\u8C03\u6574\u503C", disabled: !adjustedMode, tooltip: "\u767E\u5206\u6BD4\u6A21\u5F0F\u4E0B 1 \u8868\u793A 1%\uFF0C\u56FA\u5B9A\u52A0\u51CF\u503C\u6309\u6C47\u7387\u503C\u586B\u5199" })] }));
                        } }), _jsx(ProFormDigit, { name: "ratePrecision", label: "\u6C47\u7387\u7CBE\u5EA6", min: 0, max: 10 }), _jsx(ProFormDigit, { name: "amountPrecision", label: "\u91D1\u989D\u7CBE\u5EA6", min: 0, max: 6 }), _jsx(ProFormSelect, { name: "roundingMode", label: "\u820D\u5165\u65B9\u5F0F", options: roundingModeOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormSelect, { name: "isDefault", label: "\u9ED8\u8BA4\u5E01\u79CD", valueEnum: yesNoValueEnum, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormSelect, { name: "status", label: "\u72B6\u6001", valueEnum: statusValueEnum, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8" })] }, currentCurrency?.currencyCode || 'new-currency'), _jsx(DrawerForm, { title: `${historyCurrency?.currencyCode || ''} 汇率历史`, open: !!historyCurrency, drawerProps: {
                    destroyOnHidden: true,
                    onClose: () => setHistoryCurrency(undefined),
                }, submitter: false, children: historyCurrency ? (_jsx(ProTable, { actionRef: historyActionRef, rowKey: "rateHistoryId", columns: historyColumns, search: false, scroll: getProTableScroll(1000), request: async (params) => {
                        const resp = await getRateHistoryList(historyCurrency.currencyCode, params);
                        return {
                            data: resp.rows || [],
                            success: resp.code === 200,
                            total: resp.total || 0,
                        };
                    } })) : null })] }));
}
