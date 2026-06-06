import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess, history } from '@umijs/max';
import { Button, Dropdown, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, DownOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getJobList, removeJob, addJob, updateJob, exportJob, runJob } from '@/services/monitor/job';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import UpdateForm from './edit';
import DetailForm from './detail';
import DictTag from '@/components/DictTag';
/**
 * 定时任务调度 List Page
 *
 * @author whiteshader
 * @date 2023-02-07
 */
/**
 * 添加节点
 *
 * @param fields
 */
const handleAdd = async (fields) => {
    const hide = message.loading('正在添加');
    try {
        const resp = await addJob({ ...fields });
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
        const resp = await updateJob(fields);
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
        const resp = await removeJob(selectedRows.map((row) => row.jobId).join(','));
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
        const params = [selectedRow.jobId];
        const resp = await removeJob(params.join(','));
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
 */
const handleExport = async () => {
    const hide = message.loading('正在导出');
    try {
        await exportJob();
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
const JobTableList = () => {
    const formTableRef = { current: undefined };
    const [modalVisible, setModalVisible] = useState(false);
    const [detailModalVisible, setDetailModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [jobGroupOptions, setJobGroupOptions] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
        getDictSelectOption('sys_job_group').then((data) => {
            setJobGroupOptions(data);
        });
        getDictValueEnum('sys_normal_disable').then((data) => {
            setStatusOptions(data);
        });
    }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.job_id", defaultMessage: "\u4EFB\u52A1\u7F16\u53F7" }),
            dataIndex: 'jobId',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.job_name", defaultMessage: "\u4EFB\u52A1\u540D\u79F0" }),
            dataIndex: 'jobName',
            valueType: 'text',
            render: (dom, record) => {
                return (_jsx("a", { onClick: () => {
                        setDetailModalVisible(true);
                        setCurrentRow(record);
                    }, children: dom }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.job_group", defaultMessage: "\u4EFB\u52A1\u7EC4\u540D" }),
            dataIndex: 'jobGroup',
            valueType: 'text',
            valueEnum: jobGroupOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { options: jobGroupOptions, value: record.jobGroup }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.invoke_target", defaultMessage: "\u8C03\u7528\u76EE\u6807\u5B57\u7B26\u4E32" }),
            dataIndex: 'invokeTarget',
            valueType: 'textarea',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.cron_expression", defaultMessage: "cron\u6267\u884C\u8868\u8FBE\u5F0F" }),
            dataIndex: 'cronExpression',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.status", defaultMessage: "\u72B6\u6001" }),
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
            width: '220px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('monitor:job:edit'), onClick: () => {
                        setModalVisible(true);
                        setCurrentRow(record);
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('monitor:job:remove'), onClick: async () => {
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
                                label: '执行一次',
                                key: 'runOnce',
                            },
                            {
                                label: '详细',
                                key: 'detail',
                            },
                            {
                                label: '历史',
                                key: 'log',
                            },
                        ],
                        onClick: ({ key }) => {
                            if (key === 'runOnce') {
                                Modal.confirm({
                                    title: '警告',
                                    content: '确认要立即执行一次？',
                                    okText: '确认',
                                    cancelText: '取消',
                                    onOk: async () => {
                                        const success = await runJob(record.jobId, record.jobGroup);
                                        if (success) {
                                            message.success('执行成功');
                                        }
                                    },
                                });
                            }
                            else if (key === 'detail') {
                                setDetailModalVisible(true);
                                setCurrentRow(record);
                            }
                            else if (key === 'log') {
                                history.push(`/monitor/job-log/index/${record.jobId}`);
                            }
                        }
                    }, children: _jsxs("a", { className: "ant-dropdown-link", onClick: (e) => e.preventDefault(), children: ["\u66F4\u591A ", _jsx(DownOutlined, { style: { fontSize: 10 } })] }) }, "more"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "jobId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('monitor:job:add'), onClick: async () => {
                                setCurrentRow(undefined);
                                setModalVisible(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('monitor:job:remove'), onClick: async () => {
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
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('monitor:job:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                    ], request: (params) => getJobList({ ...params }).then((res) => {
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
                    } }, "jobList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('monitor:job:del'), onClick: async () => {
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
                    if (values.jobId) {
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
                }, open: modalVisible, values: currentRow || {}, jobGroupOptions: jobGroupOptions || {}, statusOptions: statusOptions }), _jsx(DetailForm, { onCancel: () => {
                    setDetailModalVisible(false);
                    setCurrentRow(undefined);
                }, open: detailModalVisible, values: currentRow || {}, statusOptions: statusOptions })] }));
};
export default JobTableList;
