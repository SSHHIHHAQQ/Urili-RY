import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess } from '@umijs/max';
import { Button, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getOperlogList, removeOperlog, addOperlog, updateOperlog, cleanAllOperlog, exportOperlog } from '@/services/monitor/operlog';
import UpdateForm from './detail';
import { getDictValueEnum } from '@/services/system/dict';
import DictTag from '@/components/DictTag';
/**
 * 添加节点
 *
 * @param fields
 */
const handleAdd = async (fields) => {
    const hide = message.loading('正在添加');
    try {
        const resp = await addOperlog({ ...fields });
        hide();
        if (resp.code === 200) {
            message.success('添加成功');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
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
        const resp = await updateOperlog(fields);
        hide();
        if (resp.code === 200) {
            message.success('更新成功');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
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
        const resp = await removeOperlog(selectedRows.map((row) => row.operId).join(','));
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
/**
 * 清空所有记录
 *
 */
const handleCleanAll = async () => {
    const hide = message.loading('正在清空');
    try {
        const resp = await cleanAllOperlog();
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
        message.error('清空失败，请重试');
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
        await exportOperlog();
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
const OperlogTableList = () => {
    const formTableRef = { current: undefined };
    const [modalVisible, setModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [businessTypeOptions, setBusinessTypeOptions] = useState([]);
    const [operatorTypeOptions, setOperatorTypeOptions] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
        getDictValueEnum('sys_oper_type', true).then((data) => {
            setBusinessTypeOptions(data);
        });
        getDictValueEnum('sys_oper_type', true).then((data) => {
            setOperatorTypeOptions(data);
        });
        getDictValueEnum('sys_common_status', true).then((data) => {
            setStatusOptions(data);
        });
    }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.oper_id", defaultMessage: "\u65E5\u5FD7\u4E3B\u952E" }),
            dataIndex: 'operId',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.title", defaultMessage: "\u64CD\u4F5C\u6A21\u5757" }),
            dataIndex: 'title',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.business_type", defaultMessage: "\u4E1A\u52A1\u7C7B\u578B" }),
            dataIndex: 'businessType',
            valueType: 'select',
            valueEnum: businessTypeOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: businessTypeOptions, value: record.businessType }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.request_method", defaultMessage: "\u8BF7\u6C42\u65B9\u5F0F" }),
            dataIndex: 'requestMethod',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.operator_type", defaultMessage: "\u64CD\u4F5C\u7C7B\u522B" }),
            dataIndex: 'operatorType',
            valueType: 'select',
            valueEnum: operatorTypeOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: operatorTypeOptions, value: record.operatorType }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.oper_name", defaultMessage: "\u64CD\u4F5C\u4EBA\u5458" }),
            dataIndex: 'operName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.oper_ip", defaultMessage: "\u4E3B\u673A\u5730\u5740" }),
            dataIndex: 'operIp',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.oper_location", defaultMessage: "\u64CD\u4F5C\u5730\u70B9" }),
            dataIndex: 'operLocation',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.status", defaultMessage: "\u64CD\u4F5C\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }, "status"));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.operlog.oper_time", defaultMessage: "\u64CD\u4F5C\u65F6\u95F4" }),
            dataIndex: 'operTime',
            valueType: 'dateTime',
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '120px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('system:operlog:edit'), onClick: () => {
                        setModalVisible(true);
                        setCurrentRow(record);
                    }, children: "\u8BE6\u7EC6" }, "edit"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "operId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:operlog:add'), onClick: async () => {
                                setCurrentRow(undefined);
                                setModalVisible(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:operlog:remove'), onClick: async () => {
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
                        _jsxs(Button, { type: "primary", danger: true, hidden: !access.hasPerms('system:operlog:remove'), onClick: async () => {
                                Modal.confirm({
                                    title: '是否确认清空所有数据项?',
                                    icon: _jsx(ExclamationCircleOutlined, {}),
                                    content: '请谨慎操作',
                                    async onOk() {
                                        const success = await handleCleanAll();
                                        if (success) {
                                            setSelectedRows([]);
                                            actionRef.current?.reloadAndRest?.();
                                        }
                                    },
                                    onCancel() { },
                                });
                            }, children: [_jsx(DeleteOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.cleanAll", defaultMessage: "\u6E05\u7A7A" })] }, "clean"),
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:operlog:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                    ], request: (params) => getOperlogList({ ...params }).then((res) => {
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
                    } }, "operlogList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('system:operlog:del'), onClick: async () => {
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
                    if (values.operId) {
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
                }, open: modalVisible, values: currentRow || {}, businessTypeOptions: businessTypeOptions, operatorTypeOptions: operatorTypeOptions, statusOptions: statusOptions })] }));
};
export default OperlogTableList;
