import { PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  PageContainer,
  type ProColumns,
  ProFormDateTimePicker,
  ProFormDependency,
  ProFormDigit,
  type ProFormInstance,
  ProFormSelect,
  ProFormSwitch,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Col, Modal, Row, Select, Switch, Tabs } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useRef, useState } from 'react';
import { getCurrencyOptions } from '@/services/finance/currency';
import {
  addQuoteScheme,
  addQuoteSchemeChannel,
  addQuoteSchemeValueFee,
  deleteQuoteSchemeChannel,
  deleteQuoteSchemeValueFee,
  getQuoteScheme,
  getQuoteSchemeBuyerOptions,
  getQuoteSchemeChannels,
  getQuoteSchemeCustomerChannelOptions,
  getQuoteSchemeFeePlaceholderOptions,
  getQuoteSchemeList,
  getQuoteSchemeSystemChannelOptions,
  getQuoteSchemeValueFees,
  getQuoteSchemeWarehouseOptions,
  updateQuoteScheme,
  updateQuoteSchemeChannel,
  updateQuoteSchemeValueFee,
  updateQuoteSchemeStatus,
} from '@/services/finance/quoteScheme';
import { getDictSelectOption } from '@/services/system/dict';
import { message } from '@/utils/feedback';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type SelectOption = {
  label: string;
  value: string | number;
  text?: string;
  listClass?: string;
  status?: string;
  searchText?: string;
};

const schemeTypeFallback = [
  { label: '计费方案', value: 'BILLING', listClass: 'processing' },
  { label: '成本方案', value: 'COST', listClass: 'warning' },
];
const feeSourceFallback = [
  { label: '外部试算', value: 'EXTERNAL_ESTIMATE', listClass: 'processing' },
  { label: '系统费率', value: 'INTERNAL_RATE', listClass: 'default' },
];
const scopeTypeFallback = [
  { label: '全部买家', value: 'ALL_BUYERS', listClass: 'default' },
  { label: '买家等级', value: 'BUYER_LEVEL', listClass: 'processing' },
  { label: '指定买家', value: 'BUYER', listClass: 'warning' },
];
const warehouseScopeFallback = [
  { label: '全部仓库', value: 'ALL_WAREHOUSES', listClass: 'default' },
  { label: '指定仓库', value: 'INCLUDE', listClass: 'processing' },
];
const statusFallback = [
  { label: '启用', value: 'ENABLED', listClass: 'success' },
  { label: '停用', value: 'DISABLED', listClass: 'default' },
];
const valueFeeTriggerFallback = [
  { label: '取消订单', value: 'ORDER_CANCELLED', listClass: 'warning' },
];
const valueFeeCalcMethodFallback = [
  { label: '按百分比调整', value: 'PERCENT', listClass: 'processing' },
  { label: '固定金额', value: 'FIXED_AMOUNT', listClass: 'success' },
];
const valueFeeDirectionFallback = [
  { label: '加收', value: 'INCREASE', listClass: 'success' },
  { label: '减免', value: 'DECREASE', listClass: 'warning' },
];
const defaultSchemeValues: Partial<API.Finance.QuoteScheme> = {
  schemeType: 'BILLING',
  feeSourceMode: 'EXTERNAL_ESTIMATE',
  scopeType: 'ALL_BUYERS',
  warehouseScopeMode: 'ALL_WAREHOUSES',
  effectivePriority: 0,
  status: 'ENABLED',
};
const defaultChannelValues: Partial<API.Finance.QuoteSchemeChannel> & { enabled?: boolean } = {
  status: 'ENABLED',
  enabled: true,
  displayOrder: 0,
};
const defaultValueFeeValues: Partial<API.Finance.QuoteSchemeValueFeeRule> & { enabled?: boolean } = {
  triggerCode: 'ORDER_CANCELLED',
  calculationMethod: 'PERCENT',
  adjustmentDirection: 'INCREASE',
  adjustmentValue: 0,
  status: 'ENABLED',
  enabled: true,
  displayOrder: 0,
};
function getLogisticsChannelLabel(schemeType?: string) {
  return schemeType === 'COST' ? '系统物流渠道' : '客户物流渠道';
}

function getFreightFeeLabel(feeSourceMode?: string) {
  return feeSourceMode === 'INTERNAL_RATE' ? '系统运费规则' : '试算运费项';
}

function getOperationFeeLabel(feeSourceMode?: string) {
  return feeSourceMode === 'INTERNAL_RATE' ? '系统操作费规则' : '试算操作费项';
}

function getFeePlaceholder(feeSourceMode?: string) {
  return feeSourceMode === 'INTERNAL_RATE' ? '暂无系统费率' : '暂无试算项';
}

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function normalizeOptions(options: any[] | undefined, fallback: SelectOption[]) {
  return Array.isArray(options) && options.length > 0 ? options : fallback;
}

function toValueEnum(options: SelectOption[]) {
  return Object.fromEntries(
    options.map((item) => [
      item.value,
      { text: item.label ?? item.text, status: item.listClass ?? item.status },
    ]),
  );
}

function toDateTime(value?: any) {
  if (!value) return undefined;
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

function toFormDateTime(value?: any) {
  return value ? dayjs(value) : undefined;
}

function normalizeWarehouseCodes(value?: string | string[]) {
  if (Array.isArray(value)) {
    const first = value.find(Boolean);
    return first ? [first] : [];
  }
  return value ? [value] : [];
}

function buildFormValues(detail?: API.Finance.QuoteScheme): any {
  if (!detail) return defaultSchemeValues;
  const warehouseCode =
    detail.warehouses?.find((item) => item.warehouseCode)?.warehouseCode
    || normalizeWarehouseCodes(detail.warehouseCodes as any)[0];
  return {
    ...detail,
    effectiveTime: toFormDateTime(detail.effectiveTime),
    expireTime: toFormDateTime(detail.expireTime),
    buyerLevelCodes: detail.scopes
      ?.filter((item) => item.scopeType === 'BUYER_LEVEL' && item.buyerLevelCode)
      .map((item) => item.buyerLevelCode as string),
    buyerIds: detail.scopes
      ?.filter((item) => item.scopeType === 'BUYER' && item.buyerId)
      .map((item) => item.buyerId as number),
    warehouseCodes: warehouseCode,
  };
}

function buildChannelFormValues(detail?: API.Finance.QuoteSchemeChannel): any {
  return {
    ...defaultChannelValues,
    ...detail,
    enabled: (detail?.status || 'ENABLED') === 'ENABLED',
  };
}

function buildValueFeeFormValues(detail?: API.Finance.QuoteSchemeValueFeeRule): any {
  return {
    ...defaultValueFeeValues,
    ...detail,
    enabled: (detail?.status || 'ENABLED') === 'ENABLED',
  };
}

function getOptionLabel(options: SelectOption[], value?: string, fallback = '-') {
  return options.find((item) => item.value === value)?.label || fallback;
}

function renderValueFeeAdjustment(
  record: API.Finance.QuoteSchemeValueFeeRule,
  directionOptions: SelectOption[],
  currencyCode?: string,
) {
  const direction = getOptionLabel(directionOptions, record.adjustmentDirection, '');
  const value = record.adjustmentValue ?? 0;
  if (record.calculationMethod === 'FIXED_AMOUNT') {
    return `${direction}${value} ${currencyCode || ''}`.trim();
  }
  return `${direction}${value}%`;
}

export default function FinanceQuoteSchemePage() {
  const access = useAccess();
  const canAdd = access.hasPerms('finance:quoteScheme:add');
  const canEdit = access.hasPerms('finance:quoteScheme:edit');
  const canChangeStatus = access.hasPerms('finance:quoteScheme:status');
  const canManageChannel = access.hasPerms('finance:quoteScheme:channel');
  const canManageValueFee = access.hasPerms('finance:quoteScheme:valueFee');
  const actionRef = useRef<ActionType>(null);
  const channelActionRef = useRef<ActionType>(null);
  const valueFeeActionRef = useRef<ActionType>(null);
  const schemeFormRef =
    useRef<ProFormInstance<API.Finance.QuoteScheme> | undefined>(undefined);
  const channelFormRef =
    useRef<ProFormInstance<API.Finance.QuoteSchemeChannel> | undefined>(undefined);
  const valueFeeFormRef =
    useRef<ProFormInstance<API.Finance.QuoteSchemeValueFeeRule> | undefined>(undefined);
  const [schemeTypeOptions, setSchemeTypeOptions] =
    useState<SelectOption[]>(schemeTypeFallback);
  const [feeSourceOptions, setFeeSourceOptions] =
    useState<SelectOption[]>(feeSourceFallback);
  const [scopeTypeOptions, setScopeTypeOptions] =
    useState<SelectOption[]>(scopeTypeFallback);
  const [warehouseScopeOptions, setWarehouseScopeOptions] =
    useState<SelectOption[]>(warehouseScopeFallback);
  const [statusOptions, setStatusOptions] =
    useState<SelectOption[]>(statusFallback);
  const [valueFeeTriggerOptions, setValueFeeTriggerOptions] =
    useState<SelectOption[]>(valueFeeTriggerFallback);
  const [valueFeeCalcMethodOptions, setValueFeeCalcMethodOptions] =
    useState<SelectOption[]>(valueFeeCalcMethodFallback);
  const [valueFeeDirectionOptions, setValueFeeDirectionOptions] =
    useState<SelectOption[]>(valueFeeDirectionFallback);
  const [currencyOptions, setCurrencyOptions] = useState<SelectOption[]>([]);
  const [buyerLevelOptions, setBuyerLevelOptions] = useState<SelectOption[]>([]);
  const [buyerOptions, setBuyerOptions] = useState<SelectOption[]>([]);
  const [warehouseOptions, setWarehouseOptions] = useState<SelectOption[]>([]);
  const [customerChannelOptions, setCustomerChannelOptions] = useState<SelectOption[]>([]);
  const [systemChannelOptions, setSystemChannelOptions] = useState<SelectOption[]>([]);
  const [operationFeeOptions, setOperationFeeOptions] = useState<SelectOption[]>([]);
  const [freightFeeOptions, setFreightFeeOptions] = useState<SelectOption[]>([]);
  const [schemeModalOpen, setSchemeModalOpen] = useState(false);
  const [channelModalOpen, setChannelModalOpen] = useState(false);
  const [valueFeeModalOpen, setValueFeeModalOpen] = useState(false);
  const [schemeActiveTab, setSchemeActiveTab] = useState('freight');
  const [schemeEditorMode, setSchemeEditorMode] = useState<'create' | 'edit'>('create');
  const [currentScheme, setCurrentScheme] = useState<API.Finance.QuoteScheme>();
  const [channelScheme, setChannelScheme] = useState<API.Finance.QuoteScheme>();
  const [currentChannel, setCurrentChannel] =
    useState<API.Finance.QuoteSchemeChannel>();
  const [currentValueFee, setCurrentValueFee] =
    useState<API.Finance.QuoteSchemeValueFeeRule>();

  useEffect(() => {
    async function loadOptions() {
      const [
        schemeTypes,
        feeSources,
        scopeTypes,
        warehouseScopes,
        statuses,
        valueFeeTriggers,
        valueFeeCalcMethods,
        valueFeeDirections,
        buyerLevels,
        currencies,
        buyers,
        warehouses,
        customerChannels,
        systemChannels,
        operationFees,
        freightFees,
      ] = await Promise.allSettled([
        getDictSelectOption('quote_scheme_type'),
        getDictSelectOption('quote_scheme_fee_source_mode'),
        getDictSelectOption('quote_scheme_scope_type'),
        getDictSelectOption('quote_scheme_warehouse_scope_mode'),
        getDictSelectOption('quote_scheme_status'),
        getDictSelectOption('quote_scheme_value_fee_trigger'),
        getDictSelectOption('quote_scheme_value_fee_calc_method'),
        getDictSelectOption('quote_scheme_value_fee_direction'),
        getDictSelectOption('buyer_level'),
        getCurrencyOptions(),
        getQuoteSchemeBuyerOptions(),
        getQuoteSchemeWarehouseOptions(),
        getQuoteSchemeCustomerChannelOptions(),
        getQuoteSchemeSystemChannelOptions(),
        getQuoteSchemeFeePlaceholderOptions('OPERATION'),
        getQuoteSchemeFeePlaceholderOptions('FREIGHT'),
      ]);
      if (schemeTypes.status === 'fulfilled') setSchemeTypeOptions(normalizeOptions(schemeTypes.value as any[], schemeTypeFallback));
      if (feeSources.status === 'fulfilled') setFeeSourceOptions(normalizeOptions(feeSources.value as any[], feeSourceFallback));
      if (scopeTypes.status === 'fulfilled') setScopeTypeOptions(normalizeOptions(scopeTypes.value as any[], scopeTypeFallback));
      if (warehouseScopes.status === 'fulfilled') setWarehouseScopeOptions(normalizeOptions(warehouseScopes.value as any[], warehouseScopeFallback));
      if (statuses.status === 'fulfilled') setStatusOptions(normalizeOptions(statuses.value as any[], statusFallback));
      if (valueFeeTriggers.status === 'fulfilled') setValueFeeTriggerOptions(normalizeOptions(valueFeeTriggers.value as any[], valueFeeTriggerFallback));
      if (valueFeeCalcMethods.status === 'fulfilled') setValueFeeCalcMethodOptions(normalizeOptions(valueFeeCalcMethods.value as any[], valueFeeCalcMethodFallback));
      if (valueFeeDirections.status === 'fulfilled') setValueFeeDirectionOptions(normalizeOptions(valueFeeDirections.value as any[], valueFeeDirectionFallback));
      if (buyerLevels.status === 'fulfilled') setBuyerLevelOptions(normalizeOptions(buyerLevels.value as any[], []));
      if (currencies.status === 'fulfilled' && currencies.value.code === 200) setCurrencyOptions(currencies.value.data);
      if (buyers.status === 'fulfilled' && buyers.value.code === 200) setBuyerOptions(buyers.value.data);
      if (warehouses.status === 'fulfilled' && warehouses.value.code === 200) setWarehouseOptions(warehouses.value.data);
      if (customerChannels.status === 'fulfilled' && customerChannels.value.code === 200) setCustomerChannelOptions(customerChannels.value.data);
      if (systemChannels.status === 'fulfilled' && systemChannels.value.code === 200) setSystemChannelOptions(systemChannels.value.data);
      if (operationFees.status === 'fulfilled' && operationFees.value.code === 200) setOperationFeeOptions(operationFees.value.data);
      if (freightFees.status === 'fulfilled' && freightFees.value.code === 200) setFreightFeeOptions(freightFees.value.data);
    }
    loadOptions();
  }, []);

  useEffect(() => {
    if (!schemeModalOpen) return;
    const timer = setTimeout(() => {
      schemeFormRef.current?.resetFields();
      schemeFormRef.current?.setFieldsValue(buildFormValues(currentScheme));
      if (currentScheme?.schemeId) {
        channelActionRef.current?.reload();
        valueFeeActionRef.current?.reload();
      }
    }, 0);
    return () => clearTimeout(timer);
  }, [currentScheme, schemeModalOpen]);

  useEffect(() => {
    if (!channelModalOpen) return;
    const timer = setTimeout(() => {
      channelFormRef.current?.resetFields();
      channelFormRef.current?.setFieldsValue(buildChannelFormValues(currentChannel));
    }, 0);
    return () => clearTimeout(timer);
  }, [currentChannel, channelModalOpen]);

  useEffect(() => {
    if (!valueFeeModalOpen) return;
    const timer = setTimeout(() => {
      valueFeeFormRef.current?.resetFields();
      valueFeeFormRef.current?.setFieldsValue(buildValueFeeFormValues(currentValueFee));
    }, 0);
    return () => clearTimeout(timer);
  }, [currentValueFee, valueFeeModalOpen]);

  const openCreateScheme = () => {
    setSchemeEditorMode('create');
    setCurrentScheme(undefined);
    setChannelScheme(undefined);
    setCurrentChannel(undefined);
    setCurrentValueFee(undefined);
    setSchemeActiveTab('freight');
    setSchemeModalOpen(true);
  };

  const openEditScheme = async (record: API.Finance.QuoteScheme, tabKey = 'freight') => {
    if (!record.schemeId) return;
    const resp = await getQuoteScheme(record.schemeId);
    if (resp.code !== 200) {
      message.error(resp.msg || '报价方案详情加载失败');
      return;
    }
    setSchemeEditorMode('edit');
    setCurrentScheme(resp.data);
    setChannelScheme(resp.data);
    setSchemeActiveTab(tabKey);
    setSchemeModalOpen(true);
  };

  const saveScheme = async (values: API.Finance.QuoteScheme) => {
    const payload: API.Finance.QuoteScheme = {
      ...values,
      status: currentScheme?.status || values.status || 'ENABLED',
      effectiveTime: toDateTime(values.effectiveTime),
      expireTime: toDateTime(values.expireTime),
      buyerIds: values.buyerIds?.map((item) => Number(item)),
      warehouseCodes: normalizeWarehouseCodes((values as any).warehouseCodes),
    };
    const isEdit = !!currentScheme?.schemeId;
    const resp = isEdit
      ? await updateQuoteScheme(currentScheme!.schemeId!, payload)
      : await addQuoteScheme(payload);
    if (resultOk(resp, isEdit ? '报价方案已更新' : '报价方案已新增')) {
      actionRef.current?.reload();
      if (!isEdit) {
        const createResp = resp as API.Finance.QuoteSchemeResult;
        const created: API.Finance.QuoteScheme = createResp.data
          ? {
              ...createResp.data,
              buyerLevelCodes: payload.buyerLevelCodes,
              buyerIds: payload.buyerIds,
              warehouseCodes: payload.warehouseCodes,
            }
          : payload;
        setCurrentScheme(created);
        setChannelScheme(created);
        setSchemeActiveTab('freight');
        schemeFormRef.current?.setFieldsValue(buildFormValues(created));
        return false;
      }
      setSchemeModalOpen(false);
      return true;
    }
    return false;
  };

  const toggleSchemeStatus = async (record: API.Finance.QuoteScheme) => {
    if (!record.schemeId) return;
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const ok = resultOk(
      await updateQuoteSchemeStatus(record.schemeId, nextStatus),
      nextStatus === 'ENABLED' ? '报价方案已启用' : '报价方案已停用',
    );
    if (ok) actionRef.current?.reload();
  };

  const openCreateChannel = () => {
    if (!channelScheme?.schemeId) {
      message.warning('请先保存基础信息，再配置物流费');
      return;
    }
    setCurrentChannel(undefined);
    setChannelModalOpen(true);
  };

  const openEditChannel = (record: API.Finance.QuoteSchemeChannel) => {
    setCurrentChannel(record);
    setChannelModalOpen(true);
  };

  const saveChannel = async (values: API.Finance.QuoteSchemeChannel) => {
    if (!channelScheme?.schemeId) return false;
    const payload = {
      ...values,
      status: (values as any).enabled === false ? 'DISABLED' : 'ENABLED',
    };
    delete (payload as any).enabled;
    const resp = currentChannel?.schemeChannelId
      ? await updateQuoteSchemeChannel(
          channelScheme.schemeId,
          currentChannel.schemeChannelId,
          payload,
        )
      : await addQuoteSchemeChannel(channelScheme.schemeId, payload);
    if (resultOk(resp, currentChannel ? '客户渠道已更新' : '客户渠道已新增')) {
      setChannelModalOpen(false);
      channelActionRef.current?.reload();
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const toggleChannelStatus = async (record: API.Finance.QuoteSchemeChannel) => {
    if (!channelScheme?.schemeId || !record.schemeChannelId) return;
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const ok = resultOk(
      await updateQuoteSchemeChannel(channelScheme.schemeId, record.schemeChannelId, {
        ...record,
        status: nextStatus,
      }),
      nextStatus === 'ENABLED' ? '客户渠道已启用' : '客户渠道已停用',
    );
    if (ok) {
      channelActionRef.current?.reload();
      actionRef.current?.reload();
    }
  };

  const removeChannel = (record: API.Finance.QuoteSchemeChannel) => {
    if (!channelScheme?.schemeId || !record.schemeChannelId) return;
    Modal.confirm({
      title: '删除客户渠道',
      content: `确认从方案中删除 ${record.customerChannelNameSnapshot || record.customerChannelCode}？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteQuoteSchemeChannel(channelScheme.schemeId!, record.schemeChannelId!),
          '客户渠道已删除',
        );
        if (ok) {
          channelActionRef.current?.reload();
          actionRef.current?.reload();
        }
      },
    });
  };

  const openCreateValueFee = () => {
    if (!channelScheme?.schemeId) {
      message.warning('请先保存基础信息，再配置增值费');
      return;
    }
    setCurrentValueFee(undefined);
    setValueFeeModalOpen(true);
  };

  const openEditValueFee = (record: API.Finance.QuoteSchemeValueFeeRule) => {
    setCurrentValueFee(record);
    setValueFeeModalOpen(true);
  };

  const saveValueFee = async (values: API.Finance.QuoteSchemeValueFeeRule) => {
    if (!channelScheme?.schemeId) return false;
    const payload = {
      ...values,
      status: (values as any).enabled === false ? 'DISABLED' : 'ENABLED',
    };
    delete (payload as any).enabled;
    const resp = currentValueFee?.valueFeeRuleId
      ? await updateQuoteSchemeValueFee(
          channelScheme.schemeId,
          currentValueFee.valueFeeRuleId,
          payload,
        )
      : await addQuoteSchemeValueFee(channelScheme.schemeId, payload);
    if (resultOk(resp, currentValueFee ? '增值费已更新' : '增值费已新增')) {
      setValueFeeModalOpen(false);
      valueFeeActionRef.current?.reload();
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const toggleValueFeeStatus = async (record: API.Finance.QuoteSchemeValueFeeRule) => {
    if (!channelScheme?.schemeId || !record.valueFeeRuleId) return;
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const ok = resultOk(
      await updateQuoteSchemeValueFee(channelScheme.schemeId, record.valueFeeRuleId, {
        ...record,
        status: nextStatus,
      }),
      nextStatus === 'ENABLED' ? '增值费已启用' : '增值费已停用',
    );
    if (ok) {
      valueFeeActionRef.current?.reload();
      actionRef.current?.reload();
    }
  };

  const removeValueFee = (record: API.Finance.QuoteSchemeValueFeeRule) => {
    if (!channelScheme?.schemeId || !record.valueFeeRuleId) return;
    Modal.confirm({
      title: '删除增值费',
      content: `确认删除 ${record.logisticsChannelNameSnapshot || record.logisticsChannelCode} 的增值费规则？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteQuoteSchemeValueFee(channelScheme.schemeId!, record.valueFeeRuleId!),
          '增值费已删除',
        );
        if (ok) {
          valueFeeActionRef.current?.reload();
          actionRef.current?.reload();
        }
      },
    });
  };

  const activeChannelScheme = channelScheme || currentScheme;
  const isCreateBaseStep = schemeEditorMode === 'create' && !currentScheme?.schemeId;
  const shouldShowSchemeDetails = schemeEditorMode === 'edit' || !!currentScheme?.schemeId;
  const schemeModalTitle = schemeEditorMode === 'edit'
    ? '编辑报价方案'
    : (isCreateBaseStep ? '新增报价方案' : '配置报价方案');
  const schemeSubmitText = isCreateBaseStep
    ? '保存并下一步'
    : (schemeEditorMode === 'edit' ? '确定' : '完成');

  const schemeColumns: ProColumns<API.Finance.QuoteScheme>[] = [
    { title: '方案名称', dataIndex: 'schemeName', width: 190 },
    {
      title: '方案类型',
      dataIndex: 'schemeType',
      valueType: 'select',
      valueEnum: toValueEnum(schemeTypeOptions),
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 110,
    },
    {
      title: '费用来源',
      dataIndex: 'feeSourceMode',
      valueType: 'select',
      valueEnum: toValueEnum(feeSourceOptions),
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 120,
    },
    {
      title: '币种',
      dataIndex: 'currencyCode',
      valueType: 'select',
      fieldProps: { ...SEARCHABLE_SELECT_PROPS, options: currencyOptions },
      width: 90,
    },
    {
      title: '适用对象',
      dataIndex: 'scopeSummary',
      search: false,
      width: 200,
      ellipsis: true,
    },
    {
      title: '仓库范围',
      dataIndex: 'warehouseSummary',
      search: false,
      width: 200,
      ellipsis: true,
    },
    {
      title: '物流渠道',
      dataIndex: 'channelCount',
      search: false,
      width: 120,
      render: (_, record) => record.channelCount ? `${record.channelCount} 个` : '未配置',
    },
    { title: '优先级', dataIndex: 'effectivePriority', search: false, width: 90 },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: toValueEnum(statusOptions),
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 120,
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          checkedChildren="启用"
          unCheckedChildren="停用"
          disabled={!canChangeStatus}
          onClick={() => toggleSchemeStatus(record)}
        />
      ),
    },
    { title: '生效时间', dataIndex: 'effectiveTime', search: false, width: 170 },
    {
      title: '生效时间',
      dataIndex: 'effectiveTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          effectiveBeginTime: value?.[0],
          effectiveEndTime: value?.[1],
        }),
      },
    },
    { title: '失效时间', dataIndex: 'expireTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 90,
      fixed: 'right',
      render: (_, record) => [
        canEdit ? (
          <Button key="edit" type="link" size="small" onClick={() => openEditScheme(record)}>
            编辑
          </Button>
        ) : null,
      ],
    },
  ];

  const channelColumns: ProColumns<API.Finance.QuoteSchemeChannel>[] = [
    {
      title: `${getLogisticsChannelLabel(activeChannelScheme?.schemeType)}编码`,
      dataIndex: 'customerChannelCode',
      width: 180,
    },
    {
      title: `${getLogisticsChannelLabel(activeChannelScheme?.schemeType)}名称`,
      dataIndex: 'customerChannelNameSnapshot',
      width: 240,
    },
    {
      title: getFreightFeeLabel(activeChannelScheme?.feeSourceMode),
      dataIndex: 'freightFeeCode',
      width: 180,
      renderText: (value) => value || '占位',
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: toValueEnum(statusOptions),
      width: 90,
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          checkedChildren="启用"
          unCheckedChildren="停用"
          disabled={!canManageChannel}
          onClick={() => toggleChannelStatus(record)}
        />
      ),
    },
    { title: '排序', dataIndex: 'displayOrder', width: 80 },
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      render: (_, record) => [
        canManageChannel ? (
          <Button key="edit" type="link" size="small" onClick={() => openEditChannel(record)}>
            编辑
          </Button>
        ) : null,
        canManageChannel ? (
          <Button key="delete" type="link" size="small" danger onClick={() => removeChannel(record)}>
            删除
          </Button>
        ) : null,
      ],
    },
  ];

  const valueFeeColumns: ProColumns<API.Finance.QuoteSchemeValueFeeRule>[] = [
    {
      title: `${getLogisticsChannelLabel(activeChannelScheme?.schemeType)}编码`,
      dataIndex: 'logisticsChannelCode',
      width: 180,
    },
    {
      title: `${getLogisticsChannelLabel(activeChannelScheme?.schemeType)}名称`,
      dataIndex: 'logisticsChannelNameSnapshot',
      width: 240,
    },
    {
      title: '触发情况',
      dataIndex: 'triggerCode',
      valueType: 'select',
      valueEnum: toValueEnum(valueFeeTriggerOptions),
      width: 120,
    },
    {
      title: '收费方式',
      dataIndex: 'calculationMethod',
      valueType: 'select',
      valueEnum: toValueEnum(valueFeeCalcMethodOptions),
      width: 130,
    },
    {
      title: '调整值',
      dataIndex: 'adjustmentValue',
      width: 140,
      render: (_, record) =>
        renderValueFeeAdjustment(record, valueFeeDirectionOptions, activeChannelScheme?.currencyCode),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: toValueEnum(statusOptions),
      width: 90,
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          checkedChildren="启用"
          unCheckedChildren="停用"
          disabled={!canManageValueFee}
          onClick={() => toggleValueFeeStatus(record)}
        />
      ),
    },
    { title: '排序', dataIndex: 'displayOrder', width: 80 },
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      render: (_, record) => [
        canManageValueFee ? (
          <Button key="edit" type="link" size="small" onClick={() => openEditValueFee(record)}>
            编辑
          </Button>
        ) : null,
        canManageValueFee ? (
          <Button key="delete" type="link" size="small" danger onClick={() => removeValueFee(record)}>
            删除
          </Button>
        ) : null,
      ],
    },
  ];

  return (
    <PageContainer title={false}>
      <ProTable<API.Finance.QuoteScheme>
        actionRef={actionRef}
        rowKey="schemeId"
        columns={schemeColumns}
        search={getPersistedProTableSearch({ fieldCount: 6 }, 'finance-quote-scheme')}
        pagination={getProTablePagination()}
        scroll={getProTableScroll(1600)}
        request={async (params) => {
          const { current, pageSize, ...rest } = params;
          const resp = await getQuoteSchemeList({
            ...rest,
            pageNum: current,
            pageSize,
          });
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        toolBarRender={() =>
          canAdd
            ? [
                <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreateScheme}>
                  新增
                </Button>,
              ]
            : []
        }
      />

      <ModalForm<API.Finance.QuoteScheme>
        title={schemeModalTitle}
        width="92vw"
        open={schemeModalOpen}
        formRef={schemeFormRef}
        initialValues={buildFormValues(currentScheme)}
        modalProps={{
          destroyOnClose: true,
          onCancel: () => setSchemeModalOpen(false),
          style: { maxWidth: 1320, top: 24 },
          styles: {
            body: {
              maxHeight: 'calc(100vh - 168px)',
              overflowY: 'auto',
              overflowX: 'hidden',
              paddingTop: 8,
            },
          },
        }}
        submitter={{
          searchConfig: {
            submitText: schemeSubmitText,
            resetText: '取消',
          },
        }}
        onFinish={saveScheme}
      >
        <div style={{ padding: '8px 0 14px' }}>
          <div style={{ borderLeft: '3px solid #1677ff', paddingLeft: 10, fontWeight: 600, marginBottom: 16 }}>
            基础信息
          </div>
          <Row gutter={24} style={{ marginLeft: 0, marginRight: 0 }}>
            <Col xs={24} md={12} xl={8}>
              <ProFormText
                name="schemeName"
                label="方案名称"
                rules={[{ required: true, message: '请输入方案名称' }]}
                fieldProps={{ maxLength: 200, style: { width: '100%' } }}
              />
            </Col>
            <Col xs={24} md={12} xl={8}>
              <ProFormSelect
                name="schemeType"
                label="方案类型"
                options={schemeTypeOptions}
                fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                rules={[{ required: true, message: '请选择方案类型' }]}
              />
            </Col>
            <Col xs={24} md={12} xl={8}>
              <ProFormSelect
                name="feeSourceMode"
                label="费用来源模式"
                options={feeSourceOptions}
                fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                rules={[{ required: true, message: '请选择费用来源模式' }]}
              />
            </Col>
            <Col xs={24} md={12} xl={8}>
              <ProFormSelect
                name="currencyCode"
                label="币种"
                options={currencyOptions}
                fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                rules={[{ required: true, message: '请选择币种' }]}
              />
            </Col>
            <Col xs={24} md={12} xl={8}>
              <ProFormSelect
                name="scopeType"
                label="适用对象"
                options={scopeTypeOptions}
                fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                rules={[{ required: true, message: '请选择适用对象' }]}
              />
            </Col>
            <ProFormDependency name={['scopeType']}>
              {({ scopeType }) =>
                scopeType === 'BUYER_LEVEL' ? (
                  <Col xs={24} md={12} xl={8}>
                    <ProFormSelect
                      name="buyerLevelCodes"
                      label="买家等级"
                      mode="multiple"
                      options={buyerLevelOptions}
                      fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                      rules={[{ required: true, message: '请选择买家等级' }]}
                    />
                  </Col>
                ) : scopeType === 'BUYER' ? (
                  <Col xs={24} md={12} xl={8}>
                    <ProFormSelect
                      name="buyerIds"
                      label="买家"
                      mode="multiple"
                      options={buyerOptions}
                      fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                      rules={[{ required: true, message: '请选择买家' }]}
                    />
                  </Col>
                ) : null
              }
            </ProFormDependency>
            <Col xs={24} md={12} xl={8}>
              <ProFormSelect
                name="warehouseScopeMode"
                label="仓库范围"
                options={warehouseScopeOptions}
                fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                rules={[{ required: true, message: '请选择仓库范围' }]}
              />
            </Col>
            <ProFormDependency name={['warehouseScopeMode']}>
              {({ warehouseScopeMode }) =>
                warehouseScopeMode === 'INCLUDE' ? (
                  <Col xs={24} md={12} xl={8}>
                    <ProFormSelect
                      name="warehouseCodes"
                      label="适用仓库"
                      options={warehouseOptions}
                      fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                      rules={[{ required: true, message: '请选择适用仓库' }]}
                    />
                  </Col>
                ) : null
              }
            </ProFormDependency>
            <Col xs={24} md={12} xl={8}>
              <ProFormDateTimePicker
                name="effectiveTime"
                label="生效时间"
                fieldProps={{ style: { width: '100%' } }}
                rules={[{ required: true, message: '请选择生效时间' }]}
              />
            </Col>
            <Col xs={24} md={12} xl={8}>
              <ProFormDateTimePicker
                name="expireTime"
                label="失效时间"
                fieldProps={{ style: { width: '100%' } }}
              />
            </Col>
            <Col xs={24} md={12} xl={8}>
              <ProFormDigit
                name="effectivePriority"
                label="生效优先级"
                min={0}
                fieldProps={{ precision: 0, placeholder: '数字越大越优先', style: { width: '100%' } }}
              />
            </Col>
            <Col span={24}>
              <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500, rows: 2 }} />
            </Col>
          </Row>
        </div>

        {shouldShowSchemeDetails ? (
          <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8 }}>
            <ProFormDependency name={['schemeType', 'feeSourceMode']}>
              {({ schemeType, feeSourceMode }) => {
                const channelLabel = getLogisticsChannelLabel(schemeType);
                return (
                  <Tabs
                    activeKey={schemeActiveTab}
                    onChange={setSchemeActiveTab}
                    items={[
                      {
                        key: 'freight',
                        label: '物流费',
                        children: (
                          <ProTable<API.Finance.QuoteSchemeChannel>
                            actionRef={channelActionRef}
                            rowKey="schemeChannelId"
                            columns={channelColumns}
                            search={false}
                            pagination={getProTablePagination(8)}
                            options={false}
                            scroll={getProTableScroll(920, { y: 280 })}
                            request={async () => {
                              if (!currentScheme?.schemeId) {
                                return { data: [], success: true, total: 0 };
                              }
                              const resp = await getQuoteSchemeChannels(currentScheme.schemeId);
                              return {
                                data: resp.data || [],
                                success: resp.code === 200,
                                total: resp.data?.length || 0,
                              };
                            }}
                            toolBarRender={() =>
                              canManageChannel
                                ? [
                                    <Button
                                      key="add"
                                      type="primary"
                                      icon={<PlusOutlined />}
                                      onClick={openCreateChannel}
                                    >
                                      新增{channelLabel}
                                    </Button>,
                                  ]
                                : []
                            }
                          />
                        ),
                      },
                      {
                        key: 'operation',
                        label: '操作费',
                        children: (
                          <Row gutter={24} style={{ marginLeft: 0, marginRight: 0 }}>
                            <Col xs={24} md={12} xl={8}>
                              <div style={{ marginBottom: 8 }}>{getOperationFeeLabel(feeSourceMode)}</div>
                              <Select
                                disabled
                                allowClear
                                showSearch
                                options={operationFeeOptions}
                                placeholder={getFeePlaceholder(feeSourceMode)}
                                style={{ width: '100%' }}
                              />
                            </Col>
                          </Row>
                        ),
                      },
                      {
                        key: 'valueFee',
                        label: '增值费',
                        children: (
                          <ProTable<API.Finance.QuoteSchemeValueFeeRule>
                            actionRef={valueFeeActionRef}
                            rowKey="valueFeeRuleId"
                            columns={valueFeeColumns}
                            search={false}
                            pagination={getProTablePagination(8)}
                            options={false}
                            scroll={getProTableScroll(980, { y: 280 })}
                            request={async () => {
                              if (!currentScheme?.schemeId) {
                                return { data: [], success: true, total: 0 };
                              }
                              const resp = await getQuoteSchemeValueFees(currentScheme.schemeId);
                              return {
                                data: resp.data || [],
                                success: resp.code === 200,
                                total: resp.data?.length || 0,
                              };
                            }}
                            toolBarRender={() =>
                              canManageValueFee
                                ? [
                                    <Button
                                      key="add"
                                      type="primary"
                                      icon={<PlusOutlined />}
                                      onClick={openCreateValueFee}
                                    >
                                      新增增值费
                                    </Button>,
                                  ]
                                : []
                            }
                          />
                        ),
                      },
                    ]}
                  />
                );
              }}
            </ProFormDependency>
          </div>
        ) : null}
      </ModalForm>

      <ModalForm<API.Finance.QuoteSchemeChannel>
        title={
          currentChannel
            ? `编辑${getLogisticsChannelLabel(channelScheme?.schemeType)}`
            : `新增${getLogisticsChannelLabel(channelScheme?.schemeType)}`
        }
        width={640}
        open={channelModalOpen}
        formRef={channelFormRef}
        modalProps={{ destroyOnClose: true, onCancel: () => setChannelModalOpen(false) }}
        onFinish={saveChannel}
      >
        <ProFormSelect
          name="customerChannelCode"
          label={getLogisticsChannelLabel(channelScheme?.schemeType)}
          options={channelScheme?.schemeType === 'COST' ? systemChannelOptions : customerChannelOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: `请选择${getLogisticsChannelLabel(channelScheme?.schemeType)}` }]}
        />
        <ProFormSelect
          name="freightFeeCode"
          label={getFreightFeeLabel(channelScheme?.feeSourceMode)}
          options={freightFeeOptions}
          disabled={freightFeeOptions.length === 0}
          placeholder={getFeePlaceholder(channelScheme?.feeSourceMode)}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormSwitch
          name="enabled"
          label="状态"
          fieldProps={{
            checkedChildren: '启用',
            unCheckedChildren: '停用',
          }}
        />
        <ProFormDigit name="displayOrder" label="排序" min={0} fieldProps={{ precision: 0 }} />
        <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500 }} />
      </ModalForm>

      <ModalForm<API.Finance.QuoteSchemeValueFeeRule>
        title={currentValueFee ? '编辑增值费' : '新增增值费'}
        width={640}
        open={valueFeeModalOpen}
        formRef={valueFeeFormRef}
        modalProps={{ destroyOnClose: true, onCancel: () => setValueFeeModalOpen(false) }}
        onFinish={saveValueFee}
      >
        <ProFormSelect
          name="logisticsChannelCode"
          label={getLogisticsChannelLabel(channelScheme?.schemeType)}
          options={channelScheme?.schemeType === 'COST' ? systemChannelOptions : customerChannelOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: `请选择${getLogisticsChannelLabel(channelScheme?.schemeType)}` }]}
        />
        <ProFormSelect
          name="triggerCode"
          label="触发情况"
          options={valueFeeTriggerOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择触发情况' }]}
        />
        <ProFormSelect
          name="calculationMethod"
          label="收费方式"
          options={valueFeeCalcMethodOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择收费方式' }]}
        />
        <ProFormSelect
          name="adjustmentDirection"
          label="调整方向"
          options={valueFeeDirectionOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择调整方向' }]}
        />
        <ProFormDependency name={['calculationMethod']}>
          {({ calculationMethod }) => (
            <ProFormDigit
              name="adjustmentValue"
              label={calculationMethod === 'FIXED_AMOUNT' ? '固定金额' : '调整比例'}
              min={0}
              fieldProps={{
                precision: 6,
                addonAfter: calculationMethod === 'FIXED_AMOUNT' ? channelScheme?.currencyCode : '%',
              }}
              rules={[{ required: true, message: '请输入调整值' }]}
            />
          )}
        </ProFormDependency>
        <ProFormSwitch
          name="enabled"
          label="状态"
          fieldProps={{
            checkedChildren: '启用',
            unCheckedChildren: '停用',
          }}
        />
        <ProFormDigit name="displayOrder" label="排序" min={0} fieldProps={{ precision: 0 }} />
        <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500 }} />
      </ModalForm>
    </PageContainer>
  );
}
