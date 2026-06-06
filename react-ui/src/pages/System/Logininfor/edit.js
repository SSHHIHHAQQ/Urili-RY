import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormRadio, ProFormTimePicker, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
const LogininforForm = (props) => {
    const [form] = Form.useForm();
    const { statusOptions, } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            infoId: props.values.infoId,
            userName: props.values.userName,
            ipaddr: props.values.ipaddr,
            loginLocation: props.values.loginLocation,
            browser: props.values.browser,
            os: props.values.os,
            status: props.values.status,
            msg: props.values.msg,
            loginTime: props.values.loginTime,
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
            id: 'system.logininfor.title',
            defaultMessage: '编辑系统访问记录',
        }), open: props.open, destroyOnHidden: true, forceRender: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "infoId", label: intl.formatMessage({
                        id: 'system.logininfor.info_id',
                        defaultMessage: '访问编号',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u8BBF\u95EE\u7F16\u53F7", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8BBF\u95EE\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8BBF\u95EE\u7F16\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "userName", label: intl.formatMessage({
                        id: 'system.logininfor.user_name',
                        defaultMessage: '用户账号',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u8D26\u53F7", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u8D26\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7528\u6237\u8D26\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "ipaddr", label: intl.formatMessage({
                        id: 'system.logininfor.ipaddr',
                        defaultMessage: '登录IP地址',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u767B\u5F55IP\u5730\u5740", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u767B\u5F55IP\u5730\u5740\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u767B\u5F55IP\u5730\u5740\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "loginLocation", label: intl.formatMessage({
                        id: 'system.logininfor.login_location',
                        defaultMessage: '登录地点',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u767B\u5F55\u5730\u70B9", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u767B\u5F55\u5730\u70B9\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u767B\u5F55\u5730\u70B9\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "browser", label: intl.formatMessage({
                        id: 'system.logininfor.browser',
                        defaultMessage: '浏览器类型',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u6D4F\u89C8\u5668\u7C7B\u578B", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u6D4F\u89C8\u5668\u7C7B\u578B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u6D4F\u89C8\u5668\u7C7B\u578B\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "os", label: intl.formatMessage({
                        id: 'system.logininfor.os',
                        defaultMessage: '操作系统',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u64CD\u4F5C\u7CFB\u7EDF", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u64CD\u4F5C\u7CFB\u7EDF\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u64CD\u4F5C\u7CFB\u7EDF\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.logininfor.status',
                        defaultMessage: '登录状态',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u767B\u5F55\u72B6\u6001", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u767B\u5F55\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u767B\u5F55\u72B6\u6001\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "msg", label: intl.formatMessage({
                        id: 'system.logininfor.msg',
                        defaultMessage: '提示消息',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u63D0\u793A\u6D88\u606F", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u63D0\u793A\u6D88\u606F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u63D0\u793A\u6D88\u606F\uFF01" }),
                        },
                    ] }), _jsx(ProFormTimePicker, { name: "loginTime", label: intl.formatMessage({
                        id: 'system.logininfor.login_time',
                        defaultMessage: '访问时间',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u8BBF\u95EE\u65F6\u95F4", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8BBF\u95EE\u65F6\u95F4\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8BBF\u95EE\u65F6\u95F4\uFF01" }),
                        },
                    ] })] }) }));
};
export default LogininforForm;
