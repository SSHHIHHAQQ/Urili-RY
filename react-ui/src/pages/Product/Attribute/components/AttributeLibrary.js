import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DownOutlined, HistoryOutlined, ImportOutlined, PlusOutlined, } from '@ant-design/icons';
import { ModalForm, ProFormDependency, ProFormDigit, ProFormSelect, ProFormText, ProFormTextArea, ProTable, } from '@ant-design/pro-components';
import { Button, Dropdown, Form, Switch } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { addAttribute, deleteAttribute, downloadAttributeImportTemplate, downloadAttributeOptionImportTemplate, getAttributeList, importAttributeData, importAttributeOptionData, previewAttributeImport, previewAttributeOptionImport, updateAttribute, updateAttributeStatus, } from '@/services/product/product';
import { getDictTypeOptionSelect } from '@/services/system/dict';
import { message, modal } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { attributeTypeOptions, isNumberAttributeType, isOptionAttributeType, optionArrayToValueEnum, optionSourceOptions, selectAttributeOptionSourceOptions, statusValueEnum, } from '../../constants';
import ProductImportModal from '../../components/ProductImportModal';
import ProductConfigChangeLogDrawer from '../../components/ProductConfigChangeLogDrawer';
import AttributeOptionManager from './AttributeOptionManager';
const defaultAttributeValues = {
    attributeType: 'TEXT',
    optionSource: 'NONE',
    unit: '',
    valuePrecision: 0,
    status: '0',
};
function normalizeAttributeValues(values) {
    const next = { ...defaultAttributeValues, ...values };
    if (!isOptionAttributeType(next.attributeType)) {
        next.optionSource = 'NONE';
        next.dictType = '';
    }
    else if (next.optionSource !== 'ATTRIBUTE_OPTION' &&
        next.optionSource !== 'SYS_DICT') {
        next.optionSource = 'ATTRIBUTE_OPTION';
    }
    if (next.optionSource !== 'SYS_DICT') {
        next.dictType = '';
    }
    if (!isNumberAttributeType(next.attributeType)) {
        next.unit = '';
        next.valuePrecision = 0;
    }
    else {
        next.unit = next.unit || '';
        next.valuePrecision = next.valuePrecision ?? 0;
    }
    return next;
}
function resultOk(resp, successText) {
    if (resp.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp.msg || '操作失败');
    return false;
}
export default function AttributeLibrary({ access }) {
    const actionRef = useRef(null);
    const [attributeForm] = Form.useForm();
    const [attributeModalOpen, setAttributeModalOpen] = useState(false);
    const [attributeImportOpen, setAttributeImportOpen] = useState(false);
    const [optionImportOpen, setOptionImportOpen] = useState(false);
    const [optionListOpen, setOptionListOpen] = useState(false);
    const [statusUpdatingId, setStatusUpdatingId] = useState();
    const [currentAttribute, setCurrentAttribute] = useState();
    const [optionAttribute, setOptionAttribute] = useState();
    const [operationLogOpen, setOperationLogOpen] = useState(false);
    const [dictTypeOptions, setDictTypeOptions] = useState([]);
    const attributeTypeValueEnum = useMemo(() => optionArrayToValueEnum(attributeTypeOptions), []);
    const optionSourceValueEnum = useMemo(() => optionArrayToValueEnum(optionSourceOptions), []);
    useEffect(() => {
        if (!attributeModalOpen) {
            return;
        }
        attributeForm.resetFields();
        attributeForm.setFieldsValue(normalizeAttributeValues(currentAttribute));
    }, [attributeForm, attributeModalOpen, currentAttribute]);
    useEffect(() => {
        getDictTypeOptionSelect()
            .then((resp) => {
            if (resp.code !== 200) {
                return;
            }
            const options = (resp.data || [])
                .filter((item) => item.status !== '1')
                .map((item) => ({
                label: `${item.dictName}（${item.dictType}）`,
                value: item.dictType,
            }));
            setDictTypeOptions(options);
        })
            .catch(() => {
            message.error('字典类型加载失败');
        });
    }, []);
    const openCreateAttribute = () => {
        setCurrentAttribute(undefined);
        setAttributeModalOpen(true);
    };
    const openEditAttribute = (record) => {
        setCurrentAttribute(record);
        setAttributeModalOpen(true);
    };
    const saveAttribute = async (values) => {
        const payload = normalizeAttributeValues(values);
        const resp = currentAttribute?.attributeId
            ? await updateAttribute(currentAttribute.attributeId, payload)
            : await addAttribute(payload);
        if (resultOk(resp, currentAttribute?.attributeId ? '属性已更新' : '属性已新增')) {
            actionRef.current?.reload();
            return true;
        }
        return false;
    };
    const removeAttribute = (record) => {
        const attributeId = record.attributeId;
        if (!attributeId) {
            return;
        }
        modal.confirm({
            title: '删除商品属性',
            content: `确认删除 ${record.attributeName}？已被类目引用的属性会被后端拒绝删除。`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const ok = resultOk(await deleteAttribute(attributeId), '属性已删除');
                if (ok)
                    actionRef.current?.reload();
            },
        });
    };
    const changeAttributeStatus = (record, targetStatus) => {
        const attributeId = record.attributeId;
        if (!attributeId || record.status === targetStatus) {
            return;
        }
        const actionText = targetStatus === '0' ? '启用' : '停用';
        modal.confirm({
            title: `${actionText}商品属性`,
            content: targetStatus === '0'
                ? `确认启用 ${record.attributeName}？启用后可继续被类目属性模板选择。`
                : `确认停用 ${record.attributeName}？停用后卖家上传商品时不会再使用该属性。`,
            okText: `确认${actionText}`,
            cancelText: '取消',
            okButtonProps: {
                danger: targetStatus === '1',
            },
            onOk: async () => {
                setStatusUpdatingId(attributeId);
                try {
                    const ok = resultOk(await updateAttributeStatus(attributeId, targetStatus), `属性已${actionText}`);
                    if (ok) {
                        actionRef.current?.reload();
                    }
                }
                finally {
                    setStatusUpdatingId(undefined);
                }
            },
        });
    };
    const openOptions = (record) => {
        setOptionAttribute(record);
        setOptionListOpen(true);
    };
    const columns = [
        {
            title: '属性编码',
            dataIndex: 'attributeCode',
            width: 160,
        },
        {
            title: '属性名称',
            dataIndex: 'attributeName',
            width: 160,
        },
        {
            title: '属性类型',
            dataIndex: 'attributeType',
            valueType: 'select',
            valueEnum: attributeTypeValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 130,
        },
        {
            title: '选项来源',
            dataIndex: 'optionSource',
            valueType: 'select',
            valueEnum: optionSourceValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 150,
        },
        {
            title: '字典类型',
            dataIndex: 'dictType',
            width: 150,
            search: false,
        },
        {
            title: '单位',
            dataIndex: 'unit',
            width: 100,
            search: false,
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 100,
            render: (_, record) => access.hasPerms('product:attribute:edit') ? (_jsx(Switch, { checked: record.status === '0', checkedChildren: "\u542F\u7528", unCheckedChildren: "\u505C\u7528", loading: statusUpdatingId === record.attributeId, onChange: (checked) => changeAttributeStatus(record, checked ? '0' : '1') })) : record.status === '0' ? ('正常') : ('停用'),
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            width: 170,
            search: false,
        },
        {
            title: '操作',
            valueType: 'option',
            width: 190,
            render: (_, record) => {
                const canShowOptions = access.hasPerms('product:attribute:query') &&
                    record.optionSource === 'ATTRIBUTE_OPTION';
                const deleteButton = (_jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('product:attribute:remove'), onClick: () => removeAttribute(record), children: "\u5220\u9664" }, "delete"));
                return [
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:attribute:edit'), onClick: () => openEditAttribute(record), children: "\u7F16\u8F91" }, "edit"),
                    canShowOptions ? (_jsx(Button, { type: "link", size: "small", onClick: () => openOptions(record), children: "\u9009\u9879" }, "options")) : (deleteButton),
                    canShowOptions ? (_jsx(Dropdown, { trigger: ['click'], menu: {
                            items: [
                                {
                                    key: 'delete',
                                    label: '删除',
                                    danger: true,
                                    disabled: !access.hasPerms('product:attribute:remove'),
                                },
                            ],
                            onClick: ({ key }) => {
                                if (key === 'delete')
                                    removeAttribute(record);
                            },
                        }, children: _jsxs(Button, { type: "link", size: "small", children: ["\u66F4\u591A ", _jsx(DownOutlined, {})] }) }, "more")) : null,
                ];
            },
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(ProTable, { actionRef: actionRef, rowKey: "attributeId", columns: columns, scroll: getProTableScroll(1450), search: getPersistedProTableSearch({ labelWidth: 90 }, 'product-attribute'), request: async (params) => {
                    const resp = await getAttributeList(params);
                    return {
                        data: resp.rows || [],
                        total: resp.total || 0,
                        success: resp.code === 200,
                    };
                }, toolBarRender: () => [
                    _jsx(Button, { icon: _jsx(HistoryOutlined, {}), onClick: () => setOperationLogOpen(true), children: "\u64CD\u4F5C\u65E5\u5FD7" }, "operationLog"),
                    _jsx(Button, { icon: _jsx(ImportOutlined, {}), hidden: !access.hasPerms('product:attribute:add'), onClick: () => setAttributeImportOpen(true), children: "\u5BFC\u5165\u5C5E\u6027" }, "importAttribute"),
                    _jsx(Button, { icon: _jsx(ImportOutlined, {}), hidden: !access.hasPerms('product:attribute:edit'), onClick: () => setOptionImportOpen(true), children: "\u5BFC\u5165\u9009\u9879" }, "importOption"),
                    _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms('product:attribute:add'), onClick: openCreateAttribute, children: "\u65B0\u589E" }, "add"),
                ] }), _jsx(ProductImportModal, { title: "\u5BFC\u5165\u5546\u54C1\u5C5E\u6027", open: attributeImportOpen, onOpenChange: setAttributeImportOpen, onDownloadTemplate: downloadAttributeImportTemplate, onPreview: previewAttributeImport, onImport: importAttributeData, onSuccess: () => actionRef.current?.reload() }), _jsx(ProductImportModal, { title: "\u5BFC\u5165\u5546\u54C1\u5C5E\u6027\u9009\u9879", open: optionImportOpen, onOpenChange: setOptionImportOpen, onDownloadTemplate: downloadAttributeOptionImportTemplate, onPreview: previewAttributeOptionImport, onImport: importAttributeOptionData, onSuccess: () => actionRef.current?.reload() }), _jsx(ProductConfigChangeLogDrawer, { open: operationLogOpen, onOpenChange: setOperationLogOpen, bizTypes: ['ATTRIBUTE', 'ATTRIBUTE_OPTION'], title: "\u5C5E\u6027\u5E93" }), _jsxs(ModalForm, { title: currentAttribute?.attributeId ? '编辑商品属性' : '新增商品属性', open: attributeModalOpen, form: attributeForm, modalProps: {
                    destroyOnHidden: true,
                    onCancel: () => setAttributeModalOpen(false),
                }, onOpenChange: setAttributeModalOpen, onValuesChange: (changedValues, allValues) => {
                    if ('attributeType' in changedValues) {
                        const resetValues = {};
                        if (!isOptionAttributeType(changedValues.attributeType)) {
                            resetValues.optionSource = 'NONE';
                            resetValues.dictType = '';
                        }
                        else if (allValues.optionSource !== 'ATTRIBUTE_OPTION' &&
                            allValues.optionSource !== 'SYS_DICT') {
                            resetValues.optionSource = 'ATTRIBUTE_OPTION';
                            resetValues.dictType = '';
                        }
                        if (!isNumberAttributeType(changedValues.attributeType)) {
                            resetValues.unit = '';
                            resetValues.valuePrecision = 0;
                        }
                        if (Object.keys(resetValues).length > 0) {
                            attributeForm.setFieldsValue(resetValues);
                        }
                    }
                    if ('optionSource' in changedValues &&
                        changedValues.optionSource !== 'SYS_DICT') {
                        attributeForm.setFieldsValue({ dictType: '' });
                    }
                }, onFinish: saveAttribute, children: [_jsx(ProFormText, { name: "attributeCode", label: "\u5C5E\u6027\u7F16\u7801", rules: [{ required: true, message: '请输入属性编码' }] }), _jsx(ProFormText, { name: "attributeName", label: "\u5C5E\u6027\u540D\u79F0", rules: [{ required: true, message: '请输入属性名称' }] }), _jsx(ProFormSelect, { name: "attributeType", label: "\u5C5E\u6027\u7C7B\u578B", options: attributeTypeOptions, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [{ required: true, message: '请选择属性类型' }] }), _jsx(ProFormDependency, { name: ['attributeType', 'optionSource'], children: ({ attributeType, optionSource }) => isOptionAttributeType(attributeType) ? (_jsxs(_Fragment, { children: [_jsx(ProFormSelect, { name: "optionSource", label: "\u9009\u9879\u6765\u6E90", options: selectAttributeOptionSourceOptions, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [{ required: true, message: '请选择选项来源' }] }), optionSource === 'SYS_DICT' ? (_jsx(ProFormSelect, { name: "dictType", label: "\u5B57\u5178\u7C7B\u578B", options: dictTypeOptions, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [{ required: true, message: '请选择若依字典类型' }] })) : null] })) : null }), _jsx(ProFormDependency, { name: ['attributeType'], children: ({ attributeType }) => isNumberAttributeType(attributeType) ? (_jsxs(_Fragment, { children: [_jsx(ProFormText, { name: "unit", label: "\u5355\u4F4D" }), _jsx(ProFormDigit, { name: "valuePrecision", label: "\u5C0F\u6570\u4F4D\u6570", min: 0, max: 8 })] })) : null }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8" })] }), _jsx(AttributeOptionManager, { access: access, attribute: optionAttribute, open: optionListOpen, onOpenChange: setOptionListOpen })] }));
}
