import {
  LinkOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProForm,
  ProFormCheckbox,
  type ProColumns,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
  type ActionType,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Col, Form, Modal, Row, Space, Switch, Tabs, Tag, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import {
  addCarrierMapping,
  addSystemChannel,
  addWarehouseBinding,
  deleteCarrierMapping,
  deleteWarehouseBinding,
  getCarrierAccountOptions,
  getCarrierChannelOptions,
  getCarrierMappings,
  getOrderSetting,
  getSystemChannel,
  getSystemChannelList,
  getWarehouseBindings,
  getWarehouseOptions,
  saveOrderSetting,
  updateSystemChannel,
  updateSystemChannelStatus,
  updateWarehouseBinding,
} from '@/services/logistics/systemChannel';
import {
  addLogisticsChannelPairing,
  deleteLogisticsChannelPairing,
  getLogisticsChannelPairings,
  getLogisticsChannelSyncList,
  getUpstreamConnectionList,
} from '@/services/integration/upstreamSystem';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type OptionItem = {
  label: string;
  value: string;
  searchText?: string;
  extra?: string;
};

type SystemChannel = {
  systemChannelCode: string;
  systemChannelName: string;
  fulfillmentMode?: string;
  standardCarrierCode: string;
  signatureServices?: string | string[];
  status: string;
  carrierMappingCount?: number;
  warehouseCount?: number;
  carrierAccountSummary?: string;
  warehouseSummary?: string;
  updateBy?: string;
  updateTime?: string;
  remark?: string;
};

type CarrierMapping = {
  mappingId: number;
  carrierAccountId: number;
  providerKind: string;
  carrierName: string;
  externalChannelCode: string;
  externalChannelNameSnapshot: string;
  standardCarrierCode: string;
  status: string;
  createTime?: string;
  remark?: string;
};

type WarehouseBinding = {
  bindingId: number;
  warehouseId: number;
  warehouseCode: string;
  warehouseName: string;
  warehouseKind: string;
  status: string;
  shipperAddressMode: string;
  externalShipperCode?: string;
  shipperCompanyName?: string;
  shipperContactName?: string;
  shipperContactPhone?: string;
  shipperContactEmail?: string;
  shipperCountryCode?: string;
  shipperStateProvince?: string;
  shipperCity?: string;
  shipperPostalCode?: string;
  shipperAddressLine1?: string;
  shipperAddressLine2?: string;
  remark?: string;
};

type FulfillmentChannelPairing = API.Integration.LogisticsChannelPairing & {
  masterWarehouseName?: string;
};

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function toTableResult(resp: any) {
  return {
    data: resp.rows || [],
    success: resp.code === 200,
    total: resp.total || 0,
  };
}

function toListResult(resp: any) {
  const data = resp.data || [];
  return {
    data,
    success: resp.code === 200,
    total: data.length,
  };
}

function optionsFromResp(resp: any): OptionItem[] {
  return resp?.data || [];
}

function dictOptions(options: any[]): OptionItem[] {
  return (options || []).map((item) => ({
    ...item,
    label: item.label || item.text,
    value: String(item.value),
    searchText: [item.label, item.text, item.value].filter(Boolean).join(' '),
  }));
}

const fallbackSignatureOptions: OptionItem[] = [
  { label: '直接签名', value: 'DSO' },
  { label: '间接签名', value: 'SSF' },
  { label: '成人签名', value: 'ASS' },
];

const FULFILLMENT_MODE_CARRIER_LABELING = 'CARRIER_LABELING';
const FULFILLMENT_MODE_DIRECT_WAREHOUSE = 'DIRECT_FULFILLMENT_WAREHOUSE';
const UPSTREAM_PAIRING_ROLE_FULFILLMENT = 'FULFILLMENT';

const fallbackFulfillmentModeOptions: OptionItem[] = [
  { label: '物流商打单', value: FULFILLMENT_MODE_CARRIER_LABELING },
  { label: '直推履约仓', value: FULFILLMENT_MODE_DIRECT_WAREHOUSE },
];

const fallbackFulfillmentModeEnum = {
  [FULFILLMENT_MODE_CARRIER_LABELING]: { text: '物流商打单' },
  [FULFILLMENT_MODE_DIRECT_WAREHOUSE]: { text: '直推履约仓' },
};

const signatureServiceOrder = new Map([
  ['DSO', 1],
  ['SSF', 2],
  ['ASS', 3],
]);

function splitSignatureServices(value?: string | string[]) {
  if (Array.isArray(value)) {
    return value;
  }
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function joinSignatureServices(value?: string | string[]) {
  return splitSignatureServices(value).join(',');
}

function normalizeSignatureLabel(option: OptionItem) {
  if (option.value === 'DSO') return '直接签名';
  if (option.value === 'SSF') return '间接签名';
  if (option.value === 'ASS') return '成人签名';
  return option.label;
}

const shipperAddressFieldNames: Array<keyof WarehouseBinding> = [
  'externalShipperCode',
  'shipperCompanyName',
  'shipperContactName',
  'shipperContactPhone',
  'shipperContactEmail',
  'shipperCountryCode',
  'shipperStateProvince',
  'shipperCity',
  'shipperPostalCode',
  'shipperAddressLine1',
  'shipperAddressLine2',
];

function hasShipperAddressConfig(value?: Partial<WarehouseBinding>) {
  return shipperAddressFieldNames.some((fieldName) => String(value?.[fieldName] || '').trim());
}

function isFulfillmentPairingRole(role?: string) {
  return (role || UPSTREAM_PAIRING_ROLE_FULFILLMENT) === UPSTREAM_PAIRING_ROLE_FULFILLMENT;
}

export default function SystemLogisticsChannelPage() {
  const access = useAccess();
  const canAdd = access.hasPerms('logistics:systemChannel:add');
  const canEdit = access.hasPerms('logistics:systemChannel:edit');
  const canStatus = access.hasPerms('logistics:systemChannel:status');
  const canBind = access.hasPerms('logistics:systemChannel:binding');
  const canRule = access.hasPerms('logistics:systemChannel:rule');
  const canListUpstreamConnections = access.hasPerms('integration:upstream:list');
  const canQueryUpstream = access.hasPerms('integration:upstream:query');
  const canPairUpstream = access.hasPerms('integration:upstream:pair');
  const canConfigureFulfillmentChannel = canBind && canListUpstreamConnections && canQueryUpstream && canPairUpstream;

  const actionRef = useRef<ActionType>(undefined);
  const carrierMappingActionRef = useRef<ActionType>(undefined);
  const fulfillmentChannelActionRef = useRef<ActionType>(undefined);
  const warehouseActionRef = useRef<ActionType>(undefined);
  const [channelForm] = Form.useForm<SystemChannel>();
  const [carrierMappingForm] = Form.useForm();
  const [warehouseForm] = Form.useForm<WarehouseBinding>();
  const [fulfillmentChannelForm] = Form.useForm();

  const [finalCarrierOptions, setFinalCarrierOptions] = useState<OptionItem[]>([]);
  const [statusEnum, setStatusEnum] = useState<Record<string, any>>({});
  const [bindingStatusEnum, setBindingStatusEnum] = useState<Record<string, any>>({});
  const [fulfillmentModeEnum, setFulfillmentModeEnum] = useState<Record<string, any>>({});
  const [fulfillmentModeOptions, setFulfillmentModeOptions] = useState<OptionItem[]>([]);
  const [signatureServiceOptions, setSignatureServiceOptions] = useState<OptionItem[]>([]);
  const [carrierAccountOptions, setCarrierAccountOptions] = useState<OptionItem[]>([]);
  const [carrierChannelOptions, setCarrierChannelOptions] = useState<OptionItem[]>([]);
  const [warehouseOptions, setWarehouseOptions] = useState<OptionItem[]>([]);
  const [upstreamConnectionOptions, setUpstreamConnectionOptions] = useState<OptionItem[]>([]);
  const [upstreamLogisticsChannelOptions, setUpstreamLogisticsChannelOptions] = useState<OptionItem[]>([]);

  const [channelEditorMode, setChannelEditorMode] = useState<'create' | 'edit'>('create');
  const [channelModalOpen, setChannelModalOpen] = useState(false);
  const [carrierMappingModalOpen, setCarrierMappingModalOpen] = useState(false);
  const [warehouseModalOpen, setWarehouseModalOpen] = useState(false);
  const [fulfillmentChannelModalOpen, setFulfillmentChannelModalOpen] = useState(false);
  const [currentChannel, setCurrentChannel] = useState<SystemChannel>();
  const [activeEditorTab, setActiveEditorTab] = useState('carrierMappings');
  const [currentWarehouseBinding, setCurrentWarehouseBinding] = useState<WarehouseBinding>();
  const [systemFulfillmentChannelPairings, setSystemFulfillmentChannelPairings] = useState<FulfillmentChannelPairing[]>([]);
  const [orderSettingRemark, setOrderSettingRemark] = useState('');

  useEffect(() => {
    getDictSelectOption('logistics_final_carrier').then((options) => setFinalCarrierOptions(dictOptions(options)));
    getDictSelectOption('logistics_system_channel_fulfillment_mode').then((options) => setFulfillmentModeOptions(dictOptions(options)));
    getDictValueEnum('logistics_system_channel_fulfillment_mode').then(setFulfillmentModeEnum);
    getDictValueEnum('logistics_system_channel_status').then(setStatusEnum);
    getDictValueEnum('logistics_channel_binding_status').then(setBindingStatusEnum);
    getDictSelectOption('logistics_signature_service').then((options) => setSignatureServiceOptions(dictOptions(options)));
    getCarrierAccountOptions().then((resp) => setCarrierAccountOptions(optionsFromResp(resp)));
    getWarehouseOptions().then((resp) => setWarehouseOptions(optionsFromResp(resp)));
  }, []);

  const statusOptions = useMemo(
    () => [
      { label: '启用', value: 'ENABLED' },
      { label: '停用', value: 'DISABLED' },
    ],
    [],
  );

  const signatureCheckboxOptions = useMemo(
    () => (signatureServiceOptions.length ? signatureServiceOptions : fallbackSignatureOptions)
      .map((item) => ({
        ...item,
        label: normalizeSignatureLabel(item),
      }))
      .sort((left, right) => (
        (signatureServiceOrder.get(String(left.value)) || 99)
        - (signatureServiceOrder.get(String(right.value)) || 99)
      )),
    [signatureServiceOptions],
  );

  const signatureLabelMap = useMemo(
    () => new Map(signatureCheckboxOptions.map((item) => [String(item.value), item.label])),
    [signatureCheckboxOptions],
  );

  const fulfillmentModeSelectOptions = useMemo(
    () => fulfillmentModeOptions.length ? fulfillmentModeOptions : fallbackFulfillmentModeOptions,
    [fulfillmentModeOptions],
  );

  const fulfillmentModeValueEnum = useMemo(
    () => Object.keys(fulfillmentModeEnum).length ? fulfillmentModeEnum : fallbackFulfillmentModeEnum,
    [fulfillmentModeEnum],
  );

  const watchedFulfillmentMode = Form.useWatch('fulfillmentMode', channelForm);
  const currentFulfillmentMode = watchedFulfillmentMode
    || currentChannel?.fulfillmentMode
    || FULFILLMENT_MODE_CARRIER_LABELING;
  const shouldShowCarrierMappings = currentFulfillmentMode !== FULFILLMENT_MODE_DIRECT_WAREHOUSE;
  const channelActiveTab = shouldShowCarrierMappings || activeEditorTab !== 'carrierMappings'
    ? activeEditorTab
    : 'warehouses';

  useEffect(() => {
    if (!shouldShowCarrierMappings && activeEditorTab === 'carrierMappings') {
      setActiveEditorTab('fulfillmentChannel');
    }
  }, [activeEditorTab, shouldShowCarrierMappings]);

  const formatSignatureServices = (value?: string | string[]) => {
    const services = splitSignatureServices(value);
    if (!services.length) {
      return '-';
    }
    return services.map((item) => signatureLabelMap.get(item) || item).join('、');
  };

  const reloadDetailTables = () => {
    carrierMappingActionRef.current?.reload();
    fulfillmentChannelActionRef.current?.reload();
    warehouseActionRef.current?.reload();
    actionRef.current?.reload();
  };

  const loadSystemChannelFulfillmentPairings = async (systemChannelCode: string) => {
    if (!canListUpstreamConnections || !canQueryUpstream) {
      return [] as FulfillmentChannelPairing[];
    }
    try {
      const connectionResp = await getUpstreamConnectionList({ pageNum: 1, pageSize: 200 });
      const connections = connectionResp.rows || [];
      const pairingGroups = await Promise.all(
        connections.map(async (connection) => {
          try {
            const pairingResp = await getLogisticsChannelPairings(connection.connectionCode);
            return (pairingResp.data || [])
              .filter((pairing) => (
                pairing.systemChannelCode === systemChannelCode
                && isFulfillmentPairingRole(pairing.pairingRole)
              ))
              .map((pairing) => ({
                ...pairing,
                masterWarehouseName: connection.masterWarehouseName,
              }));
          } catch {
            return [] as FulfillmentChannelPairing[];
          }
        }),
      );
      return pairingGroups.reduce<FulfillmentChannelPairing[]>(
        (result, group) => result.concat(group),
        [],
      );
    } catch {
      return [] as FulfillmentChannelPairing[];
    }
  };

  const loadUpstreamConnectionOptions = async () => {
    if (!canListUpstreamConnections || !canQueryUpstream) {
      setUpstreamConnectionOptions([]);
      return [] as OptionItem[];
    }
    const connectionResp = await getUpstreamConnectionList({ pageNum: 1, pageSize: 200 });
    const connections = connectionResp.rows || [];
    const connectionOptions = connections.map((connection) => ({
      label: connection.masterWarehouseName || connection.connectionCode,
      value: connection.connectionCode,
      searchText: connection.masterWarehouseName || connection.connectionCode,
    }));
    setUpstreamConnectionOptions(connectionOptions);
    return connectionOptions;
  };

  const loadUpstreamLogisticsChannelOptions = async (connectionCode?: string) => {
    if (!connectionCode || !canQueryUpstream) {
      setUpstreamLogisticsChannelOptions([]);
      return [] as OptionItem[];
    }
    const resp = await getLogisticsChannelSyncList(connectionCode, { status: 'ACTIVE' });
    const channelMap = new Map<string, API.Integration.LogisticsChannelSyncItem>();
    (resp.data || [])
      .forEach((item) => {
        if (!channelMap.has(item.channelCode)) {
          channelMap.set(item.channelCode, item);
        }
      });
    const options = Array.from(channelMap.values()).map((item) => ({
      label: `${item.channelCode} / ${item.channelName}`,
      value: item.channelCode,
      searchText: [item.channelCode, item.channelName, item.warehouseCode].filter(Boolean).join(' '),
    }));
    setUpstreamLogisticsChannelOptions(options);
    return options;
  };

  const refreshCurrentChannel = async (systemChannelCode: string) => {
    const resp = await getSystemChannel(systemChannelCode);
    if (resp.code === 200) {
      setCurrentChannel(resp.data);
      return resp.data;
    }
    return undefined;
  };

  const openCreateChannel = () => {
    setChannelEditorMode('create');
    setCurrentChannel(undefined);
    setActiveEditorTab('carrierMappings');
    setOrderSettingRemark('');
    setChannelModalOpen(true);
    setTimeout(() => {
      channelForm.resetFields();
      channelForm.setFieldsValue({
        fulfillmentMode: FULFILLMENT_MODE_CARRIER_LABELING,
        signatureServices: [],
      });
    });
  };

  const openEditChannel = async (record: SystemChannel, tabKey = 'carrierMappings') => {
    const nextFulfillmentMode = record.fulfillmentMode || FULFILLMENT_MODE_CARRIER_LABELING;
    setChannelEditorMode('edit');
    setCurrentChannel(record);
    setActiveEditorTab(
      nextFulfillmentMode === FULFILLMENT_MODE_DIRECT_WAREHOUSE && tabKey === 'carrierMappings'
        ? 'fulfillmentChannel'
        : tabKey,
    );
    setChannelModalOpen(true);
    setTimeout(() => {
      channelForm.resetFields();
      channelForm.setFieldsValue({
        ...record,
        fulfillmentMode: nextFulfillmentMode,
        signatureServices: splitSignatureServices(record.signatureServices),
      });
    });
    await loadOrderSetting(record.systemChannelCode);
  };

  const saveChannel = async (values: SystemChannel, keepOpenAfterCreate = true) => {
    const { status: _status, ...payload } = {
      ...values,
      fulfillmentMode: values.fulfillmentMode || FULFILLMENT_MODE_CARRIER_LABELING,
      signatureServices: joinSignatureServices(values.signatureServices),
    };
    void _status;
    const resp = currentChannel?.systemChannelCode
      ? await updateSystemChannel(currentChannel.systemChannelCode, payload)
      : await addSystemChannel(payload);
    const ok = resultOk(resp, currentChannel?.systemChannelCode ? '系统渠道已更新' : '系统渠道已新增');
    if (!ok) {
      return false;
    }
    actionRef.current?.reload();
    if (!currentChannel?.systemChannelCode) {
      const created = { ...payload, status: 'ENABLED' };
      setCurrentChannel(created);
      channelForm.setFieldsValue({
        ...created,
        signatureServices: splitSignatureServices(created.signatureServices),
      });
      await loadOrderSetting(created.systemChannelCode);
      if (keepOpenAfterCreate) {
        message.info(
          created.fulfillmentMode === FULFILLMENT_MODE_DIRECT_WAREHOUSE
            ? '基础信息已保存，可以继续配置下方主仓渠道映射、仓库和下单规则'
            : '基础信息已保存，可以继续配置下方物流商映射、主仓渠道映射、仓库和下单规则',
        );
        return false;
      }
    }
    return true;
  };

  const submitChannelEditor = async () => {
    const values = await channelForm.validateFields();
    const shouldClose = await saveChannel(values as SystemChannel, true);
    if (shouldClose) {
      setChannelModalOpen(false);
    }
  };

  const toggleStatus = (record: SystemChannel) => {
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    Modal.confirm({
      title: nextStatus === 'ENABLED' ? '启用系统渠道' : '停用系统渠道',
      content: record.systemChannelName,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (resultOk(await updateSystemChannelStatus(record.systemChannelCode, nextStatus), '状态已更新')) {
          actionRef.current?.reload();
          if (currentChannel?.systemChannelCode === record.systemChannelCode) {
            await refreshCurrentChannel(record.systemChannelCode);
          }
        }
      },
    });
  };

  const loadCarrierChannels = async (carrierAccountId?: number) => {
    if (!carrierAccountId) {
      setCarrierChannelOptions([]);
      return;
    }
    const resp = await getCarrierChannelOptions(carrierAccountId);
    setCarrierChannelOptions(optionsFromResp(resp));
  };

  const openCarrierMappingModal = () => {
    if (!shouldShowCarrierMappings) {
      message.warning('直推履约仓渠道不需要维护物流商映射');
      return;
    }
    carrierMappingForm.resetFields();
    setCarrierChannelOptions([]);
    setCarrierMappingModalOpen(true);
  };

  const saveCarrierMapping = async (values: Record<string, any>) => {
    if (!currentChannel?.systemChannelCode) return false;
    const ok = resultOk(
      await addCarrierMapping(currentChannel.systemChannelCode, values),
      '物流商映射已保存',
    );
    if (ok) {
      reloadDetailTables();
      return true;
    }
    return false;
  };

  const removeCarrierMapping = (record: CarrierMapping) => {
    if (!currentChannel?.systemChannelCode) return;
    Modal.confirm({
      title: '删除物流商映射',
      content: `${record.carrierName} / ${record.externalChannelNameSnapshot}`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (
          resultOk(
            await deleteCarrierMapping(currentChannel.systemChannelCode, record.mappingId),
            '物流商映射已删除',
          )
        ) {
          reloadDetailTables();
        }
      },
    });
  };

  const openWarehouseModal = (record?: WarehouseBinding) => {
    setCurrentWarehouseBinding(record);
    warehouseForm.resetFields();
    warehouseForm.setFieldsValue(record || {
      status: 'ENABLED',
      shipperAddressMode: 'WAREHOUSE',
    } as WarehouseBinding);
    setWarehouseModalOpen(true);
  };

  const saveWarehouseBinding = async (values: WarehouseBinding) => {
    if (!currentChannel?.systemChannelCode) return false;
    const payload = {
      ...(currentWarehouseBinding || {}),
      ...values,
    };
    payload.shipperAddressMode = hasShipperAddressConfig(payload) ? 'OVERRIDE' : 'WAREHOUSE';
    const resp = currentWarehouseBinding?.bindingId
      ? await updateWarehouseBinding(currentChannel.systemChannelCode, currentWarehouseBinding.bindingId, payload)
      : await addWarehouseBinding(currentChannel.systemChannelCode, payload);
    const ok = resultOk(resp, currentWarehouseBinding?.bindingId ? '发货地址已保存' : '仓库已绑定');
    if (ok) {
      reloadDetailTables();
      return true;
    }
    return false;
  };

  const removeWarehouseBinding = (record: WarehouseBinding) => {
    if (!currentChannel?.systemChannelCode) return;
    Modal.confirm({
      title: '删除仓库绑定',
      content: record.warehouseName,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (
          resultOk(
            await deleteWarehouseBinding(currentChannel.systemChannelCode, record.bindingId),
            '仓库绑定已删除',
          )
        ) {
          reloadDetailTables();
        }
      },
    });
  };

  const openFulfillmentChannelModal = async () => {
    if (!currentChannel?.systemChannelCode) {
      return;
    }
    if (!canConfigureFulfillmentChannel) {
      message.warning('需要系统渠道绑定权限和上游系统查询/配对权限');
      return;
    }
    const pairings = await loadSystemChannelFulfillmentPairings(currentChannel.systemChannelCode);
    const existingPairing = pairings.find((pairing) => isFulfillmentPairingRole(pairing.pairingRole));
    setSystemFulfillmentChannelPairings(pairings);
    setUpstreamLogisticsChannelOptions([]);
    fulfillmentChannelForm.resetFields();
    fulfillmentChannelForm.setFieldsValue({
      connectionCode: existingPairing?.connectionCode,
      upstreamChannelCode: existingPairing?.upstreamChannelCode,
      remark: existingPairing?.remark,
    });
    setFulfillmentChannelModalOpen(true);
    try {
      const connectionOptions = await loadUpstreamConnectionOptions();
      const connectionCode = existingPairing?.connectionCode
        || (connectionOptions.length === 1 ? String(connectionOptions[0].value) : undefined);
      if (connectionCode) {
        fulfillmentChannelForm.setFieldValue('connectionCode', connectionCode);
        const channelOptions = await loadUpstreamLogisticsChannelOptions(connectionCode);
        if (
          existingPairing?.upstreamChannelCode
          && !channelOptions.some((option) => option.value === existingPairing.upstreamChannelCode)
        ) {
          setUpstreamLogisticsChannelOptions([
            ...channelOptions,
            {
              label: `${existingPairing.upstreamChannelCode} / ${existingPairing.upstreamChannelName}`,
              value: existingPairing.upstreamChannelCode,
              searchText: [existingPairing.upstreamChannelCode, existingPairing.upstreamChannelName].filter(Boolean).join(' '),
            },
          ]);
        }
      }
    } catch {
      message.error('上游系统渠道选项加载失败');
    }
  };

  const saveFulfillmentChannelPairing = async (values: Record<string, any>) => {
    if (!currentChannel?.systemChannelCode) {
      return false;
    }
    const existingPairing = systemFulfillmentChannelPairings
      .find((pairing) => isFulfillmentPairingRole(pairing.pairingRole));
    if (
      existingPairing
      && existingPairing.connectionCode === values.connectionCode
      && existingPairing.upstreamChannelCode === values.upstreamChannelCode
    ) {
      return true;
    }
    if (existingPairing?.logisticsChannelPairingId) {
      const deleteResp = await deleteLogisticsChannelPairing(
        existingPairing.connectionCode,
        existingPairing.logisticsChannelPairingId,
      );
      if (deleteResp.code !== 200) {
        message.error(deleteResp.msg || '原有主仓渠道配对解除失败');
        return false;
      }
    }
    const ok = resultOk(
      await addLogisticsChannelPairing(values.connectionCode, {
        upstreamChannelCode: values.upstreamChannelCode,
        systemChannelCode: currentChannel.systemChannelCode,
        systemChannelName: currentChannel.systemChannelName,
        pairingRole: UPSTREAM_PAIRING_ROLE_FULFILLMENT,
        remark: values.remark,
      }),
      '主仓渠道已配对',
    );
    if (ok) {
      reloadDetailTables();
      fulfillmentChannelActionRef.current?.reload();
      setFulfillmentChannelModalOpen(false);
      return true;
    }
    return false;
  };

  const removeFulfillmentChannelPairing = (pairing: FulfillmentChannelPairing) => {
    Modal.confirm({
      title: '解除主仓渠道配对',
      content: `${pairing.masterWarehouseName || pairing.connectionCode} / ${pairing.upstreamChannelName}`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (
          resultOk(
            await deleteLogisticsChannelPairing(pairing.connectionCode, pairing.logisticsChannelPairingId),
            '主仓渠道配对已解除',
          )
        ) {
          reloadDetailTables();
        }
      },
    });
  };

  const loadOrderSetting = async (systemChannelCode: string) => {
    const resp = await getOrderSetting(systemChannelCode);
    setOrderSettingRemark(resp.data?.remark || '');
  };

  const saveCurrentOrderSetting = async (values: Record<string, any>) => {
    if (!currentChannel?.systemChannelCode) return false;
    const ok = resultOk(
      await saveOrderSetting(currentChannel.systemChannelCode, values),
      '下单规则已保存',
    );
    if (ok) {
      actionRef.current?.reload();
      await refreshCurrentChannel(currentChannel.systemChannelCode);
    }
    return ok;
  };

  const channelColumns: ProColumns<SystemChannel>[] = [
    { title: '系统渠道代码', dataIndex: 'systemChannelCode', width: 170, fixed: 'left' },
    { title: '系统渠道名称', dataIndex: 'systemChannelName', width: 220, fixed: 'left' },
    {
      title: '渠道履约模式',
      dataIndex: 'fulfillmentMode',
      valueType: 'select',
      valueEnum: fulfillmentModeValueEnum,
      width: 150,
    },
    {
      title: '关联物流商账号',
      dataIndex: 'carrierAccountSummary',
      search: false,
      ellipsis: true,
      width: 260,
      render: (_, record) => record.carrierAccountSummary || '-',
    },
    {
      title: '承运商',
      dataIndex: 'standardCarrierCode',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: finalCarrierOptions,
      },
      width: 150,
    },
    {
      title: '签名服务',
      dataIndex: 'signatureServices',
      search: false,
      width: 190,
      render: (_, record) => formatSignatureServices(record.signatureServices),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusEnum,
      width: 120,
      render: (_, record) => (
        <Switch
          checked={record.status === 'ENABLED'}
          checkedChildren="启用"
          unCheckedChildren="停用"
          disabled={!canStatus}
          onClick={() => toggleStatus(record)}
        />
      ),
    },
    {
      title: '绑定仓库数量',
      dataIndex: 'warehouseCount',
      search: false,
      width: 120,
      render: (_, record) => record.warehouseCount || 0,
    },
    { title: '最后更新人', dataIndex: 'updateBy', search: false, width: 130 },
    { title: '最后更新时间', dataIndex: 'updateTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      fixed: 'right',
      width: 100,
      render: (_, record) => [
        <Button key="edit" type="link" size="small" hidden={!canEdit} onClick={() => openEditChannel(record, 'carrierMappings')}>
          编辑
        </Button>,
      ],
    },
  ];

  const carrierMappingColumns: ProColumns<CarrierMapping>[] = [
    { title: '物流商账号', dataIndex: 'carrierName', width: 220 },
    { title: '物流商系统', dataIndex: 'providerKind', width: 120 },
    { title: '物流商渠道名称', dataIndex: 'externalChannelNameSnapshot', width: 240 },
    { title: '物流商渠道代码', dataIndex: 'externalChannelCode', width: 180 },
    { title: '承运商', dataIndex: 'standardCarrierCode', width: 140 },
    { title: '状态', dataIndex: 'status', valueEnum: bindingStatusEnum, width: 100 },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 90,
      render: (_, record) => [
        <Button key="delete" type="link" size="small" danger hidden={!canBind} onClick={() => removeCarrierMapping(record)}>
          删除
        </Button>,
      ],
    },
  ];

  const fulfillmentChannelColumns: ProColumns<FulfillmentChannelPairing>[] = [
    {
      title: '上游系统',
      dataIndex: 'masterWarehouseName',
      width: 220,
      render: (_, record) => record.masterWarehouseName || record.connectionCode,
    },
    { title: '主仓渠道代码', dataIndex: 'upstreamChannelCode', width: 220 },
    { title: '主仓渠道名称', dataIndex: 'upstreamChannelName', width: 260, ellipsis: true },
    { title: '配对用途', dataIndex: 'pairingRole', width: 120, render: () => '履约' },
    { title: '状态', dataIndex: 'status', valueEnum: bindingStatusEnum, width: 100 },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 130,
      render: (_, record) => [
        <Button key="edit" type="link" size="small" hidden={!canConfigureFulfillmentChannel} onClick={openFulfillmentChannelModal}>
          配置
        </Button>,
        <Button key="delete" type="link" size="small" danger hidden={!canConfigureFulfillmentChannel} onClick={() => removeFulfillmentChannelPairing(record)}>
          解除
        </Button>,
      ],
    },
  ];

  const warehouseColumns: ProColumns<WarehouseBinding>[] = [
    { title: '仓库代码', dataIndex: 'warehouseCode', width: 150 },
    { title: '仓库名称', dataIndex: 'warehouseName', width: 220 },
    { title: '仓库类型', dataIndex: 'warehouseKind', width: 120 },
    { title: '状态', dataIndex: 'status', valueEnum: bindingStatusEnum, width: 100 },
    {
      title: '发货地址配置',
      dataIndex: 'shipperAddressMode',
      width: 160,
      render: (_, record) => (
        hasShipperAddressConfig(record)
          ? <Tag color="orange">已配置</Tag>
          : <Tag>使用仓库地址</Tag>
      ),
    },
    { title: '外部发货地址编码', dataIndex: 'externalShipperCode', width: 180, ellipsis: true },
    {
      title: '覆写地址摘要',
      dataIndex: 'shipperAddressLine1',
      width: 300,
      ellipsis: true,
      render: (_, record) =>
        [record.shipperContactName, record.shipperCity, record.shipperStateProvince, record.shipperAddressLine1]
          .filter(Boolean)
          .join(' / ') || '-',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 160,
      render: (_, record) => [
        <Button key="shipper" type="link" size="small" hidden={!canBind} onClick={() => openWarehouseModal(record)}>
          配置发货地址
        </Button>,
        <Button key="delete" type="link" size="small" danger hidden={!canBind} onClick={() => removeWarehouseBinding(record)}>
          删除
        </Button>,
      ],
    },
  ];

  const isCreateBaseStep = channelEditorMode === 'create' && !currentChannel?.systemChannelCode;
  const shouldShowChannelDetails = channelEditorMode === 'edit' || !!currentChannel?.systemChannelCode;
  const channelModalTitle = channelEditorMode === 'edit'
    ? '编辑系统渠道'
    : (isCreateBaseStep ? '新增系统渠道' : '配置系统渠道');
  const channelSubmitText = isCreateBaseStep
    ? '保存并下一步'
    : (channelEditorMode === 'edit' ? '确定' : '完成');

  return (
    <PageContainer title={false}>
      <ProTable<SystemChannel>
        actionRef={actionRef}
        rowKey="systemChannelCode"
        columns={channelColumns}
        request={(params) => getSystemChannelList(params).then(toTableResult)}
        pagination={getProTablePagination(20)}
        scroll={getProTableScroll(1570)}
        search={getPersistedProTableSearch({ labelWidth: 90, fieldCount: 4 }, 'system-logistics-channel')}
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} hidden={!canAdd} onClick={openCreateChannel}>
            新增渠道
          </Button>,
        ]}
      />

      <Modal
        title={channelModalTitle}
        open={channelModalOpen}
        width="94vw"
        style={{ maxWidth: 1680, top: 24 }}
        destroyOnHidden
        onCancel={() => setChannelModalOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setChannelModalOpen(false)}>
            取消
          </Button>,
          <Button key="ok" type="primary" onClick={submitChannelEditor}>
            {channelSubmitText}
          </Button>,
        ]}
        styles={{
          body: {
            maxHeight: 'calc(100vh - 168px)',
            overflowY: 'auto',
            overflowX: 'hidden',
            paddingTop: 8,
          },
        }}
      >
        <div style={{ padding: '8px 0 14px' }}>
          <div style={{ borderLeft: '3px solid #1677ff', paddingLeft: 10, fontWeight: 600, marginBottom: 16 }}>
            基础信息
          </div>
          <ProForm<SystemChannel> form={channelForm} layout="vertical" submitter={false}>
            <Row gutter={24} style={{ marginLeft: 0, marginRight: 0 }}>
              <Col xs={24} lg={8}>
                <ProFormSelect
                  name="fulfillmentMode"
                  label="渠道履约模式"
                  options={fulfillmentModeSelectOptions}
                  fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                  rules={[{ required: true, message: '请选择渠道履约模式' }]}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ProFormText
                  name="systemChannelCode"
                  label="系统渠道代码"
                  disabled={!!currentChannel?.systemChannelCode}
                  fieldProps={{ style: { width: '100%' } }}
                  rules={[{ required: true, message: '请输入系统渠道代码' }]}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ProFormText
                  name="systemChannelName"
                  label="系统渠道名称"
                  fieldProps={{ style: { width: '100%' } }}
                  rules={[{ required: true, message: '请输入系统渠道名称' }]}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ProFormSelect
                  name="standardCarrierCode"
                  label="承运商"
                  options={finalCarrierOptions}
                  fieldProps={{ ...SEARCHABLE_SELECT_PROPS, style: { width: '100%' } }}
                  rules={[{ required: true, message: '请选择承运商' }]}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ProFormCheckbox.Group
                  name="signatureServices"
                  label="签名服务"
                  options={signatureCheckboxOptions}
                />
              </Col>
              <Col span={24}>
                <ProFormTextArea name="remark" label="备注" fieldProps={{ rows: 2 }} />
              </Col>
            </Row>
          </ProForm>
        </div>

        {shouldShowChannelDetails ? (
          <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8 }}>
            <Tabs
              activeKey={channelActiveTab}
              onChange={setActiveEditorTab}
              items={[
              ...(shouldShowCarrierMappings ? [
              {
                key: 'carrierMappings',
                label: '物流商映射',
                children: (
                  <ProTable<CarrierMapping>
                    actionRef={carrierMappingActionRef}
                    rowKey="mappingId"
                    columns={carrierMappingColumns}
                    request={() => currentChannel?.systemChannelCode
                      ? getCarrierMappings(currentChannel.systemChannelCode).then(toListResult)
                      : Promise.resolve({ data: [], success: true, total: 0 })}
                    search={false}
                    pagination={getProTablePagination(10)}
                    scroll={getProTableScroll(1250, { y: 320 })}
                    toolBarRender={() => [
                      <Button
                        key="add"
                        icon={<LinkOutlined />}
                        disabled={!currentChannel?.systemChannelCode}
                        hidden={!canBind}
                        onClick={openCarrierMappingModal}
                      >
                        新增映射
                      </Button>,
                    ]}
                  />
                ),
              },
              ] : []),
              {
                key: 'fulfillmentChannel',
                label: '主仓渠道映射',
                children: (
                  <ProTable<FulfillmentChannelPairing>
                    actionRef={fulfillmentChannelActionRef}
                    rowKey="logisticsChannelPairingId"
                    columns={fulfillmentChannelColumns}
                    request={async () => {
                      if (!currentChannel?.systemChannelCode) {
                        return { data: [], success: true, total: 0 };
                      }
                      const data = await loadSystemChannelFulfillmentPairings(currentChannel.systemChannelCode);
                      setSystemFulfillmentChannelPairings(data);
                      return {
                        data,
                        success: true,
                        total: data.length,
                      };
                    }}
                    search={false}
                    pagination={false}
                    scroll={getProTableScroll(1050, { y: 260 })}
                    toolBarRender={() => [
                      <Button
                        key="configure"
                        icon={<LinkOutlined />}
                        disabled={!currentChannel?.systemChannelCode}
                        hidden={!canConfigureFulfillmentChannel}
                        onClick={openFulfillmentChannelModal}
                      >
                        配置主仓渠道
                      </Button>,
                    ]}
                  />
                ),
              },
              {
                key: 'warehouses',
                label: '仓库与发货地址',
                children: (
                  <ProTable<WarehouseBinding>
                    actionRef={warehouseActionRef}
                    rowKey="bindingId"
                    columns={warehouseColumns}
                    request={async () => {
                      if (!currentChannel?.systemChannelCode) {
                        return { data: [], success: true, total: 0 };
                      }
                      const warehouseResp = await getWarehouseBindings(currentChannel.systemChannelCode);
                      const data = warehouseResp.data || [];
                      return {
                        data,
                        success: warehouseResp.code === 200,
                        total: data.length,
                      };
                    }}
                    search={false}
                    pagination={getProTablePagination(10)}
                    scroll={getProTableScroll(1710, { y: 320 })}
                    toolBarRender={() => [
                      <Button
                        key="add"
                        icon={<PlusOutlined />}
                        disabled={!currentChannel?.systemChannelCode}
                        hidden={!canBind}
                        onClick={() => openWarehouseModal()}
                      >
                        添加仓库
                      </Button>,
                    ]}
                  />
                ),
              },
              {
                key: 'orderSetting',
                label: '下单规则',
                children: (
                  <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                    <Alert
                      showIcon
                      type="info"
                      title="下单规则先做预留"
                      description="包裹长宽高、最长边、次长边、体积重、实重、计费重、危险品、地址类型和组合条件会单独设计规则模型，本版暂不在这里展开。"
                    />
                    <ProForm
                      key={`${currentChannel?.systemChannelCode || 'new'}-${orderSettingRemark}`}
                      layout="vertical"
                      initialValues={{ remark: orderSettingRemark }}
                      disabled={!canRule || !currentChannel?.systemChannelCode}
                      onFinish={saveCurrentOrderSetting}
                      submitter={{
                        searchConfig: { submitText: '保存占位备注' },
                        resetButtonProps: { hidden: true },
                        submitButtonProps: { hidden: !canRule || !currentChannel?.systemChannelCode },
                      }}
                    >
                      <ProFormTextArea
                        name="remark"
                        label="规则备注"
                        placeholder="可先记录该渠道后续需要补充的包裹规则口径"
                        fieldProps={{ rows: 4 }}
                      />
                    </ProForm>
                  </Space>
                ),
              },
              ]}
            />
          </div>
        ) : null}
      </Modal>

      <ModalForm
        title="物流商映射"
        open={carrierMappingModalOpen}
        form={carrierMappingForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setCarrierMappingModalOpen(false) }}
        onOpenChange={setCarrierMappingModalOpen}
        onFinish={saveCarrierMapping}
      >
        <ProFormSelect
          name="carrierAccountId"
          label="物流商账号"
          options={carrierAccountOptions}
          fieldProps={{
            ...SEARCHABLE_SELECT_PROPS,
            onChange: (value) => loadCarrierChannels(Number(value)),
          }}
          rules={[{ required: true, message: '请选择物流商账号' }]}
        />
        <ProFormSelect name="externalChannelCode" label="物流商渠道" options={carrierChannelOptions} fieldProps={SEARCHABLE_SELECT_PROPS} rules={[{ required: true, message: '请选择物流商渠道' }]} />
        <ProFormSelect name="standardCarrierCode" label="承运商" options={finalCarrierOptions} fieldProps={SEARCHABLE_SELECT_PROPS} rules={[{ required: true, message: '请选择承运商' }]} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <ModalForm<WarehouseBinding>
        title={currentWarehouseBinding?.bindingId ? '配置发货地址' : '添加仓库'}
        open={warehouseModalOpen}
        form={warehouseForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setWarehouseModalOpen(false) }}
        onOpenChange={setWarehouseModalOpen}
        onFinish={saveWarehouseBinding}
      >
        {currentWarehouseBinding?.bindingId ? (
          <>
            <ProForm.Group>
              <ProFormText name="warehouseCode" label="仓库代码" width="md" disabled />
              <ProFormText name="warehouseName" label="仓库名称" width="md" disabled />
            </ProForm.Group>
            <ProFormText name="externalShipperCode" label="外部物流商发货地址编码" />
            <ProForm.Group>
              <ProFormText name="shipperCompanyName" label="发货公司" width="md" />
              <ProFormText name="shipperContactName" label="发货联系人" width="md" />
              <ProFormText name="shipperContactPhone" label="发货电话" width="md" />
              <ProFormText name="shipperContactEmail" label="发货邮箱" width="md" />
            </ProForm.Group>
            <ProForm.Group>
              <ProFormText name="shipperCountryCode" label="发货国家/地区" width="sm" />
              <ProFormText name="shipperStateProvince" label="发货州/省" width="sm" />
              <ProFormText name="shipperCity" label="发货城市" width="sm" />
              <ProFormText name="shipperPostalCode" label="发货邮编" width="sm" />
            </ProForm.Group>
            <ProFormText name="shipperAddressLine1" label="发货地址1" />
            <ProFormText name="shipperAddressLine2" label="发货地址2" />
          </>
        ) : (
          <>
            <ProFormSelect
              name="warehouseId"
              label="仓库"
              options={warehouseOptions}
              fieldProps={SEARCHABLE_SELECT_PROPS}
              rules={[{ required: true, message: '请选择仓库' }]}
            />
            <ProFormSelect name="status" label="状态" options={statusOptions} fieldProps={SEARCHABLE_SELECT_PROPS} />
          </>
        )}
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <ModalForm
        title="配置主仓渠道"
        open={fulfillmentChannelModalOpen}
        form={fulfillmentChannelForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setFulfillmentChannelModalOpen(false) }}
        onOpenChange={setFulfillmentChannelModalOpen}
        onFinish={saveFulfillmentChannelPairing}
      >
        <Alert
          showIcon
          type="info"
          message={`当前系统渠道：${currentChannel?.systemChannelCode || '-'} / ${currentChannel?.systemChannelName || '-'}`}
          description="系统渠道只配对一个主仓渠道；同一个主仓渠道可以被多个系统渠道复用，不按仓库过滤。"
        />
        <ProFormSelect
          name="connectionCode"
          label="上游系统"
          options={upstreamConnectionOptions}
          fieldProps={{
            ...SEARCHABLE_SELECT_PROPS,
            onChange: async (value) => {
              fulfillmentChannelForm.setFieldsValue({
                upstreamChannelCode: undefined,
              });
              setUpstreamLogisticsChannelOptions([]);
              await loadUpstreamLogisticsChannelOptions(String(value));
            },
          }}
          rules={[{ required: true, message: '请选择上游系统' }]}
        />
        <ProFormSelect
          name="upstreamChannelCode"
          label="主仓渠道"
          options={upstreamLogisticsChannelOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择领星物流渠道同步清单中的渠道' }]}
        />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </PageContainer>
  );
}
