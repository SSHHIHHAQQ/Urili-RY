import { jsx as _jsx } from "react/jsx-runtime";
import { useEffect } from 'react';
import { Form, Modal } from 'antd';
import { useIntl } from '@umijs/max';
import { ProForm, ProFormSelect } from '@ant-design/pro-components';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const AuthRoleForm = (props) => {
    const [form] = Form.useForm();
    useEffect(() => {
        form.resetFields();
        form.setFieldValue('roleIds', props.roleIds);
    });
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
            id: 'system.user.auth.role',
            defaultMessage: '分配角色',
        }), open: props.open, destroyOnHidden: true, forceRender: true, onOk: handleOk, onCancel: handleCancel, children: _jsx(ProForm, { form: form, grid: true, layout: "horizontal", onFinish: handleFinish, initialValues: {
                login_password: '',
                confirm_password: '',
            }, children: _jsx(ProFormSelect, { name: "roleIds", mode: "multiple", label: intl.formatMessage({
                    id: 'system.user.role',
                    defaultMessage: '角色',
                }), options: props.roles, placeholder: "\u8BF7\u9009\u62E9\u89D2\u8272", fieldProps: SEARCHABLE_SELECT_PROPS, rules: [{ required: true, message: '请选择角色!' }] }) }) }));
};
export default AuthRoleForm;
