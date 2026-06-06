import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Form, Input, Modal } from 'antd';
import { useEffect } from 'react';
export default function PairingModal({ open, title, codeLabel, nameLabel, codeName, nameName, upstreamLabel, upstreamValue, showCustomerName, onCancel, onSubmit, }) {
    const [form] = Form.useForm();
    useEffect(() => {
        if (!open) {
            form.resetFields();
        }
    }, [form, open]);
    return (_jsx(Modal, { title: title, open: open, onCancel: onCancel, onOk: async () => {
            const values = await form.validateFields();
            const ok = await onSubmit(values);
            if (ok) {
                form.resetFields();
            }
        }, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { label: upstreamLabel, children: _jsx(Input, { value: upstreamValue, disabled: true }) }), _jsx(Form.Item, { name: codeName, label: codeLabel, rules: [{ required: true, message: `请输入${codeLabel}` }], children: _jsx(Input, { maxLength: 128 }) }), _jsx(Form.Item, { name: nameName, label: nameLabel, rules: [{ required: true, message: `请输入${nameLabel}` }], children: _jsx(Input, { maxLength: 255 }) }), showCustomerName ? (_jsx(Form.Item, { name: "customerName", label: "\u5BA2\u6237\u540D\u79F0", children: _jsx(Input, { maxLength: 255 }) })) : null, _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 3, maxLength: 500 }) })] }) }));
}
