import {
  LinkOutlined,
  PlusOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProForm,
  ProFormCheckbox,
  type ProColumns,
  ProFormRadio,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
  type ActionType,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Col, Form, Input, Modal, Radio, Row, Space, Switch, Tabs, Tag, message } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import {
  addCustomerChannel,
  addSystemMapping,
  deleteSystemMapping,
  getBuyerOptions,
  getBuyerScope,
  getCustomerChannel,
  getCustomerChannelList,
  getSystemChannelOptions,
  getSystemMappings,
  saveBuyerScope,
  updateCustomerChannel,
  updateCustomerChannelStatus,
  updateSystemMapping,
} from '@/services/logistics/customerChannel';
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
  code?: string;
  name?: string;
  shortName?: string;
};

type CustomerChannel = {
  customerChannelCode: string;
  customerChannelName: string;
  channelType: string;
  standardCarrierCode: string;
  signatureServices?: string | string[];
  labelUploadRequired: string;
  platformLabelFetch: string;
  customerLabelUploadSupported: string;
  buyerScopeMode: string;
  status: string;
  systemMappingCount?: number;
  buyerScopeCount?: number;
  systemChannelSummary?: string;
  buyerScopeSummary?: string;
  updateBy?: string;
  updateTime?: string;
  remark?: string;
};

type SystemMapping = {
  mappingId: number;
  customerChannelCode: string;
  systemChannelCode: string;
  systemChannelNameSnapshot: string;
  standardCarrierCodeSnapshot: string;
  signatureServicesSnapshot?: string;
  status: string;
  createTime?: string;
  remark?: string;
};

type BuyerScopeRow = {
  scopeId: number;
  customerChannelCode: string;
  buyerId: number;
  buyerCodeSnapshot: string;
  buyerNameSnapshot: string;
  buyerShortNameSnapshot?: string;
  createTime?: string;
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

function hasText(value?: string) {
  return String(value || '').trim().length > 0;
}

export default function CustomerLogisticsChannelPage() {
  const access = useAccess();
  const canAdd = access.hasPerms('logistics:customerChannel:add');
  const canEdit = access.hasPerms('logistics:customerChannel:edit');
  const canStatus = access.hasPerms('logistics:customerChannel:status');
  const canBind = access.hasPerms('logistics:customerChannel:binding');
  const canBuyer = access.hasPerms('logistics:customerChannel:buyer');

  const actionRef = useRef<ActionType>(undefined);
  const systemMappingActionRef = useRef<ActionType>(undefined);
  const [channelForm] = Form.useForm<CustomerChannel>();
  const [systemMappingForm] = Form.useForm<SystemMapping>();

  const [channelModalOpen, setChannelModalOpen] = useState(false);
  const [systemMappingModalOpen, setSystemMappingModalOpen] = useState(false);
  const [buyerScopeModalOpen, setBuyerScopeModalOpen] = useState(false);
  const [channelEditorMode, setChannelEditorMode] = useState<'create' | 'edit'>('create');
  const [currentChannel, setCurrentChannel] = useState<CustomerChannel>();
  const [currentSystemMapping, setCurrentSystemMapping] = useState<SystemMapping>();
  const [activeEditorTab, setActiveEditorTab] = useState('systemMappings');

  const [finalCarrierOptions, setFinalCarrierOptions] = useState<OptionItem[]>([]);
  const [channelTypeOptions, setChannelTypeOptions] = useState<OptionItem[]>([]);
  const [statusEnum, setStatusEnum] = useState<Record<string, any>>({});
  const [bindingStatusEnum, setBindingStatusEnum] = useState<Record<string, any>>({});
  const [labelUploadEnum, setLabelUploadEnum] = useState<Record<string, any>>({});
  const [platformLabelFetchEnum, setPlatformLabelFetchEnum] = useState<Record<string, any>>({});
  const [customerLabelUploadEnum, setCustomerLabelUploadEnum] = useState<Record<string, any>>({});
  const [scopeModeEnum, setScopeModeEnum] = useState<Record<string, any>>({});
  const [signatureServiceOptions, setSignatureServiceOptions] = useState<OptionItem[]>([]);
  const [systemChannelOptions, setSystemChannelOptions] = useState<OptionItem[]>([]);
  const [buyerOptions, setBuyerOptions] = useState<OptionItem[]>([]);
  const [buyerScopeRows, setBuyerScopeRows] = useState<BuyerScopeRow[]>([]);
  const [buyerScopeModeDraft, setBuyerScopeModeDraft] = useState<'INCLUDE' | 'EXCLUDE'>('INCLUDE');
  const [buyerKeyword, setBuyerKeyword] = useState('');
  const [selectedBuyerKeys, setSelectedBuyerKeys] = useState<(string | number)[]>([]);

  const channelType = Form.useWatch('channelType', channelForm);
  const labelUploadRequired = Form.useWatch('labelUploadRequired', channelForm);

  useEffect(() => {
    getDictSelectOption('logistics_final_carrier').then((options) => setFinalCarrierOptions(dictOptions(options)));
    getDictSelectOption('logistics_customer_channel_type').then((options) => setChannelTypeOptions(dictOptions(options)));
    getDictValueEnum('logistics_customer_channel_status').then(setStatusEnum);
    getDictValueEnum('logistics_customer_channel_binding_status').then(setBindingStatusEnum);
    getDictValueEnum('logistics_label_upload_required').then(setLabelUploadEnum);
    getDictValueEnum('logistics_platform_label_fetch').then(setPlatformLabelFetchEnum);
    getDictValueEnum('logistics_customer_label_upload_support').then(setCustomerLabelUploadEnum);
    getDictValueEnum('logistics_customer_channel_scope_mode').then(setScopeModeEnum);
    getDictSelectOption('logistics_signature_service').then((options) => setSignatureServiceOptions(dictOptions(options)));
    getSystemChannelOptions().then((resp) => setSystemChannelOptions(optionsFromResp(resp)));
    getBuyerOptions().then((resp) => setBuyerOptions(optionsFromResp(resp)));
  }, []);

  const statusOptions = useMemo(
    () => [
      { label: '启用', value: 'ENABLED' },
      { label: '停用', value: 'DISABLED' },
    ],
    [],
  );

  const channelTypeFallbackOptions = useMemo(
    () => (channelTypeOptions.length ? channelTypeOptions : [
      { label: '仓库面单', value: 'WAREHOUSE_LABEL' },
      { label: '第三方面单', value: 'THIRD_PARTY_LABEL' },
    ]),
    [channelTypeOptions],
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

  const filteredBuyerOptions = useMemo(() => {
    const keyword = buyerKeyword.trim().toLowerCase();
    if (!keyword) {
      return buyerOptions;
    }
    return buyerOptions.filter((item) => (
      [item.code, item.name, item.shortName, item.label, item.value, item.searchText]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(keyword)
    ));
  }, [buyerKeyword, buyerOptions]);

  const formatSignatureServices = (value?: string | string[]) => {
    const services = splitSignatureServices(value);
    if (!services.length) {
      return '-';
    }
    return services.map((item) => signatureLabelMap.get(item) || item).join('、');
  };

  const formatValueEnum = (valueEnum: Record<string, any>, value?: string) => {
    if (!hasText(value)) return '-';
    return valueEnum[value || '']?.text || valueEnum[value || '']?.label || value;
  };

  const refreshCurrentChannel = async (customerChannelCode: string) => {
    const resp = await getCustomerChannel(customerChannelCode);
    if (resp.code === 200) {
      setCurrentChannel(resp.data);
      return resp.data;
    }
    return undefined;
  };

  const reloadDetailTables = () => {
    systemMappingActionRef.current?.reload();
    actionRef.current?.reload();
  };

  const loadBuyerScope = async (customerChannelCode: string) => {
    const resp = await getBuyerScope(customerChannelCode);
    if (resp.code === 200) {
      setBuyerScopeRows(resp.data || []);
    }
  };

  const openCreateChannel = () => {
    setChannelEditorMode('create');
    setCurrentChannel(undefined);
    setActiveEditorTab('systemMappings');
    setBuyerScopeRows([]);
    setChannelModalOpen(true);
    setTimeout(() => {
      channelForm.resetFields();
      channelForm.setFieldsValue({
        channelType: 'WAREHOUSE_LABEL',
        labelUploadRequired: 'NOT_REQUIRED',
        platformLabelFetch: 'NOT_FETCH',
        customerLabelUploadSupported: 'UNSUPPORTED',
        signatureServices: [],
      });
    });
  };

  const openEditChannel = async (record: CustomerChannel, tabKey = 'systemMappings') => {
    setChannelEditorMode('edit');
    setCurrentChannel(record);
    setActiveEditorTab(tabKey);
    setChannelModalOpen(true);
    setTimeout(() => {
      channelForm.resetFields();
      channelForm.setFieldsValue({
        ...record,
        signatureServices: splitSignatureServices(record.signatureServices),
      });
    });
    await loadBuyerScope(record.customerChannelCode);
  };

  const normalizeChannelPayload = (values: CustomerChannel) => {
    const { status: _status, ...payload } = {
      ...values,
      signatureServices: joinSignatureServices(values.signatureServices),
    };
    void _status;
    if (payload.channelType === 'WAREHOUSE_LABEL') {
      payload.labelUploadRequired = 'NOT_REQUIRED';
      payload.platformLabelFetch = 'NOT_FETCH';
      payload.customerLabelUploadSupported = 'UNSUPPORTED';
    } else if (payload.labelUploadRequired === 'NOT_REQUIRED') {
      payload.platformLabelFetch = 'NOT_FETCH';
      payload.customerLabelUploadSupported = 'UNSUPPORTED';
    } else if (
      payload.labelUploadRequired === 'REQUIRED'
      && payload.platformLabelFetch === 'NOT_FETCH'
      && payload.customerLabelUploadSupported === 'UNSUPPORTED'
    ) {
      throw new Error('需要上传物流面单时，平台面单获取和客户上传面单至少开启一个');
    }
    return payload;
  };

  const saveChannel = async (values: CustomerChannel, keepOpenAfterCreate = true) => {
    let payload: ReturnType<typeof normalizeChannelPayload>;
    try {
      payload = normalizeChannelPayload(values);
    } catch (error: any) {
      message.error(error.message || '渠道面单配置不完整');
      return false;
    }
    const resp = currentChannel?.customerChannelCode
      ? await updateCustomerChannel(currentChannel.customerChannelCode, payload)
      : await addCustomerChannel(payload);
    const ok = resultOk(resp, currentChannel?.customerChannelCode ? '客户渠道已更新' : '客户渠道已新增');
    if (!ok) {
      return false;
    }
    actionRef.current?.reload();
    if (!currentChannel?.customerChannelCode) {
      const created = {
        ...payload,
        status: 'ENABLED',
        buyerScopeMode: 'ALL',
      };
      setCurrentChannel(created);
      channelForm.setFieldsValue({
        ...created,
        signatureServices: splitSignatureServices(created.signatureServices),
      });
      setBuyerScopeRows([]);
      if (keepOpenAfterCreate) {
        message.info('基础信息已保存，可以继续配置绑定系统渠道和绑定买家');
        return false;
      }
    } else {
      await refreshCurrentChannel(currentChannel.customerChannelCode);
    }
    return true;
  };

  const submitChannelEditor = async () => {
    const values = await channelForm.validateFields();
    const shouldClose = await saveChannel(values as CustomerChannel, true);
    if (shouldClose) {
      setChannelModalOpen(false);
    }
  };

  const toggleStatus = (record: CustomerChannel) => {
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    Modal.confirm({
      title: nextStatus === 'ENABLED' ? '启用客户渠道' : '停用客户渠道',
      content: record.customerChannelName,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (resultOk(await updateCustomerChannelStatus(record.customerChannelCode, nextStatus), '状态已更新')) {
          actionRef.current?.reload();
          if (currentChannel?.customerChannelCode === record.customerChannelCode) {
            await refreshCurrentChannel(record.customerChannelCode);
          }
        }
      },
    });
  };

  const openSystemMappingModal = (record?: SystemMapping) => {
    setCurrentSystemMapping(record);
    systemMappingForm.resetFields();
    systemMappingForm.setFieldsValue(record || {
      status: 'ENABLED',
    } as SystemMapping);
    setSystemMappingModalOpen(true);
  };

  const saveSystemMapping = async (values: SystemMapping) => {
    if (!currentChannel?.customerChannelCode) return false;
    const resp = currentSystemMapping?.mappingId
      ? await updateSystemMapping(currentChannel.customerChannelCode, currentSystemMapping.mappingId, values)
      : await addSystemMapping(currentChannel.customerChannelCode, values);
    const ok = resultOk(resp, currentSystemMapping?.mappingId ? '系统渠道绑定已更新' : '系统渠道已绑定');
    if (ok) {
      reloadDetailTables();
      await refreshCurrentChannel(currentChannel.customerChannelCode);
      return true;
    }
    return false;
  };

  const removeSystemMapping = (record: SystemMapping) => {
    if (!currentChannel?.customerChannelCode) return;
    Modal.confirm({
      title: '删除系统渠道绑定',
      content: record.systemChannelNameSnapshot,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (
          resultOk(
            await deleteSystemMapping(currentChannel.customerChannelCode, record.mappingId),
            '系统渠道绑定已删除',
          )
        ) {
          reloadDetailTables();
          await refreshCurrentChannel(currentChannel.customerChannelCode);
        }
      },
    });
  };

  const openBuyerScopeModal = () => {
    const mode = currentChannel?.buyerScopeMode === 'EXCLUDE' ? 'EXCLUDE' : 'INCLUDE';
    setBuyerScopeModeDraft(mode);
    setSelectedBuyerKeys(buyerScopeRows.map((item) => String(item.buyerId)));
    setBuyerKeyword('');
    setBuyerScopeModalOpen(true);
  };

  const saveBuyerScopeFromModal = async () => {
    if (!currentChannel?.customerChannelCode) return;
    if (!selectedBuyerKeys.length) {
      message.warning('请至少选择一个买家');
      return;
    }
    const ok = resultOk(
      await saveBuyerScope(currentChannel.customerChannelCode, {
        buyerScopeMode: buyerScopeModeDraft,
        buyerIds: selectedBuyerKeys.map((item) => Number(item)),
      }),
      '买家范围已保存',
    );
    if (ok) {
      await loadBuyerScope(currentChannel.customerChannelCode);
      await refreshCurrentChannel(currentChannel.customerChannelCode);
      actionRef.current?.reload();
      setBuyerScopeModalOpen(false);
    }
  };

  const resetBuyerScopeToAll = () => {
    if (!currentChannel?.customerChannelCode) return;
    Modal.confirm({
      title: '恢复全部买家可用',
      content: '恢复后不再保留可用名单或不可用名单。',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await saveBuyerScope(currentChannel.customerChannelCode, { buyerScopeMode: 'ALL', buyerIds: [] }),
          '已恢复全部买家可用',
        );
        if (ok) {
          setBuyerScopeRows([]);
          await refreshCurrentChannel(currentChannel.customerChannelCode);
          actionRef.current?.reload();
        }
      },
    });
  };

  const channelColumns: ProColumns<CustomerChannel>[] = [
    { title: '客户渠道代码', dataIndex: 'customerChannelCode', width: 170, fixed: 'left' },
    { title: '客户渠道名称', dataIndex: 'customerChannelName', width: 220, fixed: 'left' },
    {
      title: '渠道类型',
      dataIndex: 'channelType',
      valueType: 'select',
      valueEnum: channelTypeOptions.reduce<Record<string, any>>((memo, item) => {
        memo[item.value] = { text: item.label };
        return memo;
      }, {}),
      width: 130,
    },
    {
      title: '承运商',
      dataIndex: 'standardCarrierCode',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: finalCarrierOptions,
      },
      width: 140,
    },
    {
      title: '签名服务',
      dataIndex: 'signatureServices',
      search: false,
      width: 180,
      render: (_, record) => formatSignatureServices(record.signatureServices),
    },
    {
      title: '上传物流面单',
      dataIndex: 'labelUploadRequired',
      search: false,
      width: 130,
      render: (_, record) => (
        record.channelType === 'THIRD_PARTY_LABEL'
          ? formatValueEnum(labelUploadEnum, record.labelUploadRequired)
          : '-'
      ),
    },
    {
      title: '平台面单获取',
      dataIndex: 'platformLabelFetch',
      search: false,
      width: 130,
      render: (_, record) => (
        record.channelType === 'THIRD_PARTY_LABEL' && record.labelUploadRequired === 'REQUIRED'
          ? formatValueEnum(platformLabelFetchEnum, record.platformLabelFetch)
          : '-'
      ),
    },
    {
      title: '客户上传面单支持',
      dataIndex: 'customerLabelUploadSupported',
      search: false,
      width: 150,
      render: (_, record) => (
        record.channelType === 'THIRD_PARTY_LABEL' && record.labelUploadRequired === 'REQUIRED'
          ? formatValueEnum(customerLabelUploadEnum, record.customerLabelUploadSupported)
          : '-'
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusEnum,
      width: 120,
      render: (_, record) => (
        <Switch
          size="small"
          checked={record.status === 'ENABLED'}
          checkedChildren="启用"
          unCheckedChildren="停用"
          disabled={!canStatus}
          onChange={() => toggleStatus(record)}
        />
      ),
    },
    {
      title: '绑定系统渠道数量',
      dataIndex: 'systemMappingCount',
      search: false,
      width: 140,
      render: (_, record) => record.systemMappingCount || 0,
    },
    {
      title: '买家范围',
      dataIndex: 'buyerScopeSummary',
      search: false,
      ellipsis: true,
      width: 240,
      render: (_, record) => record.buyerScopeSummary || '全部买家',
    },
    { title: '最后更新人', dataIndex: 'updateBy', search: false, width: 130 },
    { title: '最后更新时间', dataIndex: 'updateTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      fixed: 'right',
      width: 100,
      render: (_, record) => [
        <Button key="edit" type="link" size="small" hidden={!canEdit} onClick={() => openEditChannel(record, 'systemMappings')}>
          编辑
        </Button>,
      ],
    },
  ];

  const systemMappingColumns: ProColumns<SystemMapping>[] = [
    { title: '系统渠道代码', dataIndex: 'systemChannelCode', width: 170 },
    { title: '系统渠道名称', dataIndex: 'systemChannelNameSnapshot', width: 240 },
    { title: '承运商', dataIndex: 'standardCarrierCodeSnapshot', width: 140 },
    {
      title: '签名服务',
      dataIndex: 'signatureServicesSnapshot',
      width: 180,
      render: (_, record) => formatSignatureServices(record.signatureServicesSnapshot),
    },
    { title: '绑定状态', dataIndex: 'status', valueEnum: bindingStatusEnum, width: 110 },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      render: (_, record) => [
        <Button key="edit" type="link" size="small" hidden={!canBind} onClick={() => openSystemMappingModal(record)}>
          编辑
        </Button>,
        <Button key="delete" type="link" size="small" danger hidden={!canBind} onClick={() => removeSystemMapping(record)}>
          删除
        </Button>,
      ],
    },
  ];

  const buyerScopeColumns: ProColumns<BuyerScopeRow>[] = [
    { title: '买家代码', dataIndex: 'buyerCodeSnapshot', width: 160 },
    { title: '买家名称', dataIndex: 'buyerNameSnapshot', width: 240 },
    { title: '买家简称', dataIndex: 'buyerShortNameSnapshot', width: 180 },
    { title: '添加时间', dataIndex: 'createTime', width: 170 },
  ];

  const buyerOptionColumns: ProColumns<OptionItem>[] = [
    { title: '买家代码', dataIndex: 'code', width: 160 },
    { title: '买家名称', dataIndex: 'name', width: 240 },
    { title: '买家简称', dataIndex: 'shortName', width: 180 },
  ];

  const isThirdPartyLabel = channelType === 'THIRD_PARTY_LABEL';
  const shouldShowLabelSource = isThirdPartyLabel && labelUploadRequired === 'REQUIRED';
  const isCreateBaseStep = channelEditorMode === 'create' && !currentChannel?.customerChannelCode;
  const shouldShowChannelDetails = channelEditorMode === 'edit' || !!currentChannel?.customerChannelCode;
  const channelModalTitle = channelEditorMode === 'edit'
    ? '编辑客户渠道'
    : (isCreateBaseStep ? '新增客户渠道' : '配置客户渠道');
  const channelSubmitText = isCreateBaseStep
    ? '保存并下一步'
    : (channelEditorMode === 'edit' ? '确定' : '完成');

  return (
    <PageContainer title={false}>
      <ProTable<CustomerChannel>
        actionRef={actionRef}
        rowKey="customerChannelCode"
        columns={channelColumns}
        request={(params) => getCustomerChannelList(params).then(toTableResult)}
        pagination={getProTablePagination(20)}
        scroll={getProTableScroll(1760)}
        search={getPersistedProTableSearch({ labelWidth: 100, fieldCount: 5 }, 'customer-logistics-channel')}
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} hidden={!canAdd} onClick={openCreateChannel}>
            新增渠道
          </Button>,
        ]}
      />

      <Modal
        title={channelModalTitle}
        open={channelModalOpen}
        width="88vw"
        style={{ maxWidth: 1440, top: 24 }}
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
            paddingTop: 8,
          },
        }}
      >
        <div style={{ padding: '8px 0 14px' }}>
          <div style={{ borderLeft: '3px solid #1677ff', paddingLeft: 10, fontWeight: 600, marginBottom: 16 }}>
            基础信息
          </div>
          <ProForm<CustomerChannel> form={channelForm} layout="vertical" submitter={false}>
            <Row gutter={24}>
              <Col xs={24} lg={8}>
                <ProFormSelect
                  name="channelType"
                  label="渠道类型"
                  options={channelTypeFallbackOptions}
                  fieldProps={{
                    ...SEARCHABLE_SELECT_PROPS,
                    style: { width: '100%' },
                    onChange: (value) => {
                      if (value === 'WAREHOUSE_LABEL') {
                        channelForm.setFieldsValue({
                          labelUploadRequired: 'NOT_REQUIRED',
                          platformLabelFetch: 'NOT_FETCH',
                          customerLabelUploadSupported: 'UNSUPPORTED',
                        });
                      }
                    },
                  }}
                  rules={[{ required: true, message: '请选择渠道类型' }]}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ProFormText
                  name="customerChannelCode"
                  label="客户渠道代码"
                  disabled={!!currentChannel?.customerChannelCode}
                  fieldProps={{ style: { width: '100%' } }}
                  rules={[{ required: true, message: '请输入客户渠道代码' }]}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ProFormText
                  name="customerChannelName"
                  label="客户渠道名称"
                  fieldProps={{ style: { width: '100%' } }}
                  rules={[{ required: true, message: '请输入客户渠道名称' }]}
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
              {isThirdPartyLabel ? (
                <Col xs={24} lg={8}>
                  <ProFormRadio.Group
                    name="labelUploadRequired"
                    label="上传物流面单"
                    options={[
                      { label: '需要上传', value: 'REQUIRED' },
                      { label: '不需要上传', value: 'NOT_REQUIRED' },
                    ]}
                    rules={[{ required: true, message: '请选择是否需要上传物流面单' }]}
                  />
                </Col>
              ) : null}
              {shouldShowLabelSource ? (
                <>
                  <Col xs={24} lg={8}>
                    <ProFormRadio.Group
                      name="platformLabelFetch"
                      label="平台面单获取"
                      options={[
                        { label: '获取', value: 'FETCH' },
                        { label: '不获取', value: 'NOT_FETCH' },
                      ]}
                      rules={[{ required: true, message: '请选择平台面单获取方式' }]}
                    />
                  </Col>
                  <Col xs={24} lg={8}>
                    <ProFormRadio.Group
                      name="customerLabelUploadSupported"
                      label="客户上传面单支持"
                      options={[
                        { label: '支持', value: 'SUPPORTED' },
                        { label: '不支持', value: 'UNSUPPORTED' },
                      ]}
                      rules={[{ required: true, message: '请选择是否支持客户上传面单' }]}
                    />
                  </Col>
                </>
              ) : null}
              <Col span={24}>
                <ProFormTextArea name="remark" label="备注" fieldProps={{ rows: 2 }} />
              </Col>
            </Row>
          </ProForm>
        </div>

        {shouldShowChannelDetails ? (
          <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8 }}>
            <Tabs
              activeKey={activeEditorTab}
              onChange={setActiveEditorTab}
              items={[
                {
                  key: 'systemMappings',
                  label: '绑定系统渠道',
                  children: (
                    <ProTable<SystemMapping>
                      actionRef={systemMappingActionRef}
                      rowKey="mappingId"
                      columns={systemMappingColumns}
                      request={() => currentChannel?.customerChannelCode
                        ? getSystemMappings(currentChannel.customerChannelCode).then(toListResult)
                        : Promise.resolve({ data: [], success: true, total: 0 })}
                      search={false}
                      pagination={getProTablePagination(10)}
                      scroll={getProTableScroll(1180, { y: 320 })}
                      toolBarRender={() => [
                        <Button
                          key="add"
                          icon={<LinkOutlined />}
                          disabled={!currentChannel?.customerChannelCode}
                          hidden={!canBind}
                          onClick={() => openSystemMappingModal()}
                        >
                          绑定系统渠道
                        </Button>,
                      ]}
                    />
                  ),
                },
                {
                  key: 'buyerScope',
                  label: '绑定买家',
                  children: (
                    <Space orientation="vertical" size={12} style={{ width: '100%' }}>
                      <Alert
                        showIcon
                        type={currentChannel?.buyerScopeMode === 'ALL' ? 'info' : 'success'}
                        title={currentChannel?.buyerScopeSummary || '当前未绑定买家，默认所有买家可用'}
                      />
                      <Space>
                        <Button
                          icon={<TeamOutlined />}
                          disabled={!currentChannel?.customerChannelCode}
                          hidden={!canBuyer}
                          onClick={openBuyerScopeModal}
                        >
                          绑定买家
                        </Button>
                        <Button
                          disabled={!currentChannel?.customerChannelCode || currentChannel?.buyerScopeMode === 'ALL'}
                          hidden={!canBuyer}
                          onClick={resetBuyerScopeToAll}
                        >
                          恢复全部买家可用
                        </Button>
                      </Space>
                      <ProTable<BuyerScopeRow>
                        rowKey="scopeId"
                        columns={buyerScopeColumns}
                        dataSource={buyerScopeRows}
                        search={false}
                        pagination={getProTablePagination(10)}
                        scroll={getProTableScroll(780, { y: 300 })}
                        toolBarRender={false}
                      />
                    </Space>
                  ),
                },
              ]}
            />
          </div>
        ) : null}
      </Modal>

      <ModalForm<SystemMapping>
        title={currentSystemMapping?.mappingId ? '编辑系统渠道绑定' : '绑定系统渠道'}
        open={systemMappingModalOpen}
        form={systemMappingForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setSystemMappingModalOpen(false) }}
        onOpenChange={setSystemMappingModalOpen}
        onFinish={saveSystemMapping}
      >
        <ProFormSelect
          name="systemChannelCode"
          label="系统渠道"
          options={systemChannelOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          disabled={!!currentSystemMapping?.mappingId}
          rules={[{ required: true, message: '请选择系统渠道' }]}
        />
        <ProFormSelect name="status" label="绑定状态" options={statusOptions} fieldProps={SEARCHABLE_SELECT_PROPS} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <Modal
        title="绑定买家"
        open={buyerScopeModalOpen}
        width={920}
        destroyOnHidden
        onCancel={() => setBuyerScopeModalOpen(false)}
        onOk={saveBuyerScopeFromModal}
        okText="确定"
        cancelText="取消"
        styles={{
          body: {
            maxHeight: 'calc(100vh - 220px)',
            overflow: 'hidden',
          },
        }}
      >
        <Space orientation="vertical" size={12} style={{ width: '100%' }}>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Radio.Group
              value={buyerScopeModeDraft}
              onChange={(event) => setBuyerScopeModeDraft(event.target.value)}
              options={[
                { label: '可用名单', value: 'INCLUDE' },
                { label: '不可用名单', value: 'EXCLUDE' },
              ]}
            />
            <Input.Search
              allowClear
              placeholder="搜索买家代码/买家名称/买家简称"
              style={{ width: 320 }}
              value={buyerKeyword}
              onChange={(event) => setBuyerKeyword(event.target.value)}
            />
          </Space>
          <ProTable<OptionItem>
            rowKey="value"
            columns={buyerOptionColumns}
            dataSource={filteredBuyerOptions}
            search={false}
            pagination={getProTablePagination(10)}
            scroll={getProTableScroll(680, { y: 300 })}
            toolBarRender={false}
            rowSelection={{
              selectedRowKeys: selectedBuyerKeys,
              onChange: (keys) => setSelectedBuyerKeys(keys as (string | number)[]),
            }}
          />
        </Space>
      </Modal>
    </PageContainer>
  );
}
