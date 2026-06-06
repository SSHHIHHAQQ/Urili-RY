import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { Form, Input, Modal, Select } from 'antd';
import { useEffect } from 'react';
import { normalizeSettlementTypeValue, normalizeSystemKindValue, settlementOptions, systemKindOptions, } from '@/pages/UpstreamSystem/constants';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
export default function ConnectionModal({ mode, open, record, onCancel, onSubmit, }) {
    const [form] = Form.useForm();
    const titleMap = {
        create: '新增主仓接入',
        edit: '编辑主仓信息',
        credential: '重新授权',
    };
    useEffect(() => {
        if (!open) {
            form.resetFields();
            return;
        }
        form.setFieldsValue({
            systemKind: normalizeSystemKindValue(record?.systemKind),
            masterWarehouseName: record?.masterWarehouseName,
            settlementType: normalizeSettlementTypeValue(record?.settlementType),
            remark: record?.remark,
        });
    }, [form, open, record]);
    const showInfoFields = mode !== 'credential';
    const showCredentialFields = mode !== 'edit';
    return (_jsx(Modal, { title: titleMap[mode], open: open, onCancel: onCancel, onOk: async () => {
            const values = await form.validateFields();
            const ok = await onSubmit(values);
            if (ok) {
                form.resetFields();
            }
        }, destroyOnHidden: true, children: _jsxs(Form, { form: form, layout: "vertical", initialValues: {
                systemKind: 'lingxing-wms',
                settlementType: 'upstream-payable',
            }, children: [showInfoFields ? (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "systemKind", label: "\u4E0A\u6E38\u7CFB\u7EDF\u7C7B\u578B", rules: [{ required: true, message: '请选择上游系统类型' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, disabled: mode !== 'create', options: systemKindOptions }) }), _jsx(Form.Item, { name: "masterWarehouseName", label: "\u4E3B\u4ED3\u540D\u79F0", rules: [{ required: true, message: '请输入主仓名称' }], children: _jsx(Input, { placeholder: "\u4F8B\u5982 CA012", maxLength: 200 }) }), _jsx(Form.Item, { name: "settlementType", label: "\u7ED3\u7B97\u7C7B\u578B", rules: [{ required: true, message: '请选择结算类型' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: settlementOptions }) }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 3, maxLength: 500 }) })] })) : null, showCredentialFields ? (_jsxs(_Fragment, { children: [_jsx(Form.Item, { name: "appKey", label: "Key", rules: [{ required: true, message: '请输入Key' }], children: _jsx(Input, { autoComplete: "off" }) }), _jsx(Form.Item, { name: "appSecret", label: "Secret", rules: [{ required: true, message: '请输入Secret' }], children: _jsx(Input.Password, { autoComplete: "new-password" }) })] })) : null] }) }));
}
