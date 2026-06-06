import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess } from '@umijs/max';
import { Button, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined, UnlockOutlined } from '@ant-design/icons';
import { getLogininforList, removeLogininfor, exportLogininfor, unlockLogininfor, cleanLogininfor } from '@/services/monitor/logininfor';
import DictTag from '@/components/DictTag';
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
        const resp = await removeLogininfor(selectedRows.map((row) => row.infoId).join(','));
        hide();
        if (resp.code === 200) {
            message.success('删除成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
const handleClean = async () => {
    const hide = message.loading('请稍候');
    try {
        const resp = await cleanLogininfor();
        hide();
        if (resp.code === 200) {
            message.success('清空成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
        hide();
        message.error('请求失败，请重试');
        return false;
    }
};
const handleUnlock = async (userName) => {
    const hide = message.loading('正在解锁');
    try {
        const resp = await unlockLogininfor(userName);
        hide();
        if (resp.code === 200) {
            message.success('解锁成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
        hide();
        message.error('解锁失败，请重试');
        return false;
    }
};
/**
 * 导出数据
 *
 * @param id
 */
const handleExport = async () => {
    const hide = message.loading('正在导出');
    try {
        await exportLogininfor();
        hide();
        message.success('导出成功');
        return true;
    }
    catch (error) {
        hide();
        message.error('导出失败，请重试');
        return false;
    }
};
const LogininforTableList = () => {
    const formTableRef = { current: undefined };
    const actionRef = useRef(null);
    const [selectedRows, setSelectedRows] = useState([]);
    const access = useAccess();
    const statusOptions = {
        0: {
            label: '成功',
            key: '0',
            value: '0',
            text: '成功',
            status: 'success',
            listClass: 'success'
        },
        1: {
            label: '失败',
            key: '1',
            value: '1',
            text: '失败',
            status: 'error',
            listClass: 'danger'
        },
    };
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
    }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.info_id", defaultMessage: "\u8BBF\u95EE\u7F16\u53F7" }),
            dataIndex: 'infoId',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.user_name", defaultMessage: "\u7528\u6237\u8D26\u53F7" }),
            dataIndex: 'userName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.ipaddr", defaultMessage: "\u767B\u5F55IP\u5730\u5740" }),
            dataIndex: 'ipaddr',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.login_location", defaultMessage: "\u767B\u5F55\u5730\u70B9" }),
            dataIndex: 'loginLocation',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.browser", defaultMessage: "\u6D4F\u89C8\u5668\u7C7B\u578B" }),
            dataIndex: 'browser',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.os", defaultMessage: "\u64CD\u4F5C\u7CFB\u7EDF" }),
            dataIndex: 'os',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.status", defaultMessage: "\u767B\u5F55\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.msg", defaultMessage: "\u63D0\u793A\u6D88\u606F" }),
            dataIndex: 'msg',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.logininfor.login_time", defaultMessage: "\u8BBF\u95EE\u65F6\u95F4" }),
            dataIndex: 'loginTime',
            valueType: 'dateTime',
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "infoId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('monitor:logininfor:remove'), onClick: async () => {
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
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('monitor:logininfor:remove'), onClick: async () => {
                                Modal.confirm({
                                    title: '是否确认清空所有数据项?',
                                    icon: _jsx(ExclamationCircleOutlined, {}),
                                    content: '请谨慎操作',
                                    async onOk() {
                                        const success = await handleClean();
                                        if (success) {
                                            setSelectedRows([]);
                                            actionRef.current?.reloadAndRest?.();
                                        }
                                    },
                                    onCancel() { },
                                });
                            }, children: [_jsx(DeleteOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.cleanAll", defaultMessage: "\u6E05\u7A7A" })] }, "clean"),
                        _jsxs(Button, { type: "primary", hidden: selectedRows?.length === 0 || !access.hasPerms('monitor:logininfor:unlock'), onClick: async () => {
                                Modal.confirm({
                                    title: '是否确认解锁该用户的数据项?',
                                    icon: _jsx(ExclamationCircleOutlined, {}),
                                    content: '请谨慎操作',
                                    async onOk() {
                                        const success = await handleUnlock(selectedRows[0].userName);
                                        if (success) {
                                            setSelectedRows([]);
                                            actionRef.current?.reloadAndRest?.();
                                        }
                                    },
                                    onCancel() { },
                                });
                            }, children: [_jsx(UnlockOutlined, {}), _jsx(FormattedMessage, { id: "monitor.logininfor.unlock", defaultMessage: "\u89E3\u9501" })] }, "unlock"),
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('monitor:logininfor:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                    ], request: (params) => getLogininforList({ ...params }).then((res) => {
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
                    } }, "logininforList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('monitor:logininfor:remove'), onClick: async () => {
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
                    }, children: _jsx(FormattedMessage, { id: "pages.searchTable.batchDeletion", defaultMessage: "\u6279\u91CF\u5220\u9664" }) }, "remove") }))] }));
};
export default LogininforTableList;
