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
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';
import PartnerAccountRoleModal from './PartnerAccountRoleModal';

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

const DEFAULT_ACCOUNT_PASSWORD = 'U12346';

const fallbackAccountRoleOptions: SelectOption[] = [
  { label: '负责人', value: 'OWNER', searchText: 'owner 负责人' },
  { label: '管理员', value: 'ADMIN', searchText: 'admin 管理员' },
  { label: '普通账号', value: 'STAFF', searchText: 'staff 普通账号' },
];

const statusOptions: SelectOption[] = [
  { label: '正常', value: '0', searchText: '0 正常' },
  { label: '停用', value: '1', searchText: '1 停用' },
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
    password: currentAccountId ? undefined : values.password || DEFAULT_ACCOUNT_PASSWORD,
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
    password: DEFAULT_ACCOUNT_PASSWORD,
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

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [accounts, setAccounts] = useState<AccountRecord[]>([]);
  const [deptTree, setDeptTree] = useState<API.Partner.PortalTreeNode[]>([]);
  const [accountRoleOptions, setAccountRoleOptions] = useState<SelectOption[]>(fallbackAccountRoleOptions);
  const [accountFormOpen, setAccountFormOpen] = useState(false);
  const [currentAccount, setCurrentAccount] = useState<AccountRecord>();
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [roleAccount, setRoleAccount] = useState<AccountRecord>();

  const partnerId = Number(getValue(partner, config.idField) || 0);
  const partnerName = getValue(partner, config.nameField) || getValue(partner, config.codeField) || '';
  const permPrefix = `${config.moduleKey}:admin`;
  const currentAccountId = getAccountId(config, currentAccount);

  const roleSelectOptions = useMemo(
    () => accountRoleOptions.map((option) => ({
      ...option,
      disabled: option.value === 'OWNER' && currentAccount?.accountRole !== 'OWNER',
    })),
    [accountRoleOptions, currentAccount?.accountRole],
  );

  const loadAccounts = async () => {
    if (!partnerId) {
      setAccounts([]);
      setDeptTree([]);
      return;
    }

    setLoading(true);
    try {
      const [accountResp, deptResp] = await Promise.all([
        config.services.getAccounts(partnerId),
        config.services.getDeptTree(partnerId),
      ]);
      if (accountResp.code === 200) {
        setAccounts((accountResp.data || []) as AccountRecord[]);
      } else {
        message.error(accountResp.msg || '账号列表加载失败');
      }
      if (deptResp.code === 200) {
        setDeptTree(deptResp.data || []);
      } else {
        setDeptTree([]);
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
      getDictSelectOption(`${config.moduleKey}_account_role`)
        .then((data) => setAccountRoleOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackAccountRoleOptions)))
        .catch(() => setAccountRoleOptions(fallbackAccountRoleOptions));
    }
  }, [open, partnerId, config.moduleKey]);

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
    if (!accountId) {
      return;
    }
    modal.confirm({
      title: `确认重置账号 ${account.userName || account.nickName || accountId} 的密码吗？`,
      content: `密码将重置为默认密码 ${DEFAULT_ACCOUNT_PASSWORD}。`,
      onOk: async () => {
        const resp = await config.services.resetAccountDefaultPassword({
          accountId,
          [config.accountIdField]: accountId,
        });
        if (resp.code === 200) {
          message.success(`密码已重置为 ${DEFAULT_ACCOUNT_PASSWORD}`);
          return;
        }
        message.error(resp.msg || '密码重置失败');
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

  const columns: ColumnsType<AccountRecord> = [
    {
      title: '登录账号',
      dataIndex: 'userName',
      width: 150,
      render: renderCompactText,
    },
    {
      title: '姓名',
      dataIndex: 'nickName',
      width: 120,
      render: renderCompactText,
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      width: 140,
      render: renderCompactText,
    },
    {
      title: '角色',
      dataIndex: 'accountRole',
      width: 110,
      render: (value) => <Tag color={value === 'OWNER' ? 'blue' : 'default'}>{optionLabel(accountRoleOptions, value)}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 88,
      render: (value) => <Tag color={value === '0' ? 'success' : 'default'}>{optionLabel(statusOptions, value)}</Tag>,
    },
    {
      title: '时间',
      dataIndex: 'timeInfo',
      width: 176,
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
      width: 190,
      render: (_, record) => {
        const moreItems = [
          access.hasPerms(`${permPrefix}:resetPwd`)
            ? { key: 'resetPwd', label: '重置密码' }
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
              hidden={!access.hasPerms(`${permPrefix}:edit`)}
              onClick={() => openAccountForm(record)}
            >
              编辑
            </Button>
            <Button
              type="link"
              size="small"
              hidden={!access.hasPerms(`${permPrefix}:role:edit`)}
              onClick={() => openAccountRoleModal(record)}
            >
              分配角色
            </Button>
            {moreItems && moreItems.length > 0 ? (
              <Dropdown
                menu={{
                  items: moreItems,
                  onClick: ({ key }) => {
                    if (key === 'resetPwd') {
                      handleResetPassword(record);
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
        width={1000}
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
              hidden={!access.hasPerms(`${permPrefix}:add`)}
              onClick={() => openAccountForm()}
            >
              新增账号
            </Button>
          )}
        />
      </Modal>

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
            <Form.Item label="初始密码" name="password" rules={[{ required: true, message: '请输入初始密码' }]}>
              <Input.Password placeholder="请输入" />
            </Form.Item>
          ) : null}
          <Form.Item label="部门" name="deptId">
            <TreeSelect
              {...SEARCHABLE_TREE_SELECT_PROPS}
              allowClear
              treeDefaultExpandAll
              placeholder="请选择"
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
