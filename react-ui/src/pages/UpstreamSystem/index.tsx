import { type ActionType, PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Empty } from 'antd';
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
import { message } from '@/utils/feedback';
import ConnectionModal, {
  type ConnectionModalMode,
} from './components/ConnectionModal';
import ConnectionSidebar from './components/ConnectionSidebar';
import ConnectionSummary from './components/ConnectionSummary';
import PairingModal from './components/PairingModal';
import SyncTabs from './components/SyncTabs';
import { resultOk } from './helpers';
import './style.css';
import styles from './style.module.css';
import type { PairingModalState } from './types';

const connectionPageSize = 200;

export default function UpstreamSystemPage() {
  const access = useAccess();
  const warehouseActionRef = useRef<ActionType>(null);
  const logisticsActionRef = useRef<ActionType>(null);
  const skuActionRef = useRef<ActionType>(null);
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

  const fetchConnections = useCallback(async (preferredCode?: string) => {
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
  }, []);

  useEffect(() => {
    fetchConnections();
  }, [fetchConnections]);

  const selectedCode = selectedConnection?.connectionCode || '';

  const reloadTabs = () => {
    warehouseActionRef.current?.reload();
    logisticsActionRef.current?.reload();
    skuActionRef.current?.reload();
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

  const handleSync = async (record: API.Integration.UpstreamConnection) => {
    const hide = message.loading('正在同步');
    const resp = await syncUpstreamConnection(record.connectionCode);
    hide();
    if (resp.code === 200) {
      message.success(
        `同步完成：仓库 ${resp.data?.warehouseCount || 0}，渠道 ${resp.data?.logisticsChannelCount || 0}，SKU ${resp.data?.skuCount || 0}`,
      );
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
          hide();
          const ok = resultOk(resp, '保存成功');
          if (ok) {
            setConnectionModal({ open: false, mode: 'create' });
            await fetchConnections(
              modalRecord?.connectionCode || values.connectionCode,
            );
          }
          return ok;
        }}
      />

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
        onCancel={() => setPairingModal({ open: false })}
        onSubmit={async (values) => {
          if (!pairingModal.open) return false;
          const hide = message.loading('正在配对');
          const resp =
            pairingModal.type === 'warehouse'
              ? await addWarehousePairing(selectedCode, {
                  ...values,
                  upstreamWarehouseCode: pairingModal.row.warehouseCode,
                })
              : pairingModal.type === 'logistics'
                ? await addLogisticsChannelPairing(selectedCode, {
                    ...values,
                    upstreamChannelCode: pairingModal.row.channelCode,
                  })
                : await addSkuPairing(selectedCode, {
                    ...values,
                    masterSku: pairingModal.row.masterSku,
                  });
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
