import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess, useParams, history } from '@umijs/max';
import { Button, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getJobLogList, removeJobLog, exportJobLog } from '@/services/monitor/jobLog';
import DetailForm from './detail';
import { getDictValueEnum } from '@/services/system/dict';
import { getJob } from '@/services/monitor/job';
import DictTag from '@/components/DictTag';
/**
 * 定时任务调度日志 List Page
 *
 * @author whiteshader
 * @date 2023-02-07
 */
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
        const resp = await removeJobLog(selectedRows.map((row) => row.jobLogId).join(','));
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
const handleRemoveOne = async (selectedRow) => {
    const hide = message.loading('正在删除');
    if (!selectedRow)
        return true;
    try {
        const params = [selectedRow.jobLogId];
        const resp = await removeJobLog(params.join(','));
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
 * 清空日志数据
 *
 */
const handleExport = async () => {
    const hide = message.loading('正在导出');
    try {
        await exportJobLog();
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
const JobLogTableList = () => {
    const formTableRef = { current: undefined };
    const [modalOpen, setModalOpen] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [jobGroupOptions, setJobGroupOptions] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const [queryParams, setQueryParams] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    const params = useParams();
    if (params.id === undefined) {
        history.push('/monitor/job');
    }
    const jobId = params.id || 0;
    useEffect(() => {
        if (jobId !== undefined && jobId !== 0) {
            getJob(Number(jobId)).then(response => {
                setQueryParams({
                    jobName: response.data.jobName,
                    jobGroup: response.data.jobGroup
                });
            });
        }
        getDictValueEnum('sys_job_status').then((data) => {
            setStatusOptions(data);
        });
        getDictValueEnum('sys_job_group').then((data) => {
            setJobGroupOptions(data);
        });
    }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.job_log_id", defaultMessage: "\u4EFB\u52A1\u65E5\u5FD7\u7F16\u53F7" }),
            dataIndex: 'jobLogId',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.job_name", defaultMessage: "\u4EFB\u52A1\u540D\u79F0" }),
            dataIndex: 'jobName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.job_group", defaultMessage: "\u4EFB\u52A1\u7EC4\u540D" }),
            dataIndex: 'jobGroup',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.invoke_target", defaultMessage: "\u8C03\u7528\u76EE\u6807\u5B57\u7B26\u4E32" }),
            dataIndex: 'invokeTarget',
            valueType: 'textarea',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.job_message", defaultMessage: "\u65E5\u5FD7\u4FE1\u606F" }),
            dataIndex: 'jobMessage',
            valueType: 'textarea',
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.status", defaultMessage: "\u6267\u884C\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "monitor.job.log.create_time", defaultMessage: "\u5F02\u5E38\u4FE1\u606F" }),
            dataIndex: 'createTime',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '120px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('monitor:job-log:edit'), onClick: () => {
                        setModalOpen(true);
                        setCurrentRow(record);
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('monitor:job-log:remove'), onClick: async () => {
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
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "jobLogId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('monitor:job-log:add'), onClick: async () => {
                                setCurrentRow(undefined);
                                setModalOpen(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('monitor:job-log:remove'), onClick: async () => {
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
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('monitor:job-log:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                    ], params: queryParams, request: (params) => getJobLogList({ ...params }).then((res) => {
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
                    } }, "job-logList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('monitor:job-log:del'), onClick: async () => {
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
                    }, children: _jsx(FormattedMessage, { id: "pages.searchTable.batchDeletion", defaultMessage: "\u6279\u91CF\u5220\u9664" }) }, "remove") })), _jsx(DetailForm, { onCancel: () => {
                    setModalOpen(false);
                    setCurrentRow(undefined);
                }, open: modalOpen, values: currentRow || {}, statusOptions: statusOptions, jobGroupOptions: jobGroupOptions })] }));
};
export default JobLogTableList;
