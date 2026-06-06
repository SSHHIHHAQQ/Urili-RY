import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess } from '@umijs/max';
import { App, Button, Card, Col, Dropdown, Modal, Row, Switch } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, DownOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getUserList, removeUser, addUser, updateUser, exportUser, getUser, changeUserStatus, updateAuthRole, resetUserPwd } from '@/services/system/user';
import UpdateForm from './edit';
import { getDictValueEnum } from '@/services/system/dict';
import { getDeptTree } from '@/services/system/user';
import DeptTree from './components/DeptTree';
import ResetPwd from './components/ResetPwd';
import { getPostList } from '@/services/system/post';
import { getRoleList } from '@/services/system/role';
import AuthRoleForm from './components/AuthRole';
/* *
 *
 * @author whiteshader@163.com
 * @datetime  2023/02/06
 *
 * */
const UserTableList = () => {
    const { message } = App.useApp();
    const handleAdd = async (fields) => {
        const hide = message.loading('正在添加');
        try {
            await addUser({ ...fields });
            hide();
            message.success('添加成功');
            return true;
        }
        catch {
            hide();
            message.error('添加失败请重试！');
            return false;
        }
    };
    const handleUpdate = async (fields) => {
        const hide = message.loading('正在配置');
        try {
            await updateUser(fields);
            hide();
            message.success('配置成功');
            return true;
        }
        catch {
            hide();
            message.error('配置失败请重试！');
            return false;
        }
    };
    const handleRemove = async (selectedRows) => {
        const hide = message.loading('正在删除');
        if (!selectedRows)
            return true;
        try {
            await removeUser(selectedRows.map((row) => row.userId).join(','));
            hide();
            message.success('删除成功，即将刷新');
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
            const params = [selectedRow.userId];
            await removeUser(params.join(','));
            hide();
            message.success('删除成功，即将刷新');
            return true;
        }
        catch {
            hide();
            message.error('删除失败，请重试');
            return false;
        }
    };
    const handleExport = async () => {
        const hide = message.loading('正在导出');
        try {
            await exportUser();
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
    const formTableRef = { current: undefined };
    const [modalVisible, setModalVisible] = useState(false);
    const [resetPwdModalVisible, setResetPwdModalVisible] = useState(false);
    const [authRoleModalVisible, setAuthRoleModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [selectDept, setSelectDept] = useState({ id: 0 });
    const [sexOptions, setSexOptions] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const [postIds, setPostIds] = useState();
    const [postList, setPostList] = useState();
    const [roleIds, setRoleIds] = useState();
    const [roleList, setRoleList] = useState();
    const [deptTree, setDeptTree] = useState();
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
        getDictValueEnum('sys_user_sex').then((data) => {
            setSexOptions(data);
        });
        getDictValueEnum('sys_normal_disable').then((data) => {
            setStatusOptions(data);
        });
    }, []);
    const showChangeStatusConfirm = (record) => {
        const text = record.status === "1" ? "启用" : "停用";
        const newStatus = record.status === '0' ? '1' : '0';
        Modal.confirm({
            title: `确认要${text}${record.userName}用户吗？`,
            onOk() {
                changeUserStatus(record.userId, newStatus).then(resp => {
                    if (resp.code === 200) {
                        message.success({
                            type: 'success',
                            content: '更新成功！',
                        });
                        actionRef.current?.reload();
                    }
                    else {
                        message.success({
                            type: 'error',
                            content: '更新失败！',
                        });
                    }
                });
            },
        });
    };
    const fetchUserInfo = async (userId) => {
        const res = await getUser(userId);
        setPostIds(res.postIds);
        setPostList(res.posts.map((item) => {
            return {
                value: item.postId,
                label: item.postName,
            };
        }));
        setRoleIds(res.roleIds);
        setRoleList(res.roles.map((item) => {
            return {
                value: item.roleId,
                label: item.roleName,
            };
        }));
    };
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
            title: _jsx(FormattedMessage, { id: "system.user.dept_name", defaultMessage: "\u90E8\u95E8" }),
            dataIndex: ['dept', 'deptName'],
            valueType: 'text',
            search: false
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.phonenumber", defaultMessage: "\u624B\u673A\u53F7\u7801" }),
            dataIndex: 'phonenumber',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.status", defaultMessage: "\u5E10\u53F7\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(Switch, { checked: record.status === '0', checkedChildren: "\u6B63\u5E38", unCheckedChildren: "\u505C\u7528", defaultChecked: true, onClick: () => showChangeStatusConfirm(record) }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '220px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('system:user:edit'), onClick: async () => {
                        fetchUserInfo(record.userId);
                        const treeData = await getDeptTree({});
                        setDeptTree(treeData);
                        setModalVisible(true);
                        setCurrentRow(record);
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('system:user:remove'), onClick: async () => {
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
                                label: _jsx(FormattedMessage, { id: "system.user.reset.password", defaultMessage: "\u5BC6\u7801\u91CD\u7F6E" }),
                                key: 'reset',
                                disabled: !access.hasPerms('system:user:edit'),
                            },
                            {
                                label: '分配角色',
                                key: 'authRole',
                                disabled: !access.hasPerms('system:user:edit'),
                            },
                        ],
                        onClick: ({ key }) => {
                            if (key === 'reset') {
                                setResetPwdModalVisible(true);
                                setCurrentRow(record);
                            }
                            else if (key === 'authRole') {
                                fetchUserInfo(record.userId);
                                setAuthRoleModalVisible(true);
                                setCurrentRow(record);
                            }
                        }
                    }, children: _jsxs("a", { onClick: (e) => e.preventDefault(), children: ["\u66F4\u591A ", _jsx(DownOutlined, { style: { fontSize: 10 } })] }) }, "more"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsxs(Row, { gutter: [16, 24], children: [_jsx(Col, { lg: 6, md: 24, children: _jsx(Card, { children: _jsx(DeptTree, { onSelect: async (value) => {
                                    setSelectDept(value);
                                    if (actionRef.current) {
                                        formTableRef?.current?.submit();
                                    }
                                } }) }) }), _jsx(Col, { lg: 18, md: 24, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                                id: 'pages.searchTable.title',
                                defaultMessage: '信息',
                            }), actionRef: actionRef, formRef: formTableRef, rowKey: "userId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                                _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:user:add'), onClick: async () => {
                                        const treeData = await getDeptTree({});
                                        setDeptTree(treeData);
                                        const postResp = await getPostList();
                                        if (postResp.code === 200) {
                                            setPostList(postResp.rows.map((item) => {
                                                return {
                                                    value: item.postId,
                                                    label: item.postName,
                                                };
                                            }));
                                        }
                                        const roleResp = await getRoleList();
                                        if (roleResp.code === 200) {
                                            setRoleList(roleResp.rows.map((item) => {
                                                return {
                                                    value: item.roleId,
                                                    label: item.roleName,
                                                };
                                            }));
                                        }
                                        setCurrentRow(undefined);
                                        setModalVisible(true);
                                    }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                                _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:user:remove'), onClick: async () => {
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
                                _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:user:export'), onClick: async () => {
                                        handleExport();
                                    }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                            ], request: (params) => getUserList({ ...params, deptId: selectDept.id }).then((res) => {
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
                            } }, "userList") })] }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('system:user:del'), onClick: async () => {
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
                    if (values.userId) {
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
                }, open: modalVisible, values: currentRow || {}, sexOptions: sexOptions, statusOptions: statusOptions, posts: postList || [], postIds: postIds || [], roles: roleList || [], roleIds: roleIds || [], depts: deptTree || [] }), _jsx(ResetPwd, { onSubmit: async (values) => {
                    const success = await resetUserPwd(values.userId, values.password);
                    if (success) {
                        setResetPwdModalVisible(false);
                        setSelectedRows([]);
                        setCurrentRow(undefined);
                        message.success('密码重置成功。');
                    }
                }, onCancel: () => {
                    setResetPwdModalVisible(false);
                    setSelectedRows([]);
                    setCurrentRow(undefined);
                }, open: resetPwdModalVisible, values: currentRow || {} }), _jsx(AuthRoleForm, { onSubmit: async (values) => {
                    const success = await updateAuthRole(values);
                    if (success) {
                        setAuthRoleModalVisible(false);
                        setSelectedRows([]);
                        setCurrentRow(undefined);
                        message.success('配置成功。');
                    }
                }, onCancel: () => {
                    setAuthRoleModalVisible(false);
                    setSelectedRows([]);
                    setCurrentRow(undefined);
                }, open: authRoleModalVisible, roles: roleList || [], roleIds: roleIds || [] })] }));
};
export default UserTableList;
