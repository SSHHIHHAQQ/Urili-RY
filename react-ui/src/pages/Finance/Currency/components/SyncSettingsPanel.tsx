import { ReloadOutlined, SettingOutlined, SyncOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  type ProColumns,
  type ProFormInstance,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProFormTimePicker,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Descriptions, Space, Tag, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  getSyncConfig,
  getSyncLogList,
  saveSyncConfig,
  syncRates,
  testSyncConfig,
} from '@/services/finance/currency';
import {
  statusValueEnum,
  syncStatusValueEnum,
} from '../constants';

const showApiApplicationName = 'fenxiao';
const showApiApplicationId = '2080411';

type SyncSettingsPanelProps = {
  access: { hasPerms: (permission: string) => boolean };
  onSynced?: () => void;
};

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function normalizeSyncValues(values: API.Finance.SyncConfig) {
  const rateAnchorTime = values.rateAnchorTime as any;
  return {
    ...values,
    rateAnchorTime:
      rateAnchorTime && typeof rateAnchorTime.format === 'function'
        ? rateAnchorTime.format('HH:mm:ss')
        : rateAnchorTime,
  };
}

function buildSyncInitialValues(syncConfig?: API.Finance.SyncConfig) {
  return {
    baseCurrencyCode: 'CNY',
    providerCode: 'SHOWAPI_BANK_RATE',
    providerName: 'ShowAPI银行汇率查询',
    showApiApplicationName,
    showApiApplicationId,
    rateAnchorTime: '09:30:00',
    status: '0',
    ...syncConfig,
    credential: undefined,
  };
}

function statusText(value?: string) {
  if (!value) return '-';
  return statusValueEnum[value as keyof typeof statusValueEnum]?.text || value;
}

function syncStatusText(value?: string) {
  if (!value) return '-';
  return syncStatusValueEnum[value as keyof typeof syncStatusValueEnum]?.text || value;
}

function syncStatusTag(value?: string) {
  if (!value) return '-';
  const color = value === 'SUCCESS' ? 'success' : value === 'FAILED' ? 'error' : 'warning';
  return <Tag color={color}>{syncStatusText(value)}</Tag>;
}

function statusTag(value?: string) {
  if (!value) return '-';
  return <Tag color={value === '0' ? 'success' : 'default'}>{statusText(value)}</Tag>;
}

export default function SyncSettingsPanel({
  access,
  onSynced,
}: SyncSettingsPanelProps) {
  const syncFormRef =
    useRef<ProFormInstance<API.Finance.SyncConfig> | undefined>(undefined);
  const syncLogActionRef = useRef<ActionType | undefined>(undefined);
  const [syncConfig, setSyncConfig] = useState<API.Finance.SyncConfig>();
  const [syncModalOpen, setSyncModalOpen] = useState(false);
  const [testing, setTesting] = useState(false);
  const [syncing, setSyncing] = useState(false);

  const reloadSyncConfig = async () => {
    const resp = await getSyncConfig();
    if (resp.code === 200) {
      setSyncConfig({ ...resp.data, credential: undefined });
    }
  };

  useEffect(() => {
    reloadSyncConfig();
  }, []);

  const handleSave = async (values: API.Finance.SyncConfig) => {
    const ok = resultOk(
      await saveSyncConfig(normalizeSyncValues(values)),
      '同步设置已保存',
    );
    if (ok) {
      await reloadSyncConfig();
    }
    return ok;
  };

  const handleTestConnection = async () => {
    setTesting(true);
    try {
      const values = syncFormRef.current?.getFieldsValue() || {};
      const resp = await testSyncConfig(normalizeSyncValues(values));
      if (resp.code === 200) {
        message.success(`测试成功，返回币种 ${resp.data.currencyCount}`);
        syncLogActionRef.current?.reload();
        await reloadSyncConfig();
      } else {
        message.error(resp.msg);
      }
    } finally {
      setTesting(false);
    }
  };

  const handleSyncNow = async () => {
    setSyncing(true);
    try {
      const values = await syncFormRef.current?.validateFields();
      const saveOk = resultOk(
        await saveSyncConfig(normalizeSyncValues(values || {})),
        '同步设置已保存',
      );
      if (!saveOk) return;

      const resp = await syncRates();
      if (resp.code === 200) {
        message.success(`同步完成，更新币种 ${resp.data.updatedCount}`);
        onSynced?.();
        syncLogActionRef.current?.reload();
        await reloadSyncConfig();
        setSyncModalOpen(false);
      } else {
        message.error(resp.msg);
      }
    } finally {
      setSyncing(false);
    }
  };

  const syncLogColumns: ProColumns<API.Finance.SyncLog>[] = [
    { title: '请求时间', dataIndex: 'requestTime', width: 170, search: false },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: syncStatusValueEnum,
      width: 110,
    },
    { title: '服务商', dataIndex: 'providerCode', width: 140 },
    { title: '返回币种数', dataIndex: 'currencyCount', width: 110, search: false },
    { title: '更新币种数', dataIndex: 'updatedCount', width: 110, search: false },
    { title: '耗时(ms)', dataIndex: 'costMs', width: 100, search: false },
    { title: '错误码', dataIndex: 'errorCode', width: 130, search: false },
    { title: '错误信息', dataIndex: 'errorMessage', ellipsis: true, search: false },
    { title: 'TraceId', dataIndex: 'traceId', width: 220, copyable: true },
  ];

  return (
    <>
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <div
          style={{
            background: '#fff',
            border: '1px solid #f0f0f0',
            borderRadius: 6,
            padding: 16,
          }}
        >
          <Space
            align="start"
            style={{
              width: '100%',
              justifyContent: 'space-between',
              marginBottom: 12,
            }}
          >
            <Space direction="vertical" size={2}>
              <Typography.Text strong>
                {syncConfig?.providerName || 'ShowAPI银行汇率查询'}
              </Typography.Text>
              <Typography.Text type="secondary">
                官方汇率取现汇卖出价，基准币种 CNY
              </Typography.Text>
            </Space>
            <Button
              icon={<SettingOutlined />}
              hidden={!access.hasPerms('finance:currency:syncConfig')}
              onClick={() => setSyncModalOpen(true)}
            >
              同步设置
            </Button>
          </Space>
          <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3, xl: 4 }}>
            <Descriptions.Item label="启用状态">
              {statusTag(syncConfig?.status)}
            </Descriptions.Item>
            <Descriptions.Item label="汇率基准时间">
              {syncConfig?.rateAnchorTime || '09:30:00'}
            </Descriptions.Item>
            <Descriptions.Item label="接入密钥">
              {syncConfig?.credentialMasked || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="最近同步">
              {syncConfig?.lastSyncTime || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="最近状态">
              {syncStatusTag(syncConfig?.lastSyncStatus)}
            </Descriptions.Item>
            <Descriptions.Item label="应用名称">
              {showApiApplicationName}
            </Descriptions.Item>
            <Descriptions.Item label="应用 ID">
              {showApiApplicationId}
            </Descriptions.Item>
            <Descriptions.Item label="基准币种">
              人民币 (CNY)
            </Descriptions.Item>
          </Descriptions>
        </div>

        <ProTable<API.Finance.SyncLog>
          actionRef={syncLogActionRef}
          rowKey="syncLogId"
          columns={syncLogColumns}
          search={getPersistedProTableSearch(
            { labelWidth: 100 },
            'finance-currency-sync-log',
          )}
          request={async (params) => {
            const resp = await getSyncLogList(params);
            return {
              data: resp.rows || [],
              success: resp.code === 200,
              total: resp.total || 0,
            };
          }}
        />
      </Space>

      <ModalForm<API.Finance.SyncConfig>
        formRef={syncFormRef}
        title="同步设置"
        open={syncModalOpen}
        width={760}
        grid
        key={syncConfig?.syncConfigId || 'new-sync-config'}
        modalProps={{
          destroyOnHidden: true,
          onCancel: () => setSyncModalOpen(false),
        }}
        initialValues={buildSyncInitialValues(syncConfig)}
        onOpenChange={setSyncModalOpen}
        onFinish={handleSave}
        submitter={{
          searchConfig: {
            submitText: '保存设置',
          },
          render: (_, dom) => [
            <Button
              key="test"
              icon={<ReloadOutlined />}
              loading={testing}
              hidden={!access.hasPerms('finance:currency:sync')}
              onClick={handleTestConnection}
            >
              测试连接
            </Button>,
            <Button
              key="sync"
              icon={<SyncOutlined />}
              loading={syncing}
              hidden={!access.hasPerms('finance:currency:sync')}
              onClick={handleSyncNow}
            >
              立即同步
            </Button>,
            ...dom,
          ],
        }}
      >
        <ProFormText
          name="providerName"
          label="官方汇率服务"
          colProps={{ xs: 24, md: 12 }}
          readonly
        />
        <ProFormSelect
          name="baseCurrencyCode"
          label="基准币种"
          colProps={{ xs: 24, md: 12 }}
          options={[{ label: '人民币 (CNY)', value: 'CNY' }]}
          readonly
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormText
          name="showApiApplicationName"
          label="应用名称"
          colProps={{ xs: 24, md: 12 }}
          readonly
        />
        <ProFormText
          name="showApiApplicationId"
          label="应用 ID"
          colProps={{ xs: 24, md: 12 }}
          readonly
        />
        <ProFormText.Password
          name="credential"
          label="接入密钥"
          colProps={{ xs: 24 }}
          placeholder={syncConfig?.credentialMasked || '保存后只展示脱敏值'}
          fieldProps={{ autoComplete: 'new-password' }}
        />
        <ProFormTimePicker
          name="rateAnchorTime"
          label="汇率基准时间"
          colProps={{ xs: 24, md: 12 }}
          fieldProps={{ format: 'HH:mm:ss' }}
          rules={[{ required: true }]}
        />
        <ProFormSelect
          name="status"
          label="启用状态"
          colProps={{ xs: 24, md: 12 }}
          valueEnum={statusValueEnum}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormTextArea name="remark" label="备注" colProps={{ xs: 24 }} />
      </ModalForm>
    </>
  );
}
