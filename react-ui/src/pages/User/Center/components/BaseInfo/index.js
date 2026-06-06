import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Form, Row } from 'antd';
import { FormattedMessage, useIntl } from '@umijs/max';
import { ProForm, ProFormRadio, ProFormText } from '@ant-design/pro-components';
import { updateUserProfile } from '@/services/system/user';
import { message } from '@/utils/feedback';
const BaseInfo = (props) => {
    const [form] = Form.useForm();
    const intl = useIntl();
    const handleFinish = async (values) => {
        const data = { ...props.values, ...values };
        const resp = await updateUserProfile(data);
        if (resp.code === 200) {
            message.success('修改成功');
        }
        else {
            message.warning(resp.msg);
        }
    };
    return (_jsx(_Fragment, { children: _jsxs(ProForm, { form: form, onFinish: handleFinish, initialValues: props.values, children: [_jsx(Row, { children: _jsx(ProFormText, { name: "nickName", label: intl.formatMessage({
                            id: 'system.user.nick_name',
                            defaultMessage: '用户昵称',
                        }), width: "xl", placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0", rules: [
                            {
                                required: true,
                                message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0\uFF01" })),
                            },
                        ] }) }), _jsx(Row, { children: _jsx(ProFormText, { name: "phonenumber", label: intl.formatMessage({
                            id: 'system.user.phonenumber',
                            defaultMessage: '手机号码',
                        }), width: "xl", placeholder: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7\u7801", rules: [
                            {
                                required: false,
                                message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u624B\u673A\u53F7\u7801\uFF01" })),
                            },
                        ] }) }), _jsx(Row, { children: _jsx(ProFormText, { name: "email", label: intl.formatMessage({
                            id: 'system.user.email',
                            defaultMessage: '邮箱',
                        }), width: "xl", placeholder: "\u8BF7\u8F93\u5165\u90AE\u7BB1", rules: [
                            {
                                type: 'email',
                                message: '无效的邮箱地址!',
                            },
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u90AE\u7BB1\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u90AE\u7BB1\uFF01" }),
                            },
                        ] }) }), _jsx(Row, { children: _jsx(ProFormRadio.Group, { options: [
                            {
                                label: '男',
                                value: '0',
                            },
                            {
                                label: '女',
                                value: '1',
                            },
                        ], name: "sex", label: intl.formatMessage({
                            id: 'system.user.sex',
                            defaultMessage: 'sex',
                        }), width: "xl", rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u6027\u522B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u6027\u522B\uFF01" }),
                            },
                        ] }) })] }) }));
};
export default BaseInfo;
