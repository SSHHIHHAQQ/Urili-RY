import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormTextArea, ProFormRadio, ProFormSelect, ProFormCaptcha, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const JobForm = (props) => {
    const [form] = Form.useForm();
    const { jobGroupOptions, statusOptions } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            jobId: props.values.jobId,
            jobName: props.values.jobName,
            jobGroup: props.values.jobGroup,
            invokeTarget: props.values.invokeTarget,
            cronExpression: props.values.cronExpression,
            misfirePolicy: props.values.misfirePolicy,
            concurrent: props.values.concurrent,
            status: props.values.status,
            createBy: props.values.createBy,
            createTime: props.values.createTime,
            updateBy: props.values.updateBy,
            updateTime: props.values.updateTime,
            remark: props.values.remark,
        });
    }, [form, props]);
    const intl = useIntl();
    const handleOk = () => {
        form.submit();
    };
    const handleCancel = () => {
        props.onCancel();
        form.resetFields();
    };
    const handleFinish = async (values) => {
        props.onSubmit(values);
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'monitor.job.title',
            defaultMessage: '编辑定时任务调度',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "jobId", label: intl.formatMessage({
                        id: 'monitor.job.job_id',
                        defaultMessage: '任务编号',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7F16\u53F7", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7F16\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "jobName", label: intl.formatMessage({
                        id: 'monitor.job.job_name',
                        defaultMessage: '任务名称',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u540D\u79F0", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormSelect, { name: "jobGroup", options: jobGroupOptions, label: intl.formatMessage({
                        id: 'monitor.job.job_group',
                        defaultMessage: '任务组名',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7EC4\u540D", fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7EC4\u540D\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u4EFB\u52A1\u7EC4\u540D\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "invokeTarget", label: intl.formatMessage({
                        id: 'monitor.job.invoke_target',
                        defaultMessage: '调用目标字符串',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u8C03\u7528\u76EE\u6807\u5B57\u7B26\u4E32", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8C03\u7528\u76EE\u6807\u5B57\u7B26\u4E32\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8C03\u7528\u76EE\u6807\u5B57\u7B26\u4E32\uFF01" }),
                        },
                    ] }), _jsx(ProFormCaptcha, { name: "cronExpression", label: intl.formatMessage({
                        id: 'monitor.job.cron_expression',
                        defaultMessage: 'cron执行表达式',
                    }), captchaTextRender: () => "生成表达式", onGetCaptcha: () => {
                        // form.setFieldValue('cronExpression', '0/20 * * * * ?');
                        return new Promise((resolve, reject) => {
                            reject();
                        });
                    } }), _jsx(ProFormRadio.Group, { name: "misfirePolicy", label: intl.formatMessage({
                        id: 'monitor.job.misfire_policy',
                        defaultMessage: '计划执行错误策略',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u8BA1\u5212\u6267\u884C\u9519\u8BEF\u7B56\u7565", valueEnum: {
                        0: '立即执行',
                        1: '执行一次',
                        3: '放弃执行'
                    }, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8BA1\u5212\u6267\u884C\u9519\u8BEF\u7B56\u7565\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8BA1\u5212\u6267\u884C\u9519\u8BEF\u7B56\u7565\uFF01" }),
                        },
                    ], fieldProps: {
                        optionType: "button",
                        buttonStyle: "solid"
                    } }), _jsx(ProFormRadio.Group, { name: "concurrent", label: intl.formatMessage({
                        id: 'monitor.job.concurrent',
                        defaultMessage: '是否并发执行',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u662F\u5426\u5E76\u53D1\u6267\u884C", valueEnum: {
                        0: '允许',
                        1: '禁止',
                    }, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u662F\u5426\u5E76\u53D1\u6267\u884C\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u662F\u5426\u5E76\u53D1\u6267\u884C\uFF01" }),
                        },
                    ], fieldProps: {
                        optionType: "button",
                        buttonStyle: "solid"
                    } }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'monitor.job.status',
                        defaultMessage: '状态',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u72B6\u6001", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01" }),
                        },
                    ] })] }) }));
};
export default JobForm;
