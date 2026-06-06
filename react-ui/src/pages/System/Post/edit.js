import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormRadio, ProFormTextArea, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
const PostForm = (props) => {
    const [form] = Form.useForm();
    const { statusOptions, } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            postId: props.values.postId,
            postCode: props.values.postCode,
            postName: props.values.postName,
            postSort: props.values.postSort,
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
    };
    const handleFinish = async (values) => {
        props.onSubmit(values);
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'system.post.title',
            defaultMessage: '编辑岗位信息',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "postId", label: intl.formatMessage({
                        id: 'system.post.post_id',
                        defaultMessage: '岗位编号',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u53F7", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "postName", label: intl.formatMessage({
                        id: 'system.post.post_name',
                        defaultMessage: '岗位名称',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u540D\u79F0", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "postCode", label: intl.formatMessage({
                        id: 'system.post.post_code',
                        defaultMessage: '岗位编码',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u7801", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5C97\u4F4D\u7F16\u7801\uFF01" }),
                        },
                    ] }), _jsx(ProFormDigit, { name: "postSort", label: intl.formatMessage({
                        id: 'system.post.post_sort',
                        defaultMessage: '显示顺序',
                    }), placeholder: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.post.status',
                        defaultMessage: '状态',
                    }), placeholder: "\u8BF7\u8F93\u5165\u72B6\u6001", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "remark", label: intl.formatMessage({
                        id: 'system.post.remark',
                        defaultMessage: '备注',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default PostForm;
