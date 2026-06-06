import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormTextArea, ProFormRadio, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
const ConfigForm = (props) => {
    const [form] = Form.useForm();
    const { configTypeOptions } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            configId: props.values.configId,
            configName: props.values.configName,
            configKey: props.values.configKey,
            configValue: props.values.configValue,
            configType: props.values.configType,
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
    };
    const handleFinish = async (values) => {
        props.onSubmit(values);
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'system.config.title',
            defaultMessage: '编辑参数配置',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "configId", label: intl.formatMessage({
                        id: 'system.config.config_id',
                        defaultMessage: '参数主键',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u53C2\u6570\u4E3B\u952E", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u53C2\u6570\u4E3B\u952E\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u53C2\u6570\u4E3B\u952E\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "configName", label: intl.formatMessage({
                        id: 'system.config.config_name',
                        defaultMessage: '参数名称',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u53C2\u6570\u540D\u79F0", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u53C2\u6570\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u53C2\u6570\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "configKey", label: intl.formatMessage({
                        id: 'system.config.config_key',
                        defaultMessage: '参数键名',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u53C2\u6570\u952E\u540D", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u53C2\u6570\u952E\u540D\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u53C2\u6570\u952E\u540D\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "configValue", label: intl.formatMessage({
                        id: 'system.config.config_value',
                        defaultMessage: '参数键值',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u53C2\u6570\u952E\u503C", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u53C2\u6570\u952E\u503C\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u53C2\u6570\u952E\u503C\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: configTypeOptions, name: "configType", label: intl.formatMessage({
                        id: 'system.config.config_type',
                        defaultMessage: '系统内置',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u7CFB\u7EDF\u5185\u7F6E", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7CFB\u7EDF\u5185\u7F6E\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7CFB\u7EDF\u5185\u7F6E\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "remark", label: intl.formatMessage({
                        id: 'system.config.remark',
                        defaultMessage: '备注',
                    }), colProps: { md: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default ConfigForm;
