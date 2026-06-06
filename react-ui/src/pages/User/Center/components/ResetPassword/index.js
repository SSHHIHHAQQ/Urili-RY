import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Form } from 'antd';
import { FormattedMessage, useIntl } from '@umijs/max';
import { updateUserPwd } from '@/services/system/user';
import { ProForm, ProFormText } from '@ant-design/pro-components';
import { message } from '@/utils/feedback';
const ResetPassword = () => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const handleFinish = async (values) => {
        const resp = await updateUserPwd(values.oldPassword, values.newPassword);
        if (resp.code === 200) {
            message.success('密码重置成功。');
        }
        else {
            message.warning(resp.msg);
        }
    };
    const checkPassword = (rule, value) => {
        const login_password = form.getFieldValue('newPassword');
        if (value === login_password) {
            return Promise.resolve();
        }
        return Promise.reject(new Error('两次密码输入不一致'));
    };
    return (_jsx(_Fragment, { children: _jsxs(ProForm, { form: form, onFinish: handleFinish, children: [_jsx(ProFormText.Password, { name: "oldPassword", label: intl.formatMessage({
                        id: 'system.user.old_password',
                        defaultMessage: '旧密码',
                    }), width: "xl", placeholder: "\u8BF7\u8F93\u5165\u65E7\u5BC6\u7801", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u65E7\u5BC6\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u65E7\u5BC6\u7801\uFF01" }),
                        },
                    ] }), _jsx(ProFormText.Password, { name: "newPassword", label: intl.formatMessage({
                        id: 'system.user.new_password',
                        defaultMessage: '新密码',
                    }), width: "xl", placeholder: "\u8BF7\u8F93\u5165\u65B0\u5BC6\u7801", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u65B0\u5BC6\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u65B0\u5BC6\u7801\uFF01" }),
                        },
                    ] }), _jsx(ProFormText.Password, { name: "confirmPassword", label: intl.formatMessage({
                        id: 'system.user.confirm_password',
                        defaultMessage: '确认密码',
                    }), width: "xl", placeholder: "\u8BF7\u8F93\u5165\u786E\u8BA4\u5BC6\u7801", rules: [
                        {
                            required: true,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u786E\u8BA4\u5BC6\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u786E\u8BA4\u5BC6\u7801\uFF01" })),
                        },
                        { validator: checkPassword },
                    ] })] }) }));
};
export default ResetPassword;
