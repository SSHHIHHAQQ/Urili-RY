import React, { useEffect, useRef, useState } from 'react';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Dropdown,
  Flex,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
  Upload,
} from 'antd';
import type { MenuProps, UploadFile, UploadProps } from 'antd';
import {
  PageContainer,
  ProTable,
  type ActionType,
  type ProColumns,
} from '@ant-design/pro-components';
import {
  AuditOutlined,
  DownOutlined,
  DollarOutlined,
  MenuOutlined,
  PlusOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { DictValueEnumObj } from '@/components/DictTag';
import { uploadCommonFile } from '@/services/common/file';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import PartnerAccountModal from './PartnerAccountModal';
import PartnerAuditModal from './PartnerAuditModal';
import PartnerDeptModal from './PartnerDeptModal';
import PartnerMenuModal from './PartnerMenuModal';
import PartnerRoleModal from './PartnerRoleModal';
import PartnerSessionModal from './PartnerSessionModal';

type PartnerRecord = Record<string, any>;
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
  getAccounts: (id: number) => Promise<{ code: number; msg?: string; data: API.Partner.PortalAccountBase[] }>;
  addAccount: (id: number, data: API.Partner.PortalAccountBase) => Promise<API.Result>;
  updateAccount: (id: number, data: API.Partner.PortalAccountBase) => Promise<API.Result>;
  listDepts: (id: number) => Promise<API.Partner.PortalDeptListResult>;
  getDeptTree: (id: number) => Promise<API.Partner.PortalDeptTreeResult>;
  addDept: (id: number, data: API.Partner.PortalDept) => Promise<API.Result>;
  updateDept: (id: number, data: API.Partner.PortalDept) => Promise<API.Result>;
  removeDept: (id: number, deptId: number) => Promise<API.Result>;
  getMenuTree: () => Promise<API.Partner.PortalMenuTreeResult>;
  listMenus: (params?: Record<string, any>) => Promise<API.Partner.PortalMenuListResult>;
  getMenu: (menuId: number) => Promise<API.Partner.PortalMenuInfoResult>;
  addMenu: (data: API.Partner.PortalMenu) => Promise<API.Result>;
  updateMenu: (data: API.Partner.PortalMenu) => Promise<API.Result>;
  removeMenu: (menuId: number) => Promise<API.Result>;
  getRoleMenuTree: (id: number, roleId: number) => Promise<API.Partner.PortalRoleMenuTreeResult>;
  listRoles: (id: number, params?: Record<string, any>) => Promise<API.Partner.PortalRolePageResult>;
  getRole: (id: number, roleId: number) => Promise<API.Partner.PortalRoleInfoResult>;
  addRole: (id: number, data: API.Partner.PortalRole) => Promise<API.Result>;
  updateRole: (id: number, data: API.Partner.PortalRole) => Promise<API.Result>;
  changeRoleStatus: (id: number, data: Pick<API.Partner.PortalRole, 'roleId' | 'status'>) => Promise<API.Result>;
  removeRoles: (id: number, roleIds: number[]) => Promise<API.Result>;
  resetAccountDefaultPassword: (data: any) => Promise<API.Result>;
  listSubjectSessions?: (id: number, params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>;
  listAccountSessions?: (id: number, accountId: number, params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>;
  forceLogoutSubject: (id: number) => Promise<API.Result>;
  forceLogoutAccount: (id: number, accountId: number) => Promise<API.Result>;
  getAccountRoles: (id: number, accountId: number) => Promise<API.Partner.PortalAccountRoleResult>;
  assignAccountRoles: (id: number, accountId: number, roleIds: number[]) => Promise<API.Result>;
  resetOwnerPassword: (id: number) => Promise<API.Result>;
  directLogin: (id: number, reason: string) => Promise<API.Partner.DirectLoginApiResult>;
  directLoginAccount?: (id: number, accountId: number, reason: string) => Promise<API.Partner.DirectLoginApiResult>;
  listLoginLogs: (params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<API.Partner.PortalLoginLog>>;
  listOperLogs: (params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<API.Partner.PortalOperLog>>;
  listDirectLoginTickets: (params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<API.Partner.PortalDirectLoginTicket>>;
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
  ownerIdField: string;
  accountIdField: string;
  balanceTitle: string;
  showRechargePlaceholder?: boolean;
  levelDictType: string;
  listTemplate?: 'standard';
  searchStorageKey?: string;
  accountPermissions?: {
    list: string;
    add: string;
    edit: string;
    resetPwd: string;
    roleQuery: string;
    roleEdit: string;
  };
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

const ATTACHMENT_ACCEPT = 'image/*,.pdf,.doc,.docx,.xls,.xlsx,.csv,.txt';
const ATTACHMENT_MAX_SIZE = 10 * 1024 * 1024;
const ATTACHMENT_ALLOWED_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'bmp', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'csv', 'txt']);

function getFileExtension(fileName: string) {
  const index = fileName.lastIndexOf('.');
  return index >= 0 ? fileName.slice(index + 1).toLowerCase() : '';
}

function validateAttachmentCandidate(file: File) {
  if (file.size > ATTACHMENT_MAX_SIZE) {
    return '附件大小不能超过 10MB';
  }
  const extension = getFileExtension(file.name);
  if (!ATTACHMENT_ALLOWED_EXTENSIONS.has(extension)) {
    return '附件类型仅支持图片、PDF、Word、Excel、CSV 或 TXT';
  }
  return '';
}

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

const balanceRangeNumberStyle: React.CSSProperties = {
  width: '50%',
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

function getRangeValue(value: unknown) {
  return Array.isArray(value) ? value : [];
}

function hasSearchValue(value: unknown) {
  return value !== undefined && value !== null && value !== '';
}

function getBalanceRangeInputValue(value: unknown): [number | string | undefined, number | string | undefined] {
  const range = getRangeValue(value);
  return [range[0], range[1]];
}

function BalanceRangeInput({
  value,
  onChange,
  disabled,
}: {
  value?: unknown;
  onChange?: (value?: [number | string | undefined, number | string | undefined]) => void;
  disabled?: boolean;
}) {
  const [minValue, maxValue] = getBalanceRangeInputValue(value);

  const updateValue = (index: 0 | 1, nextValue: number | string | null) => {
    const next: [number | string | undefined, number | string | undefined] = [minValue, maxValue];
    next[index] = nextValue ?? undefined;
    onChange?.(hasSearchValue(next[0]) || hasSearchValue(next[1]) ? next : undefined);
  };

  return (
    <Space.Compact block>
      <InputNumber
        controls={false}
        disabled={disabled}
        min={0}
        precision={2}
        placeholder="最小"
        value={minValue as number | undefined}
        style={balanceRangeNumberStyle}
        onChange={(nextValue) => updateValue(0, nextValue)}
      />
      <InputNumber
        controls={false}
        disabled={disabled}
        min={0}
        precision={2}
        placeholder="最大"
        value={maxValue as number | undefined}
        style={balanceRangeNumberStyle}
        onChange={(nextValue) => updateValue(1, nextValue)}
      />
    </Space.Compact>
  );
}

function buildListParams(params: Record<string, any>, current?: number, pageSize?: number) {
  const { createTimeRange, lastLoginTimeRange, balanceRange, balanceMin, balanceMax, ...rest } = params;
  const createRange = getRangeValue(createTimeRange);
  const lastLoginRange = getRangeValue(lastLoginTimeRange);
  const accountBalanceRange = getRangeValue(balanceRange);
  const resolvedBalanceMin = accountBalanceRange[0] ?? balanceMin;
  const resolvedBalanceMax = accountBalanceRange[1] ?? balanceMax;
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
  if (hasSearchValue(resolvedBalanceMin)) {
    next['params[balanceMin]'] = resolvedBalanceMin;
  }
  if (hasSearchValue(resolvedBalanceMax)) {
    next['params[balanceMax]'] = resolvedBalanceMax;
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
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

function getValue(record: PartnerRecord | undefined, field: string) {
  return record ? record[field] : undefined;
}

function getAttachmentUrl(attachment?: API.Partner.PartyAttachment | null) {
  return attachment?.fileUrl || attachment?.dataUrl || '';
}

function isManagedAttachmentUrl(url?: string) {
  return !!url && url.startsWith('/profile/');
}

function isLegacyDataUrl(url?: string) {
  return !!url && url.toLowerCase().startsWith('data:');
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

async function uploadFileToAttachment(file: AttachmentUploadFile): Promise<API.Partner.PartyAttachment> {
  if (file.response?.fileUrl) {
    const fileUrl = getAttachmentUrl(file.response);
    if (isManagedAttachmentUrl(fileUrl) || isLegacyDataUrl(fileUrl)) {
      return { ...file.response, fileUrl };
    }
    throw new Error('UNSUPPORTED_ATTACHMENT_URL');
  }
  if (!file.originFileObj) {
    throw new Error('ATTACHMENT_FILE_MISSING');
  }

  const uploadResult = await uploadCommonFile(file.originFileObj);
  if (uploadResult.code !== 200 || !uploadResult.fileName) {
    throw new Error(uploadResult.msg || 'ATTACHMENT_UPLOAD_FAILED');
  }

  return {
    fileName: uploadResult.originalFilename || file.originFileObj.name,
    mimeType: file.originFileObj.type || 'application/octet-stream',
    sizeBytes: file.originFileObj.size,
    fileUrl: uploadResult.fileName,
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
  const [directLoginForm] = Form.useForm<{ reason?: string }>();

  const [statusOptions, setStatusOptions] = useState<DictValueEnumObj>({});
  const [subjectTypeOptions, setSubjectTypeOptions] = useState<SelectOption[]>(fallbackSubjectTypeOptions);
  const [levelOptions, setLevelOptions] = useState<SelectOption[]>(fallbackLevelOptions);
  const [countryRegionOptions, setCountryRegionOptions] = useState<SelectOption[]>(fallbackCountryRegionOptions);
  const [partnerModalOpen, setPartnerModalOpen] = useState(false);
  const [accountModalOpen, setAccountModalOpen] = useState(false);
  const [accountPartner, setAccountPartner] = useState<PartnerRecord>();
  const [deptModalOpen, setDeptModalOpen] = useState(false);
  const [deptPartner, setDeptPartner] = useState<PartnerRecord>();
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [rolePartner, setRolePartner] = useState<PartnerRecord>();
  const [menuModalOpen, setMenuModalOpen] = useState(false);
  const [auditModalOpen, setAuditModalOpen] = useState(false);
  const [auditPartner, setAuditPartner] = useState<PartnerRecord>();
  const [sessionModalOpen, setSessionModalOpen] = useState(false);
  const [sessionPartner, setSessionPartner] = useState<PartnerRecord>();
  const [currentPartner, setCurrentPartner] = useState<PartnerRecord>();
  const [attachmentFileList, setAttachmentFileList] = useState<AttachmentUploadFile[]>([]);

  const permPrefix = `${config.moduleKey}:admin`;
  const accountPermissions = config.accountPermissions ?? {
    list: `${permPrefix}:query`,
    add: `${permPrefix}:add`,
    edit: `${permPrefix}:edit`,
    resetPwd: `${permPrefix}:resetPwd`,
    roleQuery: `${permPrefix}:role:query`,
    roleEdit: `${permPrefix}:role:edit`,
  };
  const hasAuditPermission = access.hasPerms(`${permPrefix}:loginLog:list`)
    || access.hasPerms(`${permPrefix}:operLog:list`)
    || access.hasPerms(`${permPrefix}:ticket:list`);
  const statusValueEnum = getStatusOptions(statusOptions);
  const levelValueEnum = optionsToValueEnum(levelOptions);
  const useStandardListTemplate = config.listTemplate === 'standard';

  useEffect(() => {
    getDictValueEnum('sys_normal_disable').then((data) => setStatusOptions(data));
    getDictSelectOption('subject_type')
      .then((data) => setSubjectTypeOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackSubjectTypeOptions)))
      .catch(() => setSubjectTypeOptions(fallbackSubjectTypeOptions));
    getDictSelectOption(config.levelDictType)
      .then((data) => setLevelOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackLevelOptions)))
      .catch(() => setLevelOptions(fallbackLevelOptions));
    getDictSelectOption('country_region')
      .then((data) => setCountryRegionOptions(normalizeDictSelectOptions(data as DictSelectRawOption[], fallbackCountryRegionOptions)))
      .catch(() => setCountryRegionOptions(fallbackCountryRegionOptions));
  }, [config.levelDictType]);

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
      message.error('附件上传失败，请重新选择文件');
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
    directLoginForm.resetFields();
    modal.confirm({
      title: `生成${config.label}端免密登录链接`,
      okText: '生成并打开',
      content: (
        <Form form={directLoginForm} layout="vertical" preserve={false}>
          <Form.Item
            name="reason"
            label="代入原因"
            rules={[
              { required: true, whitespace: true, message: '请输入免密登录原因' },
              { max: 255, message: '代入原因不能超过 255 个字符' },
            ]}
          >
            <Input.TextArea
              rows={3}
              maxLength={255}
              showCount
              placeholder="例如：协助客户排查订单问题"
            />
          </Form.Item>
        </Form>
      ),
      onOk: async () => {
        const values = await directLoginForm.validateFields();
        const hide = message.loading('正在生成免密登录链接');
        try {
          const resp = await config.services.directLogin(partnerId, values.reason?.trim() || '');
          if (resp.code === 200 && resp.data?.loginUrl) {
            window.open(resp.data.loginUrl, '_blank', 'noopener,noreferrer');
            message.success(`免密登录链接已生成，有效期 ${resp.data.expireMinutes || 30} 分钟`);
            return;
          }
          message.error(resp.msg || '免密登录链接生成失败');
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

  const handleForceLogoutSubject = (record: PartnerRecord) => {
    const partnerId = getValue(record, config.idField);
    if (!partnerId) {
      return;
    }
    modal.confirm({
      title: `确认强制踢出${config.label}端在线会话吗？`,
      content: '该主体下当前在线的端账号会话会立即失效。',
      onOk: async () => {
        const resp = await config.services.forceLogoutSubject(partnerId);
        if (resp.code === 200) {
          message.success('在线会话已强制踢出');
          return;
        }
        message.error(resp.msg || '强制踢出失败');
      },
    });
  };

  const handleAttachmentBeforeUpload: UploadProps['beforeUpload'] = (file) => {
    const error = validateAttachmentCandidate(file);
    if (error) {
      message.error(error);
      return Upload.LIST_IGNORE;
    }
    return false;
  };

  const handleAttachmentChange: UploadProps['onChange'] = ({ fileList }) => {
    const nextList = fileList.slice(-1).map((file) => ({ ...file, status: 'done' as const }));
    setAttachmentFileList(nextList as AttachmentUploadFile[]);
  };

  const columns: ProColumns<PartnerRecord>[] = [
    {
      title: useStandardListTemplate ? `${config.label}编号/代码` : `内部${config.label}编号`,
      dataIndex: config.noField,
      valueType: 'text',
      width: useStandardListTemplate ? 136 : 128,
      render: (_, record) => {
        if (!useStandardListTemplate) {
          return renderCompactText(getValue(record, config.noField));
        }
        return (
          <Flex vertical gap={0}>
            <Typography.Text style={compactCellTextStyle} title={String(getValue(record, config.noField) || '-')}>
              {getValue(record, config.noField) || '-'}
            </Typography.Text>
            <Typography.Text style={compactSubTextStyle} type="secondary" title={String(getValue(record, config.codeField) || '-')}>
              {getValue(record, config.codeField) || '-'}
            </Typography.Text>
          </Flex>
        );
      },
    },
    {
      title: `${config.label}代码`,
      dataIndex: config.codeField,
      valueType: 'text',
      hideInTable: useStandardListTemplate,
      width: 110,
      render: (_, record) => renderCompactText(getValue(record, config.codeField)),
    },
    {
      title: `${config.label}名称`,
      dataIndex: config.nameField,
      valueType: 'text',
      width: useStandardListTemplate ? 170 : 180,
      render: (_, record) => {
        if (!useStandardListTemplate) {
          return renderCompactText(getValue(record, config.nameField));
        }
        return (
          <Flex vertical gap={0}>
            <Typography.Text style={compactCellTextStyle} title={String(getValue(record, config.nameField) || '-')}>
              {getValue(record, config.nameField) || '-'}
            </Typography.Text>
            <Typography.Text style={compactSubTextStyle} type="secondary" title={String(getValue(record, config.shortNameField) || '-')}>
              {getValue(record, config.shortNameField) || '-'}
            </Typography.Text>
          </Flex>
        );
      },
    },
    {
      title: `${config.label}简称`,
      dataIndex: config.shortNameField,
      valueType: 'text',
      hideInTable: useStandardListTemplate,
      width: 120,
      render: (_, record) => renderCompactText(getValue(record, config.shortNameField)),
    },
    {
      title: useStandardListTemplate ? '登录账号/等级' : '登录账号',
      dataIndex: 'username',
      valueType: 'text',
      width: useStandardListTemplate ? 128 : 140,
      render: (_, record) => {
        if (!useStandardListTemplate) {
          return renderCompactText(record.username);
        }
        return (
          <Flex vertical gap={0}>
            <Typography.Text style={compactCellTextStyle} title={record.username || '-'}>
              {record.username || '-'}
            </Typography.Text>
            <span>
              <Tag color="blue">{levelValueEnum[getValue(record, config.levelField)]?.label || getValue(record, config.levelField) || '-'}</Tag>
            </span>
          </Flex>
        );
      },
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
      fieldProps: SEARCHABLE_SELECT_PROPS,
      hideInTable: useStandardListTemplate,
      width: 96,
      render: (_, record) => <Tag color="blue">{levelValueEnum[getValue(record, config.levelField)]?.label || getValue(record, config.levelField) || '-'}</Tag>,
    },
    {
      title: config.balanceTitle,
      dataIndex: 'accountBalance',
      search: false,
      width: useStandardListTemplate ? 112 : 140,
      render: (_, record) => (
        <Flex vertical gap={0}>
          <span>{formatBalance(record)}</span>
          <Tag>占位</Tag>
        </Flex>
      ),
    },
    {
      title: config.balanceTitle,
      dataIndex: 'balanceRange',
      colSize: 2,
      valueType: 'digitRange',
      hideInTable: true,
      formItemRender: () => <BalanceRangeInput />,
    },
    ...(config.showRechargePlaceholder
      ? [
          {
            title: '充值',
            dataIndex: 'rechargePlaceholder',
            search: false,
            width: 96,
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
      width: useStandardListTemplate ? 140 : 180,
      render: (_, record) => (
        <Flex vertical gap={0}>
          <Typography.Text style={compactCellTextStyle}>{record.contactName || '-'}</Typography.Text>
          <Typography.Text style={compactSubTextStyle} type="secondary">
            {record.phone || record.contactPhone || record.email || record.contactEmail || '-'}
          </Typography.Text>
        </Flex>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: useStandardListTemplate ? 78 : 96,
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
      width: useStandardListTemplate ? 150 : 170,
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
      title: '创建时间',
      dataIndex: 'createTimeRange',
      colSize: 2,
      valueType: 'dateRange',
      hideInTable: true,
    },
    {
      title: '最后登录时间',
      dataIndex: 'lastLoginTimeRange',
      colSize: 2,
      valueType: 'dateRange',
      hideInTable: true,
    },
    {
      title: '操作',
      dataIndex: 'option',
      valueType: 'option',
      width: useStandardListTemplate ? 136 : 128,
      render: (_, record) => {
        const moreItems: MenuProps['items'] = [];

        if (access.hasPerms(`${permPrefix}:directLogin`)) {
          moreItems.push({
            key: 'directLogin',
            label: `登录${config.label}端`,
          });
        }
        if (access.hasPerms(`${permPrefix}:dept:list`)) {
          moreItems.push({
            key: 'depts',
            label: '部门',
          });
        }
        if (access.hasPerms(`${permPrefix}:role:list`)) {
          moreItems.push({
            key: 'roles',
            label: '角色',
          });
        }
        if (access.hasPerms(`${permPrefix}:resetPwd`)) {
          moreItems.push({
            key: 'resetOwnerPwd',
            label: '重置主账号',
          });
        }
        if (access.hasPerms(`${permPrefix}:forceLogout`)) {
          if (config.services.listSubjectSessions) {
            moreItems.push({
              key: 'sessions',
              label: '会话',
            });
          }
          moreItems.push({
            key: 'forceLogout',
            label: '强制踢出',
          });
        }
        if (hasAuditPermission) {
          moreItems.push({
            key: 'audit',
            label: '审计',
          });
        }

        const operationItems = [
          <Button
            type="link"
            size="small"
            key="edit"
            hidden={!access.hasPerms(`${permPrefix}:edit`)}
            onClick={() => void openPartnerModal(record)}
          >
            编辑
          </Button>,
          <Button
            type="link"
            size="small"
            key="accounts"
            hidden={!access.hasPerms(accountPermissions.list)}
            onClick={() => {
              setAccountPartner(record);
              setAccountModalOpen(true);
            }}
          >
            账号
          </Button>,
          moreItems.length > 0 ? (
            <Dropdown
              key="more"
              trigger={['click']}
              menu={{
                items: moreItems,
                onClick: ({ key }) => {
                  if (key === 'directLogin') {
                    void handleDirectLogin(record);
                  } else if (key === 'depts') {
                    setDeptPartner(record);
                    setDeptModalOpen(true);
                  } else if (key === 'roles') {
                    setRolePartner(record);
                    setRoleModalOpen(true);
                  } else if (key === 'resetOwnerPwd') {
                    handleResetOwnerPassword(record);
                  } else if (key === 'sessions') {
                    setSessionPartner(record);
                    setSessionModalOpen(true);
                  } else if (key === 'forceLogout') {
                    handleForceLogoutSubject(record);
                  } else if (key === 'audit') {
                    setAuditPartner(record);
                    setAuditModalOpen(true);
                  }
                },
              }}
            >
              <a onClick={(event) => event.preventDefault()}>
                更多 <DownOutlined style={{ fontSize: 10 }} />
              </a>
            </Dropdown>
          ) : null,
        ].filter(Boolean);

        if (useStandardListTemplate) {
          return <Space size={2} wrap={false}>{operationItems}</Space>;
        }

        return operationItems;
      },
    },
  ];

  return (
    <PageContainer>
      <ProTable<PartnerRecord>
        actionRef={actionRef}
        rowKey={config.idField}
        headerTitle={config.title}
        search={getPersistedProTableSearch({ labelWidth: 112 }, config.searchStorageKey)}
        columns={columns}
        scroll={getProTableScroll(useStandardListTemplate ? 1420 : 1500)}
        tableLayout="fixed"
        toolBarRender={() => [
          <Button
            key="menus"
            icon={<MenuOutlined />}
            hidden={!access.hasPerms(`${permPrefix}:menu:list`)}
            onClick={() => setMenuModalOpen(true)}
          >
            菜单配置
          </Button>,
          <Button
            key="audit"
            icon={<AuditOutlined />}
            hidden={!hasAuditPermission}
            onClick={() => {
              setAuditPartner(undefined);
              setAuditModalOpen(true);
            }}
          >
            审计
          </Button>,
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

      <PartnerMenuModal
        config={config}
        open={menuModalOpen}
        onOpenChange={setMenuModalOpen}
      />

      <PartnerAuditModal
        config={config}
        open={auditModalOpen}
        partner={auditPartner}
        onOpenChange={(open) => {
          setAuditModalOpen(open);
          if (!open) {
            setAuditPartner(undefined);
          }
        }}
      />

      <PartnerSessionModal
        config={config}
        open={sessionModalOpen}
        partner={sessionPartner}
        onOpenChange={(open) => {
          setSessionModalOpen(open);
          if (!open) {
            setSessionPartner(undefined);
          }
        }}
      />

      <PartnerAccountModal
        config={config}
        open={accountModalOpen}
        partner={accountPartner}
        onOpenChange={(open) => {
          setAccountModalOpen(open);
          if (!open) {
            setAccountPartner(undefined);
          }
        }}
      />

      <PartnerDeptModal
        config={config}
        open={deptModalOpen}
        partner={deptPartner}
        onOpenChange={(open) => {
          setDeptModalOpen(open);
          if (!open) {
            setDeptPartner(undefined);
          }
        }}
      />

      <PartnerRoleModal
        config={config}
        open={roleModalOpen}
        partner={rolePartner}
        onOpenChange={(open) => {
          setRoleModalOpen(open);
          if (!open) {
            setRolePartner(undefined);
          }
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
              <Select {...SEARCHABLE_SELECT_PROPS} options={levelOptions} />
            </Form.Item>
            <Form.Item label="主体类型" name="type" rules={[{ required: true, message: '请选择主体类型' }]}>
              <Select {...SEARCHABLE_SELECT_PROPS} options={subjectTypeOptions} />
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
              <Flex vertical gap={8}>
                <Upload
                  accept={ATTACHMENT_ACCEPT}
                  beforeUpload={handleAttachmentBeforeUpload}
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
              </Flex>
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
                {...SEARCHABLE_SELECT_PROPS}
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

    </PageContainer>
  );
};

export default PartnerManagementPage;
