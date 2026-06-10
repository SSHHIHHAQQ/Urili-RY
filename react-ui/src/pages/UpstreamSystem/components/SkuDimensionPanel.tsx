import { SearchOutlined, SyncOutlined } from '@ant-design/icons';
import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Input, Modal, Select, Space, Typography } from 'antd';
import { type Key, type MutableRefObject, useEffect, useState } from 'react';
import {
  getSkuSyncList,
  getUpstreamSyncStates,
  syncUpstreamSkuDimensions,
  syncUpstreamSkuDimensionsSelected,
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

const parseSkuText = (value: string) =>
  Array.from(
    new Set(
      value
        .split(/[\s,，;；]+/)
        .map((item) => item.trim())
        .filter(Boolean),
    ),
  );

export default function SkuDimensionPanel({
  access,
  actionRef,
  selectedCode,
}: SkuDimensionPanelProps) {
  const [syncing, setSyncing] = useState(false);
  const [selectedSyncing, setSelectedSyncing] = useState(false);
  const [syncState, setSyncState] = useState<API.Integration.SyncState>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [selectedSkuText, setSelectedSkuText] = useState('');
  const [selectedModalOpen, setSelectedModalOpen] = useState(false);
  const [searchField, setSearchField] = useState('all');
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [pairingStatus, setPairingStatus] = useState('');
  const [syncStatus, setSyncStatus] = useState('');
  const [wmsStatus, setWmsStatus] = useState('');
  const canQueryUpstream = access.hasPerms('integration:upstream:query');

  const loadSyncState = async () => {
    if (!selectedCode || !canQueryUpstream) {
      setSyncState(undefined);
      return;
    }
    const resp = await getUpstreamSyncStates(selectedCode);
    if (resp.code === 200) {
      setSyncState((resp.data || []).find((item) => item.syncType === 'SKU_DIMENSION'));
    }
  };

  useEffect(() => {
    loadSyncState();
  }, [selectedCode, canQueryUpstream]);

  const isDimensionSyncing =
    syncing || selectedSyncing || syncState?.status === 'SYNCING';

  useEffect(() => {
    if (!isDimensionSyncing || !selectedCode) {
      return;
    }
    const timer = window.setInterval(() => {
      loadSyncState();
      actionRef.current?.reload();
    }, 5000);
    return () => window.clearInterval(timer);
  }, [isDimensionSyncing, selectedCode]);

  useEffect(() => {
    if (syncState?.status && syncState.status !== 'SYNCING') {
      setSyncing(false);
      setSelectedSyncing(false);
    }
  }, [syncState?.status]);

  const triggerDimensionSync = async () => {
    setSyncing(true);
    const hide = message.loading('正在提交仓库尺寸重量同步任务');
    let resp: (API.Result & { data: API.Integration.SyncResult }) | undefined;
    try {
      resp = await syncUpstreamSkuDimensions(selectedCode);
    } catch {
      setSyncing(false);
      return;
    } finally {
      hide();
    }
    if (!resp) {
      setSyncing(false);
      return;
    }
    if (resp.code === 200) {
      message.success('仓库尺寸重量同步已开始，可继续操作');
      await loadSyncState();
      actionRef.current?.reload();
    } else {
      setSyncing(false);
      message.error(resp.msg);
      await loadSyncState();
    }
  };

  const openSelectedSyncModal = () => {
    setSelectedSkuText(selectedRowKeys.map(String).join('\n'));
    setSelectedModalOpen(true);
  };

  const triggerSelectedDimensionSync = async () => {
    const skuList = parseSkuText(selectedSkuText);
    if (skuList.length === 0) {
      message.warning('请输入SKU');
      return;
    }
    if (skuList.length > 100) {
      message.warning('指定SKU一次最多100个');
      return;
    }
    setSelectedSyncing(true);
    const hide = message.loading('正在提交指定SKU尺寸重量获取任务');
    let resp: (API.Result & { data: API.Integration.SyncResult }) | undefined;
    try {
      resp = await syncUpstreamSkuDimensionsSelected(selectedCode, skuList);
    } catch {
      setSelectedSyncing(false);
      return;
    } finally {
      hide();
    }
    if (!resp) {
      setSelectedSyncing(false);
      return;
    }
    if (resp.code === 200) {
      message.success('指定SKU尺寸重量获取已开始，可继续操作');
      setSelectedModalOpen(false);
      setSelectedSkuText('');
      setSelectedRowKeys([]);
      await loadSyncState();
      actionRef.current?.reload();
    } else {
      setSelectedSyncing(false);
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
        <Space>
          <Button
            icon={<SearchOutlined />}
            disabled={isDimensionSyncing}
            loading={selectedSyncing}
            hidden={!access.hasPerms('integration:upstream:dimensionSync')}
            onClick={openSelectedSyncModal}
          >
            指定SKU获取
          </Button>
          <Button
            type="primary"
            icon={<SyncOutlined />}
            disabled={isDimensionSyncing}
            loading={syncing || syncState?.status === 'SYNCING'}
            hidden={!access.hasPerms('integration:upstream:dimensionSync')}
            onClick={triggerDimensionSync}
          >
            {isDimensionSyncing ? '正在同步' : '同步尺寸重量'}
          </Button>
        </Space>
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
          if (!canQueryUpstream) {
            return { data: [], total: 0, success: true };
          }
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
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys),
        }}
        search={false}
        options={false}
        scroll={getProTableScroll(1500)}
        toolBarRender={false}
      />
      <Modal
        title="指定SKU获取仓库尺寸重量"
        open={selectedModalOpen}
        confirmLoading={selectedSyncing}
        okText="开始获取"
        onCancel={() => setSelectedModalOpen(false)}
        onOk={triggerSelectedDimensionSync}
      >
        <Input.TextArea
          rows={7}
          value={selectedSkuText}
          placeholder="每行一个SKU，最多100个"
          onChange={(event) => setSelectedSkuText(event.target.value)}
        />
      </Modal>
    </div>
  );
}
