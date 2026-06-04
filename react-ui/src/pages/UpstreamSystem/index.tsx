import { message } from '@/utils/feedback';
import {
  addLogisticsChannelPairing,
  addSkuPairing,
  addUpstreamConnection,
  addWarehousePairing,
  authorizeUpstreamConnection,
  getUpstreamConnectionList,
  syncUpstreamConnection,
  updateUpstreamConnection,
  updateUpstreamCredentials,
  updateUpstreamStatus,
} from '@/services/integration/upstreamSystem';
import {
  CheckCircleOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { PageContainer, type ProColumns, ProTable, type ActionType } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Empty, Popconfirm } from 'antd';
import { useRef, useState } from 'react';
import ConnectionModal, { type ConnectionModalMode } from './components/ConnectionModal';
import PairingModal from './components/PairingModal';
import SyncTabs from './components/SyncTabs';
import { resultOk, statusTag } from './helpers';
import type { PairingModalState } from './types';

export default function UpstreamSystemPage() {
  const access = useAccess();
  const connectionActionRef = useRef<ActionType>(null);
  const warehouseActionRef = useRef<ActionType>(null);
  const logisticsActionRef = useRef<ActionType>(null);
  const skuActionRef = useRef<ActionType>(null);
  const logActionRef = useRef<ActionType>(null);
  const [selectedConnection, setSelectedConnection] = useState<API.Integration.UpstreamConnection>();
  const [connectionModal, setConnectionModal] = useState<{
    open: boolean;
    mode: ConnectionModalMode;
    record?: API.Integration.UpstreamConnection;
  }>({ open: false, mode: 'create' });
  const [pairingModal, setPairingModal] = useState<PairingModalState>({ open: false });

  const reloadCurrent = () => {
    connectionActionRef.current?.reload();
    warehouseActionRef.current?.reload();
    logisticsActionRef.current?.reload();
    skuActionRef.current?.reload();
    logActionRef.current?.reload();
  };

  const requireConnectionCode = () => selectedConnection?.connectionCode || '';

  const connectionColumns: ProColumns<API.Integration.UpstreamConnection>[] = [
    { title: '主仓名称', dataIndex: 'masterWarehouseName', width: 160 },
    { title: '接入编号', dataIndex: 'connectionCode', width: 190, copyable: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      search: false,
      render: (_, record) => statusTag(record.status),
    },
    { title: 'Key', dataIndex: 'appKeyMask', width: 120, search: false },
    { title: '结算类型', dataIndex: 'settlementType', width: 140 },
    { title: '最近授权', dataIndex: 'lastAuthorizedTime', width: 170, search: false },
    { title: '最近同步', dataIndex: 'lastSyncTime', width: 170, search: false },
    {
      title: '操作',
      valueType: 'option',
      width: 300,
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          size="small"
          icon={<EditOutlined />}
          hidden={!access.hasPerms('integration:upstream:edit')}
          onClick={() => setConnectionModal({ open: true, mode: 'edit', record })}
        >
          编辑
        </Button>,
        <Button
          key="credential"
          type="link"
          size="small"
          icon={<KeyOutlined />}
          hidden={!access.hasPerms('integration:upstream:credential')}
          onClick={() => setConnectionModal({ open: true, mode: 'credential', record })}
        >
          授权
        </Button>,
        <Button
          key="authorize"
          type="link"
          size="small"
          icon={<SafetyCertificateOutlined />}
          hidden={!access.hasPerms('integration:upstream:sync')}
          onClick={async () => {
            const hide = message.loading('正在校验授权');
            const ok = resultOk(await authorizeUpstreamConnection(record.connectionCode), '授权校验通过');
            hide();
            if (ok) reloadCurrent();
          }}
        >
          校验
        </Button>,
        <Button
          key="sync"
          type="link"
          size="small"
          icon={<SyncOutlined />}
          hidden={!access.hasPerms('integration:upstream:sync')}
          onClick={async () => {
            const hide = message.loading('正在同步');
            const resp = await syncUpstreamConnection(record.connectionCode);
            hide();
            if (resp.code === 200) {
              message.success(`同步完成：仓库 ${resp.data?.warehouseCount || 0}，渠道 ${resp.data?.logisticsChannelCount || 0}，SKU ${resp.data?.skuCount || 0}`);
              setSelectedConnection(record);
              reloadCurrent();
            } else {
              message.error(resp.msg);
            }
          }}
        >
          同步
        </Button>,
        <Popconfirm
          key="status"
          title={record.status === 'ENABLED' ? '确认停用该主仓接入？' : '确认启用该主仓接入？'}
          onConfirm={async () => {
            const nextStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
            const ok = resultOk(await updateUpstreamStatus(record.connectionCode, nextStatus), '状态已更新');
            if (ok) reloadCurrent();
          }}
        >
          <Button
            type="link"
            size="small"
            icon={record.status === 'ENABLED' ? <StopOutlined /> : <CheckCircleOutlined />}
            hidden={!access.hasPerms('integration:upstream:edit')}
          >
            {record.status === 'ENABLED' ? '停用' : '启用'}
          </Button>
        </Popconfirm>,
      ],
    },
  ];

  const selectedCode = requireConnectionCode();

  return (
    <PageContainer>
      <ProTable<API.Integration.UpstreamConnection>
        actionRef={connectionActionRef}
        rowKey="connectionCode"
        columns={connectionColumns}
        request={async (params) => {
          const resp = await getUpstreamConnectionList(params);
          if (!selectedConnection && resp.rows?.length) {
            setSelectedConnection(resp.rows[0]);
          }
          return { data: resp.rows || [], total: resp.total || 0, success: resp.code === 200 };
        }}
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            hidden={!access.hasPerms('integration:upstream:add')}
            onClick={() => setConnectionModal({ open: true, mode: 'create' })}
          >
            新增主仓接入
          </Button>,
        ]}
        onRow={(record) => ({
          onClick: () => setSelectedConnection(record),
        })}
        pagination={{ pageSize: 10 }}
        search={{ labelWidth: 90 }}
      />

      {selectedConnection ? (
        <SyncTabs
          access={access}
          logActionRef={logActionRef}
          logisticsActionRef={logisticsActionRef}
          selectedConnection={selectedConnection}
          setPairingModal={setPairingModal}
          skuActionRef={skuActionRef}
          warehouseActionRef={warehouseActionRef}
        />
      ) : (
        <Empty description="暂无主仓接入" />
      )}

      <ConnectionModal
        mode={connectionModal.mode}
        open={connectionModal.open}
        record={connectionModal.record}
        onCancel={() => setConnectionModal({ open: false, mode: 'create' })}
        onSubmit={async (values) => {
          const modalRecord = connectionModal.record;
          if (connectionModal.mode !== 'create' && !modalRecord?.connectionCode) {
            return false;
          }
          const hide = message.loading('正在保存');
          let resp: API.Result;
          if (connectionModal.mode === 'create') {
            resp = await addUpstreamConnection(values);
          } else if (connectionModal.mode === 'edit') {
            if (!modalRecord) return false;
            resp = await updateUpstreamConnection(modalRecord.connectionCode, values);
          } else {
            if (!modalRecord) return false;
            resp = await updateUpstreamCredentials(modalRecord.connectionCode, values);
          }
          hide();
          const ok = resultOk(resp, '保存成功');
          if (ok) {
            setConnectionModal({ open: false, mode: 'create' });
            connectionActionRef.current?.reload();
          }
          return ok;
        }}
      />

      <PairingModal
        open={pairingModal.open}
        title={pairingModal.open && pairingModal.type === 'warehouse' ? '仓库配对' : pairingModal.open && pairingModal.type === 'logistics' ? '物流渠道配对' : 'SKU配对'}
        upstreamLabel={pairingModal.open && pairingModal.type === 'warehouse' ? '领星仓库' : pairingModal.open && pairingModal.type === 'logistics' ? '领星渠道' : '领星masterSku'}
        upstreamValue={
          pairingModal.open && pairingModal.type === 'warehouse'
            ? `${pairingModal.row.warehouseCode} / ${pairingModal.row.warehouseName}`
            : pairingModal.open && pairingModal.type === 'logistics'
              ? `${pairingModal.row.channelCode} / ${pairingModal.row.channelName}`
              : pairingModal.open
                ? `${pairingModal.row.masterSku} / ${pairingModal.row.masterProductName}`
                : ''
        }
        codeLabel={pairingModal.open && pairingModal.type === 'sku' ? '系统SKU' : pairingModal.open && pairingModal.type === 'warehouse' ? '系统仓库代码' : '系统渠道代码'}
        nameLabel={pairingModal.open && pairingModal.type === 'sku' ? '系统SKU名称' : pairingModal.open && pairingModal.type === 'warehouse' ? '系统仓库名称' : '系统渠道名称'}
        codeName={pairingModal.open && pairingModal.type === 'sku' ? 'systemSku' : pairingModal.open && pairingModal.type === 'warehouse' ? 'systemWarehouseCode' : 'systemChannelCode'}
        nameName={pairingModal.open && pairingModal.type === 'sku' ? 'systemSkuName' : pairingModal.open && pairingModal.type === 'warehouse' ? 'systemWarehouseName' : 'systemChannelName'}
        onCancel={() => setPairingModal({ open: false })}
        onSubmit={async (values) => {
          if (!pairingModal.open) return false;
          const hide = message.loading('正在配对');
          const resp =
            pairingModal.type === 'warehouse'
              ? await addWarehousePairing(selectedCode, { ...values, upstreamWarehouseCode: pairingModal.row.warehouseCode })
              : pairingModal.type === 'logistics'
                ? await addLogisticsChannelPairing(selectedCode, { ...values, upstreamChannelCode: pairingModal.row.channelCode })
                : await addSkuPairing(selectedCode, { ...values, masterSku: pairingModal.row.masterSku });
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
