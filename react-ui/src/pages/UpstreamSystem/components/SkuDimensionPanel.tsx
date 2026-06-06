import { SearchOutlined, SyncOutlined } from '@ant-design/icons';
import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Input, Select, Typography } from 'antd';
import { type MutableRefObject, useEffect, useState } from 'react';
import {
  getSkuSyncList,
  getSkuSyncState,
  syncUpstreamSkuDimensions,
} from '@/services/integration/upstreamSystem';
import { message } from '@/utils/feedback';
import {
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  dimensionStatusOptions,
  skuPairingStatusOptions,
  skuSearchFieldOptions,
  skuSyncItemStatusOptions,
} from '../constants';
import { statusTag } from '../helpers';
import styles from '../style.module.css';

type SkuDimensionPanelProps = {
  access: { hasPerms: (permission: string) => boolean };
  actionRef: MutableRefObject<ActionType | null>;
  selectedCode: string;
};

const hasDimensionValue = (value?: number) => value !== undefined && value !== null;

const dimensionStatus = (record: API.Integration.SkuSyncItem) => {
  const count = [
    record.wmsLength,
    record.wmsWidth,
    record.wmsHeight,
    record.wmsWeight,
  ].filter(hasDimensionValue).length;
  if (count === 4) return 'COMPLETE';
  if (count === 0) return 'MISSING';
  return 'PARTIAL';
};

const dimensionText = (
  length?: number,
  width?: number,
  height?: number,
  unit = 'cm',
) => {
  if (!hasDimensionValue(length) && !hasDimensionValue(width) && !hasDimensionValue(height)) {
    return '-';
  }
  return `${length ?? '-'} * ${width ?? '-'} * ${height ?? '-'} ${unit}`;
};

const weightText = (weight?: number) =>
  hasDimensionValue(weight) ? `${weight} kg` : '-';

export default function SkuDimensionPanel({
  access,
  actionRef,
  selectedCode,
}: SkuDimensionPanelProps) {
  const [syncing, setSyncing] = useState(false);
  const [syncState, setSyncState] = useState<API.Integration.SkuSyncState>();
  const [searchField, setSearchField] = useState('all');
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [pairingStatus, setPairingStatus] = useState('');
  const [syncStatus, setSyncStatus] = useState('');
  const [wmsStatus, setWmsStatus] = useState('');

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

  const triggerDimensionSync = async () => {
    setSyncing(true);
    const hide = message.loading('正在同步仓库尺寸重量');
    const resp = await syncUpstreamSkuDimensions(selectedCode);
    hide();
    setSyncing(false);
    if (resp.code === 200) {
      message.success(`仓库尺寸重量同步完成：${resp.data?.skuDimensionCount || 0}`);
      await loadSyncState();
      actionRef.current?.reload();
    } else {
      message.error(resp.msg);
      await loadSyncState();
    }
  };

  const columns: ProColumns<API.Integration.SkuSyncItem>[] = [
    {
      title: '领星 masterSku',
      dataIndex: 'masterSku',
      width: 180,
      copyable: true,
    },
    {
      title: '领星产品名',
      dataIndex: 'masterProductName',
      width: 220,
      ellipsis: true,
    },
    {
      title: '客户尺寸',
      dataIndex: 'length',
      width: 180,
      render: (_, record) => dimensionText(record.length, record.width, record.height),
    },
    {
      title: '客户重量',
      dataIndex: 'weight',
      width: 110,
      render: (_, record) => weightText(record.weight),
    },
    {
      title: '仓库尺寸',
      dataIndex: 'wmsLength',
      width: 180,
      render: (_, record) =>
        dimensionText(record.wmsLength, record.wmsWidth, record.wmsHeight),
    },
    {
      title: '仓库重量',
      dataIndex: 'wmsWeight',
      width: 110,
      render: (_, record) => weightText(record.wmsWeight),
    },
    {
      title: '尺寸状态',
      dataIndex: 'dimensionStatus',
      width: 110,
      render: (_, record) => statusTag(dimensionStatus(record)),
    },
    {
      title: '系统SKU',
      dataIndex: 'systemSku',
      width: 160,
    },
    {
      title: '客户名称',
      dataIndex: 'customerName',
      width: 160,
      ellipsis: true,
    },
    {
      title: '最近发现',
      dataIndex: 'lastSeenTime',
      width: 170,
    },
  ];

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
        </div>
        <Button
          type="primary"
          icon={<SyncOutlined />}
          loading={syncing}
          hidden={!access.hasPerms('integration:upstream:dimensionSync')}
          onClick={triggerDimensionSync}
        >
          同步尺寸重量
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
          placeholder="搜索SKU/产品"
          style={{ width: 240 }}
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
          options={dimensionStatusOptions}
          value={wmsStatus}
          onChange={setWmsStatus}
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
        columns={columns}
        params={{
          field: searchField,
          keyword,
          pairingStatus,
          syncStatus,
          wmsStatus,
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
            dimensionStatus: wmsStatus || undefined,
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
        scroll={getProTableScroll(1500)}
        toolBarRender={false}
      />
    </div>
  );
}
