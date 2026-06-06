import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess, history } from '@umijs/max';
import { Button, Modal, Dropdown, Switch, message as antdMessage } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, DownOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getRoleList, removeRole, addRole, updateRole, exportRole, getRoleMenuList, changeRoleStatus, updateRoleDataScope, getDeptTreeSelect, getRole } from '@/services/system/role';
import UpdateForm from './edit';
import { getDictValueEnum } from '@/services/system/dict';
import { formatTreeData } from '@/utils/tree';
import { getMenuTree } from '@/services/system/menu';
import DataScopeForm from './components/DataScope';
const { confirm } = Modal;
/**
 * 添加节点
 *
 * @param fields
 */
const handleAdd = async (fields) => {
    const hide = message.loading('正在添加');
    try {
        const resp = await addRole({ ...fields });
        hide();
        if (resp.code === 200) {
            message.success('添加成功');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch {
        hide();
        message.error('添加失败请重试！');
        return false;
    }
};
/**
 * 更新节点
 *
 * @param fields
 */
const handleUpdate = async (fields) => {
    const hide = message.loading('正在更新');
    try {
        const resp = await updateRole(fields);
        hide();
        if (resp.code === 200) {
            message.success('更新成功');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch {
        hide();
        message.error('配置失败请重试！');
        return false;
    }
};
/**
 * 删除节点
 *
 * @param selectedRows
 */
const handleRemove = async (selectedRows) => {
    const hide = message.loading('正在删除');
    if (!selectedRows)
        return true;
    try {
        const resp = await removeRole(selectedRows.map((row) => row.roleId).join(','));
        hide();
        if (resp.code === 200) {
            message.success('删除成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
const handleRemoveOne = async (selectedRow) => {
    const hide = message.loading('正在删除');
    if (!selectedRow)
        return true;
    try {
        const params = [selectedRow.roleId];
        const resp = await removeRole(params.join(','));
        hide();
        if (resp.code === 200) {
            message.success('删除成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
/**
 * 导出数据
 *
 *
 */
const handleExport = async () => {
    const hide = message.loading('正在导出');
    try {
        await exportRole();
        hide();
        message.success('导出成功');
        return true;
    }
    catch {
        hide();
        message.error('导出失败，请重试');
        return false;
    }
};
const RoleTableList = () => {
    const [messageApi, contextHolder] = antdMessage.useMessage();
    const formTableRef = { current: undefined };
    const [modalVisible, setModalVisible] = useState(false);
    const [dataScopeModalOpen, setDataScopeModalOpen] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [menuTree, setMenuTree] = useState();
    const [menuIds, setMenuIds] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
        getDictValueEnum('sys_normal_disable').then((data) => {
            setStatusOptions(data);
        });
    }, []);
    const showChangeStatusConfirm = (record) => {
        const text = record.status === "1" ? "启用" : "停用";
        const newStatus = record.status === '0' ? '1' : '0';
        confirm({
            title: `确认要${text}${record.roleName}角色吗？`,
            onOk() {
                changeRoleStatus(record.roleId, newStatus).then(resp => {
                    if (resp.code === 200) {
                        messageApi.open({
                            type: 'success',
                            content: '更新成功！',
                        });
                        actionRef.current?.reload();
                    }
                    else {
                        messageApi.open({
                            type: 'error',
                            content: '更新失败！',
                        });
                    }
                });
            },
        });
    };
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "system.role.role_id", defaultMessage: "\u89D2\u8272\u7F16\u53F7" }),
            dataIndex: 'roleId',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.role.role_name", defaultMessage: "\u89D2\u8272\u540D\u79F0" }),
            dataIndex: 'roleName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.role.role_key", defaultMessage: "\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32" }),
            dataIndex: 'roleKey',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.role.role_sort", defaultMessage: "\u663E\u793A\u987A\u5E8F" }),
            dataIndex: 'roleSort',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.role.status", defaultMessage: "\u89D2\u8272\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(Switch, { checked: record.status === '0', checkedChildren: "\u6B63\u5E38", unCheckedChildren: "\u505C\u7528", defaultChecked: true, onClick: () => showChangeStatusConfirm(record) }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "system.role.create_time", defaultMessage: "\u521B\u5EFA\u65F6\u95F4" }),
            dataIndex: 'createTime',
            colSize: 2,
            valueType: 'dateRange',
            render: (_, record) => {
                return (_jsxs("span", { children: [record.createTime.toString(), " "] }));
            },
            search: {
                transform: (value) => {
                    return {
                        'params[beginTime]': value[0],
                        'params[endTime]': value[1],
                    };
                },
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '220px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('system:role:edit'), onClick: () => {
                        getRoleMenuList(record.roleId).then((res) => {
                            if (res.code === 200) {
                                const treeData = formatTreeData(res.menus);
                                setMenuTree(treeData);
                                setMenuIds(res.checkedKeys.map(item => {
                                    return `${item}`;
                                }));
                                setModalVisible(true);
                                setCurrentRow(record);
                            }
                            else {
                                message.warning(res.msg);
                            }
                        });
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('system:role:remove'), onClick: async () => {
                        Modal.confirm({
                            title: '删除',
                            content: '确定删除该项吗？',
                            okText: '确认',
                            cancelText: '取消',
                            onOk: async () => {
                                const success = await handleRemoveOne(record);
                                if (success) {
                                    if (actionRef.current) {
                                        actionRef.current.reload();
                                    }
                                }
                            },
                        });
                    }, children: "\u5220\u9664" }, "batchRemove"),
                _jsx(Dropdown, { trigger: ['click'], menu: {
                        items: [
                            {
                                label: '数据权限',
                                key: 'datascope',
                                disabled: !access.hasPerms('system:role:edit'),
                            },
                            {
                                label: '分配用户',
                                key: 'authUser',
                                disabled: !access.hasPerms('system:role:edit'),
                            },
                        ],
                        onClick: ({ key }) => {
                            if (key === 'datascope') {
                                getRole(record.roleId).then(resp => {
                                    if (resp.code === 200) {
                                        setCurrentRow(resp.data);
                                        setDataScopeModalOpen(true);
                                    }
                                });
                                getDeptTreeSelect(record.roleId).then(resp => {
                                    if (resp.code === 200) {
                                        setMenuTree(formatTreeData(resp.depts));
                                        setMenuIds(resp.checkedKeys.map((item) => {
                                            return `${item}`;
                                        }));
                                    }
                                });
                            }
                            else if (key === 'authUser') {
                                history.push(`/system/role-auth/user/${record.roleId}`);
                            }
                        }
                    }, children: _jsxs("a", { onClick: (e) => e.preventDefault(), children: ["\u66F4\u591A ", _jsx(DownOutlined, { style: { fontSize: 10 } })] }) }, "more"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [contextHolder, _jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "roleId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:role:add'), onClick: async () => {
                                getMenuTree().then((res) => {
                                    if (res.code === 200) {
                                        const treeData = formatTreeData(res.data);
                                        setMenuTree(treeData);
                                        setMenuIds([]);
                                        setModalVisible(true);
                                        setCurrentRow(undefined);
                                    }
                                    else {
                                        message.warning(res.msg);
                                    }
                                });
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:role:remove'), onClick: async () => {
                                Modal.confirm({
                                    title: '是否确认删除所选数据项?',
                                    icon: _jsx(ExclamationCircleOutlined, {}),
                                    content: '请谨慎操作',
                                    async onOk() {
                                        const success = await handleRemove(selectedRows);
                                        if (success) {
                                            setSelectedRows([]);
                                            actionRef.current?.reloadAndRest?.();
                                        }
                                    },
                                    onCancel() { },
                                });
                            }, children: [_jsx(DeleteOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.delete", defaultMessage: "\u5220\u9664" })] }, "remove"),
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:role:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                    ], request: (params) => getRoleList({ ...params }).then((res) => {
                        const result = {
                            data: res.rows,
                            total: res.total,
                            success: true,
                        };
                        return result;
                    }), columns: columns, rowSelection: {
                        onChange: (_, selectedRows) => {
                            setSelectedRows(selectedRows);
                        },
                    } }, "roleList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('system:role:del'), onClick: async () => {
                        Modal.confirm({
                            title: '删除',
                            content: '确定删除该项吗？',
                            okText: '确认',
                            cancelText: '取消',
                            onOk: async () => {
                                const success = await handleRemove(selectedRows);
                                if (success) {
                                    setSelectedRows([]);
                                    actionRef.current?.reloadAndRest?.();
                                }
                            },
                        });
                    }, children: _jsx(FormattedMessage, { id: "pages.searchTable.batchDeletion", defaultMessage: "\u6279\u91CF\u5220\u9664" }) }, "remove") })), _jsx(UpdateForm, { onSubmit: async (values) => {
                    let success = false;
                    if (values.roleId) {
                        success = await handleUpdate({ ...values });
                    }
                    else {
                        success = await handleAdd({ ...values });
                    }
                    if (success) {
                        setModalVisible(false);
                        setCurrentRow(undefined);
                        if (actionRef.current) {
                            actionRef.current.reload();
                        }
                    }
                }, onCancel: () => {
                    setModalVisible(false);
                    setCurrentRow(undefined);
                }, open: modalVisible, values: currentRow || {}, menuTree: menuTree || [], menuCheckedKeys: menuIds || [], statusOptions: statusOptions }), _jsx(DataScopeForm, { onSubmit: async (values) => {
                    const success = await updateRoleDataScope(values);
                    if (success) {
                        setDataScopeModalOpen(false);
                        setSelectedRows([]);
                        setCurrentRow(undefined);
                        message.success('配置成功。');
                    }
                }, onCancel: () => {
                    setDataScopeModalOpen(false);
                    setSelectedRows([]);
                    setCurrentRow(undefined);
                }, open: dataScopeModalOpen, values: currentRow || {}, deptTree: menuTree || [], deptCheckedKeys: menuIds || [] })] }));
};
export default RoleTableList;
