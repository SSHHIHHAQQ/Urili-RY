import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tabs, Tag, Typography } from 'antd';
import { useRef } from 'react';
import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import {
  cancelSyncTask,
  deleteLogisticsChannelPairing,
  deleteWarehousePairing,
  getLogisticsChannelPairings,
  getLogisticsChannelSyncList,
  getRequestLogList,
  getSyncTaskList,
  getWarehousePairings,
  getWarehouseSyncList,
  retrySyncTask,
} from '@/services/integration/upstreamSystem';
import {
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import {
  normalizeSettlementTypeValue,
  pairingRoleTagColor,
  pairingRoleText,
  requestOperationText,
  requestResultText,
  syncTaskStatusColor,
  syncTaskStatusText,
  syncTypeText,
} from '../constants';
import { resultOk, statusTag } from '../helpers';
import styles from '../style.module.css';
import type { LogisticsRow, PairingModalState, WarehouseRow } from '../types';
import SkuDimensionPanel from './SkuDimensionPanel';
import SkuInventoryPanel from './SkuInventoryPanel';
import SkuSyncPanel from './SkuSyncPanel';

type ActionRef = MutableRefObject<ActionType | null>;

type SyncTabsProps = {
  access: { hasPerms: (permission: string) => boolean };
  dimensionActionRef: ActionRef;
  inventoryActionRef: ActionRef;
  logActionRef: ActionRef;
  logisticsActionRef: ActionRef;
  onSkuSynced?: () => void;
  selectedConnection: API.Integration.UpstreamConnection;
  setPairingModal: Dispatch<SetStateAction<PairingModalState>>;
  skuActionRef: ActionRef;
  warehouseActionRef: ActionRef;
};

const requestResultLabel = (value?: string) => {
  const normalizedValue = value?.toUpperCase() || '';
  return requestResultText[normalizedValue] || value || '-';
};

const requestResultColor = (value?: string) => {
  const normalizedValue = value?.toUpperCase();
  if (normalizedValue === 'SUCCESS') return 'green';
  if (normalizedValue === 'STARTED') return 'processing';
  if (normalizedValue === 'TIMEOUT') return 'orange';
  if (['FAILURE', 'FAILED', 'ERROR'].includes(normalizedValue || '')) return 'red';
  return 'default';
};

export default function SyncTabs({
  access,
  dimensionActionRef,
  inventoryActionRef,
  logActionRef,
  logisticsActionRef,
  onSkuSynced,
  selectedConnection,
  setPairingModal,
  skuActionRef,
  warehouseActionRef,
}: SyncTabsProps) {
  const selectedCode = selectedConnection.connectionCode;
  const canQueryUpstream = access.hasPerms('integration:upstream:query');
  const canPairUpstream = access.hasPerms('integration:upstream:pair');
  const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list');
  const canQueryInventory = access.hasPerms('integration:upstream:inventoryQuery');
  const canViewLogs = access.hasPerms('integration:upstream:log');
  const canViewSyncTasks = access.hasPerms('integration:upstream:task:list');
  const canRetrySyncTask = access.hasPerms('integration:upstream:task:retry');
  const canCancelSyncTask = access.hasPerms('integration:upstream:task:cancel');
  const taskActionRef = useRef<ActionType>(null);
  const normalizedSettlementType = normalizeSettlementTypeValue(
    selectedConnection.settlementType,
  );
  const currentPairingRole =
    normalizedSettlementType === 'self-operated-receivable'
      ? 'QUOTE'
      : 'FULFILLMENT';
  const currentPairingRoleLabel =
    pairingRoleText[currentPairingRole] || currentPairingRole;

  const warehouseColumns: ProColumns<WarehouseRow>[] = [
    { title: '领星仓库代码', dataIndex: 'warehouseCode', width: 150 },
    { title: '领星仓库名称', dataIndex: 'warehouseName', width: 180 },
    { title: '国家/地区', dataIndex: 'countryCode', width: 100 },
    {
      title: '同步状态',
      dataIndex: 'status',
      width: 110,
      render: (_, record) => statusTag(record.status),
    },
    {
      title: '配对用途',
      dataIndex: 'pairingRole',
      width: 100,
      search: false,
      render: (_, record) => (
        <Tag color={pairingRoleTagColor[record.pairingRole || currentPairingRole]}>
          {pairingRoleText[record.pairingRole || currentPairingRole] ||
            currentPairingRoleLabel}
        </Tag>
      ),
    },
    {
      title: '系统仓库代码',
      dataIndex: 'systemWarehouseCode',
      width: 150,
      search: false,
    },
    {
      title: '系统仓库名称',
      dataIndex: 'systemWarehouseName',
      width: 180,
      search: false,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 160,
      render: (_, record) =>
        record.warehousePairingId
          ? [
              <Popconfirm
                key="unpair"
                title={`确认解除${currentPairingRoleLabel}仓配对？`}
                onConfirm={async () => {
                  if (!record.warehousePairingId) return;
                  const ok = resultOk(
                    await deleteWarehousePairing(selectedCode, record.warehousePairingId),
                    '已解除配对',
                  );
                  if (ok) warehouseActionRef.current?.reload();
                }}
              >
                <Button
                  type="link"
                  size="small"
                  hidden={!canPairUpstream}
                >
                  解除
                </Button>
              </Popconfirm>,
            ]
          : [
              <Button
                key="pair"
                type="link"
                size="small"
                hidden={!canPairUpstream || !canQueryOfficialWarehouses}
                onClick={() =>
                  setPairingModal({
                    open: true,
                    type: 'warehouse',
                    row: record,
                  })
                }
              >
                配为{currentPairingRoleLabel}仓
              </Button>,
            ],
    },
  ];

  const logisticsColumns: ProColumns<LogisticsRow>[] = [
    { title: '领星渠道代码', dataIndex: 'channelCode', width: 150 },
    { title: '领星渠道名称', dataIndex: 'channelName', width: 200 },
    {
      title: '涉及仓库',
      dataIndex: 'warehouseCodes',
      width: 200,
      search: false,
    },
    {
      title: '系统渠道',
      dataIndex: 'pairings',
      search: false,
      render: (_, record) => (
        <Space wrap>
          {record.pairings.length === 0 ? (
            <Typography.Text type="secondary">未配对</Typography.Text>
          ) : (
            record.pairings.map((pairing) => {
              const tag = (
                <Tag key={pairing.logisticsChannelPairingId} color="blue">
                  {pairing.systemChannelCode} / {pairing.systemChannelName}
                </Tag>
              );
              return canPairUpstream ? (
                <Popconfirm
                  key={pairing.logisticsChannelPairingId}
                  title="确认解除物流渠道配对？"
                  onConfirm={async () => {
                    const ok = resultOk(
                      await deleteLogisticsChannelPairing(
                        selectedCode,
                        pairing.logisticsChannelPairingId,
                      ),
                      '已解除配对',
                    );
                    if (ok) logisticsActionRef.current?.reload();
                  }}
                >
                  {tag}
                </Popconfirm>
              ) : (
                <span key={pairing.logisticsChannelPairingId}>{tag}</span>
              );
            })
          )}
        </Space>
      ),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
      render: (_, record) => [
        <Button
          key="pair"
          type="link"
          size="small"
          hidden={!canPairUpstream}
          onClick={() =>
            setPairingModal({ open: true, type: 'logistics', row: record })
          }
        >
          配为{currentPairingRoleLabel}渠道
        </Button>,
      ],
    },
  ];

  const taskColumns: ProColumns<API.Integration.SyncTask>[] = [
    { title: '创建时间', dataIndex: 'createTime', width: 170, search: false },
    {
      title: '同步项',
      dataIndex: 'syncType',
      width: 150,
      renderText: (value) => syncTypeText[value] || value || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (_, record) => {
        const status = record.status?.toUpperCase() || '';
        return (
          <Tag color={syncTaskStatusColor[status] || 'default'}>
            {syncTaskStatusText[status] || record.status || '-'}
          </Tag>
        );
      },
    },
    {
      title: '模式',
      dataIndex: 'mode',
      width: 110,
      renderText: (value) => (value === 'SCHEDULED' ? '定时' : value === 'SELECTED' ? '指定SKU' : '手动'),
    },
    { title: '批次号', dataIndex: 'syncBatchId', width: 220, copyable: true, search: false },
    { title: '开始时间', dataIndex: 'startedTime', width: 170, search: false },
    { title: '结束时间', dataIndex: 'finishedTime', width: 170, search: false },
    {
      title: '拉取/变更',
      dataIndex: 'pulledCount',
      width: 120,
      search: false,
      render: (_, record) => `${record.pulledCount ?? 0}/${record.changedCount ?? 0}`,
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      ellipsis: true,
      search: false,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 150,
      render: (_, record) => {
        const status = record.status?.toUpperCase();
        const canRetry = ['FAILED', 'TIMEOUT', 'SKIPPED', 'CANCELED'].includes(status || '');
        const canCancel = ['PENDING', 'CLAIMED'].includes(status || '');
        return [
          <Button
            key="retry"
            type="link"
            size="small"
            hidden={!canRetrySyncTask || !canRetry}
            onClick={async () => {
              const ok = resultOk(
                await retrySyncTask(selectedCode, record.taskId),
                '已提交重试任务',
              );
              if (ok) taskActionRef.current?.reload();
            }}
          >
            重试
          </Button>,
          <Popconfirm
            key="cancel"
            title="确认取消该同步任务？"
            onConfirm={async () => {
              const ok = resultOk(
                await cancelSyncTask(selectedCode, record.taskId),
                '已取消任务',
              );
              if (ok) taskActionRef.current?.reload();
            }}
          >
            <Button
              type="link"
              size="small"
              hidden={!canCancelSyncTask || !canCancel}
            >
              取消
            </Button>
          </Popconfirm>,
        ];
      },
    },
  ];

  const logColumns: ProColumns<API.Integration.RequestLog>[] = [
    { title: '时间', dataIndex: 'createTime', width: 170, search: false },
    {
      title: '类型',
      dataIndex: 'operation',
      width: 190,
      renderText: (value) => requestOperationText[value] || value || '-',
    },
    {
      title: '结果',
      dataIndex: 'status',
      width: 100,
      render: (_, record) => (
        <Tag color={requestResultColor(record.status)}>
          {requestResultLabel(record.status)}
        </Tag>
      ),
    },
    { title: '耗时(ms)', dataIndex: 'durationMs', width: 100, search: false },
    {
      title: '错误码',
      dataIndex: 'externalErrorCode',
      width: 130,
      search: false,
    },
    {
      title: '错误信息',
      dataIndex: 'externalErrorMessage',
      ellipsis: true,
      search: false,
    },
    {
      title: 'TraceId',
      dataIndex: 'traceId',
      width: 220,
      copyable: true,
      search: false,
    },
  ];

  return (
    <div className={`${styles.syncTabs} upstream-sync-tabs`}>
      <Tabs
        items={[
          {
            key: 'warehouse',
            label: '领星仓库同步清单',
            children: (
              <div className={styles.tablePane}>
                <ProTable<WarehouseRow>
                  actionRef={warehouseActionRef}
                  className={`${styles.fillTable} upstream-fill-table`}
                  key={`warehouse-${selectedCode}`}
                  rowKey={(record) => `${selectedCode}:${record.warehouseCode}`}
                  columns={warehouseColumns}
                  search={false}
                  options={false}
                  toolBarRender={false}
                  scroll={getProTableScroll(1100)}
                  params={{ selectedCode }}
                  request={async () => {
                    if (!canQueryUpstream) {
                      return { data: [], success: true };
                    }
                    const requestCode = selectedCode;
                    const [syncResp, pairingResp] = await Promise.all([
                      getWarehouseSyncList(requestCode),
                      getWarehousePairings(requestCode),
                    ]);
                    const pairingMap = new Map(
                      (pairingResp.data || [])
                        .filter(
                          (item) =>
                            (item.pairingRole || 'FULFILLMENT') ===
                            currentPairingRole,
                        )
                        .map((item) => [item.upstreamWarehouseCode, item]),
                    );
                    const rows = (syncResp.data || []).map((item) => ({
                      ...item,
                      ...(pairingMap.get(item.warehouseCode) || {}),
                    }));
                    return { data: rows, success: syncResp.code === 200 };
                  }}
                  pagination={getProTablePagination(10)}
                />
              </div>
            ),
          },
          {
            key: 'logistics',
            label: '领星物流渠道同步清单',
            children: (
              <div className={styles.tablePane}>
                <ProTable<LogisticsRow>
                  actionRef={logisticsActionRef}
                  className={`${styles.fillTable} upstream-fill-table`}
                  key={`logistics-${selectedCode}`}
                  rowKey={(record) => `${selectedCode}:${record.channelCode}`}
                  columns={logisticsColumns}
                  search={false}
                  options={false}
                  toolBarRender={false}
                  scroll={getProTableScroll(1000)}
                  params={{ selectedCode }}
                  request={async () => {
                    if (!canQueryUpstream) {
                      return { data: [], success: true };
                    }
                    const requestCode = selectedCode;
                    const [syncResp, pairingResp] = await Promise.all([
                      getLogisticsChannelSyncList(requestCode),
                      getLogisticsChannelPairings(requestCode),
                    ]);
                    const groups = new Map<string, LogisticsRow>();
                    (syncResp.data || []).forEach((item) => {
                      const current = groups.get(item.channelCode);
                      if (current) {
                        current.warehouseCodes = Array.from(
                          new Set(
                            `${current.warehouseCodes},${item.warehouseCode}`.split(
                              ',',
                            ),
                          ),
                        ).join(',');
                        current.warehouseItems.push(item);
                      } else {
                        groups.set(item.channelCode, {
                          ...item,
                          warehouseCodes: item.warehouseCode,
                          warehouseItems: [item],
                          pairings: [],
                        });
                      }
                    });
                    const rows = Array.from(groups.values()).map((row) => ({
                      ...row,
                      pairings: (pairingResp.data || []).filter(
                        (pairing) =>
                          pairing.upstreamChannelCode === row.channelCode &&
                          (pairing.pairingRole || 'FULFILLMENT') ===
                            currentPairingRole,
                      ),
                    }));
                    return { data: rows, success: syncResp.code === 200 };
                  }}
                  pagination={getProTablePagination(10)}
                />
              </div>
            ),
          },
          {
            key: 'sku',
            label: '领星SKU同步清单',
            children: (
              <SkuSyncPanel
                access={access}
                actionRef={skuActionRef}
                onSynced={onSkuSynced}
                selectedCode={selectedCode}
                setPairingModal={setPairingModal}
              />
            ),
          },
          {
            key: 'dimensions',
            label: '仓库尺寸重量',
            children: (
              <SkuDimensionPanel
                access={access}
                actionRef={dimensionActionRef}
                selectedCode={selectedCode}
              />
            ),
          },
          {
            key: 'inventory',
            label: 'SKU库存同步清单',
            children: (
              <SkuInventoryPanel
                access={access}
                actionRef={inventoryActionRef}
                selectedCode={selectedCode}
              />
            ),
            disabled: !canQueryInventory,
          },
          {
            key: 'tasks',
            label: '同步任务',
            disabled: !canViewSyncTasks,
            children: (
              <ProTable<API.Integration.SyncTask>
                actionRef={taskActionRef}
                className={`${styles.fillTable} upstream-fill-table`}
                key={`tasks-${selectedCode}`}
                rowKey="taskId"
                columns={taskColumns}
                params={{ selectedCode }}
                request={async (params) => {
                  if (!canViewSyncTasks) {
                    return { data: [], total: 0, success: true };
                  }
                  const requestCode = selectedCode;
                  const { current, pageSize, ...rest } = params;
                  const resp = await getSyncTaskList(requestCode, {
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
                pagination={getProTablePagination(10)}
                search={false}
                options={false}
                toolBarRender={false}
                scroll={getProTableScroll(1300)}
              />
            ),
          },
          {
            key: 'logs',
            label: '请求日志',
            disabled: !canViewLogs,
            children: (
              <ProTable<API.Integration.RequestLog>
                actionRef={logActionRef}
                className={`${styles.fillTable} upstream-fill-table`}
                key={`logs-${selectedCode}`}
                rowKey="requestLogId"
                columns={logColumns}
                params={{ selectedCode }}
                request={async (params) => {
                  if (!canViewLogs) {
                    return { data: [], total: 0, success: true };
                  }
                  const requestCode = selectedCode;
                  const { current, pageSize, ...rest } = params;
                  const resp = await getRequestLogList(requestCode, {
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
                pagination={getProTablePagination(10)}
                search={false}
                options={false}
                toolBarRender={false}
                scroll={getProTableScroll(1100)}
              />
            ),
          },
        ]}
      />
    </div>
  );
}
