import React, { useEffect, useRef, useState } from 'react';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Form,
  Image,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
  Upload,
} from 'antd';
import type { UploadFile, UploadProps } from 'antd';
import {
  PageContainer,
  ProForm,
  ProFormRadio,
  ProFormText,
  ProFormTextArea,
  ProTable,
  type ActionType,
  type ProColumns,
} from '@ant-design/pro-components';
import {
  DollarOutlined,
  EditOutlined,
  KeyOutlined,
  LoginOutlined,
  PlusOutlined,
  TeamOutlined,
  UploadOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import type { DictValueEnumObj } from '@/components/DictTag';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';

type PartnerRecord = Record<string, any>;
type PortalAccountRecord = API.Partner.PortalAccountBase & Record<string, any>;
type AttachmentUploadFile = UploadFile<API.Partner.PartyAttachment>;

type SelectOption = {
  label: string;
  value: string;
  key?: React.Key;
  searchText?: string;
};

type DictSelectRawOption = {
  label?: React.ReactNode;
  text?: React.ReactNode;
  value?: string | number;
  key?: React.Key;
};

type PartnerService = {
  list: (params?: any) => Promise<{ code: number; total: number; rows: PartnerRecord[] }>;
  get: (id: number) => Promise<{ code: number; data: PartnerRecord }>;
  add: (data: any) => Promise<API.Result>;
  update: (data: any) => Promise<API.Result>;
  changeStatus: (data: any) => Promise<API.Result>;
  listAccounts: (id: number) => Promise<{ code: number; msg: string; data: PortalAccountRecord[] }>;
  addAccount: (id: number, data: any) => Promise<API.Result>;
  resetPassword: (data: any) => Promise<API.Result>;
  resetDefaultPassword: (data: any) => Promise<API.Result>;
  resetOwnerPassword: (id: number) => Promise<API.Result>;
  directLogin: (id: number) => Promise<API.Partner.DirectLoginApiResult>;
};

export type PartnerModuleConfig = {
  moduleKey: 'seller' | 'buyer';
  title: string;
  label: string;
  idField: string;
  noField: string;
  codeField: string;
  nameField: string;
  shortNameField: string;
  typeField: string;
  levelField: string;
  accountIdField: string;
  ownerIdField: string;
  balanceTitle: string;
  showRechargePlaceholder?: boolean;
  levelDictType: string;
  accountRoleDictType: string;
  services: PartnerService;
};

type PartnerFormValues = {
  username?: string;
  level?: string;
  type?: string;
  code?: string;
  name?: string;
  shortName?: string;
  legalId?: string;
  businessLicenseNo?: string;
  contactName?: string;
  phone?: string;
  email?: string;
  address1?: string;
  address2?: string;
  city?: string;
  state?: string;
  countryCode?: string;
  postalCode?: string;
  remark?: string;
};

const fallbackStatusOptions: DictValueEnumObj = {
  '0': { text: '正常', label: '正常', value: '0', listClass: 'success' },
  '1': { text: '停用', label: '停用', value: '1', listClass: 'danger' },
};

const fallbackAccountRoleOptions: DictValueEnumObj = {
  OWNER: { text: '负责人', label: '负责人', value: 'OWNER' },
  ADMIN: { text: '管理员', label: '管理员', value: 'ADMIN' },
  STAFF: { text: '普通账号', label: '普通账号', value: 'STAFF' },
};

const fallbackLevelOptions: SelectOption[] = [
  { label: '等级1', value: 'L1', searchText: 'l1 等级1' },
  { label: '等级2', value: 'L2', searchText: 'l2 等级2' },
  { label: '等级3', value: 'L3', searchText: 'l3 等级3' },
];

const fallbackSubjectTypeOptions: SelectOption[] = [
  { label: '公司', value: 'COMPANY', searchText: 'company 公司' },
  { label: '个人', value: 'PERSON', searchText: 'person 个人' },
  { label: '其他', value: 'OTHER', searchText: 'other 其他' },
];

const fallbackCountryRegionOptions: SelectOption[] = [
  { label: '中国 / China (CN)', value: 'CN', searchText: 'cn 中国 china' },
  { label: '美国 / United States (US)', value: 'US', searchText: 'us 美国 united states' },
];

const formGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  columnGap: 20,
};

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  whiteSpace: 'normal',
  wordBreak: 'break-word',
  lineHeight: 1.35,
};

const compactSubTextStyle: React.CSSProperties = {
  display: 'block',
  whiteSpace: 'normal',
  wordBreak: 'break-word',
  lineHeight: 1.35,
};

const compactOperationStyle: React.CSSProperties = {
  columnGap: 4,
  rowGap: 0,
  flexWrap: 'wrap',
};

function getStatusOptions(statusOptions: DictValueEnumObj) {
  return Object.keys(statusOptions).length > 0 ? statusOptions : fallbackStatusOptions;
}

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
      key: option.key,
      searchText: `${value} ${labelText}`.toLowerCase(),
    });
  });

  return normalized.length > 0 ? normalized : fallback;
}

function optionsToValueEnum(options: SelectOption[]): DictValueEnumObj {
  return options.reduce<DictValueEnumObj>((acc, option) => {
    acc[option.value] = { text: option.label, label: option.label, value: option.value };
    return acc;
  }, {});
}

function filterSelectOption(
  input: string,
  option?: { label?: React.ReactNode; value?: string | number; searchText?: string },
) {
  const keyword = input.trim().toLowerCase();
  const label = typeof option?.label === 'string' ? option.label : String(option?.label ?? '');
  const value = option?.value == null ? '' : String(option.value);
  const searchText = option?.searchText || `${value} ${label}`;
  return keyword === '' || searchText.toLowerCase().includes(keyword);
}

function getRangeValue(value: unknown) {
  return Array.isArray(value) ? value : [];
}

function buildListParams(params: Record<string, any>, current?: number, pageSize?: number) {
  const { createTimeRange, lastLoginTimeRange, balanceMin, balanceMax, ...rest } = params;
  const createRange = getRangeValue(createTimeRange);
  const lastLoginRange = getRangeValue(lastLoginTimeRange);
  const next: Record<string, any> = {
    ...rest,
    pageNum: current,
    pageSize,
  };

  if (createRange[0]) {
    next['params[createBeginTime]'] = createRange[0];
  }
  if (createRange[1]) {
    next['params[createEndTime]'] = createRange[1];
  }
  if (lastLoginRange[0]) {
    next['params[lastLoginBeginTime]'] = lastLoginRange[0];
  }
  if (lastLoginRange[1]) {
    next['params[lastLoginEndTime]'] = lastLoginRange[1];
  }
  if (balanceMin !== undefined && balanceMin !== '') {
    next['params[balanceMin]'] = balanceMin;
  }
  if (balanceMax !== undefined && balanceMax !== '') {
    next['params[balanceMax]'] = balanceMax;
  }

  return next;
}

function formatBalance(record: PartnerRecord) {
  const currency = record.balanceCurrency || 'USD';
  const value = Number(record.accountBalance ?? 0);
  return `${currency} ${Number.isFinite(value) ? value.toFixed(2) : '0.00'}`;
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
  return <Typography.Text style={compactCellTextStyle}>{text}</Typography.Text>;
}

function getStatusTag(value?: string, statusOptions: DictValueEnumObj = fallbackStatusOptions) {
  const options = getStatusOptions(statusOptions);
  const option = value ? options[value] : undefined;
  if (!option) {
    return <Tag>{value || '-'}</Tag>;
  }
  const color = option.listClass === 'success' ? 'success' : option.listClass === 'danger' ? 'error' : 'default';
  return <Tag color={color}>{option.label}</Tag>;
}

function getValue(record: PartnerRecord | undefined, field: string) {
  return record ? record[field] : undefined;
}

function getAttachmentUrl(attachment?: API.Partner.PartyAttachment | null) {
  return attachment?.fileUrl || attachment?.dataUrl || '';
}

function getAttachmentFromPartner(partner?: PartnerRecord): API.Partner.PartyAttachment | undefined {
  if (partner?.attachment) {
    const attachment = partner.attachment as API.Partner.PartyAttachment;
    return { ...attachment, fileUrl: getAttachmentUrl(attachment) };
  }
  if (!partner?.attachmentFileName || !partner?.attachmentFileUrl) {
    return undefined;
  }
  return {
    fileName: partner.attachmentFileName,
    mimeType: partner.attachmentMimeType || 'application/octet-stream',
    sizeBytes: partner.attachmentSizeBytes || 0,
    fileUrl: partner.attachmentFileUrl,
  };
}

function isImageAttachment(attachment: API.Partner.PartyAttachment) {
  return attachment.mimeType.toLowerCase().startsWith('image/');
}

function attachmentToUploadFile(attachment: API.Partner.PartyAttachment): AttachmentUploadFile {
  const fileUrl = getAttachmentUrl(attachment);
  return {
    uid: `${attachment.fileName}-${attachment.sizeBytes}`,
    name: attachment.fileName,
    status: 'done',
    size: attachment.sizeBytes,
    type: attachment.mimeType,
    url: fileUrl,
    thumbUrl: isImageAttachment(attachment) ? fileUrl : undefined,
    response: { ...attachment, fileUrl },
  };
}

function readFileAsDataUrl(file: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        resolve(reader.result);
        return;
      }
      reject(new Error('INVALID_FILE_RESULT'));
    };
    reader.onerror = () => reject(new Error('FILE_READ_FAILED'));
    reader.readAsDataURL(file);
  });
}

async function uploadFileToAttachment(file: AttachmentUploadFile): Promise<API.Partner.PartyAttachment> {
  if (file.response?.fileUrl) {
    return file.response;
  }
  if (!file.originFileObj) {
    throw new Error('ATTACHMENT_FILE_MISSING');
  }
  const dataUrl = await readFileAsDataUrl(file.originFileObj);
  return {
    fileName: file.originFileObj.name,
    mimeType: file.originFileObj.type || 'application/octet-stream',
    sizeBytes: file.originFileObj.size,
    fileUrl: dataUrl,
  };
}

async function attachmentFileListToAttachment(
  fileList: readonly AttachmentUploadFile[],
): Promise<API.Partner.PartyAttachment | undefined> {
  const [file] = fileList;
  return file ? uploadFileToAttachment(file) : undefined;
}

function mapPartnerToFormValues(config: PartnerModuleConfig, partner?: PartnerRecord): PartnerFormValues {
  return {
    username: partner?.username,
    level: getValue(partner, config.levelField) || 'L1',
    type: getValue(partner, config.typeField) || 'COMPANY',
    code: getValue(partner, config.codeField),
    name: getValue(partner, config.nameField),
    shortName: getValue(partner, config.shortNameField),
    legalId: partner?.legalId,
    businessLicenseNo: partner?.businessLicenseNo,
    contactName: partner?.contactName,
    phone: partner?.phone || partner?.contactPhone,
    email: partner?.email || partner?.contactEmail,
    address1: partner?.address1 || partner?.addressLine1,
    address2: partner?.address2 || partner?.addressLine2,
    city: partner?.city,
    state: partner?.state || partner?.stateProvince,
    countryCode: partner?.countryCode || 'CN',
    postalCode: partner?.postalCode,
    remark: partner?.remark,
  };
}

function buildPartnerPayload(
  config: PartnerModuleConfig,
  currentPartner: PartnerRecord | undefined,
  values: PartnerFormValues,
  attachment: API.Partner.PartyAttachment | undefined,
) {
  return {
    [config.idField]: getValue(currentPartner, config.idField),
    username: values.username,
    [config.levelField]: values.level,
    [config.typeField]: values.type,
    [config.codeField]: values.code,
    [config.nameField]: values.name,
    [config.shortNameField]: values.shortName,
    legalId: values.legalId,
    businessLicenseNo: values.businessLicenseNo,
    contactName: values.contactName,
    phone: values.phone,
    email: values.email,
    address1: values.address1,
    address2: values.address2,
    city: values.city,
    state: values.state,
    countryCode: values.countryCode,
    postalCode: values.postalCode,
    remark: values.remark,
    status: currentPartner?.status || '0',
    attachment: attachment || null,
  };
}

const PartnerManagementPage: React.FC<{ config: PartnerModuleConfig }> = ({ config }) => {
  const { message, modal } = App.useApp();
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const [partnerForm] = Form.useForm<PartnerFormValues>();
  const [accountForm] = Form.useForm<PortalAccountRecord & { confirmPassword?: string }>();
  const accountPassword = Form.useWatch('password', accountForm);

  const [statusOptions, setStatusOptions] = useState<DictValueEnumObj>({});
  const [subjectTypeOptions, setSubjectTypeOptions] = useState<SelectOption[]>(fallbackSubjectTypeOptions);
  const [levelOptions, setLevelOptions] = useState<SelectOption[]>(fallbackLevelOptions);
  const [accountRoleOptions, setAccountRoleOptions] = useState<DictValueEnumObj>(fallbackAccountRoleOptions);
  const [countryRegionOptions, setCountryRegionOptions] = useState<SelectOption[]>(fallbackCountryRegionOptions);
  const [partnerModalOpen, setPartnerModalOpen] = useState(false);
  const [accountListOpen, setAccountListOpen] = useState(false);
  const [accountModalOpen, setAccountModalOpen] = useState(false);
  const [currentPartner, setCurrentPartner] = useState<PartnerRecord>();
  const [accounts, setAccounts] = useState<PortalAccountRecord[]>([]);
  const [accountsLoading, setAccountsLoading] = useState(false);
  const [attachmentFileList, setAttachmentFileList] = useState<AttachmentUploadFile[]>([]);

  const permPrefix = `${config.moduleKey}:admin`;
  const statusValueEnum = getStatusOptions(statusOptions);
  const levelValueEnum = optionsToValueEnum(levelOptions);

  useEffect(() => {
    getDictValueEnum('sys_normal_disable').then((data) => setStatusOptions(data));
    getDictSelectOption('subject_type')
      .then((data) => setSubjectTypeOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackSubjectTypeOptions)))
      .catch(() => setSubjectTypeOptions(fallbackSubjectTypeOptions));
    getDictSelectOption(config.levelDictType)
      .then((data) => setLevelOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackLevelOptions)))
      .catch(() => setLevelOptions(fallbackLevelOptions));
    getDictValueEnum(config.accountRoleDictType)
      .then((data) => setAccountRoleOptions(Object.keys(data).length > 0 ? data : fallbackAccountRoleOptions))
      .catch(() => setAccountRoleOptions(fallbackAccountRoleOptions));
    getDictSelectOption('country_region')
      .then((data) => setCountryRegionOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackCountryRegionOptions)))
      .catch(() => setCountryRegionOptions(fallbackCountryRegionOptions));
  }, [config.accountRoleDictType, config.levelDictType]);

  const openPartnerModal = async (record?: PartnerRecord) => {
    setCurrentPartner(record);
    partnerForm.resetFields();
    setAttachmentFileList([]);

    if (!getValue(record, config.idField)) {
      partnerForm.setFieldsValue(mapPartnerToFormValues(config));
      setPartnerModalOpen(true);
      return;
    }

    setPartnerModalOpen(true);
    try {
      const resp = await config.services.get(getValue(record, config.idField));
      const detail = resp.code === 200 ? resp.data : record;
      setCurrentPartner(detail);
      partnerForm.setFieldsValue(mapPartnerToFormValues(config, detail));
      const attachment = getAttachmentFromPartner(detail);
      setAttachmentFileList(attachment ? [attachmentToUploadFile(attachment)] : []);
    } catch {
      partnerForm.setFieldsValue(mapPartnerToFormValues(config, record));
      message.error(`${config.label}详情加载失败，已使用列表数据`);
    }
  };

  const handlePartnerSubmit = async () => {
    const values = await partnerForm.validateFields();
    let attachment: API.Partner.PartyAttachment | undefined;
    try {
      attachment = await attachmentFileListToAttachment(attachmentFileList);
    } catch {
      message.error('附件读取失败，请重新选择文件');
      return;
    }

    const payload = buildPartnerPayload(config, currentPartner, values, attachment);
    const currentId = getValue(currentPartner, config.idField);
    const hide = message.loading(currentId ? '正在更新' : '正在添加');
    try {
      const resp = currentId ? await config.services.update(payload) : await config.services.add(payload);
      hide();
      if (resp.code === 200) {
        message.success(currentId ? '更新成功' : '添加成功');
        setPartnerModalOpen(false);
        actionRef.current?.reload();
        return;
      }
      message.error(resp.msg || '操作失败');
    } catch {
      hide();
      message.error('操作失败，请重试');
    }
  };

  const handleStatusChange = (record: PartnerRecord) => {
    const nextStatus = record.status === '0' ? '1' : '0';
    const actionText = nextStatus === '0' ? '启用' : '停用';
    modal.confirm({
      title: `确认要${actionText}${getValue(record, config.nameField)}吗？`,
      onOk: async () => {
        const resp = await config.services.changeStatus({
          [config.idField]: getValue(record, config.idField),
          status: nextStatus,
        });
        if (resp.code === 200) {
          message.success('状态已更新');
          actionRef.current?.reload();
        } else {
          message.error(resp.msg || '状态更新失败');
        }
      },
    });
  };

  const loadAccounts = async (partner: PartnerRecord) => {
    const partnerId = getValue(partner, config.idField);
    if (!partnerId) {
      return;
    }
    setCurrentPartner(partner);
    setAccountListOpen(true);
    setAccountsLoading(true);
    try {
      const resp = await config.services.listAccounts(partnerId);
      if (resp.code === 200) {
        setAccounts(resp.data || []);
      } else {
        message.error(resp.msg || '账号加载失败');
      }
    } finally {
      setAccountsLoading(false);
    }
  };

  const openAccountModal = () => {
    accountForm.resetFields();
    accountForm.setFieldsValue({
      accountRole: 'STAFF',
      status: '0',
    });
    setAccountModalOpen(true);
  };

  const handleAccountSubmit = async () => {
    const partnerId = getValue(currentPartner, config.idField);
    if (!partnerId) {
      return;
    }
    const values = await accountForm.validateFields();
    const hide = message.loading('正在创建账号');
    try {
      const resp = await config.services.addAccount(partnerId, {
        ...values,
        status: values.status || '0',
      });
      hide();
      if (resp.code === 200) {
        message.success('账号创建成功');
        setAccountModalOpen(false);
        await loadAccounts(currentPartner as PartnerRecord);
        actionRef.current?.reload();
        return;
      }
      message.error(resp.msg || '账号创建失败');
    } catch {
      hide();
      message.error('账号创建失败，请重试');
    }
  };

  const handleResetAccountDefaultPassword = (record: PortalAccountRecord) => {
    modal.confirm({
      title: `确认重置账号 ${record.userName || '-'} 的密码吗？`,
      content: '密码将重置为默认密码 U12346。',
      onOk: async () => {
        const resp = await config.services.resetDefaultPassword({ userId: record.userId });
        if (resp.code === 200) {
          message.success('密码已重置为默认密码 U12346');
          return;
        }
        message.error(resp.msg || '密码重置失败');
      },
    });
  };

  const handleResetOwnerPassword = (record: PartnerRecord) => {
    const partnerId = getValue(record, config.idField);
    if (!partnerId) {
      return;
    }
    modal.confirm({
      title: `确认重置${config.label}主账号密码吗？`,
      content: '密码将重置为默认密码 U12346。',
      onOk: async () => {
        const resp = await config.services.resetOwnerPassword(partnerId);
        if (resp.code === 200) {
          message.success('主账号密码已重置为默认密码 U12346');
          return;
        }
        message.error(resp.msg || '密码重置失败');
      },
    });
  };

  const handleDirectLogin = async (record: PartnerRecord) => {
    const partnerId = getValue(record, config.idField);
    if (!partnerId) {
      return;
    }
    const hide = message.loading('正在生成免密登录链接');
    try {
      const resp = await config.services.directLogin(partnerId);
      hide();
      if (resp.code === 200 && resp.data?.loginUrl) {
        window.open(resp.data.loginUrl, '_blank', 'noopener,noreferrer');
        message.success(`免密登录链接已生成，有效期 ${resp.data.expireMinutes || 30} 分钟`);
        return;
      }
      message.error(resp.msg || '免密登录链接生成失败');
    } catch {
      hide();
      message.error('免密登录链接生成失败，请重试');
    }
  };

  const checkAccountConfirmPassword = (_rule: unknown, value: string) => {
    if (value === accountPassword) {
      return Promise.resolve();
    }
    return Promise.reject(new Error('两次密码输入不一致'));
  };

  const handleAttachmentChange: UploadProps['onChange'] = ({ fileList }) => {
    const nextList = fileList.slice(-1).map((file) => ({ ...file, status: 'done' as const }));
    setAttachmentFileList(nextList as AttachmentUploadFile[]);
  };

  const columns: ProColumns<PartnerRecord>[] = [
    {
      title: `内部${config.label}编号`,
      dataIndex: config.noField,
      valueType: 'text',
      width: 96,
      render: (_, record) => renderCompactText(getValue(record, config.noField)),
    },
    {
      title: `${config.label}代码`,
      dataIndex: config.codeField,
      valueType: 'text',
      width: 82,
      render: (_, record) => renderCompactText(getValue(record, config.codeField)),
    },
    {
      title: `${config.label}名称`,
      dataIndex: config.nameField,
      valueType: 'text',
      width: 128,
      render: (_, record) => renderCompactText(getValue(record, config.nameField)),
    },
    {
      title: `${config.label}简称`,
      dataIndex: config.shortNameField,
      valueType: 'text',
      width: 82,
      render: (_, record) => renderCompactText(getValue(record, config.shortNameField)),
    },
    {
      title: '登录账号',
      dataIndex: 'username',
      valueType: 'text',
      width: 98,
      render: (_, record) => renderCompactText(record.username),
    },
    {
      title: '公司名称',
      dataIndex: 'companyName',
      valueType: 'text',
      hideInTable: true,
      search: true,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      valueType: 'text',
      hideInTable: true,
      search: true,
    },
    {
      title: `${config.label}等级`,
      dataIndex: config.levelField,
      valueType: 'select',
      valueEnum: levelValueEnum,
      width: 64,
      render: (_, record) => <Tag color="blue">{levelValueEnum[getValue(record, config.levelField)]?.label || getValue(record, config.levelField) || '-'}</Tag>,
    },
    {
      title: config.balanceTitle,
      dataIndex: 'accountBalance',
      search: false,
      width: 92,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span>{formatBalance(record)}</span>
          <Tag>占位</Tag>
        </Space>
      ),
    },
    {
      title: `${config.balanceTitle}最小`,
      dataIndex: 'balanceMin',
      valueType: 'digit',
      hideInTable: true,
      fieldProps: { min: 0, precision: 2 },
    },
    {
      title: `${config.balanceTitle}最大`,
      dataIndex: 'balanceMax',
      valueType: 'digit',
      hideInTable: true,
      fieldProps: { min: 0, precision: 2 },
    },
    ...(config.showRechargePlaceholder
      ? [
          {
            title: '充值',
            dataIndex: 'rechargePlaceholder',
            search: false,
            width: 64,
            render: () => (
              <Tag icon={<DollarOutlined />} color="default">
                待接入
              </Tag>
            ),
          } as ProColumns<PartnerRecord>,
        ]
      : []),
    {
      title: '联系人',
      dataIndex: 'contactName',
      search: false,
      width: 120,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text style={compactCellTextStyle}>{record.contactName || '-'}</Typography.Text>
          <Typography.Text style={compactSubTextStyle} type="secondary">
            {record.phone || record.contactPhone || record.email || record.contactEmail || '-'}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '账号数',
      dataIndex: 'accountCount',
      search: false,
      width: 56,
      align: 'right',
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      width: 76,
      render: (_, record) => (
        <Switch
          checked={record.status === '0'}
          checkedChildren="正常"
          unCheckedChildren="停用"
          disabled={!access.hasPerms(`${permPrefix}:changeStatus`)}
          onClick={() => handleStatusChange(record)}
        />
      ),
    },
    {
      title: '时间',
      dataIndex: 'timeInfo',
      search: false,
      width: 132,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text style={compactCellTextStyle}>{formatDateTimeText(record.createTime)}</Typography.Text>
          <Typography.Text style={compactSubTextStyle} type="secondary">
            {formatDateTimeText(record.lastLoginTime)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
    },
    {
      title: '最后登录时间',
      dataIndex: 'lastLoginTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      width: 152,
      render: (_, record) => (
        <Space size={0} style={compactOperationStyle} wrap>
          <Button
            type="link"
            size="small"
            key="directLogin"
            icon={<LoginOutlined />}
            hidden={!access.hasPerms(`${permPrefix}:directLogin`)}
            onClick={() => void handleDirectLogin(record)}
          >
            登录{config.label}端
          </Button>
          <Button
            type="link"
            size="small"
            key="resetOwnerPwd"
            icon={<KeyOutlined />}
            hidden={!access.hasPerms(`${permPrefix}:resetPwd`)}
            onClick={() => handleResetOwnerPassword(record)}
          >
            重置主账号
          </Button>
          <Button
            type="link"
            size="small"
            key="edit"
            icon={<EditOutlined />}
            hidden={!access.hasPerms(`${permPrefix}:edit`)}
            onClick={() => void openPartnerModal(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            key="accounts"
            icon={<TeamOutlined />}
            hidden={!access.hasPerms(`${permPrefix}:query`)}
            onClick={() => void loadAccounts(record)}
          >
            账号
          </Button>
        </Space>
      ),
    },
  ];

  const accountColumns: ProColumns<PortalAccountRecord>[] = [
    { title: '登录账号', dataIndex: 'userName' },
    { title: '姓名', dataIndex: 'nickName' },
    {
      title: '账号角色',
      dataIndex: 'accountRole',
      renderText: (value) => accountRoleOptions[value]?.label || value || '-',
    },
    { title: '手机', dataIndex: 'phonenumber', renderText: (value) => value || '-' },
    { title: '邮箱', dataIndex: 'email', ellipsis: true, renderText: (value) => value || '-' },
    { title: '绑定状态', dataIndex: 'status', render: (_, record) => getStatusTag(record.status, statusOptions) },
    { title: '用户状态', dataIndex: 'userStatus', render: (_, record) => getStatusTag(record.userStatus, statusOptions) },
    { title: '创建时间', dataIndex: 'createTime', valueType: 'dateTime' },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      render: (_, record) => [
        <Button
          type="link"
          size="small"
          key="resetPwd"
          icon={<KeyOutlined />}
          hidden={!access.hasPerms(`${permPrefix}:resetPwd`)}
          onClick={() => handleResetAccountDefaultPassword(record)}
        >
          重置默认密码
        </Button>,
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<PartnerRecord>
        actionRef={actionRef}
        rowKey={config.idField}
        headerTitle={config.title}
        search={getPersistedProTableSearch({ labelWidth: 112 })}
        columns={columns}
        tableLayout="auto"
        toolBarRender={() => [
          <Button
            type="primary"
            key="add"
            icon={<PlusOutlined />}
            hidden={!access.hasPerms(`${permPrefix}:add`)}
            onClick={() => void openPartnerModal()}
          >
            新增{config.label}账户
          </Button>,
        ]}
        request={(params) => {
          const { current, pageSize, ...rest } = params;
          return config.services
            .list(buildListParams(rest, current, pageSize))
            .then((res) => ({
              data: res.rows || [],
              total: res.total || 0,
              success: res.code === 200,
            }));
        }}
      />

      <Modal
        width={920}
        title={getValue(currentPartner, config.idField) ? `编辑${config.label}账户` : `新增${config.label}账户`}
        open={partnerModalOpen}
        destroyOnHidden
        onOk={handlePartnerSubmit}
        onCancel={() => setPartnerModalOpen(false)}
      >
        <Form form={partnerForm} layout="vertical">
          <div style={formGridStyle}>
            <Form.Item label="登录账号" name="username" rules={[{ required: true, message: '请输入登录账号' }]}>
              <Input disabled={Boolean(getValue(currentPartner, config.idField) && currentPartner?.username)} placeholder="请输入" />
            </Form.Item>
            <Form.Item label={`${config.label}等级`} name="level" rules={[{ required: true, message: `请选择${config.label}等级` }]}>
              <Select options={levelOptions} />
            </Form.Item>
            <Form.Item label="主体类型" name="type" rules={[{ required: true, message: '请选择主体类型' }]}>
              <Select options={subjectTypeOptions} />
            </Form.Item>

            <Form.Item label={`${config.label}全称`} name="name" rules={[{ required: true, message: `请输入${config.label}全称` }]}>
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label={`${config.label}代码`} name="code" rules={[{ required: true, message: `请输入${config.label}代码` }]}>
              <Input disabled={Boolean(getValue(currentPartner, config.idField))} placeholder="请输入" />
            </Form.Item>
            <Form.Item label={`${config.label}简称`} name="shortName" rules={[{ required: true, message: `请输入${config.label}简称` }]}>
              <Input placeholder="请输入" />
            </Form.Item>

            <Form.Item label="法人证件号" name="legalId">
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="营业执照号码" name="businessLicenseNo">
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="附件">
              <Space direction="vertical" size={8}>
                <Upload
                  accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.csv,.txt"
                  beforeUpload={() => false}
                  fileList={attachmentFileList}
                  maxCount={1}
                  onChange={handleAttachmentChange}
                  onRemove={() => {
                    setAttachmentFileList([]);
                    return true;
                  }}
                >
                  <Button icon={<UploadOutlined />}>{attachmentFileList.length > 0 ? '更换文件' : '上传文件'}</Button>
                </Upload>
                {attachmentFileList[0]?.response ? (
                  isImageAttachment(attachmentFileList[0].response) ? (
                    <Image
                      alt={attachmentFileList[0].response.fileName}
                      height={54}
                      src={getAttachmentUrl(attachmentFileList[0].response)}
                      style={{ objectFit: 'cover' }}
                      width={72}
                    />
                  ) : (
                    <Typography.Link download={attachmentFileList[0].response.fileName} href={getAttachmentUrl(attachmentFileList[0].response)}>
                      {attachmentFileList[0].response.fileName}
                    </Typography.Link>
                  )
                ) : null}
              </Space>
            </Form.Item>

            <Form.Item label="联系人" name="contactName" rules={[{ required: true, message: '请输入联系人' }]}>
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="手机号" name="phone" rules={[{ required: true, message: '请输入手机号' }]}>
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="邮箱" name="email">
              <Input placeholder="请输入" />
            </Form.Item>

            <Form.Item label="地址1" name="address1" rules={[{ required: true, message: '请输入地址1' }]}>
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="地址2" name="address2">
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="城市" name="city" rules={[{ required: true, message: '请输入城市' }]}>
              <Input placeholder="请输入" />
            </Form.Item>

            <Form.Item label="省/州" name="state">
              <Input placeholder="请输入" />
            </Form.Item>
            <Form.Item label="国家/地区" name="countryCode" rules={[{ required: true, message: '请选择国家/地区' }]}>
              <Select
                showSearch
                filterOption={filterSelectOption}
                optionFilterProp="label"
                options={countryRegionOptions}
                placeholder="请选择国家/地区"
              />
            </Form.Item>
            <Form.Item label="邮编" name="postalCode" rules={[{ required: true, message: '请输入邮编' }]}>
              <Input placeholder="请输入" />
            </Form.Item>

            <Form.Item label="备注" name="remark" style={{ gridColumn: 'span 3' }}>
              <Input placeholder="请输入" />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        width={980}
        title={`${getValue(currentPartner, config.nameField) || config.label} - 账号`}
        open={accountListOpen}
        destroyOnHidden
        footer={null}
        onCancel={() => setAccountListOpen(false)}
      >
        <ProTable<PortalAccountRecord>
          rowKey={(record) => String(getValue(record, config.accountIdField) || record.userId)}
          columns={accountColumns}
          dataSource={accounts}
          loading={accountsLoading}
          search={false}
          pagination={false}
          options={false}
          toolBarRender={() => [
            <Button
              type="primary"
              key="addAccount"
              icon={<UserAddOutlined />}
              hidden={!access.hasPerms(`${permPrefix}:add`)}
              onClick={openAccountModal}
            >
              新增账号
            </Button>,
          ]}
        />
      </Modal>

      <Modal
        width={640}
        title={`新增${config.label}端账号`}
        open={accountModalOpen}
        destroyOnHidden
        onOk={handleAccountSubmit}
        onCancel={() => setAccountModalOpen(false)}
      >
        <ProForm form={accountForm} grid submitter={false} layout="horizontal">
          <ProFormText
            name="userName"
            label="登录账号"
            colProps={{ span: 12 }}
            rules={[{ required: true, message: '登录账号不能为空' }]}
          />
          <ProFormText
            name="nickName"
            label="姓名"
            colProps={{ span: 12 }}
            rules={[{ required: true, message: '姓名不能为空' }]}
          />
          <ProFormText.Password
            name="password"
            label="初始密码"
            colProps={{ span: 12 }}
            rules={[{ required: true, message: '初始密码不能为空' }]}
          />
          <ProFormText.Password
            name="confirmPassword"
            label="确认密码"
            colProps={{ span: 12 }}
            rules={[{ required: true, message: '确认密码不能为空' }, { validator: checkAccountConfirmPassword }]}
          />
          <ProFormRadio.Group
            name="accountRole"
            label="账号角色"
            valueEnum={accountRoleOptions}
            colProps={{ span: 12 }}
            rules={[{ required: true, message: '账号角色不能为空' }]}
          />
          <ProFormRadio.Group
            name="status"
            label="状态"
            valueEnum={statusValueEnum}
            colProps={{ span: 12 }}
            rules={[{ required: true, message: '状态不能为空' }]}
          />
          <ProFormText name="phonenumber" label="手机" colProps={{ span: 12 }} fieldProps={{ maxLength: 11 }} />
          <ProFormText name="email" label="邮箱" colProps={{ span: 12 }} />
          <ProFormTextArea name="remark" label="备注" colProps={{ span: 24 }} />
        </ProForm>
      </Modal>

    </PageContainer>
  );
};

export default PartnerManagementPage;
