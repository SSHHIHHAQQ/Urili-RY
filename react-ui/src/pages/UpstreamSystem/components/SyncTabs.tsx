import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Popconfirm, Space, Tabs, Tag, Typography } from 'antd';
import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import {
  deleteLogisticsChannelPairing,
  deleteWarehousePairing,
  getLogisticsChannelPairings,
  getLogisticsChannelSyncList,
  getRequestLogList,
  getWarehousePairings,
  getWarehouseSyncList,
} from '@/services/integration/upstreamSystem';
import {
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { resultOk, statusTag } from '../helpers';
import styles from '../style.module.css';
import type { LogisticsRow, PairingModalState, WarehouseRow } from '../types';
import SkuSyncPanel from './SkuSyncPanel';

type ActionRef = MutableRefObject<ActionType | null>;

type SyncTabsProps = {
  access: { hasPerms: (permission: string) => boolean };
  logActionRef: ActionRef;
  logisticsActionRef: ActionRef;
  onSkuSynced?: () => void;
  selectedConnection: API.Integration.UpstreamConnection;
  setPairingModal: Dispatch<SetStateAction<PairingModalState>>;
  skuActionRef: ActionRef;
  warehouseActionRef: ActionRef;
};

export default function SyncTabs({
  access,
  logActionRef,
  logisticsActionRef,
  onSkuSynced,
  selectedConnection,
  setPairingModal,
  skuActionRef,
  warehouseActionRef,
}: SyncTabsProps) {
  const selectedCode = selectedConnection.connectionCode;

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
                title="确认解除仓库配对？"
                onConfirm={async () => {
                  if (!record.warehousePairingId) return;
                  const ok = resultOk(
                    await deleteWarehousePairing(record.warehousePairingId),
                    '已解除配对',
                  );
                  if (ok) warehouseActionRef.current?.reload();
                }}
              >
                <Button
                  type="link"
                  size="small"
                  hidden={!access.hasPerms('integration:upstream:pair')}
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
                hidden={!access.hasPerms('integration:upstream:pair')}
                onClick={() =>
                  setPairingModal({
                    open: true,
                    type: 'warehouse',
                    row: record,
                  })
                }
              >
                配对
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
            record.pairings.map((pairing) => (
              <Popconfirm
                key={pairing.logisticsChannelPairingId}
                title="确认解除物流渠道配对？"
                onConfirm={async () => {
                  const ok = resultOk(
                    await deleteLogisticsChannelPairing(
                      pairing.logisticsChannelPairingId,
                    ),
                    '已解除配对',
                  );
                  if (ok) logisticsActionRef.current?.reload();
                }}
              >
                <Tag color="blue">
                  {pairing.systemChannelCode} / {pairing.systemChannelName}
                </Tag>
              </Popconfirm>
            ))
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
          hidden={!access.hasPerms('integration:upstream:pair')}
          onClick={() =>
            setPairingModal({ open: true, type: 'logistics', row: record })
          }
        >
          配对
        </Button>,
      ],
    },
  ];

  const logColumns: ProColumns<API.Integration.RequestLog>[] = [
    { title: '时间', dataIndex: 'createTime', width: 170, search: false },
    { title: '操作', dataIndex: 'operation', width: 150 },
    {
      title: '结果',
      dataIndex: 'status',
      width: 100,
      render: (_, record) => (
        <Tag color={record.status === 'SUCCESS' ? 'green' : 'red'}>
          {record.status}
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
                    const requestCode = selectedCode;
                    const [syncResp, pairingResp] = await Promise.all([
                      getWarehouseSyncList(requestCode),
                      getWarehousePairings(requestCode),
                    ]);
                    const pairingMap = new Map(
                      (pairingResp.data || []).map((item) => [
                        item.upstreamWarehouseCode,
                        item,
                      ]),
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
                      } else {
                        groups.set(item.channelCode, {
                          ...item,
                          warehouseCodes: item.warehouseCode,
                          pairings: [],
                        });
                      }
                    });
                    const rows = Array.from(groups.values()).map((row) => ({
                      ...row,
                      pairings: (pairingResp.data || []).filter(
                        (pairing) =>
                          pairing.upstreamChannelCode === row.channelCode,
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
            key: 'logs',
            label: '请求日志',
            children: (
              <ProTable<API.Integration.RequestLog>
                actionRef={logActionRef}
                className={`${styles.fillTable} upstream-fill-table`}
                key={`logs-${selectedCode}`}
                rowKey="requestLogId"
                columns={logColumns}
                params={{ selectedCode }}
                request={async (params) => {
                  const requestCode = selectedCode;
                  const resp = await getRequestLogList(requestCode, params);
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
