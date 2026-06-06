import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { getValueEnumLabel } from '@/utils/options';
import { FormattedMessage, useIntl } from '@umijs/max';
import { Descriptions, Modal } from 'antd';
import { useEffect } from 'react';
const JobLogDetailForm = (props) => {
    const { values, statusOptions, jobGroupOptions } = props;
    useEffect(() => {
    }, []);
    const intl = useIntl();
    const handleOk = () => {
    };
    const handleCancel = () => {
        props.onCancel();
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'monitor.job.log.title',
            defaultMessage: '定时任务调度日志',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(Descriptions, { column: 24, children: [_jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.job_id", defaultMessage: "\u4EFB\u52A1\u7F16\u53F7" }), children: values.jobLogId }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.create_time", defaultMessage: "\u6267\u884C\u65F6\u95F4" }), children: values.createTime?.toString() }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.job_name", defaultMessage: "\u4EFB\u52A1\u540D\u79F0" }), children: values.jobName }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.job_group", defaultMessage: "\u4EFB\u52A1\u7EC4\u540D" }), children: getValueEnumLabel(jobGroupOptions, values.jobGroup, '无') }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.job.invoke_target", defaultMessage: "\u8C03\u7528\u76EE\u6807" }), children: values.invokeTarget }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.job.log.job_message", defaultMessage: "\u65E5\u5FD7\u4FE1\u606F" }), children: values.jobMessage }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.job.log.exception_info", defaultMessage: "\u5F02\u5E38\u4FE1\u606F" }), children: values.exceptionInfo }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.status", defaultMessage: "\u6267\u884C\u72B6\u6001" }), children: getValueEnumLabel(statusOptions, values.status, '未知') })] }) }));
};
export default JobLogDetailForm;
