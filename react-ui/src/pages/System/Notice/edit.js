import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormSelect, ProFormTextArea, ProFormRadio, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const NoticeForm = (props) => {
    const [form] = Form.useForm();
    const { noticeTypeOptions, statusOptions, } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            noticeId: props.values.noticeId,
            noticeTitle: props.values.noticeTitle,
            noticeType: props.values.noticeType,
            noticeContent: props.values.noticeContent,
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
            id: 'system.notice.title',
            defaultMessage: '编辑通知公告',
        }), forceRender: true, open: props.open, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "noticeId", label: intl.formatMessage({
                        id: 'system.notice.notice_id',
                        defaultMessage: '公告编号',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u516C\u544A\u7F16\u53F7", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u516C\u544A\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u516C\u544A\u7F16\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "noticeTitle", label: intl.formatMessage({
                        id: 'system.notice.notice_title',
                        defaultMessage: '公告标题',
                    }), placeholder: "\u8BF7\u8F93\u5165\u516C\u544A\u6807\u9898", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u516C\u544A\u6807\u9898\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u516C\u544A\u6807\u9898\uFF01" }),
                        },
                    ] }), _jsx(ProFormSelect, { valueEnum: noticeTypeOptions, name: "noticeType", label: intl.formatMessage({
                        id: 'system.notice.notice_type',
                        defaultMessage: '公告类型',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u516C\u544A\u7C7B\u578B", fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u516C\u544A\u7C7B\u578B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u516C\u544A\u7C7B\u578B\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.notice.status',
                        defaultMessage: '公告状态',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u516C\u544A\u72B6\u6001", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u516C\u544A\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u516C\u544A\u72B6\u6001\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "noticeContent", label: intl.formatMessage({
                        id: 'system.notice.notice_content',
                        defaultMessage: '公告内容',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u516C\u544A\u5185\u5BB9", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u516C\u544A\u5185\u5BB9\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u516C\u544A\u5185\u5BB9\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "remark", label: intl.formatMessage({
                        id: 'system.notice.remark',
                        defaultMessage: '备注',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default NoticeForm;
