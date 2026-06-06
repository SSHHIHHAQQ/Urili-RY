import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useRef, useState } from 'react';
import { Modal } from 'antd';
import { FormattedMessage, useIntl } from '@umijs/max';
import { ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { getDictValueEnum } from '@/services/system/dict';
import DictTag from '@/components/DictTag';
const UserSelectorModal = (props) => {
    const actionRef = useRef(null);
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    useEffect(() => {
        getDictValueEnum('sys_normal_disable').then((data) => {
            setStatusOptions(data);
        });
    }, [props]);
    const intl = useIntl();
    const handleOk = () => {
        props.onSubmit(selectedRowKeys);
    };
    const handleCancel = () => {
        props.onCancel();
    };
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "system.user.user_id", defaultMessage: "\u7528\u6237\u7F16\u53F7" }),
            dataIndex: 'userId',
            valueType: 'text',
            search: false,
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
            search: false,
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
            search: false,
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "system.user.create_time", defaultMessage: "\u521B\u5EFA\u65F6\u95F4" }),
            dataIndex: 'createTime',
            valueType: 'dateRange',
            search: false,
            render: (_, record) => {
                return (_jsxs("span", { children: [record.createTime.toString(), " "] }));
            },
        }
    ];
    return (_jsx(Modal, { width: 800, title: intl.formatMessage({
            id: 'system.role.auth.user',
            defaultMessage: '选择用户',
        }), open: props.open, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                id: 'pages.searchTable.title',
                defaultMessage: '信息',
            }), actionRef: actionRef, rowKey: "userId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolbar: {}, params: props.params, request: props.request, columns: columns, rowSelection: {
                onChange: (selectedRowKeys) => {
                    setSelectedRowKeys(selectedRowKeys);
                },
            } }, "userList") }));
};
export default UserSelectorModal;
