import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useRef, useState } from 'react';
import { useAccess } from '@umijs/max';
import { App, Button, Dropdown, Flex, Form, Image, Input, InputNumber, Modal, Select, Space, Switch, Tag, Typography, Upload, } from 'antd';
import { PageContainer, ProTable, } from '@ant-design/pro-components';
import { AuditOutlined, DownOutlined, DollarOutlined, MenuOutlined, PlusOutlined, UploadOutlined, } from '@ant-design/icons';
import { uploadCommonFile } from '@/services/common/file';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { openPortalDirectLoginWindow } from '@/utils/portalDirectLoginMessage';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import PartnerAccountModal from './PartnerAccountModal';
import PartnerAuditModal from './PartnerAuditModal';
import PartnerDeptModal from './PartnerDeptModal';
import PartnerMenuModal from './PartnerMenuModal';
import PartnerRoleModal from './PartnerRoleModal';
import PartnerSessionModal from './PartnerSessionModal';
const fallbackStatusOptions = {
    '0': { text: '正常', label: '正常', value: '0', listClass: 'success' },
    '1': { text: '停用', label: '停用', value: '1', listClass: 'danger' },
};
const ATTACHMENT_ACCEPT = 'image/*,.pdf,.doc,.docx,.xls,.xlsx,.csv,.txt';
const ATTACHMENT_MAX_SIZE = 10 * 1024 * 1024;
const ATTACHMENT_ALLOWED_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'bmp', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'csv', 'txt']);
function getFileExtension(fileName) {
    const index = fileName.lastIndexOf('.');
    return index >= 0 ? fileName.slice(index + 1).toLowerCase() : '';
}
function validateAttachmentCandidate(file) {
    if (file.size > ATTACHMENT_MAX_SIZE) {
        return '附件大小不能超过 10MB';
    }
    const extension = getFileExtension(file.name);
    if (!ATTACHMENT_ALLOWED_EXTENSIONS.has(extension)) {
        return '附件类型仅支持图片、PDF、Word、Excel、CSV 或 TXT';
    }
    return '';
}
const fallbackLevelOptions = [
    { label: '等级1', value: 'L1', searchText: 'l1 等级1' },
    { label: '等级2', value: 'L2', searchText: 'l2 等级2' },
    { label: '等级3', value: 'L3', searchText: 'l3 等级3' },
];
const fallbackSubjectTypeOptions = [
    { label: '公司', value: 'COMPANY', searchText: 'company 公司' },
    { label: '个人', value: 'PERSON', searchText: 'person 个人' },
    { label: '其他', value: 'OTHER', searchText: 'other 其他' },
];
const fallbackCountryRegionOptions = [
    { label: '中国 / China (CN)', value: 'CN', searchText: 'cn 中国 china' },
    { label: '美国 / United States (US)', value: 'US', searchText: 'us 美国 united states' },
];
const formGridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
    columnGap: 20,
};
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
const balanceRangeNumberStyle = {
    width: '50%',
};
function getStatusOptions(statusOptions) {
    return Object.keys(statusOptions).length > 0 ? statusOptions : fallbackStatusOptions;
}
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
            key: option.key,
            searchText: `${value} ${labelText}`.toLowerCase(),
        });
    });
    return normalized.length > 0 ? normalized : fallback;
}
function optionsToValueEnum(options) {
    return options.reduce((acc, option) => {
        acc[option.value] = { text: option.label, label: option.label, value: option.value };
        return acc;
    }, {});
}
function getRangeValue(value) {
    return Array.isArray(value) ? value : [];
}
function hasSearchValue(value) {
    return value !== undefined && value !== null && value !== '';
}
function getBalanceRangeInputValue(value) {
    const range = getRangeValue(value);
    return [range[0], range[1]];
}
function BalanceRangeInput({ value, onChange, disabled, }) {
    const [minValue, maxValue] = getBalanceRangeInputValue(value);
    const updateValue = (index, nextValue) => {
        const next = [minValue, maxValue];
        next[index] = nextValue ?? undefined;
        onChange?.(hasSearchValue(next[0]) || hasSearchValue(next[1]) ? next : undefined);
    };
    return (_jsxs(Space.Compact, { block: true, children: [_jsx(InputNumber, { controls: false, disabled: disabled, min: 0, precision: 2, placeholder: "\u6700\u5C0F", value: minValue, style: balanceRangeNumberStyle, onChange: (nextValue) => updateValue(0, nextValue) }), _jsx(InputNumber, { controls: false, disabled: disabled, min: 0, precision: 2, placeholder: "\u6700\u5927", value: maxValue, style: balanceRangeNumberStyle, onChange: (nextValue) => updateValue(1, nextValue) })] }));
}
function buildListParams(params, current, pageSize) {
    const { createTimeRange, lastLoginTimeRange, balanceRange, balanceMin, balanceMax, ...rest } = params;
    const createRange = getRangeValue(createTimeRange);
    const lastLoginRange = getRangeValue(lastLoginTimeRange);
    const accountBalanceRange = getRangeValue(balanceRange);
    const resolvedBalanceMin = accountBalanceRange[0] ?? balanceMin;
    const resolvedBalanceMax = accountBalanceRange[1] ?? balanceMax;
    const next = {
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
function formatBalance(record) {
    const currency = record.balanceCurrency || 'USD';
    const value = Number(record.accountBalance ?? 0);
    return `${currency} ${Number.isFinite(value) ? value.toFixed(2) : '0.00'}`;
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
function getValue(record, field) {
    return record ? record[field] : undefined;
}
function getAttachmentUrl(attachment) {
    return attachment?.fileUrl || attachment?.dataUrl || '';
}
function isManagedAttachmentUrl(url) {
    return !!url && url.startsWith('/profile/');
}
function isLegacyDataUrl(url) {
    return !!url && url.toLowerCase().startsWith('data:');
}
function getAttachmentFromPartner(partner) {
    if (partner?.attachment) {
        const attachment = partner.attachment;
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
function isImageAttachment(attachment) {
    return attachment.mimeType.toLowerCase().startsWith('image/');
}
function attachmentToUploadFile(attachment) {
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
async function uploadFileToAttachment(file) {
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
async function attachmentFileListToAttachment(fileList) {
    const [file] = fileList;
    return file ? uploadFileToAttachment(file) : undefined;
}
function mapPartnerToFormValues(config, partner) {
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
function buildPartnerPayload(config, currentPartner, values, attachment) {
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
const PartnerManagementPage = ({ config }) => {
    const { message, modal } = App.useApp();
    const access = useAccess();
    const actionRef = useRef(null);
    const [partnerForm] = Form.useForm();
    const [directLoginForm] = Form.useForm();
    const [statusOptions, setStatusOptions] = useState({});
    const [subjectTypeOptions, setSubjectTypeOptions] = useState(fallbackSubjectTypeOptions);
    const [levelOptions, setLevelOptions] = useState(fallbackLevelOptions);
    const [countryRegionOptions, setCountryRegionOptions] = useState(fallbackCountryRegionOptions);
    const [partnerModalOpen, setPartnerModalOpen] = useState(false);
    const [accountModalOpen, setAccountModalOpen] = useState(false);
    const [accountPartner, setAccountPartner] = useState();
    const [deptModalOpen, setDeptModalOpen] = useState(false);
    const [deptPartner, setDeptPartner] = useState();
    const [roleModalOpen, setRoleModalOpen] = useState(false);
    const [rolePartner, setRolePartner] = useState();
    const [menuModalOpen, setMenuModalOpen] = useState(false);
    const [auditModalOpen, setAuditModalOpen] = useState(false);
    const [auditPartner, setAuditPartner] = useState();
    const [sessionModalOpen, setSessionModalOpen] = useState(false);
    const [sessionPartner, setSessionPartner] = useState();
    const [currentPartner, setCurrentPartner] = useState();
    const [attachmentFileList, setAttachmentFileList] = useState([]);
    const permPrefix = `${config.moduleKey}:admin`;
    const accountPermissions = config.accountPermissions ?? {
        list: `${permPrefix}:account:list`,
        add: `${permPrefix}:account:add`,
        edit: `${permPrefix}:account:edit`,
        resetPwd: `${permPrefix}:account:resetPwd`,
        roleQuery: `${permPrefix}:account:role:query`,
        roleEdit: `${permPrefix}:account:role:edit`,
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
            .then((data) => setSubjectTypeOptions(normalizeDictSelectOptions(data, fallbackSubjectTypeOptions)))
            .catch(() => setSubjectTypeOptions(fallbackSubjectTypeOptions));
        getDictSelectOption(config.levelDictType)
            .then((data) => setLevelOptions(normalizeDictSelectOptions(data, fallbackLevelOptions)))
            .catch(() => setLevelOptions(fallbackLevelOptions));
        getDictSelectOption('country_region')
            .then((data) => setCountryRegionOptions(normalizeDictSelectOptions(data, fallbackCountryRegionOptions)))
            .catch(() => setCountryRegionOptions(fallbackCountryRegionOptions));
    }, [config.levelDictType]);
    const openPartnerModal = async (record) => {
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
        }
        catch {
            partnerForm.setFieldsValue(mapPartnerToFormValues(config, record));
            message.error(`${config.label}详情加载失败，已使用列表数据`);
        }
    };
    const handlePartnerSubmit = async () => {
        const values = await partnerForm.validateFields();
        let attachment;
        try {
            attachment = await attachmentFileListToAttachment(attachmentFileList);
        }
        catch {
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
        }
        catch {
            hide();
            message.error('操作失败，请重试');
        }
    };
    const handleStatusChange = (record) => {
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
                }
                else {
                    message.error(resp.msg || '状态更新失败');
                }
            },
        });
    };
    const handleResetOwnerPassword = (record) => {
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
    const handleDirectLogin = async (record) => {
        const partnerId = getValue(record, config.idField);
        if (!partnerId) {
            return;
        }
        directLoginForm.resetFields();
        modal.confirm({
            title: `生成${config.label}端免密登录链接`,
            okText: '生成并打开',
            content: (_jsx(Form, { form: directLoginForm, layout: "vertical", preserve: false, children: _jsx(Form.Item, { name: "reason", label: "\u4EE3\u5165\u539F\u56E0", rules: [
                        { required: true, whitespace: true, message: '请输入免密登录原因' },
                        { max: 255, message: '代入原因不能超过 255 个字符' },
                    ], children: _jsx(Input.TextArea, { rows: 3, maxLength: 255, showCount: true, placeholder: "\u4F8B\u5982\uFF1A\u534F\u52A9\u5BA2\u6237\u6392\u67E5\u8BA2\u5355\u95EE\u9898" }) }) })),
            onOk: async () => {
                const values = await directLoginForm.validateFields();
                const hide = message.loading('正在生成免密登录链接');
                try {
                    const resp = await config.services.directLogin(partnerId, values.reason?.trim() || '');
                    if (resp.code === 200 && openPortalDirectLoginWindow(resp.data, config.moduleKey)) {
                        message.success(`免密登录链接已生成，有效期 ${resp.data.expireMinutes || 30} 分钟`);
                        return;
                    }
                    message.error(resp.msg || '免密登录链接生成失败');
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
    const handleForceLogoutSubject = (record) => {
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
    const handleAttachmentBeforeUpload = (file) => {
        const error = validateAttachmentCandidate(file);
        if (error) {
            message.error(error);
            return Upload.LIST_IGNORE;
        }
        return false;
    };
    const handleAttachmentChange = ({ fileList }) => {
        const nextList = fileList.slice(-1).map((file) => ({ ...file, status: 'done' }));
        setAttachmentFileList(nextList);
    };
    const columns = [
        {
            title: useStandardListTemplate ? `${config.label}编号/代码` : `内部${config.label}编号`,
            dataIndex: config.noField,
            valueType: 'text',
            width: useStandardListTemplate ? 136 : 128,
            render: (_, record) => {
                if (!useStandardListTemplate) {
                    return renderCompactText(getValue(record, config.noField));
                }
                return (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { style: compactCellTextStyle, title: String(getValue(record, config.noField) || '-'), children: getValue(record, config.noField) || '-' }), _jsx(Typography.Text, { style: compactSubTextStyle, type: "secondary", title: String(getValue(record, config.codeField) || '-'), children: getValue(record, config.codeField) || '-' })] }));
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
                return (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { style: compactCellTextStyle, title: String(getValue(record, config.nameField) || '-'), children: getValue(record, config.nameField) || '-' }), _jsx(Typography.Text, { style: compactSubTextStyle, type: "secondary", title: String(getValue(record, config.shortNameField) || '-'), children: getValue(record, config.shortNameField) || '-' })] }));
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
                return (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { style: compactCellTextStyle, title: record.username || '-', children: record.username || '-' }), _jsx("span", { children: _jsx(Tag, { color: "blue", children: levelValueEnum[getValue(record, config.levelField)]?.label || getValue(record, config.levelField) || '-' }) })] }));
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
            render: (_, record) => _jsx(Tag, { color: "blue", children: levelValueEnum[getValue(record, config.levelField)]?.label || getValue(record, config.levelField) || '-' }),
        },
        {
            title: config.balanceTitle,
            dataIndex: 'accountBalance',
            search: false,
            width: useStandardListTemplate ? 112 : 140,
            render: (_, record) => (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx("span", { children: formatBalance(record) }), _jsx(Tag, { children: "\u5360\u4F4D" })] })),
        },
        {
            title: config.balanceTitle,
            dataIndex: 'balanceRange',
            colSize: 2,
            valueType: 'digitRange',
            hideInTable: true,
            formItemRender: (_, config) => (_jsx(BalanceRangeInput, { value: config.value, onChange: config.onChange })),
        },
        ...(config.showRechargePlaceholder
            ? [
                {
                    title: '充值',
                    dataIndex: 'rechargePlaceholder',
                    search: false,
                    width: 96,
                    render: () => (_jsx(Tag, { icon: _jsx(DollarOutlined, {}), color: "default", children: "\u5F85\u63A5\u5165" })),
                },
            ]
            : []),
        {
            title: '联系人',
            dataIndex: 'contactName',
            search: false,
            width: useStandardListTemplate ? 140 : 180,
            render: (_, record) => (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { style: compactCellTextStyle, children: record.contactName || '-' }), _jsx(Typography.Text, { style: compactSubTextStyle, type: "secondary", children: record.phone || record.contactPhone || record.email || record.contactEmail || '-' })] })),
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: useStandardListTemplate ? 78 : 96,
            render: (_, record) => (_jsx(Switch, { checked: record.status === '0', checkedChildren: "\u6B63\u5E38", unCheckedChildren: "\u505C\u7528", disabled: !access.hasPerms(`${permPrefix}:changeStatus`), onClick: () => handleStatusChange(record) })),
        },
        {
            title: '时间',
            dataIndex: 'timeInfo',
            search: false,
            width: useStandardListTemplate ? 150 : 170,
            render: (_, record) => (_jsxs(Flex, { vertical: true, gap: 0, children: [_jsx(Typography.Text, { style: compactCellTextStyle, children: formatDateTimeText(record.createTime) }), _jsx(Typography.Text, { style: compactSubTextStyle, type: "secondary", children: formatDateTimeText(record.lastLoginTime) })] })),
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
                const moreItems = [];
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
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms(`${permPrefix}:edit`), onClick: () => void openPartnerModal(record), children: "\u7F16\u8F91" }, "edit"),
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms(accountPermissions.list), onClick: () => {
                            setAccountPartner(record);
                            setAccountModalOpen(true);
                        }, children: "\u8D26\u53F7" }, "accounts"),
                    moreItems.length > 0 ? (_jsx(Dropdown, { trigger: ['click'], menu: {
                            items: moreItems,
                            onClick: ({ key }) => {
                                if (key === 'directLogin') {
                                    void handleDirectLogin(record);
                                }
                                else if (key === 'depts') {
                                    setDeptPartner(record);
                                    setDeptModalOpen(true);
                                }
                                else if (key === 'roles') {
                                    setRolePartner(record);
                                    setRoleModalOpen(true);
                                }
                                else if (key === 'resetOwnerPwd') {
                                    handleResetOwnerPassword(record);
                                }
                                else if (key === 'sessions') {
                                    setSessionPartner(record);
                                    setSessionModalOpen(true);
                                }
                                else if (key === 'forceLogout') {
                                    handleForceLogoutSubject(record);
                                }
                                else if (key === 'audit') {
                                    setAuditPartner(record);
                                    setAuditModalOpen(true);
                                }
                            },
                        }, children: _jsxs("a", { onClick: (event) => event.preventDefault(), children: ["\u66F4\u591A ", _jsx(DownOutlined, { style: { fontSize: 10 } })] }) }, "more")) : null,
                ].filter(Boolean);
                if (useStandardListTemplate) {
                    return _jsx(Space, { size: 2, wrap: false, children: operationItems });
                }
                return operationItems;
            },
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx(ProTable, { actionRef: actionRef, rowKey: config.idField, headerTitle: config.title, search: getPersistedProTableSearch({ labelWidth: 112, fieldCount: config.searchFieldCount }, config.searchStorageKey), columns: columns, scroll: getProTableScroll(useStandardListTemplate ? 1420 : 1500), tableLayout: "fixed", toolBarRender: () => [
                    _jsx(Button, { icon: _jsx(MenuOutlined, {}), hidden: !access.hasPerms(`${permPrefix}:menu:list`), onClick: () => setMenuModalOpen(true), children: "\u83DC\u5355\u914D\u7F6E" }, "menus"),
                    _jsx(Button, { icon: _jsx(AuditOutlined, {}), hidden: !hasAuditPermission, onClick: () => {
                            setAuditPartner(undefined);
                            setAuditModalOpen(true);
                        }, children: "\u5BA1\u8BA1" }, "audit"),
                    _jsxs(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms(`${permPrefix}:add`), onClick: () => void openPartnerModal(), children: ["\u65B0\u589E", config.label, "\u8D26\u6237"] }, "add"),
                ], request: (params) => {
                    const { current, pageSize, ...rest } = params;
                    return config.services
                        .list(buildListParams(rest, current, pageSize))
                        .then((res) => ({
                        data: res.rows || [],
                        total: res.total || 0,
                        success: res.code === 200,
                    }));
                } }), _jsx(PartnerMenuModal, { config: config, open: menuModalOpen, onOpenChange: setMenuModalOpen }), _jsx(PartnerAuditModal, { config: config, open: auditModalOpen, partner: auditPartner, onOpenChange: (open) => {
                    setAuditModalOpen(open);
                    if (!open) {
                        setAuditPartner(undefined);
                    }
                } }), _jsx(PartnerSessionModal, { config: config, open: sessionModalOpen, partner: sessionPartner, onOpenChange: (open) => {
                    setSessionModalOpen(open);
                    if (!open) {
                        setSessionPartner(undefined);
                    }
                } }), _jsx(PartnerAccountModal, { config: config, open: accountModalOpen, partner: accountPartner, onOpenChange: (open) => {
                    setAccountModalOpen(open);
                    if (!open) {
                        setAccountPartner(undefined);
                    }
                } }), _jsx(PartnerDeptModal, { config: config, open: deptModalOpen, partner: deptPartner, onOpenChange: (open) => {
                    setDeptModalOpen(open);
                    if (!open) {
                        setDeptPartner(undefined);
                    }
                } }), _jsx(PartnerRoleModal, { config: config, open: roleModalOpen, partner: rolePartner, onOpenChange: (open) => {
                    setRoleModalOpen(open);
                    if (!open) {
                        setRolePartner(undefined);
                    }
                } }), _jsx(Modal, { width: 920, title: getValue(currentPartner, config.idField) ? `编辑${config.label}账户` : `新增${config.label}账户`, open: partnerModalOpen, destroyOnHidden: true, onOk: handlePartnerSubmit, onCancel: () => setPartnerModalOpen(false), children: _jsx(Form, { form: partnerForm, layout: "vertical", children: _jsxs("div", { style: formGridStyle, children: [_jsx(Form.Item, { label: "\u767B\u5F55\u8D26\u53F7", name: "username", rules: [{ required: true, message: '请输入登录账号' }], children: _jsx(Input, { disabled: Boolean(getValue(currentPartner, config.idField) && currentPartner?.username), placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: `${config.label}等级`, name: "level", rules: [{ required: true, message: `请选择${config.label}等级` }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: levelOptions }) }), _jsx(Form.Item, { label: "\u4E3B\u4F53\u7C7B\u578B", name: "type", rules: [{ required: true, message: '请选择主体类型' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: subjectTypeOptions }) }), _jsx(Form.Item, { label: `${config.label}全称`, name: "name", rules: [{ required: true, message: `请输入${config.label}全称` }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: `${config.label}代码`, name: "code", rules: [{ required: true, message: `请输入${config.label}代码` }], children: _jsx(Input, { disabled: Boolean(getValue(currentPartner, config.idField)), placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: `${config.label}简称`, name: "shortName", rules: [{ required: true, message: `请输入${config.label}简称` }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u6CD5\u4EBA\u8BC1\u4EF6\u53F7", name: "legalId", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u8425\u4E1A\u6267\u7167\u53F7\u7801", name: "businessLicenseNo", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u9644\u4EF6", children: _jsxs(Flex, { vertical: true, gap: 8, children: [_jsx(Upload, { accept: ATTACHMENT_ACCEPT, beforeUpload: handleAttachmentBeforeUpload, fileList: attachmentFileList, maxCount: 1, onChange: handleAttachmentChange, onRemove: () => {
                                                setAttachmentFileList([]);
                                                return true;
                                            }, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), children: attachmentFileList.length > 0 ? '更换文件' : '上传文件' }) }), attachmentFileList[0]?.response ? (isImageAttachment(attachmentFileList[0].response) ? (_jsx(Image, { alt: attachmentFileList[0].response.fileName, height: 54, src: getAttachmentUrl(attachmentFileList[0].response), style: { objectFit: 'cover' }, width: 72 })) : (_jsx(Typography.Link, { download: attachmentFileList[0].response.fileName, href: getAttachmentUrl(attachmentFileList[0].response), children: attachmentFileList[0].response.fileName }))) : null] }) }), _jsx(Form.Item, { label: "\u8054\u7CFB\u4EBA", name: "contactName", rules: [{ required: true, message: '请输入联系人' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u624B\u673A\u53F7", name: "phone", rules: [{ required: true, message: '请输入手机号' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u90AE\u7BB1", name: "email", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u5730\u57401", name: "address1", rules: [{ required: true, message: '请输入地址1' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u5730\u57402", name: "address2", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u57CE\u5E02", name: "city", rules: [{ required: true, message: '请输入城市' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u7701/\u5DDE", name: "state", children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u56FD\u5BB6/\u5730\u533A", name: "countryCode", rules: [{ required: true, message: '请选择国家/地区' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: countryRegionOptions, placeholder: "\u8BF7\u9009\u62E9\u56FD\u5BB6/\u5730\u533A" }) }), _jsx(Form.Item, { label: "\u90AE\u7F16", name: "postalCode", rules: [{ required: true, message: '请输入邮编' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u5907\u6CE8", name: "remark", style: { gridColumn: 'span 3' }, children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) })] }) }) })] }));
};
export default PartnerManagementPage;
