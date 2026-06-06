import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormSelect, ProFormRadio, ProFormTextArea, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const DictDataForm = (props) => {
    const [form] = Form.useForm();
    const { statusOptions } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            dictCode: props.values.dictCode,
            dictSort: props.values.dictSort,
            dictLabel: props.values.dictLabel,
            dictValue: props.values.dictValue,
            dictType: props.values.dictType,
            cssClass: props.values.cssClass,
            listClass: props.values.listClass,
            isDefault: props.values.isDefault,
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
            id: 'system.dict.data.title',
            defaultMessage: '编辑字典数据',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "dictCode", label: intl.formatMessage({
                        id: 'system.dict.data.dict_code',
                        defaultMessage: '字典编码',
                    }), colProps: { md: 24, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u7F16\u7801", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u7F16\u7801\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u7F16\u7801\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "dictType", label: intl.formatMessage({
                        id: 'system.dict.data.dict_type',
                        defaultMessage: '字典类型',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u7C7B\u578B", disabled: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u7C7B\u578B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u7C7B\u578B\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "dictLabel", label: intl.formatMessage({
                        id: 'system.dict.data.dict_label',
                        defaultMessage: '字典标签',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u6807\u7B7E", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u6807\u7B7E\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u6807\u7B7E\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "dictValue", label: intl.formatMessage({
                        id: 'system.dict.data.dict_value',
                        defaultMessage: '字典键值',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u952E\u503C", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u952E\u503C\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u952E\u503C\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "cssClass", label: intl.formatMessage({
                        id: 'system.dict.data.css_class',
                        defaultMessage: '样式属性',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u6837\u5F0F\u5C5E\u6027", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u6837\u5F0F\u5C5E\u6027\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u6837\u5F0F\u5C5E\u6027\uFF01" }),
                        },
                    ] }), _jsx(ProFormSelect, { name: "listClass", label: intl.formatMessage({
                        id: 'system.dict.data.list_class',
                        defaultMessage: '回显样式',
                    }), colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u56DE\u663E\u6837\u5F0F", valueEnum: {
                        'default': '默认',
                        'primary': '主要',
                        'success': '成功',
                        'info': '信息',
                        'warning': '警告',
                        'danger': '危险',
                    }, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u56DE\u663E\u6837\u5F0F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u56DE\u663E\u6837\u5F0F\uFF01" }),
                        },
                    ] }), _jsx(ProFormDigit, { name: "dictSort", label: intl.formatMessage({
                        id: 'system.dict.data.dict_sort',
                        defaultMessage: '字典排序',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u5B57\u5178\u6392\u5E8F", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5B57\u5178\u6392\u5E8F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5B57\u5178\u6392\u5E8F\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { name: "isDefault", label: intl.formatMessage({
                        id: 'system.dict.data.is_default',
                        defaultMessage: '是否默认',
                    }), valueEnum: {
                        'Y': '是',
                        'N': '否',
                    }, initialValue: 'N', colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u662F\u5426\u9ED8\u8BA4", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u662F\u5426\u9ED8\u8BA4\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u662F\u5426\u9ED8\u8BA4\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.dict.data.status',
                        defaultMessage: '状态',
                    }), initialValue: '0', colProps: { md: 12, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u72B6\u6001", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u72B6\u6001\uFF01" }),
                        },
                    ] }), _jsx(ProFormTextArea, { name: "remark", label: intl.formatMessage({
                        id: 'system.dict.data.remark',
                        defaultMessage: '备注',
                    }), colProps: { md: 24, xl: 24 }, placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default DictDataForm;
