import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Col, Form, Row } from 'antd';
import { Fragment, useEffect } from 'react';
import { history } from '@umijs/max';
import styles from '../style.module.css';
import { ProForm, ProFormText, ProFormTextArea } from '@ant-design/pro-components';
import { message } from '@/utils/feedback';
const BaseInfo = (props) => {
    const [form] = Form.useForm();
    const { onStepSubmit } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            tableName: props.values.tableName,
        });
    });
    const onValidateForm = async () => {
        const values = await form.validateFields();
        if (onStepSubmit) {
            onStepSubmit('base', values);
        }
    };
    return (_jsxs(Fragment, { children: [_jsx(Row, { children: _jsx(Col, { span: 24, children: _jsxs(ProForm, { form: form, onFinish: async () => {
                            message.success('提交成功');
                        }, initialValues: {
                            tableName: props.values?.tableName,
                            tableComment: props.values?.tableComment,
                            className: props.values?.className,
                            functionAuthor: props.values?.functionAuthor,
                            remark: props.values?.remark,
                        }, submitter: {
                            resetButtonProps: {
                                style: { display: 'none' },
                            },
                            submitButtonProps: {
                                style: { display: 'none' },
                            },
                        }, children: [_jsxs(Row, { children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormText, { name: "tableName", label: "\u8868\u540D\u79F0", rules: [
                                                {
                                                    required: true,
                                                    message: '表名称不可为空。',
                                                },
                                            ] }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProFormText, { name: "tableComment", label: "\u8868\u63CF\u8FF0" }) })] }), _jsxs(Row, { children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormText, { name: "className", label: "\u5B9E\u4F53\u7C7B\u540D\u79F0", rules: [
                                                {
                                                    required: true,
                                                    message: '实体类名称不可为空。',
                                                },
                                            ] }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProFormText, { name: "functionAuthor", label: "\u4F5C\u8005" }) })] }), _jsx(Row, { children: _jsx(Col, { span: 24, children: _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8" }) }) })] }) }) }), _jsxs(Row, { justify: "center", children: [_jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", className: styles.step_buttons, onClick: () => {
                                history.back();
                            }, children: "\u8FD4\u56DE" }) }), _jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", onClick: onValidateForm, children: "\u4E0B\u4E00\u6B65" }) })] })] }));
};
export default BaseInfo;
