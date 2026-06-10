import { type ActionType, PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Checkbox, Empty, Form, Modal, Select, Space, Typography } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  addLogisticsChannelPairing,
  addSkuPairing,
  addUpstreamConnection,
  addWarehousePairing,
  authorizeUpstreamConnection,
  getLogisticsChannelPairings,
  getUpstreamConnectionList,
  getUpstreamSyncStates,
  syncUpstreamConnection,
  updateUpstreamConnection,
  updateUpstreamConnectionOrder,
  updateUpstreamCredentials,
  updateUpstreamStatus,
} from '@/services/integration/upstreamSystem';
import { getSystemChannelList } from '@/services/logistics/systemChannel';
import { getOfficialWarehouseList } from '@/services/warehouse/warehouse';
import { message } from '@/utils/feedback';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import ConnectionModal, {
  type ConnectionModalMode,
} from './components/ConnectionModal';
import ConnectionSidebar from './components/ConnectionSidebar';
import ConnectionSummary from './components/ConnectionSummary';
import PairingModal from './components/PairingModal';
import SyncTabs from './components/SyncTabs';
import { normalizeSettlementTypeValue, pairingRoleText } from './constants';
import { resultOk } from './helpers';
import './style.css';
import styles from './style.module.css';
import type { PairingModalState } from './types';

const connectionPageSize = 200;
const warehouseOptionPageSize = 1000;
const defaultManualSyncTypes = ['WAREHOUSE', 'LOGISTICS_CHANNEL', 'SKU'];

type WarehouseSelectOption = API.Warehouse.Option & {
  warehouseCode: string;
  warehouseName: string;
};

type SystemChannelSelectOption = {
  label: string;
  value: string;
  systemChannelCode: string;
  systemChannelName: string;
  searchText: string;
};

const syncTypeOptions = [
  {
    label: '仓库',
    value: 'WAREHOUSE',
    permission: 'integration:upstream:sync',
  },
  {
    label: '物流渠道',
    value: 'LOGISTICS_CHANNEL',
    permission: 'integration:upstream:sync',
  },
  {
    label: 'SKU信息',
    value: 'SKU',
    permission: 'integration:upstream:sync',
  },
  {
    label: 'SKU仓库尺寸重量',
    value: 'SKU_DIMENSION',
    permission: 'integration:upstream:dimensionSync',
  },
  {
    label: 'SKU库存',
    value: 'INVENTORY',
    permission: 'integration:upstream:inventorySync',
  },
];

export default function UpstreamSystemPage() {
  const access = useAccess();
  const canListUpstreamConnections = access.hasPerms('integration:upstream:list');
  const canQueryUpstream = access.hasPerms('integration:upstream:query');
  const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list');
  const canQuerySystemChannels = access.hasPerms('channel:system:list');
  const warehouseActionRef = useRef<ActionType>(null);
  const logisticsActionRef = useRef<ActionType>(null);
  const skuActionRef = useRef<ActionType>(null);
  const dimensionActionRef = useRef<ActionType>(null);
  const inventoryActionRef = useRef<ActionType>(null);
  const logActionRef = useRef<ActionType>(null);
  const [connections, setConnections] = useState<
    API.Integration.UpstreamConnection[]
  >([]);
  const [loadingConnections, setLoadingConnections] = useState(false);
  const [selectedConnection, setSelectedConnection] =
    useState<API.Integration.UpstreamConnection>();
  const [connectionModal, setConnectionModal] = useState<{
    open: boolean;
    mode: ConnectionModalMode;
    record?: API.Integration.UpstreamConnection;
  }>({ open: false, mode: 'create' });
  const [pairingModal, setPairingModal] = useState<PairingModalState>({
    open: false,
  });
  const [syncModal, setSyncModal] = useState<{
    open: boolean;
    record?: API.Integration.UpstreamConnection;
    syncTypes: string[];
    submitting: boolean;
  }>({
    open: false,
    syncTypes: defaultManualSyncTypes,
    submitting: false,
  });
  const [warehouseOptions, setWarehouseOptions] = useState<
    WarehouseSelectOption[]
  >([]);
  const [warehouseOptionsLoading, setWarehouseOptionsLoading] = useState(false);
  const [systemChannelOptions, setSystemChannelOptions] = useState<
    SystemChannelSelectOption[]
  >([]);
  const [systemChannelOptionsLoading, setSystemChannelOptionsLoading] =
    useState(false);
  const [syncingConnectionCodes, setSyncingConnectionCodes] = useState<
    string[]
  >([]);
  const syncPollersRef = useRef<Record<string, number>>({});

  useEffect(
    () => () => {
      Object.values(syncPollersRef.current).forEach((timer) => {
        window.clearInterval(timer);
      });
    },
    [],
  );

  const fetchConnections = useCallback(async (preferredCode?: string) => {
    if (!canListUpstreamConnections) {
      setConnections([]);
      setSelectedConnection(undefined);
      return [];
    }
    setLoadingConnections(true);
    try {
      const resp = await getUpstreamConnectionList({
        pageNum: 1,
        pageSize: connectionPageSize,
      });
      if (resp.code !== 200) {
        message.error(resp.msg || '主仓接入加载失败');
        return [];
      }
      const rows = resp.rows || [];
      setConnections(rows);
      setSelectedConnection((current) => {
        const nextCode = preferredCode || current?.connectionCode;
        return rows.find((item) => item.connectionCode === nextCode) || rows[0];
      });
      return rows;
    } finally {
      setLoadingConnections(false);
    }
  }, [canListUpstreamConnections]);

  const loadWarehouseOptions = useCallback(
    async (connectionCode: string, pairingRole: string) => {
      if (!connectionCode) {
        setWarehouseOptions([]);
        return;
      }
      if (!canQueryOfficialWarehouses) {
        setWarehouseOptions([]);
        message.warning('缺少系统仓库查询权限');
        return;
      }
      setWarehouseOptionsLoading(true);
      try {
        const warehouseResp = await getOfficialWarehouseList({
          pageNum: 1,
          pageSize: warehouseOptionPageSize,
          status: '0',
        });
        if (warehouseResp.code !== 200) {
          message.error(warehouseResp.msg || '系统仓库加载失败');
          setWarehouseOptions([]);
          return;
        }
        const options = (warehouseResp.rows || [])
          .filter((item) => item.warehouseCode)
          .filter((item) =>
            pairingRole === 'QUOTE'
              ? !item.quoteWarehousePairingId
              : !item.warehousePairingId,
          )
          .map((item) => {
            const warehouseCode = item.warehouseCode || '';
            const warehouseName = item.warehouseName || '';
            return {
              label: `${warehouseCode} / ${warehouseName}`,
              value: warehouseCode,
              warehouseCode,
              warehouseName,
              name: warehouseName,
              code: warehouseCode,
              searchText: `${warehouseCode} ${warehouseName} ${item.countryCode || ''}`,
            };
          });
        setWarehouseOptions(options);
      } finally {
        setWarehouseOptionsLoading(false);
      }
    },
    [canQueryOfficialWarehouses],
  );

  useEffect(() => {
    fetchConnections();
  }, [fetchConnections]);

  const selectedCode = selectedConnection?.connectionCode || '';
  const selectedPairingRole =
    normalizeSettlementTypeValue(selectedConnection?.settlementType) ===
    'self-operated-receivable'
      ? 'QUOTE'
      : 'FULFILLMENT';
  const selectedPairingRoleLabel =
    pairingRoleText[selectedPairingRole] || selectedPairingRole;

  const loadSystemChannelOptions = useCallback(async () => {
    if (!canQuerySystemChannels) {
      setSystemChannelOptions([]);
      message.warning('缺少系统渠道查询权限');
      return;
    }
    setSystemChannelOptionsLoading(true);
    try {
      const [channelResp, ...pairingResponses] = await Promise.all([
        getSystemChannelList({ pageNum: 1, pageSize: 1000, status: '0' }),
        ...connections.map((item) =>
          getLogisticsChannelPairings(item.connectionCode),
        ),
      ]);
      if (channelResp.code !== 200) {
        message.error(channelResp.msg || '系统渠道加载失败');
        setSystemChannelOptions([]);
        return;
      }
      const pairedSystemChannels = new Set(
        pairingResponses.flatMap((resp) =>
          (resp.data || [])
            .filter(
              (item) =>
                (item.pairingRole || 'FULFILLMENT') === selectedPairingRole,
            )
            .map((item) => item.systemChannelCode),
        ),
      );
      const options = (channelResp.rows || [])
        .filter((item: any) => item.systemChannelCode)
        .filter((item: any) => !pairedSystemChannels.has(item.systemChannelCode))
        .map((item: any) => {
          const systemChannelCode = item.systemChannelCode || '';
          const systemChannelName = item.systemChannelName || '';
          return {
            label: `${systemChannelCode} / ${systemChannelName}`,
            value: systemChannelCode,
            systemChannelCode,
            systemChannelName,
            searchText: [
              systemChannelCode,
              systemChannelName,
              item.standardCarrierCode,
            ]
              .filter(Boolean)
              .join(' '),
          };
        });
      setSystemChannelOptions(options);
    } finally {
      setSystemChannelOptionsLoading(false);
    }
  }, [canQuerySystemChannels, connections, selectedPairingRole]);

  useEffect(() => {
    if (pairingModal.open && pairingModal.type === 'warehouse') {
      loadWarehouseOptions(selectedCode, selectedPairingRole);
    } else if (pairingModal.open && pairingModal.type === 'logistics') {
      loadSystemChannelOptions();
    }
  }, [
    loadSystemChannelOptions,
    loadWarehouseOptions,
    pairingModal,
    selectedCode,
    selectedPairingRole,
  ]);

  const reloadTabs = () => {
    warehouseActionRef.current?.reload();
    logisticsActionRef.current?.reload();
    skuActionRef.current?.reload();
    dimensionActionRef.current?.reload();
    inventoryActionRef.current?.reload();
    logActionRef.current?.reload();
  };

  const reloadCurrent = async (preferredCode?: string) => {
    const targetCode = preferredCode || selectedCode;
    await fetchConnections(targetCode);
    if (!targetCode || targetCode === selectedCode) {
      reloadTabs();
    }
  };

  const markConnectionSyncing = (connectionCode: string, syncing: boolean) => {
    setSyncingConnectionCodes((current) => {
      if (syncing) {
        return current.includes(connectionCode)
          ? current
          : [...current, connectionCode];
      }
      return current.filter((item) => item !== connectionCode);
    });
  };

  const stopSyncPolling = (connectionCode: string) => {
    const timer = syncPollersRef.current[connectionCode];
    if (timer) {
      window.clearInterval(timer);
      delete syncPollersRef.current[connectionCode];
    }
  };

  const pollSyncState = async (connectionCode: string, syncTypes: string[]) => {
    const resp = await getUpstreamSyncStates(connectionCode);
    if (resp.code !== 200) {
      return;
    }
    const states = resp.data || [];
    const targetStates =
      syncTypes.length > 0
        ? states.filter((item) => syncTypes.includes(item.syncType))
        : states;
    const stillSyncing = targetStates.some((item) => item.status === 'SYNCING');
    if (!stillSyncing) {
      markConnectionSyncing(connectionCode, false);
      stopSyncPolling(connectionCode);
      await reloadCurrent(connectionCode);
    }
  };

  const startSyncPolling = (connectionCode: string, syncTypes: string[]) => {
    markConnectionSyncing(connectionCode, true);
    stopSyncPolling(connectionCode);
    window.setTimeout(() => pollSyncState(connectionCode, syncTypes), 1200);
    syncPollersRef.current[connectionCode] = window.setInterval(
      () => pollSyncState(connectionCode, syncTypes),
      5000,
    );
  };

  useEffect(() => {
    if (!selectedCode || !canQueryUpstream) {
      return;
    }
    let disposed = false;
    getUpstreamSyncStates(selectedCode).then((resp) => {
      if (disposed || resp.code !== 200) {
        return;
      }
      const syncingTypes = (resp.data || [])
        .filter((item) => item.status === 'SYNCING')
        .map((item) => item.syncType);
      if (syncingTypes.length > 0) {
        startSyncPolling(selectedCode, syncingTypes);
      } else {
        markConnectionSyncing(selectedCode, false);
      }
    });
    return () => {
      disposed = true;
    };
  }, [selectedCode, canQueryUpstream]);

  const handleAuthorize = async (
    record: API.Integration.UpstreamConnection,
  ) => {
    const hide = message.loading('正在校验授权');
    const ok = resultOk(
      await authorizeUpstreamConnection(record.connectionCode),
      '授权校验通过',
    );
    hide();
    if (ok) {
      await reloadCurrent(record.connectionCode);
    }
  };

  const handleSync = (record: API.Integration.UpstreamConnection) => {
    const allowedDefaults = defaultManualSyncTypes.filter((type) => {
      const option = syncTypeOptions.find((item) => item.value === type);
      return option ? access.hasPerms(option.permission) : false;
    });
    setSyncModal({
      open: true,
      record,
      syncTypes: allowedDefaults,
      submitting: false,
    });
  };

  const submitSync = async () => {
    const record = syncModal.record;
    if (!record) return;
    if (syncModal.syncTypes.length === 0) {
      message.warning('请选择同步内容');
      return;
    }
    setSyncModal((current) => ({ ...current, submitting: true }));
    let resp: (API.Result & { data: API.Integration.SyncResult }) | undefined;
    try {
      resp = await syncUpstreamConnection(record.connectionCode, {
        syncTypes: syncModal.syncTypes,
      });
    } finally {
      setSyncModal((current) => ({ ...current, submitting: false }));
    }
    if (!resp) {
      return;
    }
    if (resp.code === 200) {
      const submittedTypes = [...syncModal.syncTypes];
      setSyncModal({
        open: false,
        syncTypes: defaultManualSyncTypes,
        submitting: false,
      });
      message.success('已开始后台同步，可继续操作');
      startSyncPolling(record.connectionCode, submittedTypes);
      await reloadCurrent(record.connectionCode);
    } else {
      message.error(resp.msg);
    }
  };

  const handleToggleStatus = async (
    record: API.Integration.UpstreamConnection,
  ) => {
    const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const ok = resultOk(
      await updateUpstreamStatus(record.connectionCode, nextStatus),
      '状态已更新',
    );
    if (ok) {
      await reloadCurrent(record.connectionCode);
    }
  };

  const saveOrder = async (connectionCodes: string[]) => {
    const ok = resultOk(
      await updateUpstreamConnectionOrder(connectionCodes),
      '排序已保存',
    );
    if (ok) {
      await fetchConnections(selectedCode);
    }
    return ok;
  };

  return (
    <PageContainer>
      <div className={styles.workspace}>
        <ConnectionSidebar
          access={access}
          connections={connections}
          loading={loadingConnections}
          onCreate={() => setConnectionModal({ open: true, mode: 'create' })}
          onSaveOrder={saveOrder}
          onSelect={setSelectedConnection}
          selectedCode={selectedCode}
        />

        {selectedConnection ? (
          <div className={styles.detailPane}>
            <ConnectionSummary
              access={access}
              connection={selectedConnection}
              syncing={syncingConnectionCodes.includes(selectedCode)}
              onAuthorize={() => handleAuthorize(selectedConnection)}
              onCredential={() =>
                setConnectionModal({
                  open: true,
                  mode: 'credential',
                  record: selectedConnection,
                })
              }
              onEdit={() =>
                setConnectionModal({
                  open: true,
                  mode: 'edit',
                  record: selectedConnection,
                })
              }
              onSync={() => handleSync(selectedConnection)}
              onToggleStatus={() => handleToggleStatus(selectedConnection)}
            />
            <SyncTabs
              key={selectedCode}
              access={access}
              dimensionActionRef={dimensionActionRef}
              inventoryActionRef={inventoryActionRef}
              logActionRef={logActionRef}
              logisticsActionRef={logisticsActionRef}
              onSkuSynced={() => fetchConnections(selectedCode)}
              selectedConnection={selectedConnection}
              setPairingModal={setPairingModal}
              skuActionRef={skuActionRef}
              warehouseActionRef={warehouseActionRef}
            />
          </div>
        ) : (
          <div className={styles.emptyPane}>
            <Empty description="暂无主仓接入" />
          </div>
        )}
      </div>

      <ConnectionModal
        mode={connectionModal.mode}
        open={connectionModal.open}
        record={connectionModal.record}
        onCancel={() => setConnectionModal({ open: false, mode: 'create' })}
        onSubmit={async (values) => {
          const modalRecord = connectionModal.record;
          if (
            connectionModal.mode !== 'create' &&
            !modalRecord?.connectionCode
          ) {
            return false;
          }
          const hide = message.loading('正在保存');
          try {
            let resp: API.Result;
            if (connectionModal.mode === 'create') {
              resp = await addUpstreamConnection(values);
            } else if (connectionModal.mode === 'edit') {
              if (!modalRecord) return false;
              resp = await updateUpstreamConnection(modalRecord.connectionCode, {
                masterWarehouseName: values.masterWarehouseName,
                settlementType: values.settlementType,
                remark: values.remark,
              });
            } else {
              if (!modalRecord) return false;
              resp = await updateUpstreamCredentials(
                modalRecord.connectionCode,
                values,
              );
            }
            const ok = resultOk(resp, '保存成功');
            if (ok) {
              setConnectionModal({ open: false, mode: 'create' });
              await fetchConnections(
                modalRecord?.connectionCode || values.connectionCode,
              );
            }
            return ok;
          } finally {
            hide();
          }
        }}
      />

      <Modal
        title="选择同步内容"
        open={syncModal.open}
        confirmLoading={syncModal.submitting}
        okText="开始同步"
        onCancel={() =>
          setSyncModal({
            open: false,
            syncTypes: defaultManualSyncTypes,
            submitting: false,
          })
        }
        onOk={submitSync}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            尺寸重量为限速同步，库存为上游库存快照。
          </Typography.Text>
          <Checkbox.Group
            value={syncModal.syncTypes}
            onChange={(values) =>
              setSyncModal((current) => ({
                ...current,
                syncTypes: values.map(String),
              }))
            }
          >
            <Space direction="vertical">
              {syncTypeOptions
                .filter((option) => access.hasPerms(option.permission))
                .map((option) => (
                  <Checkbox key={option.value} value={option.value}>
                    {option.label}
                  </Checkbox>
                ))}
            </Space>
          </Checkbox.Group>
        </Space>
      </Modal>

      <PairingModal
        open={pairingModal.open}
        title={
          pairingModal.open && pairingModal.type === 'warehouse'
            ? '仓库配对'
            : pairingModal.open && pairingModal.type === 'logistics'
              ? '物流渠道配对'
              : 'SKU配对'
        }
        upstreamLabel={
          pairingModal.open && pairingModal.type === 'warehouse'
            ? '领星仓库'
            : pairingModal.open && pairingModal.type === 'logistics'
              ? '领星渠道'
              : '领星masterSku'
        }
        upstreamValue={
          pairingModal.open && pairingModal.type === 'warehouse'
            ? `${pairingModal.row.warehouseCode} / ${pairingModal.row.warehouseName}`
            : pairingModal.open && pairingModal.type === 'logistics'
              ? `${pairingModal.row.channelCode} / ${pairingModal.row.channelName}`
              : pairingModal.open
                ? `${pairingModal.row.masterSku} / ${pairingModal.row.masterProductName}`
                : ''
        }
        codeLabel={
          pairingModal.open && pairingModal.type === 'sku'
            ? '系统SKU'
            : pairingModal.open && pairingModal.type === 'warehouse'
              ? '系统仓库代码'
              : '系统渠道代码'
        }
        nameLabel={
          pairingModal.open && pairingModal.type === 'sku'
            ? '系统SKU名称'
            : pairingModal.open && pairingModal.type === 'warehouse'
              ? '系统仓库名称'
              : '系统渠道名称'
        }
        codeName={
          pairingModal.open && pairingModal.type === 'sku'
            ? 'systemSku'
            : pairingModal.open && pairingModal.type === 'warehouse'
              ? 'systemWarehouseCode'
              : 'systemChannelCode'
        }
        nameName={
          pairingModal.open && pairingModal.type === 'sku'
            ? 'systemSkuName'
            : pairingModal.open && pairingModal.type === 'warehouse'
              ? 'systemWarehouseName'
              : 'systemChannelName'
        }
        showCustomerName={pairingModal.open && pairingModal.type === 'sku'}
        customPairingItems={
          pairingModal.open && pairingModal.type === 'warehouse' ? (
            <Form.Item
              name="systemWarehouseCode"
              label="系统仓库"
              rules={[{ required: true, message: '请选择系统仓库' }]}
            >
              <Select
                {...SEARCHABLE_SELECT_PROPS}
                loading={warehouseOptionsLoading}
                options={warehouseOptions}
                placeholder={`请选择要绑定为${selectedPairingRoleLabel}仓的系统仓库`}
              />
            </Form.Item>
          ) : pairingModal.open && pairingModal.type === 'logistics' ? (
            <Form.Item
              name="systemChannelCode"
              label="系统渠道"
              rules={[{ required: true, message: '请选择系统渠道' }]}
            >
              <Select
                {...SEARCHABLE_SELECT_PROPS}
                loading={systemChannelOptionsLoading}
                options={systemChannelOptions}
                placeholder="请选择要绑定的系统渠道"
              />
            </Form.Item>
          ) : null
        }
        onCancel={() => setPairingModal({ open: false })}
        onSubmit={async (values) => {
          if (!pairingModal.open) return false;
          const hide = message.loading('正在配对');
          let resp: API.Result;
          if (pairingModal.type === 'warehouse') {
            const systemWarehouse = warehouseOptions.find(
              (item) => item.value === values.systemWarehouseCode,
            );
            if (!systemWarehouse) {
              hide();
              message.error('请选择系统仓库');
              return false;
            }
            resp = await addWarehousePairing(selectedCode, {
              remark: values.remark,
              pairingRole: selectedPairingRole,
              upstreamWarehouseCode: pairingModal.row.warehouseCode,
              systemWarehouseCode: systemWarehouse.warehouseCode,
              systemWarehouseName: systemWarehouse.warehouseName,
            });
          } else if (pairingModal.type === 'logistics') {
            const systemChannel = systemChannelOptions.find(
              (item) => item.value === values.systemChannelCode,
            );
            if (!systemChannel) {
              hide();
              message.error('请选择系统渠道');
              return false;
            }
            resp = await addLogisticsChannelPairing(selectedCode, {
              remark: values.remark,
              pairingRole: selectedPairingRole,
              upstreamChannelCode: pairingModal.row.channelCode,
              systemChannelCode: systemChannel.systemChannelCode,
              systemChannelName: systemChannel.systemChannelName,
            });
          } else {
            resp = await addSkuPairing(selectedCode, {
              ...values,
              masterSku: pairingModal.row.masterSku,
            });
          }
          hide();
          const ok = resultOk(resp, '配对成功');
          if (ok) {
            setPairingModal({ open: false });
            warehouseActionRef.current?.reload();
            logisticsActionRef.current?.reload();
            skuActionRef.current?.reload();
          }
          return ok;
        }}
      />
    </PageContainer>
  );
}
