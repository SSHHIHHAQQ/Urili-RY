import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { useAccess } from '@umijs/max';
import { App, Button, Dropdown, Flex, Form, Input, Modal, Select, Space, Table, Tag, TreeSelect, Typography, } from 'antd';
import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import { getDictSelectOption } from '@/services/system/dict';
import { openPortalDirectLoginWindow } from '@/utils/portalDirectLoginMessage';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import PartnerAccountRoleModal from './PartnerAccountRoleModal';
import PartnerAuditModal from './PartnerAuditModal';
import PartnerSessionModal from './PartnerSessionModal';
const DEFAULT_ACCOUNT_PASSWORD = 'U12346';
const PASSWORD_MIN_LENGTH = 5;
const PASSWORD_MAX_LENGTH = 20;
const fallbackAccountRoleOptions = [
    { label: '负责人', value: 'OWNER', searchText: 'owner 负责人' },
    { label: '管理员', value: 'ADMIN', searchText: 'admin 管理员' },
    { label: '普通账号', value: 'STAFF', searchText: 'staff 普通账号' },
];
const statusOptions = [
    { label: '正常', value: '0', searchText: '0 正常' },
    { label: '停用', value: '1', searchText: '1 停用' },
];
const lockStatusOptions = [
    { label: '未锁定', value: '0', searchText: '0 未锁定' },
    { label: '已锁定', value: '1', searchText: '1 已锁定' },
];
const compactCellTextStyle = {
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    lineHeight: 1.35,
};
const compactSubTextStyle = {
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    lineHeight: 1.35,
};
function normalizeDictSelectOptions(options, fallback) {
    const normalized = [];
    (options || []).forEach((option) => {
        const value = option.value == null ? '' : String(option.value);
        const label = option.label ?? option.text ?? value;
        const labelText = typeof label === 'string' ? label : String(label);
        if (!value) {
            return;
        }
        normalized.push({
            label: labelText,
            value,
            searchText: `${value} ${labelText}`.toLowerCase(),
        });
    });
    return normalized.length > 0 ? normalized : fallback;
}
function getValue(record, field) {
    return record ? record[field] : undefined;
}
function getAccountId(config, account) {
    return account ? account[config.accountIdField] || account.accountId : undefined;
}
function formatDateTimeText(value) {
    if (!value) {
        return '-';
    }
    const text = String(value).trim();
    if (!text || text.toLowerCase() === 'invalid date') {
        return '-';
    }
    return text.replace('T', ' ').replace(/\.\d{3}Z?$/, '');
}
function renderCompactText(value) {
    const text = value == null || value === '' ? '-' : String(value);
    return _jsx(Typography.Text, { style: compactCellTextStyle, title: text, children: text });
}
function optionLabel(options, value) {
    return options.find((option) => option.value === value)?.label || value || '-';
}
function isAccountLocked(account) {
    return account.lockStatus === '1';
}
function buildAccountPayload(config, partnerId, currentAccount, values) {
    const currentAccountId = getAccountId(config, currentAccount);
    return {
        accountId: currentAccountId,
        [config.accountIdField]: currentAccountId,
        [config.idField]: partnerId,
        userName: values.userName,
        nickName: values.nickName,
        password: currentAccountId ? undefined : values.password || DEFAULT_ACCOUNT_PASSWORD,
        deptId: values.deptId,
        accountRole: values.accountRole || 'STAFF',
        status: values.status || '0',
        email: values.email,
        phonenumber: values.phonenumber,
        remark: values.remark,
    };
}
function mapAccountToForm(account) {
    return {
        userName: account?.userName,
        nickName: account?.nickName,
        password: DEFAULT_ACCOUNT_PASSWORD,
        deptId: account?.deptId,
        accountRole: account?.accountRole || 'STAFF',
        status: account?.status || '0',
        email: account?.email,
        phonenumber: account?.phonenumber,
        remark: account?.remark,
    };
}
const PartnerAccountModal = ({ config, open, partner, onOpenChange, }) => {
    const { message, modal } = App.useApp();
    const access = useAccess();
    const [accountForm] = Form.useForm();
    const [resetPasswordForm] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [accounts, setAccounts] = useState([]);
    const [deptTree, setDeptTree] = useState([]);
    const [accountRoleOptions, setAccountRoleOptions] = useState(fallbackAccountRoleOptions);
    const [accountLockStatusOptions, setAccountLockStatusOptions] = useState(lockStatusOptions);
    const [accountFormOpen, setAccountFormOpen] = useState(false);
    const [currentAccount, setCurrentAccount] = useState();
    const [roleModalOpen, setRoleModalOpen] = useState(false);
    const [roleAccount, setRoleAccount] = useState();
    const [sessionModalOpen, setSessionModalOpen] = useState(false);
    const [sessionAccount, setSessionAccount] = useState();
    const [auditModalOpen, setAuditModalOpen] = useState(false);
    const [auditAccount, setAuditAccount] = useState();
    const partnerId = Number(getValue(partner, config.idField) || 0);
    const partnerName = getValue(partner, config.nameField) || getValue(partner, config.codeField) || '';
    const permPrefix = `${config.moduleKey}:admin`;
    const accountPermissions = config.accountPermissions ?? {
        list: `${permPrefix}:account:list`,
        add: `${permPrefix}:account:add`,
        edit: `${permPrefix}:account:edit`,
        resetPwd: `${permPrefix}:account:resetPwd`,
        roleQuery: `${permPrefix}:account:role:query`,
        roleEdit: `${permPrefix}:account:role:edit`,
    };
    const canAssignAccountRoles = access.hasPerms(accountPermissions.roleQuery)
        && access.hasPerms(accountPermissions.roleEdit);
    const canQueryDept = access.hasPerms(`${permPrefix}:dept:query`);
    const canViewAccountAudit = access.hasPerms(`${permPrefix}:loginLog:list`)
        || access.hasPerms(`${permPrefix}:operLog:list`)
        || access.hasPerms(`${permPrefix}:ticket:list`);
    const accountLockEnabled = Boolean(config.services.lockAccount && config.services.unlockAccount);
    const canLockAccount = accountLockEnabled
        && Boolean(accountPermissions.lock)
        && access.hasPerms(accountPermissions.lock);
    const currentAccountId = getAccountId(config, currentAccount);
    const roleSelectOptions = useMemo(() => accountRoleOptions.map((option) => ({
        ...option,
        disabled: option.value === 'OWNER' && currentAccount?.accountRole !== 'OWNER',
    })), [accountRoleOptions, currentAccount?.accountRole]);
    const loadDeptTree = async () => {
        if (!partnerId || !canQueryDept) {
            setDeptTree([]);
            return;
        }
        try {
            const resp = await config.services.getDeptTree(partnerId);
            if (resp.code === 200) {
                setDeptTree(resp.data || []);
                return;
            }
            setDeptTree([]);
        }
        catch {
            setDeptTree([]);
        }
    };
    const loadAccounts = async () => {
        if (!partnerId) {
            setAccounts([]);
            setDeptTree([]);
            return;
        }
        setLoading(true);
        try {
            const accountResp = await config.services.getAccounts(partnerId);
            if (accountResp.code === 200) {
                setAccounts((accountResp.data || []));
            }
            else {
                message.error(accountResp.msg || '账号列表加载失败');
            }
        }
        catch {
            message.error('账号列表加载失败，请重试');
        }
        finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (open) {
            void loadAccounts();
            void loadDeptTree();
            getDictSelectOption(`${config.moduleKey}_account_role`)
                .then((data) => setAccountRoleOptions(normalizeDictSelectOptions(data, fallbackAccountRoleOptions)))
                .catch(() => setAccountRoleOptions(fallbackAccountRoleOptions));
            if (accountLockEnabled) {
                getDictSelectOption(`${config.moduleKey}_account_lock_status`)
                    .then((data) => setAccountLockStatusOptions(normalizeDictSelectOptions(data, lockStatusOptions)))
                    .catch(() => setAccountLockStatusOptions(lockStatusOptions));
            }
        }
    }, [open, partnerId, config.moduleKey, accountLockEnabled, canQueryDept]);
    const openAccountForm = (account) => {
        setCurrentAccount(account);
        accountForm.resetFields();
        accountForm.setFieldsValue(mapAccountToForm(account));
        setAccountFormOpen(true);
    };
    const closeAccountForm = () => {
        setAccountFormOpen(false);
        setCurrentAccount(undefined);
    };
    const openAccountRoleModal = (account) => {
        setRoleAccount(account);
        setRoleModalOpen(true);
    };
    const closeAccountRoleModal = () => {
        setRoleModalOpen(false);
        setRoleAccount(undefined);
    };
    const handleAccountSubmit = async () => {
        if (!partnerId) {
            return;
        }
        const values = await accountForm.validateFields();
        const payload = buildAccountPayload(config, partnerId, currentAccount, values);
        setSaving(true);
        try {
            const resp = currentAccountId
                ? await config.services.updateAccount(partnerId, payload)
                : await config.services.addAccount(partnerId, payload);
            if (resp.code === 200) {
                message.success(currentAccountId ? '账号已更新' : '账号已新增');
                closeAccountForm();
                await loadAccounts();
                return;
            }
            message.error(resp.msg || '账号保存失败');
        }
        catch {
            message.error('账号保存失败，请重试');
        }
        finally {
            setSaving(false);
        }
    };
    const handleResetPassword = (account) => {
        const accountId = getAccountId(config, account);
        if (!partnerId || !accountId) {
            return;
        }
        resetPasswordForm.resetFields();
        modal.confirm({
            title: `确认重置账号 ${account.userName || account.nickName || accountId} 的密码吗？`,
            okText: '重置',
            content: (_jsxs(Form, { form: resetPasswordForm, layout: "vertical", children: [_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u8F93\u5165\u4E34\u65F6\u5BC6\u7801\u3002\u91CD\u7F6E\u540E\u8BE5\u8D26\u53F7\u5F53\u524D\u5728\u7EBF\u4F1A\u8BDD\u4F1A\u7ACB\u5373\u5931\u6548\u3002" }), _jsx(Form.Item, { label: "\u4E34\u65F6\u5BC6\u7801", name: "password", rules: [
                            { required: true, message: '请输入临时密码' },
                            {
                                validator: (_, value) => {
                                    const password = String(value || '');
                                    if (!password.trim()) {
                                        return Promise.reject(new Error('请输入临时密码'));
                                    }
                                    if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
                                        return Promise.reject(new Error('密码长度必须在5到20个字符之间'));
                                    }
                                    return Promise.resolve();
                                },
                            },
                        ], children: _jsx(Input.Password, { placeholder: "\u8BF7\u8F93\u51655-20\u4F4D\u4E34\u65F6\u5BC6\u7801" }) }), _jsx(Form.Item, { dependencies: ['password'], label: "\u786E\u8BA4\u5BC6\u7801", name: "confirmPassword", rules: [
                            { required: true, message: '请再次输入临时密码' },
                            ({ getFieldValue }) => ({
                                validator: (_, value) => {
                                    if (!value || getFieldValue('password') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject(new Error('两次密码输入不一致'));
                                },
                            }),
                        ], children: _jsx(Input.Password, { placeholder: "\u8BF7\u518D\u6B21\u8F93\u5165\u4E34\u65F6\u5BC6\u7801" }) })] })),
            onOk: async () => {
                const values = await resetPasswordForm.validateFields();
                const resp = await config.services.resetAccountPassword(partnerId, accountId, values.password || '');
                if (resp.code === 200) {
                    message.success('账号密码已重置');
                    return;
                }
                message.error(resp.msg || '密码重置失败');
                throw new Error('RESET_ACCOUNT_PASSWORD_FAILED');
            },
        });
    };
    const handleForceLogoutAccount = (account) => {
        const accountId = getAccountId(config, account);
        if (!partnerId || !accountId) {
            return;
        }
        modal.confirm({
            title: `确认强制踢出账号 ${account.userName || account.nickName || accountId} 吗？`,
            content: '该账号当前在线会话会立即失效。',
            onOk: async () => {
                const resp = await config.services.forceLogoutAccount(partnerId, accountId);
                if (resp.code === 200) {
                    message.success('账号在线会话已强制踢出');
                    return;
                }
                message.error(resp.msg || '强制踢出失败');
            },
        });
    };
    const handleLockAccount = (account) => {
        const accountId = getAccountId(config, account);
        if (!partnerId || !accountId || !config.services.lockAccount) {
            return;
        }
        let lockReason = '';
        modal.confirm({
            title: `确认锁定账号 ${account.userName || account.nickName || accountId} 吗？`,
            okText: '锁定',
            content: (_jsxs(Flex, { vertical: true, gap: 8, children: [_jsx(Typography.Text, { children: "\u9501\u5B9A\u540E\u8BE5\u8D26\u53F7\u4E0D\u53EF\u767B\u5F55\uFF0C\u5F53\u524D\u5728\u7EBF\u4F1A\u8BDD\u4F1A\u7ACB\u5373\u5931\u6548\u3002" }), _jsx(Typography.Text, { children: "\u9501\u5B9A\u539F\u56E0" }), _jsx(Input.TextArea, { rows: 3, maxLength: 500, showCount: true, placeholder: "\u8BF7\u8F93\u5165\u9501\u5B9A\u539F\u56E0", onChange: (event) => {
                            lockReason = event.target.value;
                        } })] })),
            onOk: async () => {
                const normalizedReason = lockReason.trim();
                if (!normalizedReason) {
                    message.error('请输入锁定原因');
                    throw new Error('LOCK_REASON_REQUIRED');
                }
                const resp = await config.services.lockAccount?.(partnerId, accountId, normalizedReason);
                if (resp?.code === 200) {
                    message.success('账号已锁定');
                    await loadAccounts();
                    return;
                }
                message.error(resp?.msg || '账号锁定失败');
                throw new Error('LOCK_ACCOUNT_FAILED');
            },
        });
    };
    const handleUnlockAccount = (account) => {
        const accountId = getAccountId(config, account);
        if (!partnerId || !accountId || !config.services.unlockAccount) {
            return;
        }
        modal.confirm({
            title: `确认解锁账号 ${account.userName || account.nickName || accountId} 吗？`,
            content: '解锁后不会恢复旧会话，账号需要重新登录。',
            onOk: async () => {
                const resp = await config.services.unlockAccount?.(partnerId, accountId);
                if (resp?.code === 200) {
                    message.success('账号已解锁');
                    await loadAccounts();
                    return;
                }
                message.error(resp?.msg || '账号解锁失败');
                throw new Error('UNLOCK_ACCOUNT_FAILED');
            },
        });
    };
    const handleDirectLoginAccount = (account) => {
        const accountId = getAccountId(config, account);
        const directLoginAccount = config.services.directLoginAccount;
        if (!partnerId || !accountId || !directLoginAccount) {
            return;
        }
        let reason = '';
        modal.confirm({
            title: `生成${config.label}端账号免密登录链接`,
            okText: '生成并打开',
            content: (_jsxs(Flex, { vertical: true, gap: 8, children: [_jsx(Typography.Text, { children: "\u4EE3\u5165\u539F\u56E0" }), _jsx(Input.TextArea, { rows: 3, maxLength: 255, showCount: true, placeholder: "\u4F8B\u5982\uFF1A\u534F\u52A9\u5BA2\u6237\u6392\u67E5\u8BA2\u5355\u95EE\u9898", onChange: (event) => {
                            reason = event.target.value;
                        } })] })),
            onOk: async () => {
                const normalizedReason = reason.trim();
                if (!normalizedReason) {
                    message.error('请输入免密登录原因');
                    throw new Error('DIRECT_LOGIN_REASON_REQUIRED');
                }
                const hide = message.loading('正在生成免密登录链接');
                try {
                    const resp = await directLoginAccount(partnerId, accountId, normalizedReason);
                    const bridgeResult = resp?.code === 200
                        ? await openPortalDirectLoginWindow(resp.data, config.moduleKey)
                        : false;
                    if (bridgeResult) {
                        message.success(`${config.label}端账号免密登录已确认，有效期 ${resp.data.expireMinutes || 30} 分钟`);
                        return;
                    }
                    message.error(resp?.msg || '免密登录链接生成失败');
                    throw new Error('DIRECT_LOGIN_FAILED');
                }
                catch (error) {
                    if (!(error instanceof Error && error.message === 'DIRECT_LOGIN_FAILED')) {
                        message.error('免密登录链接生成失败，请重试');
                    }
                    throw error;
                }
                finally {
                    hide();
                }
            },
        });
    };
    const columns = [
        {
            title: '登录账号',
            dataIndex: 'userName',
            width: 128,
            render: renderCompactText,
        },
        {
            title: '姓名',
            dataIndex: 'nickName',
            width: 100,
            render: renderCompactText,
        },
        {
            title: '部门',
            dataIndex: 'deptName',
            width: 108,
            render: renderCompactText,
        },
        {
            title: '角色',
            dataIndex: 'accountRole',
            width: 88,
            render: (value) => _jsx(Tag, { color: value === 'OWNER' ? 'blue' : 'default', children: optionLabel(accountRoleOptions, value) }),
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 76,
            render: (value) => _jsx(Tag, { color: value === '0' ? 'success' : 'default', children: optionLabel(statusOptions, value) }),
        },
        ...(accountLockEnabled ? [{
                title: '锁定',
                dataIndex: 'lockStatus',
                width: 86,
                render: (value, record) => (_jsx(Tag, { color: value === '1' ? 'error' : 'success', title: record.lockReason || undefined, children: optionLabel(accountLockStatusOptions, value || '0') })),
            }] : []),
        {
            title: '时间',
            dataIndex: 'timeInfo',
            width: 148,
            render: (_, record) => (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { style: compactCellTextStyle, children: formatDateTimeText(record.createTime) }), _jsx(Typography.Text, { style: compactSubTextStyle, type: "secondary", children: formatDateTimeText(record.lastLoginTime) })] })),
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: 160,
            render: (_, record) => {
                const locked = isAccountLocked(record);
                const moreItems = [
                    access.hasPerms(`${permPrefix}:directLogin`) && config.services.directLoginAccount && !locked
                        ? { key: 'directLogin', label: `登录${config.label}端` }
                        : null,
                    canLockAccount
                        ? { key: locked ? 'unlockAccount' : 'lockAccount', label: locked ? '解锁账号' : '锁定账号' }
                        : null,
                    access.hasPerms(accountPermissions.resetPwd)
                        ? { key: 'resetPwd', label: '重置密码' }
                        : null,
                    access.hasPerms(`${permPrefix}:forceLogout`) && config.services.listAccountSessions
                        ? { key: 'sessions', label: '会话' }
                        : null,
                    canViewAccountAudit
                        ? { key: 'audit', label: '审计' }
                        : null,
                    access.hasPerms(`${permPrefix}:forceLogout`)
                        ? { key: 'forceLogout', label: '强制踢出' }
                        : null,
                ].filter(Boolean);
                return (_jsxs(Space, { size: 4, children: [_jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms(accountPermissions.edit), onClick: () => openAccountForm(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", hidden: !canAssignAccountRoles, onClick: () => openAccountRoleModal(record), children: "\u5206\u914D\u89D2\u8272" }), moreItems && moreItems.length > 0 ? (_jsx(Dropdown, { menu: {
                                items: moreItems,
                                onClick: ({ key }) => {
                                    if (key === 'directLogin') {
                                        handleDirectLoginAccount(record);
                                    }
                                    if (key === 'lockAccount') {
                                        handleLockAccount(record);
                                    }
                                    if (key === 'unlockAccount') {
                                        handleUnlockAccount(record);
                                    }
                                    if (key === 'resetPwd') {
                                        handleResetPassword(record);
                                    }
                                    if (key === 'sessions') {
                                        setSessionAccount(record);
                                        setSessionModalOpen(true);
                                    }
                                    if (key === 'audit') {
                                        setAuditAccount(record);
                                        setAuditModalOpen(true);
                                    }
                                    if (key === 'forceLogout') {
                                        handleForceLogoutAccount(record);
                                    }
                                },
                            }, children: _jsxs(Button, { type: "link", size: "small", children: ["\u66F4\u591A ", _jsx(DownOutlined, {})] }) })) : null] }));
            },
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { width: 1040, title: `${config.label}账号 - ${partnerName || '-'}`, open: open, destroyOnHidden: true, footer: null, onCancel: () => onOpenChange(false), children: _jsx(Table, { rowKey: (record) => String(getAccountId(config, record) || record.userName), loading: loading, columns: columns, dataSource: accounts, size: "small", pagination: false, tableLayout: "fixed", title: () => (_jsx(Button, { type: "primary", size: "small", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms(accountPermissions.add), onClick: () => openAccountForm(), children: "\u65B0\u589E\u8D26\u53F7" })) }) }), _jsx(PartnerSessionModal, { config: config, open: sessionModalOpen, partner: partner, account: sessionAccount, onOpenChange: (nextOpen) => {
                    setSessionModalOpen(nextOpen);
                    if (!nextOpen) {
                        setSessionAccount(undefined);
                    }
                } }), _jsx(PartnerAuditModal, { config: config, open: auditModalOpen, partner: partner, account: auditAccount, onOpenChange: (nextOpen) => {
                    setAuditModalOpen(nextOpen);
                    if (!nextOpen) {
                        setAuditAccount(undefined);
                    }
                } }), _jsx(PartnerAccountRoleModal, { config: config, partnerId: partnerId, account: roleAccount, open: roleModalOpen, onOpenChange: (nextOpen) => {
                    if (nextOpen) {
                        setRoleModalOpen(true);
                        return;
                    }
                    closeAccountRoleModal();
        } }), _jsx(Modal, { width: 640, title: currentAccountId ? '编辑账号' : '新增账号', open: accountFormOpen, destroyOnHidden: true, forceRender: true, confirmLoading: saving, onOk: handleAccountSubmit, onCancel: closeAccountForm, children: _jsxs(Form, { form: accountForm, layout: "vertical", children: [_jsx(Form.Item, { label: "\u767B\u5F55\u8D26\u53F7", name: "userName", rules: [{ required: true, message: '请输入登录账号' }], children: _jsx(Input, { disabled: Boolean(currentAccountId), placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u59D3\u540D", name: "nickName", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), !currentAccountId ? (_jsx(Form.Item, { label: "\u521D\u59CB\u5BC6\u7801", name: "password", rules: [{ required: true, message: '请输入初始密码' }], children: _jsx(Input.Password, { placeholder: "\u8BF7\u8F93\u5165" }) })) : null, _jsx(Form.Item, { label: "\u90E8\u95E8", name: "deptId", children: _jsx(TreeSelect, { ...SEARCHABLE_TREE_SELECT_PROPS, allowClear: true, disabled: !canQueryDept, treeDefaultExpandAll: true, placeholder: canQueryDept ? '\u8BF7\u9009\u62E9' : '\u65E0\u90E8\u95E8\u67E5\u8BE2\u6743\u9650', treeData: deptTree, fieldNames: { label: 'label', value: 'id', children: 'children' } }) }), _jsx(Form.Item, { label: "\u8D26\u53F7\u89D2\u8272", name: "accountRole", rules: [{ required: true, message: '请选择账号角色' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: roleSelectOptions }) }), _jsx(Form.Item, { label: "\u72B6\u6001", name: "status", rules: [{ required: true, message: '请选择状态' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: statusOptions }) }), _jsx(Form.Item, { label: "\u624B\u673A", name: "phonenumber", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u90AE\u7BB1", name: "email", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u5907\u6CE8", name: "remark", children: _jsx(Input.TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165" }) })] }) })] }));
};
export default PartnerAccountModal;
