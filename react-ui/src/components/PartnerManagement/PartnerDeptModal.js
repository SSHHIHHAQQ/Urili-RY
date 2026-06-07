import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { useAccess } from '@umijs/max';
import { App, Button, Flex, Form, Input, InputNumber, Modal, Select, Table, Tag, TreeSelect, Typography, } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
const statusOptions = [
    { label: '正常', value: '0' },
    { label: '停用', value: '1' },
];
const compactCellTextStyle = {
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    lineHeight: 1.35,
};
function getValue(record, field) {
    return record ? record[field] : undefined;
}
function mapDeptToForm(dept) {
    return {
        parentId: dept?.parentId && dept.parentId > 0 ? dept.parentId : 0,
        deptName: dept?.deptName,
        orderNum: dept?.orderNum ?? 0,
        leader: dept?.leader,
        phone: dept?.phone,
        email: dept?.email,
        status: dept?.status || '0',
    };
}
function buildDeptPayload(currentDept, values) {
    return {
        deptId: currentDept?.deptId,
        parentId: values.parentId || 0,
        deptName: values.deptName,
        orderNum: values.orderNum ?? 0,
        leader: values.leader,
        phone: values.phone,
        email: values.email,
        status: values.status || '0',
    };
}
function renderCompactText(value) {
    const text = value == null || value === '' ? '-' : String(value);
    return _jsx(Typography.Text, { style: compactCellTextStyle, title: text, children: text });
}
const PartnerDeptModal = ({ config, open, partner, onOpenChange, }) => {
    const { message, modal } = App.useApp();
    const access = useAccess();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [formOpen, setFormOpen] = useState(false);
    const [currentDept, setCurrentDept] = useState();
    const [depts, setDepts] = useState([]);
    const [deptTree, setDeptTree] = useState([]);
    const partnerId = Number(getValue(partner, config.idField) || 0);
    const partnerName = getValue(partner, config.nameField) || getValue(partner, config.codeField) || '';
    const permPrefix = `${config.moduleKey}:admin`;
    const currentDeptId = currentDept?.deptId;
    const canQueryDeptTree = access.hasPerms(`${permPrefix}:dept:query`);
    const canAddDept = access.hasPerms(`${permPrefix}:dept:add`) && canQueryDeptTree;
    const canEditDept = access.hasPerms(`${permPrefix}:dept:edit`) && canQueryDeptTree;
    const parentTreeData = [
        {
            id: 0,
            label: '顶级部门',
            children: deptTree,
        },
    ];
    const loadDeptTree = async () => {
        if (!partnerId || !canQueryDeptTree) {
            setDeptTree([]);
            return;
        }
        try {
            const treeResp = await config.services.getDeptTree(partnerId);
            if (treeResp.code === 200) {
                setDeptTree(treeResp.data || []);
                return;
            }
            setDeptTree([]);
        }
        catch {
            setDeptTree([]);
        }
    };
    const loadDepts = async () => {
        if (!partnerId) {
            setDepts([]);
            return;
        }
        setLoading(true);
        try {
            const listResp = await config.services.listDepts(partnerId);
            if (listResp.code === 200) {
                setDepts((listResp.data || []));
            }
            else {
                message.error(listResp.msg || '部门列表加载失败');
            }
        }
        catch {
            message.error('部门列表加载失败，请重试');
        }
        finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (open) {
            void loadDepts();
            void loadDeptTree();
            return;
        }
        setDepts([]);
        setDeptTree([]);
    }, [open, partnerId, canQueryDeptTree]);
    const openDeptForm = (dept) => {
        setCurrentDept(dept);
        setFormOpen(true);
    };
    useEffect(() => {
        if (formOpen) {
            form.resetFields();
            form.setFieldsValue(mapDeptToForm(currentDept));
        }
    }, [formOpen, currentDept?.deptId]);
    const closeDeptForm = () => {
        setFormOpen(false);
        setCurrentDept(undefined);
    };
    const handleSubmit = async () => {
        if (!partnerId) {
            return;
        }
        const values = await form.validateFields();
        const payload = buildDeptPayload(currentDept, values);
        setSaving(true);
        try {
            const resp = currentDeptId
                ? await config.services.updateDept(partnerId, payload)
                : await config.services.addDept(partnerId, payload);
            if (resp.code === 200) {
                message.success(currentDeptId ? '部门已更新' : '部门已新增');
                closeDeptForm();
                await loadDepts();
                await loadDeptTree();
                return;
            }
            message.error(resp.msg || '部门保存失败');
        }
        catch {
            message.error('部门保存失败，请重试');
        }
        finally {
            setSaving(false);
        }
    };
    const handleRemove = (dept) => {
        if (!partnerId || !dept.deptId) {
            return;
        }
        modal.confirm({
            title: `确认删除部门 ${dept.deptName || dept.deptId} 吗？`,
            content: '存在子部门或账号占用时，后端会拒绝删除。',
            onOk: async () => {
                const resp = await config.services.removeDept(partnerId, dept.deptId);
                if (resp.code === 200) {
                    message.success('部门已删除');
                    await loadDepts();
                    await loadDeptTree();
                    return;
                }
                message.error(resp.msg || '部门删除失败');
            },
        });
    };
    const columns = [
        {
            title: '部门名称',
            dataIndex: 'deptName',
            width: 180,
            render: renderCompactText,
        },
        {
            title: '上级部门',
            dataIndex: 'parentName',
            width: 150,
            render: renderCompactText,
        },
        {
            title: '排序',
            dataIndex: 'orderNum',
            width: 72,
            render: renderCompactText,
        },
        {
            title: '负责人',
            dataIndex: 'leader',
            width: 120,
            render: renderCompactText,
        },
        {
            title: '联系方式',
            dataIndex: 'phone',
            width: 150,
            render: (_, record) => (_jsxs(Flex, { vertical: true, gap: 0, children: [renderCompactText(record.phone), _jsx(Typography.Text, { type: "secondary", style: compactCellTextStyle, title: record.email || '-', children: record.email || '-' })] })),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: (value) => _jsx(Tag, { color: value === '0' ? 'success' : 'default', children: value === '0' ? '正常' : '停用' }),
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: 120,
            render: (_, record) => (_jsxs(Flex, { gap: 4, children: [_jsx(Button, { type: "link", size: "small", hidden: !canEditDept, onClick: () => openDeptForm(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms(`${permPrefix}:dept:remove`), onClick: () => handleRemove(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { width: 900, title: `${config.label}部门 - ${partnerName || '-'}`, open: open, destroyOnHidden: true, footer: null, onCancel: () => onOpenChange(false), children: _jsx(Table, { rowKey: (record) => String(record.deptId), loading: loading, columns: columns, dataSource: depts, size: "small", pagination: false, tableLayout: "fixed", title: () => (_jsx(Button, { type: "primary", size: "small", icon: _jsx(PlusOutlined, {}), hidden: !canAddDept, onClick: () => openDeptForm(), children: "\u65B0\u589E\u90E8\u95E8" })) }) }), _jsx(Modal, { width: 640, title: currentDeptId ? '编辑部门' : '新增部门', open: formOpen, destroyOnHidden: true, confirmLoading: saving, onOk: handleSubmit, onCancel: closeDeptForm, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { label: "\u4E0A\u7EA7\u90E8\u95E8", name: "parentId", children: _jsx(TreeSelect, { ...SEARCHABLE_TREE_SELECT_PROPS, treeDefaultExpandAll: true, placeholder: "\u8BF7\u9009\u62E9", treeData: parentTreeData, fieldNames: { label: 'label', value: 'id', children: 'children' } }) }), _jsx(Form.Item, { label: "\u90E8\u95E8\u540D\u79F0", name: "deptName", rules: [{ required: true, message: '请输入部门名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u6392\u5E8F", name: "orderNum", rules: [{ required: true, message: '请输入排序' }], children: _jsx(InputNumber, { min: 0, precision: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u8D1F\u8D23\u4EBA", name: "leader", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u8054\u7CFB\u7535\u8BDD", name: "phone", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u90AE\u7BB1", name: "email", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u72B6\u6001", name: "status", rules: [{ required: true, message: '请选择状态' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: statusOptions }) })] }) })] }));
};
export default PartnerDeptModal;
