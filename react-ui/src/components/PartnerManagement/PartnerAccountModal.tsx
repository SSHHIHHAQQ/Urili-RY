import React, { useEffect, useMemo, useState } from 'react';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Dropdown,
  Flex,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import type { MenuProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import { getDictSelectOption } from '@/services/system/dict';
import { openPortalDirectLoginWindow } from '@/utils/portalDirectLoginMessage';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';
import PartnerAccountRoleModal from './PartnerAccountRoleModal';
import PartnerAuditModal from './PartnerAuditModal';
import PartnerSessionModal from './PartnerSessionModal';

type PartnerRecord = Record<string, any>;
type AccountRecord = API.Partner.PortalAccountBase & Record<string, any>;

type SelectOption = {
  label: string;
  value: string;
  disabled?: boolean;
  searchText?: string;
};

type DictSelectRawOption = {
  label?: React.ReactNode;
  text?: React.ReactNode;
  value?: string | number;
};

type PartnerAccountModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  partner?: PartnerRecord;
  onOpenChange: (open: boolean) => void;
};

type AccountFormValues = {
  userName?: string;
  nickName?: string;
  password?: string;
  deptId?: number;
  accountRole?: string;
  status?: string;
  email?: string;
  phonenumber?: string;
  remark?: string;
};

type ResetPasswordFormValues = {
  password?: string;
  confirmPassword?: string;
};

const PASSWORD_MIN_LENGTH = 5;
const PASSWORD_MAX_LENGTH = 20;

function normalizePassword(value: unknown) {
  const password = String(value ?? '').trim();
  return password || undefined;
}

const passwordRules = [
  { required: true, message: '请输入密码' },
  {
    validator: (_: unknown, value: unknown) => {
      const password = normalizePassword(value);
      if (!password) {
        return Promise.reject(new Error('请输入密码'));
      }
      if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
        return Promise.reject(new Error('密码长度必须在5到20个字符之间'));
      }
      return Promise.resolve();
    },
  },
];

const fallbackAccountRoleOptions: SelectOption[] = [
  { label: '负责人', value: 'OWNER', searchText: 'owner 负责人' },
  { label: '管理员', value: 'ADMIN', searchText: 'admin 管理员' },
  { label: '普通账号', value: 'STAFF', searchText: 'staff 普通账号' },
];

const statusOptions: SelectOption[] = [
  { label: '正常', value: '0', searchText: '0 正常' },
  { label: '停用', value: '1', searchText: '1 停用' },
];

const lockStatusOptions: SelectOption[] = [
  { label: '未锁定', value: '0', searchText: '0 未锁定' },
  { label: '已锁定', value: '1', searchText: '1 已锁定' },
];

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
};

const compactSubTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
};

function normalizeDictSelectOptions(
  options: DictSelectRawOption[] | undefined,
  fallback: SelectOption[],
) {
  const normalized: SelectOption[] = [];
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

function getValue(record: PartnerRecord | undefined, field: string) {
  return record ? record[field] : undefined;
}

function getAccountId(config: PartnerModuleConfig, account?: AccountRecord) {
  return account ? account[config.accountIdField] || account.accountId : undefined;
}

function formatDateTimeText(value: unknown) {
  if (!value) {
    return '-';
  }
  const text = String(value).trim();
  if (!text || text.toLowerCase() === 'invalid date') {
    return '-';
  }
  return text.replace('T', ' ').replace(/\.\d{3}Z?$/, '');
}

function renderCompactText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

function optionLabel(options: SelectOption[], value?: string) {
  return options.find((option) => option.value === value)?.label || value || '-';
}

function isAccountLocked(account: AccountRecord) {
  return account.lockStatus === '1';
}

function buildAccountPayload(
  config: PartnerModuleConfig,
  partnerId: number,
  currentAccount: AccountRecord | undefined,
  values: AccountFormValues,
) {
  const currentAccountId = getAccountId(config, currentAccount);
  return {
    accountId: currentAccountId,
    [config.accountIdField]: currentAccountId,
    [config.idField]: partnerId,
    userName: values.userName,
    nickName: values.nickName,
    password: currentAccountId ? undefined : normalizePassword(values.password),
    deptId: values.deptId,
    accountRole: values.accountRole || 'STAFF',
    status: values.status || '0',
    email: values.email,
    phonenumber: values.phonenumber,
    remark: values.remark,
  };
}

function mapAccountToForm(account?: AccountRecord): AccountFormValues {
  return {
    userName: account?.userName,
    nickName: account?.nickName,
    password: undefined,
    deptId: account?.deptId,
    accountRole: account?.accountRole || 'STAFF',
    status: account?.status || '0',
    email: account?.email,
    phonenumber: account?.phonenumber,
    remark: account?.remark,
  };
}

const PartnerAccountModal: React.FC<PartnerAccountModalProps> = ({
  config,
  open,
  partner,
  onOpenChange,
}) => {
  const { message, modal } = App.useApp();
  const access = useAccess();
  const [accountForm] = Form.useForm<AccountFormValues>();
  const [resetPasswordForm] = Form.useForm<ResetPasswordFormValues>();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [accounts, setAccounts] = useState<AccountRecord[]>([]);
  const [deptTree, setDeptTree] = useState<API.Partner.PortalTreeNode[]>([]);
  const [accountRoleOptions, setAccountRoleOptions] = useState<SelectOption[]>(fallbackAccountRoleOptions);
  const [accountLockStatusOptions, setAccountLockStatusOptions] = useState<SelectOption[]>(lockStatusOptions);
  const [accountFormOpen, setAccountFormOpen] = useState(false);
  const [currentAccount, setCurrentAccount] = useState<AccountRecord>();
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [roleAccount, setRoleAccount] = useState<AccountRecord>();
  const [sessionModalOpen, setSessionModalOpen] = useState(false);
  const [sessionAccount, setSessionAccount] = useState<AccountRecord>();
  const [auditModalOpen, setAuditModalOpen] = useState(false);
  const [auditAccount, setAuditAccount] = useState<AccountRecord>();

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
  const canQueryRole = access.hasPerms(`${permPrefix}:role:query`);
  const canAssignAccountRoles = canQueryRole
    && access.hasPerms(accountPermissions.roleQuery)
    && access.hasPerms(accountPermissions.roleEdit);
  const canQueryDept = access.hasPerms(`${permPrefix}:dept:query`);
  const canViewAccountAudit = access.hasPerms(`${permPrefix}:loginLog:list`)
    || access.hasPerms(`${permPrefix}:operLog:list`)
    || access.hasPerms(`${permPrefix}:ticket:list`);
  const accountLockEnabled = Boolean(config.services.lockAccount && config.services.unlockAccount);
  const canLockAccount = accountLockEnabled
    && Boolean(accountPermissions.lock)
    && access.hasPerms(accountPermissions.lock as string);
  const currentAccountId = getAccountId(config, currentAccount);

  const roleSelectOptions = useMemo(
    () => accountRoleOptions.map((option) => ({
      ...option,
      disabled: option.value === 'OWNER' && currentAccount?.accountRole !== 'OWNER',
    })),
    [accountRoleOptions, currentAccount?.accountRole],
  );

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
    } catch {
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
        setAccounts((accountResp.data || []) as AccountRecord[]);
      } else {
        message.error(accountResp.msg || '账号列表加载失败');
      }
    } catch {
      message.error('账号列表加载失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      void loadAccounts();
      void loadDeptTree();
      getDictSelectOption(`${config.moduleKey}_account_role`)
        .then((data) => setAccountRoleOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackAccountRoleOptions)))
        .catch(() => setAccountRoleOptions(fallbackAccountRoleOptions));
      if (accountLockEnabled) {
        getDictSelectOption(`${config.moduleKey}_account_lock_status`)
          .then((data) => setAccountLockStatusOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], lockStatusOptions)))
          .catch(() => setAccountLockStatusOptions(lockStatusOptions));
      }
    }
  }, [open, partnerId, config.moduleKey, accountLockEnabled, canQueryDept]);

  const openAccountForm = (account?: AccountRecord) => {
    setCurrentAccount(account);
    accountForm.resetFields();
    accountForm.setFieldsValue(mapAccountToForm(account));
    setAccountFormOpen(true);
  };

  const closeAccountForm = () => {
    setAccountFormOpen(false);
    setCurrentAccount(undefined);
  };

  const openAccountRoleModal = (account: AccountRecord) => {
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
    } catch {
      message.error('账号保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleResetPassword = (account: AccountRecord) => {
    const accountId = getAccountId(config, account);
    if (!partnerId || !accountId) {
      return;
    }
    resetPasswordForm.resetFields();
    modal.confirm({
      title: `确认重置账号 ${account.userName || account.nickName || accountId} 的密码吗？`,
      okText: '重置',
      content: (
        <Form form={resetPasswordForm} layout="vertical">
          <Typography.Text type="secondary">
            请输入临时密码。重置后该账号当前在线会话会立即失效。
          </Typography.Text>
          <Form.Item
            label="临时密码"
            name="password"
            rules={[
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
            ]}
          >
            <Input.Password placeholder="请输入5-20位临时密码" />
          </Form.Item>
          <Form.Item
            dependencies={['password']}
            label="确认密码"
            name="confirmPassword"
            rules={[
              { required: true, message: '请再次输入临时密码' },
              ({ getFieldValue }) => ({
                validator: (_, value) => {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次密码输入不一致'));
                },
              }),
            ]}
          >
            <Input.Password placeholder="请再次输入临时密码" />
          </Form.Item>
        </Form>
      ),
      onOk: async () => {
        const values = await resetPasswordForm.validateFields();
        const resp = await config.services.resetAccountPassword(partnerId, accountId, normalizePassword(values.password) || '');
        if (resp.code === 200) {
          message.success('账号密码已重置');
          return;
        }
        message.error(resp.msg || '密码重置失败');
        throw new Error('RESET_ACCOUNT_PASSWORD_FAILED');
      },
    });
  };

  const handleForceLogoutAccount = (account: AccountRecord) => {
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

  const handleLockAccount = (account: AccountRecord) => {
    const accountId = getAccountId(config, account);
    if (!partnerId || !accountId || !config.services.lockAccount) {
      return;
    }
    let lockReason = '';
    modal.confirm({
      title: `确认锁定账号 ${account.userName || account.nickName || accountId} 吗？`,
      okText: '锁定',
      content: (
        <Flex vertical gap={8}>
          <Typography.Text>锁定后该账号不可登录，当前在线会话会立即失效。</Typography.Text>
          <Typography.Text>锁定原因</Typography.Text>
          <Input.TextArea
            rows={3}
            maxLength={500}
            showCount
            placeholder="请输入锁定原因"
            onChange={(event) => {
              lockReason = event.target.value;
            }}
          />
        </Flex>
      ),
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

  const handleUnlockAccount = (account: AccountRecord) => {
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

  const handleDirectLoginAccount = (account: AccountRecord) => {
    const accountId = getAccountId(config, account);
    const directLoginAccount = config.services.directLoginAccount;
    if (!partnerId || !accountId || !directLoginAccount) {
      return;
    }
    let reason = '';
    modal.confirm({
      title: `生成${config.label}端账号免密登录链接`,
      okText: '生成并打开',
      content: (
        <Flex vertical gap={8}>
          <Typography.Text>代入原因（选填）</Typography.Text>
          <Input.TextArea
            rows={3}
            maxLength={255}
            showCount
            placeholder="可选，例如：协助客户排查订单问题"
            onChange={(event) => {
              reason = event.target.value;
            }}
          />
        </Flex>
      ),
      onOk: async () => {
        const normalizedReason = reason.trim();
        const hide = message.loading('正在生成免密登录链接');
        try {
          const resp = await directLoginAccount(
            partnerId,
            accountId,
            normalizedReason,
          );
          const bridgeResult = resp?.code === 200
            ? await openPortalDirectLoginWindow(resp.data, config.moduleKey)
            : false;
          if (bridgeResult) {
            message.success(`${config.label}端账号免密登录已确认，有效期 ${resp.data.expireMinutes || 30} 分钟`);
            return;
          }
          message.error(resp?.msg || '免密登录链接生成失败');
          throw new Error('DIRECT_LOGIN_FAILED');
        } catch (error) {
          if (!(error instanceof Error && error.message === 'DIRECT_LOGIN_FAILED')) {
            message.error('免密登录链接生成失败，请重试');
          }
          throw error;
        } finally {
          hide();
        }
      },
    });
  };

  const columns: ColumnsType<AccountRecord> = [
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
      render: (value) => <Tag color={value === 'OWNER' ? 'blue' : 'default'}>{optionLabel(accountRoleOptions, value)}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 76,
      render: (value) => <Tag color={value === '0' ? 'success' : 'default'}>{optionLabel(statusOptions, value)}</Tag>,
    },
    ...(accountLockEnabled ? [{
      title: '锁定',
      dataIndex: 'lockStatus',
      width: 86,
      render: (value: string, record: AccountRecord) => (
        <Tag color={value === '1' ? 'error' : 'success'} title={record.lockReason || undefined}>
          {optionLabel(accountLockStatusOptions, value || '0')}
        </Tag>
      ),
    }] : []),
    {
      title: '时间',
      dataIndex: 'timeInfo',
      width: 148,
      render: (_, record) => (
        <Flex vertical gap={0}>
          <Typography.Text style={compactCellTextStyle}>{formatDateTimeText(record.createTime)}</Typography.Text>
          <Typography.Text style={compactSubTextStyle} type="secondary">
            {formatDateTimeText(record.lastLoginTime)}
          </Typography.Text>
        </Flex>
      ),
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
          access.hasPerms(`${permPrefix}:session:list`) && config.services.listAccountSessions
            ? { key: 'sessions', label: '会话' }
            : null,
          canViewAccountAudit
            ? { key: 'audit', label: '审计' }
            : null,
          access.hasPerms(`${permPrefix}:forceLogout`)
            ? { key: 'forceLogout', label: '强制踢出' }
            : null,
        ].filter(Boolean) as MenuProps['items'];

        return (
          <Space size={4}>
            <Button
              type="link"
              size="small"
              hidden={!access.hasPerms(accountPermissions.edit)}
              onClick={() => openAccountForm(record)}
            >
              编辑
            </Button>
            <Button
              type="link"
              size="small"
              hidden={!canAssignAccountRoles}
              onClick={() => openAccountRoleModal(record)}
            >
              分配角色
            </Button>
            {moreItems && moreItems.length > 0 ? (
              <Dropdown
                menu={{
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
                }}
              >
                <Button type="link" size="small">
                  更多 <DownOutlined />
                </Button>
              </Dropdown>
            ) : null}
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <Modal
        width={1040}
        title={`${config.label}账号 - ${partnerName || '-'}`}
        open={open}
        destroyOnHidden
        footer={null}
        onCancel={() => onOpenChange(false)}
      >
        <Table<AccountRecord>
          rowKey={(record) => String(getAccountId(config, record) || record.userName)}
          loading={loading}
          columns={columns}
          dataSource={accounts}
          size="small"
          pagination={false}
          tableLayout="fixed"
          title={() => (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              hidden={!access.hasPerms(accountPermissions.add)}
              onClick={() => openAccountForm()}
            >
              新增账号
            </Button>
          )}
        />
      </Modal>

      <PartnerSessionModal
        config={config}
        open={sessionModalOpen}
        partner={partner}
        account={sessionAccount}
        onOpenChange={(nextOpen) => {
          setSessionModalOpen(nextOpen);
          if (!nextOpen) {
            setSessionAccount(undefined);
          }
        }}
      />

      <PartnerAuditModal
        config={config}
        open={auditModalOpen}
        partner={partner}
        account={auditAccount}
        onOpenChange={(nextOpen) => {
          setAuditModalOpen(nextOpen);
          if (!nextOpen) {
            setAuditAccount(undefined);
          }
        }}
      />

      <PartnerAccountRoleModal
        config={config}
        partnerId={partnerId}
        account={roleAccount}
        open={roleModalOpen}
        onOpenChange={(nextOpen) => {
          if (nextOpen) {
            setRoleModalOpen(true);
            return;
          }
          closeAccountRoleModal();
        }}
      />

      <Modal
        width={640}
        title={currentAccountId ? '编辑账号' : '新增账号'}
        open={accountFormOpen}
        destroyOnHidden
        forceRender
        confirmLoading={saving}
        onOk={handleAccountSubmit}
        onCancel={closeAccountForm}
      >
        <Form form={accountForm} layout="vertical">
          <Form.Item label="登录账号" name="userName" rules={[{ required: true, message: '请输入登录账号' }]}>
            <Input disabled={Boolean(currentAccountId)} placeholder="请输入" />
          </Form.Item>
          <Form.Item label="姓名" name="nickName" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="请输入" />
          </Form.Item>
          {!currentAccountId ? (
            <Form.Item label="初始密码" name="password" rules={passwordRules}>
              <Input.Password placeholder="请输入" />
            </Form.Item>
          ) : null}
          <Form.Item label="部门" name="deptId">
            <TreeSelect
              {...SEARCHABLE_TREE_SELECT_PROPS}
              allowClear
              disabled={!canQueryDept}
              treeDefaultExpandAll
              placeholder={canQueryDept ? '请选择' : '无部门查询权限'}
              treeData={deptTree}
              fieldNames={{ label: 'label', value: 'id', children: 'children' }}
            />
          </Form.Item>
          <Form.Item label="账号角色" name="accountRole" rules={[{ required: true, message: '请选择账号角色' }]}>
            <Select {...SEARCHABLE_SELECT_PROPS} options={roleSelectOptions} />
          </Form.Item>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
            <Select {...SEARCHABLE_SELECT_PROPS} options={statusOptions} />
          </Form.Item>
          <Form.Item label="手机" name="phonenumber">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="邮箱" name="email">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default PartnerAccountModal;
