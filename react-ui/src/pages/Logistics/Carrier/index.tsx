import {
  DownOutlined,
  KeyOutlined,
  LinkOutlined,
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  type ProColumns,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
  type ActionType,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Drawer, Dropdown, Form, Modal, Space, Tabs, Tag, message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { getDictSelectOption, getDictValueEnum } from '@/services/system/dict';
import {
  addCarrier,
  addChannelMapping,
  addSystemChannel,
  authorizeCarrier,
  deleteChannelMapping,
  getCarrierChannels,
  getCarrierList,
  getChannelMappings,
  getRequestLogs,
  getSystemChannelList,
  saveAgg56Credentials,
  syncCarrierChannels,
  updateCarrier,
  updateCarrierStatus,
  updateSystemChannel,
} from '@/services/logistics/carrier';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type CarrierConnection = {
  carrierAccountId: number;
  providerKind: string;
  carrierName: string;
  apiBaseUrl: string;
  appToken?: string;
  appKey?: string;
  status: string;
  credentialStatus: string;
  lastAuthorizedTime?: string;
  lastChannelSyncTime?: string;
  displayOrder?: number;
  remark?: string;
  agg56?: {
    appTokenMask?: string;
    appKeyMask?: string;
    agg56UserAccountMask?: string;
    agg56CustomerCode?: string;
  };
};

type ChannelCandidate = {
  carrierAccountId: number;
  externalChannelCode: string;
  externalChannelName: string;
  rawFinalCarrierText?: string;
  status: string;
  lastSeenTime?: string;
};

type SystemChannel = {
  systemChannelCode: string;
  systemChannelName: string;
  standardCarrierCode: string;
  status: string;
  remark?: string;
};

type ChannelMapping = {
  mappingId: number;
  externalChannelCode: string;
  externalChannelNameSnapshot: string;
  systemChannelCode: string;
  systemChannelNameSnapshot: string;
  standardCarrierCode: string;
  status: string;
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
  return {
    data: resp.data || [],
    success: resp.code === 200,
    total: (resp.data || []).length,
  };
}

export default function LogisticsCarrierPage() {
  const access = useAccess();
  const canQueryCarrier = access.hasPerms('logistics:carrier:query');
  const canAddCarrier = access.hasPerms('logistics:carrier:add');
  const canEditCarrier = access.hasPerms('logistics:carrier:edit');
  const canManageCredential = access.hasPerms('logistics:carrier:credential');
  const canSyncCarrier = access.hasPerms('logistics:carrier:sync');
  const canManageCarrierChannel = access.hasPerms('logistics:carrier:channel');
  const canViewCarrierLog = access.hasPerms('logistics:carrier:log');
  const actionRef = useRef<ActionType>(undefined);
  const channelActionRef = useRef<ActionType>(undefined);
  const mappingActionRef = useRef<ActionType>(undefined);
  const logActionRef = useRef<ActionType>(undefined);
  const systemChannelActionRef = useRef<ActionType>(undefined);
  const [carrierForm] = Form.useForm<CarrierConnection>();
  const [credentialForm] = Form.useForm();
  const [systemChannelForm] = Form.useForm<SystemChannel>();
  const [mappingForm] = Form.useForm();
  const [providerOptions, setProviderOptions] = useState<any[]>([]);
  const [finalCarrierOptions, setFinalCarrierOptions] = useState<any[]>([]);
  const [connectionStatusEnum, setConnectionStatusEnum] = useState<Record<string, any>>({});
  const [credentialStatusEnum, setCredentialStatusEnum] = useState<Record<string, any>>({});
  const [channelStatusEnum, setChannelStatusEnum] = useState<Record<string, any>>({});
  const [mappingStatusEnum, setMappingStatusEnum] = useState<Record<string, any>>({});
  const [carrierModalOpen, setCarrierModalOpen] = useState(false);
  const [credentialModalOpen, setCredentialModalOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [systemChannelDrawerOpen, setSystemChannelDrawerOpen] = useState(false);
  const [systemChannelModalOpen, setSystemChannelModalOpen] = useState(false);
  const [mappingModalOpen, setMappingModalOpen] = useState(false);
  const [currentCarrier, setCurrentCarrier] = useState<CarrierConnection>();
  const [currentSystemChannel, setCurrentSystemChannel] = useState<SystemChannel>();
  const [systemChannelOptions, setSystemChannelOptions] = useState<any[]>([]);
  const [candidateOptions, setCandidateOptions] = useState<any[]>([]);

  useEffect(() => {
    getDictSelectOption('logistics_provider_kind').then(setProviderOptions);
    getDictSelectOption('logistics_final_carrier').then(setFinalCarrierOptions);
    getDictValueEnum('logistics_connection_status').then(setConnectionStatusEnum);
    getDictValueEnum('logistics_credential_status').then(setCredentialStatusEnum);
    getDictValueEnum('logistics_channel_status').then(setChannelStatusEnum);
    getDictValueEnum('logistics_mapping_status').then(setMappingStatusEnum);
  }, []);

  const reloadAll = () => {
    actionRef.current?.reload();
    channelActionRef.current?.reload();
    mappingActionRef.current?.reload();
    logActionRef.current?.reload();
    systemChannelActionRef.current?.reload();
  };

  const openCreateCarrier = () => {
    setCurrentCarrier(undefined);
    carrierForm.setFieldsValue({
      providerKind: 'AGG56',
      apiBaseUrl: 'https://www.agg56.com',
      status: 'ENABLED',
    } as CarrierConnection);
    setCarrierModalOpen(true);
  };

  const openEditCarrier = (record: CarrierConnection) => {
    setCurrentCarrier(record);
    carrierForm.setFieldsValue(record);
    setCarrierModalOpen(true);
  };

  const saveCarrier = async (values: CarrierConnection) => {
    const resp = currentCarrier?.carrierAccountId
      ? await updateCarrier(currentCarrier.carrierAccountId, values)
      : await addCarrier(values);
    if (resultOk(resp, currentCarrier?.carrierAccountId ? '物流商已更新' : '物流商已新增')) {
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const openCredentials = (record: CarrierConnection) => {
    setCurrentCarrier(record);
    credentialForm.resetFields();
    setCredentialModalOpen(true);
  };

  const saveCredentials = async (values: Record<string, any>) => {
    if (!currentCarrier?.carrierAccountId) return false;
    const ok = resultOk(
      await saveAgg56Credentials(currentCarrier.carrierAccountId, values),
      '授权信息已保存',
    );
    if (ok) {
      actionRef.current?.reload();
    }
    return ok;
  };

  const runAuthorize = async (record: CarrierConnection) => {
    resultOk(await authorizeCarrier(record.carrierAccountId), '授权校验已通过');
    actionRef.current?.reload();
    logActionRef.current?.reload();
  };

  const runSyncChannels = async (record: CarrierConnection) => {
    resultOk(await syncCarrierChannels(record.carrierAccountId), '物流商渠道已同步');
    reloadAll();
  };

  const toggleCarrierStatus = (record: CarrierConnection) => {
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    Modal.confirm({
      title: nextStatus === 'ENABLED' ? '启用物流商' : '停用物流商',
      content: record.carrierName,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (resultOk(await updateCarrierStatus(record.carrierAccountId, nextStatus), '状态已更新')) {
          actionRef.current?.reload();
        }
      },
    });
  };

  const openCarrierDrawer = (record: CarrierConnection) => {
    setCurrentCarrier(record);
    setDrawerOpen(true);
  };

  const loadSystemChannelOptions = async () => {
    const resp = await getSystemChannelList({ pageNum: 1, pageSize: 200, status: 'ENABLED' });
    setSystemChannelOptions(
      (resp.rows || []).map((item: SystemChannel) => ({
        label: `${item.systemChannelName}（${item.systemChannelCode}）`,
        value: item.systemChannelCode,
      })),
    );
  };

  const loadCandidateOptions = async (carrierAccountId: number) => {
    const resp = await getCarrierChannels(carrierAccountId, { status: 'ACTIVE' });
    setCandidateOptions(
      (resp.data || []).map((item: ChannelCandidate) => ({
        label: `${item.externalChannelName}（${item.externalChannelCode}）`,
        value: item.externalChannelCode,
      })),
    );
  };

  const openMappingModal = async (candidate?: ChannelCandidate) => {
    if (!currentCarrier?.carrierAccountId) return;
    mappingForm.resetFields();
    await Promise.all([
      loadSystemChannelOptions(),
      loadCandidateOptions(currentCarrier.carrierAccountId),
    ]);
    if (candidate) {
      mappingForm.setFieldsValue({
        externalChannelCode: candidate.externalChannelCode,
      });
    }
    setMappingModalOpen(true);
  };

  const saveMapping = async (values: Record<string, any>) => {
    if (!currentCarrier?.carrierAccountId) return false;
    const ok = resultOk(
      await addChannelMapping(currentCarrier.carrierAccountId, values),
      '渠道映射已保存',
    );
    if (ok) {
      mappingActionRef.current?.reload();
      return true;
    }
    return false;
  };

  const removeMapping = (record: ChannelMapping) => {
    if (!currentCarrier?.carrierAccountId) return;
    Modal.confirm({
      title: '删除渠道映射',
      content: record.externalChannelNameSnapshot,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        if (
          resultOk(
            await deleteChannelMapping(currentCarrier.carrierAccountId, record.mappingId),
            '渠道映射已删除',
          )
        ) {
          mappingActionRef.current?.reload();
        }
      },
    });
  };

  const openSystemChannelModal = (record?: SystemChannel) => {
    setCurrentSystemChannel(record);
    systemChannelForm.resetFields();
    systemChannelForm.setFieldsValue(record || { status: 'ENABLED' } as SystemChannel);
    setSystemChannelModalOpen(true);
  };

  const saveSystemChannel = async (values: SystemChannel) => {
    const resp = currentSystemChannel?.systemChannelCode
      ? await updateSystemChannel(currentSystemChannel.systemChannelCode, values.status, values)
      : await addSystemChannel(values);
    if (resultOk(resp, currentSystemChannel?.systemChannelCode ? '系统渠道已更新' : '系统渠道已新增')) {
      systemChannelActionRef.current?.reload();
      return true;
    }
    return false;
  };

  const carrierColumns: ProColumns<CarrierConnection>[] = [
    { title: '物流商名称', dataIndex: 'carrierName', width: 200 },
    {
      title: '物流商系统',
      dataIndex: 'providerKind',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: providerOptions,
      },
      width: 120,
    },
    { title: 'API地址', dataIndex: 'apiBaseUrl', search: false, width: 260, ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: connectionStatusEnum,
      width: 100,
    },
    {
      title: '凭据',
      dataIndex: 'credentialStatus',
      valueType: 'select',
      valueEnum: credentialStatusEnum,
      width: 110,
    },
    { title: '最近授权', dataIndex: 'lastAuthorizedTime', search: false, width: 170 },
    { title: '最近同步', dataIndex: 'lastChannelSyncTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      fixed: 'right',
      width: 210,
      render: (_, record) => [
        <Button key="authorize" type="link" size="small" hidden={!canSyncCarrier} onClick={() => runAuthorize(record)}>
          校验
        </Button>,
        <Button key="sync" type="link" size="small" hidden={!canSyncCarrier} onClick={() => runSyncChannels(record)}>
          同步
        </Button>,
        <Dropdown
          key="more"
          trigger={['click']}
          menu={{
            items: [
              { key: 'detail', label: '渠道', disabled: !canQueryCarrier },
              { key: 'credential', label: '授权', disabled: !canManageCredential },
              { key: 'edit', label: '编辑', disabled: !canEditCarrier },
              {
                key: 'status',
                label: record.status === 'ENABLED' ? '停用' : '启用',
                disabled: !canEditCarrier,
              },
            ],
            onClick: ({ key }) => {
              if (key === 'detail') openCarrierDrawer(record);
              if (key === 'credential') openCredentials(record);
              if (key === 'edit') openEditCarrier(record);
              if (key === 'status') toggleCarrierStatus(record);
            },
          }}
        >
          <Button type="link" size="small">
            更多 <DownOutlined />
          </Button>
        </Dropdown>,
      ],
    },
  ];

  const channelColumns: ProColumns<ChannelCandidate>[] = [
    { title: '物流商渠道代码', dataIndex: 'externalChannelCode', width: 180 },
    { title: '物流商渠道名称', dataIndex: 'externalChannelName', width: 220 },
    { title: '状态', dataIndex: 'status', valueEnum: channelStatusEnum, width: 100 },
    { title: '最近发现', dataIndex: 'lastSeenTime', width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 90,
      render: (_, record) => [
        <Button key="map" type="link" size="small" hidden={!canManageCarrierChannel} onClick={() => openMappingModal(record)}>
          映射
        </Button>,
      ],
    },
  ];

  const mappingColumns: ProColumns<ChannelMapping>[] = [
    { title: '物流商渠道名称', dataIndex: 'externalChannelNameSnapshot', width: 220 },
    { title: '物流商渠道代码', dataIndex: 'externalChannelCode', width: 160 },
    { title: '系统渠道', dataIndex: 'systemChannelNameSnapshot', width: 220 },
    { title: '系统代码', dataIndex: 'systemChannelCode', width: 160 },
    { title: '标准承运商', dataIndex: 'standardCarrierCode', width: 140 },
    { title: '状态', dataIndex: 'status', valueEnum: mappingStatusEnum, width: 100 },
    {
      title: '操作',
      valueType: 'option',
      width: 90,
      render: (_, record) => [
        <Button key="remove" type="link" size="small" danger hidden={!canManageCarrierChannel} onClick={() => removeMapping(record)}>
          删除
        </Button>,
      ],
    },
  ];

  const logColumns: ProColumns<Record<string, any>>[] = [
    { title: '操作', dataIndex: 'operation', width: 130 },
    { title: '接口', dataIndex: 'endpoint', width: 180 },
    { title: '状态', dataIndex: 'status', width: 90, render: (_, record) => <Tag color={record.status === 'SUCCESS' ? 'green' : 'red'}>{record.status}</Tag> },
    { title: '业务单号', dataIndex: 'businessOrderNo', width: 160 },
    { title: '物流商单号', dataIndex: 'providerOrderNo', width: 170 },
    { title: '返回码', dataIndex: 'providerCode', width: 90 },
    { title: '返回信息', dataIndex: 'providerMessage', width: 220, ellipsis: true },
    { title: '耗时(ms)', dataIndex: 'durationMs', width: 100 },
    { title: '请求时间', dataIndex: 'requestTime', width: 170 },
  ];

  const systemChannelColumns: ProColumns<SystemChannel>[] = [
    { title: '系统渠道代码', dataIndex: 'systemChannelCode', width: 180 },
    { title: '系统渠道名称', dataIndex: 'systemChannelName', width: 220 },
    {
      title: '标准承运商',
      dataIndex: 'standardCarrierCode',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: finalCarrierOptions,
      },
      width: 160,
    },
    { title: '状态', dataIndex: 'status', valueEnum: connectionStatusEnum, width: 100 },
    {
      title: '操作',
      valueType: 'option',
      width: 90,
      render: (_, record) => [
        <Button key="edit" type="link" size="small" hidden={!canManageCarrierChannel} onClick={() => openSystemChannelModal(record)}>
          编辑
        </Button>,
      ],
    },
  ];

  return (
    <PageContainer title={false}>
      <ProTable<CarrierConnection>
        actionRef={actionRef}
        rowKey="carrierAccountId"
        columns={carrierColumns}
        request={(params) =>
          getCarrierList({ ...params, pageNum: params.current, pageSize: params.pageSize }).then(toTableResult)
        }
        pagination={getProTablePagination(20)}
        scroll={getProTableScroll(1500)}
        search={getPersistedProTableSearch({ labelWidth: 90, fieldCount: 4 }, 'logistics-carrier')}
        toolBarRender={() => [
          <Button key="systemChannels" icon={<SettingOutlined />} hidden={!canManageCarrierChannel} onClick={() => setSystemChannelDrawerOpen(true)}>
            系统渠道
          </Button>,
          <Button key="add" type="primary" icon={<PlusOutlined />} hidden={!canAddCarrier} onClick={openCreateCarrier}>
            新增
          </Button>,
        ]}
      />

      <Drawer
        title={currentCarrier?.carrierName}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width="82vw"
        destroyOnClose
      >
        <Tabs
          items={[
            {
              key: 'channels',
              label: '物流商渠道',
              children: (
                <ProTable<ChannelCandidate>
                  actionRef={channelActionRef}
                  rowKey="externalChannelCode"
                  columns={channelColumns}
                  request={() => currentCarrier?.carrierAccountId ? getCarrierChannels(currentCarrier.carrierAccountId).then(toListResult) : Promise.resolve({ data: [], success: true, total: 0 })}
                  search={false}
                  pagination={getProTablePagination(10)}
                  scroll={getProTableScroll(1000, { y: 420 })}
                  toolBarRender={() => [
                    <Button key="sync" icon={<ReloadOutlined />} hidden={!access.hasPerms('logistics:carrier:sync')} onClick={() => currentCarrier && runSyncChannels(currentCarrier)}>
                      同步
                    </Button>,
                  ]}
                />
              ),
            },
            {
              key: 'mappings',
              label: '渠道映射',
              disabled: !canManageCarrierChannel,
              children: (
                <ProTable<ChannelMapping>
                  actionRef={mappingActionRef}
                  rowKey="mappingId"
                  columns={mappingColumns}
                  request={() => currentCarrier?.carrierAccountId ? getChannelMappings(currentCarrier.carrierAccountId).then(toListResult) : Promise.resolve({ data: [], success: true, total: 0 })}
                  search={false}
                  pagination={getProTablePagination(10)}
                  scroll={getProTableScroll(1100, { y: 420 })}
                  toolBarRender={() => [
                    <Button key="add" icon={<LinkOutlined />} hidden={!canManageCarrierChannel} onClick={() => openMappingModal()}>
                      新增映射
                    </Button>,
                  ]}
                />
              ),
            },
            {
              key: 'logs',
              label: '请求日志',
              disabled: !canViewCarrierLog,
              children: (
                <ProTable<Record<string, any>>
                  actionRef={logActionRef}
                  rowKey="requestLogId"
                  columns={logColumns}
                  request={(params) => currentCarrier?.carrierAccountId
                    ? getRequestLogs(currentCarrier.carrierAccountId, { ...params, pageNum: params.current, pageSize: params.pageSize }).then(toTableResult)
                    : Promise.resolve({ data: [], success: true, total: 0 })}
                  search={false}
                  pagination={getProTablePagination(10)}
                  scroll={getProTableScroll(1300, { y: 420 })}
                />
              ),
            },
          ]}
        />
      </Drawer>

      <Drawer
        title="系统渠道"
        open={systemChannelDrawerOpen}
        onClose={() => setSystemChannelDrawerOpen(false)}
        width="76vw"
        destroyOnClose
      >
        <ProTable<SystemChannel>
          actionRef={systemChannelActionRef}
          rowKey="systemChannelCode"
          columns={systemChannelColumns}
          request={(params) => getSystemChannelList({ ...params, pageNum: params.current, pageSize: params.pageSize }).then(toTableResult)}
          pagination={getProTablePagination(20)}
          scroll={getProTableScroll(1000, { y: 520 })}
          search={getPersistedProTableSearch({ labelWidth: 90, fieldCount: 4 }, 'logistics-system-channel')}
          toolBarRender={() => [
            <Button key="add" type="primary" icon={<PlusOutlined />} hidden={!canManageCarrierChannel} onClick={() => openSystemChannelModal()}>
              新增
            </Button>,
          ]}
        />
      </Drawer>

      <ModalForm<CarrierConnection>
        title={currentCarrier?.carrierAccountId ? '编辑物流商' : '新增物流商'}
        open={carrierModalOpen}
        form={carrierForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setCarrierModalOpen(false) }}
        onOpenChange={setCarrierModalOpen}
        onFinish={saveCarrier}
      >
        <ProFormText name="carrierName" label="物流商名称" rules={[{ required: true, message: '请输入物流商名称' }]} />
        <ProFormSelect
          name="providerKind"
          label="物流商系统"
          options={providerOptions}
          disabled={!!currentCarrier?.carrierAccountId}
          fieldProps={SEARCHABLE_SELECT_PROPS}
          rules={[{ required: true, message: '请选择物流商系统' }]}
        />
        {!currentCarrier?.carrierAccountId ? (
          <>
            <ProFormText.Password name="appToken" label="APP Token" rules={[{ required: true, message: '请输入APP Token' }]} />
            <ProFormText.Password name="appKey" label="APP Key" rules={[{ required: true, message: '请输入APP Key' }]} />
          </>
        ) : null}
        <ProFormText name="apiBaseUrl" label="API地址" />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <ModalForm
        title={`更换密钥${currentCarrier ? ` - ${currentCarrier.carrierName}` : ''}`}
        open={credentialModalOpen}
        form={credentialForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setCredentialModalOpen(false) }}
        onOpenChange={setCredentialModalOpen}
        onFinish={saveCredentials}
        submitter={{ searchConfig: { submitText: '保存并校验' } }}
      >
        <Space direction="vertical" size={2}>
          {currentCarrier?.agg56?.appTokenMask ? <Tag icon={<KeyOutlined />}>{currentCarrier.agg56.appTokenMask}</Tag> : null}
          {currentCarrier?.agg56?.agg56CustomerCode ? <Tag>{currentCarrier.agg56.agg56CustomerCode}</Tag> : null}
        </Space>
        <ProFormText.Password name="appToken" label="APP Token" rules={[{ required: true, message: '请输入APP Token' }]} />
        <ProFormText.Password name="appKey" label="APP Key" rules={[{ required: true, message: '请输入APP Key' }]} />
      </ModalForm>

      <ModalForm<SystemChannel>
        title={currentSystemChannel?.systemChannelCode ? '编辑系统渠道' : '新增系统渠道'}
        open={systemChannelModalOpen}
        form={systemChannelForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setSystemChannelModalOpen(false) }}
        onOpenChange={setSystemChannelModalOpen}
        onFinish={saveSystemChannel}
      >
        <ProFormText name="systemChannelCode" label="系统渠道代码" disabled={!!currentSystemChannel?.systemChannelCode} rules={[{ required: true, message: '请输入系统渠道代码' }]} />
        <ProFormText name="systemChannelName" label="系统渠道名称" rules={[{ required: true, message: '请输入系统渠道名称' }]} />
        <ProFormSelect name="standardCarrierCode" label="标准承运商" options={finalCarrierOptions} fieldProps={SEARCHABLE_SELECT_PROPS} rules={[{ required: true, message: '请选择标准承运商' }]} />
        <ProFormSelect name="status" label="状态" valueEnum={connectionStatusEnum} fieldProps={SEARCHABLE_SELECT_PROPS} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <ModalForm
        title="渠道映射"
        open={mappingModalOpen}
        form={mappingForm}
        modalProps={{ destroyOnHidden: true, onCancel: () => setMappingModalOpen(false) }}
        onOpenChange={setMappingModalOpen}
        onFinish={saveMapping}
      >
        <ProFormSelect name="externalChannelCode" label="物流商渠道" options={candidateOptions} fieldProps={SEARCHABLE_SELECT_PROPS} rules={[{ required: true, message: '请选择物流商渠道' }]} />
        <ProFormSelect name="systemChannelCode" label="系统渠道" options={systemChannelOptions} fieldProps={SEARCHABLE_SELECT_PROPS} rules={[{ required: true, message: '请选择系统渠道' }]} />
        <ProFormSelect name="standardCarrierCode" label="标准承运商" options={finalCarrierOptions} fieldProps={SEARCHABLE_SELECT_PROPS} rules={[{ required: true, message: '请选择标准承运商' }]} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>
    </PageContainer>
  );
}
