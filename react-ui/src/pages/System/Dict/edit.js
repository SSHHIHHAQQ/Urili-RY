import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormRadio, ProFormTextArea, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
const DictTypeForm = (props) => {
    const [form] = Form.useForm();
    const { statusOptions } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            dictId: props.values.dictId,
            dictName: props.values.dictName,
            dictType: props.values.dictType,
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
            id: 'system.dict.title',
            defaultMessage: '编辑字典类型',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "dictId", label: intl.formatMessage({
                        id: 'system.dict.dict_id',
                        defaultMessage: '字典主键',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u4E3B\u952E", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u4E3B\u952E\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u4E3B\u952E\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "dictName", label: intl.formatMessage({
                        id: 'system.dict.dict_name',
                        defaultMessage: '字典名称',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u540D\u79F0", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "dictType", label: intl.formatMessage({
                        id: 'system.dict.dict_type',
                        defaultMessage: '字典类型',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u7C7B\u578B", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u7C7B\u578B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u7C7B\u578B\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.dict.status',
                        defaultMessage: '状态',
                    }), initialValue: '0', placeholder: "\u8BF7\u8F93\u5165\u72B6\u6001", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "remark", label: intl.formatMessage({
                        id: 'system.dict.remark',
                        defaultMessage: '备注',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default DictTypeForm;
