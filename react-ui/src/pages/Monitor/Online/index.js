import { jsx as _jsx } from "react/jsx-runtime";
import { Button, Modal } from 'antd';
import { useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess } from '@umijs/max';
import { getOnlineUserList, forceLogout } from '@/services/monitor/online';
import { ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { message } from '@/utils/feedback';
/* *
 *
 * @author whiteshader@163.com
 * @datetime  2023/02/07
 *
 * */
const handleForceLogout = async (selectedRow) => {
    const hide = message.loading('正在强制下线');
    try {
        await forceLogout(selectedRow.tokenId);
        hide();
        message.success('强制下线成功，即将刷新');
        return true;
    }
    catch {
        hide();
        message.error('强制下线失败，请重试');
        return false;
    }
};
const OnlineUserTableList = () => {
    const formTableRef = { current: undefined };
    const actionRef = useRef(null);
    const access = useAccess();
    const intl = useIntl();
    useEffect(() => { }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.token_id", defaultMessage: "\u4F1A\u8BDD\u7F16\u53F7" }),
            dataIndex: 'tokenId',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.user_name", defaultMessage: "\u7528\u6237\u8D26\u53F7" }),
            dataIndex: 'userName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.dept_name", defaultMessage: "\u90E8\u95E8\u540D\u79F0" }),
            dataIndex: 'deptName',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.ipaddr", defaultMessage: "\u767B\u5F55IP\u5730\u5740" }),
            dataIndex: 'ipaddr',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.login_location", defaultMessage: "\u767B\u5F55\u5730\u70B9" }),
            dataIndex: 'loginLocation',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.browser", defaultMessage: "\u6D4F\u89C8\u5668\u7C7B\u578B" }),
            dataIndex: 'browser',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.os", defaultMessage: "\u64CD\u4F5C\u7CFB\u7EDF" }),
            dataIndex: 'os',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.online.user.login_time", defaultMessage: "\u767B\u5F55\u65F6\u95F4" }),
            dataIndex: 'loginTime',
            colSize: 2,
            valueType: 'dateRange',
            render: (_, record) => _jsx("span", { children: record.loginTime }),
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
            width: '60px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('monitor:online:forceLogout'), onClick: async () => {
                        Modal.confirm({
                            title: '强踢',
                            content: '确定强制将该用户踢下线吗？',
                            okText: '确认',
                            cancelText: '取消',
                            onOk: async () => {
                                const success = await handleForceLogout(record);
                                if (success) {
                                    if (actionRef.current) {
                                        actionRef.current.reload();
                                    }
                                }
                            },
                        });
                    }, children: "\u5F3A\u9000" }, "batchRemove"),
            ],
        },
    ];
    return (_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                id: 'pages.searchTable.title',
                defaultMessage: '信息',
            }), actionRef: actionRef, formRef: formTableRef, rowKey: "tokenId", search: getPersistedProTableSearch({ labelWidth: 120 }), request: (params) => getOnlineUserList({ ...params }).then((res) => {
                const result = {
                    data: res.rows,
                    total: res.total,
                    success: true,
                };
                return result;
            }), columns: columns }, "logininforList") }));
};
export default OnlineUserTableList;
