import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { Modal, Descriptions, Button } from 'antd';
import { FormattedMessage, useIntl } from '@umijs/max';
import { getValueEnumLabel } from '@/utils/options';
const OperlogForm = (props) => {
    const { values, statusOptions } = props;
    useEffect(() => { }, [props]);
    const intl = useIntl();
    const misfirePolicy = {
        '0': '默认策略',
        '1': '立即执行',
        '2': '执行一次',
        '3': '放弃执行',
    };
    const handleCancel = () => {
        props.onCancel();
    };
    return (_jsx(Modal, { width: 800, title: intl.formatMessage({
            id: 'monitor.job.detail',
            defaultMessage: '操作日志详细信息',
        }), open: props.open, destroyOnHidden: true, onCancel: handleCancel, footer: [
            _jsx(Button, { onClick: handleCancel, children: "\u5173\u95ED" }, "back"),
        ], children: _jsxs(Descriptions, { column: 24, children: [_jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.job_id", defaultMessage: "\u4EFB\u52A1\u7F16\u53F7" }), children: values.jobId }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.job_name", defaultMessage: "\u4EFB\u52A1\u540D\u79F0" }), children: values.jobName }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.job_group", defaultMessage: "\u4EFB\u52A1\u7EC4\u540D" }), children: values.jobGroup }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.concurrent", defaultMessage: "\u662F\u5426\u5E76\u53D1\u6267\u884C" }), children: values.concurrent === '1' ? '禁止' : '允许' }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.misfire_policy", defaultMessage: "\u8BA1\u5212\u6267\u884C\u9519\u8BEF\u7B56\u7565" }), children: misfirePolicy[values.misfirePolicy ? values.misfirePolicy : '0'] }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.create_time", defaultMessage: "\u521B\u5EFA\u65F6\u95F4" }), children: values.createTime?.toString() }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.status", defaultMessage: "\u72B6\u6001" }), children: getValueEnumLabel(statusOptions, values.status, '未知') }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.job.next_valid_time", defaultMessage: "\u4E0B\u6B21\u6267\u884C\u65F6\u95F4" }), children: values.nextValidTime }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.job.cron_expression", defaultMessage: "cron\u6267\u884C\u8868\u8FBE\u5F0F" }), children: values.cronExpression }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.job.invoke_target", defaultMessage: "\u8C03\u7528\u76EE\u6807\u5B57\u7B26\u4E32" }), children: values.invokeTarget })] }) }));
};
export default OperlogForm;
