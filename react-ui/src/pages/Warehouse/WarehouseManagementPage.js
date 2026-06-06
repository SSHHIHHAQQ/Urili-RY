import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { PlusOutlined, SyncOutlined } from '@ant-design/icons';
import { PageContainer, ProTable, } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Modal, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getDictSelectOption } from '@/services/system/dict';
import { addOfficialWarehouse, addThirdPartyWarehouse, getOfficialWarehouseList, getThirdPartyWarehouseList, getWarehouseCurrencyOptions, getWarehouseSellerOptions, syncOfficialWarehouse, updateOfficialWarehouse, updateOfficialWarehouseStatus, updateThirdPartyWarehouse, updateThirdPartyWarehouseStatus, } from '@/services/warehouse/warehouse';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTablePagination, getProTableScroll, } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { statusColor, statusText, WAREHOUSE_STATUS_OPTIONS, WAREHOUSE_STATUS_VALUE_ENUM, } from './constants';
import OfficialSyncModal from './components/OfficialSyncModal';
import WarehouseFormModal from './components/WarehouseFormModal';
const serviceMap = {
    official: {
        list: getOfficialWarehouseList,
        add: addOfficialWarehouse,
        update: updateOfficialWarehouse,
        updateStatus: updateOfficialWarehouseStatus,
    },
    third_party: {
        list: getThirdPartyWarehouseList,
        add: addThirdPartyWarehouse,
        update: updateThirdPartyWarehouse,
        updateStatus: updateThirdPartyWarehouseStatus,
    },
};
const permissionMap = {
    official: {
        add: 'warehouse:official:add',
        edit: 'warehouse:official:edit',
        status: 'warehouse:official:status',
        sync: 'warehouse:official:sync',
    },
    third_party: {
        add: 'warehouse:thirdParty:add',
        edit: 'warehouse:thirdParty:edit',
        status: 'warehouse:thirdParty:status',
    },
};
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
function displayText(value) {
    return value === undefined || value === null || value === '' ? '-' : value;
}
function cleanParams(params) {
    return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''));
}
export default function WarehouseManagementPage({ kind }) {
    const access = useAccess();
    const actionRef = useRef(null);
    const [countryOptions, setCountryOptions] = useState([]);
    const [currencyOptions, setCurrencyOptions] = useState([]);
    const [sellerOptions, setSellerOptions] = useState([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [syncOpen, setSyncOpen] = useState(false);
    const [currentWarehouse, setCurrentWarehouse] = useState();
    const isOfficial = kind === 'official';
    const services = serviceMap[kind];
    const permissions = permissionMap[kind];
    const searchFieldCount = isOfficial ? 7 : 8;
    useEffect(() => {
        getDictSelectOption('country_region').then(setCountryOptions);
        getWarehouseCurrencyOptions().then((resp) => {
            if (resp.code === 200) {
                setCurrencyOptions(resp.data || []);
            }
        });
        if (!isOfficial) {
            getWarehouseSellerOptions().then((resp) => {
                if (resp.code === 200) {
                    setSellerOptions(resp.data || []);
                }
            });
        }
    }, [isOfficial]);
    const openCreate = () => {
        setCurrentWarehouse(undefined);
        setModalOpen(true);
    };
    const openEdit = (record) => {
        setCurrentWarehouse(record);
        setModalOpen(true);
    };
    const saveWarehouse = async (values) => {
        const resp = values.warehouseId
            ? await services.update(values)
            : await services.add(values);
        if (resultOk(resp, values.warehouseId ? '仓库已更新' : '仓库已新增')) {
            setModalOpen(false);
            actionRef.current?.reload();
            return true;
        }
        return false;
    };
    const toggleStatus = (record) => {
        const nextStatus = record.status === '0' ? '1' : '0';
        Modal.confirm({
            title: `${nextStatus === '0' ? '启用' : '停用'}仓库`,
            content: `确认${nextStatus === '0' ? '启用' : '停用'} ${record.warehouseCode}？`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const ok = resultOk(await services.updateStatus({
                    warehouseId: record.warehouseId,
                    status: nextStatus,
                }), nextStatus === '0' ? '仓库已启用' : '仓库已停用');
                if (ok) {
                    actionRef.current?.reload();
                }
            },
        });
    };
    const submitSync = async (values) => {
        const ok = resultOk(await syncOfficialWarehouse(values), '官方仓库已同步并配对');
        if (ok) {
            setSyncOpen(false);
            actionRef.current?.reload();
            return true;
        }
        return false;
    };
    const countryValueEnum = useMemo(() => Object.fromEntries(countryOptions.map((item) => [item.value, { text: item.label }])), [countryOptions]);
    const currencyValueEnum = useMemo(() => Object.fromEntries(currencyOptions.map((item) => [item.value, { text: item.label }])), [currencyOptions]);
    const columns = [
        {
            title: '仓库编码',
            dataIndex: 'warehouseCode',
            width: 150,
            copyable: true,
            ellipsis: true,
        },
        {
            title: '仓库名称',
            dataIndex: 'warehouseName',
            width: 200,
            ellipsis: true,
        },
        {
            title: '国家/地区',
            dataIndex: 'countryCode',
            width: 130,
            valueType: 'select',
            valueEnum: countryValueEnum,
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: countryOptions,
            },
        },
        {
            title: '州/省',
            dataIndex: 'stateProvince',
            width: 140,
            ellipsis: true,
        },
        {
            title: '城市',
            dataIndex: 'city',
            width: 130,
            ellipsis: true,
        },
        {
            title: '地址',
            dataIndex: 'addressLine1',
            width: 260,
            search: false,
            ellipsis: true,
            render: (_, record) => (_jsxs(Space, { direction: "vertical", size: 0, children: [_jsx(Typography.Text, { ellipsis: { tooltip: record.addressLine1 }, children: displayText(record.addressLine1) }), _jsx(Typography.Text, { type: "secondary", ellipsis: { tooltip: record.addressLine2 }, children: displayText(record.addressLine2) })] })),
        },
        {
            title: '结算币种',
            dataIndex: 'settlementCurrency',
            width: 120,
            valueType: 'select',
            valueEnum: currencyValueEnum,
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: currencyOptions,
            },
        },
        {
            title: '联系人',
            dataIndex: 'contactName',
            width: 130,
            search: false,
            ellipsis: true,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            valueType: 'select',
            valueEnum: WAREHOUSE_STATUS_VALUE_ENUM,
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: WAREHOUSE_STATUS_OPTIONS,
            },
            render: (_, record) => _jsx(Tag, { color: statusColor(record.status), children: statusText(record.status) }),
        },
    ];
    if (isOfficial) {
        columns.push({
            title: '上游配对',
            dataIndex: 'upstreamWarehouseName',
            width: 220,
            search: false,
            render: (_, record) => record.warehousePairingId ? (_jsxs(Space, { direction: "vertical", size: 0, children: [_jsx(Typography.Text, { children: displayText(record.masterWarehouseName || record.connectionCode) }), _jsxs(Typography.Text, { type: "secondary", children: [displayText(record.upstreamWarehouseName), " / ", displayText(record.upstreamWarehouseCode)] })] })) : (_jsx(Tag, { children: "\u672A\u914D\u5BF9" })),
        });
    }
    else {
        columns.splice(2, 0, {
            title: '归属卖家',
            dataIndex: 'sellerKeyword',
            width: 220,
            ellipsis: true,
            render: (_, record) => (_jsxs(Space, { direction: "vertical", size: 0, children: [_jsx(Typography.Text, { children: displayText(record.sellerCode) }), _jsx(Typography.Text, { type: "secondary", children: displayText(record.sellerShortName || record.sellerName) })] })),
        });
    }
    columns.push({
        title: '更新时间',
        dataIndex: 'updateTime',
        width: 170,
        search: false,
        renderText: (_, record) => record.updateTime || record.createTime || '-',
    });
    columns.push({
        title: '操作',
        valueType: 'option',
        width: 130,
        fixed: 'right',
        render: (_, record) => [
            access.hasPerms(permissions.edit) ? (_jsx(Button, { type: "link", size: "small", onClick: () => openEdit(record), children: "\u7F16\u8F91" }, "edit")) : null,
            access.hasPerms(permissions.status) ? (_jsx(Button, { type: "link", size: "small", onClick: () => toggleStatus(record), children: record.status === '0' ? '停用' : '启用' }, "status")) : null,
        ],
    });
    return (_jsxs(PageContainer, { title: false, children: [_jsx(ProTable, { actionRef: actionRef, rowKey: "warehouseId", columns: columns, options: false, search: getPersistedProTableSearch({ labelWidth: 96, fieldCount: searchFieldCount }, `warehouse-${kind}`), pagination: getProTablePagination(20), scroll: getProTableScroll(isOfficial ? 1540 : 1640), request: async (params) => {
                    const resp = await services.list(cleanParams(params));
                    return {
                        data: resp.rows || [],
                        success: resp.code === 200,
                        total: resp.total,
                    };
                }, toolBarRender: () => [
                    isOfficial && access.hasPerms(permissionMap.official.sync) ? (_jsx(Button, { icon: _jsx(SyncOutlined, {}), onClick: () => setSyncOpen(true), children: "\u540C\u6B65\u4ED3\u5E93" }, "sync")) : null,
                    access.hasPerms(permissions.add) ? (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: openCreate, children: isOfficial ? '新增官方仓库' : '新增第三方仓库' }, "add")) : null,
                ] }), _jsx(WarehouseFormModal, { open: modalOpen, title: currentWarehouse ? '编辑仓库' : isOfficial ? '新增官方仓库' : '新增第三方仓库', current: currentWarehouse, countryOptions: countryOptions, currencyOptions: currencyOptions, sellerOptions: sellerOptions, showSeller: !isOfficial, onOpenChange: setModalOpen, onSubmit: saveWarehouse }), isOfficial ? (_jsx(OfficialSyncModal, { open: syncOpen, countryOptions: countryOptions, currencyOptions: currencyOptions, onOpenChange: setSyncOpen, onSubmit: submitSync })) : null] }));
}
