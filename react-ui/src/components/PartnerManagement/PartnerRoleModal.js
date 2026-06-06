import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { useAccess } from '@umijs/max';
import { App, Button, Flex, Form, Input, InputNumber, Modal, Select, Table, Tag, Tree, Typography, } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
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
function renderCompactText(value) {
    const text = value == null || value === '' ? '-' : String(value);
    return _jsx(Typography.Text, { style: compactCellTextStyle, title: text, children: text });
}
function normalizeKeys(keys) {
    return keys
        .map((key) => Number(key))
        .filter((key) => Number.isFinite(key));
}
function toTreeData(nodes) {
    return nodes.map((node) => ({
        key: node.id,
        title: node.label,
        children: node.children ? toTreeData(node.children) : undefined,
    }));
}
function mapRoleToForm(role) {
    return {
        roleName: role?.roleName,
        roleKey: role?.roleKey,
        roleSort: role?.roleSort ?? 0,
        status: role?.status || '0',
        remark: role?.remark,
    };
}
function buildRolePayload(currentRole, values, checkedMenuIds) {
    return {
        roleId: currentRole?.roleId,
        roleName: values.roleName,
        roleKey: values.roleKey,
        roleSort: values.roleSort ?? 0,
        status: values.status || '0',
        remark: values.remark,
        menuIds: checkedMenuIds,
    };
}
const PartnerRoleModal = ({ config, open, partner, onOpenChange, }) => {
    const { message, modal } = App.useApp();
    const access = useAccess();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [formOpen, setFormOpen] = useState(false);
    const [currentRole, setCurrentRole] = useState();
    const [roles, setRoles] = useState([]);
    const [menuTree, setMenuTree] = useState([]);
    const [checkedMenuIds, setCheckedMenuIds] = useState([]);
    const partnerId = Number(getValue(partner, config.idField) || 0);
    const partnerName = getValue(partner, config.nameField) || getValue(partner, config.codeField) || '';
    const permPrefix = `${config.moduleKey}:admin`;
    const currentRoleId = currentRole?.roleId;
    const canQueryRole = access.hasPerms(`${permPrefix}:role:query`);
    const canQueryMenu = access.hasPerms(`${permPrefix}:menu:query`);
    const canAddRole = access.hasPerms(`${permPrefix}:role:add`) && canQueryMenu;
    const canChangeRoleStatus = access.hasPerms(`${permPrefix}:role:edit`);
    const canEditRoleForm = canChangeRoleStatus && canQueryRole && canQueryMenu;
    const loadRoles = async () => {
        if (!partnerId) {
            setRoles([]);
            return;
        }
        setLoading(true);
        try {
            const resp = await config.services.listRoles(partnerId);
            if (resp.code === 200) {
                setRoles((resp.rows || []));
                return;
            }
            message.error(resp.msg || '角色列表加载失败');
        }
        catch {
            message.error('角色列表加载失败，请重试');
        }
        finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (open) {
            void loadRoles();
            return;
        }
        setRoles([]);
    }, [open, partnerId]);
    useEffect(() => {
        if (formOpen) {
            form.resetFields();
            form.setFieldsValue(mapRoleToForm(currentRole));
        }
    }, [formOpen, currentRole?.roleId]);
    const openRoleForm = async (role) => {
        setCurrentRole(role);
        setCheckedMenuIds([]);
        setMenuTree([]);
        setFormOpen(true);
        try {
            if (role?.roleId) {
                const [roleResp, menuResp] = await Promise.all([
                    config.services.getRole(partnerId, role.roleId),
                    config.services.getRoleMenuTree(partnerId, role.roleId),
                ]);
                if (roleResp.code === 200) {
                    setCurrentRole(roleResp.data);
                    form.setFieldsValue(mapRoleToForm(roleResp.data));
                }
                if (menuResp.code === 200) {
                    setMenuTree(menuResp.menus || []);
                    setCheckedMenuIds((menuResp.checkedKeys || []).map(Number));
                }
                return;
            }
            const menuResp = await config.services.getMenuTree();
            if (menuResp.code === 200) {
                setMenuTree(menuResp.data || []);
            }
        }
        catch {
            message.error('角色菜单加载失败，请重试');
        }
    };
    const closeRoleForm = () => {
        setFormOpen(false);
        setCurrentRole(undefined);
        setMenuTree([]);
        setCheckedMenuIds([]);
    };
    const handleSubmit = async () => {
        if (!partnerId) {
            return;
        }
        const values = await form.validateFields();
        const payload = buildRolePayload(currentRole, values, checkedMenuIds);
        setSaving(true);
        try {
            const resp = currentRoleId
                ? await config.services.updateRole(partnerId, payload)
                : await config.services.addRole(partnerId, payload);
            if (resp.code === 200) {
                message.success(currentRoleId ? '角色已更新' : '角色已新增');
                closeRoleForm();
                await loadRoles();
                return;
            }
            message.error(resp.msg || '角色保存失败');
        }
        catch {
            message.error('角色保存失败，请重试');
        }
        finally {
            setSaving(false);
        }
    };
    const handleStatusChange = async (role) => {
        if (!partnerId || !role.roleId) {
            return;
        }
        const nextStatus = role.status === '0' ? '1' : '0';
        const resp = await config.services.changeRoleStatus(partnerId, {
            roleId: role.roleId,
            status: nextStatus,
        });
        if (resp.code === 200) {
            message.success('角色状态已更新');
            await loadRoles();
            return;
        }
        message.error(resp.msg || '角色状态更新失败');
    };
    const handleRemove = (role) => {
        if (!partnerId || !role.roleId) {
            return;
        }
        modal.confirm({
            title: `确认删除角色 ${role.roleName || role.roleId} 吗？`,
            content: '已有账号绑定时，后端会按端内权限规则处理。',
            onOk: async () => {
                const resp = await config.services.removeRoles(partnerId, [role.roleId]);
                if (resp.code === 200) {
                    message.success('角色已删除');
                    await loadRoles();
                    return;
                }
                message.error(resp.msg || '角色删除失败');
            },
        });
    };
    const columns = [
        {
            title: '角色名称',
            dataIndex: 'roleName',
            width: 160,
            render: renderCompactText,
        },
        {
            title: '权限字符',
            dataIndex: 'roleKey',
            width: 160,
            render: renderCompactText,
        },
        {
            title: '排序',
            dataIndex: 'roleSort',
            width: 72,
            render: renderCompactText,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 96,
            render: (value, record) => (_jsx(Tag, { color: value === '0' ? 'success' : 'default', style: { cursor: canChangeRoleStatus ? 'pointer' : 'default' }, onClick: () => {
                    if (canChangeRoleStatus) {
                        void handleStatusChange(record);
                    }
                }, children: value === '0' ? '正常' : '停用' })),
        },
        {
            title: '备注',
            dataIndex: 'remark',
            width: 180,
            render: renderCompactText,
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: 120,
            render: (_, record) => (_jsxs(Flex, { gap: 4, children: [_jsx(Button, { type: "link", size: "small", hidden: !canEditRoleForm, onClick: () => void openRoleForm(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms(`${permPrefix}:role:remove`), onClick: () => handleRemove(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { width: 900, title: `${config.label}角色 - ${partnerName || '-'}`, open: open, destroyOnHidden: true, footer: null, onCancel: () => onOpenChange(false), children: _jsx(Table, { rowKey: (record) => String(record.roleId), loading: loading, columns: columns, dataSource: roles, size: "small", pagination: false, tableLayout: "fixed", title: () => (_jsx(Button, { type: "primary", size: "small", icon: _jsx(PlusOutlined, {}), hidden: !canAddRole, onClick: () => void openRoleForm(), children: "\u65B0\u589E\u89D2\u8272" })) }) }), _jsx(Modal, { width: 680, title: currentRoleId ? '编辑角色' : '新增角色', open: formOpen, destroyOnHidden: true, confirmLoading: saving, onOk: handleSubmit, onCancel: closeRoleForm, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { label: "\u89D2\u8272\u540D\u79F0", name: "roleName", rules: [{ required: true, message: '请输入角色名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u6743\u9650\u5B57\u7B26", name: "roleKey", rules: [{ required: true, message: '请输入权限字符' }], children: _jsx(Input, { placeholder: "\u4F8B\u5982 admin / staff" }) }), _jsx(Form.Item, { label: "\u6392\u5E8F", name: "roleSort", rules: [{ required: true, message: '请输入排序' }], children: _jsx(InputNumber, { min: 0, precision: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u72B6\u6001", name: "status", rules: [{ required: true, message: '请选择状态' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: statusOptions }) }), _jsx(Form.Item, { label: "\u83DC\u5355\u6743\u9650", children: _jsx("div", { style: { maxHeight: 260, overflow: 'auto', border: '1px solid #f0f0f0', padding: 8 }, children: menuTree.length > 0 ? (_jsx(Tree, { checkable: true, checkedKeys: checkedMenuIds, treeData: toTreeData(menuTree), defaultExpandAll: true, onCheck: (checked) => {
                                        const keys = Array.isArray(checked) ? checked : checked.checked;
                                        setCheckedMenuIds(normalizeKeys(keys));
                                    } })) : (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u53EF\u5206\u914D\u83DC\u5355" })) }) }), _jsx(Form.Item, { label: "\u5907\u6CE8", name: "remark", children: _jsx(Input.TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165" }) })] }) })] }));
};
export default PartnerRoleModal;
