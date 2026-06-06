import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormRadio, ProFormTreeSelect, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
const DeptForm = (props) => {
    const [form] = Form.useForm();
    const { statusOptions, deptTree } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            deptId: props.values.deptId,
            parentId: props.values.parentId,
            ancestors: props.values.ancestors,
            deptName: props.values.deptName,
            orderNum: props.values.orderNum,
            leader: props.values.leader,
            phone: props.values.phone,
            email: props.values.email,
            status: props.values.status,
            delFlag: props.values.delFlag,
            createBy: props.values.createBy,
            createTime: props.values.createTime,
            updateBy: props.values.updateBy,
            updateTime: props.values.updateTime,
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
            id: 'system.dept.title',
            defaultMessage: '编辑部门',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "deptId", label: intl.formatMessage({
                        id: 'system.dept.dept_id',
                        defaultMessage: '部门id',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u90E8\u95E8id", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u90E8\u95E8id\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u90E8\u95E8id\uFF01" }),
                        },
                    ] }), _jsx(ProFormTreeSelect, { name: "parentId", label: intl.formatMessage({
                        id: 'system.dept.parent_dept',
                        defaultMessage: '上级部门:',
                    }), request: async () => {
                        return deptTree;
                    }, fieldProps: SEARCHABLE_TREE_SELECT_PROPS, placeholder: "\u8BF7\u9009\u62E9\u4E0A\u7EA7\u90E8\u95E8", rules: [
                        {
                            required: true,
                            message: (_jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7528\u6237\u6635\u79F0\uFF01", defaultMessage: "\u8BF7\u9009\u62E9\u4E0A\u7EA7\u90E8\u95E8!" })),
                        },
                    ] }), _jsx(ProFormText, { name: "deptName", label: intl.formatMessage({
                        id: 'system.dept.dept_name',
                        defaultMessage: '部门名称',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u90E8\u95E8\u540D\u79F0", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u90E8\u95E8\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u90E8\u95E8\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormDigit, { name: "orderNum", label: intl.formatMessage({
                        id: 'system.dept.order_num',
                        defaultMessage: '显示顺序',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "leader", label: intl.formatMessage({
                        id: 'system.dept.leader',
                        defaultMessage: '负责人',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u8D1F\u8D23\u4EBA", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8D1F\u8D23\u4EBA\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8D1F\u8D23\u4EBA\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "phone", label: intl.formatMessage({
                        id: 'system.dept.phone',
                        defaultMessage: '联系电话',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u8054\u7CFB\u7535\u8BDD", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8054\u7CFB\u7535\u8BDD\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8054\u7CFB\u7535\u8BDD\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "email", label: intl.formatMessage({
                        id: 'system.dept.email',
                        defaultMessage: '邮箱',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u90AE\u7BB1", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u90AE\u7BB1\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u90AE\u7BB1\uFF01" }),
                        },
                    ] }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.dept.status',
                        defaultMessage: '部门状态',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u90E8\u95E8\u72B6\u6001", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u90E8\u95E8\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u90E8\u95E8\u72B6\u6001\uFF01" }),
                        },
                    ] })] }) }));
};
export default DeptForm;
