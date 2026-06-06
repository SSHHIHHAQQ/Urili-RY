import { jsxs as _jsxs, jsx as _jsx } from "react/jsx-runtime";
import { Form, Modal } from 'antd';
import { useIntl } from '@umijs/max';
import { ProForm, ProFormText } from '@ant-design/pro-components';
const UpdateForm = (props) => {
    const [form] = Form.useForm();
    const loginPassword = Form.useWatch('password', form);
    const userId = props.values.userId;
    const intl = useIntl();
    const handleOk = () => {
        form.submit();
    };
    const handleCancel = () => {
        props.onCancel();
    };
    const handleFinish = async (values) => {
        props.onSubmit({ ...values, userId });
    };
    const checkPassword = (rule, value) => {
        if (value === loginPassword) {
            // 校验条件自定义
            return Promise.resolve();
        }
        return Promise.reject(new Error('两次密码输入不一致'));
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'system.user.reset.password',
            defaultMessage: '密码重置',
        }), open: props.open, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { grid: true, form: form, layout: "horizontal", onFinish: handleFinish, initialValues: {
                password: '',
                confirm_password: '',
            }, children: [_jsxs("p", { children: ["\u8BF7\u8F93\u5165\u7528\u6237", props.values.userName, "\u7684\u65B0\u5BC6\u7801\uFF01"] }), _jsx(ProFormText.Password, { name: "password", label: "\u767B\u5F55\u5BC6\u7801", rules: [
                        {
                            required: true,
                            message: '登录密码不可为空。',
                        },
                    ] }), _jsx(ProFormText.Password, { name: "confirm_password", label: "\u786E\u8BA4\u5BC6\u7801", rules: [
                        {
                            required: true,
                            message: "确认密码",
                        },
                        { validator: checkPassword },
                    ] })] }) }));
};
export default UpdateForm;
