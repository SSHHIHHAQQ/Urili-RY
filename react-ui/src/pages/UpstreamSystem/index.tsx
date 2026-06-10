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
  getUpstreamConnectionList,
  syncUpstreamConnection,
  updateUpstreamConnection,
  updateUpstreamConnectionOrder,
  updateUpstreamCredentials,
  updateUpstreamStatus,
} from '@/services/integration/upstreamSystem';
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
import {
  normalizeSettlementTypeValue,
  pairingRoleText,
  syncTypeText,
} from './constants';
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

const formatSyncItemResult = (item: API.Integration.SyncItemResult) => {
  const label = syncTypeText[item.syncType || ''] || item.syncType || '同步项';
  if (item.status === 'FAILED') {
    return `${label}失败：${item.errorMessage || '-'}`;
  }
  const changedTotal = (item.insertedCount || 0) + (item.changedCount || 0);
  return `${label}：拉取${item.pulledCount || item.count || 0}，新增${item.insertedCount || 0}，变更${item.changedCount || 0}，停用${item.disabledCount || 0}，未变${item.unchangedCount || 0}，写入${changedTotal}`;
};

const formatSyncResult = (data?: API.Integration.SyncResult) => {
  if (data?.items?.length) {
    return data.items.map(formatSyncItemResult).join('；');
  }
  return `仓库 ${data?.warehouseCount || 0}，渠道 ${data?.logisticsChannelCount || 0}，SKU ${data?.skuCount || 0}，尺寸重量 ${data?.skuDimensionCount || 0}，库存 ${data?.warehouseStockCount || 0}`;
};

export default function UpstreamSystemPage() {
  const access = useAccess();
  const canListUpstreamConnections = access.hasPerms('integration:upstream:list');
  const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list');
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

  useEffect(() => {
    if (pairingModal.open && pairingModal.type === 'warehouse') {
      loadWarehouseOptions(selectedCode, selectedPairingRole);
    }
  }, [loadWarehouseOptions, pairingModal, selectedCode, selectedPairingRole]);

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
    const resp = await syncUpstreamConnection(record.connectionCode, {
      syncTypes: syncModal.syncTypes,
    });
    setSyncModal((current) => ({ ...current, submitting: false }));
    if (resp.code === 200) {
      const hasFailed = resp.data?.items?.some((item) => item.status === 'FAILED');
      const feedback = `同步完成：${formatSyncResult(resp.data)}`;
      if (hasFailed) {
        message.warning(feedback);
      } else {
        message.success(feedback);
      }
      setSyncModal({
        open: false,
        syncTypes: defaultManualSyncTypes,
        submitting: false,
      });
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

  const logisticsWarehouseOptions =
    pairingModal.open && pairingModal.type === 'logistics'
      ? pairingModal.row.warehouseItems.map((item) => ({
          label: item.systemWarehouseCode
            ? `${item.warehouseCode} -> ${item.systemWarehouseCode} / ${item.systemWarehouseName || '-'}`
            : `${item.warehouseCode}（请先配对${selectedPairingRoleLabel}仓）`,
          value: item.warehouseCode,
          disabled: !item.systemWarehouseCode,
          searchText: `${item.warehouseCode} ${item.systemWarehouseCode || ''} ${
            item.systemWarehouseName || ''
          }`,
        }))
      : [];

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
        extraItems={
          pairingModal.open && pairingModal.type === 'logistics' ? (
            <Form.Item
              name="upstreamWarehouseCode"
              label="领星仓库"
              rules={[{ required: true, message: '请选择领星仓库' }]}
            >
              <Select
                {...SEARCHABLE_SELECT_PROPS}
                options={logisticsWarehouseOptions}
              />
            </Form.Item>
          ) : null
        }
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
            const warehouseContext = pairingModal.row.warehouseItems.find(
              (item) => item.warehouseCode === values.upstreamWarehouseCode,
            );
            if (!warehouseContext?.systemWarehouseCode) {
              hide();
              message.error(`请先配对${selectedPairingRoleLabel}仓`);
              return false;
            }
            resp = await addLogisticsChannelPairing(selectedCode, {
              ...values,
              pairingRole: selectedPairingRole,
              systemWarehouseCode: warehouseContext.systemWarehouseCode,
              upstreamChannelCode: pairingModal.row.channelCode,
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
