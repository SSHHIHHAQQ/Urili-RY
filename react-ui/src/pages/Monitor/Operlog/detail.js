import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Descriptions, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { getValueEnumLabel } from '@/utils/options';
const OperlogDetailForm = (props) => {
    const { values, businessTypeOptions, operatorTypeOptions, statusOptions, } = props;
    const intl = useIntl();
    const handleOk = () => { };
    const handleCancel = () => {
        props.onCancel();
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'monitor.operlog.title',
            defaultMessage: '编辑操作日志记录',
        }), open: props.open, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(Descriptions, { column: 24, children: [_jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.operlog.module", defaultMessage: "\u64CD\u4F5C\u6A21\u5757" }), children: `${values.title}/${getValueEnumLabel(businessTypeOptions, values.businessType)}` }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.operlog.request_method", defaultMessage: "\u8BF7\u6C42\u65B9\u5F0F" }), children: values.requestMethod }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.operlog.oper_name", defaultMessage: "\u64CD\u4F5C\u4EBA\u5458" }), children: `${values.operName}/${values.operIp}` }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.operlog.operator_type", defaultMessage: "\u64CD\u4F5C\u7C7B\u522B" }), children: getValueEnumLabel(operatorTypeOptions, values.operatorType) }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.operlog.method", defaultMessage: "\u65B9\u6CD5\u540D\u79F0" }), children: values.method }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.operlog.oper_url", defaultMessage: "\u8BF7\u6C42URL" }), children: values.operUrl }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.operlog.oper_param", defaultMessage: "\u8BF7\u6C42\u53C2\u6570" }), children: values.operParam }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.operlog.json_result", defaultMessage: "\u8FD4\u56DE\u53C2\u6570" }), children: values.jsonResult }), _jsx(Descriptions.Item, { span: 24, label: _jsx(FormattedMessage, { id: "monitor.operlog.error_msg", defaultMessage: "\u9519\u8BEF\u6D88\u606F" }), children: values.errorMsg }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.operlog.status", defaultMessage: "\u64CD\u4F5C\u72B6\u6001" }), children: getValueEnumLabel(statusOptions, values.status) }), _jsx(Descriptions.Item, { span: 12, label: _jsx(FormattedMessage, { id: "monitor.operlog.oper_time", defaultMessage: "\u64CD\u4F5C\u65F6\u95F4" }), children: values.operTime?.toString() })] }) }));
};
export default OperlogDetailForm;
