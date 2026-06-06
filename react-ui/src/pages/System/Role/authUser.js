import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess, history, useParams } from '@umijs/max';
import { Button, Modal } from 'antd';
import { PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined, RollbackOutlined } from '@ant-design/icons';
import { authUserSelectAll, authUserCancel, authUserCancelAll, allocatedUserList, unallocatedUserList } from '@/services/system/role';
import { getDictValueEnum } from '@/services/system/dict';
import DictTag from '@/components/DictTag';
import UserSelectorModal from './components/UserSelectorModal';
import { HttpResult } from '@/enums/httpEnum';
/**
 * 删除节点
 *
 * @param selectedRows
 */
const cancelAuthUserAll = async (roleId, selectedRows) => {
    const hide = message.loading('正在取消授权');
    if (!selectedRows)
        return true;
    try {
        const userIds = selectedRows.map((row) => row.userId).join(',');
        const resp = await authUserCancelAll({ roleId, userIds });
        hide();
        if (resp.code === 200) {
            message.success('取消授权成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch {
        hide();
        message.error('取消授权失败，请重试');
        return false;
    }
};
const cancelAuthUser = async (roleId, userId) => {
    const hide = message.loading('正在取消授权');
    try {
        const resp = await authUserCancel({ userId, roleId });
        hide();
        if (resp.code === 200) {
            message.success('取消授权成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch {
        hide();
        message.error('取消授权失败，请重试');
        return false;
    }
};
const AuthUserTableList = () => {
    const [modalVisible, setModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [selectedRows, setSelectedRows] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    const params = useParams();
    if (params.id === undefined) {
        history.back();
    }
    const roleId = params.id || '0';
    useEffect(() => {
        getDictValueEnum('sys_normal_disable').then((data) => {
            setStatusOptions(data);
        });
    }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "system.user.user_id", defaultMessage: "\u7528\u6237\u7F16\u53F7" }),
            dataIndex: 'deptId',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.user_name", defaultMessage: "\u7528\u6237\u8D26\u53F7" }),
            dataIndex: 'userName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.nick_name", defaultMessage: "\u7528\u6237\u6635\u79F0" }),
            dataIndex: 'nickName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.phonenumber", defaultMessage: "\u624B\u673A\u53F7\u7801" }),
            dataIndex: 'phonenumber',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.role.create_time", defaultMessage: "\u521B\u5EFA\u65F6\u95F4" }),
            dataIndex: 'createTime',
            valueType: 'dateRange',
            render: (_, record) => {
                return (_jsxs("span", { children: [record.createTime.toString(), " "] }));
            },
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.status", defaultMessage: "\u5E10\u53F7\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '60px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('system:role:remove'), onClick: async () => {
                        Modal.confirm({
                            title: '删除',
                            content: `确认要取消该用户${record.userName}的角色授权吗？`,
                            okText: '确认',
                            cancelText: '取消',
                            onOk: async () => {
                                const success = await cancelAuthUser(roleId, record.userId);
                                if (success) {
                                    if (actionRef.current) {
                                        actionRef.current.reload();
                                    }
                                }
                            },
                        });
                    }, children: "\u53D6\u6D88\u6388\u6743" }, "remove"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, rowKey: "userId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:role:add'), onClick: async () => {
                                setModalVisible(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "system.role.auth.addUser", defaultMessage: "\u6DFB\u52A0\u7528\u6237" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:role:remove'), onClick: async () => {
                                Modal.confirm({
                                    title: '是否确认删除所选数据项?',
                                    icon: _jsx(ExclamationCircleOutlined, {}),
                                    content: '请谨慎操作',
                                    async onOk() {
                                        const success = await cancelAuthUserAll(roleId, selectedRows);
                                        if (success) {
                                            setSelectedRows([]);
                                            actionRef.current?.reloadAndRest?.();
                                        }
                                    },
                                    onCancel() { },
                                });
                            }, children: [_jsx(DeleteOutlined, {}), _jsx(FormattedMessage, { id: "system.role.auth.cancelAll", defaultMessage: "\u6279\u91CF\u53D6\u6D88\u6388\u6743" })] }, "remove"),
                        _jsxs(Button, { type: "primary", onClick: async () => {
                                history.back();
                            }, children: [_jsx(RollbackOutlined, {}), _jsx(FormattedMessage, { id: "pages.goback", defaultMessage: "\u8FD4\u56DE" })] }, "back"),
                    ], request: (params) => allocatedUserList({ ...params, roleId }).then((res) => {
                        const rows = res?.rows ?? [];
                        const result = {
                            data: rows,
                            total: res?.total ?? rows.length,
                            success: true,
                        };
                        return result;
                    }), columns: columns, rowSelection: {
                        onChange: (_, selectedRows) => {
                            setSelectedRows(selectedRows);
                        },
                    } }, "userList") }), _jsx(UserSelectorModal, { open: modalVisible, onSubmit: (values) => {
                    const userIds = values.join(",");
                    if (userIds === "") {
                        message.warning("请选择要分配的用户");
                        return;
                    }
                    authUserSelectAll({ roleId: roleId, userIds: userIds }).then(resp => {
                        if (resp.code === HttpResult.SUCCESS) {
                            message.success('更新成功！');
                            if (actionRef.current) {
                                actionRef.current.reload();
                            }
                        }
                        else {
                            message.warning(resp.msg);
                        }
                    });
                    setModalVisible(false);
                }, onCancel: () => {
                    setModalVisible(false);
                }, params: { roleId }, request: (params) => unallocatedUserList({ ...params }).then((res) => {
                    const rows = res?.rows ?? [];
                    const result = {
                        data: rows,
                        total: res?.total ?? rows.length,
                        success: true,
                    };
                    return result;
                }) })] }));
};
export default AuthUserTableList;
