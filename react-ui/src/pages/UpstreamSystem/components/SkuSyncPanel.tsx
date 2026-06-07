import { SearchOutlined, SyncOutlined } from '@ant-design/icons';
import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Input, Popconfirm, Select, Typography } from 'antd';
import {
  type Dispatch,
  type MutableRefObject,
  type SetStateAction,
  useEffect,
  useState,
} from 'react';
import {
  deleteSkuPairing,
  getSkuSyncList,
  getSkuSyncState,
  syncUpstreamSku,
} from '@/services/integration/upstreamSystem';
import { message } from '@/utils/feedback';
import {
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  skuPairingStatusOptions,
  skuSearchFieldOptions,
  skuSyncItemStatusOptions,
} from '../constants';
import { resultOk, statusTag } from '../helpers';
import styles from '../style.module.css';
import type { PairingModalState } from '../types';

type SkuSyncPanelProps = {
  access: { hasPerms: (permission: string) => boolean };
  actionRef: MutableRefObject<ActionType | null>;
  onSynced?: () => void;
  selectedCode: string;
  setPairingModal: Dispatch<SetStateAction<PairingModalState>>;
};

export default function SkuSyncPanel({
  access,
  actionRef,
  onSynced,
  selectedCode,
  setPairingModal,
}: SkuSyncPanelProps) {
  const [syncing, setSyncing] = useState(false);
  const [syncState, setSyncState] = useState<API.Integration.SkuSyncState>();
  const [searchField, setSearchField] = useState('all');
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [pairingStatus, setPairingStatus] = useState('');
  const [syncStatus, setSyncStatus] = useState('');

  const loadSyncState = async () => {
    if (!selectedCode) {
      setSyncState(undefined);
      return;
    }
    const resp = await getSkuSyncState(selectedCode);
    if (resp.code === 200) {
      setSyncState(resp.data);
    }
  };

  useEffect(() => {
    loadSyncState();
  }, [selectedCode]);

  const skuColumns: ProColumns<API.Integration.SkuSyncItem>[] = [
    {
      title: '领星 masterSku',
      dataIndex: 'masterSku',
      width: 180,
      copyable: true,
    },
    {
      title: '领星产品名',
      dataIndex: 'masterProductName',
      width: 240,
      ellipsis: true,
    },
    {
      title: '同步状态',
      dataIndex: 'status',
      width: 100,
      render: (_, record) => statusTag(record.status),
    },
    {
      title: '配对状态',
      dataIndex: 'pairingStatus',
      width: 100,
      render: (_, record) => statusTag(record.pairingStatus),
    },
    { title: '系统SKU', dataIndex: 'systemSku', width: 160 },
    {
      title: '系统SKU名称',
      dataIndex: 'systemSkuName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '客户名称',
      dataIndex: 'customerName',
      width: 160,
      ellipsis: true,
    },
    { title: '最近发现', dataIndex: 'lastSeenTime', width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      render: (_, record) =>
        record.skuPairingId
          ? [
              <Popconfirm
                key="unpair"
                title="确认解除SKU配对？"
                onConfirm={async () => {
                  if (!record.skuPairingId) return;
                  const ok = resultOk(
                    await deleteSkuPairing(selectedCode, record.skuPairingId),
                    '已解除配对',
                  );
                  if (ok) actionRef.current?.reload();
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
                  setPairingModal({ open: true, type: 'sku', row: record })
                }
              >
                配对
              </Button>,
            ],
    },
  ];

  const triggerSkuSync = async () => {
    setSyncing(true);
    const hide = message.loading('正在同步SKU');
    const resp = await syncUpstreamSku(selectedCode);
    hide();
    setSyncing(false);
    if (resp.code === 200) {
      message.success(`SKU同步完成：${resp.data?.skuCount || 0}`);
      await loadSyncState();
      actionRef.current?.reload();
      onSynced?.();
    } else {
      message.error(resp.msg);
      await loadSyncState();
    }
  };

  return (
    <div className={styles.skuPanel}>
      <div className={styles.skuToolbar}>
        <div className={styles.skuState}>
          <Typography.Text type="secondary">同步状态</Typography.Text>
          {statusTag(syncState?.status || 'NEVER')}
          <Typography.Text>
            上次成功 {syncState?.lastSuccessTime || '-'}
          </Typography.Text>
          <Typography.Text type="secondary">
            下次同步 {syncState?.nextSyncTime || '-'}
          </Typography.Text>
          {syncState?.lastErrorMessage ? (
            <Typography.Text type="danger">
              错误：{syncState.lastErrorMessage}
            </Typography.Text>
          ) : null}
        </div>
        <Button
          type="primary"
          icon={<SyncOutlined />}
          loading={syncing}
          hidden={!access.hasPerms('integration:upstream:sync')}
          onClick={triggerSkuSync}
        >
          同步SKU
        </Button>
      </div>
      <div className={styles.skuFilters}>
        <Select
          {...SEARCHABLE_SELECT_PROPS}
          style={{ width: 160 }}
          options={skuSearchFieldOptions}
          value={searchField}
          onChange={setSearchField}
        />
        <Input.Search
          allowClear
          enterButton={<SearchOutlined />}
          placeholder="搜索同步清单"
          style={{ width: 260 }}
          value={searchInput}
          onChange={(event) => {
            setSearchInput(event.target.value);
            if (!event.target.value) {
              setKeyword('');
            }
          }}
          onSearch={(value) => setKeyword(value.trim())}
        />
        <Select
          {...SEARCHABLE_SELECT_PROPS}
          style={{ width: 150 }}
          options={skuPairingStatusOptions}
          value={pairingStatus}
          onChange={setPairingStatus}
        />
        <Select
          {...SEARCHABLE_SELECT_PROPS}
          style={{ width: 150 }}
          options={skuSyncItemStatusOptions}
          value={syncStatus}
          onChange={setSyncStatus}
        />
      </div>
      <ProTable<API.Integration.SkuSyncItem>
        actionRef={actionRef}
        className={`${styles.fillTable} upstream-fill-table`}
        rowKey="masterSku"
        columns={skuColumns}
        params={{
          field: searchField,
          keyword,
          pairingStatus,
          syncStatus,
          selectedCode,
        }}
        request={async (params) => {
          const resp = await getSkuSyncList(selectedCode, {
            pageNum: params.current,
            pageSize: params.pageSize,
            field: searchField === 'all' ? undefined : searchField,
            keyword,
            pairingStatus: pairingStatus || undefined,
            status: syncStatus || undefined,
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
        scroll={getProTableScroll(1400)}
        toolBarRender={false}
      />
    </div>
  );
}
