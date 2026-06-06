import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, ProFormDigit, ProFormSelect, ProFormText, ProFormTextArea, ProTable, } from '@ant-design/pro-components';
import { Button, Form, Modal } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { addAttributeOption, deleteAttributeOption, getAttributeOptionList, updateAttributeOption, } from '@/services/product/product';
import { message } from '@/utils/feedback';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { statusOptions, statusValueEnum, yesNoOptions, yesNoValueEnum, } from '../../constants';
const defaultOptionValues = {
    sortOrder: 0,
    defaultFlag: 'N',
    status: '0',
};
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
export default function AttributeOptionManager({ access, attribute, open, onOpenChange, }) {
    const actionRef = useRef(null);
    const [form] = Form.useForm();
    const [optionModalOpen, setOptionModalOpen] = useState(false);
    const [currentOption, setCurrentOption] = useState();
    useEffect(() => {
        if (!optionModalOpen) {
            return;
        }
        form.resetFields();
        form.setFieldsValue(currentOption || defaultOptionValues);
    }, [currentOption, form, optionModalOpen]);
    const openCreateOption = () => {
        setCurrentOption(undefined);
        setOptionModalOpen(true);
    };
    const openEditOption = (record) => {
        setCurrentOption(record);
        setOptionModalOpen(true);
    };
    const saveOption = async (values) => {
        if (!attribute?.attributeId) {
            return false;
        }
        const resp = currentOption?.optionId
            ? await updateAttributeOption(attribute.attributeId, currentOption.optionId, values)
            : await addAttributeOption(attribute.attributeId, values);
        if (resultOk(resp, currentOption?.optionId ? '选项已更新' : '选项已新增')) {
            actionRef.current?.reload();
            return true;
        }
        return false;
    };
    const removeOption = (record) => {
        const attributeId = attribute?.attributeId;
        const optionId = record.optionId;
        if (!attributeId || !optionId) {
            return;
        }
        Modal.confirm({
            title: '删除属性选项',
            content: `确认删除 ${record.optionLabel}？`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const ok = resultOk(await deleteAttributeOption(attributeId, optionId), '选项已删除');
                if (ok)
                    actionRef.current?.reload();
            },
        });
    };
    const columns = [
        {
            title: '选项编码',
            dataIndex: 'optionCode',
            width: 140,
        },
        {
            title: '选项名称',
            dataIndex: 'optionLabel',
            width: 180,
        },
        {
            title: '默认',
            dataIndex: 'defaultFlag',
            valueEnum: yesNoValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueEnum: statusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
        },
        {
            title: '排序',
            dataIndex: 'sortOrder',
            width: 90,
        },
        {
            title: '操作',
            valueType: 'option',
            width: 130,
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:attribute:edit'), onClick: () => openEditOption(record), children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('product:attribute:edit'), onClick: () => removeOption(record), children: "\u5220\u9664" }, "delete"),
            ],
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { title: `属性选项：${attribute?.attributeName || ''}`, open: open, width: 760, footer: null, destroyOnHidden: true, onCancel: () => onOpenChange(false), children: _jsx(ProTable, { actionRef: actionRef, rowKey: "optionId", columns: columns, search: false, pagination: false, request: async () => {
                        if (!attribute?.attributeId) {
                            return { data: [], success: true };
                        }
                        const resp = await getAttributeOptionList(attribute.attributeId);
                        return { data: resp.data || [], success: resp.code === 200 };
                    }, toolBarRender: () => [
                        _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms('product:attribute:edit'), onClick: openCreateOption, children: "\u65B0\u589E" }, "add"),
                    ] }) }), _jsxs(ModalForm, { title: currentOption?.optionId ? '编辑属性选项' : '新增属性选项', open: optionModalOpen, form: form, modalProps: {
                    destroyOnHidden: true,
                    onCancel: () => setOptionModalOpen(false),
                }, onOpenChange: setOptionModalOpen, onFinish: saveOption, children: [_jsx(ProFormText, { name: "optionCode", label: "\u9009\u9879\u7F16\u7801", rules: [{ required: true, message: '请输入选项编码' }] }), _jsx(ProFormText, { name: "optionLabel", label: "\u9009\u9879\u540D\u79F0", rules: [{ required: true, message: '请输入选项名称' }] }), _jsx(ProFormSelect, { name: "defaultFlag", label: "\u9ED8\u8BA4", options: yesNoOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormDigit, { name: "sortOrder", label: "\u6392\u5E8F", min: 0 }), _jsx(ProFormSelect, { name: "status", label: "\u72B6\u6001", options: statusOptions, fieldProps: SEARCHABLE_SELECT_PROPS }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8" })] })] }));
}
