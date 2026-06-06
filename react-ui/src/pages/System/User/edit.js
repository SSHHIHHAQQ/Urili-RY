import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormText, ProFormSelect, ProFormRadio, ProFormTextArea, ProFormTreeSelect, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
const UserForm = (props) => {
    const [form] = Form.useForm();
    const userId = Form.useWatch('userId', form);
    const { sexOptions, statusOptions, } = props;
    const { roles, posts, depts } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            userId: props.values.userId,
            deptId: props.values.deptId,
            postIds: props.postIds,
            roleIds: props.roleIds,
            userName: props.values.userName,
            nickName: props.values.nickName,
            email: props.values.email,
            phonenumber: props.values.phonenumber,
            sex: props.values.sex,
            avatar: props.values.avatar,
            status: props.values.status,
            delFlag: props.values.delFlag,
            loginIp: props.values.loginIp,
            loginDate: props.values.loginDate,
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
            id: 'system.user.title',
            defaultMessage: '编辑用户信息',
        }), open: props.open, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { grid: true, form: form, layout: "horizontal", submitter: false, onFinish: handleFinish, children: [_jsx(ProFormText, { name: "nickName", label: intl.formatMessage({
                        id: 'system.user.nick_name',
                        defaultMessage: '用户昵称',
                    }), placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0", colProps: { xs: 24, md: 12, xl: 12 }, rules: [
                        {
                            required: true,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0\uFF01" })),
                        },
                    ] }), _jsx(ProFormTreeSelect, { name: "deptId", label: intl.formatMessage({
                        id: 'system.user.dept_name',
                        defaultMessage: '部门',
                    }), request: async () => {
                        return depts;
                    }, fieldProps: SEARCHABLE_TREE_SELECT_PROPS, placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u90E8\u95E8", colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: true,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u90E8\u95E8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7528\u6237\u90E8\u95E8\uFF01" })),
                        },
                    ] }), _jsx(ProFormText, { name: "phonenumber", label: intl.formatMessage({
                        id: 'system.user.phonenumber',
                        defaultMessage: '手机号码',
                    }), placeholder: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7\u7801", colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: false,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7\u7801\uFF01" })),
                        },
                    ] }), _jsx(ProFormText, { name: "email", label: intl.formatMessage({
                        id: 'system.user.email',
                        defaultMessage: '用户邮箱',
                    }), placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u90AE\u7BB1", colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: false,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u90AE\u7BB1\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7528\u6237\u90AE\u7BB1\uFF01" })),
                        },
                    ] }), _jsx(ProFormText, { name: "userName", label: intl.formatMessage({
                        id: 'system.user.user_name',
                        defaultMessage: '用户账号',
                    }), hidden: userId, placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u8D26\u53F7", colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: true,
                        },
                    ] }), _jsx(ProFormText.Password, { name: "password", label: intl.formatMessage({
                        id: 'system.user.password',
                        defaultMessage: '密码',
                    }), hidden: userId, placeholder: "\u8BF7\u8F93\u5165\u5BC6\u7801", colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5BC6\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5BC6\u7801\uFF01" }),
                        },
                    ] }), _jsx(ProFormSelect, { valueEnum: sexOptions, name: "sex", label: intl.formatMessage({
                        id: 'system.user.sex',
                        defaultMessage: '用户性别',
                    }), initialValue: '0', placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u6027\u522B", fieldProps: SEARCHABLE_SELECT_PROPS, colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: false,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u6027\u522B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7528\u6237\u6027\u522B\uFF01" })),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.user.status',
                        defaultMessage: '帐号状态',
                    }), initialValue: '0', placeholder: "\u8BF7\u8F93\u5165\u5E10\u53F7\u72B6\u6001", colProps: { md: 12, xl: 12 }, rules: [
                        {
                            required: false,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5E10\u53F7\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5E10\u53F7\u72B6\u6001\uFF01" })),
                        },
                    ] }), _jsx(ProFormSelect, { name: "postIds", mode: "multiple", label: intl.formatMessage({
                        id: 'system.user.post',
                        defaultMessage: '岗位',
                    }), options: posts, placeholder: "\u8BF7\u9009\u62E9\u5C97\u4F4D", fieldProps: SEARCHABLE_SELECT_PROPS, colProps: { md: 12, xl: 12 }, rules: [{ required: true, message: '请选择岗位!' }] }), _jsx(ProFormSelect, { name: "roleIds", mode: "multiple", label: intl.formatMessage({
                        id: 'system.user.role',
                        defaultMessage: '角色',
                    }), options: roles, placeholder: "\u8BF7\u9009\u62E9\u89D2\u8272", fieldProps: SEARCHABLE_SELECT_PROPS, colProps: { md: 12, xl: 12 }, rules: [{ required: true, message: '请选择角色!' }] }), _jsx(ProFormTextArea, { name: "remark", label: intl.formatMessage({
                        id: 'system.user.remark',
                        defaultMessage: '备注',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", colProps: { md: 24, xl: 24 }, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default UserForm;
