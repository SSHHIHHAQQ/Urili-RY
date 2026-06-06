import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { App, Checkbox, Empty, Flex, Modal, Spin, Typography } from 'antd';
function getAccountId(config, account) {
    return account ? account[config.accountIdField] || account.accountId : undefined;
}
function normalizeRoleIds(roleIds) {
    return (roleIds || [])
        .map((roleId) => Number(roleId))
        .filter((roleId) => Number.isFinite(roleId));
}
const PartnerAccountRoleModal = ({ config, partnerId, account, open, onOpenChange, }) => {
    const { message } = App.useApp();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [roles, setRoles] = useState([]);
    const [selectedRoleIds, setSelectedRoleIds] = useState([]);
    const accountId = Number(getAccountId(config, account) || 0);
    const accountTitle = account?.userName || account?.nickName || accountId || '-';
    const roleItems = useMemo(() => roles.filter((role) => role.roleId != null), [roles]);
    const loadRoles = async () => {
        if (!partnerId || !accountId) {
            setRoles([]);
            setSelectedRoleIds([]);
            return;
        }
        setLoading(true);
        try {
            const resp = await config.services.getAccountRoles(partnerId, accountId);
            if (resp.code === 200) {
                setRoles(resp.roles || []);
                setSelectedRoleIds(normalizeRoleIds(resp.checkedKeys));
                return;
            }
            message.error(resp.msg || '角色加载失败');
        }
        catch {
            message.error('角色加载失败，请重试');
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
        setSelectedRoleIds([]);
    }, [open, partnerId, accountId]);
    const handleSubmit = async () => {
        if (!partnerId || !accountId) {
            return;
        }
        setSaving(true);
        try {
            const resp = await config.services.assignAccountRoles(partnerId, accountId, selectedRoleIds);
            if (resp.code === 200) {
                message.success('角色已更新');
                onOpenChange(false);
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
    return (_jsx(Modal, { width: 560, title: `分配角色 - ${accountTitle}`, open: open, destroyOnHidden: true, confirmLoading: saving, onOk: handleSubmit, onCancel: () => onOpenChange(false), children: _jsx(Spin, { spinning: loading, children: roleItems.length > 0 ? (_jsx(Checkbox.Group, { value: selectedRoleIds, onChange: (values) => setSelectedRoleIds(normalizeRoleIds(values)), style: { width: '100%' }, children: _jsx(Flex, { vertical: true, gap: 10, style: { width: '100%' }, children: roleItems.map((role) => {
                        const roleName = role.roleName || role.roleKey || String(role.roleId);
                        return (_jsx(Checkbox, { value: role.roleId, disabled: role.status === '1', children: _jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { children: roleName }), _jsx(Typography.Text, { type: "secondary", children: role.roleKey || '-' })] }) }, role.roleId));
                    }) }) })) : (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u53EF\u5206\u914D\u89D2\u8272" })) }) }));
};
export default PartnerAccountRoleModal;
